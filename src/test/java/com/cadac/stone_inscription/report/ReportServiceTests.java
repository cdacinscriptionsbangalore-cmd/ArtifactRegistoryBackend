package com.cadac.stone_inscription.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.entity.UserAuth;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.report.dto.CreateReportRequest;
import com.cadac.stone_inscription.report.dto.ModerateReportRequest;
import com.cadac.stone_inscription.report.entity.ModerationReport;
import com.cadac.stone_inscription.report.enums.ModerationAction;
import com.cadac.stone_inscription.report.enums.ReportReason;
import com.cadac.stone_inscription.report.enums.ReportStatus;
import com.cadac.stone_inscription.report.enums.ReportTargetType;
import com.cadac.stone_inscription.report.factory.ReportFactory;
import com.cadac.stone_inscription.report.moderation.AiModerationHandler;
import com.cadac.stone_inscription.report.moderation.HumanModerationHandler;
import com.cadac.stone_inscription.report.repository.ModerationReportRepository;
import com.cadac.stone_inscription.report.resolver.ReportTargetResolver;
import com.cadac.stone_inscription.report.resolver.ResolvedReportTarget;
import com.cadac.stone_inscription.report.service.ReportActionService;
import com.cadac.stone_inscription.report.service.ReportService;
import com.cadac.stone_inscription.report.specification.Specification;
import com.cadac.stone_inscription.report.specification.ReportValidationContext;
import com.cadac.stone_inscription.repository.UserAuthRepository;
import com.cadac.stone_inscription.repository.UserRepository;
import com.cadac.stone_inscription.user.service.BlacklistGuardService;

class ReportServiceTests {

    @Test
    void createReportAutoModeratesHighConfidenceReports() {
        User reporter = User.builder().id(new ObjectId()).email("reporter@example.com").name("Reporter").build();
        CreateReportRequest request = createRequest(ReportReason.SPAM, "obvious scam");
        ResolvedReportTarget target = ResolvedReportTarget.builder()
                .id(new ObjectId().toHexString())
                .authorId(new ObjectId().toHexString())
                .type(ReportTargetType.POST)
                .content("spam scam content")
                .entity(new Object())
                .build();

        TrackingReportActionService reportActionService = new TrackingReportActionService();
        ReportService reportService = new ReportService(
                moderationReportRepository(null),
                userRepository(reporter),
                userAuthRepository(null),
                new ReportFactory(),
                new FixedTargetResolver(target),
                reportActionService,
                new AiModerationHandler(),
                new HumanModerationHandler(),
                List.<Specification<ReportValidationContext>>of(),
                new BlacklistGuardService());

        ResponseEntity<?> response = reportService.createReport(reporter.getEmail(), request);

        Map<?, ?> body = assertInstanceOf(Map.class, response.getBody());
        ModerationReport report = assertInstanceOf(ModerationReport.class, body.get("data"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Report created and moderated successfully", body.get("message"));
        assertEquals(ReportStatus.RESOLVED, report.getStatus());
        assertEquals(ModerationAction.REMOVE_CONTENT, report.getActionTaken());
        assertEquals(1, reportActionService.markUnderReviewInvocations);
        assertEquals(1, reportActionService.applyActionInvocations);
        assertEquals(ModerationAction.REMOVE_CONTENT, reportActionService.lastAction);
        assertEquals("AI_MODERATOR", reportActionService.lastActor);
    }

    @Test
    void createReportAutoEscalatesLowConfidenceReports() {
        User reporter = User.builder().id(new ObjectId()).email("reporter@example.com").name("Reporter").build();
        CreateReportRequest request = createRequest(ReportReason.OTHER, "not sure");
        ResolvedReportTarget target = ResolvedReportTarget.builder()
                .id(new ObjectId().toHexString())
                .authorId(new ObjectId().toHexString())
                .type(ReportTargetType.POST)
                .content("harmless discussion")
                .entity(new Object())
                .build();

        TrackingReportActionService reportActionService = new TrackingReportActionService();
        ReportService reportService = new ReportService(
                moderationReportRepository(null),
                userRepository(reporter),
                userAuthRepository(null),
                new ReportFactory(),
                new FixedTargetResolver(target),
                reportActionService,
                new AiModerationHandler(),
                new HumanModerationHandler(),
                List.<Specification<ReportValidationContext>>of(),
                new BlacklistGuardService());

        ResponseEntity<?> response = reportService.createReport(reporter.getEmail(), request);

        Map<?, ?> body = assertInstanceOf(Map.class, response.getBody());
        ModerationReport report = assertInstanceOf(ModerationReport.class, body.get("data"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Report created and escalated for human moderation", body.get("message"));
        assertEquals(ReportStatus.ESCALATED, report.getStatus());
        assertEquals(ModerationAction.ESCALATE, report.getActionTaken());
        assertEquals(1, reportActionService.markUnderReviewInvocations);
        assertEquals(0, reportActionService.applyActionInvocations);
    }

    @Test
    void moderateReportResolvesEscalatedReportWithModeratorAction() {
        User moderator = User.builder().id(new ObjectId()).email("mod@example.com").build();
        UserAuth moderatorAuth = UserAuth.builder().email(moderator.getEmail()).roles(List.of("moderator")).build();
        ModerationReport report = ModerationReport.builder()
                .id(new ObjectId())
                .reporterId(new ObjectId().toHexString())
                .targetId(new ObjectId().toHexString())
                .targetType(ReportTargetType.POST)
                .targetAuthorId(new ObjectId().toHexString())
                .reason(ReportReason.OTHER)
                .details("needs human review")
                .status(ReportStatus.ESCALATED)
                .actionTaken(ModerationAction.ESCALATE)
                .build();
        ResolvedReportTarget target = ResolvedReportTarget.builder()
                .id(report.getTargetId())
                .authorId(report.getTargetAuthorId())
                .type(ReportTargetType.POST)
                .content("normal content")
                .entity(new Object())
                .build();
        ModerateReportRequest request = new ModerateReportRequest();
        request.setAction(ModerationAction.DISMISS);
        request.setNote("false positive");

        TrackingReportActionService reportActionService = new TrackingReportActionService();
        ReportService reportService = new ReportService(
                moderationReportRepository(report),
                userRepository(moderator),
                userAuthRepository(moderatorAuth),
                new ReportFactory(),
                new FixedTargetResolver(target),
                reportActionService,
                new AiModerationHandler(),
                new HumanModerationHandler(),
                List.<Specification<ReportValidationContext>>of(),
                new BlacklistGuardService());

        ResponseEntity<?> response = reportService.moderateReport(moderator.getEmail(), report.getId().toHexString(), request);

        Map<?, ?> body = assertInstanceOf(Map.class, response.getBody());
        ModerationReport updatedReport = assertInstanceOf(ModerationReport.class, body.get("data"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Report moderation completed", body.get("message"));
        assertEquals(ReportStatus.RESOLVED, updatedReport.getStatus());
        assertEquals(ModerationAction.DISMISS, updatedReport.getActionTaken());
        assertEquals(1, reportActionService.applyActionInvocations);
        assertEquals(ModerationAction.DISMISS, reportActionService.lastAction);
        assertEquals(moderator.getId().toHexString(), reportActionService.lastActor);
        assertEquals("false positive", reportActionService.lastNote);
    }

    @Test
    void createReportRejectsBlacklistedReporterBeforeModeration() {
        User reporter = User.builder()
                .id(new ObjectId())
                .email("reporter@example.com")
                .name("Reporter")
                .blackListed(true)
                .build();
        CreateReportRequest request = createRequest(ReportReason.SPAM, "obvious scam");
        TrackingReportActionService reportActionService = new TrackingReportActionService();
        ReportService reportService = new ReportService(
                moderationReportRepository(null),
                userRepository(reporter),
                userAuthRepository(null),
                new ReportFactory(),
                new FixedTargetResolver(baseTarget()),
                reportActionService,
                new AiModerationHandler(),
                new HumanModerationHandler(),
                List.<Specification<ReportValidationContext>>of(),
                new BlacklistGuardService());

        StoneInscriptionException exception = assertThrows(
                StoneInscriptionException.class,
                () -> reportService.createReport(reporter.getEmail(), request));

        assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        assertEquals(0, reportActionService.markUnderReviewInvocations);
        assertEquals(0, reportActionService.applyActionInvocations);
    }

    private CreateReportRequest createRequest(ReportReason reason, String details) {
        CreateReportRequest request = new CreateReportRequest();
        request.setTargetType(ReportTargetType.POST);
        request.setTargetId(new ObjectId().toHexString());
        request.setReason(reason);
        request.setDetails(details);
        return request;
    }

    private ResolvedReportTarget baseTarget() {
        return ResolvedReportTarget.builder()
                .id(new ObjectId().toHexString())
                .authorId(new ObjectId().toHexString())
                .type(ReportTargetType.POST)
                .content("content")
                .entity(new Object())
                .build();
    }

    private ModerationReportRepository moderationReportRepository(ModerationReport foundReport) {
        return (ModerationReportRepository) Proxy.newProxyInstance(
                ModerationReportRepository.class.getClassLoader(),
                new Class<?>[] { ModerationReportRepository.class },
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    if ("findById".equals(method.getName())) {
                        return Optional.ofNullable(foundReport);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private UserRepository userRepository(User user) {
        return (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[] { UserRepository.class },
                (proxy, method, args) -> {
                    if ("findByEmail".equals(method.getName())) {
                        return user;
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private UserAuthRepository userAuthRepository(UserAuth userAuth) {
        return (UserAuthRepository) Proxy.newProxyInstance(
                UserAuthRepository.class.getClassLoader(),
                new Class<?>[] { UserAuthRepository.class },
                (proxy, method, args) -> {
                    if ("findByEmail".equals(method.getName())) {
                        return userAuth;
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }

    private static class FixedTargetResolver extends ReportTargetResolver {
        private final ResolvedReportTarget target;

        FixedTargetResolver(ResolvedReportTarget target) {
            super(null, null, null);
            this.target = target;
        }

        @Override
        public ResolvedReportTarget resolve(ReportTargetType targetType, String targetId) {
            return target;
        }
    }

    private static class TrackingReportActionService extends ReportActionService {
        private int markUnderReviewInvocations;
        private int applyActionInvocations;
        private ModerationAction lastAction;
        private String lastActor;
        private String lastNote;

        TrackingReportActionService() {
            super(null, null, null, null);
        }

        @Override
        public void markTargetUnderReview(ResolvedReportTarget target, User reporter, String details) {
            markUnderReviewInvocations++;
        }

        @Override
        public void applyAction(
                ModerationReport report,
                ResolvedReportTarget target,
                ModerationAction action,
                String actor,
                String note) {
            applyActionInvocations++;
            lastAction = action;
            lastActor = actor;
            lastNote = note;
        }
    }
}
