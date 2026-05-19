package com.cadac.stone_inscription.kafka.events.base;

import java.time.Instant;
import java.util.UUID;

public abstract class BaseEvent {

    private String eventId;
    private String source;
    private String version;
    private Instant timestamp;

    protected BaseEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    protected BaseEvent(String source, String version) {
        this();
        this.source = source;
        this.version = version;
    }

    public String getEventId() {
        return eventId;
    }

    public String getSource() {
        return source;
    }

    public String getVersion() {
        return version;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
