package com.cadac.stone_inscription.kafka.consumer.base;

import java.util.function.Consumer;

import com.cadac.stone_inscription.kafka.dlt.DLTHandler;
import com.cadac.stone_inscription.kafka.events.base.BaseEvent;
import com.cadac.stone_inscription.kafka.exception.EventConsumeException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseEventConsumer {

    private final DLTHandler dltHandler;

    protected BaseEventConsumer(DLTHandler dltHandler) {
        this.dltHandler = dltHandler;
    }

    protected <T> void consume(String topic,
                               String key,
                               T event,
                               Consumer<T> handler) {
        try {
            log.info("[CONSUMER] Received | topic={} key={}", topic, key);
            handler.accept(event);
            log.info("[CONSUMER] Processed | topic={} key={}", topic, key);
        } catch (Exception ex) {
            log.error("[CONSUMER] Failed | topic={} key={} reason={}",
                    topic, key, ex.getMessage());
            dltHandler.handle(topic, key, event, ex);
            String eventId = event instanceof BaseEvent baseEvent ? baseEvent.getEventId() : key;
            throw new EventConsumeException(topic, eventId, "Failed to consume event", ex);
        }
    }
}
