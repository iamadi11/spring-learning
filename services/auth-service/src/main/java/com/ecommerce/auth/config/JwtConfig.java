package com.ecommerce.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT Configuration Properties
 * 
 * <p>This class reads JWT-related configuration from application.yml and makes it
 * available throughout the application. It uses Spring Boot's @ConfigurationProperties
 * to bind YAML properties to Java fields.</p>
 * 
 * <h2>Configuration in application.yml:</h2>
 * <pre>
 * jwt:
 *   secret: mySecretKey123...  # 256-bit secret for signing tokens
 *   access-token-expiration: 3600000  # 1 hour in milliseconds
 *   refresh-token-expiration: 86400000  # 24 hours in milliseconds
 *   issuer: ecommerce-auth-service  # Who issued the token
 * </pre>
 * 
 * <h2>JWT Token Structure:</h2>
 * <pre>
 * JWT = Header.Payload.Signature
 * 
 * Header:
 * {
 *   "alg": "HS256",  // Algorithm: HMAC SHA-256
 *   "typ": "JWT"     // Type: JSON Web Token
 * }
 * 
 * Payload:
 * {
 *   "sub": "user@example.com",  // Subject (user identifier)
 *   "iss": "ecommerce-auth-service",  // Issuer
 *   "iat": 1634567890,  // Issued At (timestamp)
 *   "exp": 1634571490,  // Expiration Time (timestamp)
 *   "roles": ["ROLE_USER"],  // Custom claim: user roles
 *   "tenantId": "tenant1"  // Custom claim: tenant ID
 * }
 * 
 * Signature:
 * HMACSHA256(
 *   base64UrlEncode(header) + "." + base64UrlEncode(payload),
 *   secret  // Secret key from configuration
 * )
 * </pre>
 * 
 * <h2>Token Types:</h2>
 * <pre>
 * Access Token:
 * - Short-lived (1 hour)
 * - Used for API authentication
 * - Sent in Authorization header: "Bearer {token}"
 * - If compromised, expires quickly
 * 
 * Refresh Token:
 * - Long-lived (24 hours)
 * - Used to obtain new access token
 * - Stored in database for revocation
 * - Can be revoked manually (logout, security breach)
 * 
 * ID Token (OAuth2/OIDC):
 * - Contains user profile information
 * - Used for identification, not authentication
 * - Returned during OAuth2 login
 * </pre>
 * 
 * <h2>Security Best Practices:</h2>
 * <ul>
 *   <li><b>Secret Key:</b> Use strong random string (256+ bits), store in environment variable</li>
 *   <li><b>Algorithm:</b> Use HS256 (HMAC SHA-256) or RS256 (RSA SHA-256)</li>
 *   <li><b>Expiration:</b> Short for access tokens (minutes/hours), longer for refresh (days)</li>
 *   <li><b>Claims:</b> Only include necessary data, avoid sensitive information</li>
 *   <li><b>HTTPS:</b> Always transmit tokens over HTTPS to prevent interception</li>
 *   <li><b>Storage:</b> Client stores in memory or HttpOnly cookie (not localStorage)</li>
 * </ul>
 * 
 * <h2>Token Lifecycle:</h2>
 * <pre>
 * 1. Login:
 *    User → [email, password] → Server
 *    Server → [validates] → [generates tokens] → Client
 *    Response: {
 *      "accessToken": "eyJhbGciOi...",
 *      "refreshToken": "eyJhbGciOi...",
 *      "expiresIn": 3600
 *    }
 * 
 * 2. API Request:
 *    Client → [Authorization: Bearer {accessToken}] → Server
 *    Server → [validates token] → [extracts user info] → Process request
 * 
 * 3. Token Refresh:
 *    Access token expires after 1 hour
 *    Client → [refreshToken] → Server
 *    Server → [validates refresh token] → [generates new access token] → Client
 *    Response: {
 *      "accessToken": "eyJhbGciOi...",
 *      "expiresIn": 3600
 *    }
 * 
 * 4. Logout:
 *    Client → [refreshToken] → Server
 *    Server → [deletes refresh token from database] → Success
 *    Client → [clears tokens from memory]
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration  // Marks this as a Spring configuration class
@ConfigurationProperties(prefix = "jwt")  // Binds properties starting with "jwt." from YAML
@Data  // Lombok: generates getters, setters, toString, equals, hashCode
public class JwtConfig {

    /**
     * Secret key used to sign JWT tokens
     * 
     * <p><b>Requirements:</b></p>
     * <ul>
     *   <li>At least 256 bits (32 characters) for HS256</li>
     *   <li>Random, unpredictable string</li>
     *   <li>Never commit to version control</li>
     *   <li>Store in environment variable or secrets manager</li>
     * </ul>
     * 
     * <p><b>Generating a secure secret:</b></p>
     * <pre>
     * // Java
     * String secret = Base64.getEncoder().encodeToString(
     *     new SecureRandom().generateSeed(32)
     * );
     * 
     * // OpenSSL
     * openssl rand -base64 32
     * 
     * // Node.js
     * require('crypto').randomBytes(32).toString('base64')
     * </pre>
     * 
     * <p><b>Environment Variable:</b></p>
     * <pre>
     * export JWT_SECRET="your-generated-secret-here"
     * 
     * In application.yml:
     * jwt:
     *   secret: ${JWT_SECRET:defaultSecretForDevOnly}
     * </pre>
     */
    private String secret;

    /**
     * Access token expiration time in milliseconds
     * 
     * <p><b>Common Values:</b></p>
     * <ul>
     *   <li>15 minutes = 900,000 ms (high security)</li>
     *   <li>1 hour = 3,600,000 ms (balanced)</li>
     *   <li>8 hours = 28,800,000 ms (convenience)</li>
     * </ul>
     * 
     * <p><b>Considerations:</b></p>
     * <ul>
     *   <li>Shorter = more secure (less time if token stolen)</li>
     *   <li>Shorter = more refresh requests (more server load)</li>
     *   <li>Longer = better UX (less frequent refreshes)</li>
     *   <li>Longer = higher risk (stolen token valid longer)</li>
     * </ul>
     */
    private Long accessTokenExpiration;

    /**
     * Refresh token expiration time in milliseconds
     * 
     * <p><b>Common Values:</b></p>
     * <ul>
     *   <li>1 day = 86,400,000 ms</li>
     *   <li>7 days = 604,800,000 ms</li>
     *   <li>30 days = 2,592,000,000 ms</li>
     * </ul>
     * 
     * <p><b>Considerations:</b></p>
     * <ul>
     *   <li>Should be significantly longer than access token</li>
     *   <li>Stored in database for revocation capability</li>
     *   <li>Can implement refresh token rotation for added security</li>
     * </ul>
     */
    private Long refreshTokenExpiration;

    /**
     * Token issuer - identifies who created the token
     * 
     * <p>The issuer (iss) claim is a standard JWT claim that identifies
     * the principal that issued the JWT. This is useful in multi-service
     * environments where tokens from different services need to be
     * distinguished.</p>
     * 
     * <p><b>Validation:</b></p>
     * <pre>
     * When validating a token, verify the issuer:
     * if (!token.getIssuer().equals(jwtConfig.getIssuer())) {
     *     throw new InvalidTokenException("Invalid token issuer");
     * }
     * </pre>
     * 
     * <p><b>Example values:</b></p>
     * <ul>
     *   <li>ecommerce-auth-service</li>
     *   <li>https://auth.yourdomain.com</li>
     *   <li>urn:ecommerce:auth</li>
     * </ul>
     */
    private String issuer;
}

