package com.ecommerce.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login Request DTO
 * 
 * <p>Data Transfer Object for user login/authentication requests.
 * Used for traditional email/password authentication.</p>
 * 
 * <h2>Authentication Flow:</h2>
 * <pre>
 * 1. User submits LoginRequest (email + password)
 * 
 * 2. Controller receives request:
 *    - Validates @Valid constraints
 *    - If invalid, returns 400 Bad Request
 * 
 * 3. Controller passes to AuthService.login():
 *    - Service creates UsernamePasswordAuthenticationToken
 *    - Passes to AuthenticationManager
 * 
 * 4. AuthenticationManager authenticates:
 *    - DaoAuthenticationProvider loads user from database
 *    - Compares password with BCrypt
 *    - If match, authentication successful
 *    - If no match, throws BadCredentialsException
 * 
 * 5. If authentication successful:
 *    - Generate JWT access token
 *    - Generate refresh token
 *    - Save refresh token to database
 *    - Return TokenResponse
 * 
 * 6. If authentication fails:
 *    - Return 401 Unauthorized
 *    - Generic error message (security)
 * </pre>
 * 
 * <h2>Example Request:</h2>
 * <pre>
 * POST /api/auth/login
 * Content-Type: application/json
 * 
 * {
 *   "email": "john.doe@example.com",
 *   "password": "SecurePass123!"
 * }
 * </pre>
 * 
 * <h2>Success Response:</h2>
 * <pre>
 * HTTP 200 OK
 * {
 *   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "tokenType": "Bearer",
 *   "expiresIn": 3600,
 *   "user": {
 *     "id": 123,
 *     "username": "johndoe",
 *     "email": "john.doe@example.com",
 *     "firstName": "John",
 *     "lastName": "Doe",
 *     "roles": ["ROLE_USER"]
 *   }
 * }
 * </pre>
 * 
 * <h2>Error Responses:</h2>
 * <pre>
 * Invalid Credentials (401):
 * {
 *   "timestamp": "2024-01-01T10:00:00",
 *   "status": 401,
 *   "message": "Invalid email or password",
 *   "path": "/api/auth/login"
 * }
 * 
 * Account Not Activated (403):
 * {
 *   "timestamp": "2024-01-01T10:00:00",
 *   "status": 403,
 *   "message": "Please verify your email address to activate your account",
 *   "path": "/api/auth/login"
 * }
 * 
 * Account Suspended (403):
 * {
 *   "timestamp": "2024-01-01T10:00:00",
 *   "status": 403,
 *   "message": "Account suspended until 2024-02-01",
 *   "path": "/api/auth/login"
 * }
 * 
 * Account Banned (403):
 * {
 *   "timestamp": "2024-01-01T10:00:00",
 *   "status": 403,
 *   "message": "Account permanently banned. Contact support.",
 *   "path": "/api/auth/login"
 * }
 * </pre>
 * 
 * <h2>Security Considerations:</h2>
 * <ul>
 *   <li><b>Rate Limiting:</b> Limit login attempts (5 per minute per IP)</li>
 *   <li><b>Account Lockout:</b> Lock after 5 failed attempts (15 minutes)</li>
 *   <li><b>Generic Errors:</b> Don't reveal if email exists (timing attacks)</li>
 *   <li><b>Password:</b> Never log password, even in errors</li>
 *   <li><b>HTTPS:</b> Always use HTTPS to protect credentials in transit</li>
 *   <li><b>Timing:</b> Use constant-time comparison to prevent timing attacks</li>
 * </ul>
 * 
 * <h2>Alternative Login Methods:</h2>
 * <pre>
 * OAuth2 (Social Login):
 * - No password required
 * - Redirect to OAuth provider
 * - Provider handles authentication
 * - Return JWT on success
 * 
 * Two-Factor Authentication (2FA):
 * - First: email + password (this DTO)
 * - If 2FA enabled, return status: "2FA_REQUIRED"
 * - Second request: email + TOTP code
 * - Return JWT on successful 2FA
 * 
 * API Key:
 * - Different endpoint: /api/auth/api-key
 * - Header: X-API-Key: <key>
 * - Used for service-to-service auth
 * - No user session, stateless
 * </pre>
 * 
 * <h2>Token Usage:</h2>
 * <pre>
 * Access Token (for API requests):
 * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 * 
 * Example API request:
 * GET /api/users/me
 * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 * 
 * When access token expires:
 * POST /api/auth/refresh
 * {
 *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
 * }
 * 
 * Response: New access token
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data  // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: generates no-argument constructor
@AllArgsConstructor  // Lombok: generates all-argument constructor
public class LoginRequest {

    /**
     * User's email address
     * 
     * <p><b>Validation:</b></p>
     * <ul>
     *   <li>Required: Cannot be blank</li>
     *   <li>Format: Valid email format</li>
     *   <li>Case-insensitive: Converted to lowercase</li>
     * </ul>
     * 
     * <p><b>Lookup:</b></p>
     * <pre>
     * // Service layer
     * User user = userRepository.findByEmail(email.toLowerCase())
     *     .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
     * </pre>
     */
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email should be valid")
    private String email;

    /**
     * User's password
     * 
     * <p><b>Validation:</b></p>
     * <ul>
     *   <li>Required: Cannot be blank</li>
     *   <li>No length check (any length accepted for login)</li>
     * </ul>
     * 
     * <p><b>Security:</b></p>
     * <ul>
     *   <li>Never logged (even in debug mode)</li>
     *   <li>Compared using BCrypt.matches()</li>
     *   <li>Constant-time comparison (timing attack prevention)</li>
     *   <li>Cleared from memory after authentication</li>
     * </ul>
     * 
     * <p><b>Authentication:</b></p>
     * <pre>
     * // Service layer
     * boolean isValid = passwordEncoder.matches(
     *     plainPassword,        // From login request
     *     user.getPassword()   // Hashed password from database
     * );
     * 
     * if (!isValid) {
     *     throw new BadCredentialsException("Invalid credentials");
     * }
     * </pre>
     */
    @NotBlank(message = "Password cannot be empty")
    private String password;

    /**
     * Custom toString to exclude password
     * 
     * <p>Override Lombok's toString to prevent password from being logged</p>
     * 
     * @return String representation without password
     */
    @Override
    public String toString() {
        return "LoginRequest{" +
                "email='" + email + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}

