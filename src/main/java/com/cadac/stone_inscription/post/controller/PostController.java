package com.cadac.stone_inscription.post.controller;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cadac.stone_inscription.auth.JwtUtil;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.post.service.PostService;
import com.cadac.stone_inscription.post.dto.InscriptionPostDto;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/post")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${file.extn}")
    private String[] fileExt;

    @PostMapping("/addPostWithFile")
    @Secured("user")
    public ResponseEntity<?> addPostWithFile(
            @RequestPart(value = "post", required = false) InscriptionPostDto InscriptionPostDto,
            HttpServletRequest request,
             @RequestPart("files") MultipartFile... files) throws IOException {
if (files == null || files.length == 0) {
            throw new StoneInscriptionException("No File Uploaded", HttpStatus.BAD_REQUEST);
        }

        Arrays.stream(files).forEach(file -> {

            if (!Arrays.stream(fileExt).anyMatch((ext) -> file.getOriginalFilename().endsWith(ext))) {
                throw new StoneInscriptionException("Invalid File format only allowed" + Arrays.toString(fileExt),
                        HttpStatus.BAD_REQUEST);
            }

        });
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.addPostWithFile(InscriptionPostDto, files,
                jwtUtil.getUsernameFromToken(token));

    }

    @PostMapping("/getAllPost")
    @Secured("user")
    public ResponseEntity<?> getAllPost() {

        return postService.getAllPost();

    }

    @GetMapping("/public/images/{id}")
    public ResponseEntity<InputStreamResource> getImage(@PathVariable String id) {

        return postService.getImages(id);

    }

    @PostMapping("/getAllUserPost")
    @Secured("user")
    public ResponseEntity<?> getAllUserPost(HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.getAllUserPost(
                jwtUtil.getUsernameFromToken(token));

    }

    @PostMapping("/addPoastDiscription")
    // @Secured("user")
    public ResponseEntity<?> addPoastDiscription(HttpServletRequest request, String postId, String discription) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.addPoastDiscription(
                jwtUtil.getUsernameFromToken(token), postId, discription);

    }

    @PostMapping("/getPostDiscription")
    // @Secured("user")
    public ResponseEntity<?> getPostDiscription(String postId) {

        return postService.getPostDiscription(
                postId);

    }

    @PostMapping("/updatePostDiscription")
    // @Secured("user")
    public ResponseEntity<?> updatePostDiscription(HttpServletRequest request, String discriptionId,
            String discription) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.updatePostDiscription(jwtUtil.getUsernameFromToken(token),
                discriptionId, discription);

    }

    @PostMapping("/addRating")
    // @Secured("user")
    public ResponseEntity<?> addRating(HttpServletRequest request, String postId, Double rating) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.addRating(jwtUtil.getUsernameFromToken(token),
                postId, rating);

    }

    @PostMapping("/addVote")
    // @Secured("user")
    public ResponseEntity<?> addVote(HttpServletRequest request, String descriptionId) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.addVote(jwtUtil.getUsernameFromToken(token),
                descriptionId);

    }

    @PostMapping("/userProfile")
    // @Secured("user")
    public ResponseEntity<?> userProfile(HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.userProfile(jwtUtil.getUsernameFromToken(token));
    }

    @PostMapping("/postDelete")
    // @Secured("user")
    public ResponseEntity<?> postDelete(HttpServletRequest request, String postId) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.postDelete(jwtUtil.getUsernameFromToken(token), postId);

    }

    @PostMapping("/discriptionDelete")
    // @Secured("user")
    public ResponseEntity<?> descriptionDelete(HttpServletRequest request, String descriptionId) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.descriptionDelete(jwtUtil.getUsernameFromToken(token), descriptionId);

    }

    @PostMapping("/updatePost")
    // @Secured("user")
    public ResponseEntity<?> updatePost(HttpServletRequest request,
            @RequestPart(value = "post") InscriptionPostDto InscriptionPostDto, String postId) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.updatePost(jwtUtil.getUsernameFromToken(token), InscriptionPostDto, postId);

    }

    @PostMapping("/getCommentByUser")
    // @Secured("user")
    public ResponseEntity<?> getCommentByUser(HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return postService.getCommentByUser(jwtUtil.getUsernameFromToken(token));

    }

    @GetMapping("/public/getDashboardCounts")
    // @Secured("user")
    public ResponseEntity<?> getDashboardCounts() {

        return postService.getDashboardCounts();

    }

}
