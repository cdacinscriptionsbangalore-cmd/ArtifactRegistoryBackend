package com.cadac.stone_inscription.moderation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "ContentModerationResponse", description = "Normalized moderation webhook response.")
public class ContentModerationResponseDto {

    @JsonProperty("timestamp")
    @Schema(description = "Webhook timestamp.", example = "2026-05-19T10:35:00Z")
    private String timestamp;

    @JsonProperty("decision")
    @JsonAlias({ "verdict", "action" })
    @Schema(description = "Moderation decision.", example = "ALLOW")
    private String decision;

    @JsonProperty("label")
    @JsonAlias({ "category", "classification" })
    @Schema(description = "Moderation category or label.", example = "safe")
    private String label;

    @JsonProperty("confidence")
    @JsonAlias({ "score", "confidenceScore", "confidence_score", "probability" })
    @Schema(description = "Confidence score from 0 to 1 when supplied.", example = "0.91")
    private Double confidence;

    @JsonProperty("reason")
    @JsonAlias({ "message", "explanation" })
    @Schema(description = "Human-readable moderation reason.", example = "No unsafe content detected")
    private String reason;

    @JsonProperty("status")
    @JsonAlias({ "state" })
    @Schema(description = "Webhook processing status.", example = "APPROVED")
    private String status;

    @JsonProperty("description")
    @Schema(description = "Moderated content description returned by the workflow.")
    private String description;

    @JsonProperty("id")
    @Schema(description = "Webhook-side moderation id.", example = "5006")
    private Long id;

    @JsonProperty("createdAt")
    @Schema(description = "Webhook record creation timestamp.", example = "2026-05-19T10:35:00Z")
    private String createdAt;

    @JsonProperty("updatedAt")
    @Schema(description = "Webhook record update timestamp.", example = "2026-05-19T10:35:01Z")
    private String updatedAt;
}
