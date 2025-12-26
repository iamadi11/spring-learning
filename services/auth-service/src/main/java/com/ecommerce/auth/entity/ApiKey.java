package com.ecommerce.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * API Key Entity - Represents an API key for service-to-service or programmatic authentication
 * 
 * <p>API keys provide an alternative authentication method to JWT tokens, suitable for:
 * - Service-to-service communication
 * - Third-party integrations
 * - Automated scripts and bots
 * - Long-lived authentication (no expiration like JWT)</p>
 * 
 * <h2>API Key Use Cases:</h2>
 * <ul>
 *   <li><b>Microservice Auth:</b> Order Service calls Product Service using API key</li>
 *   <li><b>Third-Party Integration:</b> External service accesses our API</li>
 *   <li><b>Webhooks:</b> External services send webhooks with API key</li>
 *   <li><b>Scripts/Automation:</b> CI/CD scripts access API programmatically</li>
 * </ul>
 * 
 * <h2>API Key Generation:</h2>
 * <pre>
 * 1. User requests API key creation: POST /api/auth/api-keys
 * 2. Server generates random key: SecureRandom.nextBytes(32) → Base64
 * 3. Server hashes key before storage: BCrypt.hashpw(key, BCrypt.gensalt())
 * 4. Server returns key ONCE to user (never shown again)
 * 5. User stores key securely
 * 6. For requests, user includes: X-API-Key: {key}
 * 7. Server hashes incoming key and compares with stored hash
 * </pre>
 * 
 * <h2>Security Considerations:</h2>
 * <ul>
 *   <li><b>Never store plain text:</b> Always hash API keys (like passwords)</li>
 *   <li><b>Show key once:</b> Display key only at creation time</li>
 *   <li><b>Scoped permissions:</b> API key has limited permissions</li>
 *   <li><b>Rotation:</b> Allow users to regenerate keys</li>
 *   <li><b>Revocation:</b> Easy to disable compromised keys</li>
 *   <li><b>Rate limiting:</b> Apply rate limits per API key</li>
 *   <li><b>Expiration:</b> Option to set expiry date</li>
 * </ul>
 * 
 * <h2>Example Usage:</h2>
 * <pre>
 * // Create API key
 * POST /api/auth/api-keys
 * {
 *   "name": "Production Integration",
 *   "permissions": ["PRODUCT_VIEW", "ORDER_CREATE"]
 * }
 * 
 * Response:
 * {
 *   "id": "api-key-123",
 *   "key": "eck_live_4eC39HqLyjWDarjtT1zdp7dc",  // Show ONCE
 *   "name": "Production Integration",
 *   "created_at": "2024-01-01T12:00:00"
 * }
 * 
 * // Use API key in request
 * GET /api/products
 * X-API-Key: eck_live_4eC39HqLyjWDarjtT1zdp7dc
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity  // JPA annotation: marks this class as a database entity
@Table(name = "api_keys", indexes = {
    // Index for fast API key lookup (hash column)
    @Index(name = "idx_api_key_hash", columnList = "key_hash"),
    // Index for user's API keys
    @Index(name = "idx_api_key_user", columnList = "user_id")
})
@Data  // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: generates no-args constructor (required by JPA)
@AllArgsConstructor  // Lombok: generates all-args constructor
public class ApiKey {

    /**
     * Unique identifier for the API key
     */
    @Id  // JPA: primary key
    @GeneratedValue(strategy = GenerationType.UUID)  // Auto-generate UUID
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    /**
     * Hashed API key value
     * 
     * <p><b>CRITICAL SECURITY:</b> NEVER store the plain API key!</p>
     * 
     * <p>Storage process:</p>
     * <pre>
     * 1. Generate random key: 
     *    byte[] bytes = new byte[32];
     *    SecureRandom.getInstanceStrong().nextBytes(bytes);
     *    String key = "eck_" + Base64.getUrlEncoder().encodeToString(bytes);
     * 
     * 2. Hash key before storage:
     *    String hash = BCrypt.hashpw(key, BCrypt.gensalt(12));
     * 
     * 3. Store hash in database (this field)
     * 4. Return plain key to user ONCE
     * 5. Never store or log plain key
     * </pre>
     * 
     * <p>Validation process:</p>
     * <pre>
     * 1. Receive API key from request header: X-API-Key
     * 2. Look up all API keys (or use prefix to narrow search)
     * 3. For each key, check: BCrypt.checkpw(incomingKey, storedHash)
     * 4. If match found, authenticate user
     * </pre>
     */
    @Column(name = "key_hash", nullable = false, length = 60)  // BCrypt hash length
    private String keyHash;

    /**
     * Key prefix - first 8 characters of the key
     * 
     * <p>Used for:</p>
     * <ul>
     *   <li>Quick identification in logs (without exposing full key)</li>
     *   <li>Faster lookup (index on prefix)</li>
     *   <li>Display in UI: "Key ending in: ...7dc"</li>
     * </ul>
     * 
     * <p>Example: If key is "eck_live_4eC39HqL...", prefix is "eck_live"</p>
     */
    @Column(name = "key_prefix", nullable = false, length = 20)
    private String keyPrefix;

    /**
     * User-friendly name for the API key
     * 
     * <p>Helps user identify the key's purpose</p>
     * <p>Examples: "Production Server", "CI/CD Pipeline", "Mobile App Integration"</p>
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * User who owns this API key
     * 
     * <p>Many-to-One relationship: A user can have multiple API keys</p>
     * 
     * <p>When API key is used for authentication:</p>
     * <pre>
     * 1. Validate API key
     * 2. Load associated user
     * 3. Apply user's roles and permissions
     * 4. Process request with user context
     * </pre>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Scoped permissions for this API key
     * 
     * <p>API key can have subset of user's permissions for security.
     * Principle of least privilege: give only necessary permissions.</p>
     * 
     * <p>Stored as comma-separated string of permission names</p>
     * <p>Example: "PRODUCT_VIEW,ORDER_CREATE,INVENTORY_VIEW"</p>
     * 
     * <p>When validating authorization:</p>
     * <pre>
     * 1. Parse permissions string into Set<String>
     * 2. Check if required permission is in set
     * 3. If yes, allow operation
     * 4. If no, return 403 Forbidden
     * </pre>
     */
    @Column(name = "permissions", length = 1000)
    private String permissions;

    /**
     * Whether the API key is active
     * 
     * <p>false: Key is revoked/disabled (cannot be used for authentication)
     * true: Key is active and can be used</p>
     * 
     * <p>Revocation reasons:</p>
     * <ul>
     *   <li>Key compromised</li>
     *   <li>Integration discontinued</li>
     *   <li>User manually revoked key</li>
     *   <li>Security policy enforcement</li>
     * </ul>
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * Last time this API key was used
     * 
     * <p>Updated every time key is used successfully.
     * Useful for:</p>
     * <ul>
     *   <li>Detecting unused keys (cleanup)</li>
     *   <li>Security monitoring (unusual usage patterns)</li>
     *   <li>Usage analytics</li>
     * </ul>
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * Expiration date for the API key
     * 
     * <p>null: Key never expires
     * set: Key expires at this date/time</p>
     * 
     * <p>Security best practice: Set expiration for temporary integrations</p>
     * 
     * <p>Validation:</p>
     * <pre>
     * if (apiKey.getExpiresAt() != null && 
     *     LocalDateTime.now().isAfter(apiKey.getExpiresAt())) {
     *     throw new ApiKeyExpiredException();
     * }
     * </pre>
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Timestamp when the API key was created
     */
    @CreationTimestamp  // Hibernate: auto-set on insert
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ==================== Helper Methods ====================

    /**
     * Check if API key is expired
     * 
     * @return true if key is expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if API key can be used
     * 
     * @return true if key is active and not expired
     */
    public boolean canBeUsed() {
        return active && !isExpired();
    }

    /**
     * Update last used timestamp
     */
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * Revoke this API key
     */
    public void revoke() {
        this.active = false;
    }
}

