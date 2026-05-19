package com.cadac.stone_inscription.entity.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportEntry {

    @Field("userId")
    @JsonProperty("userId")
    private String userId;

    @Field("name")
    @JsonProperty("name")
    private String name;

    @Field("reason")
    @JsonProperty("reason")
    private String reason;
}
