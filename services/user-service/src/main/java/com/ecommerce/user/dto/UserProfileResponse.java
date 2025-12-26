package com.ecommerce.user.dto;

import com.ecommerce.user.entity.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * User Profile Response DTO
 * 
 * <p>Data Transfer Object for returning user profile information in API responses.
 * Excludes internal database fields and includes only client-relevant data.</p>
 * 
 * <h2>Why Use DTOs?</h2>
 * <pre>
 * Benefits:
 * 1. Decoupling:
 *    - API contract separate from database schema
 *    - Can change database without breaking API
 *    - Can change API without changing database
 * 
 * 2. Security:
 *    - Don't expose internal IDs or fields
 *    - Filter sensitive information
 *    - Control what clients can see
 * 
 * 3. Performance:
 *    - Return only needed fields
 *    - Reduce payload size
 *    - Avoid lazy loading issues
 * 
 * 4. Versioning:
 *    - Different DTOs for different API versions
 *    - V1 vs V2 responses
 *    - Backward compatibility
 * 
 * 5. Documentation:
 *    - Clear API contract
 *    - OpenAPI/Swagger generation
 *    - Client SDK generation
 * </pre>
 * 
 * <h2>Entity vs DTO:</h2>
 * <pre>
 * Entity (UserProfile):
 * - JPA annotations (@Entity, @Table)
 * - Relationships (@OneToMany, @ManyToOne)
 * - Database-specific fields
 * - Lazy loading proxies
 * - Used internally in service layer
 * 
 * DTO (UserProfileResponse):
 * - Pure Java class (no JPA)
 * - No relationships (flat structure or nested DTOs)
 * - API-friendly fields
 * - Fully initialized (no proxies)
 * - Used in controller layer
 * 
 * Conversion:
 * Entity → DTO (via Mapper)
 * Service returns Entity → Controller maps to DTO → Response
 * </pre>
 * 
 * <h2>JSON Serialization:</h2>
 * <pre>
 * Example JSON Response:
 * {
 *   "userId": 123,
 *   "email": "john@example.com",
 *   "bio": "Software developer",
 *   "avatarUrl": "https://cdn.example.com/avatars/123.jpg",
 *   "phoneNumber": "+1234567890",
 *   "phoneVerified": true,
 *   "dateOfBirth": "1990-01-15",
 *   "gender": "MALE",
 *   "profileCompletion": 85,
 *   "createdAt": "2024-01-01T10:00:00",
 *   "updatedAt": "2024-01-15T14:30:00",
 *   "addresses": [...],    // Optional: if requested
 *   "preferences": {...}   // Optional: if requested
 * }
 * 
 * Jackson automatically serializes to JSON
 * - LocalDate → ISO date format (yyyy-MM-dd)
 * - LocalDateTime → ISO datetime format
 * - Enum → String value
 * - null fields → omitted or null based on config
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
public class UserProfileResponse {

    /**
     * User ID - unique identifier
     */
    private Long userId;

    /**
     * Email address
     */
    private String email;

    /**
     * Biography/About me
     */
    private String bio;

    /**
     * Avatar image URL
     */
    private String avatarUrl;

    /**
     * Phone number
     */
    private String phoneNumber;

    /**
     * Phone verification status
     */
    private Boolean phoneVerified;

    /**
     * Date of birth
     */
    private LocalDate dateOfBirth;

    /**
     * Gender
     */
    private UserProfile.Gender gender;

    /**
     * Profile completion percentage (0-100)
     */
    private Integer profileCompletion;

    /**
     * Last login timestamp
     */
    private LocalDateTime lastLogin;

    /**
     * Account creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    private LocalDateTime updatedAt;

    /**
     * Addresses (optional - loaded on demand)
     * 
     * <p>Only included if explicitly requested:
     * GET /api/users/me?include=addresses</p>
     */
    private List<AddressResponse> addresses;

    /**
     * Preferences (optional - loaded on demand)
     * 
     * <p>Only included if explicitly requested:
     * GET /api/users/me?include=preferences</p>
     */
    private PreferencesResponse preferences;
}

