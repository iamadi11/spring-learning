# Complete Authentication & Authorization Guide

## 🔐 Overview

This guide covers **comprehensive authentication and authorization** implementation including OAuth2, JWT, Social Login, Two-Factor Authentication, Multi-tenancy, and API Key Management.

---

## 📚 Table of Contents

1. [Authentication vs Authorization](#authentication-vs-authorization)
2. [OAuth2 Complete Guide](#oauth2-complete-guide)
3. [JWT Tokens](#jwt-tokens)
4. [Social Login Integration](#social-login-integration)
5. [Two-Factor Authentication](#two-factor-authentication)
6. [Multi-Tenancy](#multi-tenancy)
7. [API Key Management](#api-key-management)
8. [Session Management](#session-management)
9. [Security Best Practices](#security-best-practices)

---

## 1. Authentication vs Authorization

### Authentication (Who are you?)

**Definition**: Verifying the identity of a user or service

**Examples**:
- Username + Password
- Social Login (Google, GitHub)
- Biometric (fingerprint, face)
- Two-Factor Authentication

**Question Answered**: "Are you who you claim to be?"

### Authorization (What can you do?)

**Definition**: Determining what an authenticated user can access

**Examples**:
- Role-Based Access Control (RBAC)
- Permission-Based Access
- Resource ownership
- Scopes

**Question Answered**: "Are you allowed to do this?"

### Example Flow

```
1. User enters username + password → AUTHENTICATION
   ✅ Valid credentials → User is authenticated

2. User tries to access /admin/users → AUTHORIZATION
   ✅ User has ADMIN role → Access granted
   ❌ User has USER role → Access denied (403 Forbidden)
```

---

## 2. OAuth2 Complete Guide

### What is OAuth2?

**OAuth2** is an authorization framework that enables third-party applications to obtain limited access to a service without exposing user credentials.

### Key Concepts

**1. Roles**:
- **Resource Owner**: User who owns the data
- **Client**: Application requesting access (your app)
- **Authorization Server**: Issues access tokens (Auth Service)
- **Resource Server**: Holds protected resources (User/Order/Product Services)

**2. Tokens**:
- **Access Token**: Short-lived (15-60 min), grants access to resources
- **Refresh Token**: Long-lived (7-30 days), obtains new access tokens
- **ID Token**: Contains user information (OpenID Connect)

### OAuth2 Grant Types

#### Grant Type 1: Authorization Code Flow

**Used For**: Web applications with backend

**Flow**:
```
1. User clicks "Login"
   ↓
2. Redirect to Authorization Server
   https://auth.example.com/oauth/authorize?
     response_type=code&
     client_id=YOUR_CLIENT_ID&
     redirect_uri=https://yourapp.com/callback&
     scope=read write
   ↓
3. User logs in and approves
   ↓
4. Redirect back with authorization code
   https://yourapp.com/callback?code=AUTH_CODE
   ↓
5. Exchange code for tokens (server-side)
   POST /oauth/token
   {
     "grant_type": "authorization_code",
     "code": "AUTH_CODE",
     "client_id": "YOUR_CLIENT_ID",
     "client_secret": "YOUR_SECRET",
     "redirect_uri": "https://yourapp.com/callback"
   }
   ↓
6. Receive tokens
   {
     "access_token": "eyJ...",
     "refresh_token": "abc...",
     "expires_in": 3600,
     "token_type": "Bearer"
   }
```

**Security**: ✅ Most secure (client secret never exposed to browser)

#### Grant Type 2: PKCE (Proof Key for Code Exchange)

**Used For**: Mobile apps, SPAs (Single Page Applications)

**Why Needed?**: Mobile/SPA apps can't securely store client secret

**Flow**:
```
1. Generate code_verifier (random string)
   code_verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"

2. Create code_challenge (SHA256 hash)
   code_challenge = BASE64URL(SHA256(code_verifier))
   
3. Authorization request with code_challenge
   /oauth/authorize?
     response_type=code&
     client_id=YOUR_CLIENT_ID&
     code_challenge=CHALLENGE&
     code_challenge_method=S256

4. Token request with code_verifier
   POST /oauth/token
   {
     "grant_type": "authorization_code",
     "code": "AUTH_CODE",
     "client_id": "YOUR_CLIENT_ID",
     "code_verifier": "dBjftJeZ..."
   }
```

**Security**: ✅ Secure for public clients (no secret needed)

#### Grant Type 3: Client Credentials

**Used For**: Service-to-service authentication

**Flow**:
```
POST /oauth/token
{
  "grant_type": "client_credentials",
  "client_id": "service-a",
  "client_secret": "secret",
  "scope": "service:read service:write"
}

Response:
{
  "access_token": "eyJ...",
  "expires_in": 3600,
  "token_type": "Bearer"
}
```

**Use Case**: Order Service → Product Service

**Security**: ✅ Secure (credentials on server only)

#### Grant Type 4: Refresh Token

**Used For**: Obtaining new access token without re-authentication

**Flow**:
```
POST /oauth/token
{
  "grant_type": "refresh_token",
  "refresh_token": "abc...",
  "client_id": "YOUR_CLIENT_ID",
  "client_secret": "YOUR_SECRET"
}

Response:
{
  "access_token": "NEW_ACCESS_TOKEN",
  "refresh_token": "NEW_REFRESH_TOKEN",
  "expires_in": 3600
}
```

**When**: Access token expired, but refresh token still valid

---

## 3. JWT Tokens

### JWT Structure

**JWT Format**: `HEADER.PAYLOAD.SIGNATURE`

**Example**:
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

**Decoded**:

**Header**:
```json
{
  "alg": "HS256",  // Algorithm: HMAC SHA-256
  "typ": "JWT"     // Type: JWT
}
```

**Payload**:
```json
{
  "sub": "1234567890",              // Subject (user ID)
  "name": "John Doe",               // User name
  "email": "john@example.com",      // Email
  "roles": ["USER", "ADMIN"],       // Roles
  "permissions": ["read", "write"], // Permissions
  "iat": 1516239022,                // Issued At
  "exp": 1516242622,                // Expiration (1 hour later)
  "iss": "https://auth.example.com" // Issuer
}
```

**Signature**:
```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret_key
)
```

### JWT Token Types

**Access Token** (Short-lived):
- **Purpose**: Access protected resources
- **Lifetime**: 15-60 minutes
- **Storage**: Memory or sessionStorage (SPA)
- **Renewal**: Use refresh token

**Refresh Token** (Long-lived):
- **Purpose**: Obtain new access tokens
- **Lifetime**: 7-30 days
- **Storage**: HttpOnly cookie or secure storage
- **Rotation**: Issue new refresh token on use

**ID Token** (OpenID Connect):
- **Purpose**: User authentication proof
- **Contains**: User profile information
- **Standard Claims**: sub, name, email, picture

### JWT Validation

```java
@Component
public class JwtTokenValidator {
    
    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
            
            // Check expiration
            Date expiration = claims.getBody().getExpiration();
            if (expiration.before(new Date())) {
                return false; // Token expired
            }
            
            // Check issuer
            String issuer = claims.getBody().getIssuer();
            if (!issuer.equals(EXPECTED_ISSUER)) {
                return false; // Wrong issuer
            }
            
            return true;
            
        } catch (JwtException e) {
            return false; // Invalid signature or malformed
        }
    }
}
```

---

## 4. Social Login Integration

### Google OAuth2

**Step 1: Register App**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create project
3. Enable Google+ API
4. Create OAuth2 credentials
5. Get Client ID and Secret

**Step 2: Configure Spring Security**
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_GOOGLE_CLIENT_ID
            client-secret: YOUR_GOOGLE_SECRET
            scope:
              - email
              - profile
        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
            token-uri: https://oauth2.googleapis.com/token
            user-info-uri: https://www.googleapis.com/oauth2/v3/userinfo
```

**Step 3: Handle Callback**
```java
@GetMapping("/oauth2/callback/google")
public ResponseEntity<?> googleCallback(@RequestParam String code) {
    // 1. Exchange code for Google access token
    String googleAccessToken = googleOAuthService.exchangeCode(code);
    
    // 2. Fetch user profile from Google
    GoogleUserInfo userInfo = googleOAuthService.getUserInfo(googleAccessToken);
    
    // 3. Find or create user in our database
    User user = userService.findOrCreateByEmail(userInfo.getEmail());
    
    // 4. Generate our JWT tokens
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);
    
    // 5. Return tokens to client
    return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken));
}
```

### GitHub OAuth2

Similar flow, different provider URLs:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: YOUR_GITHUB_CLIENT_ID
            client-secret: YOUR_GITHUB_SECRET
            scope:
              - user:email
        provider:
          github:
            authorization-uri: https://github.com/login/oauth/authorize
            token-uri: https://github.com/login/oauth/access_token
            user-info-uri: https://api.github.com/user
```

---

## 5. Two-Factor Authentication (2FA)

### TOTP (Time-based One-Time Password)

**Algorithm**: Google Authenticator compatible

**Setup Flow**:
```
1. User enables 2FA
   ↓
2. Generate secret key
   secret = generateRandomSecret() // Base32 encoded
   ↓
3. Generate QR code
   otpauth://totp/MyApp:user@example.com?secret=SECRET&issuer=MyApp
   ↓
4. User scans QR code with authenticator app
   ↓
5. User enters 6-digit code to verify
   ↓
6. Save secret to database (encrypted)
```

**Implementation**:
```java
@Service
public class TwoFactorAuthService {
    
    public String generateSecret() {
        return new String(Base32.encode(
            SecureRandom.getSeed(20)
        ));
    }
    
    public String generateQRCodeUrl(String email, String secret) {
        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s",
            APP_NAME, email, secret, APP_NAME
        );
    }
    
    public boolean verifyCode(String secret, String code) {
        long currentTime = System.currentTimeMillis() / 1000 / 30;
        
        // Check current time window and ±1 (tolerance)
        for (int i = -1; i <= 1; i++) {
            String expectedCode = generateCode(secret, currentTime + i);
            if (expectedCode.equals(code)) {
                return true;
            }
        }
        return false;
    }
    
    private String generateCode(String secret, long timeWindow) {
        byte[] key = Base32.decode(secret);
        byte[] data = ByteBuffer.allocate(8).putLong(timeWindow).array();
        
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(data);
        
        int offset = hash[hash.length - 1] & 0xF;
        int binary = ((hash[offset] & 0x7F) << 24)
            | ((hash[offset + 1] & 0xFF) << 16)
            | ((hash[offset + 2] & 0xFF) << 8)
            | (hash[offset + 3] & 0xFF);
        
        int otp = binary % 1000000;
        return String.format("%06d", otp);
    }
}
```

**Login Flow with 2FA**:
```
1. User enters username + password
   ↓
2. Validate credentials ✅
   ↓
3. Check if 2FA enabled
   ↓
4. Request 6-digit code
   ↓
5. User enters code from authenticator app
   ↓
6. Verify code
   ↓
7. Issue JWT tokens ✅
```

---

## 6. Multi-Tenancy

### What is Multi-Tenancy?

**Definition**: Single application instance serves multiple customers (tenants)

**Types**:
1. **Database per Tenant**: Separate database for each tenant
2. **Schema per Tenant**: Separate schema in same database
3. **Shared Schema**: Same schema, filter by tenant_id

**We use**: Shared Schema (most cost-effective)

### Implementation

**Tenant Entity**:
```java
@Entity
@Table(name = "tenants")
public class Tenant {
    @Id
    private UUID id;
    
    @Column(unique = true)
    private String name; // "acme-corp"
    
    @Column(unique = true)
    private String domain; // "acme.myapp.com"
    
    private TenantStatus status; // ACTIVE, SUSPENDED, DELETED
    
    private LocalDateTime createdAt;
}
```

**Tenant Context** (Thread-local storage):
```java
public class TenantContext {
    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();
    
    public static void setTenantId(UUID tenantId) {
        currentTenant.set(tenantId);
    }
    
    public static UUID getTenantId() {
        return currentTenant.get();
    }
    
    public static void clear() {
        currentTenant.remove();
    }
}
```

**Tenant Filter** (Intercept all requests):
```java
@Component
public class TenantFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) {
        
        try {
            // Extract tenant from subdomain or header
            String tenantId = extractTenant(request);
            
            // Set in thread-local context
            TenantContext.setTenantId(UUID.fromString(tenantId));
            
            filterChain.doFilter(request, response);
            
        } finally {
            TenantContext.clear();
        }
    }
    
    private String extractTenant(HttpServletRequest request) {
        // From subdomain: acme.myapp.com
        String host = request.getServerName();
        String subdomain = host.split("\\.")[0];
        
        // Or from header: X-Tenant-ID
        String headerTenant = request.getHeader("X-Tenant-ID");
        
        return headerTenant != null ? headerTenant : subdomain;
    }
}
```

**Tenant-Aware Queries**:
```java
@Entity
@Table(name = "users")
@FilterDef(name = "tenantFilter", 
    parameters = @ParamDef(name = "tenantId", type = "uuid-char"))
@Filter(name = "tenantFilter", 
    condition = "tenant_id = :tenantId")
public class User {
    @Id
    private UUID id;
    
    @Column(name = "tenant_id")
    private UUID tenantId;
    
    private String email;
    // ...
}

// Repository usage
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId")
    List<User> findAllByTenant(@Param("tenantId") UUID tenantId);
}
```

---

## 7. API Key Management

### Use Case

**Service-to-service authentication** without OAuth2 flow

**Example**: Third-party integrations, webhooks

### Implementation

```java
@Entity
@Table(name = "api_keys")
public class ApiKey {
    @Id
    private UUID id;
    
    @Column(unique = true)
    private String keyValue; // "sk_live_abc123..."
    
    private String name; // "Production Integration"
    
    @ManyToOne
    private User owner;
    
    @ElementCollection
    private Set<String> scopes; // ["orders:read", "products:write"]
    
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private boolean revoked;
}
```

**API Key Format**:
```
Prefix: sk_live_  (secret key, live environment)
       sk_test_  (secret key, test environment)
       pk_live_  (publishable key, live)

Random: 32 characters

Example: <prefix>_<environment>_<32_random_characters>
         (e.g., sk + test + random string)
```

**Validation**:
```java
@Component
public class ApiKeyValidator {
    
    public ApiKey validateKey(String keyValue) {
        // 1. Find key in database
        ApiKey apiKey = apiKeyRepository
            .findByKeyValue(keyValue)
            .orElseThrow(() -> new InvalidApiKeyException());
        
        // 2. Check if revoked
        if (apiKey.isRevoked()) {
            throw new ApiKeyRevokedException();
        }
        
        // 3. Check expiration
        if (apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiKeyExpiredException();
        }
        
        // 4. Update last used
        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);
        
        return apiKey;
    }
}
```

**Usage**:
```bash
curl -H "Authorization: Bearer sk_live_abc123..." \
  https://api.example.com/orders
```

---

## 8. Session Management

### Stateless (JWT-based) - Recommended

**Advantages**:
- ✅ Scalable (no server state)
- ✅ Works across services
- ✅ No database lookups

**Disadvantages**:
- ❌ Can't revoke immediately (until expiry)
- ❌ Larger payload

### Stateful (Session-based)

**Implementation**:
```java
// Store session in Redis
@Component
public class SessionManager {
    
    private final RedisTemplate<String, Session> redisTemplate;
    
    public String createSession(User user) {
        String sessionId = UUID.randomUUID().toString();
        
        Session session = Session.builder()
            .userId(user.getId())
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(24))
            .build();
        
        // Store in Redis with TTL
        redisTemplate.opsForValue().set(
            "session:" + sessionId,
            session,
            24, TimeUnit.HOURS
        );
        
        return sessionId;
    }
    
    public Session getSession(String sessionId) {
        return redisTemplate.opsForValue()
            .get("session:" + sessionId);
    }
    
    public void invalidateSession(String sessionId) {
        redisTemplate.delete("session:" + sessionId);
    }
}
```

---

## 9. Security Best Practices

### Password Storage

**NEVER** store plain text passwords!

```java
@Service
public class PasswordService {
    
    private final BCryptPasswordEncoder encoder = 
        new BCryptPasswordEncoder(12); // 12 rounds
    
    public String hashPassword(String plainPassword) {
        return encoder.encode(plainPassword);
    }
    
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        return encoder.matches(plainPassword, hashedPassword);
    }
}
```

### Token Storage

**Browser Storage**:
- ❌ **localStorage**: Vulnerable to XSS
- ⚠️ **sessionStorage**: Better, but still XSS risk
- ✅ **HttpOnly Cookie**: Best (immune to XSS)

**Mobile Apps**:
- ✅ **Keychain** (iOS)
- ✅ **Keystore** (Android)

### HTTPS Everywhere

- ✅ All communication over HTTPS
- ✅ HSTS header (force HTTPS)
- ✅ Certificate pinning (mobile apps)

### Rate Limiting

```java
@RateLimiter(name = "login", fallbackMethod = "loginRateLimitFallback")
public TokenResponse login(LoginRequest request) {
    // Login logic
}

public TokenResponse loginRateLimitFallback(
        LoginRequest request, 
        Throwable throwable) {
    throw new TooManyRequestsException(
        "Too many login attempts. Try again in 15 minutes."
    );
}
```

**Configuration**:
```yaml
resilience4j:
  ratelimiter:
    instances:
      login:
        limitForPeriod: 5      # 5 attempts
        limitRefreshPeriod: 15m # per 15 minutes
        timeoutDuration: 0
```

---

## 🎯 Summary

This guide covered:
- ✅ OAuth2 (all grant types)
- ✅ JWT tokens (structure, validation)
- ✅ Social Login (Google, GitHub)
- ✅ Two-Factor Authentication (TOTP)
- ✅ Multi-Tenancy (shared schema)
- ✅ API Key Management
- ✅ Session Management
- ✅ Security Best Practices

**You now have production-grade authentication knowledge!** 🔐

