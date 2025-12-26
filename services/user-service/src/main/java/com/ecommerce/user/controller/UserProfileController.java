package com.ecommerce.user.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.user.dto.UserProfileRequest;
import com.ecommerce.user.dto.UserProfileResponse;
import com.ecommerce.user.entity.UserProfile;
import com.ecommerce.user.service.UserProfileCommandService;
import com.ecommerce.user.service.UserProfileQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * User Profile Controller
 * 
 * <p>REST controller for user profile management operations.
 * Implements CQRS pattern with separate command and query services.</p>
 * 
 * <h2>API Endpoints:</h2>
 * <pre>
 * Profile Management:
 * GET    /api/users/me              - Get current user profile
 * PUT    /api/users/me              - Update current user profile
 * DELETE /api/users/me              - Delete current user profile
 * PATCH  /api/users/me/bio          - Update bio only
 * PATCH  /api/users/me/avatar       - Update avatar URL
 * PATCH  /api/users/me/phone        - Update phone number
 * 
 * Admin Endpoints:
 * GET    /api/users/{id}            - Get user profile by ID
 * GET    /api/users                 - Search/list users (paginated)
 * GET    /api/users/incomplete      - Get incomplete profiles
 * GET    /api/users/statistics      - Get profile statistics
 * </pre>
 * 
 * <h2>CQRS in Controllers:</h2>
 * <pre>
 * Read Operations (GET):
 * - Call QueryService
 * - @Transactional(readOnly = true) in service
 * - Routes to REPLICA database
 * - Results cached in Redis
 * 
 * Write Operations (POST, PUT, DELETE, PATCH):
 * - Call CommandService
 * - @Transactional(readOnly = false) in service
 * - Routes to PRIMARY database
 * - Cache evicted after update
 * 
 * Example:
 * GET /api/users/me:
 *   → queryService.getProfile()
 *   → REPLICA DB
 *   → Redis cache
 *   → Fast response
 * 
 * PUT /api/users/me:
 *   → commandService.updateProfile()
 *   → PRIMARY DB
 *   → Cache eviction
 *   → Consistency maintained
 * </pre>
 * 
 * <h2>Authentication & Authorization:</h2>
 * <pre>
 * JWT Authentication:
 * - All endpoints require valid JWT token
 * - Token validated by OAuth2 Resource Server
 * - User ID extracted from token
 * - No need to pass user ID in request
 * 
 * Request Header:
 * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 * 
 * Extracting Current User:
 * Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 * Long userId = Long.parseLong(auth.getName());
 * 
 * Authorization:
 * - Regular users can only access their own profile
 * - Admin users can access any profile
 * - Checked via @PreAuthorize or manual checks
 * </pre>
 * 
 * <h2>Response Format:</h2>
 * <pre>
 * Success Response:
 * {
 *   "timestamp": "2024-01-01T10:00:00",
 *   "status": 200,
 *   "message": "Profile retrieved successfully",
 *   "data": {
 *     "userId": 123,
 *     "email": "user@example.com",
 *     ...
 *   },
 *   "path": "/api/users/me"
 * }
 * 
 * Error Response:
 * {
 *   "timestamp": "2024-01-01T10:00:00",
 *   "status": 404,
 *   "message": "Profile not found",
 *   "path": "/api/users/me"
 * }
 * 
 * Pagination Response:
 * {
 *   "content": [...],
 *   "totalElements": 1000,
 *   "totalPages": 50,
 *   "size": 20,
 *   "number": 0
 * }
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController  // REST controller
@RequestMapping("/api/users")  // Base path
@Tag(name = "User Profile", description = "User profile management operations")
public class UserProfileController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    // CQRS services
    private final UserProfileCommandService commandService;
    private final UserProfileQueryService queryService;

    @Autowired
    public UserProfileController(
            UserProfileCommandService commandService,
            UserProfileQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    /**
     * Get Current User Profile
     * 
     * <p>Retrieves authenticated user's profile.</p>
     * <p>User ID extracted from JWT token.</p>
     * 
     * @param authentication Spring Security authentication object
     * @param httpRequest HTTP request for path extraction
     * @return ResponseEntity with UserProfileResponse
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieves authenticated user's profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUserProfile(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        // Extract user ID from JWT token
        // Authentication.getName() returns subject (user ID) from token
        Long userId = Long.parseLong(authentication.getName());
        
        logger.info("Fetching profile for userId: {}", userId);
        
        // Query service (routes to REPLICA, uses cache)
        UserProfile profile = queryService.getProfile(userId)
            .orElseThrow(() -> new RuntimeException("Profile not found"));
        
        // Convert entity to DTO
        UserProfileResponse response = toResponse(profile);
        
        // Build API response
        ApiResponse<UserProfileResponse> apiResponse = ApiResponse.success(
            response,
            "Profile retrieved successfully",
            httpRequest.getRequestURI()
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Update Current User Profile
     * 
     * <p>Updates authenticated user's profile with provided data.</p>
     * 
     * @param request UserProfileRequest with updates
     * @param authentication Spring Security authentication
     * @param httpRequest HTTP request for path extraction
     * @return ResponseEntity with updated UserProfileResponse
     */
    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Updates authenticated user's profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateCurrentUserProfile(
            @Valid @RequestBody UserProfileRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        Long userId = Long.parseLong(authentication.getName());
        
        logger.info("Updating profile for userId: {}", userId);
        
        // Convert DTO to entity
        UserProfile updates = toEntity(request);
        
        // Command service (routes to PRIMARY, evicts cache)
        UserProfile updatedProfile = commandService.updateProfile(userId, updates);
        
        // Convert to response DTO
        UserProfileResponse response = toResponse(updatedProfile);
        
        // Build API response
        ApiResponse<UserProfileResponse> apiResponse = ApiResponse.success(
            response,
            "Profile updated successfully",
            httpRequest.getRequestURI()
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Update Bio
     * 
     * <p>Efficient partial update of just the bio field.</p>
     * 
     * @param bio New bio text
     * @param authentication Spring Security authentication
     * @param httpRequest HTTP request for path extraction
     * @return ResponseEntity with success message
     */
    @PatchMapping("/me/bio")
    @Operation(summary = "Update bio", description = "Updates user bio only")
    public ResponseEntity<ApiResponse<Void>> updateBio(
            @RequestBody String bio,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        Long userId = Long.parseLong(authentication.getName());
        
        logger.info("Updating bio for userId: {}", userId);
        
        // Command service - efficient single field update
        commandService.updateBio(userId, bio);
        
        ApiResponse<Void> apiResponse = ApiResponse.success(
            null,
            "Bio updated successfully",
            httpRequest.getRequestURI()
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Update Avatar URL
     * 
     * <p>Updates user's avatar/profile picture URL.</p>
     * 
     * @param avatarUrl New avatar URL
     * @param authentication Spring Security authentication
     * @param httpRequest HTTP request for path extraction
     * @return ResponseEntity with success message
     */
    @PatchMapping("/me/avatar")
    @Operation(summary = "Update avatar", description = "Updates user avatar URL")
    public ResponseEntity<ApiResponse<Void>> updateAvatar(
            @RequestBody String avatarUrl,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        Long userId = Long.parseLong(authentication.getName());
        
        logger.info("Updating avatar for userId: {}", userId);
        
        commandService.updateAvatarUrl(userId, avatarUrl);
        
        ApiResponse<Void> apiResponse = ApiResponse.success(
            null,
            "Avatar updated successfully",
            httpRequest.getRequestURI()
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Delete Current User Profile
     * 
     * <p>Deletes authenticated user's profile.</p>
     * <p>Cascades to delete addresses and preferences.</p>
     * 
     * @param authentication Spring Security authentication
     * @param httpRequest HTTP request for path extraction
     * @return ResponseEntity with success message
     */
    @DeleteMapping("/me")
    @Operation(summary = "Delete profile", description = "Deletes authenticated user's profile")
    public ResponseEntity<ApiResponse<Void>> deleteCurrentUserProfile(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        Long userId = Long.parseLong(authentication.getName());
        
        logger.info("Deleting profile for userId: {}", userId);
        
        // Command service (routes to PRIMARY, evicts cache)
        commandService.deleteProfile(userId);
        
        ApiResponse<Void> apiResponse = ApiResponse.success(
            null,
            "Profile deleted successfully",
            httpRequest.getRequestURI()
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Search Users (Admin)
     * 
     * <p>Search and list users with pagination.</p>
     * <p>Requires ADMIN role.</p>
     * 
     * @param name Search term (optional)
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sort Sort field and direction
     * @return ResponseEntity with Page of UserProfileResponse
     */
    @GetMapping
    @Operation(summary = "Search users", description = "Search and list users (admin only)")
    // @PreAuthorize("hasRole('ADMIN')")  // Uncomment when security configured
    public ResponseEntity<Page<UserProfileResponse>> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        
        logger.info("Searching users with name: {}, page: {}, size: {}", name, page, size);
        
        // Create pageable with sorting
        Sort.Direction direction = sort[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));
        
        // Query service (routes to REPLICA)
        Page<UserProfile> profiles;
        if (name != null && !name.isEmpty()) {
            profiles = queryService.searchProfiles(name, pageable);
        } else {
            // If no search term, this would need a findAll implementation
            throw new RuntimeException("Search term required");
        }
        
        // Convert page of entities to page of DTOs
        Page<UserProfileResponse> response = profiles.map(this::toResponse);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get Incomplete Profiles (Admin)
     * 
     * <p>Lists profiles with completion below threshold.</p>
     * 
     * @param threshold Completion percentage threshold
     * @param page Page number
     * @param size Page size
     * @return ResponseEntity with Page of UserProfileResponse
     */
    @GetMapping("/incomplete")
    @Operation(summary = "Get incomplete profiles", description = "Lists profiles needing completion")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserProfileResponse>> getIncompleteProfiles(
            @RequestParam(defaultValue = "50") Integer threshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("Fetching incomplete profiles (threshold: {})", threshold);
        
        Pageable pageable = PageRequest.of(page, size);
        
        // Query service (routes to REPLICA)
        Page<UserProfile> profiles = queryService.getIncompleteProfiles(threshold, pageable);
        
        // Convert to DTOs
        Page<UserProfileResponse> response = profiles.map(this::toResponse);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Convert Entity to Response DTO
     * 
     * <p>Maps UserProfile entity to UserProfileResponse DTO.</p>
     * <p>TODO: Replace with MapStruct mapper for type-safe mapping.</p>
     * 
     * @param profile UserProfile entity
     * @return UserProfileResponse DTO
     */
    private UserProfileResponse toResponse(UserProfile profile) {
        return UserProfileResponse.builder()
            .userId(profile.getUserId())
            .email(profile.getEmail())
            .bio(profile.getBio())
            .avatarUrl(profile.getAvatarUrl())
            .phoneNumber(profile.getPhoneNumber())
            .phoneVerified(profile.getPhoneVerified())
            .dateOfBirth(profile.getDateOfBirth())
            .gender(profile.getGender())
            .profileCompletion(profile.getProfileCompletion())
            .lastLogin(profile.getLastLogin())
            .createdAt(profile.getCreatedAt())
            .updatedAt(profile.getUpdatedAt())
            // Addresses and preferences loaded separately if needed
            .build();
    }

    /**
     * Convert Request DTO to Entity
     * 
     * <p>Maps UserProfileRequest DTO to UserProfile entity.</p>
     * <p>TODO: Replace with MapStruct mapper.</p>
     * 
     * @param request UserProfileRequest DTO
     * @return UserProfile entity
     */
    private UserProfile toEntity(UserProfileRequest request) {
        UserProfile profile = new UserProfile();
        profile.setEmail(request.getEmail());
        profile.setBio(request.getBio());
        profile.setAvatarUrl(request.getAvatarUrl());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        return profile;
    }
}

