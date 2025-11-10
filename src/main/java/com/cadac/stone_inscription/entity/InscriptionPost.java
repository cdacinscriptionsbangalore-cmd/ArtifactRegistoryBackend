package com.cadac.stone_inscription.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "inscriptionposts")
@CompoundIndexes({
        @CompoundIndex(name = "user_topic_idx", def = "{'user_id': 1, 'topic': 1}"),
        @CompoundIndex(name = "created_at_idx", def = "{'createdAt': -1}")
})

public class InscriptionPost {

    @Id
    @JsonProperty("_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @Field("user_id")
    @JsonProperty("user_id")
    @NotBlank
    @Indexed
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId userId;

    @CreatedDate
    @Field("createdAt")
    @JsonProperty("createdAt")
    @Indexed
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
    @Indexed
    private String topic;

    @Field("script")
    @JsonProperty("script")
    private List<String> script;

    @Field("type")
    @JsonProperty("type")
    private String type;

    @Field("distance")
    @JsonProperty("distance")
    private Double distance;

    @Field("rating")
    @JsonProperty("rating")
    @Builder.Default
    private Double rating = 0.0;

    @Field("userrating")
    @JsonProperty("userrating")
    @Builder.Default
    private List<UsersRating> userRating = new LinkedList<>();

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
        @NotNull
        @NotEmpty
        private String lon;

        @Field("lat")
        @JsonProperty("lat")
        @NotNull
        @NotEmpty
        private String lat;
        
        private String amenity;
        private String road;
        private String neighbourhood;
        private String suburb;

        @JsonProperty("city_district")
        private String cityDistrict;

        private String city;
        private String county;

        @JsonProperty("state_district")
        private String stateDistrict;

        private String state;

        @JsonProperty("ISO3166-2-lvl4")
        private String iso3166Lvl4;

        private String postcode;
        private String country;

        @JsonProperty("country_code")
        private String countryCode;

        @JsonProperty("place_id")
        private Long placeId;

        private String licence;

        @JsonProperty("osm_type")
        private String osmType;

        @JsonProperty("osm_id")
        private Long osmId;

        @JsonProperty("class")
        private String clazz; // "class" is reserved in Java, so renamed to clazz

        private String type;

        @JsonProperty("place_rank")
        private Integer placeRank;

        private Double importance;

        @JsonProperty("addresstype")
        private String addressType;

        private String name;

        @JsonProperty("display_name")
        private String displayName;

        private List<String> boundingbox;

        @GeoSpatialIndexed
        private List<Double> coordinates; // [lon, lat] for geo queries
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsersRating {

        @Field("userId")
        @JsonProperty("userId")
        private String userId;

        @Field("rating")
        @JsonProperty("rating")
        private double rating;
    }
}
