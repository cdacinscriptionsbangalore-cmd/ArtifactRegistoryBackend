package com.cadac.stone_inscription.report.specification;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cadac.stone_inscription.report.enums.ReportStatus;

@Component
public class NotDuplicateReportSpecification implements Specification<ReportValidationContext> {

    @Override
    public boolean isSatisfiedBy(ReportValidationContext candidate) {
        return !candidate.getReportRepository().existsByReporterIdAndTargetIdAndTargetTypeAndStatusIn(
                candidate.getReporter().getId().toHexString(),
                candidate.getTarget().getId(),
                candidate.getTarget().getType(),
                List.of(ReportStatus.PENDING, ReportStatus.AI_SCREENING, ReportStatus.ESCALATED));
    }

    @Override
    public String errorMessage() {
        return "You have already reported this target.";
    }
}
