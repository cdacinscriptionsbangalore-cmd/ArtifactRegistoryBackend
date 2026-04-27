# Low-Level Design (LLD) Documentation for Stone Inscription Application

This document provides detailed low-level design specifications for the Stone Inscription application, including system architecture, database schema, class designs, and component interactions.

## System Architecture

The application follows a layered architecture based on Spring Boot:

### Layers
1. **Presentation Layer**: REST Controllers handling HTTP requests/responses
2. **Service Layer**: Business logic implementation
3. **Repository Layer**: Data access using Spring Data MongoDB
4. **Entity Layer**: Domain objects mapped to MongoDB collections

### Key Components
- **Authentication Module**: Handles OAuth2 login, JWT token management
- **User Management Module**: Profile management, image uploads
- **Post Management Module**: Inscription post CRUD, ratings, descriptions
- **Image Management Module**: File upload, storage, and retrieval
- **Moderation Module**: Content moderation integration
- **Exception Handling**: Global error handling and responses

### Technology Stack
- **Framework**: Spring Boot 3.5.5
- **Database**: MongoDB
- **Security**: Spring Security with OAuth2, JWT
- **File Storage**: MongoDB GridFS for images
- **Build Tool**: Maven
- **Language**: Java 17

## Database Schema

The application uses MongoDB with the following collections:

### 1. users_auth
Stores user authentication information.

**Fields**:
- `_id` (ObjectId): Primary key
- `username` (String): Unique username
- `email` (String): Unique email address
- `passwordHash` (String): Hashed password (ignored in JSON)
- `provider` (String): OAuth provider (e.g., Google)
- `roles` (List<String>): User roles
- `createdAt` (Date): Creation timestamp
- `updatedAt` (Date): Last update timestamp
- `lastLogin` (Date): Last login timestamp

**Indexes**: username (unique), email (unique)

### 2. users_profile
Stores user profile information.

**Fields**:
- `_id` (ObjectId): Primary key
- `authId` (ObjectId): Reference to users_auth._id
- `name` (String): Full name
- `username` (String): Unique username
- `email` (String): Email address
- `profileImage` (String): Profile image URL/path
- `coverImage` (String): Cover image URL/path
- `bio` (String): User biography
- `imagesUploaded` (Integer): Count of uploaded images
- `upvotesReceived` (Integer): Total upvotes received
- `followers` (Integer): Number of followers
- `points` (Integer): User points/score
- `createdAt` (Date): Creation timestamp
- `updatedAt` (Date): Last update timestamp
- `active` (Boolean): Account active status

**Indexes**: name, username (unique), email (unique), createdAt

### 3. inscriptionposts
Stores inscription post data.

**Fields**:
- `_id` (ObjectId): Primary key
- `user_id` (ObjectId): Reference to users_profile._id
- `username` (String): Transient field for user name
- `createdAt` (Date): Creation timestamp
- `updatedAt` (Date): Last update timestamp
- `images` (Embedded): Image data (thumbnail and list)
- `description` (Embedded): Post description details
- `topic` (String): Post topic/category
- `script` (List<String>): Script types
- `type` (String): Post type
- `visiblity` (Boolean): Post visibility
- `distance` (Double): Distance metric
- `rating` (Double): Average rating
- `userrating` (List<UsersRating>): User ratings

**Embedded Objects**:
- **Images**:
  - `thumbnailImage` (String): Thumbnail URL
  - `image` (List<String>): Image URLs
- **Description**:
  - `title` (String): Post title
  - `subject` (String): Subject
  - `description` (String): Description text
  - `scriptLanguage` (List<String>): Script languages
  - `language` (List<String>): Languages
  - `englishTranslation` (String): English translation
  - `moderation` (ContentModeration): Moderation status

**Indexes**: user_id, topic, createdAt (compound indexes)

### 4. postdescription
Stores public descriptions for posts.

**Fields**:
- `_id` (ObjectId): Primary key
- `postId` (ObjectId): Reference to inscriptionposts._id
- `userId` (ObjectId): Reference to users_profile._id
- `username` (String): User name
- `userImageUrl` (String): User image URL
- `description` (String): Description text
- `moderation` (ContentModeration): Moderation status
- `upvote` (Integer): Upvote count
- `userVote` (List<UserVote>): User votes
- `createdAt` (Date): Creation timestamp
- `updatedAt` (Date): Last update timestamp

**Embedded Objects**:
- **UserVote**:
  - `userId` (String): Voting user ID

### 5. imagesdata
Stores binary image data for posts.

**Fields**:
- `id` (String): Primary key
- `postId` (ObjectId): Reference to inscriptionposts._id
- `imageData` (byte[]): Binary image data
- `metadata` (Embedded): Image metadata

**Embedded Objects**:
- **Metadata**:
  - `fileName` (String): Original file name
  - `fileSize` (Long): File size in bytes
  - `contentType` (String): MIME type
  - `imageHashValue` (String): Image hash for deduplication

### 6. user_images
Stores binary image data for user profiles.

**Fields**:
- `id` (String): Primary key
- `userId` (ObjectId): Reference to users_profile._id
- `imageType` (Enum): PROFILE or COVER
- `imageData` (byte[]): Binary image data
- `metadata` (Embedded): Image metadata

**Embedded Objects**:
- **Metadata**:
  - `fileName` (String): Original file name
  - `fileSize` (Long): File size in bytes
  - `contentType` (String): MIME type

## Relationships

- `users_auth` ↔ `users_profile`: One-to-One (authId in users_profile references users_auth._id)
- `users_profile` ↔ `inscriptionposts`: One-to-Many (user_id in inscriptionposts references users_profile._id)
- `users_profile` ↔ `user_images`: One-to-Many (userId in user_images references users_profile._id)
- `inscriptionposts` ↔ `imagesdata`: One-to-Many (postId in imagesdata references inscriptionposts._id)
- `inscriptionposts` ↔ `postdescription`: One-to-Many (postId in postdescription references inscriptionposts._id)

## Class Design

### Controllers
- **OAuthController**: Handles authentication endpoints
- **UserController**: Manages user profile operations
- **PostController**: Handles post CRUD and interactions
- **ExceptionController**: Global exception handling

### Services
- **StoneAuthService**: Authentication and token management
- **UserService**: User profile business logic
- **PostService**: Post management business logic

### Repositories
- **UserRepository**: User data access
- **UserAuthRepository**: Authentication data access
- **InscriptionPostRepository**: Post data access
- **PublicPostDescriptionRepository**: Description data access
- **ImagesDataRepository**: Image data access
- **UserImageRepository**: User image data access

### Entities
- **User**: User profile entity
- **UserAuth**: User authentication entity
- **InscriptionPost**: Post entity with embedded objects
- **PublicPostDescription**: Post description entity
- **ImagesData**: Post image data entity
- **UserImage**: User image data entity

### Utilities
- **JwtUtil**: JWT token operations
- **StoneInscriptionUserDetailservice**: User details for authentication

## API Design

Detailed API specifications are documented in `ApiDocumentation.MD`. Key design principles:

- RESTful endpoints with proper HTTP methods
- JWT-based authentication for secured endpoints
- File upload support with multipart/form-data
- Consistent error response format
- Pagination and filtering support

## Data Flow

### User Registration/Login Flow
1. User initiates OAuth2 login (e.g., Google)
2. OAuth2SuccessHandler processes authentication
3. User profile created/updated in database
4. JWT token generated and returned

### Post Creation Flow
1. Authenticated user uploads files and post data
2. Files validated and stored in ImagesData collection
3. Post entity created with image references
4. Moderation service called for content check
5. Post saved to inscriptionposts collection

### Image Retrieval Flow
1. Client requests image by ID
2. Repository fetches image data from ImagesData/UserImage
3. Binary data streamed as InputStreamResource
4. Content-Type header set based on metadata

## Module Interaction Flows

The following module flows are presented in dependency order. If one flow depends on another service, the dependent module is listed first.

### 1. Authentication Module Flow
Dependencies: `JwtRequestFilter` -> `JwtUtil` -> `StoneInscriptionUserDetailservice` -> `UserRepository` -> `RefreshTokenRepo` -> `StoneAuthServiceImp` -> `OAuthController`

1. Request arrives at `OAuthController` or any secured controller endpoint.
2. `JwtRequestFilter` intercepts the request and calls `extractJwtFromRequest()`.
3. `JwtUtil` validates the token and extracts the username and roles.
4. If valid, the filter creates a `UsernamePasswordAuthenticationToken` and sets it into `SecurityContextHolder`.
5. Secured controller methods (`@Secured("user")`) execute with authenticated context.
6. For refresh token flow, `OAuthController.refreshToken()` delegates to `StoneAuthServiceImp.refreshToken()`.
7. `StoneAuthServiceImp` reads the `refreshToken` cookie, hashes it, and queries `RefreshTokenRepo`.
8. It validates the refresh token state, expiration, and revoke flag.
9. It loads the `User` from `UserRepository` and uses `StoneInscriptionUserDetailservice` plus `JwtUtil` to generate a new access token.
10. It rotates the refresh token by saving a new `RefreshToken` object and sending a refreshed cookie back to the client.
11. For logout, `StoneAuthServiceImp.logoutAuth()` revokes the refresh token and deletes the cookie.
12. For active session updates, `StoneAuthServiceImp.updateLastActive()` validates the refresh token and updates `lastUseAt`.

### 2. User Management Flow
Dependencies: `Authentication Module` -> `UserController` -> `UserServiceImpl` -> `UserRepository` -> `UserImageRepo`

1. Authenticated requests to `UserController` endpoints include `Authorization: Bearer <token>`.
2. `JwtRequestFilter` authenticates the request and populates the security context.
3. `UserController` extracts the user email from the token and delegates to `UserServiceImpl`.
4. `UserServiceImpl.getProfile()` fetches the `User` entity from `UserRepository`.
5. `UserServiceImpl.updateProfile()` validates input and updates the user record.
6. `UserServiceImpl.uploadProfileImage()` and `uploadCoverImage()` validate file extension, delete any existing image of the same type, save the new `UserImage`, update the user record, and return the updated profile.
7. `getUserImage()` streams binary image bytes from `UserImageRepo`.

### 3. Image Validation and Geolocation Module Flow
Dependencies: `User Management Flow` for user identity -> `PostServiceImp` -> `ImageMetadataGeolocationWithPhash` -> `ImagesDataRepo`

1. `PostController.addPostWithFile()` receives multipart file uploads and calls `PostServiceImp.addPostWithFile()`.
2. `PostServiceImp` loads the user using `UserRepository` and validates the files.
3. It calls `ImageMetadataGeolocationWithPhash.getGeoLocationWithIamgeMetaandInfo()` to:
   - compute perceptual hash (`pHash`)
   - read EXIF metadata
   - extract GPS coordinates if present
   - call external geolocation API to map coordinates to address details
4. The helper returns metadata objects for each image.
5. `PostServiceImp` may use geolocation output to populate `InscriptionPost.Description.GeoLocation`.
6. It saves binary image data into `ImagesDataRepo` and stores returned image IDs in `InscriptionPost.Images`.

### 4. Content Moderation Module Flow
Dependencies: `Image Validation and Geolocation Module` -> `PostServiceImp` -> `ContentModerationService` -> `N8nModerationClient` -> External moderation webhook

1. When creating or updating posts or descriptions, `PostServiceImp` calls `moderatePostContent()` or `moderateCommentContent()`.
2. These helper methods delegate to `ContentModerationService.moderate(title, topic, description)`.
3. `ContentModerationService` validates all required fields and builds a `ContentModerationRequestDto`.
4. It calls `N8nModerationClient.moderate()` to send the payload to the external moderation webhook.
5. The service parses the raw JSON response into `ContentModerationResponseDto` objects.
6. It evaluates `decision`, `confidence`, and `status` against service thresholds.
7. If the content is rejected, `ContentModerationService` throws `StoneInscriptionException` and aborts the save.
8. If approved, it returns a `ContentModerationResult` that is converted into embedded `ContentModeration` metadata in the post or description record.

### 5. Post Management Module Flow
Dependencies: `Authentication Module` -> `UserRepository` -> `PostServiceImp` -> `Image Validation and Geolocation Module` -> `Content Moderation Module` -> `InscriptionPostRepo` / `PublicPostDescriptionRepo` / `ImagesDataRepo`

1. `PostController.addPostWithFile()` and other secured endpoints receive requests from authenticated users.
2. `PostServiceImp.addPostWithFile()` loads the current user and validates image files.
3. It calls image metadata extraction and optionally geolocation lookup.
4. It calls content moderation before persisting the post description.
5. If moderation approves, it creates or updates the `InscriptionPost` entity and saves it through `InscriptionPostRepo`.
6. It stores raw image bytes in `ImagesDataRepo` and updates the post with image IDs and thumbnail references.
7. `PostServiceImp.getAllPost()` and `getAllUserPost()` enrich post records with user names and public image URLs using `backendUrl`.
8. `addPoastDiscription()`, `updatePostDiscription()`, and description delete operations validate ownership, moderate content, and persist to `PublicPostDescriptionRepo`.
9. Post vote and rating updates also validate user identity and then update the relevant post or description records.

### 6. Exception and Security Filter Flow
Dependencies: `Authentication Module` -> `JwtRequestFilter` -> Controller layer -> `ExceptionController`

1. The request filter validates JWT and attaches authentication or request exceptions.
2. If a token is invalid or missing, the filter passes the exception to the request.
3. Controller methods may throw `StoneInscriptionException` for invalid business logic, authorization failures, or moderation failures.
4. `ExceptionController` catches these exceptions globally and returns a standardized JSON response.

### Topological Order Summary
1. `JwtRequestFilter` / `JwtUtil` / `StoneInscriptionUserDetailservice` (Authentication infrastructure)
2. `StoneAuthServiceImp` / `RefreshTokenRepo` / `UserRepository` (Token lifecycle)
3. `UserController` / `UserServiceImpl` / `UserImageRepo` (User profile management)
4. `PostServiceImp` / `ImageMetadataGeolocationWithPhash` / `ImagesDataRepo` (Image validation and storage)
5. `ContentModerationService` / `N8nModerationClient` (Moderation)
6. `InscriptionPostRepo` / `PublicPostDescriptionRepo` (Post persistence)
7. `ExceptionController` (Global error handling)

## Deployment Consideration for Module Dependencies
- The authentication module must be initialized before secured controller execution.
- Moderation infrastructure must be available when post creation or description submission is invoked.
- Geolocation API dependency is required only when uploaded images contain GPS EXIF data.
- Image storage persistence must complete before the post entity is marked published.

## Module Interaction Notes
- The **Authentication Module** is the foundation: if token verification fails, all secured user and post flows abort.
- The **Post Management Module** is dependent on both **Image Validation** and **Content Moderation**.
- User profile image uploads are independent of post moderation but still require authenticated user context.
- External services include the geolocation API for GPS data and the moderation webhook for content approval.

## Diagram Guidance
A complete interaction diagram should show:
- OAuth flow into Authentication Module
- Token validation into secured controllers
- User service and on-demand image service branching from authenticated user actions
- Post creation flow feeding into Image Validation and Content Moderation before persistence
- Exception handling as a final sink for failures

## Result
This ordered flow description provides clear module dependencies and explicit sequencing for the authentication, user, image, moderation, and post services in the Stone Inscription application.

## Security Design

### Authentication
- OAuth2 integration with Google provider
- JWT tokens for session management
- Refresh token mechanism for token renewal

### Authorization
- Role-based access control with @Secured annotations
- User-scoped operations (users can only modify their own data)

### Data Protection
- Passwords hashed (though OAuth2 primary)
- Sensitive fields excluded from JSON serialization
- CSRF protection enabled

### Input Validation
- Bean validation annotations (@NotBlank, @Email, etc.)
- File type and size validation for uploads
- Custom validation for business rules

## Error Handling

### Global Exception Handling
- **StoneInscriptionException**: Custom business exceptions
- **MethodArgumentNotValidException**: Validation errors
- **Authentication/Authorization Exceptions**: Security violations

### Error Response Format
```json
{
  "error_message": "Error description",
  "http_status": 400,
  "http_status_code": 400
}
```

## Performance Considerations

### Database Optimization
- Strategic indexing on frequently queried fields
- Compound indexes for multi-field queries
- Embedded documents to reduce joins

### Image Handling
- Thumbnail generation for efficient loading
- Lazy loading of image data
- Hash-based deduplication

### Caching
- Potential for Redis caching of user profiles and popular posts
- JWT token caching for performance

## Deployment Considerations

### Environment Configuration
- Application properties for different environments
- MongoDB connection configuration
- OAuth2 client credentials

### Monitoring
- Spring Boot Actuator for health checks
- Micrometer for metrics collection
- Prometheus integration for monitoring

### Scalability
- Horizontal scaling with multiple instances
- Database sharding for large datasets
- CDN integration for image serving

Date: 11 April 2026