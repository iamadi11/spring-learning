package com.ecommerce.auth.dto;

import com.ecommerce.auth.enums.AuthProvider;
import com.ecommerce.auth.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * User Response DTO
 * 
 * <p>Data Transfer Object for returning user information in API responses.
 * Excludes sensitive data like password and refresh tokens.</p>
 * 
 * <h2>Usage Scenarios:</h2>
 * <pre>
 * 1. After Registration:
 *    POST /api/auth/register
 *    Response: UserResponse (without tokens)
 * 
 * 2. After Login:
 *    POST /api/auth/login
 *    Response: {
 *      ...TokenResponse,
 *      user: UserResponse
 *    }
 * 
 * 3. Get Current User:
 *    GET /api/auth/me
 *    Response: UserResponse
 * 
 * 4. Get User Profile:
 *    GET /api/users/{id}
 *    Response: UserResponse
 * 
 * 5. List Users (Admin):
 *    GET /api/admin/users
 *    Response: {
 *      users: [UserResponse, UserResponse, ...],
 *      total: 150,
 *      page: 1
 *    }
 * </pre>
 * 
 * <h2>Example Response:</h2>
 * <pre>
 * {
 *   "id": 123,
 *   "username": "johndoe",
 *   "email": "john.doe@example.com",
 *   "firstName": "John",
 *   "lastName": "Doe",
 *   "authProvider": "LOCAL",
 *   "emailVerified": true,
 *   "imageUrl": "https://example.com/avatars/johndoe.jpg",
 *   "status": "ACTIVE",
 *   "tenantId": "tenant1",
 *   "using2FA": false,
 *   "roles": ["USER", "SELLER"],
 *   "createdAt": "2024-01-01T10:00:00",
 *   "updatedAt": "2024-01-15T14:30:00"
 * }
 * </pre>
 * 
 * <h2>Security Considerations:</h2>
 * <pre>
 * Excluded Fields (Never Expose):
 * - password: Password hash
 * - secret2FA: TOTP secret key
 * - refreshTokens: Active refresh tokens
 * - providerId: OAuth provider user ID (internal)
 * 
 * Conditional Fields (Based on Context):
 * - email: Hidden for other users (privacy)
 * - tenantId: Hidden for single-tenant apps
 * - using2FA: Only visible to self or admin
 * 
 * Public Fields (Safe to Expose):
 * - username: Public identifier
 * - firstName/lastName: Display name
 * - imageUrl: Avatar URL
 * - createdAt: Registration date
 * </pre>
 * 
 * <h2>Entity to DTO Mapping:</h2>
 * <pre>
 * // Manual mapping
 * public UserResponse toResponse(User user) {
 *     return UserResponse.builder()
 *         .id(user.getId())
 *         .username(user.getUsername())
 *         .email(user.getEmail())
 *         .firstName(user.getFirstName())
 *         .lastName(user.getLastName())
 *         .authProvider(user.getAuthProvider())
 *         .emailVerified(user.getEmailVerified())
 *         .imageUrl(user.getImageUrl())
 *         .status(user.getStatus())
 *         .tenantId(user.getTenantId())
 *         .using2FA(user.isUsing2FA())
 *         .roles(user.getRoles().stream()
 *             .map(Role::getName)
 *             .collect(Collectors.toSet()))
 *         .createdAt(user.getCreatedAt())
 *         .updatedAt(user.getUpdatedAt())
 *         .build();
 * }
 * 
 * // Using ModelMapper
 * @Autowired
 * private ModelMapper modelMapper;
 * 
 * public UserResponse toResponse(User user) {
 *     return modelMapper.map(user, UserResponse.class);
 * }
 * 
 * // Using MapStruct (compile-time, type-safe)
 * @Mapper(componentModel = "spring")
 * public interface UserMapper {
 *     @Mapping(source = "roles", target = "roles", 
 *              qualifiedByName = "mapRoles")
 *     UserResponse toResponse(User user);
 *     
 *     @Named("mapRoles")
 *     default Set<String> mapRoles(Set<Role> roles) {
 *         return roles.stream()
 *             .map(Role::getName)
 *             .collect(Collectors.toSet());
 *     }
 * }
 * </pre>
 * 
 * <h2>Privacy Levels:</h2>
 * <pre>
 * Full Details (Self or Admin):
 * - All fields included
 * - Email visible
 * - 2FA status visible
 * - Tenant info visible
 * 
 * Public Profile (Other Users):
 * - Username, name, avatar only
 * - Email hidden
 * - Status hidden (except banned)
 * - Role visible (for context: seller, admin)
 * 
 * Minimal (Search Results):
 * - Username and avatar only
 * - For autocomplete/search
 * - No sensitive data
 * 
 * Example implementation:
 * public UserResponse toResponse(User user, User viewer) {
 *     UserResponse response = toResponse(user);
 *     
 *     // If viewer is not the user and not admin
 *     if (!user.getId().equals(viewer.getId()) && 
 *         !viewer.getRoles().contains(Role.ADMIN)) {
 *         response.setEmail(null);  // Hide email
 *         response.setUsing2FA(null);  // Hide 2FA status
 *         response.setTenantId(null);  // Hide tenant
 *     }
 *     
 *     return response;
 * }
 * </pre>
 * 
 * <h2>Serialization:</h2>
 * <pre>
 * // Jackson annotations for custom serialization
 * @JsonInclude(JsonInclude.Include.NON_NULL)  // Exclude null fields
 * public class UserResponse {
 *     
 *     @JsonProperty("id")  // Explicit property name
 *     private Long id;
 *     
 *     @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")  // Date format
 *     private LocalDateTime createdAt;
 *     
 *     @JsonIgnore  // Never serialize (if needed)
 *     private String internalField;
 * }
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data  // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: generates no-argument constructor
@AllArgsConstructor  // Lombok: generates all-argument constructor
@Builder  // Lombok: provides builder pattern for object creation
public class UserResponse {

    /**
     * User's unique identifier
     * 
     * <p>Primary key from database, used for references and lookups</p>
     */
    private Long id;

    /**
     * User's unique username
     * 
     * <p>Unique within tenant, used for display and mentions (@username)</p>
     */
    private String username;

    /**
     * User's email address
     * 
     * <p><b>Privacy:</b> May be hidden for non-self/non-admin viewers</p>
     */
    private String email;

    /**
     * User's first name
     * 
     * <p>Used for personalization and display</p>
     */
    private String firstName;

    /**
     * User's last name
     * 
     * <p>Used for formal display and communications</p>
     */
    private String lastName;

    /**
     * Authentication provider used for registration
     * 
     * <p>Indicates how the user registered and authenticates:</p>
     * <ul>
     *   <li>LOCAL: Email/password</li>
     *   <li>GOOGLE: Google OAuth</li>
     *   <li>GITHUB: GitHub OAuth</li>
     *   <li>FACEBOOK: Facebook OAuth</li>
     *   <li>TWITTER: Twitter OAuth</li>
     * </ul>
     */
    private AuthProvider authProvider;

    /**
     * Whether user's email has been verified
     * 
     * <p>Required for full account activation (LOCAL provider)</p>
     * <p>Auto-true for OAuth providers (verified by provider)</p>
     */
    private Boolean emailVerified;

    /**
     * URL to user's profile image/avatar
     * 
     * <p>Can be:</p>
     * <ul>
     *   <li>OAuth provider avatar (Google, GitHub, etc.)</li>
     *   <li>Uploaded image (our CDN)</li>
     *   <li>Gravatar URL</li>
     *   <li>Default avatar (if null)</li>
     * </ul>
     */
    private String imageUrl;

    /**
     * Current account status
     * 
     * <p>Determines account capabilities:</p>
     * <ul>
     *   <li>PENDING: Awaiting verification</li>
     *   <li>ACTIVE: Normal usage</li>
     *   <li>INACTIVE: Dormant account</li>
     *   <li>SUSPENDED: Temporarily restricted</li>
     *   <li>BANNED: Permanently restricted</li>
     * </ul>
     */
    private UserStatus status;

    /**
     * Tenant identifier for multi-tenancy
     * 
     * <p>Isolates user data by tenant</p>
     * <p><b>Privacy:</b> May be hidden for non-admin viewers</p>
     */
    private String tenantId;

    /**
     * Whether Two-Factor Authentication is enabled
     * 
     * <p><b>Privacy:</b> Only visible to self or admin</p>
     * <p>Used by client to show 2FA setup options</p>
     */
    private Boolean using2FA;

    /**
     * User's assigned roles
     * 
     * <p>Role names (e.g., "USER", "ADMIN", "SELLER")</p>
     * <p>Used by client for UI permissions (hide/show features)</p>
     * 
     * <p><b>Example usage in frontend:</b></p>
     * <pre>
     * if (user.roles.includes('ADMIN')) {
     *   // Show admin panel link
     * }
     * 
     * if (user.roles.includes('SELLER')) {
     *   // Show seller dashboard
     * }
     * </pre>
     */
    private Set<String> roles;

    /**
     * Account creation timestamp
     * 
     * <p>When the user registered</p>
     * <p>Used for "Member since" display</p>
     */
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     * 
     * <p>When user profile was last modified</p>
     * <p>Used for change tracking and auditing</p>
     */
    private LocalDateTime updatedAt;
}

