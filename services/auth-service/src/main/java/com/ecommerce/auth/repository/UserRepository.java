package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User Repository - Data access layer for User entity
 * 
 * <p>This interface extends JpaRepository which provides CRUD operations out of the box.
 * Spring Data JPA automatically implements this interface at runtime.</p>
 * 
 * <h2>Provided Methods (from JpaRepository):</h2>
 * <ul>
 *   <li><b>save(User user):</b> Insert or update user</li>
 *   <li><b>findById(String id):</b> Find user by ID</li>
 *   <li><b>findAll():</b> Get all users</li>
 *   <li><b>deleteById(String id):</b> Delete user by ID</li>
 *   <li><b>count():</b> Count total users</li>
 * </ul>
 * 
 * <h2>Custom Query Methods:</h2>
 * <p>Spring Data JPA automatically implements methods based on their names:</p>
 * <pre>
 * findByEmail → SELECT * FROM users WHERE email = ?
 * findByTenantId → SELECT * FROM users WHERE tenant_id = ?
 * findByProviderAndProviderId → SELECT * FROM users WHERE provider = ? AND provider_id = ?
 * </pre>
 * 
 * <h2>Method Naming Conventions:</h2>
 * <ul>
 *   <li><b>findBy...:</b> Returns entity or Optional</li>
 *   <li><b>findAllBy...:</b> Returns List</li>
 *   <li><b>countBy...:</b> Returns Long</li>
 *   <li><b>existsBy...:</b> Returns boolean</li>
 *   <li><b>deleteBy...:</b> Deletes matching entities</li>
 * </ul>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository  // Spring annotation: marks this as a repository component
public interface UserRepository extends JpaRepository<User, String> {
    // JpaRepository<User, String> means:
    // - Entity type: User
    // - Primary key type: String

    /**
     * Find user by email address
     * 
     * <p>Used for login: check if email exists, then verify password</p>
     * 
     * <p>Generated SQL:</p>
     * <pre>
     * SELECT * FROM users WHERE email = ?
     * </pre>
     * 
     * @param email User's email address
     * @return Optional containing user if found, empty if not found
     * 
     * <p>Usage example:</p>
     * <pre>
     * Optional<User> userOpt = userRepository.findByEmail("john@example.com");
     * if (userOpt.isPresent()) {
     *     User user = userOpt.get();
     *     // Verify password, generate tokens, etc.
     * } else {
     *     throw new UserNotFoundException();
     * }
     * </pre>
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user with email exists
     * 
     * <p>Used for registration: prevent duplicate emails</p>
     * 
     * <p>Generated SQL:</p>
     * <pre>
     * SELECT COUNT(*) > 0 FROM users WHERE email = ?
     * </pre>
     * 
     * @param email Email to check
     * @return true if email exists, false otherwise
     * 
     * <p>Usage example:</p>
     * <pre>
     * if (userRepository.existsByEmail(newUser.getEmail())) {
     *     throw new EmailAlreadyExistsException();
     * }
     * </pre>
     */
    boolean existsByEmail(String email);

    /**
     * Find all users belonging to a tenant
     * 
     * <p>Used for tenant management: list all users in organization</p>
     * 
     * <p>Generated SQL:</p>
     * <pre>
     * SELECT * FROM users WHERE tenant_id = ?
     * </pre>
     * 
     * @param tenantId Tenant ID
     * @return List of users (empty list if none found)
     */
    List<User> findByTenantId(String tenantId);

    /**
     * Find user by OAuth2 provider and provider ID
     * 
     * <p>Used for social login: check if user with this social account exists</p>
     * 
     * <p>Generated SQL:</p>
     * <pre>
     * SELECT * FROM users 
     * WHERE provider = ? AND provider_id = ?
     * </pre>
     * 
     * @param provider Authentication provider (GOOGLE, GITHUB, FACEBOOK)
     * @param providerId User ID from the provider
     * @return Optional containing user if found
     * 
     * <p>Usage example:</p>
     * <pre>
     * // User logs in with Google
     * String googleUserId = "123456789";
     * Optional<User> user = userRepository.findByProviderAndProviderId(
     *     AuthProvider.GOOGLE, 
     *     googleUserId
     * );
     * 
     * if (user.isPresent()) {
     *     // Existing user, log them in
     * } else {
     *     // New user, create account
     * }
     * </pre>
     */
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    /**
     * Count users in a tenant
     * 
     * <p>Used for enforcing user limits per tenant</p>
     * 
     * <p>Generated SQL:</p>
     * <pre>
     * SELECT COUNT(*) FROM users WHERE tenant_id = ?
     * </pre>
     * 
     * @param tenantId Tenant ID
     * @return Number of users in tenant
     * 
     * <p>Usage example:</p>
     * <pre>
     * Tenant tenant = tenantRepository.findById(tenantId).get();
     * long currentUsers = userRepository.countByTenantId(tenantId);
     * 
     * if (currentUsers >= tenant.getMaxUsers()) {
     *     throw new UserLimitExceededException();
     * }
     * </pre>
     */
    long countByTenantId(String tenantId);

    /**
     * Find users with 2FA enabled
     * 
     * <p>Used for security analytics: how many users use 2FA?</p>
     * 
     * <p>Generated SQL:</p>
     * <pre>
     * SELECT * FROM users WHERE two_factor_enabled = true
     * </pre>
     * 
     * @return List of users with 2FA enabled
     */
    List<User> findByTwoFactorEnabledTrue();

    /**
     * Find locked accounts
     * 
     * <p>Used for security monitoring and admin management</p>
     * 
     * <p>Generated SQL:</p>
     * <pre>
     * SELECT * FROM users WHERE locked = true
     * </pre>
     * 
     * @return List of locked users
     */
    List<User> findByLockedTrue();

    /**
     * Find users by tenant and enabled status
     * 
     * <p>Used for tenant user management: show only active users</p>
     * 
     * <p>Generated SQL:</p>
     * <pre>
     * SELECT * FROM users 
     * WHERE tenant_id = ? AND enabled = ?
     * </pre>
     * 
     * @param tenantId Tenant ID
     * @param enabled Enabled status (true/false)
     * @return List of matching users
     */
    List<User> findByTenantIdAndEnabled(String tenantId, boolean enabled);

    /**
     * Custom query: Find users with unverified emails
     * 
     * <p>Using @Query annotation for custom JPQL</p>
     * 
     * <p>Used for: Send reminder emails to verify account</p>
     * 
     * @param tenantId Tenant ID
     * @return List of users with unverified emails
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.emailVerified = false")
    List<User> findUnverifiedUsersByTenant(@Param("tenantId") String tenantId);

    /**
     * Custom query: Search users by name or email
     * 
     * <p>Used for admin user search functionality</p>
     * 
     * @param tenantId Tenant ID (for multi-tenancy)
     * @param searchTerm Search term (matches name or email)
     * @return List of matching users
     * 
     * <p>Usage example:</p>
     * <pre>
     * List<User> results = userRepository.searchUsers(
     *     "tenant-123", 
     *     "john"
     * );
     * // Returns users with "john" in name or email
     * </pre>
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<User> searchUsers(
        @Param("tenantId") String tenantId,
        @Param("searchTerm") String searchTerm
    );
}

