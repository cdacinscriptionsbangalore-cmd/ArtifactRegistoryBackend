package com.cadac.stone_inscription.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cadac.stone_inscription.auth.JwtUtil;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.user.dto.UpdateProfileRequest;
import com.cadac.stone_inscription.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Get current user's profile
     * GET /api/v1/user/profile
     */
    @GetMapping("/profile")
    @Secured("user")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        String email = extractEmailFromToken(request);
        return userService.getProfile(email);
    }

    /**
     * Update user's profile (username only)
     * POST /api/v1/user/updateProfile
     */
    @PostMapping("/updateProfile")
    @Secured("user")
    public ResponseEntity<?> updateProfile(
            HttpServletRequest request,
            @Valid @RequestBody UpdateProfileRequest updateProfileRequest) {

        String email = extractEmailFromToken(request);

        return userService.updateProfile(email, updateProfileRequest);
    }

    /**
     * Upload/Update profile image
     * POST /api/v1/user/uploadProfileImage
     */
    @PostMapping("/uploadProfileImage")
    @Secured("user")
    public ResponseEntity<?> uploadProfileImage(
            HttpServletRequest request,
            @RequestPart("file") MultipartFile file) {

        String email = extractEmailFromToken(request);
        return userService.uploadProfileImage(email, file);
    }

    /**
     * Upload/Update cover image
     * POST /api/v1/user/uploadCoverImage
     */
    @PostMapping("/uploadCoverImage")
    @Secured("user")
    public ResponseEntity<?> uploadCoverImage(
            HttpServletRequest request,
            @RequestPart("file") MultipartFile file) {

        String email = extractEmailFromToken(request);
        return userService.uploadCoverImage(email, file);
    }

    /**
     * Get user image (profile or cover) by ID - Public endpoint
     * GET /api/v1/user/public/images/{id}
     */
    @GetMapping("/public/images/{id}")
    public ResponseEntity<InputStreamResource> getUserImage(@PathVariable String id) {
        return userService.getUserImage(id);
    }

    // ============================
    // TEST-ONLY ENDPOINTS (NO JWT)
    // TODO: Remove these methods when testing is done to restore normal behavior.
    // ============================

    /**
     * TEST ONLY: Get profile directly by email without JWT.
     * GET /api/v1/user/test/profile/{email}
     */
    @GetMapping("/test/profile/{email}")
    public ResponseEntity<?> getProfileForTest(@PathVariable String email) {
        return userService.getProfile(email);
    }

    /**
     * TEST ONLY: Update profile directly by email without JWT.
     * POST /api/v1/user/test/updateProfile/{email}
     */
    @PostMapping("/test/updateProfile/{email}")
    // @Secured("user")
    public ResponseEntity<?> updateProfileForTest(
            @PathVariable String email,
            @Valid @RequestBody UpdateProfileRequest updateProfileRequest) {

        return userService.updateProfile(email, updateProfileRequest);
    }

    /**
     * TEST ONLY: Upload profile image directly by email without JWT.
     * POST /api/v1/user/test/uploadProfileImage/{email}
     */
    @PostMapping("/test/uploadProfileImage/{email}")
    public ResponseEntity<?> uploadProfileImageForTest(
            @PathVariable String email,
            @RequestPart("file") MultipartFile file) {

        return userService.uploadProfileImage(email, file);
    }

    /**
     * TEST ONLY: Upload cover image directly by email without JWT.
     * POST /api/v1/user/test/uploadCoverImage/{email}
     */
    @PostMapping("/test/uploadCoverImage/{email}")
    public ResponseEntity<?> uploadCoverImageForTest(
            @PathVariable String email,
            @RequestPart("file") MultipartFile file) {

        return userService.uploadCoverImage(email, file);
    }

    /**
     * Helper method to extract email from JWT token
     */
    private String extractEmailFromToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            throw new StoneInscriptionException("Invalid or missing authorization token", HttpStatus.UNAUTHORIZED);
        }

        token = token.substring(7);
        return jwtUtil.getUsernameFromToken(token);
    }
}
