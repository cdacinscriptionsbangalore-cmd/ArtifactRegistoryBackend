package com.cadac.stone_inscription.kafka.events.report;

import com.cadac.stone_inscription.kafka.events.base.BaseEvent;

public class ReportSubmittedEvent extends BaseEvent {
    private String reporterId;
    private String targetType;
    private String targetId;
    private String reason;
    private String description;

    public ReportSubmittedEvent() {
        super();
    }

    public ReportSubmittedEvent(String source,
                                String version,
                                String reporterId,
                                String targetId,
                                String targetType,
                                String reason,
                                String description) {
        super(source, version);
        this.reporterId = reporterId;
        this.reason = reason;
        this.targetType = targetType;
        this.description = description;
        this.targetId = targetId;
    }

    public String getReporterId() { return reporterId; }
    public String getReason() { return reason; }
    public String getTargetId() { return targetId; }
    public String getTargetType() { return targetType; }
    public String getDescription() { return description; }
}
