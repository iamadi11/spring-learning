package com.ecommerce.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User Entity - Represents a user account in the system
 * 
 * <p>This entity stores user account information including credentials, profile data,
 * and relationships to roles, tenants, and authentication settings.</p>
 * 
 * <h2>Database Table: users</h2>
 * <pre>
 * Columns:
 * - id: UUID primary key
 * - email: Unique email address (used for login)
 * - password: BCrypt hashed password
 * - name: User's full name
 * - email_verified: Whether email is verified
 * - enabled: Account enabled/disabled
 * - locked: Account locked (security)
 * - two_factor_enabled: 2FA enabled
 * - two_factor_secret: TOTP secret key
 * - provider: Authentication provider (LOCAL, GOOGLE, GITHUB, FACEBOOK)
 * - provider_id: ID from external provider
 * - tenant_id: Foreign key to tenant
 * - created_at: Timestamp of account creation
 * - updated_at: Timestamp of last update
 * - last_login_at: Timestamp of last successful login
 * </pre>
 * 
 * <h2>Relationships:</h2>
 * <ul>
 *   <li><b>Many-to-Many with Role:</b> A user can have multiple roles</li>
 *   <li><b>Many-to-One with Tenant:</b> A user belongs to one tenant</li>
 *   <li><b>One-to-Many with ApiKey:</b> A user can have multiple API keys</li>
 * </ul>
 * 
 * <h2>Authentication Flow:</h2>
 * <pre>
 * 1. User Registration:
 *    - Create User entity with hashed password
 *    - email_verified = false
 *    - Send verification email
 * 
 * 2. Email Verification:
 *    - User clicks link in email
 *    - Set email_verified = true
 * 
 * 3. Login:
 *    - Verify email and password
 *    - Check if account enabled and not locked
 *    - If 2FA enabled, require 2FA code
 *    - Update last_login_at
 *    - Generate JWT tokens
 * 
 * 4. Social Login:
 *    - User authenticates with Google/GitHub/Facebook
 *    - Create or update User entity
 *    - provider = GOOGLE, provider_id = google_user_id
 *    - email_verified = true (trusted provider)
 * </pre>
 * 
 * <h2>Security Considerations:</h2>
 * <ul>
 *   <li><b>Password Storage:</b> Never store plain text passwords</li>
 *   <li><b>BCrypt Hashing:</b> Use strength 12 (2^12 iterations)</li>
 *   <li><b>Account Locking:</b> Lock account after failed login attempts</li>
 *   <li><b>2FA Secret:</b> Store encrypted, never expose in API responses</li>
 *   <li><b>Tenant Isolation:</b> Always filter queries by tenant_id</li>
 * </ul>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity  // JPA annotation: marks this class as a database entity
@Table(name = "users", indexes = {
    // Index on email for fast login queries
    @Index(name = "idx_user_email", columnList = "email"),
    // Index on tenant_id for multi-tenancy queries
    @Index(name = "idx_user_tenant", columnList = "tenant_id"),
    // Composite index for provider login
    @Index(name = "idx_user_provider", columnList = "provider, provider_id")
})
@Data  // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: generates no-args constructor (required by JPA)
@AllArgsConstructor  // Lombok: generates all-args constructor
public class User {

    /**
     * Unique identifier for the user
     * 
     * <p>Using UUID instead of auto-increment for:</p>
     * <ul>
     *   <li>Global uniqueness across distributed systems</li>
     *   <li>Security (non-sequential, unpredictable)</li>
     *   <li>Easier data migration and merging</li>
     * </ul>
     */
    @Id  // JPA: primary key
    @GeneratedValue(strategy = GenerationType.UUID)  // Auto-generate UUID
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    /**
     * Email address - used as username for login
     * 
     * <p>Must be unique across the system. Used for:</p>
     * <ul>
     *   <li>User identification</li>
     *   <li>Login credential</li>
     *   <li>Communication (verification, password reset)</li>
     * </ul>
     */
    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    /**
     * Password hash - BCrypt hashed password
     * 
     * <p><b>Security Note:</b> This field should NEVER be exposed in API responses.
     * Use @JsonIgnore or exclude from DTOs.</p>
     * 
     * <p>BCrypt format: $2a$12$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     * - $2a: BCrypt identifier
     * - $12: Cost factor (2^12 iterations)
     * - Remaining: Salt + Hash</p>
     * 
     * <p>When user changes password:</p>
     * <pre>
     * String rawPassword = "newPassword123";
     * String hashedPassword = passwordEncoder.encode(rawPassword);
     * user.setPassword(hashedPassword);
     * </pre>
     */
    @Column(name = "password", length = 255)
    private String password;

    /**
     * User's full name
     * 
     * <p>Displayed in UI, emails, and invoices</p>
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Email verification status
     * 
     * <p>true: Email has been verified (user clicked verification link)
     * false: Email not yet verified (pending verification)</p>
     * 
     * <p>Unverified users may have limited functionality until verification</p>
     */
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    /**
     * Account enabled status
     * 
     * <p>false: Account disabled by admin (user cannot login)
     * true: Account active and can be used</p>
     * 
     * <p>Use cases for disabling:</p>
     * <ul>
     *   <li>Account suspension due to policy violation</li>
     *   <li>Temporary account deactivation</li>
     *   <li>Soft delete (instead of hard delete)</li>
     * </ul>
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * Account locked status
     * 
     * <p>true: Account locked (security measure)
     * false: Account not locked</p>
     * 
     * <p>Account is locked when:</p>
     * <ul>
     *   <li>Too many failed login attempts</li>
     *   <li>Suspicious activity detected</li>
     *   <li>Admin manually locks account</li>
     * </ul>
     * 
     * <p>To unlock: Admin action or automatic after timeout period</p>
     */
    @Column(name = "locked", nullable = false)
    private Boolean locked = false;

    /**
     * Two-Factor Authentication enabled status
     * 
     * <p>true: User has enabled 2FA (requires TOTP code on login)
     * false: 2FA not enabled (password only)</p>
     */
    @Column(name = "two_factor_enabled", nullable = false)
    private Boolean twoFactorEnabled = false;

    /**
     * Two-Factor Authentication secret key
     * 
     * <p>Base32-encoded secret for TOTP generation.
     * Used by Google Authenticator app to generate 6-digit codes.</p>
     * 
     * <p><b>Security Note:</b> This field is HIGHLY SENSITIVE.
     * - Never expose in API responses
     * - Should be encrypted at rest
     * - Only used for code validation</p>
     * 
     * <p>TOTP Algorithm:</p>
     * <pre>
     * 1. Get current Unix timestamp
     * 2. Divide by time step (30 seconds)
     * 3. HMAC-SHA1(secret, time_step)
     * 4. Extract 6-digit code from hash
     * </pre>
     */
    @Column(name = "two_factor_secret", length = 32)
    private String twoFactorSecret;

    /**
     * Authentication provider type
     * 
     * <p>Indicates how the user account was created:</p>
     * <ul>
     *   <li><b>LOCAL:</b> Traditional username/password registration</li>
     *   <li><b>GOOGLE:</b> Google OAuth2 login</li>
     *   <li><b>GITHUB:</b> GitHub OAuth2 login</li>
     *   <li><b>FACEBOOK:</b> Facebook OAuth2 login</li>
     * </ul>
     * 
     * <p>Social login users may not have a password (OAuth2 handles authentication)</p>
     */
    @Enumerated(EnumType.STRING)  // Store enum as string in database
    @Column(name = "provider", nullable = false, length = 20)
    private AuthProvider provider = AuthProvider.LOCAL;

    /**
     * Provider-specific user ID
     * 
     * <p>For social login, this is the user ID from the OAuth2 provider:</p>
     * <ul>
     *   <li>Google: Google user ID</li>
     *   <li>GitHub: GitHub user ID</li>
     *   <li>Facebook: Facebook user ID</li>
     * </ul>
     * 
     * <p>For LOCAL provider, this field is null</p>
     * 
     * <p>Used to link accounts: if user logs in with Google and we find
     * existing account with same email, we can merge/link accounts</p>
     */
    @Column(name = "provider_id", length = 255)
    private String providerId;

    /**
     * Tenant ID - for multi-tenancy support
     * 
     * <p>Each user belongs to exactly one tenant (organization/company).
     * Tenant isolation ensures data privacy between different organizations.</p>
     * 
     * <p>Example tenant structure:</p>
     * <pre>
     * Tenant A (ID: tenant-123):
     *   - Users: alice@companyA.com, bob@companyA.com
     *   - Data: Orders, Products visible only to this tenant
     * 
     * Tenant B (ID: tenant-456):
     *   - Users: charlie@companyB.com, diana@companyB.com
     *   - Data: Separate Orders, Products
     * </pre>
     * 
     * <p>JWT token includes tenant_id, and all queries are filtered:
     * SELECT * FROM orders WHERE tenant_id = :tenantId AND user_id = :userId</p>
     */
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    /**
     * Roles assigned to this user
     * 
     * <p>Many-to-Many relationship: A user can have multiple roles,
     * and a role can be assigned to multiple users.</p>
     * 
     * <p>Join table: user_roles</p>
     * <pre>
     * Columns:
     * - user_id: Foreign key to users.id
     * - role_id: Foreign key to roles.id
     * </pre>
     * 
     * <p>Example roles:</p>
     * <ul>
     *   <li><b>USER:</b> Basic user (can browse, order)</li>
     *   <li><b>ADMIN:</b> Administrator (full access)</li>
     *   <li><b>PRODUCT_MANAGER:</b> Can manage products</li>
     *   <li><b>CUSTOMER_SUPPORT:</b> Can view orders, assist customers</li>
     * </ul>
     * 
     * <p>Loading strategy: LAZY (roles loaded only when accessed)
     * Prevents N+1 query problem when loading many users</p>
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",  // Join table name
        joinColumns = @JoinColumn(name = "user_id"),  // This entity's foreign key
        inverseJoinColumns = @JoinColumn(name = "role_id")  // Other entity's foreign key
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Timestamp when the user account was created
     * 
     * <p>Automatically set by Hibernate when entity is first persisted.
     * Cannot be updated after creation (updatable = false).</p>
     * 
     * <p>Uses database server time (not application server time)
     * to ensure consistency across distributed systems.</p>
     */
    @CreationTimestamp  // Hibernate: auto-set on insert
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the user account was last updated
     * 
     * <p>Automatically updated by Hibernate whenever entity is modified.
     * Useful for:</p>
     * <ul>
     *   <li>Audit trails</li>
     *   <li>Conflict resolution in distributed systems</li>
     *   <li>Cache invalidation</li>
     * </ul>
     */
    @UpdateTimestamp  // Hibernate: auto-update on modify
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Timestamp of last successful login
     * 
     * <p>Updated every time user successfully logs in.
     * Useful for:</p>
     * <ul>
     *   <li>Security monitoring (detect unusual access patterns)</li>
     *   <li>Inactive account cleanup</li>
     *   <li>User analytics</li>
     * </ul>
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // ==================== Helper Methods ====================

    /**
     * Add a role to this user
     * 
     * @param role Role to add
     */
    public void addRole(Role role) {
        this.roles.add(role);
    }

    /**
     * Remove a role from this user
     * 
     * @param role Role to remove
     */
    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    /**
     * Check if user has a specific role
     * 
     * @param roleName Role name to check
     * @return true if user has the role
     */
    public boolean hasRole(String roleName) {
        return roles.stream()
            .anyMatch(role -> role.getName().equals(roleName));
    }

    /**
     * Check if this is a social login account
     * 
     * @return true if account created via OAuth2 provider
     */
    public boolean isSocialLogin() {
        return provider != AuthProvider.LOCAL;
    }

    /**
     * Check if account can login
     * 
     * <p>Account can login if:</p>
     * <ul>
     *   <li>Email is verified</li>
     *   <li>Account is enabled</li>
     *   <li>Account is not locked</li>
     * </ul>
     * 
     * @return true if account can login
     */
    public boolean canLogin() {
        return emailVerified && enabled && !locked;
    }
}

/**
 * Authentication Provider Enum
 * 
 * <p>Defines supported authentication methods</p>
 */
enum AuthProvider {
    LOCAL,      // Username/password
    GOOGLE,     // Google OAuth2
    GITHUB,     // GitHub OAuth2
    FACEBOOK    // Facebook OAuth2
}

