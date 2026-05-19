package com.cadac.stone_inscription.kafka.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.cadac.stone_inscription.kafka.dlt.DLTHandler;
import com.cadac.stone_inscription.kafka.exception.EventPublishException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public abstract class BaseEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DLTHandler                    dltHandler;

    protected BaseEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
                                DLTHandler dltHandler) {
        this.kafkaTemplate = kafkaTemplate;
        this.dltHandler    = dltHandler;
    }

    protected <T> void send(String topic, String key, T event) {
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[PRODUCER] Failed | topic={} key={} reason={}",
                                topic, key, ex.getMessage());
                        dltHandler.handle(topic, key, event, ex);
                        throw new EventPublishException(topic, key,
                                "Failed to publish event", ex);
                    }
                    log.info("[PRODUCER] Success | topic={} key={} offset={}",
                            topic, key,
                            result.getRecordMetadata().offset());
                });
    }
}
