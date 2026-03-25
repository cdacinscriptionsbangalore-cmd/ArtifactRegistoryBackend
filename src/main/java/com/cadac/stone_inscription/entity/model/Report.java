package com.cadac.stone_inscription.entity.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.LinkedList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    /**
     * Incremented only when admin validates a report as spam/abuse/violation.
     * Not incremented on every report submission.
     */
    @Field("count")
    @JsonProperty("count")
    @Builder.Default
    private Integer count = 0;

    /**
     * List of all reporters with their reason.
     * Admin reviews this list to decide on validation.
     */
    @Field("reporters")
    @JsonProperty("reporters")
    @Builder.Default
    private List<ReportEntry> reporters = new LinkedList<>();
}
