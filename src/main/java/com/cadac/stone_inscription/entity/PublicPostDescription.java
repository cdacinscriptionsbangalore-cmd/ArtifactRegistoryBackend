package com.cadac.stone_inscription.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.cadac.stone_inscription.moderation.model.ContentModeration;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "postdescription")

public class PublicPostDescription {

    @Id
    @JsonProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @Field("postId")
    @JsonProperty("postId")
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId postId;

    @Field("userId")
    @JsonProperty("userId")
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId userId;

    @Field("username")
    @JsonProperty("username")
    private String username;

    @Field("userImageUrl")
    @JsonProperty("userImageUrl")
    private String userImageUrl;

    @Field("description")
    @JsonProperty("description")
    private String description;

    @Field("moderation")
    @JsonProperty("moderation")
    private ContentModeration moderation;

    @Field("upvote")
    @JsonProperty("upvote")
    @Builder.Default
    private Integer upvote = 0;

    @Field("uservote")
    @JsonProperty("userVote")
    @Builder.Default
    private List<UserVote> userVote = new LinkedList<>();

    @CreatedDate
    @Field("createdAt")
    @JsonProperty("createdAt")
    private Date createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    @JsonProperty("updatedAt")
    private Date updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserVote {

        @Field("userId")
        @JsonProperty("userId")
        private String userId;

    }
}
