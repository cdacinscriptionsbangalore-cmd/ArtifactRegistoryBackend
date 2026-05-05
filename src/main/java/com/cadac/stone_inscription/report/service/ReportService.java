package com.cadac.stone_inscription.report.service;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.entity.UserAuth;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.report.dto.CreateReportRequest;
import com.cadac.stone_inscription.report.dto.ModerateReportRequest;
import com.cadac.stone_inscription.report.entity.ModerationReport;
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

    public ResponseEntity<?> createReport(String reporterEmail, CreateReportRequest request) {
        User reporter = getUserByEmail(reporterEmail);
        ResolvedReportTarget target = reportTargetResolver.resolve(request.getTargetType(), request.getTargetId());

        validateReportRequest(reporter, target);

        ModerationReport report = reportFactory.create(reporter, target, request);
        moderationReportRepository.save(report);
        reportActionService.markTargetUnderReview(target, reporter, request.getDetails());

        return UserResponse.responseHandler("Report created successfully", HttpStatus.CREATED, report);
    }

    public ResponseEntity<?> getReports(String requesterEmail, ReportStatus status) {
        ensureModerator(requesterEmail);

        List<ModerationReport> reports = status == null
                ? moderationReportRepository.findAllByOrderByCreatedAtDesc()
                : moderationReportRepository.findByStatusOrderByCreatedAtDesc(status);

        return UserResponse.responseHandler("Reports fetched successfully", HttpStatus.OK, reports);
    }

    public ResponseEntity<?> moderateReport(String actorEmail, String reportId, ModerateReportRequest request) {
        User actor = getUserByEmail(actorEmail);
        ModerationReport report = moderationReportRepository.findById(parseObjectId(reportId))
                .orElseThrow(() -> new StoneInscriptionException("Report not found", HttpStatus.NOT_FOUND));

        ResolvedReportTarget target = reportTargetResolver.resolve(report.getTargetType(), report.getTargetId());

        if (report.getStatus() == ReportStatus.RESOLVED) {
            throw new StoneInscriptionException("Report is already resolved", HttpStatus.BAD_REQUEST);
        }

        if (report.getStatus() == ReportStatus.ESCALATED) {
            ensureModerator(actorEmail);
        }

        ModerationHandler moderationPipeline = buildModerationPipeline();
        ModerationExecutionContext context = ModerationExecutionContext.builder()
                .report(report)
                .target(target)
                .actor(actor)
                .actorLabel(actor.getId().toHexString())
                .requestedAction(request == null ? null : request.getAction())
                .note(request == null ? null : request.getNote())
                .reportActionService(reportActionService)
                .build();

        moderationPipeline.handle(context);
        moderationReportRepository.save(report);

        String message = report.getStatus() == ReportStatus.ESCALATED
                ? "Report escalated for human moderation"
                : "Report moderation completed";

        return UserResponse.responseHandler(message, HttpStatus.OK, report);
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

    private ModerationHandler buildModerationPipeline() {
        aiModerationHandler.setNext(humanModerationHandler);
        return aiModerationHandler;
    }

    private User getUserByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new StoneInscriptionException("User not found", HttpStatus.NOT_FOUND);
        }
        return user;
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

    private ObjectId parseObjectId(String id) {
        if (!ObjectId.isValid(id)) {
            throw new StoneInscriptionException("Invalid report id", HttpStatus.BAD_REQUEST);
        }
        return new ObjectId(id);
    }
}
