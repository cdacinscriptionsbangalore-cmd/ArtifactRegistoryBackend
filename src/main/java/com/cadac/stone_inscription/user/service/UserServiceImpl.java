package com.cadac.stone_inscription.user.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.entity.UserImage;
import com.cadac.stone_inscription.entity.UserImage.ImageType;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.file.FileValidationService;
import com.cadac.stone_inscription.file.FileValidationService.ValidatedImage;
import com.cadac.stone_inscription.minio.service.MinioStorageService;
import com.cadac.stone_inscription.repository.UserImageRepo;
import com.cadac.stone_inscription.repository.UserRepository;
import com.cadac.stone_inscription.user.dto.UpdateProfileRequest;
import com.cadac.stone_inscription.user.dto.UserProfileResponse;
import com.cadac.stone_inscription.util.UserResponse;

@Service
public class UserServiceImpl implements UserService {

    private static final String USERNAME_PATTERN = "^[A-Za-z0-9_ ]+$";
    private static final String BIO_PATTERN = "^(?=.*[A-Za-z0-9])[A-Za-z0-9 .,!?'-]+$";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserImageRepo userImageRepo;

    @Autowired
    private FileValidationService fileValidationService;

    @Autowired
    private MinioStorageService minioStorageService;

    @Value("${app.backend.url}")
    private String backendUrl;

    @Override
    public ResponseEntity<?> getProfile(String emailFromToken) {
        User user = userRepository.findByEmail(emailFromToken);

        if (user == null) {
            throw new StoneInscriptionException("User not found", HttpStatus.UNAUTHORIZED);
        }

        UserProfileResponse response = mapToUserProfileResponse(user);
        return UserResponse.responseHandler("Profile fetched successfully", HttpStatus.OK, response);
    }

    @Override
    public ResponseEntity<?> updateProfile(String emailFromToken, UpdateProfileRequest request) {
        if (request == null) {
            throw new StoneInscriptionException("Invalid request", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByEmail(emailFromToken);

        if (user == null) {
            throw new StoneInscriptionException("User not found", HttpStatus.UNAUTHORIZED);
        }

        boolean hasUpdates = false;

        // Update username if provided
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            String newUsername = sanitiseInput(request.getUsername(), "username");

            if (!newUsername.matches(USERNAME_PATTERN)) {
                throw new StoneInscriptionException("Username may only contain letters, numbers, spaces, and underscores", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            User existingUserWithUsername = userRepository.findByUsername(newUsername);
            if (existingUserWithUsername != null && !existingUserWithUsername.getId().equals(user.getId())) {
                throw new StoneInscriptionException("Username is already taken", HttpStatus.BAD_REQUEST);
            }

            user.setUsername(newUsername);
            hasUpdates = true;
        }

        if (request.getBio() != null) {
            String newBio = sanitiseInput(request.getBio(), "bio");

            if (newBio.length() < 3 || newBio.length() > 150) {
                throw new StoneInscriptionException("Bio must be between 3 and 150 characters", HttpStatus.BAD_REQUEST);
            }

            if (!newBio.matches(BIO_PATTERN)) {
                throw new StoneInscriptionException("Bio contains invalid characters", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            user.setBio(newBio);
            hasUpdates = true;
        }

        if (!hasUpdates) {
            throw new StoneInscriptionException("No valid fields to update", HttpStatus.BAD_REQUEST);
        }

        User savedUser = userRepository.save(user);
        UserProfileResponse response = mapToUserProfileResponse(savedUser);

        return UserResponse.responseHandler("Profile updated successfully", HttpStatus.OK, response);
    }

    @Override
    public ResponseEntity<?> uploadProfileImage(String emailFromToken, MultipartFile file) {
        return uploadUserImage(emailFromToken, file, ImageType.PROFILE);
    }

    @Override
    public ResponseEntity<?> uploadCoverImage(String emailFromToken, MultipartFile file) {
        return uploadUserImage(emailFromToken, file, ImageType.COVER);
    }

    @Override
    public ResponseEntity<InputStreamResource> getUserImage(String id) {
        UserImage image = userImageRepo.findById(id)
                .orElseThrow(() -> new StoneInscriptionException("Image not found", HttpStatus.NOT_FOUND));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getMetadata().getContentType()))
                .contentLength(image.getMetadata().getFileSize())
                .body(new InputStreamResource(minioStorageService.getObject(image.getObjectName())));
    }

    /**
     * Helper method to upload user image (profile or cover)
     */
    private ResponseEntity<?> uploadUserImage(String emailFromToken, MultipartFile file, ImageType imageType) {
        if (file == null || file.isEmpty()) {
            throw new StoneInscriptionException(imageType.name().toLowerCase() + " image is required", HttpStatus.BAD_REQUEST);
        }

        ValidatedImage validatedImage;
        try {
            validatedImage = fileValidationService.validateImageFile(file);
        } catch (IOException e) {
            throw new StoneInscriptionException("Failed to validate image file", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        User user = userRepository.findByEmail(emailFromToken);
        if (user == null) {
            throw new StoneInscriptionException("User not found", HttpStatus.UNAUTHORIZED);
        }

        Optional<UserImage> existingImage = userImageRepo.findByUserIdAndImageType(user.getId(), imageType);
        String objectName = buildUserImageObjectName(user, imageType, validatedImage.storedFileName());

        minioStorageService.putObject(
                objectName,
                new ByteArrayInputStream(validatedImage.bytes()),
                validatedImage.fileSize(),
                validatedImage.contentType());

        UserImage userImage = existingImage.orElseGet(() -> UserImage.builder()
                .userId(user.getId())
                .imageType(imageType)
                .build());
        String previousObjectName = userImage.getObjectName();
        UserImage.Metadata previousMetadata = userImage.getMetadata();

        userImage.setObjectName(objectName);
        userImage.setMetadata(UserImage.Metadata.builder()
                .fileName(validatedImage.storedFileName())
                .fileSize(validatedImage.fileSize())
                .contentType(validatedImage.contentType())
                .build());

        UserImage savedImage;
        try {
            savedImage = userImageRepo.save(userImage);
        } catch (RuntimeException exception) {
            minioStorageService.removeObject(objectName);
            throw exception;
        }

        // Update user's profile/cover image URL
        String imageUrl = backendUrl + "/user/public/images/" + savedImage.getId();

        if (imageType == ImageType.PROFILE) {
            user.setProfileImage(imageUrl);
        } else {
            user.setCoverImage(imageUrl);
        }

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (RuntimeException exception) {
            minioStorageService.removeObject(objectName);
            restorePreviousUserImage(savedImage, previousObjectName, previousMetadata, existingImage.isPresent());
            throw exception;
        }

        if (previousObjectName != null && !previousObjectName.equals(objectName)) {
            minioStorageService.removeObject(previousObjectName);
        }
        UserProfileResponse response = mapToUserProfileResponse(savedUser);

        String message = imageType == ImageType.PROFILE ? "Profile image updated successfully" : "Cover image updated successfully";
        return UserResponse.responseHandler(message, HttpStatus.OK, response);
    }

    private String buildUserImageObjectName(User user, ImageType imageType, String fileName) {
        return "users/" + user.getId().toHexString() + "/" + imageType.name().toLowerCase() + "/" + fileName;
    }

    private void restorePreviousUserImage(
            UserImage image,
            String previousObjectName,
            UserImage.Metadata previousMetadata,
            boolean existedPreviously) {
        if (!existedPreviously) {
            userImageRepo.delete(image);
            return;
        }

        image.setObjectName(previousObjectName);
        image.setMetadata(previousMetadata);
        userImageRepo.save(image);
    }

    /**
     * Helper method to map User entity to UserProfileResponse DTO
     */
    private UserProfileResponse mapToUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId().toString())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .profileImage(user.getProfileImage())
                .coverImage(user.getCoverImage())
                .bio(user.getBio())
                .imagesUploaded(user.getImagesUploaded())
                .upvotesReceived(user.getUpvotesReceived())
                .followers(user.getFollowers())
                .points(user.getPoints())
                .build();
    }

    private String sanitiseInput(String input, String fieldName) {
        if (input == null) {
            return null;
        }

        String trimmed = input.trim();
        String strippedHtml = trimmed.replaceAll("<[^>]*>", "");

        if (!strippedHtml.equals(trimmed)) {
            throw new StoneInscriptionException(fieldName + " contains disallowed HTML content", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        return trimmed;
    }
}
