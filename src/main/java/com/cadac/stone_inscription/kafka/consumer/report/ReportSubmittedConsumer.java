package com.cadac.stone_inscription.kafka.consumer.report;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.cadac.stone_inscription.kafka.consumer.base.BaseEventConsumer;
import com.cadac.stone_inscription.kafka.dlt.DLTHandler;
import com.cadac.stone_inscription.kafka.events.report.ReportSubmittedEvent;
import com.cadac.stone_inscription.kafka.port.ReportServicePort;
import com.cadac.stone_inscription.kafka.registry.TopicRegistry;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ReportSubmittedConsumer extends BaseEventConsumer {

    private final ReportServicePort reportServicePort;

    public ReportSubmittedConsumer(DLTHandler dltHandler,
                                   ReportServicePort reportServicePort) {
        super(dltHandler);
        this.reportServicePort = reportServicePort;
    }

    @KafkaListener(
            topics   = TopicRegistry.REPORT_SUBMITTED,
            groupId  = "report-submitted-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onReportSubmitted(@Payload ReportSubmittedEvent event,
                                  @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        consume(
                TopicRegistry.REPORT_SUBMITTED,
                key,
                event,
                reportServicePort::createReport
        );
    }
}
