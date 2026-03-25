package com.cadac.stone_inscription.admin.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Admin-facing registry of blacklisted users.
 * A record is created/updated here when a user's reportCount hits the
 * threshold.
 * Allows admin to quickly query all blacklisted users without scanning
 * users_profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "blacklist_users")
public class BlacklistUser {

    @Id
    @JsonProperty("_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    /**
     * Reference to the user in users_profile collection.
     */
    @Field("userId")
    @JsonProperty("userId")
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId userId;

    @Field("name")
    @JsonProperty("name")
    private String name;

    /**
     * Total number of admin-validated reports against this user.
     * Synced from User.reportCount at time of blacklisting.
     */
    @Field("reportCount")
    @JsonProperty("reportCount")
    @Builder.Default
    private Integer reportCount = 0;

    @Field("blackListed")
    @JsonProperty("blackListed")
    @Builder.Default
    private Boolean blackListed = true;
}
