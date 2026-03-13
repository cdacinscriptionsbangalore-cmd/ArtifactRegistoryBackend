package com.cadac.stone_inscription.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("profileImage")
    private String profileImage;

    @JsonProperty("coverImage")
    private String coverImage;

    @JsonProperty("imagesUploaded")
    private Integer imagesUploaded;

    @JsonProperty("upvotesReceived")
    private Integer upvotesReceived;

    @JsonProperty("followers")
    private Integer followers;

    @JsonProperty("points")
    private Integer points;
}
