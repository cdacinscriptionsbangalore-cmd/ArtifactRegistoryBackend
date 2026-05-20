package com.cadac.stone_inscription.moderation.dto;

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
@Schema(name = "ContentModerationRequest", description = "Text fields sent to the content moderation workflow.")
public class ContentModerationRequestDto {

    @JsonProperty("title")
    @Schema(description = "Content title.", example = "Ashokan pillar fragment")
    private String title;

    @JsonProperty("topic")
    @Schema(description = "Content topic.", example = "Temple inscription")
    private String topic;

    @JsonProperty("description")
    @Schema(description = "Content body to moderate.", example = "Fragmentary stone inscription found near the temple entrance.")
    private String description;
}
