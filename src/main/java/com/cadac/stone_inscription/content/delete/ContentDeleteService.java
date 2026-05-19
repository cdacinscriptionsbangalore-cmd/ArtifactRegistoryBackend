package com.cadac.stone_inscription.content.delete;

import java.util.Collections;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cadac.stone_inscription.admin.repository.ArchiveCommentRepository;
import com.cadac.stone_inscription.admin.repository.ArchivePostRepository;
import com.cadac.stone_inscription.entity.InscriptionPost;
import com.cadac.stone_inscription.entity.PublicPostDescription;
import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.repository.ImagesDataRepo;
import com.cadac.stone_inscription.repository.InscriptionPostRepo;
import com.cadac.stone_inscription.repository.PublicPostDescriptionRepo;
import com.cadac.stone_inscription.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContentDeleteService {

    private final InscriptionPostRepo inscriptionPostRepo;
    private final PublicPostDescriptionRepo publicPostDescriptionRepo;
    private final ArchivePostRepository archivePostRepository;
    private final ArchiveCommentRepository archiveCommentRepository;
    private final ArchiveContentMapper archiveContentMapper;
    private final ImagesDataRepo imagesDataRepo;
    private final UserRepository userRepository;

    public ContentDeleteResult deletePost(ObjectId postId) {
        InscriptionPost post = inscriptionPostRepo.findById(postId)
                .orElseThrow(() -> new StoneInscriptionException("Unprocesable request", HttpStatus.BAD_REQUEST));

        List<PublicPostDescription> comments = publicPostDescriptionRepo.findByPostId(postId);
        List<String> imageIds = getImageIds(post);

        archivePostRepository.save(archiveContentMapper.toArchivePost(post));
        comments.stream()
                .map(archiveContentMapper::toArchiveComment)
                .forEach(archiveCommentRepository::save);

        imageIds.forEach(imagesDataRepo::deleteById);
        publicPostDescriptionRepo.deleteAll(comments);
        inscriptionPostRepo.delete(post);
        decrementUserImagesUploaded(post.getUserId(), imageIds.size());

        return ContentDeleteResult.builder()
                .archivedPosts(1)
                .archivedComments(comments.size())
                .deletedImages(imageIds.size())
                .build();
    }

    public ContentDeleteResult deleteComment(ObjectId commentId) {
        PublicPostDescription comment = publicPostDescriptionRepo.findById(commentId)
                .orElseThrow(() -> new StoneInscriptionException("Unprocesable request", HttpStatus.BAD_REQUEST));

        archiveCommentRepository.save(archiveContentMapper.toArchiveComment(comment));
        publicPostDescriptionRepo.delete(comment);

        return ContentDeleteResult.builder()
                .archivedPosts(0)
                .archivedComments(1)
                .deletedImages(0)
                .build();
    }

    private List<String> getImageIds(InscriptionPost post) {
        if (post.getImages() == null || post.getImages().getImage() == null) {
            return Collections.emptyList();
        }

        return post.getImages().getImage();
    }

    private void decrementUserImagesUploaded(ObjectId userId, int imageCount) {
        if (imageCount <= 0) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new StoneInscriptionException("User not found", HttpStatus.NOT_FOUND));

        int currentCount = user.getImagesUploaded() == null ? 0 : user.getImagesUploaded();
        user.setImagesUploaded(Math.max(0, currentCount - imageCount));
        userRepository.save(user);
    }
}
