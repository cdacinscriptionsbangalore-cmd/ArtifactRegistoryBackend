package com.cadac.stone_inscription.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import dev.brachtendorf.jimagehash.hash.Hash;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "imagesdata")
public class ImagesData {

    @Id
    @JsonProperty("id")
    private String id;   // Primary Id (String)

    @Field("post_id")
    @JsonProperty("postId")
        @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId postId;  // Reference to Post document

    @Field("object_name")
    @JsonProperty("objectName")
    private String objectName;

    
    @JsonProperty("metadata")
    private Metadata metadata;  // Embedded metadata object


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

        @Field("image_hash_value")
        @JsonProperty("imageHashValue")
        private String imageHashValue; 
    }
}
