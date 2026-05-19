package com.cadac.stone_inscription.report.specification;

import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.report.repository.ModerationReportRepository;
import com.cadac.stone_inscription.report.resolver.ResolvedReportTarget;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportValidationContext {

    private final User reporter;
    private final ResolvedReportTarget target;
    private final ModerationReportRepository reportRepository;
}
