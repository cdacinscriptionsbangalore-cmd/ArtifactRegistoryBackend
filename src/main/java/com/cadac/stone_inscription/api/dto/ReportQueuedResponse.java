package com.cadac.stone_inscription.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReportQueuedResponse", description = "Payload returned after a report is accepted for asynchronous moderation.")
public class ReportQueuedResponse {

    @Schema(description = "Published report event identifier.", example = "7f8b7d33-16f2-4e84-9f54-8085a9e84791")
    private String eventId;

    @Schema(description = "Identifier of the reported content or user.", example = "665f1df013ad4e18f6a11244")
    private String targetId;

    @Schema(description = "Reported resource type.", example = "POST")
    private String targetType;

    @Schema(description = "Queue processing state.", example = "QUEUED")
    private String status;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
