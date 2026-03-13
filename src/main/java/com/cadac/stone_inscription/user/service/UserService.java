package com.cadac.stone_inscription.user.service;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.cadac.stone_inscription.user.dto.UpdateProfileRequest;

public interface UserService {

    /**
     * Get current user's profile
     * @param emailFromToken User's email extracted from JWT token
     * @return User profile data
     */
    ResponseEntity<?> getProfile(String emailFromToken);

    /**
     * Update user's profile (username only)
     * @param emailFromToken User's email extracted from JWT token
     * @param request Contains optional username to update
     * @return Updated user profile
     */
    ResponseEntity<?> updateProfile(String emailFromToken, UpdateProfileRequest request);

    /**
     * Upload/Update profile image
     * @param emailFromToken User's email extracted from JWT token
     * @param file Profile image file
     * @return Updated user profile with new profile image URL
     */
    ResponseEntity<?> uploadProfileImage(String emailFromToken, MultipartFile file);

    /**
     * Upload/Update cover image
     * @param emailFromToken User's email extracted from JWT token
     * @param file Cover image file
     * @return Updated user profile with new cover image URL
     */
    ResponseEntity<?> uploadCoverImage(String emailFromToken, MultipartFile file);

    /**
     * Get user image (profile or cover) by ID
     * @param id Image ID
     * @return Image binary data
     */
    ResponseEntity<InputStreamResource> getUserImage(String id);
}
