package com.cadac.stone_inscription.post.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class InscriptionPostDto {


    @JsonProperty("description")
    private DescriptionDto description;

    @JsonProperty("topic")
    private String topic;

    @JsonProperty("script")
    private List<String> script;

    @JsonProperty("type")
    private String type;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DescriptionDto {
        
        @JsonProperty("title")
        private String title;

        @JsonProperty("subject")
        private String subject;

        @JsonProperty("description")
        private String description;

        @JsonProperty("scriptLanguage")
        private List<String> scriptLanguage;

        @JsonProperty("language")
        private List<String> language;

    }


}

