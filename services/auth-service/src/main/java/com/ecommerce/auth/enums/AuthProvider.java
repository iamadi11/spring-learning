package com.ecommerce.auth.enums;

/**
 * Authentication Provider Enum
 * 
 * <p>Represents the different authentication providers supported by the system.
 * This enum is used to track how a user registered and authenticated with the platform.</p>
 * 
 * <h2>Provider Types:</h2>
 * <pre>
 * LOCAL:
 * - Traditional email/password registration
 * - Credentials stored in our database
 * - Password hashed with BCrypt
 * - User flow:
 *   1. User fills registration form (email, password, name)
 *   2. Server validates and creates account
 *   3. Email verification sent
 *   4. User confirms email
 *   5. Account activated
 * 
 * GOOGLE:
 * - OAuth2 authentication via Google
 * - No password stored in our system
 * - User data from Google profile
 * - User flow:
 *   1. User clicks "Sign in with Google"
 *   2. Redirected to Google login
 *   3. User grants permissions
 *   4. Google returns user info
 *   5. Account created/linked automatically
 * 
 * GITHUB:
 * - OAuth2 authentication via GitHub
 * - Common for developer platforms
 * - Access to GitHub profile data
 * - Similar flow to Google OAuth
 * 
 * FACEBOOK:
 * - OAuth2 authentication via Facebook
 * - Access to Facebook profile
 * - Large user base, convenient login
 * 
 * TWITTER:
 * - OAuth 1.0a or OAuth2 authentication
 * - Twitter profile integration
 * - Useful for social platforms
 * </pre>
 * 
 * <h2>OAuth2 Flow (Google Example):</h2>
 * <pre>
 * 1. User clicks "Sign in with Google":
 *    Frontend → /oauth2/authorization/google → Backend
 * 
 * 2. Backend redirects to Google:
 *    Backend → https://accounts.google.com/o/oauth2/auth?
 *              client_id=...&
 *              redirect_uri=http://localhost:8081/login/oauth2/code/google&
 *              scope=profile email&
 *              response_type=code
 * 
 * 3. User logs in to Google and grants permissions
 * 
 * 4. Google redirects back with authorization code:
 *    Google → http://localhost:8081/login/oauth2/code/google?code=AUTH_CODE
 * 
 * 5. Backend exchanges code for access token:
 *    Backend → [POST] https://oauth2.googleapis.com/token
 *              code=AUTH_CODE&
 *              client_id=...&
 *              client_secret=...&
 *              redirect_uri=...&
 *              grant_type=authorization_code
 * 
 * 6. Google returns access token:
 *    Response: {
 *      "access_token": "ya29.a0AfH6S...",
 *      "expires_in": 3599,
 *      "scope": "https://www.googleapis.com/auth/userinfo.profile ...",
 *      "token_type": "Bearer"
 *    }
 * 
 * 7. Backend fetches user profile:
 *    Backend → [GET] https://www.googleapis.com/oauth2/v2/userinfo
 *              Authorization: Bearer ya29.a0AfH6S...
 * 
 * 8. Google returns user info:
 *    Response: {
 *      "id": "1234567890",
 *      "email": "user@gmail.com",
 *      "verified_email": true,
 *      "name": "John Doe",
 *      "picture": "https://lh3.googleusercontent.com/..."
 *    }
 * 
 * 9. Backend creates/updates user:
 *    User user = userRepository.findByEmail(email).orElse(new User());
 *    user.setAuthProvider(AuthProvider.GOOGLE);
 *    user.setProviderId(googleUserId);
 *    user.setEmailVerified(true);  // Google already verified
 *    userRepository.save(user);
 * 
 * 10. Backend generates JWT and returns to frontend:
 *     Response: {
 *       "accessToken": "eyJhbGciOi...",
 *       "refreshToken": "eyJhbGciOi...",
 *       "user": { ... }
 *     }
 * </pre>
 * 
 * <h2>Handling Multiple Providers:</h2>
 * <pre>
 * Scenario: User has LOCAL account, then tries Google OAuth with same email
 * 
 * Option 1: Merge accounts
 * - Link Google account to existing user
 * - User can login with either method
 * - Requires confirmation from user
 * 
 * Option 2: Separate accounts
 * - Treat as different users
 * - Require unique email per provider
 * - Simpler but less user-friendly
 * 
 * Option 3: Block duplicate
 * - If email exists with different provider, reject
 * - Force user to use original method
 * - Secure but inconvenient
 * 
 * Our implementation (Option 1 - Merge):
 * if (userRepository.existsByEmail(email)) {
 *     User existingUser = userRepository.findByEmail(email).get();
 *     if (existingUser.getAuthProvider() == AuthProvider.LOCAL) {
 *         // Ask user to confirm account merge
 *         // Update authProvider to GOOGLE
 *         // Store Google providerId
 *     }
 * }
 * </pre>
 * 
 * <h2>Security Considerations:</h2>
 * <ul>
 *   <li><b>OAuth tokens:</b> Never store OAuth access tokens, use them once to fetch profile</li>
 *   <li><b>Provider ID:</b> Store OAuth provider's user ID for account linking</li>
 *   <li><b>Email verification:</b> OAuth users' emails are pre-verified by provider</li>
 *   <li><b>Password:</b> OAuth users don't have password in our system</li>
 *   <li><b>Profile updates:</b> Periodically refresh profile data from OAuth provider</li>
 * </ul>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public enum AuthProvider {
    
    /**
     * Local authentication - email/password stored in our database
     * 
     * <p>User registered directly with our platform using email and password.
     * We are responsible for password security, reset flows, and email verification.</p>
     */
    LOCAL,
    
    /**
     * Google OAuth2 authentication
     * 
     * <p>User authenticated via Google account. Email is pre-verified by Google.
     * No password stored in our system. User profile data from Google.</p>
     */
    GOOGLE,
    
    /**
     * GitHub OAuth2 authentication
     * 
     * <p>User authenticated via GitHub account. Common for developer tools and platforms.
     * Access to GitHub profile and (if requested) repository information.</p>
     */
    GITHUB,
    
    /**
     * Facebook OAuth2 authentication
     * 
     * <p>User authenticated via Facebook account. Large user base makes it convenient.
     * Access to Facebook profile data based on granted permissions.</p>
     */
    FACEBOOK,
    
    /**
     * Twitter OAuth authentication
     * 
     * <p>User authenticated via Twitter account. Supports OAuth 1.0a and OAuth 2.0.
     * Useful for social platforms and real-time communication features.</p>
     */
    TWITTER
}

