package com.cadac.stone_inscription.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_images")
public class UserImage {

    @Id
    @JsonProperty("id")
    private String id;

    @Field("user_id")
    @JsonProperty("userId")
    @JsonSerialize(using = ToStringSerializer.class)
    @Indexed
    private ObjectId userId;

    @Field("image_type")
    @JsonProperty("imageType")
    private ImageType imageType;

    @Field("object_name")
    @JsonProperty("objectName")
    private String objectName;

    @JsonProperty("metadata")
    private Metadata metadata;

    public enum ImageType {
        PROFILE,
        COVER
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {

        @Field("file_name")
        @JsonProperty("fileName")
        private String fileName;

        @Field("file_size")
        @JsonProperty("fileSize")
        private Long fileSize;

        @Field("content_type")
        @JsonProperty("contentType")
        private String contentType;
    }
}
