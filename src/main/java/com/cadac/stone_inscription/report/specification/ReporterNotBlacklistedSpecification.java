package com.cadac.stone_inscription.report.specification;

import org.springframework.stereotype.Component;

@Component
public class ReporterNotBlacklistedSpecification implements Specification<ReportValidationContext> {

    @Override
    public boolean isSatisfiedBy(ReportValidationContext candidate) {
        return candidate.getReporter().getBlackListed() == null || !candidate.getReporter().getBlackListed();
    }

    @Override
    public String errorMessage() {
        return "Blacklisted users cannot file reports.";
    }
}
