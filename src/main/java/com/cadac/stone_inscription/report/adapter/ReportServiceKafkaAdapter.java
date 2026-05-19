package com.cadac.stone_inscription.report.adapter;

import org.springframework.stereotype.Component;

import com.cadac.stone_inscription.kafka.events.report.ReportSubmittedEvent;
import com.cadac.stone_inscription.kafka.port.ReportServicePort;
import com.cadac.stone_inscription.report.service.ReportService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportServiceKafkaAdapter implements ReportServicePort {

    private final ReportService reportService;

    @Override
    public void createReport(ReportSubmittedEvent event) {
        reportService.runAiModerationForSubmittedEvent(event);
    }
}
