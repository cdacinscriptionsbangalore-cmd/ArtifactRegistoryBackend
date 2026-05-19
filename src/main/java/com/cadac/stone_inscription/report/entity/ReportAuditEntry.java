package com.cadac.stone_inscription.report.entity;

import java.util.Date;

import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportAuditEntry {

    @Field("actor")
    @JsonProperty("actor")
    private String actor;

    @Field("message")
    @JsonProperty("message")
    private String message;

    @Field("createdAt")
    @JsonProperty("createdAt")
    private Date createdAt;
}
