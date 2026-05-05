package com.cadac.stone_inscription.report.specification;

import org.springframework.stereotype.Component;

import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.report.enums.ReportTargetType;

@Component
public class TargetNotBlacklistedSpecification implements Specification<ReportValidationContext> {

    @Override
    public boolean isSatisfiedBy(ReportValidationContext candidate) {
        if (candidate.getTarget().getType() != ReportTargetType.USER) {
            return true;
        }

        User user = (User) candidate.getTarget().getEntity();
        return user.getBlackListed() == null || !user.getBlackListed();
    }

    @Override
    public String errorMessage() {
        return "This user is already blacklisted.";
    }
}
