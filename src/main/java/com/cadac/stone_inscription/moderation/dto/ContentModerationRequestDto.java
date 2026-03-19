package com.cadac.stone_inscription.moderation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentModerationRequestDto {

    @JsonProperty("title")
    private String title;

    @JsonProperty("topic")
    private String topic;

    @JsonProperty("description")
    private String description;
}
