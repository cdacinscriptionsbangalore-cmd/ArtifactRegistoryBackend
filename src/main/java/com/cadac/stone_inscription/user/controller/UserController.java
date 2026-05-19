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
import com.cadac.stone_inscription.api.dto.ApiErrorResponse;
import com.cadac.stone_inscription.api.dto.ApiSuccessResponse;
import com.cadac.stone_inscription.user.dto.UpdateProfileRequest;
import com.cadac.stone_inscription.user.dto.UserProfileResponse;
import com.cadac.stone_inscription.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/user")
@Tag(name = "Users", description = "Authenticated user profile and image management APIs.")
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
    @Operation(
            summary = "Get my profile",
            description = "Returns the profile attached to the JWT subject.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile fetched successfully",
                            content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class),
                                    examples = @ExampleObject(value = "{\"message\":\"Profile fetched successfully\",\"http-status\":\"OK\",\"data\":{\"id\":\"665f1df013ad4e18f6a11244\",\"name\":\"Asha Rao\",\"username\":\"asha_rao\",\"email\":\"asha@example.com\",\"profileImage\":\"https://inscriptions.cdacb.in/api/user/public/images/665f1df013ad4e18f6a11245\",\"coverImage\":\"https://inscriptions.cdacb.in/api/user/public/images/665f1df013ad4e18f6a11246\",\"bio\":\"Epigraphy researcher\",\"imagesUploaded\":12,\"upvotesReceived\":34,\"followers\":8,\"points\":240}}"))),
                    @ApiResponse(responseCode = "401", description = "Token is missing or user cannot be resolved",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
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
    @Operation(
            summary = "Update my profile",
            description = "Updates the authenticated user's editable profile fields. Bean validation constraints are visible in the request schema.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateProfileRequest.class),
                            examples = @ExampleObject(value = "{\"username\":\"inscription_scholar\",\"bio\":\"Epigraphy researcher\"}"))),
            responses = @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))))
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
    @Operation(
            summary = "Upload profile image",
            description = "Replaces the authenticated user's profile image. Accepts one multipart image file using the configured extension allow-list.",
            responses = @ApiResponse(responseCode = "200", description = "Profile image updated",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))))
    public ResponseEntity<?> uploadProfileImage(
            HttpServletRequest request,
            @Parameter(description = "Profile image file. Allowed extensions come from `file.extn`.", required = true)
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
    @Operation(
            summary = "Upload cover image",
            description = "Replaces the authenticated user's cover image. Accepts one multipart image file using the configured extension allow-list.",
            responses = @ApiResponse(responseCode = "200", description = "Cover image updated",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))))
    public ResponseEntity<?> uploadCoverImage(
            HttpServletRequest request,
            @Parameter(description = "Cover image file. Allowed extensions come from `file.extn`.", required = true)
            @RequestPart("file") MultipartFile file) {

        String email = extractEmailFromToken(request);
        return userService.uploadCoverImage(email, file);
    }

    /**
     * Get user image (profile or cover) by ID - Public endpoint
     * GET /api/v1/user/public/images/{id}
     */
    @GetMapping("/public/images/{id}")
    @Operation(
            summary = "Download user image",
            description = "Public endpoint that streams a profile or cover image by id.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Image stream",
                            content = @Content(mediaType = "image/*", schema = @Schema(type = "string", format = "binary"))),
                    @ApiResponse(responseCode = "404", description = "Image not found",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<InputStreamResource> getUserImage(@PathVariable String id) {
        return userService.getUserImage(id);
    }

    // ============================
    // TEST-ONLY ENDPOINTS (NO JWT)
    // TODO: Remove these methods when testing is done to restore normal behavior.
    // ============================

    // /**
    //  * TEST ONLY: Get profile directly by email without JWT.
    //  * GET /api/v1/user/test/profile/{email}
    //  */
    // @GetMapping("/test/profile/{email}")
    // public ResponseEntity<?> getProfileForTest(@PathVariable String email) {
    //     return userService.getProfile(email);
    // }

    // /**
    //  * TEST ONLY: Update profile directly by email without JWT.
    //  * POST /api/v1/user/test/updateProfile/{email}
    //  */
    // @PostMapping("/test/updateProfile/{email}")
    // public ResponseEntity<?> updateProfileForTest(
    //         @PathVariable String email,
    //         @Valid @RequestBody UpdateProfileRequest updateProfileRequest) {

    //     return userService.updateProfile(email, updateProfileRequest);
    // }

    // /**
    //  * TEST ONLY: Upload profile image directly by email without JWT.
    //  * POST /api/v1/user/test/uploadProfileImage/{email}
    //  */
    // @PostMapping("/test/uploadProfileImage/{email}")
    // public ResponseEntity<?> uploadProfileImageForTest(
    //         @PathVariable String email,
    //         @RequestPart("file") MultipartFile file) {

    //     return userService.uploadProfileImage(email, file);
    // }

    // /**
    //  * TEST ONLY: Upload cover image directly by email without JWT.
    //  * POST /api/v1/user/test/uploadCoverImage/{email}
    //  */
    // @PostMapping("/test/uploadCoverImage/{email}")
    // public ResponseEntity<?> uploadCoverImageForTest(
    //         @PathVariable String email,
    //         @RequestPart("file") MultipartFile file) {

    //     return userService.uploadCoverImage(email, file);
    // }

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
