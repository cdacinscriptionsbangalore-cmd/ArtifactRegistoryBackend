package com.cadac.stone_inscription.kafka.exception;

public class EventConsumeException extends RuntimeException {
    private final String topic;
    private final String eventId;

    public EventConsumeException(String topic, String eventId, String message, Throwable cause) {
        super(message, cause);
        this.topic = topic;
        this.eventId = eventId;
    }

    public String getTopic() {
        return topic;
    }

    public String getEventId() {
        return eventId;
    }
}
