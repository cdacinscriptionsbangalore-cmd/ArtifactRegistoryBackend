package com.cadac.stone_inscription.moderation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentModerationResponseDto {

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("decision")
    @JsonAlias({ "verdict", "action" })
    private String decision;

    @JsonProperty("label")
    @JsonAlias({ "category", "classification" })
    private String label;

    @JsonProperty("confidence")
    @JsonAlias({ "score", "confidenceScore", "confidence_score", "probability" })
    private Double confidence;

    @JsonProperty("reason")
    @JsonAlias({ "message", "explanation" })
    private String reason;

    @JsonProperty("status")
    @JsonAlias({ "state" })
    private String status;

    @JsonProperty("description")
    private String description;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedAt")
    private String updatedAt;
}
