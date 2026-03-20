package com.cadac.stone_inscription.post.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.cadac.stone_inscription.moderation.model.ContentModeration;
import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicPostUserDescriptionDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("postId")
    private String postId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("userImageUrl")
    private String userImageUrl;

    @JsonProperty("postImageUrl")
    private String postImageUrl;

    @JsonProperty("description")
    private String description;

    @JsonProperty("upvote")
    private Integer upvote;

    @JsonProperty("moderation")
    private ContentModeration moderation;

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
    public static class UserVoteDto {
        @JsonProperty("userId")
        private String userId;
    }
}
