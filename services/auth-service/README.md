# Auth Service

## Overview

The Authentication Service is the core security component of the E-commerce Microservices Platform. It provides comprehensive user authentication and authorization capabilities including JWT-based authentication, OAuth2 social login, two-factor authentication, and multi-tenancy support.

## Features Implemented

### Core Authentication
- ✅ **User Registration** - Email/password registration with validation
- ✅ **User Login** - Secure authentication with BCrypt password hashing
- ✅ **JWT Tokens** - Access and refresh token generation
- ✅ **Token Refresh** - Seamless token renewal without re-authentication
- ✅ **Logout** - Token revocation via database

### Security Features
- ✅ **Password Encryption** - BCrypt with configurable strength (12 rounds)
- ✅ **Password Strength Validation** - Uppercase, lowercase, digit, special character requirements
- ✅ **JWT Signature Validation** - HMAC SHA-256 signature verification
- ✅ **Token Expiration** - Configurable access (1h) and refresh (24h) token lifetimes
- ✅ **Account Status** - PENDING, ACTIVE, INACTIVE, SUSPENDED, BANNED states

### Authorization
- ✅ **Role-Based Access Control (RBAC)** - USER, ADMIN, SELLER, SUPPORT roles
- ✅ **Permission System** - Granular permissions (READ_PRODUCT, WRITE_ORDER, etc.)
- ✅ **Role-Permission Mapping** - Flexible assignment of permissions to roles

### Multi-Tenancy
- ✅ **Tenant Isolation** - Separate data per tenant
- ✅ **Tenant-Scoped Users** - Username unique within tenant
- ✅ **Global Email Uniqueness** - Email unique across all tenants

### OAuth2 Support (Structure Ready)
- ✅ **OAuth2 Client Configuration** - Google, GitHub, Facebook, Twitter
- ✅ **OAuth2 Security Config** - Integration with Spring Security
- ✅ **Auth Provider Enum** - Support for multiple authentication providers
- 🔄 **OAuth2 Controllers** - To be implemented in next iteration

### 2FA Support (Structure Ready)
- ✅ **2FA Flag** - User entity supports 2FA status
- ✅ **TOTP Secret Storage** - Secure storage of 2FA secrets
- ✅ **Dependencies** - Google Authenticator library integrated
- 🔄 **2FA Endpoints** - To be implemented in next iteration

### API Key Management (Structure Ready)
- ✅ **API Key Entity** - Database schema for API keys
- ✅ **API Key Repository** - CRUD operations
- 🔄 **API Key Endpoints** - To be implemented in next iteration

## Architecture

### Technology Stack
- **Framework**: Spring Boot 3.2.0
- **Security**: Spring Security 6.x
- **JWT**: jjwt 0.12.3
- **Database**: PostgreSQL 15
- **Migration**: Flyway
- **Testing**: JUnit 5, Testcontainers, MockMvc

### Database Schema

```sql
-- Core Tables
tenants              # Multi-tenancy
users                # User accounts
roles                # RBAC roles
permissions          # Granular permissions
user_roles           # Many-to-many: users <-> roles
role_permissions     # Many-to-many: roles <-> permissions
api_keys             # Service-to-service authentication
refresh_tokens       # JWT refresh token storage
```

### API Endpoints

#### Public Endpoints (No Authentication)
```
POST   /api/auth/register      - Register new user
POST   /api/auth/login         - Authenticate and get tokens
POST   /api/auth/refresh       - Refresh access token
```

#### Protected Endpoints (Authentication Required)
```
POST   /api/auth/logout        - Logout and revoke refresh token
GET    /api/auth/me            - Get current user info (to be implemented)
```

### Authentication Flow

```
1. Registration:
   User → RegisterRequest → AuthService → BCrypt Hash → Database
   ↓
   Response: UserResponse (status: PENDING)

2. Login:
   User → LoginRequest → AuthenticationManager → UserDetailsService
   ↓                                              ↓
   Load from DB ← Check Password (BCrypt)
   ↓
   Generate JWT (access + refresh) → Save Refresh Token → Database
   ↓
   Response: TokenResponse

3. API Request:
   Client → [Authorization: Bearer {token}] → API Gateway
   ↓
   JWT Validation → Extract User → SecurityContext → Controller
   ↓
   Response: Data

4. Token Refresh:
   Client → RefreshToken → Validate (DB + JWT) → Generate New Access Token
   ↓
   Response: TokenResponse (new access token)

5. Logout:
   Client → RefreshToken → Delete from Database
   ↓
   Response: Success (access token expires naturally)
```

## Configuration

### Application Properties

```yaml
# Server Configuration
server:
  port: 8081

# Database Configuration
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ecommerce_auth_db
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway handles schema
    show-sql: true
  
  # Flyway Migration
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

# JWT Configuration
jwt:
  secret: ${JWT_SECRET:yourSecretKeyHere}  # Use env variable in production
  access-token-expiration: 3600000   # 1 hour in ms
  refresh-token-expiration: 86400000 # 24 hours in ms
  issuer: ecommerce-auth-service

# OAuth2 Configuration (for social login)
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: profile, email
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: read:user, user:email

# Service Discovery
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### Environment Variables

```bash
# Production Environment
export JWT_SECRET="your-256-bit-secret-key-here"
export GOOGLE_CLIENT_ID="your-google-client-id"
export GOOGLE_CLIENT_SECRET="your-google-client-secret"
export GITHUB_CLIENT_ID="your-github-client-id"
export GITHUB_CLIENT_SECRET="your-github-client-secret"
export SPRING_DATASOURCE_URL="jdbc:postgresql://postgres-host:5432/auth_db"
export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="secure-password"
```

## Testing

### Unit Tests
```bash
# Run unit tests
./gradlew :services:auth-service:test --tests "*Test"

# Example: JwtTokenServiceTest
- Token generation
- Token validation
- Token parsing
- Claim extraction
```

### Integration Tests
```bash
# Run integration tests (with Testcontainers)
./gradlew :services:auth-service:test --tests "*IntegrationTest"

# Example: AuthControllerIntegrationTest
- Registration flow
- Login flow
- Token refresh
- Validation errors
- Duplicate email handling
```

### Test Coverage
- Unit Tests: Core business logic (services, utilities)
- Integration Tests: API endpoints with real database
- Security Tests: Authentication and authorization
- Validation Tests: Input validation and error handling

## Running the Service

### Prerequisites
- Java 21
- PostgreSQL 15
- Docker (for Testcontainers)

### Local Development

1. **Start PostgreSQL**:
```bash
docker run -d \
  --name postgres-auth \
  -p 5432:5432 \
  -e POSTGRES_DB=ecommerce_auth_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:15-alpine
```

2. **Run Service**:
```bash
./gradlew :services:auth-service:bootRun
```

3. **Verify Service**:
```bash
curl http://localhost:8081/actuator/health
```

### Docker Deployment

```bash
# Build image
docker build -t auth-service:latest ./services/auth-service

# Run container
docker run -d \
  --name auth-service \
  -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/auth_db \
  -e JWT_SECRET=your-secret-key \
  auth-service:latest
```

## API Examples

### Register User

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe",
    "tenantId": "default"
  }'
```

### Login

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePass123!"
  }'
```

### Refresh Token

```bash
curl -X POST http://localhost:8081/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOi..."
  }'
```

### Logout

```bash
curl -X POST http://localhost:8081/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOi..."
  }'
```

## Security Best Practices

1. **JWT Secret**: Use strong, random 256-bit secret stored in environment variable
2. **HTTPS**: Always use HTTPS in production to protect tokens in transit
3. **Token Storage**: Store access tokens in memory, refresh tokens in HttpOnly cookies
4. **Password Policy**: Enforce strong passwords (uppercase, lowercase, digit, special char)
5. **Rate Limiting**: Implement rate limiting on login endpoint (to be added)
6. **Account Lockout**: Lock account after multiple failed login attempts (to be added)
7. **Email Verification**: Verify email before activating account (to be added)
8. **Audit Logging**: Log all authentication attempts and failures
9. **Token Rotation**: Consider rotating refresh tokens on each use
10. **CORS**: Configure CORS properly for frontend applications

## Next Steps

### Immediate Enhancements
1. ✅ Email verification flow
2. ✅ Password reset functionality
3. ✅ OAuth2 controller implementation
4. ✅ 2FA setup and validation endpoints
5. ✅ API key management endpoints
6. ✅ Rate limiting on authentication endpoints
7. ✅ Account lockout after failed attempts
8. ✅ User profile management endpoints
9. ✅ Admin endpoints for user management
10. ✅ Comprehensive security auditing

### Future Features
- Session management
- Remember me functionality
- Device tracking
- Security notifications
- Passwordless authentication
- Biometric authentication support
- Risk-based authentication
- Single Sign-On (SSO)

## Learning Resources

### Concepts Covered
- **Authentication vs Authorization**: Identity verification vs access control
- **JWT**: Stateless token-based authentication
- **BCrypt**: Adaptive hash function for passwords
- **RBAC**: Role-Based Access Control
- **Multi-Tenancy**: Data isolation per tenant
- **OAuth2**: Delegated authorization protocol
- **2FA**: Two-Factor Authentication for enhanced security

### Code Documentation
Every class, method, and complex logic block includes:
- Purpose and responsibility
- Usage examples
- Architecture explanations
- Security considerations
- Best practices

## Contributors

E-commerce Platform Team

## License

This is a learning project for understanding Spring Boot microservices architecture.

