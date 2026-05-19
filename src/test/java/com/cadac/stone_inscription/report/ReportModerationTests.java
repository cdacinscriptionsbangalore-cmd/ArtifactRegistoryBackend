package com.cadac.stone_inscription.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;

import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.report.entity.ModerationReport;
import com.cadac.stone_inscription.report.enums.ModerationAction;
import com.cadac.stone_inscription.report.enums.ReportReason;
import com.cadac.stone_inscription.report.enums.ReportStatus;
import com.cadac.stone_inscription.report.enums.ReportTargetType;
import com.cadac.stone_inscription.report.moderation.AiModerationHandler;
import com.cadac.stone_inscription.report.moderation.HumanModerationHandler;
import com.cadac.stone_inscription.report.moderation.ModerationExecutionContext;
import com.cadac.stone_inscription.report.resolver.ResolvedReportTarget;
import com.cadac.stone_inscription.report.service.ReportActionService;

class ReportModerationTests {

    @Test
    void moderationReportRejectsInvalidStateTransition() {
        ModerationReport report = baseReport();

        StoneInscriptionException exception = assertThrows(
                StoneInscriptionException.class,
                () -> report.transitionTo(ReportStatus.RESOLVED, "tester", ModerationAction.DISMISS, null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void aiHandlerAutoResolvesHighConfidenceReports() {
        AiModerationHandler aiModerationHandler = new StubAiModerationHandler("{\"confidence\":0.95}");
        TrackingReportActionService reportActionService = new TrackingReportActionService();

        ModerationReport report = baseReport();
        ResolvedReportTarget target = ResolvedReportTarget.builder()
                .id(new ObjectId().toHexString())
                .authorId(new ObjectId().toHexString())
                .type(ReportTargetType.POST)
                .content("This is obvious spam scam content")
                .entity(new Object())
                .build();

        ModerationExecutionContext context = ModerationExecutionContext.builder()
                .report(report)
                .target(target)
                .actor(User.builder().id(new ObjectId()).build())
                .actorLabel("tester")
                .requestedAction(null)
                .note(null)
                .reportActionService(reportActionService)
                .build();

        aiModerationHandler.handle(context);

        assertEquals(ReportStatus.RESOLVED, report.getStatus());
        assertEquals(ModerationAction.REMOVE_CONTENT, report.getActionTaken());
        assertEquals(1, reportActionService.invocations);
        assertEquals(ModerationAction.REMOVE_CONTENT, reportActionService.lastAction);
    }

    @Test
    void aiHandlerEscalatesLowConfidenceReports() {
        AiModerationHandler aiModerationHandler = new StubAiModerationHandler("{\"confidence\":0.20}");
        TrackingReportActionService reportActionService = new TrackingReportActionService();

        ModerationReport report = baseReport();
        report.setReason(ReportReason.OTHER);

        ResolvedReportTarget target = ResolvedReportTarget.builder()
                .id(new ObjectId().toHexString())
                .authorId(new ObjectId().toHexString())
                .type(ReportTargetType.POST)
                .content("Normal cooking discussion")
                .entity(new Object())
                .build();

        ModerationExecutionContext context = ModerationExecutionContext.builder()
                .report(report)
                .target(target)
                .actor(User.builder().id(new ObjectId()).build())
                .actorLabel("tester")
                .requestedAction(null)
                .note(null)
                .reportActionService(reportActionService)
                .build();

        aiModerationHandler.handle(context);

        assertEquals(ReportStatus.ESCALATED, report.getStatus());
        assertEquals(ModerationAction.ESCALATE, report.getActionTaken());
        assertEquals(0, reportActionService.invocations);
    }

    @Test
    void aiHandlerKeepsReportInAiScreeningWhenWebhookIsUnavailable() {
        AiModerationHandler aiModerationHandler = new StubAiModerationHandler(null);
        TrackingReportActionService reportActionService = new TrackingReportActionService();

        ModerationReport report = baseReport();

        ModerationExecutionContext context = ModerationExecutionContext.builder()
                .report(report)
                .target(baseTarget(new ObjectId().toHexString()))
                .actor(User.builder().id(new ObjectId()).build())
                .actorLabel("tester")
                .requestedAction(null)
                .note(null)
                .reportActionService(reportActionService)
                .build();

        aiModerationHandler.handle(context);

        assertEquals(ReportStatus.AI_SCREENING, report.getStatus());
        assertEquals(ModerationAction.NONE, report.getActionTaken());
        assertEquals(0, reportActionService.invocations);
    }

    @Test
    void humanHandlerRejectsMissingActionForEscalatedReport() {
        HumanModerationHandler humanModerationHandler = new HumanModerationHandler();
        TrackingReportActionService reportActionService = new TrackingReportActionService();

        ModerationReport report = baseReport();
        report.setStatus(ReportStatus.ESCALATED);

        ModerationExecutionContext context = ModerationExecutionContext.builder()
                .report(report)
                .target(baseTarget(new ObjectId().toHexString()))
                .actor(User.builder().id(new ObjectId()).build())
                .actorLabel("moderator")
                .requestedAction(null)
                .reportActionService(reportActionService)
                .build();

        StoneInscriptionException exception = assertThrows(
                StoneInscriptionException.class,
                () -> humanModerationHandler.handle(context));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void humanHandlerRejectsModeratingOwnContent() {
        HumanModerationHandler humanModerationHandler = new HumanModerationHandler();
        TrackingReportActionService reportActionService = new TrackingReportActionService();

        ObjectId moderatorId = new ObjectId();
        ModerationReport report = baseReport();
        report.setStatus(ReportStatus.ESCALATED);

        ModerationExecutionContext context = ModerationExecutionContext.builder()
                .report(report)
                .target(baseTarget(moderatorId.toHexString()))
                .actor(User.builder().id(moderatorId).build())
                .actorLabel("moderator")
                .requestedAction(ModerationAction.DISMISS)
                .reportActionService(reportActionService)
                .build();

        StoneInscriptionException exception = assertThrows(
                StoneInscriptionException.class,
                () -> humanModerationHandler.handle(context));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
        assertEquals(0, reportActionService.invocations);
    }

    @Test
    void humanHandlerResolvesEscalatedReportWithValidAction() {
        HumanModerationHandler humanModerationHandler = new HumanModerationHandler();
        TrackingReportActionService reportActionService = new TrackingReportActionService();

        ModerationReport report = baseReport();
        report.setStatus(ReportStatus.ESCALATED);

        ModerationExecutionContext context = ModerationExecutionContext.builder()
                .report(report)
                .target(baseTarget(new ObjectId().toHexString()))
                .actor(User.builder().id(new ObjectId()).build())
                .actorLabel("moderator")
                .requestedAction(ModerationAction.BAN_AUTHOR)
                .note("repeat offense")
                .reportActionService(reportActionService)
                .build();

        humanModerationHandler.handle(context);

        assertEquals(ReportStatus.RESOLVED, report.getStatus());
        assertEquals(ModerationAction.BAN_AUTHOR, report.getActionTaken());
        assertEquals(1, reportActionService.invocations);
        assertEquals(ModerationAction.BAN_AUTHOR, reportActionService.lastAction);
    }

    @Test
    void moderationReportClearsActiveKeyWhenResolved() {
        ModerationReport report = baseReport();
        report.setActiveReportKey(ModerationReport.buildActiveReportKey(
                report.getReporterId(),
                report.getTargetId(),
                report.getTargetType()));

        report.transitionTo(ReportStatus.AI_SCREENING, "ai", ModerationAction.NONE, null);
        report.transitionTo(ReportStatus.RESOLVED, "ai", ModerationAction.DISMISS, null);

        assertEquals(ReportStatus.RESOLVED, report.getStatus());
        assertEquals(null, report.getActiveReportKey());
    }

    private ModerationReport baseReport() {
        return ModerationReport.builder()
                .id(new ObjectId())
                .reporterId(new ObjectId().toHexString())
                .targetId(new ObjectId().toHexString())
                .targetType(ReportTargetType.POST)
                .targetAuthorId(new ObjectId().toHexString())
                .reason(ReportReason.SPAM)
                .details("Clearly spam")
                .status(ReportStatus.PENDING)
                .actionTaken(ModerationAction.NONE)
                .build();
    }

    private ResolvedReportTarget baseTarget(String authorId) {
        return ResolvedReportTarget.builder()
                .id(new ObjectId().toHexString())
                .authorId(authorId)
                .type(ReportTargetType.POST)
                .content("Normal content")
                .entity(new Object())
                .build();
    }

    private static class TrackingReportActionService extends ReportActionService {
        private int invocations;
        private ModerationAction lastAction;

        TrackingReportActionService() {
            super(null, null, null, null);
        }

        @Override
        public void applyAction(
                ModerationReport report,
                ResolvedReportTarget target,
                ModerationAction action,
                String actor,
                String note) {
            invocations++;
            lastAction = action;
        }
    }

    private static class StubAiModerationHandler extends AiModerationHandler {
        private final String rawResponse;

        StubAiModerationHandler(String rawResponse) {
            this.rawResponse = rawResponse;
        }

        @Override
        protected String invokeWebhook(java.util.Map<String, Object> requestBody, HttpHeaders headers) {
            return rawResponse;
        }

        @Override
        protected boolean isWebhookConfigured() {
            return true;
        }
    }
}
