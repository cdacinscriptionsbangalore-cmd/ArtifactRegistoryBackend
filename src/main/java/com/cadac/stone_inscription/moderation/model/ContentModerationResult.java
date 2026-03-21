package com.cadac.stone_inscription.moderation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentModerationResult {

    private boolean approved;

    private String label;

    private Double confidence;

    private String decision;

    private String status;

    private String reason;

    public ContentModeration toContentModeration() {
        return ContentModeration.builder()
                .label(label)
                .confidence(confidence)
                .decision(decision)
                .status(status)
                .reason(reason)
                .build();
    }
}
