package com.cadac.stone_inscription.kafka.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.cadac.stone_inscription.kafka.dlt.DLTHandler;
import com.cadac.stone_inscription.kafka.events.report.ReportSubmittedEvent;
import com.cadac.stone_inscription.kafka.registry.TopicRegistry;

@Component
public class ReportEventProducer extends BaseEventProducer {

    public ReportEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
                               DLTHandler dltHandler) {
        super(kafkaTemplate, dltHandler);
    }

    public void publishReportSubmitted(ReportSubmittedEvent event) {
        String key = event.getReporterId()
                + ":" + event.getTargetType()
                + ":" + event.getTargetId();

        send(TopicRegistry.REPORT_SUBMITTED, key, event);
    }
}
