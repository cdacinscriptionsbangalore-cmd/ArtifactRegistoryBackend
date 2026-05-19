package com.cadac.stone_inscription.post.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "InscriptionPost", description = "Post metadata submitted with one or more inscription images in multipart requests.")
public class InscriptionPostDto {

    @JsonProperty("description")
    @Schema(description = "Localized inscription description and language metadata.")
    private DescriptionDto description;

    @JsonProperty("topic")
    @Schema(description = "High-level post topic.", example = "Temple inscription")
    private String topic;

    @JsonProperty("script")
    @ArraySchema(schema = @Schema(description = "Script family used in the inscription.", example = "Brahmi"))
    private List<String> script;

    @JsonProperty("type")
    @Schema(description = "Content type or category.", example = "STONE_INSCRIPTION")
    private String type;

    @JsonProperty("visiblity")
    @Schema(description = "Whether the post should be visible publicly. Field name preserves the existing API spelling.", example = "true")
    private Boolean visiblity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "InscriptionPostDescription", description = "Human-readable inscription description payload.")
    public static class DescriptionDto {

        @JsonProperty("title")
        @Schema(description = "Display title for the inscription.", example = "Ashokan pillar fragment")
        private String title;

        @JsonProperty("subject")
        @Schema(description = "Primary subject of the inscription.", example = "Donation record")
        private String subject;

        @JsonProperty("description")
        @Schema(description = "Detailed inscription description.", example = "Fragmentary stone inscription found near the temple entrance.")
        private String description;

        @JsonProperty("scriptLanguage")
        @ArraySchema(schema = @Schema(description = "Script language.", example = "Prakrit"))
        private List<String> scriptLanguage;

        @JsonProperty("language")
        @ArraySchema(schema = @Schema(description = "Readable language.", example = "Hindi"))
        private List<String> language;

    }

}
