package com.cadac.stone_inscription.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserProfile", description = "Profile details returned for the authenticated user.")
public class UserProfileResponse {

    @JsonProperty("id")
    @Schema(description = "User identifier.", example = "665f1df013ad4e18f6a11244")
    private String id;

    @JsonProperty("name")
    @Schema(description = "OAuth provider display name.", example = "Asha Rao")
    private String name;

    @JsonProperty("username")
    @Schema(description = "Application username.", example = "asha_rao")
    private String username;

    @JsonProperty("email")
    @Schema(description = "User email address.", example = "asha@example.com", format = "email")
    private String email;

    @JsonProperty("profileImage")
    @Schema(description = "Public profile image URL.", example = "https://inscriptions.cdacb.in/api/user/public/images/665f1df013ad4e18f6a11245")
    private String profileImage;

    @JsonProperty("coverImage")
    @Schema(description = "Public cover image URL.", example = "https://inscriptions.cdacb.in/api/user/public/images/665f1df013ad4e18f6a11246")
    private String coverImage;

    @JsonProperty("bio")
    @Schema(description = "Short user bio.", example = "Epigraphy researcher")
    private String bio;

    @JsonProperty("imagesUploaded")
    @Schema(description = "Number of uploaded inscription images.", example = "12")
    private Integer imagesUploaded;

    @JsonProperty("upvotesReceived")
    @Schema(description = "Total upvotes received on user comments/descriptions.", example = "34")
    private Integer upvotesReceived;

    @JsonProperty("followers")
    @Schema(description = "Follower count.", example = "8")
    private Integer followers;

    @JsonProperty("points")
    @Schema(description = "Reputation points.", example = "240")
    private Integer points;
}
