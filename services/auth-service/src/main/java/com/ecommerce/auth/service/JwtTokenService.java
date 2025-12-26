package com.ecommerce.auth.service;

import com.ecommerce.auth.config.JwtConfig;
import com.ecommerce.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT Token Service
 * 
 * <p>Service responsible for creating, validating, and parsing JWT tokens.
 * Handles both access tokens and refresh tokens.</p>
 * 
 * <h2>JWT Structure:</h2>
 * <pre>
 * JWT = Header.Payload.Signature (separated by dots)
 * 
 * Example JWT:
 * eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
 * eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNTE2MjM5MDIyfQ.
 * SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
 * 
 * Header (Base64 encoded):
 * {
 *   "alg": "HS256",  // Algorithm: HMAC SHA-256
 *   "typ": "JWT"     // Type: JSON Web Token
 * }
 * 
 * Payload (Base64 encoded):
 * {
 *   "sub": "user@example.com",           // Subject (user email)
 *   "iss": "ecommerce-auth-service",     // Issuer
 *   "iat": 1634567890,                   // Issued At (Unix timestamp)
 *   "exp": 1634571490,                   // Expiration (Unix timestamp)
 *   "username": "johndoe",               // Custom claim: username
 *   "roles": ["ROLE_USER", "ROLE_SELLER"], // Custom claim: roles
 *   "tenantId": "tenant1"                // Custom claim: tenant
 * }
 * 
 * Signature:
 * HMACSHA256(
 *   base64UrlEncode(header) + "." + base64UrlEncode(payload),
 *   secret_key
 * )
 * </pre>
 * 
 * <h2>Token Generation Flow:</h2>
 * <pre>
 * 1. User authenticates successfully (login)
 * 
 * 2. Call generateAccessToken(User user):
 *    a. Create claims map with user info
 *    b. Set subject (user email)
 *    c. Set issued at time (now)
 *    d. Set expiration time (now + 1 hour)
 *    e. Add custom claims (roles, tenant, etc.)
 *    f. Sign with secret key
 *    g. Return JWT string
 * 
 * 3. Call generateRefreshToken(User user):
 *    a. Similar to access token
 *    b. Longer expiration (24 hours)
 *    c. Save to database for revocation
 *    d. Return JWT string
 * 
 * 4. Return both tokens to client
 * </pre>
 * 
 * <h2>Token Validation Flow:</h2>
 * <pre>
 * 1. Client sends request with token:
 *    Authorization: Bearer eyJhbGciOi...
 * 
 * 2. Extract token from header
 * 
 * 3. Call validateToken(String token):
 *    a. Parse JWT with secret key
 *    b. Check signature validity
 *    c. Check expiration date
 *    d. Check issuer
 *    e. If all valid, return true
 *    f. If any invalid, throw exception
 * 
 * 4. Call getEmailFromToken(String token):
 *    a. Extract subject claim
 *    b. Return user email
 * 
 * 5. Load user from database using email
 * 
 * 6. Create Authentication object
 * 
 * 7. Set in SecurityContext
 * 
 * 8. Proceed with request processing
 * </pre>
 * 
 * <h2>Security Considerations:</h2>
 * <ul>
 *   <li><b>Secret Key:</b> Must be strong (256+ bits), stored securely (env variable)</li>
 *   <li><b>Algorithm:</b> HS256 (symmetric) or RS256 (asymmetric for multiple services)</li>
 *   <li><b>Expiration:</b> Short for access (1h), longer for refresh (24h)</li>
 *   <li><b>Signature Verification:</b> Always verify signature before trusting token</li>
 *   <li><b>Claims Validation:</b> Validate issuer, expiration, and required claims</li>
 *   <li><b>No Sensitive Data:</b> Never include passwords or secrets in token</li>
 * </ul>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service  // Marks this as a Spring service component
public class JwtTokenService {

    // Logger for debugging and error tracking
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    // JWT configuration (secret, expiration, issuer)
    private final JwtConfig jwtConfig;

    // Secret key for signing tokens (derived from config)
    private final SecretKey secretKey;

    /**
     * Constructor with dependency injection
     * 
     * @param jwtConfig JWT configuration from application.yml
     */
    @Autowired  // Spring will inject JwtConfig bean
    public JwtTokenService(JwtConfig jwtConfig) {
        // Store configuration
        this.jwtConfig = jwtConfig;
        
        // Create secret key from configuration secret string
        // Keys.hmacShaKeyFor() creates a SecretKey suitable for HMAC-SHA algorithms
        // We convert the secret string to bytes using UTF-8 encoding
        this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        
        // Log initialization (without revealing secret)
        logger.info("JWT Token Service initialized with issuer: {}", jwtConfig.getIssuer());
    }

    /**
     * Generate Access Token
     * 
     * <p>Creates a short-lived JWT token for API authentication.
     * Contains user identity and permissions.</p>
     * 
     * @param user User entity to create token for
     * @return JWT access token string
     */
    public String generateAccessToken(User user) {
        // Log token generation (for debugging)
        logger.debug("Generating access token for user: {}", user.getEmail());
        
        // Create custom claims (additional user information)
        Map<String, Object> claims = new HashMap<>();
        
        // Add username claim
        claims.put("username", user.getUsername());
        
        // Add roles claim (convert Role objects to role names)
        // Extract role names from user's roles and collect into list
        claims.put("roles", user.getRoles().stream()
                .map(role -> role.getName())  // Get role name
                .collect(Collectors.toList()));  // Collect to list
        
        // Add tenant ID claim for multi-tenancy
        claims.put("tenantId", user.getTenantId());
        
        // Add user ID claim (useful for quick lookups)
        claims.put("userId", user.getId());
        
        // Build and return the JWT token
        return Jwts.builder()
                // Set subject (primary identifier - user email)
                .setSubject(user.getEmail())
                
                // Set issuer (who created this token)
                .setIssuer(jwtConfig.getIssuer())
                
                // Set issued at time (current time)
                .setIssuedAt(new Date())
                
                // Set expiration time (current time + configured expiration)
                // jwtConfig.getAccessTokenExpiration() returns milliseconds
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getAccessTokenExpiration()))
                
                // Add all custom claims
                .addClaims(claims)
                
                // Sign the token with secret key using HS256 algorithm
                .signWith(secretKey, SignatureAlgorithm.HS256)
                
                // Build the final JWT string
                .compact();
    }

    /**
     * Generate Refresh Token
     * 
     * <p>Creates a long-lived JWT token for obtaining new access tokens.
     * Should be stored in database for revocation capability.</p>
     * 
     * @param user User entity to create token for
     * @return JWT refresh token string
     */
    public String generateRefreshToken(User user) {
        // Log token generation
        logger.debug("Generating refresh token for user: {}", user.getEmail());
        
        // Refresh tokens typically have minimal claims (lighter payload)
        Map<String, Object> claims = new HashMap<>();
        
        // Just include user ID for lookup
        claims.put("userId", user.getId());
        
        // Build and return the JWT token
        return Jwts.builder()
                // Set subject (user email)
                .setSubject(user.getEmail())
                
                // Set issuer
                .setIssuer(jwtConfig.getIssuer())
                
                // Set issued at time
                .setIssuedAt(new Date())
                
                // Set expiration time (longer than access token)
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getRefreshTokenExpiration()))
                
                // Add minimal claims
                .addClaims(claims)
                
                // Sign with secret key
                .signWith(secretKey, SignatureAlgorithm.HS256)
                
                // Build the JWT string
                .compact();
    }

    /**
     * Validate Token
     * 
     * <p>Validates JWT token signature, expiration, and issuer.
     * Returns true if valid, throws exception if invalid.</p>
     * 
     * @param token JWT token string to validate
     * @return true if token is valid
     * @throws JwtException if token is invalid (signature, expiration, format)
     */
    public boolean validateToken(String token) {
        try {
            // Parse the JWT token with secret key
            // This will automatically verify:
            // 1. Signature is valid (token not tampered with)
            // 2. Token is not expired
            // 3. Token format is valid
            Jws<Claims> claims = Jwts.parser()
                    // Set the signing key to verify signature
                    .verifyWith(secretKey)
                    
                    // Build the parser
                    .build()
                    
                    // Parse the JWT token
                    // Throws JwtException if any validation fails
                    .parseSignedClaims(token);
            
            // Additional validation: Check issuer matches our configuration
            String issuer = claims.getPayload().getIssuer();
            if (!jwtConfig.getIssuer().equals(issuer)) {
                // Issuer mismatch - token from different service
                logger.warn("Invalid token issuer: {}", issuer);
                return false;
            }
            
            // All validations passed
            logger.debug("Token validated successfully");
            return true;
            
        } catch (ExpiredJwtException e) {
            // Token has expired (exp claim < current time)
            logger.error("JWT token expired: {}", e.getMessage());
            throw e;
            
        } catch (UnsupportedJwtException e) {
            // Token format not supported
            logger.error("JWT token unsupported: {}", e.getMessage());
            throw e;
            
        } catch (MalformedJwtException e) {
            // Token structure is invalid
            logger.error("JWT token malformed: {}", e.getMessage());
            throw e;
            
        } catch (SignatureException e) {
            // Signature verification failed (token tampered with or wrong secret)
            logger.error("JWT signature invalid: {}", e.getMessage());
            throw e;
            
        } catch (IllegalArgumentException e) {
            // Token is empty or null
            logger.error("JWT token empty: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get Email from Token
     * 
     * <p>Extracts the subject claim (user email) from JWT token.</p>
     * 
     * @param token JWT token string
     * @return User email from token subject
     */
    public String getEmailFromToken(String token) {
        // Parse token and extract claims
        // The subject claim contains the user email
        return Jwts.parser()
                // Set signing key
                .verifyWith(secretKey)
                
                // Build parser
                .build()
                
                // Parse token
                .parseSignedClaims(token)
                
                // Get claims body
                .getPayload()
                
                // Get subject claim (email)
                .getSubject();
    }

    /**
     * Get All Claims from Token
     * 
     * <p>Extracts all claims from JWT token payload.</p>
     * 
     * @param token JWT token string
     * @return Claims object containing all token claims
     */
    public Claims getClaimsFromToken(String token) {
        // Parse token and return all claims
        return Jwts.parser()
                // Set signing key
                .verifyWith(secretKey)
                
                // Build parser
                .build()
                
                // Parse token
                .parseSignedClaims(token)
                
                // Get claims body (contains all claims)
                .getPayload();
    }

    /**
     * Get Username from Token
     * 
     * <p>Extracts the username custom claim from JWT token.</p>
     * 
     * @param token JWT token string
     * @return Username from token claims
     */
    public String getUsernameFromToken(String token) {
        // Get all claims
        Claims claims = getClaimsFromToken(token);
        
        // Extract username claim
        return claims.get("username", String.class);
    }

    /**
     * Get User ID from Token
     * 
     * <p>Extracts the user ID custom claim from JWT token.</p>
     * 
     * @param token JWT token string
     * @return User ID from token claims
     */
    public Long getUserIdFromToken(String token) {
        // Get all claims
        Claims claims = getClaimsFromToken(token);
        
        // Extract userId claim and convert to Long
        return claims.get("userId", Long.class);
    }

    /**
     * Check if Token is Expired
     * 
     * <p>Checks if the token's expiration time has passed.</p>
     * 
     * @param token JWT token string
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            // Get all claims
            Claims claims = getClaimsFromToken(token);
            
            // Get expiration date
            Date expiration = claims.getExpiration();
            
            // Compare with current date
            // Returns true if expiration is before now (expired)
            return expiration.before(new Date());
            
        } catch (ExpiredJwtException e) {
            // Token is expired (exception thrown during parsing)
            return true;
        }
    }
}

