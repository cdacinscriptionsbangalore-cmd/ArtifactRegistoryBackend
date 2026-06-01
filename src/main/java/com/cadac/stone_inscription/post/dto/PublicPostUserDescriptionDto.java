package com.cadac.stone_inscription.post.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PublicPostUserDescription", description = "Description/comment authored by a user with voting and image preview details.")
public class PublicPostUserDescriptionDto {

    @JsonProperty("id")
    @Schema(description = "Description identifier.", example = "665f1df013ad4e18f6a11247")
    private String id;

    @JsonProperty("postId")
    @Schema(description = "Associated post identifier.", example = "665f1df013ad4e18f6a11244")
    private String postId;

    @JsonProperty("userId")
    @Schema(description = "Author user identifier.", example = "665f1df013ad4e18f6a11240")
    private String userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("userImageUrl")
    private String userImageUrl;

    @JsonProperty("postImageUrl")
    private String postImageUrl;

    @JsonProperty("description")
    @Schema(description = "Description text.", example = "The inscription appears to reference a land grant.")
    private String description;

    @JsonProperty("upvote")
    @Schema(description = "Current upvote count.", example = "7")
    private Integer upvote;

    @JsonProperty("userVote")
    private List<UserVoteDto> userVote;

    @JsonProperty("createdAt")
    private Date createdAt;

    @JsonProperty("updatedAt")
    private Date updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "UserVote", description = "User vote marker for a description.")
    public static class UserVoteDto {
        @JsonProperty("userId")
        @Schema(description = "Voting user identifier.", example = "665f1df013ad4e18f6a11240")
        private String userId;
    }
}
