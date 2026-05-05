package com.cadac.stone_inscription.report.specification;

import org.springframework.stereotype.Component;

@Component
public class NotSelfReportSpecification implements Specification<ReportValidationContext> {

    @Override
    public boolean isSatisfiedBy(ReportValidationContext candidate) {
        return !candidate.getReporter().getId().toHexString().equals(candidate.getTarget().getAuthorId());
    }

    @Override
    public String errorMessage() {
        return "A user cannot report their own content.";
    }
}
