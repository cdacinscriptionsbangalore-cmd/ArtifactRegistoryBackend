package com.cadac.stone_inscription.report.service;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.entity.UserAuth;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.kafka.events.report.ReportSubmittedEvent;
import com.cadac.stone_inscription.report.dto.CreateReportRequest;
import com.cadac.stone_inscription.report.dto.ModerateReportRequest;
import com.cadac.stone_inscription.report.entity.ModerationReport;
import com.cadac.stone_inscription.report.enums.ReportReason;
import com.cadac.stone_inscription.report.enums.ReportTargetType;
import com.cadac.stone_inscription.report.enums.ReportStatus;
import com.cadac.stone_inscription.report.factory.ReportFactory;
import com.cadac.stone_inscription.report.moderation.AiModerationHandler;
import com.cadac.stone_inscription.report.moderation.HumanModerationHandler;
import com.cadac.stone_inscription.report.moderation.ModerationExecutionContext;
import com.cadac.stone_inscription.report.moderation.ModerationHandler;
import com.cadac.stone_inscription.report.repository.ModerationReportRepository;
import com.cadac.stone_inscription.report.resolver.ReportTargetResolver;
import com.cadac.stone_inscription.report.resolver.ResolvedReportTarget;
import com.cadac.stone_inscription.report.specification.ReportValidationContext;
import com.cadac.stone_inscription.report.specification.Specification;
import com.cadac.stone_inscription.repository.UserAuthRepository;
import com.cadac.stone_inscription.repository.UserRepository;
import com.cadac.stone_inscription.user.service.BlacklistGuardService;
import com.cadac.stone_inscription.util.UserResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ModerationReportRepository moderationReportRepository;
    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final ReportFactory reportFactory;
    private final ReportTargetResolver reportTargetResolver;
    private final ReportActionService reportActionService;
    private final AiModerationHandler aiModerationHandler;
    private final HumanModerationHandler humanModerationHandler;
    private final List<Specification<ReportValidationContext>> reportSpecifications;
    private final BlacklistGuardService blacklistGuardService;

    public ResponseEntity<?> submitLegacySynchronousReport(String reporterEmail, CreateReportRequest request) {
        User reporter = getUserByEmailOrThrow(reporterEmail);
        blacklistGuardService.ensureCanReport(reporter);
        ResolvedReportTarget target = reportTargetResolver.resolve(request.getTargetType(), request.getTargetId());

        validateReportRequest(reporter, target);

        ModerationReport report = reportFactory.create(reporter, target, request);
        saveNewReport(report);
        reportActionService.markTargetUnderReview(target, reporter, request.getDetails());
        report = moderateReportInternal(report, target, null, null);

        String message = switch (report.getStatus()) {
            case ESCALATED -> "Report created and escalated for human moderation";
            case AI_SCREENING -> "Report created and awaiting AI moderation";
            default -> "Report created and moderated successfully";
        };

        return UserResponse.responseHandler(message, HttpStatus.CREATED, report);
    }

    public ResponseEntity<?> createReport(String reporterEmail, CreateReportRequest request) {
        return submitLegacySynchronousReport(reporterEmail, request);
    }

    public ResponseEntity<?> getReports(String requesterEmail, ReportStatus status) {
        ensureModerator(requesterEmail);

        List<ModerationReport> reports = status == null
                ? moderationReportRepository.findAllByOrderByCreatedAtDesc()
                : moderationReportRepository.findByStatusOrderByCreatedAtDesc(status);

        return UserResponse.responseHandler("Reports fetched successfully", HttpStatus.OK, reports);
    }

    public ResponseEntity<?> moderateReport(String actorEmail, String reportId, ModerateReportRequest request) {
        User actor = getUserByEmailOrThrow(actorEmail);
        ModerationReport report = moderationReportRepository.findById(parseObjectId(reportId))
                .orElseThrow(() -> new StoneInscriptionException("Report not found", HttpStatus.NOT_FOUND));

        ResolvedReportTarget target = reportTargetResolver.resolve(report.getTargetType(), report.getTargetId());

        if (report.getStatus() == ReportStatus.RESOLVED) {
            throw new StoneInscriptionException("Report is already resolved", HttpStatus.BAD_REQUEST);
        }

        if (report.getStatus() == ReportStatus.ESCALATED) {
            ensureModerator(actorEmail);
        }

        report = moderateReportInternal(report, target, actor, request);

        String message = switch (report.getStatus()) {
            case ESCALATED -> "Report escalated for human moderation";
            case AI_SCREENING -> "Report remains in AI screening";
            default -> "Report moderation completed";
        };

        return UserResponse.responseHandler(message, HttpStatus.OK, report);
    }

    public ResolvedReportTarget validateSubmission(User reporter, CreateReportRequest request) {
        blacklistGuardService.ensureCanReport(reporter);
        ResolvedReportTarget target = reportTargetResolver.resolve(request.getTargetType(), request.getTargetId());
        validateReportRequest(reporter, target);
        return target;
    }

    public ModerationReport createOrGetReportFromEvent(ReportSubmittedEvent event) {
        ReportTargetType targetType = parseTargetType(event.getTargetType());
        String activeReportKey = ModerationReport.buildActiveReportKey(
                event.getReporterId(),
                event.getTargetId(),
                targetType);

        Optional<ModerationReport> existingReport = moderationReportRepository.findFirstByActiveReportKey(activeReportKey);

        if (existingReport.isPresent()) {
            return existingReport.get();
        }

        User reporter = getUserByIdOrThrow(event.getReporterId());
        ResolvedReportTarget target = reportTargetResolver.resolve(targetType, event.getTargetId());
        CreateReportRequest request = buildCreateReportRequest(event);
        validateSubmission(reporter, request);

        ModerationReport report = reportFactory.create(reporter, target, request);
        return saveNewReportOrGetExisting(report, target, reporter, request.getDetails());
    }

    public String runAiModerationForSubmittedEvent(ReportSubmittedEvent event) {
        ModerationReport report = createOrGetReportFromEvent(event);

        if (report.getStatus() == ReportStatus.RESOLVED) {
            return null;
        }

        if (report.getStatus() == ReportStatus.ESCALATED) {
            return report.getId().toHexString();
        }

        ResolvedReportTarget target = reportTargetResolver.resolve(report.getTargetType(), report.getTargetId());
        moderateAiOnly(report, target, "Queued AI moderation review");
        report = saveReportOrReloadLatest(report);

        return report.getStatus() == ReportStatus.ESCALATED
                ? report.getId().toHexString()
                : null;
    }

    private void validateReportRequest(User reporter, ResolvedReportTarget target) {
        ReportValidationContext context = ReportValidationContext.builder()
                .reporter(reporter)
                .target(target)
                .reportRepository(moderationReportRepository)
                .build();

        List<String> violations = reportSpecifications.stream()
                .filter(specification -> !specification.isSatisfiedBy(context))
                .map(Specification::errorMessage)
                .toList();

        if (!violations.isEmpty()) {
            throw new StoneInscriptionException(String.join(" ", violations), HttpStatus.BAD_REQUEST);
        }
    }

    private void moderateAiOnly(ModerationReport report, ResolvedReportTarget target, String note) {
        ModerationExecutionContext context = ModerationExecutionContext.builder()
                .report(report)
                .target(target)
                .actor(null)
                .actorLabel("AI_MODERATOR")
                .requestedAction(null)
                .note(note)
                .reportActionService(reportActionService)
                .build();

        aiModerationHandler.handle(context);
    }

    private ModerationHandler buildModerationPipeline() {
        aiModerationHandler.setNext(humanModerationHandler);
        return aiModerationHandler;
    }

    private ModerationReport moderateReportInternal(
            ModerationReport report,
            ResolvedReportTarget target,
            User actor,
            ModerateReportRequest request) {
        ModerationHandler moderationPipeline = buildModerationPipeline();
        ModerationExecutionContext context = ModerationExecutionContext.builder()
                .report(report)
                .target(target)
                .actor(actor)
                .actorLabel(actor == null || actor.getId() == null ? null : actor.getId().toHexString())
                .requestedAction(request == null ? null : request.getAction())
                .note(request == null ? null : request.getNote())
                .reportActionService(reportActionService)
                .build();

        moderationPipeline.handle(context);
        return saveReportWithConflictHandling(report);
    }

    private void saveNewReport(ModerationReport report) {
        try {
            moderationReportRepository.save(report);
        } catch (DuplicateKeyException ex) {
            throw new StoneInscriptionException("You have already reported this target.", HttpStatus.BAD_REQUEST);
        }
    }

    private ModerationReport saveNewReportOrGetExisting(
            ModerationReport report,
            ResolvedReportTarget target,
            User reporter,
            String details) {
        try {
            moderationReportRepository.save(report);
            reportActionService.markTargetUnderReview(target, reporter, details);
            return report;
        } catch (DuplicateKeyException ex) {
            return moderationReportRepository.findFirstByActiveReportKey(report.getActiveReportKey())
                    .orElseThrow(() -> new StoneInscriptionException(
                            "Concurrent report creation failed to reload the active report.",
                            HttpStatus.CONFLICT));
        }
    }

    private ModerationReport saveReportWithConflictHandling(ModerationReport report) {
        try {
            return moderationReportRepository.save(report);
        } catch (OptimisticLockingFailureException ex) {
            throw new StoneInscriptionException(
                    "Report was updated by another moderation flow. Please reload and try again.",
                    HttpStatus.CONFLICT);
        }
    }

    private ModerationReport saveReportOrReloadLatest(ModerationReport report) {
        try {
            return moderationReportRepository.save(report);
        } catch (OptimisticLockingFailureException ex) {
            return getReportByIdOrThrow(report.getId().toHexString());
        }
    }

    public User getUserByEmailOrThrow(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new StoneInscriptionException("User not found", HttpStatus.NOT_FOUND);
        }
        return user;
    }

    private User getUserByIdOrThrow(String userId) {
        return userRepository.findById(parseObjectId(userId))
                .orElseThrow(() -> new StoneInscriptionException("User not found", HttpStatus.NOT_FOUND));
    }

    private ModerationReport getReportByIdOrThrow(String reportId) {
        return moderationReportRepository.findById(parseObjectId(reportId))
                .orElseThrow(() -> new StoneInscriptionException("Report not found", HttpStatus.NOT_FOUND));
    }

    private void ensureModerator(String email) {
        UserAuth userAuth = userAuthRepository.findByEmail(email);
        if (userAuth == null || userAuth.getRoles() == null) {
            throw new StoneInscriptionException("Moderator access required", HttpStatus.FORBIDDEN);
        }

        boolean hasModeratorRole = userAuth.getRoles().stream()
                .map(String::toLowerCase)
                .anyMatch(role -> role.equals("admin")
                        || role.equals("moderator")
                        || role.equals("human_moderator")
                        || role.equals("ai_moderator"));

        if (!hasModeratorRole) {
            throw new StoneInscriptionException("Moderator access required", HttpStatus.FORBIDDEN);
        }
    }

    private CreateReportRequest buildCreateReportRequest(ReportSubmittedEvent event) {
        CreateReportRequest request = new CreateReportRequest();
        request.setTargetType(parseTargetType(event.getTargetType()));
        request.setTargetId(event.getTargetId());
        request.setReason(parseReason(event.getReason()));
        request.setDetails(event.getDescription());
        return request;
    }

    private ReportTargetType parseTargetType(String rawTargetType) {
        try {
            return ReportTargetType.valueOf(rawTargetType);
        } catch (IllegalArgumentException ex) {
            throw new StoneInscriptionException("Invalid target type", HttpStatus.BAD_REQUEST);
        }
    }

    private ReportReason parseReason(String rawReason) {
        try {
            return ReportReason.valueOf(rawReason);
        } catch (IllegalArgumentException ex) {
            throw new StoneInscriptionException("Invalid report reason", HttpStatus.BAD_REQUEST);
        }
    }

    private ObjectId parseObjectId(String id) {
        if (!ObjectId.isValid(id)) {
            throw new StoneInscriptionException("Invalid report id", HttpStatus.BAD_REQUEST);
        }
        return new ObjectId(id);
    }
}
