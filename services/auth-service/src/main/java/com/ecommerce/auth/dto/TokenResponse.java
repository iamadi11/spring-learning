package com.ecommerce.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token Response DTO
 * 
 * <p>Response object returned after successful authentication (login or token refresh).
 * Contains JWT tokens and metadata needed by the client.</p>
 * 
 * <h2>Response Structure:</h2>
 * <pre>
 * {
 *   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
 *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNTE2MjM5MDIyfQ.different_signature_here",
 *   "tokenType": "Bearer",
 *   "expiresIn": 3600
 * }
 * </pre>
 * 
 * <h2>Token Usage:</h2>
 * <pre>
 * Client stores tokens securely:
 * - Access token: In memory (not localStorage - XSS risk)
 * - Refresh token: HttpOnly cookie (CSRF protection) or secure storage
 * 
 * Making authenticated requests:
 * fetch('/api/users/me', {
 *   headers: {
 *     'Authorization': `Bearer ${accessToken}`,
 *     'Content-Type': 'application/json'
 *   }
 * });
 * 
 * When access token expires (401 response):
 * 1. Use refresh token to get new access token
 * 2. Retry original request with new access token
 * 3. If refresh fails (403), redirect to login
 * </pre>
 * 
 * <h2>Token Refresh Flow:</h2>
 * <pre>
 * 1. Client detects expired access token (401 or exp claim):
 *    GET /api/users/me
 *    Response: 401 Unauthorized
 * 
 * 2. Client requests new access token:
 *    POST /api/auth/refresh
 *    {
 *      "refreshToken": "eyJhbGciOi..."
 *    }
 * 
 * 3. Server validates refresh token:
 *    - Check signature
 *    - Check expiration
 *    - Check if exists in database
 *    - Check if user is still active
 * 
 * 4. If valid, generate new access token:
 *    Response: {
 *      "accessToken": "eyJhbGciOi... (new token)",
 *      "refreshToken": "eyJhbGciOi... (same or new)",
 *      "tokenType": "Bearer",
 *      "expiresIn": 3600
 *    }
 * 
 * 5. Client updates access token and retries original request
 * </pre>
 * 
 * <h2>Security Best Practices:</h2>
 * <ul>
 *   <li><b>Access Token Storage:</b> Memory (SPA) or HttpOnly cookie (server-rendered)</li>
 *   <li><b>Refresh Token Storage:</b> HttpOnly cookie with SameSite=Strict</li>
 *   <li><b>Token Rotation:</b> Issue new refresh token on each refresh (optional)</li>
 *   <li><b>Revocation:</b> Delete refresh token from database on logout</li>
 *   <li><b>HTTPS Only:</b> Never transmit tokens over HTTP</li>
 *   <li><b>Expiration:</b> Short access token (1h), longer refresh (24h)</li>
 * </ul>
 * 
 * <h2>Token Anatomy:</h2>
 * <pre>
 * Access Token (JWT):
 * Header: { "alg": "HS256", "typ": "JWT" }
 * Payload: {
 *   "sub": "user@example.com",           // Subject (user identifier)
 *   "iss": "ecommerce-auth-service",     // Issuer
 *   "iat": 1634567890,                   // Issued At
 *   "exp": 1634571490,                   // Expiration (1 hour later)
 *   "roles": ["ROLE_USER"],              // User roles
 *   "tenantId": "tenant1",               // Tenant identifier
 *   "username": "johndoe"                // Username
 * }
 * Signature: HMACSHA256(base64(header) + "." + base64(payload), secret)
 * 
 * Refresh Token (JWT):
 * Similar structure but:
 * - Longer expiration (24 hours)
 * - Different signature
 * - Stored in database for revocation
 * - Only used for /auth/refresh endpoint
 * </pre>
 * 
 * <h2>Client-Side Token Management:</h2>
 * <pre>
 * // Token service (React/TypeScript example)
 * class TokenService {
 *   private accessToken: string | null = null;
 *   
 *   setTokens(tokenResponse: TokenResponse) {
 *     this.accessToken = tokenResponse.accessToken;
 *     // Store refresh token in HttpOnly cookie (handled by server)
 *     // Or in secure storage if mobile app
 *   }
 *   
 *   getAccessToken(): string | null {
 *     return this.accessToken;
 *   }
 *   
 *   isAccessTokenExpired(): boolean {
 *     if (!this.accessToken) return true;
 *     const payload = JSON.parse(atob(this.accessToken.split('.')[1]));
 *     return payload.exp * 1000 < Date.now();
 *   }
 *   
 *   async refreshAccessToken(): Promise<void> {
 *     const response = await fetch('/api/auth/refresh', {
 *       method: 'POST',
 *       credentials: 'include'  // Include HttpOnly cookie
 *     });
 *     const tokenResponse = await response.json();
 *     this.setTokens(tokenResponse);
 *   }
 *   
 *   clearTokens() {
 *     this.accessToken = null;
 *     // Clear refresh token cookie via logout endpoint
 *   }
 * }
 * 
 * // Axios interceptor for automatic token refresh
 * axios.interceptors.response.use(
 *   response => response,
 *   async error => {
 *     if (error.response?.status === 401) {
 *       try {
 *         await tokenService.refreshAccessToken();
 *         // Retry original request
 *         return axios(error.config);
 *       } catch (refreshError) {
 *         // Refresh failed, redirect to login
 *         window.location.href = '/login';
 *       }
 *     }
 *     return Promise.reject(error);
 *   }
 * );
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data  // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: generates no-argument constructor
@AllArgsConstructor  // Lombok: generates all-argument constructor
@Builder  // Lombok: provides builder pattern for object creation
public class TokenResponse {

    /**
     * JWT access token - short-lived token for API authentication
     * 
     * <p><b>Purpose:</b></p>
     * <ul>
     *   <li>Authenticate API requests</li>
     *   <li>Sent in Authorization header</li>
     *   <li>Contains user identity and permissions</li>
     *   <li>Self-contained (no database lookup needed)</li>
     * </ul>
     * 
     * <p><b>Lifetime:</b> 1 hour (configurable)</p>
     * 
     * <p><b>Usage:</b></p>
     * <pre>
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * </pre>
     */
    private String accessToken;

    /**
     * JWT refresh token - long-lived token for obtaining new access tokens
     * 
     * <p><b>Purpose:</b></p>
     * <ul>
     *   <li>Obtain new access token when expired</li>
     *   <li>Stored in database for revocation</li>
     *   <li>Only used with /auth/refresh endpoint</li>
     *   <li>Allows logout from server side</li>
     * </ul>
     * 
     * <p><b>Lifetime:</b> 24 hours (configurable)</p>
     * 
     * <p><b>Revocation:</b></p>
     * <pre>
     * On logout:
     * - Delete from database
     * - Future refresh attempts fail
     * - Access token still valid until expiration
     * 
     * On security breach:
     * - Delete all user's refresh tokens
     * - Force re-authentication
     * </pre>
     */
    private String refreshToken;

    /**
     * Token type - always "Bearer" for JWT
     * 
     * <p>Used in Authorization header:</p>
     * <pre>
     * Authorization: {tokenType} {accessToken}
     * Authorization: Bearer eyJhbGciOi...
     * </pre>
     * 
     * <p><b>Other token types (not used here):</b></p>
     * <ul>
     *   <li>Basic - Base64(username:password)</li>
     *   <li>Digest - MD5 hash</li>
     *   <li>HOBA - Origin-Bound Auth</li>
     * </ul>
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Access token expiration time in seconds
     * 
     * <p>Tells client when to refresh the token</p>
     * 
     * <p><b>Client-side check:</b></p>
     * <pre>
     * const expirationTime = Date.now() + (expiresIn * 1000);
     * 
     * // Check before each request
     * if (Date.now() >= expirationTime - 60000) {  // 1 min buffer
     *   await refreshAccessToken();
     * }
     * </pre>
     * 
     * <p><b>Common values:</b></p>
     * <ul>
     *   <li>900 seconds (15 minutes)</li>
     *   <li>3600 seconds (1 hour)</li>
     *   <li>28800 seconds (8 hours)</li>
     * </ul>
     */
    private Long expiresIn;
}

