# API Documentation for Stone Inscription Application

This document outlines the API endpoints of the Stone Inscription application, organized by Functional Requirements (FR) and Non-Functional Requirements (NFR). Each FR/NFR includes the responsible APIs with brief descriptions.

## Functional Requirements

### FR1: User Authentication and Authorization
Responsible for handling user login, logout, token management, and session tracking.

- **FR1.1: User Logout**  
  API: `POST /oauth2/logout`  
  Details: Logs out the authenticated user by invalidating tokens and clearing session data.

- **FR1.2: Token Refresh**  
  API: `POST /oauth2/authenticated/refresh-token`  
  Details: Refreshes the JWT access token using a valid refresh token to maintain user session.

- **FR1.3: Update User Activity**  
  API: `POST /oauth2/authenticated/active`  
  Details: Updates the user's last active timestamp to track session activity.

### FR2: User Profile Management
Handles user profile retrieval, updates, and image uploads.

- **FR2.1: Retrieve User Profile**  
  API: `GET /user/profile`  
  Details: Fetches the current authenticated user's profile information.

- **FR2.2: Update User Profile**  
  API: `POST /user/updateProfile`  
  Details: Updates the user's profile details, such as username.

- **FR2.3: Upload Profile Image**  
  API: `POST /user/uploadProfileImage`  
  Details: Uploads or updates the user's profile picture.

- **FR2.4: Upload Cover Image**  
  API: `POST /user/uploadCoverImage`  
  Details: Uploads or updates the user's cover image.

- **FR2.5: Retrieve User Images**  
  API: `GET /user/public/images/{id}`  
  Details: Publicly accessible endpoint to retrieve user images (profile or cover) by ID.

### FR3: Post Management
Manages creation, retrieval, and deletion of inscription posts.

- **FR3.1: Create Post with Files**  
  API: `POST /post/addPostWithFile`  
  Details: Creates a new inscription post with attached image files.

- **FR3.2: Retrieve All Posts**  
  API: `POST /post/getAllPost`  
  Details: Fetches all available inscription posts.

- **FR3.3: Retrieve User's Posts**  
  API: `POST /post/getAllUserPost`  
  Details: Fetches all posts created by the authenticated user.

- **FR3.4: Delete Post**  
  API: `POST /post/postDelete`  
  Details: Deletes a specific post by the authenticated user.

- **FR3.5: Retrieve Post Images**  
  API: `GET /post/public/images/{id}`  
  Details: Publicly accessible endpoint to retrieve post images by ID.

### FR4: Post Description Management
Handles adding, retrieving, updating, and deleting descriptions for posts.

- **FR4.1: Add Post Description**  
  API: `POST /post/addPoastDiscription`  
  Details: Adds a description to a specific post.

- **FR4.2: Retrieve Post Description**  
  API: `POST /post/getPostDiscription`  
  Details: Fetches the description of a specific post.

- **FR4.3: Update Post Description**  
  API: `POST /post/updatePostDiscription`  
  Details: Updates an existing description for a post.

- **FR4.4: Delete Post Description**  
  API: `POST /post/discriptionDelete`  
  Details: Deletes a specific description from a post.

### FR5: Post Interactions
Manages user interactions like ratings and votes on posts.

- **FR5.1: Add Rating to Post**  
  API: `POST /post/addRating`  
  Details: Allows users to rate a post with a numerical value.

- **FR5.2: Add Vote to Description**  
  API: `POST /post/addVote`  
  Details: Allows users to vote on a post description.

### FR6: User Profile in Context
Provides user profile information within the post context.

- **FR6.1: Retrieve User Profile for Posts**  
  API: `POST /post/userProfile`  
  Details: Fetches the authenticated user's profile information in the context of post operations.

## Non-Functional Requirements

### NFR1: Error Handling
Ensures consistent error responses across the application.

- **NFR1.1: Custom Exception Handling**  
  API: Global exception handlers in `ExceptionController`  
  Details: Handles `StoneInscriptionException` and validation errors, providing standardized error responses with HTTP status codes.

- **NFR1.2: Validation Error Handling**  
  API: Global exception handlers in `ExceptionController`  
  Details: Manages `MethodArgumentNotValidException` for request body validation, returning field-specific error messages.

### NFR2: Security
Implements authentication and authorization mechanisms.

- **NFR2.1: JWT Token Validation**  
  API: Applied via `JwtRequestFilter` and `@Secured` annotations  
  Details: Validates JWT tokens on protected endpoints, ensuring only authenticated users can access secured resources.

- **NFR2.2: OAuth2 Integration**  
  API: Handled by Spring Security OAuth2 configuration  
  Details: Supports OAuth2 login flows (e.g., Google), with success handlers managing user authentication and registration.

- **NFR2.3: CSRF Protection**  
  API: Implicit in Spring Security configuration  
  Details: Protects against Cross-Site Request Forgery attacks on state-changing operations.

### NFR3: Performance and Scalability
Addresses response times and resource handling.

- **NFR3.1: File Upload Validation**  
  API: Validation in `PostController.addPostWithFile`  
  Details: Validates file types and sizes during upload to prevent resource exhaustion.

- **NFR3.2: Image Serving Optimization**  
  API: `GET /post/public/images/{id}` and `GET /user/public/images/{id}`  
  Details: Serves images efficiently as `InputStreamResource` for public access without authentication overhead.