package com.cadac.stone_inscription.moderation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentModeration {

    @JsonProperty("label")
    private String label;

    @JsonProperty("confidence")
    private Double confidence;

    @JsonProperty("decision")
    private String decision;

    @JsonProperty("status")
    private String status;

    @JsonProperty("reason")
    private String reason;
}
