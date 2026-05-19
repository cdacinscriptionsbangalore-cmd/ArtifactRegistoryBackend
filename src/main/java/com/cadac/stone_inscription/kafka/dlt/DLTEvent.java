package com.cadac.stone_inscription.kafka.dlt;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "dead_letter_logs")
public class DLTEvent {
    @Id
    private String id;
    private String topic;
    private String key;
    private String payload;
    private String failureReason;
    private Instant timestamp;

    public DLTEvent() {
    }

    public DLTEvent(String topic,
                    String key,
                    String payload,
                    String failureReason,
                    Instant timestamp) {
        this.topic = topic;
        this.key = key;
        this.payload = payload;
        this.failureReason = failureReason;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getTopic() { return topic; }
    public String getKey() { return key; }
    public String getPayload() { return payload; }
    public String getFailureReason() { return failureReason; }
    public Instant getTimestamp() { return timestamp; }
}
