package com.cadac.stone_inscription.post.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.annotation.Secured;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.cadac.stone_inscription.auth.JwtUtil;
import com.cadac.stone_inscription.post.dto.InscriptionPostDto;
import com.cadac.stone_inscription.post.service.PostService;

class PostControllerTests {

    @Test
    void addPoastDiscriptionRequiresUserRole() throws NoSuchMethodException {
        Method method = PostController.class.getMethod(
                "addPoastDiscription",
                jakarta.servlet.http.HttpServletRequest.class,
                String.class,
                String.class);

        Secured secured = method.getAnnotation(Secured.class);

        assertArrayEquals(new String[] { "user" }, secured.value());
    }

    @Test
    void addPoastDiscriptionPassesDecodedUserAndCommentToService() throws Exception {
        PostController controller = new PostController();
        TrackingPostService postService = new TrackingPostService();
        JwtUtil jwtUtil = newJwtUtil();

        ReflectionTestUtils.setField(controller, "postService", postService);
        ReflectionTestUtils.setField(controller, "jwtUtil", jwtUtil);

        String token = jwtUtil.doGenerateToken(java.util.Map.of("user", "tester@example.com", "role", "user"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        ResponseEntity<?> expectedResponse = ResponseEntity.ok("saved");
        postService.addDescriptionResponse = expectedResponse;

        ResponseEntity<?> response = controller.addPoastDiscription(
                request,
                "680f6c0b5a4e3b2d1c9f1234",
                "Public description for the post");

        assertSame(expectedResponse, response);
        assertEquals("tester@example.com", postService.usernameFromToken);
        assertEquals("680f6c0b5a4e3b2d1c9f1234", postService.postId);
        assertEquals("Public description for the post", postService.description);
    }

    private JwtUtil newJwtUtil() throws Exception {
        Constructor<JwtUtil> constructor = JwtUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static class TrackingPostService implements PostService {
        private ResponseEntity<?> addDescriptionResponse;
        private String usernameFromToken;
        private String postId;
        private String description;

        @Override
        public ResponseEntity<?> addPoastDiscription(String usernameFromToken, String postId, String discription) {
            this.usernameFromToken = usernameFromToken;
            this.postId = postId;
            this.description = discription;
            return addDescriptionResponse;
        }

        @Override
        public ResponseEntity<?> addPostWithFile(InscriptionPostDto inscriptionPostDto, MultipartFile[] files,
                String usernameFromToken) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResponseEntity<?> getAllPost() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResponseEntity<InputStreamResource> getImages(String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResponseEntity<?> getAllUserPost(String usernameFromToken) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResponseEntity<?> getPostDiscription(String usernameFromToken) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResponseEntity<?> updatePostDiscription(String request, String postId, String discription) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResponseEntity<?> addRating(String usernameFromToken, String postId, Double rating) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResponseEntity<?> addVote(String usernameFromToken, String descriptionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResponseEntity<?> userProfile(String usernameFromToken) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResponseEntity<?> postDelete(String usernameFromToken, String postId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResponseEntity<?> descriptionDelete(String usernameFromToken, String descriptionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResponseEntity<?> updatePost(String usernameFromToken, InscriptionPostDto inscriptionPostDto,
                String postId, List<String> deletedImageIds, MultipartFile[] files) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResponseEntity<?> addImagesToPost(String usernameFromToken, String postId, MultipartFile[] files) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResponseEntity<?> deleteImagesFromPost(String usernameFromToken, String postId,
                List<String> deletedImageIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResponseEntity<?> getCommentByUser(String usernameFromToken) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResponseEntity<?> getDashboardCounts() {
            throw new UnsupportedOperationException();
        }
    }
}
