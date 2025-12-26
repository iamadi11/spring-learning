package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.TokenResponse;
import com.ecommerce.auth.dto.UserResponse;
import com.ecommerce.auth.service.AuthService;
import com.ecommerce.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * 
 * <p>REST controller for authentication and authorization operations.
 * Provides endpoints for registration, login, token refresh, and logout.</p>
 * 
 * <h2>API Endpoints:</h2>
 * <pre>
 * POST   /api/auth/register  - Register new user
 * POST   /api/auth/login     - Authenticate and get tokens
 * POST   /api/auth/refresh   - Refresh access token
 * POST   /api/auth/logout    - Logout and revoke refresh token
 * GET    /api/auth/me        - Get current user info (authenticated)
 * </pre>
 * 
 * <h2>REST API Design Principles:</h2>
 * <pre>
 * 1. Resource-Based URLs:
 *    - /api/auth/register (not /api/registerUser)
 *    - Use nouns, not verbs
 * 
 * 2. HTTP Methods:
 *    - GET: Retrieve resources (idempotent)
 *    - POST: Create resources or actions
 *    - PUT: Update entire resource
 *    - PATCH: Update partial resource
 *    - DELETE: Remove resource
 * 
 * 3. Status Codes:
 *    - 200 OK: Success (GET, PUT, PATCH)
 *    - 201 Created: Resource created (POST)
 *    - 204 No Content: Success, no response body (DELETE)
 *    - 400 Bad Request: Validation error
 *    - 401 Unauthorized: Authentication required
 *    - 403 Forbidden: Insufficient permissions
 *    - 404 Not Found: Resource doesn't exist
 *    - 409 Conflict: Duplicate resource
 *    - 500 Internal Server Error: Server error
 * 
 * 4. Consistent Response Format:
 *    Success: {
 *      "timestamp": "2024-01-01T10:00:00",
 *      "status": 200,
 *      "message": "Operation successful",
 *      "data": { ... },
 *      "path": "/api/auth/register"
 *    }
 *    
 *    Error: {
 *      "timestamp": "2024-01-01T10:00:00",
 *      "status": 400,
 *      "message": "Validation failed",
 *      "errors": { ... },
 *      "path": "/api/auth/register"
 *    }
 * 
 * 5. Validation:
 *    - Use @Valid for request validation
 *    - Use Bean Validation annotations (@NotBlank, @Email, etc.)
 *    - Return detailed error messages
 * 
 * 6. Security:
 *    - Use HTTPS in production
 *    - Validate all inputs
 *    - Rate limit sensitive endpoints
 *    - Log authentication attempts
 *    - Use generic error messages (don't reveal if email exists)
 * </pre>
 * 
 * <h2>Example API Usage:</h2>
 * <pre>
 * 1. Register:
 *    POST /api/auth/register
 *    Content-Type: application/json
 *    
 *    {
 *      "username": "johndoe",
 *      "email": "john@example.com",
 *      "password": "SecurePass123!",
 *      "firstName": "John",
 *      "lastName": "Doe",
 *      "tenantId": "tenant1"
 *    }
 *    
 *    Response 201:
 *    {
 *      "timestamp": "2024-01-01T10:00:00",
 *      "status": 201,
 *      "message": "Registration successful",
 *      "data": {
 *        "id": 123,
 *        "username": "johndoe",
 *        "email": "john@example.com",
 *        "status": "PENDING"
 *      }
 *    }
 * 
 * 2. Login:
 *    POST /api/auth/login
 *    Content-Type: application/json
 *    
 *    {
 *      "email": "john@example.com",
 *      "password": "SecurePass123!"
 *    }
 *    
 *    Response 200:
 *    {
 *      "accessToken": "eyJhbGciOi...",
 *      "refreshToken": "eyJhbGciOi...",
 *      "tokenType": "Bearer",
 *      "expiresIn": 3600
 *    }
 * 
 * 3. Use Access Token:
 *    GET /api/users/me
 *    Authorization: Bearer eyJhbGciOi...
 *    
 *    Response 200:
 *    {
 *      "id": 123,
 *      "username": "johndoe",
 *      "email": "john@example.com"
 *    }
 * 
 * 4. Refresh Token:
 *    POST /api/auth/refresh
 *    Content-Type: application/json
 *    
 *    {
 *      "refreshToken": "eyJhbGciOi..."
 *    }
 *    
 *    Response 200:
 *    {
 *      "accessToken": "eyJhbGciOi... (new)",
 *      "refreshToken": "eyJhbGciOi... (same)",
 *      "tokenType": "Bearer",
 *      "expiresIn": 3600
 *    }
 * 
 * 5. Logout:
 *    POST /api/auth/logout
 *    Content-Type: application/json
 *    
 *    {
 *      "refreshToken": "eyJhbGciOi..."
 *    }
 *    
 *    Response 200:
 *    {
 *      "message": "Logout successful"
 *    }
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController  // Marks this as a REST controller (combines @Controller + @ResponseBody)
@RequestMapping("/api/auth")  // Base path for all endpoints in this controller
@Tag(name = "Authentication", description = "Authentication and authorization operations")  // Swagger documentation
public class AuthController {

    // Logger for debugging and auditing
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    // Authentication service (business logic)
    private final AuthService authService;

    /**
     * Constructor with dependency injection
     * 
     * @param authService Authentication service
     */
    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register New User
     * 
     * <p>Creates a new user account with provided information.</p>
     * 
     * <p><b>Endpoint:</b> POST /api/auth/register</p>
     * <p><b>Access:</b> Public (no authentication required)</p>
     * <p><b>Rate Limit:</b> 5 requests per hour per IP (should be implemented)</p>
     * 
     * @param request Registration request with user details
     * @param httpRequest HTTP servlet request for extracting path
     * @return ResponseEntity with ApiResponse containing UserResponse
     */
    @PostMapping("/register")  // Maps to POST /api/auth/register
    @Operation(summary = "Register new user", description = "Creates a new user account")  // Swagger doc
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request,  // @Valid triggers validation, @RequestBody parses JSON
            HttpServletRequest httpRequest) {
        
        // Log registration attempt (for debugging and security monitoring)
        logger.info("Registration attempt for email: {}", request.getEmail());
        
        // Call service to register user
        // Service handles validation and business logic
        UserResponse userResponse = authService.register(request);
        
        // Log successful registration
        logger.info("User registered successfully: {} (ID: {})", 
                userResponse.getEmail(), userResponse.getId());
        
        // Build API response with success message
        ApiResponse<UserResponse> response = ApiResponse.success(
                userResponse,  // User data
                "Registration successful. Please check your email to verify your account.",  // Message
                httpRequest.getRequestURI()  // Request path
        );
        
        // Return ResponseEntity with 201 Created status
        // 201 indicates a new resource was created
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Login User
     * 
     * <p>Authenticates user credentials and generates JWT tokens.</p>
     * 
     * <p><b>Endpoint:</b> POST /api/auth/login</p>
     * <p><b>Access:</b> Public (no authentication required)</p>
     * <p><b>Rate Limit:</b> 5 requests per minute per IP (should be implemented)</p>
     * 
     * @param request Login request with email and password
     * @return ResponseEntity with TokenResponse
     */
    @PostMapping("/login")  // Maps to POST /api/auth/login
    @Operation(summary = "Login user", description = "Authenticates user and returns JWT tokens")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request) {  // @Valid validates request, @RequestBody parses JSON
        
        // Log login attempt (for security monitoring)
        // Don't log password!
        logger.info("Login attempt for email: {}", request.getEmail());
        
        // Call service to authenticate user
        // Service will throw AuthenticationException if credentials invalid
        TokenResponse tokenResponse = authService.login(request);
        
        // Log successful login
        logger.info("Login successful for email: {}", request.getEmail());
        
        // Return ResponseEntity with 200 OK status
        // Return TokenResponse directly (no ApiResponse wrapper for tokens)
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * Refresh Access Token
     * 
     * <p>Generates new access token using valid refresh token.</p>
     * 
     * <p><b>Endpoint:</b> POST /api/auth/refresh</p>
     * <p><b>Access:</b> Public (validated via refresh token)</p>
     * 
     * @param refreshTokenRequest Request body containing refresh token
     * @return ResponseEntity with new TokenResponse
     */
    @PostMapping("/refresh")  // Maps to POST /api/auth/refresh
    @Operation(summary = "Refresh access token", description = "Issues new access token using refresh token")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestBody RefreshTokenRequest refreshTokenRequest) {
        
        // Log token refresh attempt
        logger.info("Token refresh attempt");
        
        // Call service to refresh access token
        // Service validates refresh token and generates new access token
        TokenResponse tokenResponse = authService.refreshAccessToken(refreshTokenRequest.getRefreshToken());
        
        // Log successful refresh
        logger.info("Token refreshed successfully");
        
        // Return ResponseEntity with 200 OK status
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * Logout User
     * 
     * <p>Revokes refresh token by deleting it from database.</p>
     * 
     * <p><b>Endpoint:</b> POST /api/auth/logout</p>
     * <p><b>Access:</b> Public (validated via refresh token)</p>
     * 
     * @param refreshTokenRequest Request body containing refresh token
     * @param httpRequest HTTP servlet request for extracting path
     * @return ResponseEntity with success message
     */
    @PostMapping("/logout")  // Maps to POST /api/auth/logout
    @Operation(summary = "Logout user", description = "Revokes refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest httpRequest) {
        
        // Log logout attempt
        logger.info("Logout attempt");
        
        // Call service to logout (delete refresh token)
        authService.logout(refreshTokenRequest.getRefreshToken());
        
        // Log successful logout
        logger.info("Logout successful");
        
        // Build API response with success message
        ApiResponse<Void> response = ApiResponse.success(
                null,  // No data to return
                "Logout successful",  // Message
                httpRequest.getRequestURI()  // Request path
        );
        
        // Return ResponseEntity with 200 OK status
        return ResponseEntity.ok(response);
    }

    /**
     * Inner DTO class for refresh token requests
     * 
     * <p>Simple DTO to wrap refresh token string in request body.</p>
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RefreshTokenRequest {
        /**
         * Refresh token string from previous login/refresh
         */
        @jakarta.validation.constraints.NotBlank(message = "Refresh token cannot be empty")
        private String refreshToken;
    }
}

