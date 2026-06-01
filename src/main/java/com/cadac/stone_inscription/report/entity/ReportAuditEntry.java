package com.cadac.stone_inscription.report.entity;

import java.util.Date;

import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ReportAuditEntry", description = "Single audit trail entry recorded during report moderation.")
public class ReportAuditEntry {

    @Field("actor")
    @JsonProperty("actor")
    @Schema(description = "Actor email or system actor name.", example = "moderator@example.com")
    private String actor;

    @Field("message")
    @JsonProperty("message")
    @Schema(description = "Audit message.", example = "Status -> RESOLVED | Action -> REMOVE_CONTENT | By -> moderator@example.com")
    private String message;

    @Field("createdAt")
    @JsonProperty("createdAt")
    @Schema(description = "Audit creation timestamp.", example = "2026-05-19T10:35:00.000+00:00")
    private Date createdAt;
}
