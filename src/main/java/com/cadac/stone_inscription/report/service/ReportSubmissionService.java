package com.cadac.stone_inscription.report.service;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.kafka.events.report.ReportSubmittedEvent;
import com.cadac.stone_inscription.kafka.producer.ReportEventProducer;
import com.cadac.stone_inscription.report.dto.CreateReportRequest;
import com.cadac.stone_inscription.report.resolver.ResolvedReportTarget;
import com.cadac.stone_inscription.util.UserResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportSubmissionService {

    private static final String KAFKA_SOURCE = "report-module";
    private static final String EVENT_VERSION = "1.0";

    private final ReportService reportService;
    private final ReportEventProducer reportEventProducer;

    public ResponseEntity<?> submitReport(String reporterEmail, CreateReportRequest request) {
        User reporter = reportService.getUserByEmailOrThrow(reporterEmail);
        ResolvedReportTarget target = reportService.validateSubmission(reporter, request);

        ReportSubmittedEvent event = new ReportSubmittedEvent(
                KAFKA_SOURCE,
                EVENT_VERSION,
                reporter.getId().toHexString(),
                target.getId(),
                target.getType().name(),
                request.getReason().name(),
                request.getDetails().trim()
        );

        reportEventProducer.publishReportSubmitted(event);

        return UserResponse.responseHandler(
                "Report submitted for AI moderation",
                HttpStatus.ACCEPTED,
                Map.of(
                        "eventId", event.getEventId(),
                        "targetId", target.getId(),
                        "targetType", target.getType().name(),
                        "status", "QUEUED"
                )
        );
    }
}
