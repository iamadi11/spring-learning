package com.ecommerce.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Auth Service Application - Comprehensive Authentication & Authorization Service
 * 
 * <p>This service is the central authentication and authorization provider for the entire
 * e-commerce platform. It implements industry-standard security protocols and modern
 * authentication mechanisms.</p>
 * 
 * <h2>Core Responsibilities:</h2>
 * <ul>
 *   <li><b>User Authentication:</b> Verify user identity (username/password, social login)</li>
 *   <li><b>Token Management:</b> Issue, validate, refresh, and revoke tokens</li>
 *   <li><b>Authorization:</b> Control access to resources based on roles and permissions</li>
 *   <li><b>OAuth2 Server:</b> Act as authorization server for OAuth2 flows</li>
 *   <li><b>Multi-Tenancy:</b> Support multiple tenants with data isolation</li>
 *   <li><b>Two-Factor Authentication:</b> Enhanced security with TOTP/SMS/Email</li>
 *   <li><b>API Key Management:</b> Generate and validate API keys for service-to-service auth</li>
 * </ul>
 * 
 * <h2>Authentication Methods Supported:</h2>
 * <pre>
 * 1. Username/Password Authentication:
 *    - POST /api/auth/login
 *    - Returns JWT access token and refresh token
 *    - Password hashed with BCrypt (strength 12)
 * 
 * 2. OAuth2 Social Login:
 *    - Google: GET /oauth2/authorization/google
 *    - GitHub: GET /oauth2/authorization/github
 *    - Facebook: GET /oauth2/authorization/facebook
 *    - Returns JWT tokens after successful OAuth2 flow
 * 
 * 3. OAuth2 Authorization Code Flow:
 *    - Client redirects user to: GET /oauth2/authorize
 *    - User authenticates and grants consent
 *    - Server returns authorization code
 *    - Client exchanges code for tokens: POST /oauth2/token
 * 
 * 4. OAuth2 Client Credentials Flow:
 *    - For service-to-service authentication
 *    - POST /oauth2/token with client_id and client_secret
 *    - Returns access token
 * 
 * 5. API Key Authentication:
 *    - Generate: POST /api/auth/api-keys
 *    - Use: X-API-Key header in requests
 *    - For long-lived service authentication
 * 
 * 6. Refresh Token Flow:
 *    - POST /api/auth/refresh
 *    - Exchange refresh token for new access token
 *    - Extends user session without re-authentication
 * </pre>
 * 
 * <h2>JWT Token Structure:</h2>
 * <pre>
 * Access Token (short-lived: 15-60 minutes):
 * Header: {"alg": "RS256", "typ": "JWT"}
 * Payload: {
 *   "sub": "user-id",
 *   "email": "user@example.com",
 *   "roles": ["USER", "ADMIN"],
 *   "permissions": ["PRODUCT_CREATE", "ORDER_VIEW"],
 *   "tenant_id": "tenant-123",
 *   "iat": 1234567890,
 *   "exp": 1234571490,
 *   "jti": "unique-token-id"
 * }
 * Signature: RS256(header + payload, private_key)
 * 
 * Refresh Token (long-lived: 7-30 days):
 * Similar structure but with longer expiry
 * Used only to obtain new access tokens
 * </pre>
 * 
 * <h2>Authorization Model (RBAC):</h2>
 * <pre>
 * User → Role → Permission
 * 
 * Example:
 * - User: john@example.com
 * - Role: PRODUCT_MANAGER
 * - Permissions:
 *   - PRODUCT_CREATE
 *   - PRODUCT_UPDATE
 *   - PRODUCT_DELETE
 *   - INVENTORY_MANAGE
 * 
 * Method-level security:
 * @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
 * public Product createProduct(ProductRequest request) { ... }
 * </pre>
 * 
 * <h2>Multi-Tenancy Implementation:</h2>
 * <pre>
 * Each tenant has isolated data:
 * - Users belong to a tenant
 * - Tenant ID embedded in JWT token
 * - Database queries filtered by tenant_id
 * - Cross-tenant access prevented at database level
 * 
 * Example:
 * Tenant 1 (tenant-123): Company A users and data
 * Tenant 2 (tenant-456): Company B users and data
 * 
 * User from Tenant 1 cannot access Tenant 2 data
 * </pre>
 * 
 * <h2>Two-Factor Authentication (2FA):</h2>
 * <pre>
 * Setup Flow:
 * 1. User enables 2FA: POST /api/auth/enable-2fa
 * 2. Server generates secret key
 * 3. Server returns QR code
 * 4. User scans QR code with Google Authenticator
 * 5. User enters 6-digit code to verify: POST /api/auth/verify-2fa
 * 6. 2FA enabled for user
 * 
 * Login with 2FA:
 * 1. User enters username/password
 * 2. Server returns temporary token (not full access)
 * 3. User enters 2FA code
 * 4. Server validates code and returns full access token
 * 
 * TOTP Algorithm:
 * - Time-based One-Time Password
 * - 30-second validity window
 * - HMAC-SHA1 algorithm
 * - 6-digit code
 * </pre>
 * 
 * <h2>Security Best Practices Implemented:</h2>
 * <ul>
 *   <li><b>Password Hashing:</b> BCrypt with strength 12 (2^12 iterations)</li>
 *   <li><b>JWT Signing:</b> RS256 (RSA with SHA-256) for asymmetric signing</li>
 *   <li><b>Token Expiry:</b> Short-lived access tokens, longer refresh tokens</li>
 *   <li><b>Token Rotation:</b> Refresh tokens rotated on use (security best practice)</li>
 *   <li><b>CORS:</b> Configured to allow specific origins only</li>
 *   <li><b>CSRF Protection:</b> For state-changing operations</li>
 *   <li><b>Rate Limiting:</b> Prevent brute force attacks (5 attempts per minute)</li>
 *   <li><b>Input Validation:</b> All inputs validated and sanitized</li>
 *   <li><b>SQL Injection Prevention:</b> Parameterized queries via JPA</li>
 *   <li><b>Secure Headers:</b> X-Frame-Options, X-Content-Type-Options, etc.</li>
 * </ul>
 * 
 * <h2>Database Schema (PostgreSQL):</h2>
 * <pre>
 * Tables:
 * - users: User accounts
 * - roles: User roles (ADMIN, USER, PRODUCT_MANAGER, etc.)
 * - permissions: Fine-grained permissions
 * - user_roles: Many-to-many mapping
 * - role_permissions: Many-to-many mapping
 * - oauth2_registered_client: OAuth2 client applications
 * - oauth2_authorization: OAuth2 authorizations and tokens
 * - api_keys: API keys for service authentication
 * - tenants: Tenant information
 * - two_factor_auth: 2FA configuration per user
 * </pre>
 * 
 * <h2>API Endpoints:</h2>
 * <pre>
 * Authentication:
 * - POST /api/auth/register - Register new user
 * - POST /api/auth/login - Login with username/password
 * - POST /api/auth/refresh - Refresh access token
 * - POST /api/auth/logout - Logout and revoke tokens
 * - GET /api/auth/me - Get current user info
 * 
 * OAuth2:
 * - GET /oauth2/authorization/{provider} - Initiate OAuth2 flow
 * - GET /oauth2/authorize - OAuth2 authorization endpoint
 * - POST /oauth2/token - Token endpoint
 * - POST /oauth2/revoke - Revoke token
 * - GET /oauth2/userinfo - Get user info
 * 
 * Password Management:
 * - POST /api/auth/forgot-password - Request password reset
 * - POST /api/auth/reset-password - Reset password with token
 * - POST /api/auth/change-password - Change password (authenticated)
 * 
 * Email Verification:
 * - POST /api/auth/verify-email - Verify email with token
 * - POST /api/auth/resend-verification - Resend verification email
 * 
 * Two-Factor Authentication:
 * - POST /api/auth/enable-2fa - Enable 2FA (returns QR code)
 * - POST /api/auth/verify-2fa - Verify 2FA code
 * - POST /api/auth/disable-2fa - Disable 2FA
 * 
 * API Keys:
 * - POST /api/auth/api-keys - Generate new API key
 * - GET /api/auth/api-keys - List user's API keys
 * - DELETE /api/auth/api-keys/{id} - Revoke API key
 * 
 * Multi-Tenancy:
 * - POST /api/tenants - Create tenant (Super Admin)
 * - GET /api/tenants/{id} - Get tenant info
 * - PUT /api/tenants/{id} - Update tenant
 * </pre>
 * 
 * <h2>Configuration:</h2>
 * <ul>
 *   <li><b>Port:</b> 9001</li>
 *   <li><b>Database:</b> PostgreSQL (auth_db)</li>
 *   <li><b>Cache:</b> Redis (for session and rate limiting)</li>
 *   <li><b>Eureka:</b> Registered as 'auth-service'</li>
 * </ul>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication  // Enables Spring Boot auto-configuration
@EnableDiscoveryClient  // Registers with Eureka Service Discovery
public class AuthServiceApplication {

    /**
     * Main method - Entry point for Auth Service
     * 
     * <p>Spring Boot startup process:</p>
     * <ol>
     *   <li>Create Spring ApplicationContext</li>
     *   <li>Scan for components (@Service, @Repository, @Controller)</li>
     *   <li>Auto-configure Spring Security with OAuth2 support</li>
     *   <li>Initialize JPA and connect to PostgreSQL (auth_db)</li>
     *   <li>Connect to Redis for session management</li>
     *   <li>Register with Eureka Server as 'auth-service'</li>
     *   <li>Start embedded Tomcat on port 9001</li>
     *   <li>Expose OAuth2 endpoints and REST APIs</li>
     * </ol>
     * 
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        // Start Spring Boot application
        SpringApplication.run(AuthServiceApplication.class, args);
        
        // Auth Service is now ready to:
        // - Authenticate users
        // - Issue JWT tokens
        // - Handle OAuth2 flows
        // - Manage API keys
        // - Validate 2FA codes
    }
}

