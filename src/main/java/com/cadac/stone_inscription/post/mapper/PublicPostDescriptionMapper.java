package com.cadac.stone_inscription.post.mapper;


import com.cadac.stone_inscription.entity.PublicPostDescription;
import com.cadac.stone_inscription.post.dto.PublicPostUserDescriptionDto;

import java.util.List;
import java.util.stream.Collectors;

public class PublicPostDescriptionMapper {

    public static PublicPostUserDescriptionDto toDto(PublicPostDescription entity) {
        if (entity == null) {
            return null;
        }

        return PublicPostUserDescriptionDto.builder()
                .id(entity.getId() != null ? entity.getId().toString() : null)
                .postId(entity.getPostId() != null ? entity.getPostId().toString() : null)
                .userId(entity.getUserId() != null ? entity.getUserId().toString() : null)
                .username(entity.getUsername())
                .userImageUrl(entity.getUserImageUrl())
                .description(entity.getDescription())
                .upvote(entity.getUpvote())
                .userVote(toUserVoteDtoList(entity.getUserVote()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static List<PublicPostUserDescriptionDto.UserVoteDto> toUserVoteDtoList(List<PublicPostDescription.UserVote> userVotes) {
        if (userVotes == null) {
            return null;
        }

        return userVotes.stream()
                .map(vote -> PublicPostUserDescriptionDto.UserVoteDto.builder()
                        .userId(vote.getUserId())
                        .build())
                .collect(Collectors.toList());
    }
}

