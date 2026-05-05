package com.cadac.stone_inscription.report;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Collection;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.report.enums.ReportTargetType;
import com.cadac.stone_inscription.report.repository.ModerationReportRepository;
import com.cadac.stone_inscription.report.resolver.ResolvedReportTarget;
import com.cadac.stone_inscription.report.specification.NotDuplicateReportSpecification;
import com.cadac.stone_inscription.report.specification.NotSelfReportSpecification;
import com.cadac.stone_inscription.report.specification.ReportValidationContext;
import com.cadac.stone_inscription.report.specification.ReporterNotBlacklistedSpecification;

class ReportSpecificationTests {

    @Test
    void notSelfReportSpecRejectsOwnContent() {
        ObjectId userId = new ObjectId();
        ReportValidationContext context = baseContext(
                User.builder().id(userId).blackListed(false).build(),
                target(userId.toHexString()),
                repositoryReturning(false));

        assertFalse(new NotSelfReportSpecification().isSatisfiedBy(context));
    }

    @Test
    void notDuplicateReportSpecRejectsOpenDuplicate() {
        User reporter = User.builder().id(new ObjectId()).blackListed(false).build();
        ReportValidationContext context = baseContext(
                reporter,
                target(new ObjectId().toHexString()),
                repositoryReturning(true));

        assertFalse(new NotDuplicateReportSpecification().isSatisfiedBy(context));
    }

    @Test
    void reporterNotBlacklistedSpecRejectsBlacklistedReporter() {
        ReportValidationContext context = baseContext(
                User.builder().id(new ObjectId()).blackListed(true).build(),
                target(new ObjectId().toHexString()),
                repositoryReturning(false));

        assertFalse(new ReporterNotBlacklistedSpecification().isSatisfiedBy(context));
    }

    @Test
    void notDuplicateReportSpecAllowsResolvedDuplicate() {
        User reporter = User.builder().id(new ObjectId()).blackListed(false).build();
        ReportValidationContext context = baseContext(
                reporter,
                target(new ObjectId().toHexString()),
                repositoryReturning(false));

        assertTrue(new NotDuplicateReportSpecification().isSatisfiedBy(context));
    }

    private ReportValidationContext baseContext(
            User reporter,
            ResolvedReportTarget target,
            ModerationReportRepository repository) {
        return ReportValidationContext.builder()
                .reporter(reporter)
                .target(target)
                .reportRepository(repository)
                .build();
    }

    private ResolvedReportTarget target(String authorId) {
        return ResolvedReportTarget.builder()
                .id(new ObjectId().toHexString())
                .authorId(authorId)
                .type(ReportTargetType.POST)
                .content("target content")
                .entity(new Object())
                .build();
    }

    private ModerationReportRepository repositoryReturning(boolean duplicateExists) {
        return (ModerationReportRepository) Proxy.newProxyInstance(
                ModerationReportRepository.class.getClassLoader(),
                new Class<?>[] { ModerationReportRepository.class },
                (proxy, method, args) -> {
                    if ("existsByReporterIdAndTargetIdAndTargetTypeAndStatusIn".equals(method.getName())) {
                        return duplicateExists;
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("toString".equals(method.getName())) {
                        return "ModerationReportRepositoryProxy";
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType.equals(boolean.class)) {
                        return false;
                    }
                    if (returnType.equals(int.class)) {
                        return 0;
                    }
                    if (returnType.equals(long.class)) {
                        return 0L;
                    }
                    if (Collection.class.isAssignableFrom(returnType)) {
                        return java.util.List.of();
                    }
                    return null;
                });
    }
}
