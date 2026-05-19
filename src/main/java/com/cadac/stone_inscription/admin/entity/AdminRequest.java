package com.cadac.stone_inscription.admin.entity;

import java.util.Date;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "admin_requests")
public class AdminRequest {

    @Id
    @JsonProperty("_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @Field("userAuthId")
    @JsonProperty("userAuthId")
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId userAuthId;

    @Field("email")
    @JsonProperty("email")
    @Indexed(unique = true)
    private String email;

    @Field("name")
    @JsonProperty("name")
    private String name;

    @Field("provider")
    @JsonProperty("provider")
    private String provider;

    @Field("status")
    @JsonProperty("status")
    private AdminRequestStatus status;

    @Field("approvalTokenHash")
    private String approvalTokenHash;

    @Field("approvalTokenExpiresAt")
    @JsonProperty("approvalTokenExpiresAt")
    private Date approvalTokenExpiresAt;

    @Field("approvedAt")
    @JsonProperty("approvedAt")
    private Date approvedAt;

    @Field("approvedBy")
    @JsonProperty("approvedBy")
    private String approvedBy;

    @CreatedDate
    @Field("createdAt")
    @JsonProperty("createdAt")
    private Date createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    @JsonProperty("updatedAt")
    private Date updatedAt;
}
