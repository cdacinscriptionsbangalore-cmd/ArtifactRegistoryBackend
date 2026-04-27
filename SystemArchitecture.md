# System Architecture Documentation
## Artifact Registry Backend

---

## Table of Contents
1. [System Architecture](#1-system-architecture)
   - 1.1 [System Software Layer](#11-system-software-layer)
   - 1.2 [Audit](#12-audit)
   - 1.3 [Security](#13-security)
   - 1.4 [Components Interface Design](#14-components-interface-design)

---

## 1. System Architecture

### Overview
The Artifact Registry Backend is a modern, microservices-ready Spring Boot 3.5.5 application built with Java 17. The system is designed to manage artifact inscriptions with multi-layered architecture supporting authentic user authentication, content moderation, and comprehensive security controls.

**Core Technologies:**
- **Framework:** Spring Boot 3.5.5
- **Language:** Java 17
- **Database:** MongoDB (with Auditing enabled)
- **Authentication:** OAuth2 (Google, Facebook, Apple) + JWT
- **Monitoring:** Prometheus, Micrometer, Spring Actuator
- **Containerization:** Docker, Docker Compose

---

## 1.1 System Software Layer

### 1.1.1 Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                    API Layer                            │
│           (REST Endpoints via Controllers)              │
├─────────────────────────────────────────────────────────┤
│                 Business Logic Layer                    │
│              (Services & Processors)                    │
├─────────────────────────────────────────────────────────┤
│              Data Access Layer                          │
│            (Repositories & DAOs)                        │
├─────────────────────────────────────────────────────────┤
│                 Database Layer                          │
│               (MongoDB Collections)                     │
└─────────────────────────────────────────────────────────┘
```

### 1.1.2 Component Structure

#### **Authentication & Authorization Layer**
- **JwtUtil:** JWT token generation, validation, and claims management using Nimbus JOSE
- **JwtAuthenticationEntryPoint:** Entry point for unauthorized requests
- **JwtRequestFilter:** Filter chain processor for JWT validation on each request
- **CustomOAuth2SuccessHandler:** Handles successful OAuth2 authentication flows
- **StoneInscriptionUserDetailservice:** Custom UserDetails service for user information loading

#### **Core Application Components**

**1. User Management Module** (`user/`)
- User profile management and retrieval
- Profile image and cover image uploads
- User activity tracking
- Components:
  - Controllers: Handle HTTP requests for user operations
  - Services: Business logic for user operations
  - DTOs: Data transfer objects for API communication

**2. Post Management Module** (`post/`)
- Create, retrieve, update, and delete posts (inscriptions)
- Post image management
- Post description handling with versioning
- Components:
  - Controllers: HTTP endpoints for post operations
  - Services: Business logic for post management
  - Mappers: Entity-DTO conversions
  - DTOs: Data structures for API communication

**3. Moderation Module** (`moderation/`)
- Content moderation and validation
- External moderation service integration
- Flag inappropriate content
- Components:
  - Client: External API communication
  - Config: Moderation configuration
  - Service: Moderation business logic
  - Model: Moderation data structures
  - DTO: Data transfer objects

**4. Entity Layer** (`entity/`)
- **User:** User profile information
- **UserAuth:** Authentication credentials and OAuth provider details
- **InscriptionPost:** Post/inscription content
- **PublicPostDescription:** Post descriptions with versioning
- **UserImage:** User profile and cover images
- **ImagesData:** Image metadata and storage information

#### **Cross-Cutting Concerns**

**Exception Handling Layer** (`exception/`)
- **ExceptionController:** REST endpoint for error responses
- **ExceptionHandlerFilter:** Global exception handling filter
- **StoneInscriptionException:** Custom exception type
- Provides unified error response format

**Configuration Layer** (`configuration/`)
- **StoneinscriptionConfiguration:** Main security and application configuration
  - Security filter chain setup
  - OAuth2 provider configuration (Google, Facebook, Apple)
  - CORS configuration
  - Password encoding setup
  - MongoDB Auditing activation

**Utility Layer** (`util/`)
- Common utility functions
- Helper methods for complex operations

### 1.1.3 External Integrations

**OAuth2 Providers:**
1. **Google OAuth2**
   - Authorization URI: `https://accounts.google.com/o/oauth2/auth`
   - Token URI: `https://oauth2.googleapis.com/token`
   - User Info: `https://openidconnect.googleapis.com/v1/userinfo`

2. **Facebook OAuth2**
   - Authorization URI: `https://www.facebook.com/v18.0/dialog/oauth`
   - Token URI: `https://graph.facebook.com/v18.0/oauth/access_token`
   - User Info: `https://graph.facebook.com/v18.0/me`

3. **Apple OAuth2**
   - Authorization URI: `https://appleid.apple.com/auth/authorize`
   - Token URI: `https://appleid.apple.com/auth/token`

**Monitoring & Observability:**
- **Prometheus Metrics:** Application metrics exposed at `/actuator/prometheus`
- **Health Checks:** `/actuator/health`
- **App Info:** `/actuator/info`
- **HikariCP Pool Monitoring:** Database connection pool metrics

### 1.1.4 Request Processing Flow

```
HTTP Request
    ↓
CORS Filter
    ↓
Exception Handler Filter
    ↓
JWT Request Filter (Validation)
    ↓
Security Filter Chain
    ↓
Controller (endpoint handling)
    ↓
Service Layer (business logic)
    ↓
Repository Layer (data access)
    ↓
MongoDB (persistence)
    ↓
Response Processing & Return
```

---

## 1.2 Audit

### 1.2.1 MongoDB Auditing Configuration

The system implements comprehensive audit trails using Spring Data MongoDB's `@EnableMongoAuditing` feature:

```java
@EnableMongoAuditing
```

**Audit Capabilities:**
- Automatic timestamp tracking for entity creation (`@CreatedDate`)
- Automatic timestamp tracking for entity modifications (`@LastModifiedDate`)
- User tracking for creation (`@CreatedBy`)
- User tracking for modifications (`@LastModifiedBy`)

### 1.2.2 Auditable Entities

**User Entity**
- Creation timestamp
- Last modification timestamp
- Created by username
- Last modified by username

**InscriptionPost Entity**
- Creation timestamp
- Last modification timestamp
- Created by user
- Last modified by user
- Post deletion audit trail

**PublicPostDescription Entity**
- Version tracking
- Description history
- Created by user
- Modification history

**UserImage Entity**
- Image upload timestamp
- Image modification timestamp
- User association

### 1.2.3 Activity Tracking

**User Activity Logging**
- Endpoint: `POST /oauth2/authenticated/active`
- Tracks: Last active timestamp for each user session
- Purpose: Monitor user engagement and session validity

### 1.2.4 Post Lifecycle Tracking

**Post Creation Audit Trail**
- Timestamp of post creation
- Creator (user ID/username)
- Initial post content

**Post Modification Audit Trail**
- Timestamp of modification
- Modified by (user ID/username)
- Description version history

**Post Deletion Audit Trail**
- Timestamp of deletion
- Deleted by (user ID/username)
- Soft/hard delete tracking

### 1.2.5 Image Upload Auditing

**Upload Events**
- User profile image uploads tracked
- Cover image uploads tracked
- Timestamp and user association

### 1.2.6 Authentication Auditing

**OAuth2 Login Events**
- Provider (Google/Facebook/Apple)
- Timestamp of login
- User information captured
- Token generation records

**JWT Token Usage**
- Token generation timestamp
- Token expiration
- Token refresh events

---

## 1.3 Security

### 1.3.1 Authentication Architecture

#### **Multi-Provider OAuth2 Authentication**

The system implements a sophisticated OAuth2 authentication mechanism supporting three major providers:

```
User
  ↓
Browser/Client
  ↓
OAuth2 Provider (Google/Facebook/Apple)
  ↓
Authorization Code Flow
  ↓
CustomOAuth2SuccessHandler
  ↓
JWT Token Generation
  ↓
Application
```

**OAuth2 Flow:**
1. User initiates login via OAuth2 provider
2. User authenticates with provider (Google, Facebook, or Apple)
3. Authorization code returned to application
4. Application exchanges code for access token
5. Application retrieves user information
6. `CustomOAuth2SuccessHandler` processes successful authentication
7. JWT token generated for stateless API authentication

#### **JWT (JSON Web Token) Implementation**

**JWT Components:**
- **Algorithm:** HS256 (HMAC with SHA-256)
- **Key Generation:** 256-bit secure random key using `SecureRandom`
- **Key Encoding:** Base64 encoded for transmission
- **Signing:** Using Nimbus JOSE library

**JWT Structure:**
```
Header.Payload.Signature

Header: 
  {
    "alg": "HS256",
    "typ": "JWT"
  }

Payload (Claims):
  {
    "sub": "user-id",
    "username": "username",
    "iat": 1234567890,
    "exp": 1234571490,
    "authorities": ["ROLE_USER"],
    "provider": "GOOGLE|FACEBOOK|APPLE"
  }

Signature: HMAC-SHA256(base64(header) + "." + base64(payload), secret)
```

**Token Lifecycle:**
- **Generation:** During successful OAuth2 authentication
- **Refresh:** Using `/oauth2/authenticated/refresh-token` endpoint
- **Validation:** On each API request via `JwtRequestFilter`
- **Expiration:** Based on configured TTL

### 1.3.2 Authorization & Access Control

#### **Method-Level Security**

The configuration enables three levels of authorization:

```java
@EnableMethodSecurity(
  prePostEnabled = true,      // @PreAuthorize, @PostAuthorize
  securedEnabled = true,      // @Secured
  jsr250Enabled = true        // @RolesAllowed
)
```

**Security Annotations Used:**
- `@PreAuthorize`: Pre-execution authorization checks
- `@PostAuthorize`: Post-execution authorization checks
- `@Secured`: Role-based access control
- `@RolesAllowed`: JSR-250 role annotations

#### **Request-Level Security**

```java
SecurityFilterChain:
  - CORS enabled
  - CSRF disabled (stateless JWT authentication)
  - Session policy: STATELESS
  - Authorization rules:
    * Public endpoints: /login/**, /post/public/**, /user/public/**
    * Authenticated required: /oauth2/authenticated/**, /user/**, /post/**
    * Admin endpoints: (configurable via annotations)
```

### 1.3.3 Session Management

**Session Creation Policy: STATELESS**
- No server-side session storage
- Each request contains JWT token
- Reduces server memory footprint
- Improves scalability and distributed deployment support

**CSRF Protection:**
- Disabled (appropriate for stateless JWT authentication)
- CSRF tokens not required for API requests

### 1.3.4 Password Security

**Password Encoding:**
- Algorithm: BCrypt (industry standard)
- Cost factor: 12 (default Spring Security)
- Automatically hashes passwords
- Prevents password plaintext storage

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### 1.3.5 CORS (Cross-Origin Resource Sharing)

**Configuration:**
- Configurable origin URL via `app.cors.url` property
- Supports:
  - Allowed origins
  - Allowed HTTP methods
  - Allowed headers
  - Credentials support
  - Max age for preflight cache

**Purpose:**
- Prevents unauthorized cross-domain requests
- Protects against CORS-based attacks

### 1.3.6 Data Validation & Input Sanitization

**Exception Handling Filter:**
- Captures and validates all exceptions
- Returns sanitized error messages
- Prevents information leakage
- Implements custom exception handling

### 1.3.7 API Endpoint Security

#### **Public Endpoints:**
```
GET  /post/public/images/{id}         - Retrieve post images
GET  /user/public/images/{id}         - Retrieve user images
POST /oauth2/login/**                 - OAuth2 login initiation
```

#### **Authenticated Endpoints:**
```
POST /oauth2/authenticated/refresh-token      - JWT refresh
POST /oauth2/authenticated/active             - Activity tracking
POST /oauth2/logout                           - Logout

GET  /user/profile                            - User profile retrieval
POST /user/updateProfile                      - Profile update
POST /user/uploadProfileImage                 - Profile image upload
POST /user/uploadCoverImage                   - Cover image upload

POST /post/addPostWithFile                    - Create post
POST /post/getAllPost                         - List all posts
POST /post/getAllUserPost                     - List user posts
POST /post/postDelete                         - Delete post
POST /post/addPoastDiscription               - Add description
POST /post/getPostDiscription                - Get description
POST /post/updatePostDiscription             - Update description
POST /post/discriptionDelete                 - Delete description
POST /post/addRating                         - Rate post
POST /post/addVote                           - Vote on description
```

### 1.3.8 Security Headers & Filters

**Security Filter Chain:**
1. **ExceptionHandlerFilter:** Catches and handles security exceptions
2. **JwtRequestFilter:** Validates JWT tokens
3. **JwtAuthenticationEntryPoint:** Handles authentication failures

### 1.3.9 Third-Party Secret Management

**Environment Variables (Configuration):**
```
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
FACEBOOK_CLIENT_ID
FACEBOOK_CLIENT_SECRET
APPLE_CLIENT_ID
APPLE_CLIENT_SECRET
DATABASE_URL
JWT_SECRET_KEY
```

**Storage:** `.env` file loaded via Spring configuration (excluded from version control)

### 1.3.10 Security Best Practices

| Practice | Implementation |
|----------|-----------------|
| **Stateless Authentication** | JWT tokens instead of server-side sessions |
| **Strong Encryption** | BCrypt for passwords, HS256 for JWT signing |
| **Secure Defaults** | HTTPS-ready, CSRF enabled where applicable |
| **Input Validation** | Request validation via filters and controllers |
| **Error Handling** | Sanitized error messages without stack traces |
| **CORS Protection** | Configurable allowed origins |
| **Secret Management** | Environment variables for sensitive data |
| **Audit Trails** | MongoDB auditing on all entities |
| **Session Timeout** | JWT expiration built-in |
| **Rate Limiting** | (Configurable via properties) |

---

## 1.4 Components Interface Design

### 1.4.1 API Contract Specifications

#### **Authentication Endpoints**

**1. OAuth2 Login Initiation**
```
Endpoint: POST /oauth2/authorize/{provider}
Provider: google | facebook | apple
Response: Redirect to OAuth2 provider authorization URL
```

**2. OAuth2 Callback with Authorization Code**
```
Endpoint: POST /oauth2/code/{registrationId}
Parameters: 
  - code: OAuth2 authorization code
  - state: CSRF protection state
Response:
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "refresh_token_value",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "user-id",
      "username": "username",
      "email": "user@example.com",
      "provider": "GOOGLE"
    }
  }
```

**3. Token Refresh**
```
Endpoint: POST /oauth2/authenticated/refresh-token
Headers: Authorization: Bearer {refreshToken}
Response: 
  {
    "accessToken": "new_jwt_token",
    "expiresIn": 3600
  }
```

**4. User Activity Update**
```
Endpoint: POST /oauth2/authenticated/active
Headers: Authorization: Bearer {accessToken}
Response: 
  {
    "lastActive": "2024-04-11T10:30:00Z",
    "status": "success"
  }
```

**5. Logout**
```
Endpoint: POST /oauth2/logout
Headers: Authorization: Bearer {accessToken}
Response: 
  {
    "message": "Logout successful",
    "status": "success"
  }
```

#### **User Profile Endpoints**

**1. Get User Profile**
```
Endpoint: GET /user/profile
Headers: Authorization: Bearer {accessToken}
Response:
  {
    "id": "user-id",
    "username": "john_doe",
    "email": "john@example.com",
    "profileImageId": "image-id-1",
    "coverImageId": "image-id-2",
    "createdAt": "2024-01-15T00:00:00Z",
    "lastModifiedAt": "2024-04-10T00:00:00Z"
  }
```

**2. Update User Profile**
```
Endpoint: POST /user/updateProfile
Headers: Authorization: Bearer {accessToken}
Content-Type: application/json
Body:
  {
    "username": "new_username",
    "bio": "User bio",
    "location": "City, Country"
  }
Response:
  {
    "id": "user-id",
    "username": "new_username",
    "message": "Profile updated successfully"
  }
```

**3. Upload Profile Image**
```
Endpoint: POST /user/uploadProfileImage
Headers: Authorization: Bearer {accessToken}
Content-Type: multipart/form-data
Body: 
  {
    "file": <binary_image_data>
  }
Response:
  {
    "imageId": "image-id-1",
    "fileName": "profile.jpg",
    "uploadedAt": "2024-04-11T10:30:00Z",
    "url": "/user/public/images/image-id-1"
  }
```

**4. Upload Cover Image**
```
Endpoint: POST /user/uploadCoverImage
Headers: Authorization: Bearer {accessToken}
Content-Type: multipart/form-data
Body: 
  {
    "file": <binary_image_data>
  }
Response:
  {
    "imageId": "image-id-2",
    "fileName": "cover.jpg",
    "uploadedAt": "2024-04-11T10:30:00Z",
    "url": "/user/public/images/image-id-2"
  }
```

**5. Get User Images (Public)**
```
Endpoint: GET /user/public/images/{id}
Response: <binary_image_data>
Headers: Content-Type: image/jpeg | image/png | image/webp
```

#### **Post Management Endpoints**

**1. Create Post with Files**
```
Endpoint: POST /post/addPostWithFile
Headers: Authorization: Bearer {accessToken}
Content-Type: multipart/form-data
Body:
  {
    "title": "Post title",
    "content": "Post content description",
    "location": "Location information",
    "files": [<binary_file_1>, <binary_file_2>],
    "tags": ["tag1", "tag2"]
  }
Response:
  {
    "postId": "post-id-1",
    "title": "Post title",
    "createdBy": "user-id",
    "createdAt": "2024-04-11T10:30:00Z",
    "imageCount": 2,
    "images": [
      {
        "imageId": "img-1",
        "url": "/post/public/images/img-1"
      }
    ]
  }
```

**2. Get All Posts**
```
Endpoint: POST /post/getAllPost
Headers: Authorization: Bearer {accessToken}
Query: 
  {
    "page": 1,
    "limit": 20,
    "sortBy": "createdAt",
    "order": "desc"
  }
Response:
  {
    "totalPosts": 150,
    "page": 1,
    "limit": 20,
    "posts": [
      {
        "postId": "post-id-1",
        "title": "Post title",
        "content": "Post content",
        "createdBy": "user-id",
        "createdAt": "2024-04-11T10:30:00Z",
        "ratingCount": 5,
        "averageRating": 4.5
      }
    ]
  }
```

**3. Get User Posts**
```
Endpoint: POST /post/getAllUserPost
Headers: Authorization: Bearer {accessToken}
Query: 
  {
    "userId": "user-id",
    "page": 1,
    "limit": 20
  }
Response:
  {
    "totalUserPosts": 25,
    "posts": [...]
  }
```

**4. Delete Post**
```
Endpoint: POST /post/postDelete
Headers: Authorization: Bearer {accessToken}
Body:
  {
    "postId": "post-id-1"
  }
Response:
  {
    "postId": "post-id-1",
    "message": "Post deleted successfully",
    "deletedAt": "2024-04-11T10:30:00Z"
  }
```

**5. Get Post Images (Public)**
```
Endpoint: GET /post/public/images/{id}
Response: <binary_image_data>
Headers: Content-Type: image/jpeg | image/png | image/webp
```

#### **Post Description Endpoints**

**1. Add Post Description**
```
Endpoint: POST /post/addPoastDiscription
Headers: Authorization: Bearer {accessToken}
Body:
  {
    "postId": "post-id-1",
    "description": "Detailed description of the artifact",
    "language": "en"
  }
Response:
  {
    "descriptionId": "desc-id-1",
    "postId": "post-id-1",
    "description": "Detailed description...",
    "version": 1,
    "createdAt": "2024-04-11T10:30:00Z"
  }
```

**2. Get Post Description**
```
Endpoint: POST /post/getPostDiscription
Headers: Authorization: Bearer {accessToken}
Body:
  {
    "postId": "post-id-1"
  }
Response:
  {
    "descriptionId": "desc-id-1",
    "postId": "post-id-1",
    "description": "Detailed description...",
    "version": 1,
    "createdBy": "user-id",
    "createdAt": "2024-04-11T10:30:00Z"
  }
```

**3. Update Post Description**
```
Endpoint: POST /post/updatePostDiscription
Headers: Authorization: Bearer {accessToken}
Body:
  {
    "descriptionId": "desc-id-1",
    "description": "Updated description",
    "version": 2
  }
Response:
  {
    "descriptionId": "desc-id-1",
    "description": "Updated description",
    "version": 2,
    "updatedAt": "2024-04-11T10:35:00Z"
  }
```

**4. Delete Post Description**
```
Endpoint: POST /post/discriptionDelete
Headers: Authorization: Bearer {accessToken}
Body:
  {
    "descriptionId": "desc-id-1"
  }
Response:
  {
    "descriptionId": "desc-id-1",
    "message": "Description deleted successfully",
    "deletedAt": "2024-04-11T10:30:00Z"
  }
```

#### **Post Interactions Endpoints**

**1. Add Rating to Post**
```
Endpoint: POST /post/addRating
Headers: Authorization: Bearer {accessToken}
Body:
  {
    "postId": "post-id-1",
    "rating": 5,
    "comment": "Excellent artifact"
  }
Response:
  {
    "ratingId": "rating-id-1",
    "postId": "post-id-1",
    "rating": 5,
    "ratedBy": "user-id",
    "createdAt": "2024-04-11T10:30:00Z"
  }
```

**2. Add Vote to Description**
```
Endpoint: POST /post/addVote
Headers: Authorization: Bearer {accessToken}
Body:
  {
    "descriptionId": "desc-id-1",
    "voteType": "upvote|downvote"
  }
Response:
  {
    "voteId": "vote-id-1",
    "descriptionId": "desc-id-1",
    "voteType": "upvote",
    "votedBy": "user-id",
    "createdAt": "2024-04-11T10:30:00Z"
  }
```

#### **Error Response Format**

**Standard Error Response:**
```json
{
  "timestamp": "2024-04-11T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": {
    "field": "email",
    "message": "Invalid email format"
  },
  "path": "/user/updateProfile"
}
```

**Authentication Error (401):**
```json
{
  "timestamp": "2024-04-11T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired JWT token",
  "path": "/post/getAllPost"
}
```

**Authorization Error (403):**
```json
{
  "timestamp": "2024-04-11T10:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied. You do not have permission to delete this post.",
  "path": "/post/postDelete"
}
```

### 1.4.2 Data Model Interfaces

#### **User Entity**
```typescript
interface User {
  id: string;
  username: string;
  email: string;
  provider: "GOOGLE" | "FACEBOOK" | "APPLE";
  profileImageId?: string;
  coverImageId?: string;
  bio?: string;
  location?: string;
  createdAt: Date;
  createdBy?: string;
  lastModifiedAt?: Date;
  lastModifiedBy?: string;
}
```

#### **InscriptionPost Entity**
```typescript
interface InscriptionPost {
  postId: string;
  title: string;
  content: string;
  location: string;
  createdBy: string;
  createdAt: Date;
  lastModifiedAt: Date;
  lastModifiedBy?: string;
  images: ImageReference[];
  descriptionId?: string;
  ratings: Rating[];
  visibility: "PUBLIC" | "PRIVATE" | "RESTRICTED";
  tags: string[];
}
```

#### **PublicPostDescription Entity**
```typescript
interface PublicPostDescription {
  descriptionId: string;
  postId: string;
  description: string;
  version: number;
  language: string;
  createdBy: string;
  createdAt: Date;
  lastModifiedAt: Date;
  votes: {
    upvote: number;
    downvote: number;
  };
}
```

#### **UserImage Entity**
```typescript
interface UserImage {
  imageId: string;
  userId: string;
  imageType: "PROFILE" | "COVER";
  fileName: string;
  fileSize: number;
  mimeType: string;
  uploadedAt: Date;
  url: string;
  metadata?: {
    width?: number;
    height?: number;
    format?: string;
  };
}
```

### 1.4.3 Service Layer Interfaces

#### **User Service Interface**
```java
interface IUserService {
    User getUserProfile(String userId);
    User updateUserProfile(String userId, UserUpdateRequest request);
    UserImage uploadProfileImage(String userId, MultipartFile file);
    UserImage uploadCoverImage(String userId, MultipartFile file);
    UserImage getUserImage(String imageId);
    void deleteUser(String userId);
}
```

#### **Post Service Interface**
```java
interface IPostService {
    InscriptionPost createPost(PostCreateRequest request, String userId);
    List<InscriptionPost> getAllPosts(Pageable pageable);
    List<InscriptionPost> getUserPosts(String userId, Pageable pageable);
    InscriptionPost getPostById(String postId);
    void deletePost(String postId, String userId);
    Rating addRating(String postId, RatingRequest request, String userId);
    Vote addVote(String descriptionId, VoteRequest request, String userId);
}
```

#### **Description Service Interface**
```java
interface IDescriptionService {
    PublicPostDescription addDescription(String postId, DescriptionRequest request, String userId);
    PublicPostDescription getDescription(String postId);
    PublicPostDescription updateDescription(String descriptionId, DescriptionRequest request, String userId);
    void deleteDescription(String descriptionId, String userId);
}
```

### 1.4.4 Authentication Service Interface

```java
interface IAuthenticationService {
    JwtTokenResponse authenticateViaOAuth2(String provider, String authorizationCode);
    JwtTokenResponse refreshAccessToken(String refreshToken);
    void logout(String userId);
    UserDetails loadUserByUsername(String username);
    boolean validateJwtToken(String token);
    String extractUserIdFromToken(String token);
}
```

### 1.4.5 Communication Protocol

**Protocol:** REST/HTTP(S)
- **Secure Transport:** HTTPS (TLS 1.2+)
- **Request Format:** JSON (application/json)
- **Response Format:** JSON (application/json)
- **File Uploads:** Multipart Form Data (multipart/form-data)
- **Default Encoding:** UTF-8

### 1.4.6 Component Dependency Graph

```
┌─────────────────────────────────────────────────────────┐
│                   Controllers                           │
│        (UserController, PostController)                 │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        ↓                         ↓
┌────────────────┐        ┌──────────────────┐
│ UserService    │        │ PostService      │
│ (Business      │        │ (Business Logic) │
│  Logic)        │        │                  │
└────┬───────────┘        └──────┬───────────┘
     │                           │
     ├─────────────┬─────────────┤
     ↓             ↓             ↓
┌──────────┐  ┌──────────┐  ┌──────────────┐
│UserRepo  │  │PostRepo  │  │DescRepro     │
│(Data     │  │(Data     │  │(Data Access) │
│Access)   │  │Access)   │  │              │
└──────┬───┘  └──────┬───┘  └────────┬─────┘
       │             │              │
       └─────────────┴──────────────┴─────┐
                                          ↓
                                   ┌────────────┐
                                   │ MongoDB    │
                                   │ Database   │
                                   └────────────┘
```

---

## Conclusion

This system architecture provides a robust, scalable, and secure foundation for the Artifact Registry Backend. The multi-layered approach with clear separation of concerns, comprehensive security controls, audit trails, and well-defined component interfaces ensures maintainability, reliability, and compliance with modern application development standards.

**Key Strengths:**
- ✓ OAuth2 multi-provider authentication
- ✓ JWT stateless API authentication
- ✓ Comprehensive audit trails
- ✓ Strong security controls
- ✓ MongoDB auditing built-in
- ✓ Clear component interfaces
- ✓ Scalable microservices-ready design
- ✓ Full observability with Prometheus monitoring

---

**Document Version:** 1.0  
**Last Updated:** 2024-04-11  
**Status:** Complete
