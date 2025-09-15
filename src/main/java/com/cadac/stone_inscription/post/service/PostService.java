package com.cadac.stone_inscription.post.service;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.cadac.stone_inscription.post.dto.InscriptionPostDto;

import jakarta.servlet.http.HttpServletRequest;

public interface PostService {

    ResponseEntity<?> addPostWithFile(InscriptionPostDto inscriptionPostDto, MultipartFile[] files,
            String usernameFromToken);

    ResponseEntity<?> getAllPost();

    ResponseEntity<InputStreamResource> getImages(String id);

    ResponseEntity<?> getAllUserPost(String usernameFromToken);

    ResponseEntity<?> addPoastDiscription(String usernameFromToken, String postId, String discription);

    ResponseEntity<?> getPostDiscription(String usernameFromToken);

    ResponseEntity<?> updatePostDiscription(String request, String postId, String discription);

    ResponseEntity<?> addRating(String usernameFromToken, String postId, Double rating);

   

    ResponseEntity<?> addVote(String usernameFromToken, String descriptionId);

    ResponseEntity<?> userProfile(String usernameFromToken);

    ResponseEntity<?> postDelete(String usernameFromToken, String postId);

    ResponseEntity<?> descriptionDelete(String usernameFromToken, String descriptionId);

    ResponseEntity<?> updatePost(String usernameFromToken, InscriptionPostDto inscriptionPostDto, String postId);

    ResponseEntity<?> getCommentByUser(String usernameFromToken);

    ResponseEntity<?> getDashboardCounts();

}
