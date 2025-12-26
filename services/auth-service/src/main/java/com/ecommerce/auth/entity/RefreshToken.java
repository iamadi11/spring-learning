package com.ecommerce.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Refresh Token Entity - Stores refresh tokens for JWT authentication
 * 
 * <p>Refresh tokens allow users to obtain new access tokens without re-authenticating.
 * This provides a balance between security (short-lived access tokens) and user
 * experience (don't require frequent login).</p>
 * 
 * <h2>JWT Token Strategy:</h2>
 * <pre>
 * Access Token:
 *   - Short-lived (15-60 minutes)
 *   - Contains user info, roles, permissions
 *   - Used for API requests
 *   - Stateless (not stored in database)
 * 
 * Refresh Token:
 *   - Long-lived (7-30 days)
 *   - Used only to get new access token
 *   - Stored in database (can be revoked)
 *   - Rotated on each use (security best practice)
 * </pre>
 * 
 * <h2>Token Lifecycle:</h2>
 * <pre>
 * 1. Login:
 *    POST /api/auth/login
 *    Response:
 *    {
 *      "accessToken": "eyJhbGc...",  // Expires in 15 min
 *      "refreshToken": "def50200...", // Expires in 7 days
 *      "expiresIn": 900
 *    }
 * 
 * 2. Use Access Token:
 *    GET /api/products
 *    Authorization: Bearer eyJhbGc...
 *    → Success (while token valid)
 * 
 * 3. Access Token Expires:
 *    GET /api/products
 *    Authorization: Bearer eyJhbGc...
 *    → 401 Unauthorized (token expired)
 * 
 * 4. Refresh Access Token:
 *    POST /api/auth/refresh
 *    {
 *      "refreshToken": "def50200..."
 *    }
 *    Response:
 *    {
 *      "accessToken": "eyJhbGc...",      // New access token
 *      "refreshToken": "abc12345...",    // New refresh token (rotated)
 *      "expiresIn": 900
 *    }
 * 
 * 5. Old refresh token is revoked
 * 6. Continue using new tokens
 * </pre>
 * 
 * <h2>Token Rotation (Security Best Practice):</h2>
 * <pre>
 * Why rotate refresh tokens?
 * - If refresh token is stolen, attacker has limited time
 * - Each use creates new token, old one invalid
 * - Detects token theft: If old token used, revoke all tokens for user
 * 
 * How it works:
 * 1. User uses refresh token to get new access token
 * 2. Server:
 *    - Validates refresh token
 *    - Creates new access token
 *    - Creates new refresh token
 *    - Marks old refresh token as used/revoked
 *    - Returns both new tokens
 * 3. Old refresh token cannot be reused
 * </pre>
 * 
 * <h2>Token Revocation:</h2>
 * <pre>
 * Revoke tokens when:
 * - User logs out
 * - User changes password
 * - Security breach detected
 * - Token theft suspected
 * - Admin action
 * 
 * How to revoke:
 * 1. Set revoked = true
 * 2. Optionally delete all user's refresh tokens
 * 3. Access tokens remain valid until expiry (stateless)
 * 4. Consider adding access token blacklist for immediate revocation
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity  // JPA annotation: marks this class as a database entity
@Table(name = "refresh_tokens", indexes = {
    // Index for fast token lookup
    @Index(name = "idx_refresh_token", columnList = "token"),
    // Index for user's tokens
    @Index(name = "idx_refresh_token_user", columnList = "user_id")
})
@Data  // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: generates no-args constructor (required by JPA)
@AllArgsConstructor  // Lombok: generates all-args constructor
@Builder  // Lombok: generates builder pattern
public class RefreshToken {

    /**
     * Unique identifier for the refresh token record
     */
    @Id  // JPA: primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    /**
     * The refresh token value
     * 
     * <p><b>Security Options:</b></p>
     * <ol>
     *   <li><b>Store plain token:</b> Faster lookup, but less secure</li>
     *   <li><b>Store hashed token:</b> More secure, but slower lookup</li>
     * </ol>
     * 
     * <p>For this implementation: Store hashed token for security</p>
     * 
     * <p>Generation process:</p>
     * <pre>
     * 1. Generate random token:
     *    byte[] bytes = new byte[64];
     *    SecureRandom.getInstanceStrong().nextBytes(bytes);
     *    String token = Base64.getUrlEncoder().encodeToString(bytes);
     * 
     * 2. Hash token before storage:
     *    String hash = BCrypt.hashpw(token, BCrypt.gensalt());
     * 
     * 3. Store hash in database
     * 4. Return plain token to user
     * </pre>
     * 
     * <p>Note: Some implementations store plain token for simplicity.
     * Trade-off: Performance vs. Security</p>
     */
    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    /**
     * User who owns this refresh token
     * 
     * <p>Many-to-One relationship: A user can have multiple refresh tokens
     * (one per device/browser)</p>
     * 
     * <p>Example: User has tokens for:
     * - Mobile app
     * - Desktop browser
     * - Tablet app
     * Each device has its own refresh token</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Token expiration date/time
     * 
     * <p>Typically 7-30 days from creation</p>
     * 
     * <p>Validation:</p>
     * <pre>
     * if (Instant.now().isAfter(refreshToken.getExpiryDate())) {
     *     throw new TokenExpiredException("Refresh token expired");
     * }
     * </pre>
     */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * Whether the token has been revoked
     * 
     * <p>true: Token is revoked (cannot be used)
     * false: Token is valid (can be used if not expired)</p>
     * 
     * <p>Revocation scenarios:</p>
     * <ul>
     *   <li>User logs out</li>
     *   <li>Token is rotated (old token revoked)</li>
     *   <li>Security breach</li>
     *   <li>Password change</li>
     * </ul>
     */
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private Boolean revoked = false;

    /**
     * IP address from which the token was created
     * 
     * <p>Used for security monitoring:
     * - Detect token usage from different IP
     * - Track suspicious activity
     * - Geographic analysis</p>
     */
    @Column(name = "created_from_ip", length = 45)  // IPv6 max length
    private String createdFromIp;

    /**
     * Device/User-Agent information
     * 
     * <p>Stored for:
     * - User can see list of active devices
     * - Security monitoring
     * - Device-specific token revocation</p>
     * 
     * <p>Example: "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X)"</p>
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Timestamp when the token was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when the token was last used
     * 
     * <p>Updated each time token is used to refresh access token</p>
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    // ==================== Helper Methods ====================

    /**
     * Check if token is expired
     * 
     * @return true if token is past expiration date
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    /**
     * Check if token can be used
     * 
     * @return true if token is valid (not revoked, not expired)
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }

    /**
     * Revoke this token
     */
    public void revoke() {
        this.revoked = true;
    }

    /**
     * Update last used timestamp
     */
    public void updateLastUsed() {
        this.lastUsedAt = Instant.now();
    }

    /**
     * Check if token is about to expire (within 24 hours)
     * 
     * @return true if token expires soon
     */
    public boolean isExpiringSoon() {
        return !isExpired() && 
               Instant.now().plusSeconds(24 * 3600).isAfter(expiryDate);
    }
}

