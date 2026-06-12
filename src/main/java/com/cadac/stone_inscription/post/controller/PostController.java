package com.cadac.stone_inscription.post.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cadac.stone_inscription.api.dto.ApiErrorResponse;
import com.cadac.stone_inscription.api.dto.ApiSuccessResponse;
import com.cadac.stone_inscription.api.dto.DashboardCountsResponse;
import com.cadac.stone_inscription.auth.JwtUtil;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.file.FileValidationService;
import com.cadac.stone_inscription.post.dto.InscriptionPostDto;
import com.cadac.stone_inscription.post.service.PostService;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/post")
@Tag(name = "Posts", description = "Inscription post, image, description, rating, and dashboard APIs.")
public class PostController {

    private static final int MAX_IMAGES_PER_POST = 16;
    private static final long MAX_IMAGE_SIZE_BYTES = 75L * 1024 * 1024;

    @Autowired
    private PostService postService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FileValidationService fileValidationService;

    @Value("${file.extn}")
    private String[] fileExt;

    @PostMapping("/addPostWithFile")
    @Secured("user")
    @Operation(
            summary = "Create post with images",
            description = "Creates an inscription post from multipart metadata and one or more images. The server validates extension, image count, size, metadata, geolocation, perceptual hash data, and content moderation.",
            responses = @ApiResponse(responseCode = "200", description = "Post images uploaded",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"Images Uploaded Sucessfully\",\"http-status\":\"OK\",\"data\":true}"))))
    public ResponseEntity<?> addPostWithFile(
            @Parameter(description = "Post metadata JSON part.", required = false)
            @RequestPart(value = "post", required = false) InscriptionPostDto InscriptionPostDto,
            HttpServletRequest request,
            @Parameter(description = "Image files. Maximum 16 files, 75 MB each.", required = true)
             @RequestPart("files") MultipartFile... files) throws IOException {
        files = getNonEmptyFiles(files);

        if (files.length == 0) {
            throw new StoneInscriptionException("No File Uploaded", HttpStatus.BAD_REQUEST);
        }

        validateFiles(files, MAX_IMAGES_PER_POST);
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.addPostWithFile(InscriptionPostDto, files,
                jwtUtil.getUsernameFromToken(token));

    }

    @PostMapping("/getAllPost")
    @Secured("user")
    @Operation(
            summary = "List all visible posts",
            description = "Returns all posts with image identifiers expanded to public image URLs.",
            responses = @ApiResponse(responseCode = "200", description = "Posts fetched",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))))
    public ResponseEntity<?> getAllPost() {

        return postService.getAllPost();

    }

    @GetMapping("/public/images/{id}")
    @Secured("user")
    @Operation(
            summary = "Download post image",
            description = "Public endpoint that streams a stored inscription image by id.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Image stream",
                            content = @Content(mediaType = "image/*", schema = @Schema(type = "string", format = "binary"))),
                    @ApiResponse(responseCode = "404", description = "Image not found",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<InputStreamResource> getImage(@PathVariable String id) {

        return postService.getImages(id);

    }

    @PostMapping("/getAllUserPost")
    @Secured("user")
    @Operation(
            summary = "List my posts",
            description = "Returns posts created by the authenticated user with image URLs hydrated.",
            responses = @ApiResponse(responseCode = "200", description = "User posts fetched",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))))
    public ResponseEntity<?> getAllUserPost(HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.getAllUserPost(
                jwtUtil.getUsernameFromToken(token));

    }

    @PostMapping("/addPoastDiscription")
    @Secured("user")
    @Operation(
            summary = "Add post description",
            description = "Adds a user-authored description/comment to a post after content moderation. Endpoint spelling preserves the existing API contract.",
            responses = @ApiResponse(responseCode = "200", description = "Description added",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))))
    public ResponseEntity<?> addPoastDiscription(HttpServletRequest request,
            @Parameter(description = "Post id.", example = "665f1df013ad4e18f6a11244") @RequestParam("postId") String postId,
            @Parameter(description = "Description text.", example = "This line appears to mention a land grant.") @RequestParam("discription") String discription) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.addPoastDiscription(
                jwtUtil.getUsernameFromToken(token), postId, discription);

    }

    @PostMapping("/getPostDiscription")
    @Secured("user")
    @Operation(
            summary = "List post descriptions",
            description = "Returns all user descriptions/comments for a post. Endpoint spelling preserves the existing API contract.",
            responses = @ApiResponse(responseCode = "200", description = "Descriptions fetched",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))))
    public ResponseEntity<?> getPostDiscription(
            @Parameter(description = "Post id.", example = "665f1df013ad4e18f6a11244") @RequestParam("postId") String postId) {

        return postService.getPostDiscription(
                postId);

    }

    @PostMapping("/updatePostDiscription")
    @Secured("user")
    @Operation(
            summary = "Update post description",
            description = "Updates an authenticated user's own description/comment after moderation.",
            responses = @ApiResponse(responseCode = "200", description = "Description updated",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))))
    public ResponseEntity<?> updatePostDiscription(HttpServletRequest request,
            @Parameter(description = "Description id.", example = "665f1df013ad4e18f6a11247")
            @RequestParam("discriptionId") String discriptionId,
            @Parameter(description = "Updated description text.", example = "Updated historical reading.")
            @RequestParam("discription") String discription) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.updatePostDiscription(jwtUtil.getUsernameFromToken(token),
                discriptionId, discription);

    }

    @PostMapping("/addRating")
    @Secured("user")
    @Operation(
            summary = "Rate post",
            description = "Adds or updates the authenticated user's numeric rating for a post.",
            responses = @ApiResponse(responseCode = "200", description = "Rating added",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))))
    public ResponseEntity<?> addRating(HttpServletRequest request,
            @Parameter(description = "Post id.", example = "665f1df013ad4e18f6a11244") @RequestParam("postId") String postId,
            @Parameter(description = "Rating value.", example = "4.5") @RequestParam("rating") Double rating) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.addRating(jwtUtil.getUsernameFromToken(token),
                postId, rating);

    }

    @PostMapping("/addVote")
    @Secured("user")
    @Operation(
            summary = "Toggle description vote",
            description = "Adds an upvote when the authenticated user has not voted, or removes the existing vote.",
            responses = @ApiResponse(responseCode = "200", description = "Vote updated",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))))
    public ResponseEntity<?> addVote(HttpServletRequest request,
            @Parameter(description = "Description id.", example = "665f1df013ad4e18f6a11247") @RequestParam("descriptionId") String descriptionId) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.addVote(jwtUtil.getUsernameFromToken(token),
                descriptionId);

    }

    @PostMapping("/userProfile")
    @Secured("user")
    @Operation(
            summary = "Get current user entity profile",
            description = "Legacy post-module profile endpoint that returns the authenticated user object in the standard envelope.",
            responses = @ApiResponse(responseCode = "200", description = "User profile fetched",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))))
    public ResponseEntity<?> userProfile(HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.userProfile(jwtUtil.getUsernameFromToken(token));
    }

    @PostMapping("/postDelete")
    @Secured("user")
    @Operation(
            summary = "Delete my post",
            description = "Deletes a post owned by the authenticated user and archives related content according to the content delete service.",
            responses = @ApiResponse(responseCode = "200", description = "Post deleted",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))))
    public ResponseEntity<?> postDelete(HttpServletRequest request,
            @Parameter(description = "Post id.", example = "665f1df013ad4e18f6a11244") @RequestParam("postId") String postId) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.postDelete(jwtUtil.getUsernameFromToken(token), postId);

    }

    @PostMapping("/discriptionDelete")
    @Secured("user")
    @Operation(
            summary = "Delete my description",
            description = "Deletes a description/comment owned by the authenticated user. Endpoint spelling preserves the existing API contract.",
            responses = @ApiResponse(responseCode = "200", description = "Description deleted",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))))
    public ResponseEntity<?> descriptionDelete(HttpServletRequest request,
            @Parameter(description = "Description id.", example = "665f1df013ad4e18f6a11247") @RequestParam("descriptionId") String descriptionId) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.descriptionDelete(jwtUtil.getUsernameFromToken(token), descriptionId);

    }
// Updated this function for all the updation 
// Helps logged user to create a post and upload images 
    @PostMapping("/updatePost")
    @Secured("user")
    @Operation(
            summary = "Update post metadata and images",
            description = "Updates post metadata, removes selected images, adds new images, and enforces that at least one image remains and no more than 16 images are attached.",
            responses = @ApiResponse(responseCode = "200", description = "Post updated",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))))
    public ResponseEntity<?> updatePost(HttpServletRequest request,
            @Parameter(description = "Updated post metadata JSON part.", required = false)
            @RequestPart(value = "post", required = false) InscriptionPostDto InscriptionPostDto,
            @Parameter(description = "Post id.", example = "665f1df013ad4e18f6a11244")
            @RequestParam String postId,
            @Parameter(description = "Image ids to remove from the post.", example = "[\"665f1df013ad4e18f6a11249\"]")
            @RequestParam(value = "deletedImageIds", required = false) List<String> deletedImageIds,
            @Parameter(description = "New image files to add.", required = false)
            @RequestPart(value = "files", required = false) MultipartFile... files) {

        files = getNonEmptyFiles(files);
        validateFiles(files, MAX_IMAGES_PER_POST);

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.updatePost(jwtUtil.getUsernameFromToken(token), InscriptionPostDto, postId,
                deletedImageIds, files);

    }

    @PostMapping("/addImagesToPost")
    @Secured("user")
    @Operation(
            summary = "Add images to post",
            description = "Adds one or more images to an owned post while enforcing extension, size, and max image-count rules.",
            responses = @ApiResponse(responseCode = "200", description = "Images added",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))))
    public ResponseEntity<?> addImagesToPost(HttpServletRequest request,
            @Parameter(description = "Post id.", example = "665f1df013ad4e18f6a11244")
            @RequestParam String postId,
            @Parameter(description = "Image files. Maximum 16 images per post total.", required = true)
            @RequestPart("files") MultipartFile... files) {

        files = getNonEmptyFiles(files);

        if (files.length == 0) {
            throw new StoneInscriptionException("No File Uploaded", HttpStatus.BAD_REQUEST);
        }

        validateFiles(files, MAX_IMAGES_PER_POST);

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.addImagesToPost(jwtUtil.getUsernameFromToken(token), postId, files);
    }

    @PostMapping("/deleteImagesFromPost")
    @Secured("user")
    @Operation(
            summary = "Delete post images",
            description = "Deletes selected images from an owned post while ensuring the post still has at least one image.",
            responses = @ApiResponse(responseCode = "200", description = "Images deleted",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))))
    public ResponseEntity<?> deleteImagesFromPost(HttpServletRequest request,
            @Parameter(description = "Post id.", example = "665f1df013ad4e18f6a11244")
            @RequestParam String postId,
            @Parameter(description = "Image ids to delete.", example = "[\"665f1df013ad4e18f6a11249\"]")
            @RequestParam(value = "deletedImageIds") List<String> deletedImageIds) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.deleteImagesFromPost(jwtUtil.getUsernameFromToken(token), postId, deletedImageIds);
    }

    // ============================
    // TEST-ONLY ENDPOINTS (NO JWT)
    // TODO: Remove these methods when testing is done to restore normal behavior.
    // ============================

    // @PostMapping("/test/updatePost/{email}")
    // public ResponseEntity<?> updatePostForTest(
    //         @PathVariable String email,
    //         @RequestPart(value = "post", required = false) InscriptionPostDto InscriptionPostDto,
    //         @RequestParam String postId,
    //         @RequestParam(value = "deletedImageIds", required = false) List<String> deletedImageIds,
    //         @RequestPart(value = "files", required = false) MultipartFile... files) {

    //     files = getNonEmptyFiles(files);
    //     validateFiles(files, MAX_IMAGES_PER_POST);

    //     return postService.updatePost(email, InscriptionPostDto, postId, deletedImageIds, files);
    // }

    @PostMapping("/test/addPostWithFile/{email}")
    @Hidden
    public ResponseEntity<?> addPostWithFileForTest(
            @PathVariable String email,
            @RequestPart(value = "post", required = false) InscriptionPostDto InscriptionPostDto,
            @RequestPart("files") MultipartFile... files) throws IOException {

        files = getNonEmptyFiles(files);

        if (files.length == 0) {
            throw new StoneInscriptionException("No File Uploaded", HttpStatus.BAD_REQUEST);
        }

        validateFiles(files, MAX_IMAGES_PER_POST);

        return postService.addPostWithFile(InscriptionPostDto, files, email);
    }

    // @PostMapping("/test/addPoastDiscription/{email}")
    // public ResponseEntity<?> addPoastDiscriptionForTest(
    //     @PathVariable String email,
    //     @RequestParam("postId") String postId,
    //     @RequestParam("discription") String discription) {

    // return postService.addPoastDiscription(email, postId, discription);
    // }


    // @PostMapping("/test/addImagesToPost/{email}")
    // public ResponseEntity<?> addImagesToPostForTest(
    //         @PathVariable String email,
    //         @RequestParam String postId,
    //         @RequestPart("files") MultipartFile... files) {

    //     files = getNonEmptyFiles(files);

    //     if (files.length == 0) {
    //         throw new StoneInscriptionException("No File Uploaded", HttpStatus.BAD_REQUEST);
    //     }

    //     validateFiles(files, MAX_IMAGES_PER_POST);

    //     return postService.addImagesToPost(email, postId, files);
    // }

    // @PostMapping("/test/deleteImagesFromPost/{email}")
    // public ResponseEntity<?> deleteImagesFromPostForTest(
    //         @PathVariable String email,
    //         @RequestParam String postId,
    //         @RequestParam(value = "deletedImageIds") List<String> deletedImageIds) {

    //     return postService.deleteImagesFromPost(email, postId, deletedImageIds);
    // }

    // @PostMapping("/test/postDelete/{email}")
    // public ResponseEntity<?> postDeleteForTest(
    //         @PathVariable String email,
    //         @RequestParam String postId) {

    //     return postService.postDelete(email, postId);
    // }


//  end of test api's 
    private MultipartFile[] getNonEmptyFiles(MultipartFile[] files) {
        if (files == null) {
            return new MultipartFile[0];
        }

        return Arrays.stream(files)
                .filter(file -> file != null && !file.isEmpty())
                .toArray(MultipartFile[]::new);
    }

    private void validateFiles(MultipartFile[] files, int maxFilesAllowed) {
        if (files.length > maxFilesAllowed) {
            throw new StoneInscriptionException("Maximum " + maxFilesAllowed + " images are allowed per post",
                    HttpStatus.BAD_REQUEST);
        }

        Arrays.stream(files).forEach(file -> {
            String originalFilename = file.getOriginalFilename();
            fileValidationService.getFileExtension(originalFilename);

            if (file.getSize() > fileValidationService.getMaxImageSizeBytes()) {
                throw new StoneInscriptionException("Each image size should be less than or equal to 75 MB",
                        HttpStatus.BAD_REQUEST);
            }
        });
    }

    @PostMapping("/getCommentByUser")
    // @Secured("user")
    @Operation(
            summary = "List my descriptions",
            description = "Returns descriptions/comments authored by the authenticated user with post preview image URLs.",
            responses = @ApiResponse(responseCode = "200", description = "User descriptions fetched",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))))
    public ResponseEntity<?> getCommentByUser(HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.getCommentByUser(jwtUtil.getUsernameFromToken(token));

    }

    @GetMapping("/public/getDashboardCounts")
    // @Secured("user")
    @Operation(
            summary = "Get public dashboard counts",
            description = "Returns public aggregate counts used by the dashboard.",
            responses = @ApiResponse(responseCode = "200", description = "Dashboard counts fetched",
                    content = @Content(schema = @Schema(implementation = DashboardCountsResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"Dashboard Counts\",\"http-status\":\"OK\",\"data\":{\"totalUsers\":128,\"totalPosts\":42,\"totalImages\":280,\"totalGeoTaggedPosts\":31,\"totalTranslations\":17}}"))))
    public ResponseEntity<?> getDashboardCounts() {

        return postService.getDashboardCounts();

    }

}
