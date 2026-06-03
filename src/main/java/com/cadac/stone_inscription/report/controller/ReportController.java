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

import com.cadac.stone_inscription.api.dto.ApiErrorResponse;
import com.cadac.stone_inscription.api.dto.ApiSuccessResponse;
import com.cadac.stone_inscription.api.dto.ReportQueuedResponse;
import com.cadac.stone_inscription.auth.JwtUtil;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.report.dto.CreateReportRequest;
import com.cadac.stone_inscription.report.dto.ModerateReportRequest;
import com.cadac.stone_inscription.report.entity.ModerationReport;
import com.cadac.stone_inscription.report.enums.ReportStatus;
import com.cadac.stone_inscription.report.service.ReportService;
import com.cadac.stone_inscription.report.service.ReportSubmissionService;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reports", description = "User reporting and moderator review workflow.")
public class ReportController {

    private final ReportService reportService;
    private final ReportSubmissionService reportSubmissionService;
    private final JwtUtil jwtUtil;

    @PostMapping("/report")
    @Secured({ "user", "admin" })
    @Operation(
            summary = "Submit report",
            description = "Accepts a report from the authenticated user, validates target/reporting rules, and queues the report for AI moderation.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateReportRequest.class),
                            examples = @ExampleObject(value = "{\"targetType\":\"POST\",\"targetId\":\"665f1df013ad4e18f6a11244\",\"reason\":\"MISINFORMATION\",\"details\":\"The description attributes the inscription to the wrong dynasty.\"}"))),
            responses = {
                    @ApiResponse(responseCode = "202", description = "Report queued",
                            content = @Content(schema = @Schema(implementation = ReportQueuedResponse.class),
                                    examples = @ExampleObject(value = "{\"message\":\"Report submitted for AI moderation\",\"http-status\":\"ACCEPTED\",\"data\":{\"eventId\":\"7f8b7d33-16f2-4e84-9f54-8085a9e84791\",\"targetId\":\"665f1df013ad4e18f6a11244\",\"targetType\":\"POST\",\"status\":\"QUEUED\"}}"))),
                    @ApiResponse(responseCode = "400", description = "Invalid target, duplicate report, or self-report",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<?> createReport(
            HttpServletRequest request,
            @Valid @RequestBody CreateReportRequest createReportRequest) {

        return reportSubmissionService.submitReport(extractEmailFromToken(request), createReportRequest);
    }

    @PostMapping("/test/report/{email}")
    @Hidden
    public ResponseEntity<?> createReportForTest(
            @PathVariable String email,
            @Valid @RequestBody CreateReportRequest createReportRequest) {

        return reportSubmissionService.submitReport(email, createReportRequest);
    }

    @GetMapping("/reports")
    @Secured({ "admin", "moderator", "human_moderator", "ai_moderator" })
    @Operation(
            summary = "List moderation reports",
            description = "Returns moderation reports ordered by creation time. Moderators may optionally filter by status.",
            responses = @ApiResponse(responseCode = "200", description = "Reports fetched",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ModerationReport.class)))))
    public ResponseEntity<?> getReports(
            HttpServletRequest request,
            @Parameter(description = "Optional report status filter.", example = "ESCALATED")
            @RequestParam(required = false) ReportStatus status) {

        return reportService.getReports(extractEmailFromToken(request), status);
    }

    @GetMapping("/test/reports/{email}")
    @Hidden
    public ResponseEntity<?> getReportsForTest(
            @PathVariable String email,
            @RequestParam(required = false) ReportStatus status) {

        return reportService.getReports(email, status);
    }

    @PostMapping("/moderate/{id}")
    @Secured({ "admin", "moderator", "human_moderator", "ai_moderator" })
    @Operation(
            summary = "Moderate report",
            description = "Runs the moderation chain for a report. Human moderator roles are required when an escalated report needs final resolution.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = false,
                    content = @Content(schema = @Schema(implementation = ModerateReportRequest.class),
                            examples = @ExampleObject(value = "{\"action\":\"REMOVE_CONTENT\",\"note\":\"Removed after human review.\"}"))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Report moderation completed",
                            content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Report not found",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<?> moderateReport(
            HttpServletRequest request,
            @PathVariable String id,
            @Valid @RequestBody(required = false) ModerateReportRequest moderateReportRequest) {

        return reportService.moderateReport(extractEmailFromToken(request), id, moderateReportRequest);
    }

    @PostMapping("/test/moderate/{id}/{email}")
    @Hidden
    public ResponseEntity<?> moderateReportForTest(
            @PathVariable String id,
            @PathVariable String email,
            @Valid @RequestBody(required = false) ModerateReportRequest moderateReportRequest) {

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
