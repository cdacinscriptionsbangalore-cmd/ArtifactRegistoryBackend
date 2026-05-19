package com.cadac.stone_inscription.kafka.dlt;

import java.time.Instant;

import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DLTHandler {

    private final DLTRepository dltRepository;
    private final ObjectMapper  objectMapper;

    public DLTHandler(DLTRepository dltRepository, 
                      ObjectMapper objectMapper) {
        this.dltRepository = dltRepository;
        this.objectMapper  = objectMapper;
    }

    public void handle(String topic,
                       String key,
                       Object payload,
                       Throwable cause) {

        String serialized = serialize(payload);

        DLTEvent dltEvent = new DLTEvent(
                topic,
                key,
                serialized,
                cause.getMessage(),
                Instant.now()
        );

        dltRepository.save(dltEvent);

        log.error("[DLT] Failed event stored | topic={} key={} reason={} timestamp={}",
                topic, key, cause.getMessage(), dltEvent.getTimestamp());
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return payload.toString();
        }
    }
}
