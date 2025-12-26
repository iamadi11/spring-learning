package com.ecommerce.auth.service;

import com.ecommerce.auth.config.JwtConfig;
import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.enums.AuthProvider;
import com.ecommerce.auth.enums.UserStatus;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit Tests for JWT Token Service
 * 
 * <p>Tests JWT token generation, validation, and parsing.
 * Uses JUnit 5 and AssertJ for assertions.</p>
 * 
 * <h2>Test Strategy:</h2>
 * <pre>
 * 1. Token Generation:
 *    - Access token contains correct claims
 *    - Refresh token is generated
 *    - Tokens have correct expiration
 * 
 * 2. Token Validation:
 *    - Valid tokens pass validation
 *    - Expired tokens fail validation
 *    - Tampered tokens fail validation
 *    - Invalid format tokens fail validation
 * 
 * 3. Token Parsing:
 *    - Extract email from token
 *    - Extract username from token
 *    - Extract user ID from token
 *    - Extract roles from token
 * 
 * 4. Token Expiration:
 *    - Check if token is expired
 *    - Handle expired token exceptions
 * </pre>
 * 
 * <h2>JUnit 5 Features Used:</h2>
 * <ul>
 *   <li>@Test: Marks test methods</li>
 *   <li>@BeforeEach: Setup before each test</li>
 *   <li>@DisplayName: Descriptive test names</li>
 *   <li>@ExtendWith: Enable Mockito extension</li>
 *   <li>AssertJ: Fluent assertions (assertThat)</li>
 * </ul>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@ExtendWith(MockitoExtension.class)  // Enable Mockito for mocking dependencies
@DisplayName("JWT Token Service Tests")  // Descriptive test suite name
class JwtTokenServiceTest {

    // Service under test
    private JwtTokenService jwtTokenService;

    // Test data
    private User testUser;
    private JwtConfig jwtConfig;

    /**
     * Setup before each test
     * 
     * <p>Creates test user and initializes JWT service with test configuration.</p>
     */
    @BeforeEach
    void setUp() {
        // Create JWT configuration for testing
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret("testSecretKeyForJwtTokenServiceThatIsLongEnoughForHS256Algorithm");  // 256-bit secret
        jwtConfig.setAccessTokenExpiration(3600000L);  // 1 hour
        jwtConfig.setRefreshTokenExpiration(86400000L);  // 24 hours
        jwtConfig.setIssuer("ecommerce-auth-service-test");
        
        // Initialize JWT token service with test config
        jwtTokenService = new JwtTokenService(jwtConfig);
        
        // Create test user with roles
        Role userRole = Role.builder()
                .id("role-uuid-1")
                .name("USER")
                .build();
        
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .password("hashedPassword")
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .tenantId("default")
                .using2FA(false)
                .roles(roles)
                .build();
    }

    /**
     * Test: Generate Access Token
     * 
     * <p>Verifies that access token is generated with correct format and claims.</p>
     */
    @Test
    @DisplayName("Should generate valid access token")
    void shouldGenerateAccessToken() {
        // When: Generate access token
        String accessToken = jwtTokenService.generateAccessToken(testUser);
        
        // Then: Token should not be null or empty
        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotEmpty();
        
        // Token should have 3 parts separated by dots (header.payload.signature)
        assertThat(accessToken.split("\\.")).hasSize(3);
    }

    /**
     * Test: Generate Refresh Token
     * 
     * <p>Verifies that refresh token is generated with correct format.</p>
     */
    @Test
    @DisplayName("Should generate valid refresh token")
    void shouldGenerateRefreshToken() {
        // When: Generate refresh token
        String refreshToken = jwtTokenService.generateRefreshToken(testUser);
        
        // Then: Token should not be null or empty
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        
        // Token should have 3 parts
        assertThat(refreshToken.split("\\.")).hasSize(3);
    }

    /**
     * Test: Validate Valid Token
     * 
     * <p>Verifies that valid tokens pass validation.</p>
     */
    @Test
    @DisplayName("Should validate valid token")
    void shouldValidateValidToken() {
        // Given: Generate a valid token
        String token = jwtTokenService.generateAccessToken(testUser);
        
        // When: Validate token
        boolean isValid = jwtTokenService.validateToken(token);
        
        // Then: Token should be valid
        assertThat(isValid).isTrue();
    }

    /**
     * Test: Reject Invalid Token Format
     * 
     * <p>Verifies that tokens with invalid format are rejected.</p>
     */
    @Test
    @DisplayName("Should reject invalid token format")
    void shouldRejectInvalidTokenFormat() {
        // Given: Invalid token (not JWT format)
        String invalidToken = "not.a.valid.jwt.token";
        
        // When/Then: Validation should throw exception
        assertThatThrownBy(() -> jwtTokenService.validateToken(invalidToken))
                .isInstanceOf(JwtException.class);
    }

    /**
     * Test: Reject Tampered Token
     * 
     * <p>Verifies that tokens with modified signature are rejected.</p>
     */
    @Test
    @DisplayName("Should reject tampered token")
    void shouldRejectTamperedToken() {
        // Given: Generate valid token and tamper with it
        String validToken = jwtTokenService.generateAccessToken(testUser);
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";
        
        // When/Then: Validation should throw exception
        assertThatThrownBy(() -> jwtTokenService.validateToken(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    /**
     * Test: Extract Email from Token
     * 
     * <p>Verifies that user email can be extracted from token.</p>
     */
    @Test
    @DisplayName("Should extract email from token")
    void shouldExtractEmailFromToken() {
        // Given: Generate token
        String token = jwtTokenService.generateAccessToken(testUser);
        
        // When: Extract email
        String email = jwtTokenService.getEmailFromToken(token);
        
        // Then: Email should match user's email
        assertThat(email).isEqualTo(testUser.getEmail());
    }

    /**
     * Test: Extract Username from Token
     * 
     * <p>Verifies that username can be extracted from token claims.</p>
     */
    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsernameFromToken() {
        // Given: Generate token
        String token = jwtTokenService.generateAccessToken(testUser);
        
        // When: Extract username
        String username = jwtTokenService.getUsernameFromToken(token);
        
        // Then: Username should match user's username
        assertThat(username).isEqualTo(testUser.getUsername());
    }

    /**
     * Test: Extract User ID from Token
     * 
     * <p>Verifies that user ID can be extracted from token claims.</p>
     */
    @Test
    @DisplayName("Should extract user ID from token")
    void shouldExtractUserIdFromToken() {
        // Given: Generate token
        String token = jwtTokenService.generateAccessToken(testUser);
        
        // When: Extract user ID
        Long userId = jwtTokenService.getUserIdFromToken(token);
        
        // Then: User ID should match user's ID
        assertThat(userId).isEqualTo(testUser.getId());
    }

    /**
     * Test: Token Not Expired Initially
     * 
     * <p>Verifies that newly generated token is not expired.</p>
     */
    @Test
    @DisplayName("Should not be expired initially")
    void shouldNotBeExpiredInitially() {
        // Given: Generate token
        String token = jwtTokenService.generateAccessToken(testUser);
        
        // When: Check expiration
        boolean isExpired = jwtTokenService.isTokenExpired(token);
        
        // Then: Token should not be expired
        assertThat(isExpired).isFalse();
    }
}

