package com.cadac.stone_inscription.report.moderation;

import java.util.EnumSet;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.report.entity.ModerationReport;
import com.cadac.stone_inscription.report.enums.ModerationAction;
import com.cadac.stone_inscription.report.enums.ReportStatus;

@Component
public class HumanModerationHandler extends ModerationHandler {

    private static final EnumSet<ModerationAction> ALLOWED_ACTIONS = EnumSet.of(
            ModerationAction.WARN,
            ModerationAction.REMOVE_CONTENT,
            ModerationAction.BAN_REPORTER,
            ModerationAction.BAN_AUTHOR,
            ModerationAction.DISMISS);

    @Override
    public void handle(ModerationExecutionContext context) {
        ModerationReport report = context.getReport();
        if (report.getStatus() != ReportStatus.ESCALATED) {
            passToNext(context);
            return;
        }

        if (context.getActor() != null
                && context.getActor().getId() != null
                && context.getActor().getId().toHexString().equals(context.getTarget().getAuthorId())) {
            throw new StoneInscriptionException(
                    "A moderator cannot moderate their own content.",
                    HttpStatus.BAD_REQUEST);
        }

        if (context.getRequestedAction() == null || !ALLOWED_ACTIONS.contains(context.getRequestedAction())) {
            throw new StoneInscriptionException(
                    "A valid moderation action is required for escalated reports.",
                    HttpStatus.BAD_REQUEST);
        }

        context.getReportActionService().applyAction(
                report,
                context.getTarget(),
                context.getRequestedAction(),
                context.getActorLabel(),
                context.getNote());

        report.transitionTo(ReportStatus.RESOLVED, context.getActorLabel(), context.getRequestedAction(), context.getNote());
    }
}
