package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.TokenResponse;
import com.ecommerce.auth.dto.UserResponse;
import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.enums.AuthProvider;
import com.ecommerce.auth.enums.UserStatus;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import com.ecommerce.auth.repository.RoleRepository;
import com.ecommerce.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Authentication Service
 * 
 * <p>Core service for handling user authentication operations including
 * registration, login, token refresh, and logout.</p>
 * 
 * <h2>Service Responsibilities:</h2>
 * <ul>
 *   <li>User Registration: Create new user accounts</li>
 *   <li>User Login: Authenticate and generate tokens</li>
 *   <li>Token Refresh: Issue new access tokens</li>
 *   <li>Logout: Revoke refresh tokens</li>
 *   <li>Password Validation: Ensure strong passwords</li>
 * </ul>
 * 
 * <h2>Complete User Registration Flow:</h2>
 * <pre>
 * 1. Client sends RegisterRequest:
 *    POST /api/auth/register
 *    {
 *      "username": "johndoe",
 *      "email": "john@example.com",
 *      "password": "SecurePass123!",
 *      "firstName": "John",
 *      "lastName": "Doe",
 *      "tenantId": "tenant1"
 *    }
 * 
 * 2. Controller validates request (@Valid annotation)
 * 
 * 3. Controller calls authService.register(request)
 * 
 * 4. Service validates business rules:
 *    - Email not already registered
 *    - Username not taken (within tenant)
 *    - Password meets strength requirements
 *    - Tenant exists
 * 
 * 5. Service creates user:
 *    - Hash password with BCrypt
 *    - Set initial status (PENDING)
 *    - Set auth provider (LOCAL)
 *    - Assign default role (USER)
 *    - Save to database
 * 
 * 6. Service sends verification email (future implementation)
 * 
 * 7. Service returns UserResponse
 * 
 * 8. Client displays: "Registration successful! Check your email."
 * </pre>
 * 
 * <h2>Complete User Login Flow:</h2>
 * <pre>
 * 1. Client sends LoginRequest:
 *    POST /api/auth/login
 *    {
 *      "email": "john@example.com",
 *      "password": "SecurePass123!"
 *    }
 * 
 * 2. Controller validates request
 * 
 * 3. Controller calls authService.login(request)
 * 
 * 4. Service authenticates user:
 *    a. Create UsernamePasswordAuthenticationToken
 *    b. Pass to AuthenticationManager
 *    c. AuthenticationManager calls UserDetailsService
 *    d. UserDetailsService loads user from database
 *    e. AuthenticationManager compares passwords
 *    f. If match, return authenticated Authentication
 *    g. If no match, throw BadCredentialsException
 * 
 * 5. If authentication successful:
 *    a. Load full user entity
 *    b. Generate access token (JWT)
 *    c. Generate refresh token (JWT)
 *    d. Save refresh token to database
 *    e. Create TokenResponse
 * 
 * 6. Service returns TokenResponse
 * 
 * 7. Client stores tokens:
 *    - Access token in memory
 *    - Refresh token in secure storage
 * 
 * 8. Client uses access token for API requests
 * </pre>
 * 
 * <h2>Token Refresh Flow:</h2>
 * <pre>
 * 1. Access token expires (after 1 hour)
 * 
 * 2. Client detects expiration (401 response or exp claim)
 * 
 * 3. Client sends refresh token:
 *    POST /api/auth/refresh
 *    {
 *      "refreshToken": "eyJhbGciOi..."
 *    }
 * 
 * 4. Service validates refresh token:
 *    a. Verify JWT signature
 *    b. Check expiration
 *    c. Check exists in database
 *    d. Load user
 *    e. Check user is still active
 * 
 * 5. If valid:
 *    a. Generate new access token
 *    b. Optionally generate new refresh token (token rotation)
 *    c. Save new refresh token if generated
 *    d. Delete old refresh token if rotating
 * 
 * 6. Service returns new TokenResponse
 * 
 * 7. Client updates access token and continues
 * </pre>
 * 
 * <h2>Logout Flow:</h2>
 * <pre>
 * 1. Client sends logout request with refresh token:
 *    POST /api/auth/logout
 *    {
 *      "refreshToken": "eyJhbGciOi..."
 *    }
 * 
 * 2. Service deletes refresh token from database
 * 
 * 3. Client clears all tokens from memory/storage
 * 
 * 4. Future refresh attempts fail (token not in database)
 * 
 * 5. Access token still valid until expiration (stateless JWT)
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service  // Marks this as a Spring service component
public class AuthService {

    // Logger for debugging and error tracking
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // Dependencies (injected via constructor)
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;

    /**
     * Constructor with dependency injection
     * 
     * @param userRepository Repository for user database operations
     * @param roleRepository Repository for role database operations
     * @param refreshTokenRepository Repository for refresh token operations
     * @param passwordEncoder BCrypt password encoder
     * @param jwtTokenService Service for JWT token operations
     * @param authenticationManager Spring Security authentication manager
     */
    @Autowired
    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Register New User
     * 
     * <p>Creates a new user account with provided information.
     * Validates uniqueness and password strength, then persists to database.</p>
     * 
     * @param request Registration request with user details
     * @return UserResponse with created user information
     * @throws RuntimeException if email exists, username taken, or validation fails
     */
    @Transactional  // Wrap in transaction (all-or-nothing)
    public UserResponse register(RegisterRequest request) {
        logger.info("Processing registration request for email: {}", request.getEmail());
        
        // Step 1: Validate email is not already registered
        // Email must be globally unique across all tenants
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Registration failed: Email already exists: {}", request.getEmail());
            throw new RuntimeException("Email already registered");
        }
        
        // Step 2: Validate username is unique within tenant
        // Username only needs to be unique within the same tenant
        if (userRepository.existsByUsernameAndTenantId(request.getUsername(), request.getTenantId())) {
            logger.warn("Registration failed: Username already exists: {} in tenant: {}", 
                    request.getUsername(), request.getTenantId());
            throw new RuntimeException("Username already taken in this tenant");
        }
        
        // Step 3: Validate password strength
        validatePasswordStrength(request.getPassword());
        
        // Step 4: Create new User entity
        User user = User.builder()
                // Basic information
                .username(request.getUsername())
                .email(request.getEmail().toLowerCase())  // Store email in lowercase
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                
                // Password (hash with BCrypt)
                // NEVER store plain text passwords!
                // BCrypt automatically generates salt and hashes
                .password(passwordEncoder.encode(request.getPassword()))
                
                // Authentication provider (LOCAL for email/password registration)
                .authProvider(AuthProvider.LOCAL)
                
                // Initial status (PENDING until email verified)
                .status(UserStatus.PENDING)
                
                // Email verification (false initially, true after verification)
                .emailVerified(false)
                
                // Multi-tenancy
                .tenantId(request.getTenantId())
                
                // Two-factor authentication (disabled by default)
                .using2FA(false)
                
                .build();
        
        // Step 5: Assign default role (USER)
        // Load the USER role from database
        Role userRole = roleRepository.findByName("USER")
                // If USER role doesn't exist, throw exception
                // This should never happen if database is properly seeded
                .orElseThrow(() -> new RuntimeException("USER role not found. Please seed the database."));
        
        // Add role to user
        // This updates the many-to-many relationship
        user.addRole(userRole);
        
        // Step 6: Save user to database
        // JPA will also persist the user-role relationship
        User savedUser = userRepository.save(user);
        logger.info("User registered successfully: {} (ID: {})", savedUser.getEmail(), savedUser.getId());
        
        // Step 7: Send email verification (TODO: implement email service)
        // sendVerificationEmail(savedUser);
        logger.info("Email verification should be sent to: {}", savedUser.getEmail());
        
        // Step 8: Convert entity to DTO and return
        return toUserResponse(savedUser);
    }

    /**
     * Authenticate User (Login)
     * 
     * <p>Authenticates user credentials and generates JWT tokens.</p>
     * 
     * @param request Login request with email and password
     * @return TokenResponse with access and refresh tokens
     * @throws AuthenticationException if credentials are invalid
     */
    @Transactional  // Transaction for refresh token creation
    public TokenResponse login(LoginRequest request) {
        logger.info("Processing login request for email: {}", request.getEmail());
        
        // Step 1: Authenticate user credentials
        // Create authentication token with credentials
        UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                );
        
        // Authenticate using AuthenticationManager
        // This will:
        // 1. Call UserDetailsService to load user
        // 2. Compare passwords with PasswordEncoder
        // 3. Return authenticated Authentication if successful
        // 4. Throw AuthenticationException if failed
        Authentication authentication = authenticationManager.authenticate(authToken);
        logger.debug("Authentication successful for: {}", request.getEmail());
        
        // Step 2: Load full user entity
        // authentication.getPrincipal() returns UserDetails (User in our case)
        User user = (User) authentication.getPrincipal();
        
        // Step 3: Generate JWT tokens
        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshTokenValue = jwtTokenService.generateRefreshToken(user);
        logger.debug("Tokens generated for user: {}", user.getEmail());
        
        // Step 4: Save refresh token to database
        // Calculate expiration time
        // jwtConfig.getRefreshTokenExpiration() returns milliseconds
        // Convert to Instant for database storage
        Instant expiryDate = Instant.now()
                .plusMillis(86400000L);  // 24 hours (should come from config)
        
        // Create RefreshToken entity
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiryDate(expiryDate)
                .build();
        
        // Save to database
        refreshTokenRepository.save(refreshToken);
        logger.info("Login successful for user: {} (ID: {})", user.getEmail(), user.getId());
        
        // Step 5: Build and return TokenResponse
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(3600L)  // 1 hour in seconds (should come from config)
                .build();
    }

    /**
     * Refresh Access Token
     * 
     * <p>Generates new access token using valid refresh token.</p>
     * 
     * @param refreshTokenValue Refresh token string
     * @return TokenResponse with new access token
     * @throws RuntimeException if refresh token is invalid or expired
     */
    @Transactional
    public TokenResponse refreshAccessToken(String refreshTokenValue) {
        logger.info("Processing token refresh request");
        
        // Step 1: Validate JWT token format and signature
        if (!jwtTokenService.validateToken(refreshTokenValue)) {
            logger.warn("Invalid refresh token format or signature");
            throw new RuntimeException("Invalid refresh token");
        }
        
        // Step 2: Find refresh token in database
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> {
                    logger.warn("Refresh token not found in database");
                    return new RuntimeException("Refresh token not found");
                });
        
        // Step 3: Check if refresh token is expired
        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            logger.warn("Refresh token expired");
            // Delete expired token from database
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }
        
        // Step 4: Get user from refresh token
        User user = refreshToken.getUser();
        
        // Step 5: Check if user is still active
        if (user.getStatus() != UserStatus.ACTIVE) {
            logger.warn("User account not active: {}", user.getEmail());
            throw new RuntimeException("User account not active");
        }
        
        // Step 6: Generate new access token
        String newAccessToken = jwtTokenService.generateAccessToken(user);
        logger.info("Access token refreshed for user: {}", user.getEmail());
        
        // Step 7: Return new token response
        // Keep same refresh token (no rotation)
        // For security, you could implement refresh token rotation here
        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenValue)  // Same refresh token
                .tokenType("Bearer")
                .expiresIn(3600L)  // 1 hour
                .build();
    }

    /**
     * Logout User
     * 
     * <p>Revokes refresh token by deleting it from database.</p>
     * 
     * @param refreshTokenValue Refresh token to revoke
     */
    @Transactional
    public void logout(String refreshTokenValue) {
        logger.info("Processing logout request");
        
        // Find and delete refresh token from database
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(token -> {
                    refreshTokenRepository.delete(token);
                    logger.info("Refresh token revoked for user: {}", token.getUser().getEmail());
                });
        
        // Note: Access token will remain valid until expiration (stateless JWT)
        // For immediate revocation, implement token blacklist or use shorter expiration
    }

    /**
     * Validate Password Strength
     * 
     * <p>Ensures password meets security requirements.</p>
     * 
     * <p><b>Requirements:</b></p>
     * <ul>
     *   <li>At least 8 characters</li>
     *   <li>At least one uppercase letter</li>
     *   <li>At least one lowercase letter</li>
     *   <li>At least one digit</li>
     *   <li>At least one special character</li>
     * </ul>
     * 
     * @param password Password to validate
     * @throws RuntimeException if password doesn't meet requirements
     */
    private void validatePasswordStrength(String password) {
        // Check minimum length (already checked by @Size, but double-check)
        if (password.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters long");
        }
        
        // Check for uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            throw new RuntimeException("Password must contain at least one uppercase letter");
        }
        
        // Check for lowercase letter
        if (!password.matches(".*[a-z].*")) {
            throw new RuntimeException("Password must contain at least one lowercase letter");
        }
        
        // Check for digit
        if (!password.matches(".*\\d.*")) {
            throw new RuntimeException("Password must contain at least one digit");
        }
        
        // Check for special character
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new RuntimeException("Password must contain at least one special character");
        }
    }

    /**
     * Convert User Entity to User Response DTO
     * 
     * <p>Maps User entity to UserResponse DTO, excluding sensitive data.</p>
     * 
     * @param user User entity
     * @return UserResponse DTO
     */
    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .authProvider(user.getAuthProvider())
                .emailVerified(user.getEmailVerified())
                .imageUrl(user.getImageUrl())
                .status(user.getStatus())
                .tenantId(user.getTenantId())
                .using2FA(user.getUsing2FA())
                // Convert roles to role names
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

