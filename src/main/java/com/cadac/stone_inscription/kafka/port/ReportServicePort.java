package com.cadac.stone_inscription.kafka.port;
import com.cadac.stone_inscription.kafka.events.report.ReportSubmittedEvent;

public interface ReportServicePort {
    void createReport(ReportSubmittedEvent event);
}