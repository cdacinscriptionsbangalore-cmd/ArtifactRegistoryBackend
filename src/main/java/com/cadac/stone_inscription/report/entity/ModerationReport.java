package com.cadac.stone_inscription.report.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.http.HttpStatus;

import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.report.enums.ModerationAction;
import com.cadac.stone_inscription.report.enums.ReportReason;
import com.cadac.stone_inscription.report.enums.ReportStatus;
import com.cadac.stone_inscription.report.enums.ReportTargetType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "moderation_reports")
@CompoundIndexes({
        @CompoundIndex(name = "report_target_idx", def = "{'targetId': 1, 'targetType': 1}"),
        @CompoundIndex(name = "reporter_target_idx", def = "{'reporterId': 1, 'targetId': 1, 'targetType': 1}")
})
@Schema(name = "ModerationReport", description = "Moderation report state, target metadata, AI score, and audit history.")
public class ModerationReport {

    @Id
    @JsonProperty("_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "Report identifier.", example = "665f1df013ad4e18f6a11250")
    private ObjectId id;

    @Field("reporterId")
    @JsonProperty("reporterId")
    @Indexed
    @Schema(description = "Reporter user id.", example = "665f1df013ad4e18f6a11240")
    private String reporterId;

    @Field("targetId")
    @JsonProperty("targetId")
    @Indexed
    @Schema(description = "Reported target id.", example = "665f1df013ad4e18f6a11244")
    private String targetId;

    @Field("targetType")
    @JsonProperty("targetType")
    @Schema(description = "Reported target type.", example = "POST")
    private ReportTargetType targetType;

    @Field("targetAuthorId")
    @JsonProperty("targetAuthorId")
    @Schema(description = "Author id of the reported target.", example = "665f1df013ad4e18f6a11241")
    private String targetAuthorId;

    @Field("reason")
    @JsonProperty("reason")
    @Schema(description = "Reporter-selected reason.", example = "MISINFORMATION")
    private ReportReason reason;

    @Field("details")
    @JsonProperty("details")
    @Schema(description = "Reporter-provided details.", example = "The inscription description contains misleading attribution.")
    private String details;

    @Field("status")
    @JsonProperty("status")
    @Indexed
    @Schema(description = "Current moderation workflow status.", example = "ESCALATED")
    private ReportStatus status;

    @Field("activeReportKey")
    @JsonProperty("activeReportKey")
    @Indexed(unique = true, sparse = true)
    private String activeReportKey;

    @Field("actionTaken")
    @JsonProperty("actionTaken")
    @Builder.Default
    @Schema(description = "Action applied during moderation.", example = "REMOVE_CONTENT")
    private ModerationAction actionTaken = ModerationAction.NONE;

    @Field("resolvedBy")
    @JsonProperty("resolvedBy")
    @Schema(description = "Actor that resolved the report.", example = "moderator@example.com")
    private String resolvedBy;

    @Field("aiConfidenceScore")
    @JsonProperty("aiConfidenceScore")
    @Builder.Default
    @Schema(description = "AI confidence score from 0 to 1.", example = "0.87")
    private Double aiConfidenceScore = 0.0;

    @CreatedDate
    @Field("createdAt")
    @JsonProperty("createdAt")
    private Date createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    @JsonProperty("updatedAt")
    private Date updatedAt;

    @Field("resolvedAt")
    @JsonProperty("resolvedAt")
    private Date resolvedAt;

    @Version
    @Field("version")
    @JsonProperty("version")
    private Long version;

    @Field("auditEntries")
    @JsonProperty("auditEntries")
    @Builder.Default
    @ArraySchema(schema = @Schema(implementation = ReportAuditEntry.class))
    private List<ReportAuditEntry> auditEntries = new ArrayList<>();

    public static String buildActiveReportKey(String reporterId, String targetId, ReportTargetType targetType) {
        return reporterId + ":" + targetType + ":" + targetId;
    }

    public void addAuditEntry(String actor, String message) {
        auditEntries.add(ReportAuditEntry.builder()
                .actor(actor)
                .message(message)
                .createdAt(new Date())
                .build());
    }

    public void setAiConfidenceScore(double score, String actor) {
        this.aiConfidenceScore = score;
        addAuditEntry(actor, String.format("AI confidence score set to %.2f", score));
    }

    public void transitionTo(ReportStatus newStatus, String actor, ModerationAction action, String note) {
        validateTransition(this.status, newStatus);
        this.status = newStatus;
        this.actionTaken = action;
        this.resolvedBy = actor;

        if (newStatus == ReportStatus.RESOLVED) {
            this.resolvedAt = new Date();
            this.activeReportKey = null;
        } else if (this.activeReportKey == null && reporterId != null && targetId != null && targetType != null) {
            this.activeReportKey = buildActiveReportKey(reporterId, targetId, targetType);
        }

        StringBuilder builder = new StringBuilder()
                .append("Status -> ").append(newStatus)
                .append(" | Action -> ").append(action)
                .append(" | By -> ").append(actor);
        if (note != null && !note.isBlank()) {
            builder.append(" | Note -> ").append(note.trim());
        }
        addAuditEntry(actor, builder.toString());
    }

    private void validateTransition(ReportStatus from, ReportStatus to) {
        if (from == null) {
            return;
        }

        Map<ReportStatus, Set<ReportStatus>> allowedTransitions = new EnumMap<>(ReportStatus.class);
        allowedTransitions.put(ReportStatus.PENDING, Set.of(ReportStatus.AI_SCREENING));
        allowedTransitions.put(ReportStatus.AI_SCREENING, Set.of(ReportStatus.ESCALATED, ReportStatus.RESOLVED));
        allowedTransitions.put(ReportStatus.ESCALATED, Set.of(ReportStatus.RESOLVED));
        allowedTransitions.put(ReportStatus.RESOLVED, Set.of());

        if (!allowedTransitions.getOrDefault(from, Set.of()).contains(to)) {
            throw new StoneInscriptionException(
                    "Invalid report status transition: " + from + " -> " + to,
                    HttpStatus.BAD_REQUEST);
        }
    }
}
