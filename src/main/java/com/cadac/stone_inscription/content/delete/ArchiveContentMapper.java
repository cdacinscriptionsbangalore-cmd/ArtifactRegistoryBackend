package com.cadac.stone_inscription.content.delete;

import org.springframework.stereotype.Component;

import com.cadac.stone_inscription.admin.entity.ArchiveComment;
import com.cadac.stone_inscription.admin.entity.ArchivePost;
import com.cadac.stone_inscription.entity.InscriptionPost;
import com.cadac.stone_inscription.entity.PublicPostDescription;

@Component
public class ArchiveContentMapper {

    public ArchivePost toArchivePost(InscriptionPost post) {
        return ArchivePost.builder()
                .originalPostId(post.getId())
                .userId(post.getUserId())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .images(post.getImages() != null ? ArchivePost.Images.builder()
                        .thumbnailImage(post.getImages().getThumbnailImage())
                        .image(post.getImages().getImage())
                        .build() : null)
                .description(post.getDescription() != null ? ArchivePost.Description.builder()
                        .title(post.getDescription().getTitle())
                        .subject(post.getDescription().getSubject())
                        .description(post.getDescription().getDescription())
                        .scriptLanguage(post.getDescription().getScriptLanguage())
                        .language(post.getDescription().getLanguage())
                        .englishTranslation(post.getDescription().getEnglishTranslation())
                        .moderation(post.getDescription().getModeration())
                        .upvote(post.getDescription().getUpvote())
                        .geolocation(post.getDescription().getGeolocation() != null ? ArchivePost.GeoLocation.builder()
                                .lat(post.getDescription().getGeolocation().getLat())
                                .lon(post.getDescription().getGeolocation().getLon())
                                .state(post.getDescription().getGeolocation().getState())
                                .city(post.getDescription().getGeolocation().getCity())
                                .country(post.getDescription().getGeolocation().getCountry())
                                .build() : null)
                        .createdAt(post.getDescription().getCreatedAt())
                        .updatedAt(post.getDescription().getUpdatedAt())
                        .build() : null)
                .topic(post.getTopic())
                .script(post.getScript())
                .type(post.getType())
                .status(post.getStatus())
                .report(post.getReport())
                .build();
    }

    public ArchiveComment toArchiveComment(PublicPostDescription comment) {
        return ArchiveComment.builder()
                .originalCommentId(comment.getId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .username(comment.getUsername())
                .userImageUrl(comment.getUserImageUrl())
                .description(comment.getDescription())
                .moderation(comment.getModeration())
                .upvote(comment.getUpvote())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .status(comment.getStatus())
                .report(comment.getReport())
                .build();
    }
}
