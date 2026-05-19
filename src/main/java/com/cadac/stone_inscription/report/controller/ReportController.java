package com.cadac.stone_inscription.report.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cadac.stone_inscription.auth.JwtUtil;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.report.dto.CreateReportRequest;
import com.cadac.stone_inscription.report.dto.ModerateReportRequest;
import com.cadac.stone_inscription.report.enums.ReportStatus;
import com.cadac.stone_inscription.report.service.ReportService;
import com.cadac.stone_inscription.report.service.ReportSubmissionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReportSubmissionService reportSubmissionService;
    private final JwtUtil jwtUtil;

    @PostMapping("/report")
    @Secured({ "user", "admin" })
    public ResponseEntity<?> createReport(
            HttpServletRequest request,
            @Valid @RequestBody CreateReportRequest createReportRequest) {

        return reportSubmissionService.submitReport(extractEmailFromToken(request), createReportRequest);
    }

    @PostMapping("/test/report/{email}")
    public ResponseEntity<?> createReportForTest(
            @PathVariable String email,
            @Valid @RequestBody CreateReportRequest createReportRequest) {

        return reportSubmissionService.submitReport(email, createReportRequest);
    }

    @GetMapping("/reports")
    @Secured({ "admin", "moderator", "human_moderator", "ai_moderator" })
    public ResponseEntity<?> getReports(
            HttpServletRequest request,
            @RequestParam(required = false) ReportStatus status) {

        return reportService.getReports(extractEmailFromToken(request), status);
    }

    @GetMapping("/test/reports/{email}")
    public ResponseEntity<?> getReportsForTest(
            @PathVariable String email,
            @RequestParam(required = false) ReportStatus status) {

        return reportService.getReports(email, status);
    }

    @PostMapping("/moderate/{id}")
    @Secured({ "admin", "moderator", "human_moderator", "ai_moderator" })
    public ResponseEntity<?> moderateReport(
            HttpServletRequest request,
            @PathVariable String id,
            @RequestBody(required = false) ModerateReportRequest moderateReportRequest) {

        return reportService.moderateReport(extractEmailFromToken(request), id, moderateReportRequest);
    }

    @PostMapping("/test/moderate/{id}/{email}")
    public ResponseEntity<?> moderateReportForTest(
            @PathVariable String id,
            @PathVariable String email,
            @RequestBody(required = false) ModerateReportRequest moderateReportRequest) {

        return reportService.moderateReport(email, id, moderateReportRequest);
    }

    private String extractEmailFromToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            throw new StoneInscriptionException("Invalid or missing authorization token", HttpStatus.UNAUTHORIZED);
        }

        return jwtUtil.getUsernameFromToken(token.substring(7));
    }
}
