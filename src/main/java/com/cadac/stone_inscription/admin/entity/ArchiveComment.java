package com.cadac.stone_inscription.admin.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.cadac.stone_inscription.entity.enums.PostStatus;
import com.cadac.stone_inscription.entity.model.Report;
import com.cadac.stone_inscription.moderation.model.ContentModeration;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/**
 * Archive of rejected comments.
 * A comment is moved here from postdescription when admin rejects it.
 * Preserves the full original comment structure for audit/record purposes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "archive_comments")
public class ArchiveComment {

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

    @CreatedDate
    @Field("createdAt")
    @JsonProperty("createdAt")
    private Date createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    @JsonProperty("updatedAt")
    private Date updatedAt;

    @Field("status")
    @JsonProperty("status")
    @Builder.Default
    private PostStatus status = PostStatus.REJECTED;

    @Field("report")
    @JsonProperty("report")
    @Builder.Default
    private Report report = new Report();
}
