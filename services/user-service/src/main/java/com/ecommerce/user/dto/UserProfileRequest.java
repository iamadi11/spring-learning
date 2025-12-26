package com.ecommerce.user.dto;

import com.ecommerce.user.entity.UserProfile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * User Profile Request DTO
 * 
 * <p>Data Transfer Object for creating or updating user profiles.
 * Includes validation annotations for input validation.</p>
 * 
 * <h2>Validation Strategy:</h2>
 * <pre>
 * Bean Validation (JSR-380):
 * - @NotBlank: Field cannot be null or empty
 * - @Email: Must be valid email format
 * - @Size: String length constraints
 * - @Past: Date must be in the past
 * - @Pattern: Regex pattern matching
 * 
 * Usage in Controller:
 * @PostMapping("/profile")
 * public ResponseEntity create(@Valid @RequestBody UserProfileRequest request) {
 *     // If validation fails, Spring returns 400 Bad Request
 *     // with detailed error messages
 * }
 * 
 * Error Response Example:
 * {
 *   "status": 400,
 *   "message": "Validation failed",
 *   "errors": {
 *     "email": "Email should be valid",
 *     "bio": "Bio cannot exceed 1000 characters"
 *   }
 * }
 * </pre>
 * 
 * <h2>Create vs Update:</h2>
 * <pre>
 * Create (POST /api/users/me):
 * - All required fields must be present
 * - Use validation groups for stricter validation
 * - UserID assigned by Auth Service
 * 
 * Update (PUT /api/users/me):
 * - All fields optional (partial update)
 * - Only non-null fields are updated
 * - UserID from authenticated user
 * 
 * PATCH vs PUT:
 * - PUT: Replace entire resource (all fields)
 * - PATCH: Update specific fields (partial)
 * - This DTO supports PATCH semantics
 * </pre>
 * 
 * <h2>Mapping to Entity:</h2>
 * <pre>
 * Manual Mapping:
 * UserProfile entity = new UserProfile();
 * entity.setEmail(request.getEmail());
 * entity.setBio(request.getBio());
 * // ... map other fields
 * 
 * MapStruct (Automated):
 * @Mapper
 * public interface UserProfileMapper {
 *     UserProfile toEntity(UserProfileRequest request);
 *     UserProfileResponse toResponse(UserProfile entity);
 * }
 * 
 * Usage:
 * UserProfile entity = userProfileMapper.toEntity(request);
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data  // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-arg constructor
@AllArgsConstructor  // Lombok: all-args constructor
@Builder  // Lombok: builder pattern
public class UserProfileRequest {

    /**
     * Email address
     * 
     * <p>Required for profile creation.
     * Optional for updates (cannot change email).</p>
     */
    @Email(message = "Email should be valid")
    private String email;

    /**
     * Biography/About me
     * 
     * <p>Free-text description written by user.</p>
     * <p>Maximum 1000 characters to prevent abuse.</p>
     */
    @Size(max = 1000, message = "Bio cannot exceed 1000 characters")
    private String bio;

    /**
     * Avatar image URL
     * 
     * <p>URL to profile picture (uploaded separately).
     * Typically stored in CDN or S3.</p>
     * 
     * <p>Avatar Upload Flow:</p>
     * <pre>
     * 1. POST /api/users/me/avatar (multipart file)
     * 2. Service uploads to S3
     * 3. Returns CDN URL
     * 4. PUT /api/users/me (with avatarUrl)
     * 5. Profile updated with avatar URL
     * </pre>
     */
    @Size(max = 500, message = "Avatar URL cannot exceed 500 characters")
    private String avatarUrl;

    /**
     * Phone number with country code
     * 
     * <p>Format: +1234567890</p>
     * <p>Verification required before marking as verified.</p>
     */
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phoneNumber;

    /**
     * Date of birth
     * 
     * <p>Must be in the past.
     * Used for age verification and birthday promotions.</p>
     */
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    /**
     * Gender
     * 
     * <p>Optional field for personalization.</p>
     * <p>Values: MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY</p>
     */
    private UserProfile.Gender gender;
}

