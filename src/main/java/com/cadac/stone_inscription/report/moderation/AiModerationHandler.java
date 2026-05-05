package com.cadac.stone_inscription.report.moderation;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.cadac.stone_inscription.report.entity.ModerationReport;
import com.cadac.stone_inscription.report.enums.ModerationAction;
import com.cadac.stone_inscription.report.enums.ReportReason;
import com.cadac.stone_inscription.report.enums.ReportStatus;

@Component
public class AiModerationHandler extends ModerationHandler {

    private static final String AI_ACTOR = "AI_MODERATOR";
    private static final double AUTO_RESOLVE_THRESHOLD = 0.85;

    @Override
    public void handle(ModerationExecutionContext context) {
        ModerationReport report = context.getReport();
        if (report.getStatus() != ReportStatus.PENDING) {
            passToNext(context);
            return;
        }

        report.transitionTo(ReportStatus.AI_SCREENING, AI_ACTOR, ModerationAction.NONE, context.getNote());

        double score = computeConfidenceScore(context);
        report.setAiConfidenceScore(score, AI_ACTOR);

        if (score >= AUTO_RESOLVE_THRESHOLD) {
            ModerationAction action = determineAutoAction(report.getReason());
            context.getReportActionService().applyAction(report, context.getTarget(), action, AI_ACTOR, context.getNote());
            report.transitionTo(ReportStatus.RESOLVED, AI_ACTOR, action, context.getNote());
            return;
        }

        report.transitionTo(ReportStatus.ESCALATED, AI_ACTOR, ModerationAction.ESCALATE, context.getNote());
    }

    private double computeConfidenceScore(ModerationExecutionContext context) {
        ModerationReport report = context.getReport();
        String combinedText = (context.getTarget().getContent() + " " + report.getDetails()).toLowerCase(Locale.ROOT);

        double base = switch (report.getReason()) {
            case HATE_SPEECH -> 0.80;
            case EXPLICIT_CONTENT -> 0.75;
            case HARASSMENT -> 0.70;
            case SPAM -> 0.65;
            case MISINFORMATION -> 0.55;
            case OTHER -> 0.30;
        };

        List<String> flaggedTerms = List.of("hate", "spam", "scam", "fake", "explicit", "kill", "abuse");
        boolean hasFlaggedTerms = flaggedTerms.stream().anyMatch(combinedText::contains);

        if (hasFlaggedTerms) {
            base += 0.20;
        }

        return Math.min(base, 1.0);
    }

    private ModerationAction determineAutoAction(ReportReason reason) {
        return switch (reason) {
            case SPAM, EXPLICIT_CONTENT, HARASSMENT, HATE_SPEECH, MISINFORMATION -> ModerationAction.REMOVE_CONTENT;
            case OTHER -> ModerationAction.ESCALATE;
        };
    }
}
