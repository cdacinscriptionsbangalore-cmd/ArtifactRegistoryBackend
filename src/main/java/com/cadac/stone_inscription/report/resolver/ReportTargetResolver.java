package com.cadac.stone_inscription.report.resolver;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.cadac.stone_inscription.entity.InscriptionPost;
import com.cadac.stone_inscription.entity.PublicPostDescription;
import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.repository.InscriptionPostRepo;
import com.cadac.stone_inscription.repository.PublicPostDescriptionRepo;
import com.cadac.stone_inscription.repository.UserRepository;
import com.cadac.stone_inscription.report.enums.ReportTargetType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportTargetResolver {

    private final InscriptionPostRepo inscriptionPostRepo;
    private final PublicPostDescriptionRepo publicPostDescriptionRepo;
    private final UserRepository userRepository;

    public ResolvedReportTarget resolve(ReportTargetType targetType, String targetId) {
        if (!ObjectId.isValid(targetId)) {
            throw new StoneInscriptionException("Invalid target id", HttpStatus.BAD_REQUEST);
        }

        ObjectId objectId = new ObjectId(targetId);

        return switch (targetType) {
            case POST -> resolvePost(objectId);
            case COMMENT -> resolveComment(objectId);
            case USER -> resolveUser(objectId);
        };
    }

    private ResolvedReportTarget resolvePost(ObjectId objectId) {
        InscriptionPost post = inscriptionPostRepo.findById(objectId)
                .orElseThrow(() -> new StoneInscriptionException("Post not found", HttpStatus.NOT_FOUND));

        String content = Optional.ofNullable(post.getDescription())
                .map(InscriptionPost.Description::getDescription)
                .orElse("");

        return ResolvedReportTarget.builder()
                .id(post.getId().toHexString())
                .authorId(post.getUserId().toHexString())
                .type(ReportTargetType.POST)
                .content(content)
                .entity(post)
                .build();
    }

    private ResolvedReportTarget resolveComment(ObjectId objectId) {
        PublicPostDescription comment = publicPostDescriptionRepo.findById(objectId)
                .orElseThrow(() -> new StoneInscriptionException("Comment not found", HttpStatus.NOT_FOUND));

        return ResolvedReportTarget.builder()
                .id(comment.getId().toHexString())
                .authorId(comment.getUserId().toHexString())
                .type(ReportTargetType.COMMENT)
                .content(comment.getDescription())
                .entity(comment)
                .build();
    }

    private ResolvedReportTarget resolveUser(ObjectId objectId) {
        User user = userRepository.findById(objectId)
                .orElseThrow(() -> new StoneInscriptionException("User not found", HttpStatus.NOT_FOUND));

        String content = user.getBio() == null ? "" : user.getBio();

        return ResolvedReportTarget.builder()
                .id(user.getId().toHexString())
                .authorId(user.getId().toHexString())
                .type(ReportTargetType.USER)
                .content(content)
                .entity(user)
                .build();
    }
}
