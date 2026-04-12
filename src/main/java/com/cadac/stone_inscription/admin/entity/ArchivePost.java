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
import java.util.List;

/**
 * Archive of rejected posts.
 * A post is moved here from inscriptionposts when admin rejects it.
 * Preserves the full original post structure for audit/record purposes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "archive_posts")
public class ArchivePost {

    @Id
    @JsonProperty("_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @Field("originalPostId")
    @JsonProperty("originalPostId")
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId originalPostId;

    @Field("user_id")
    @JsonProperty("user_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId userId;

    @CreatedDate
    @Field("createdAt")
    @JsonProperty("createdAt")
    private Date createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    @JsonProperty("updatedAt")
    private Date updatedAt;

    @Field("images")
    @JsonProperty("images")
    private Images images;

    @Field("description")
    @JsonProperty("description")
    private Description description;

    @Field("topic")
    @JsonProperty("topic")
    private String topic;

    @Field("script")
    @JsonProperty("script")
    private List<String> script;

    @Field("type")
    @JsonProperty("type")
    private String type;

    @Field("status")
    @JsonProperty("status")
    @Builder.Default
    private PostStatus status = PostStatus.REJECTED;

    @Field("report")
    @JsonProperty("report")
    @Builder.Default
    private Report report = new Report();

    // ---------- Nested Classes ----------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Images {

        @Field("thumbnailImage")
        @JsonProperty("thumbnailImage")
        private String thumbnailImage;

        @Field("image")
        @JsonProperty("image")
        private List<String> image;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Description {

        @Field("title")
        @JsonProperty("title")
        private String title;

        @Field("subject")
        @JsonProperty("subject")
        private String subject;

        @Field("description")
        @JsonProperty("description")
        private String description;

        @Field("scriptLanguage")
        @JsonProperty("scriptLanguage")
        private List<String> scriptLanguage;

        @Field("language")
        @JsonProperty("language")
        private List<String> language;

        @Field("englishTranslation")
        @JsonProperty("englishTranslation")
        private String englishTranslation;

        @Field("moderation")
        @JsonProperty("moderation")
        private ContentModeration moderation;

        @Field("upvote")
        @JsonProperty("upvote")
        @Builder.Default
        private Integer upvote = 0;

        @Field("geolocation")
        @JsonProperty("geolocation")
        private GeoLocation geolocation;

        @CreatedDate
        @Field("createdAt")
        @JsonProperty("createdAt")
        private Date createdAt;

        @LastModifiedDate
        @Field("updatedAt")
        @JsonProperty("updatedAt")
        private Date updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoLocation {

        @Field("lon")
        @JsonProperty("lon")
        private String lon;

        @Field("lat")
        @JsonProperty("lat")
        private String lat;

        private String state;
        private String city;
        private String country;
    }
}
