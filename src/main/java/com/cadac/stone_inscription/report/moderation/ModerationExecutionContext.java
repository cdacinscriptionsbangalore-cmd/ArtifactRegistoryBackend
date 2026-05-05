package com.cadac.stone_inscription.report.moderation;

import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.report.entity.ModerationReport;
import com.cadac.stone_inscription.report.enums.ModerationAction;
import com.cadac.stone_inscription.report.resolver.ResolvedReportTarget;
import com.cadac.stone_inscription.report.service.ReportActionService;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ModerationExecutionContext {

    private final ModerationReport report;
    private final ResolvedReportTarget target;
    private final User actor;
    private final String actorLabel;
    private final ModerationAction requestedAction;
    private final String note;
    private final ReportActionService reportActionService;
}
