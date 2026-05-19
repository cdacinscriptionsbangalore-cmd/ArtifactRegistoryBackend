package com.cadac.stone_inscription.report;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.annotation.Secured;

import com.cadac.stone_inscription.report.controller.ReportController;

class ReportControllerSecurityTests {

    @Test
    void getReportsAllowsModeratorRoles() throws NoSuchMethodException {
        Method method = ReportController.class.getMethod("getReports", jakarta.servlet.http.HttpServletRequest.class,
                com.cadac.stone_inscription.report.enums.ReportStatus.class);

        Secured secured = method.getAnnotation(Secured.class);

        assertArrayEquals(
                new String[] { "admin", "moderator", "human_moderator", "ai_moderator" },
                secured.value());
    }

    @Test
    void moderateReportAllowsModeratorRoles() throws NoSuchMethodException {
        Method method = ReportController.class.getMethod(
                "moderateReport",
                jakarta.servlet.http.HttpServletRequest.class,
                String.class,
                com.cadac.stone_inscription.report.dto.ModerateReportRequest.class);

        Secured secured = method.getAnnotation(Secured.class);

        assertArrayEquals(
                new String[] { "admin", "moderator", "human_moderator", "ai_moderator" },
                secured.value());
    }
}
