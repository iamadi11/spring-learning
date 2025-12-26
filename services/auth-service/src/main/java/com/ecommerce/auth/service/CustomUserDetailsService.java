package com.ecommerce.auth.service;

import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetails Service
 * 
 * <p>Implementation of Spring Security's UserDetailsService interface.
 * Used by Spring Security to load user data during authentication.</p>
 * 
 * <h2>Role in Authentication:</h2>
 * <pre>
 * Authentication Flow:
 * 
 * 1. User submits login credentials (email + password)
 * 
 * 2. AuthenticationManager receives credentials
 * 
 * 3. DaoAuthenticationProvider calls loadUserByUsername():
 *    - Pass email as username parameter
 *    - This method queries database for user
 *    - Returns UserDetails object
 * 
 * 4. DaoAuthenticationProvider compares passwords:
 *    - Uses PasswordEncoder.matches()
 *    - Compares submitted password with UserDetails.getPassword()
 *    - If match, authentication successful
 * 
 * 5. If successful:
 *    - Create authenticated Authentication object
 *    - Return to AuthenticationManager
 *    - AuthenticationManager returns to caller
 * 
 * 6. If failed:
 *    - Throw BadCredentialsException
 *    - Return 401 Unauthorized
 * </pre>
 * 
 * <h2>UserDetails vs User Entity:</h2>
 * <pre>
 * User Entity:
 * - JPA entity (@Entity)
 * - Maps to database table
 * - Contains all user data (profile, settings, etc.)
 * - Implements UserDetails interface
 * 
 * UserDetails Interface (Spring Security):
 * - Defines contract for user authentication
 * - Required methods:
 *   + getUsername()          // User identifier
 *   + getPassword()          // Hashed password
 *   + getAuthorities()       // Roles and permissions
 *   + isAccountNonExpired()  // Account validity
 *   + isAccountNonLocked()   // Account status
 *   + isCredentialsNonExpired() // Password validity
 *   + isEnabled()            // Account enabled
 * 
 * Our User entity implements UserDetails, so we can use it directly
 * without creating a separate UserDetails implementation.
 * </pre>
 * 
 * <h2>Why Transactional:</h2>
 * <pre>
 * @Transactional on loadUserByUsername():
 * - Opens database transaction
 * - Loads user entity
 * - Eagerly fetches lazy relationships (roles, permissions)
 * - Closes transaction after method returns
 * 
 * Without @Transactional:
 * - LazyInitializationException when accessing roles
 * - Roles are fetched lazily (FetchType.EAGER on roles in User entity)
 * - Session closed before accessing roles
 * 
 * With @Transactional:
 * - Session stays open during method execution
 * - All relationships loaded within transaction
 * - No LazyInitializationException
 * </pre>
 * 
 * <h2>Usage Examples:</h2>
 * <pre>
 * 1. During Login (Automatic):
 *    authenticationManager.authenticate(
 *        new UsernamePasswordAuthenticationToken(email, password)
 *    );
 *    // Internally calls loadUserByUsername(email)
 * 
 * 2. Manual Usage (JWT Token Validation):
 *    String email = jwtTokenService.getEmailFromToken(token);
 *    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
 *    // Use userDetails to create Authentication
 * 
 * 3. Getting Current User:
 *    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 *    String email = auth.getName();
 *    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
 * </pre>
 * 
 * <h2>Error Handling:</h2>
 * <pre>
 * UsernameNotFoundException:
 * - Thrown when user not found in database
 * - Spring Security catches this
 * - Converts to BadCredentialsException (generic error for security)
 * - Client receives: "Invalid email or password"
 * 
 * Why generic error?
 * - Security: Don't reveal if email exists
 * - Prevent user enumeration attacks
 * - Attacker can't determine valid emails
 * 
 * Example:
 * // Bad (reveals information)
 * if (!userExists) return "Email not found";
 * if (!passwordMatch) return "Wrong password";
 * 
 * // Good (generic error)
 * if (!userExists || !passwordMatch) return "Invalid credentials";
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service  // Marks this as a Spring service component
public class CustomUserDetailsService implements UserDetailsService {

    // Logger for debugging and error tracking
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    // Repository for database operations on User entity
    private final UserRepository userRepository;

    /**
     * Constructor with dependency injection
     * 
     * @param userRepository Repository for user database operations
     */
    @Autowired  // Spring will inject UserRepository bean
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Load User by Username (Email)
     * 
     * <p>Loads user from database by email (used as username).
     * Called automatically by Spring Security during authentication.</p>
     * 
     * <p><b>Method Flow:</b></p>
     * <pre>
     * 1. Receive email as parameter
     * 2. Log attempt (for debugging/auditing)
     * 3. Query database: userRepository.findByEmail(email)
     * 4. If found:
     *    - Log success
     *    - Return User object (implements UserDetails)
     * 5. If not found:
     *    - Log failure
     *    - Throw UsernameNotFoundException
     * </pre>
     * 
     * <p><b>@Transactional:</b></p>
     * <ul>
     *   <li>Opens database transaction</li>
     *   <li>Loads user with all relationships (roles, permissions)</li>
     *   <li>Prevents LazyInitializationException</li>
     *   <li>Commits transaction after method returns</li>
     * </ul>
     * 
     * @param email User's email address (used as username)
     * @return UserDetails object (User entity in our case)
     * @throws UsernameNotFoundException if user not found
     */
    @Override  // Implementing UserDetailsService interface method
    @Transactional(readOnly = true)  // Open transaction for reading (includes lazy loading)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Log the authentication attempt (for debugging and auditing)
        // In production, you might want to track failed login attempts
        logger.debug("Attempting to load user by email: {}", email);
        
        // Query database for user by email
        // findByEmail() returns Optional<User>
        // If user not found, Optional.empty()
        // orElseThrow() throws exception if Optional is empty
        User user = userRepository.findByEmail(email)
                // If user not found, throw UsernameNotFoundException
                // This is the standard Spring Security exception for user not found
                .orElseThrow(() -> {
                    // Log the failure (helps with debugging and security monitoring)
                    logger.warn("User not found with email: {}", email);
                    
                    // Throw exception with generic message
                    // Spring Security will catch this and return "Bad credentials" to client
                    // This prevents revealing whether the email exists (security best practice)
                    return new UsernameNotFoundException("User not found with email: " + email);
                });
        
        // Log successful user load (for debugging)
        // Don't log sensitive info like password (even hashed)
        logger.debug("User loaded successfully: {} (ID: {}, Status: {})", 
                user.getEmail(), user.getId(), user.getStatus());
        
        // Return the User object
        // User entity implements UserDetails interface, so it can be returned directly
        // Spring Security will use the UserDetails methods:
        // - getUsername() for user identifier
        // - getPassword() for password comparison
        // - getAuthorities() for role-based access control
        // - isAccountNonLocked() and isEnabled() to check account status
        return user;
    }

    /**
     * Load User by Username and Tenant ID
     * 
     * <p>Multi-tenancy variant: loads user by username within specific tenant.
     * Username is unique only within a tenant, not globally.</p>
     * 
     * <p><b>Multi-Tenancy Consideration:</b></p>
     * <ul>
     *   <li>Email is globally unique</li>
     *   <li>Username is unique per tenant</li>
     *   <li>Same username can exist in different tenants</li>
     *   <li>Need both username and tenantId for unique lookup</li>
     * </ul>
     * 
     * <p><b>Usage:</b></p>
     * <pre>
     * // Extract tenant from request (header, subdomain, etc.)
     * String tenantId = TenantContext.getCurrentTenant();
     * 
     * // Load user with tenant context
     * UserDetails user = loadUserByUsernameAndTenant(username, tenantId);
     * </pre>
     * 
     * @param username User's username
     * @param tenantId Tenant identifier
     * @return UserDetails object (User entity)
     * @throws UsernameNotFoundException if user not found in tenant
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsernameAndTenant(String username, String tenantId) 
            throws UsernameNotFoundException {
        // Log the attempt with tenant context
        logger.debug("Attempting to load user by username: {} in tenant: {}", username, tenantId);
        
        // Query database for user by username and tenant ID
        User user = userRepository.findByUsernameAndTenantId(username, tenantId)
                .orElseThrow(() -> {
                    // Log the failure with tenant context
                    logger.warn("User not found with username: {} in tenant: {}", username, tenantId);
                    
                    // Throw exception (generic message for security)
                    return new UsernameNotFoundException(
                            "User not found with username: " + username + " in tenant: " + tenantId
                    );
                });
        
        // Log success
        logger.debug("User loaded successfully: {} in tenant: {} (ID: {})", 
                user.getUsername(), user.getTenantId(), user.getId());
        
        // Return user
        return user;
    }

    /**
     * Load User Entity by Email
     * 
     * <p>Convenience method to get User entity directly (not just UserDetails).
     * Useful when you need access to User entity methods beyond UserDetails interface.</p>
     * 
     * <p><b>Use Cases:</b></p>
     * <ul>
     *   <li>Profile updates (need full entity for JPA)</li>
     *   <li>Custom user methods not in UserDetails</li>
     *   <li>Relationship access (orders, reviews, etc.)</li>
     * </ul>
     * 
     * @param email User's email address
     * @return User entity
     * @throws UsernameNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User loadUserEntityByEmail(String email) throws UsernameNotFoundException {
        // Reuse loadUserByUsername and cast to User
        // Safe because we know loadUserByUsername returns User
        return (User) loadUserByUsername(email);
    }
}

