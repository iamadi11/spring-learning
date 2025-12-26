package com.ecommerce.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration - Spring Security setup for authentication and authorization
 * 
 * <p>This configuration class sets up the security infrastructure for the Auth Service.
 * It configures:</p>
 * <ul>
 *   <li>HTTP security rules (which endpoints require authentication)</li>
 *   <li>Password encoding (BCrypt)</li>
 *   <li>CORS (Cross-Origin Resource Sharing)</li>
 *   <li>Session management (stateless for JWT)</li>
 *   <li>Method-level security (@PreAuthorize, @Secured)</li>
 * </ul>
 * 
 * <h2>Security Architecture:</h2>
 * <pre>
 * Request → SecurityFilterChain → Authentication → Authorization → Controller
 * 
 * 1. Request arrives
 * 2. SecurityFilterChain checks rules
 * 3. If protected endpoint:
 *    - Extract credentials (password or JWT)
 *    - Authenticate user
 *    - Check authorization (roles/permissions)
 * 4. If authorized, proceed to controller
 * 5. If not, return 401 Unauthorized or 403 Forbidden
 * </pre>
 * 
 * <h2>Endpoint Access Rules:</h2>
 * <pre>
 * Public (no authentication required):
 *   - POST /api/auth/register
 *   - POST /api/auth/login
 *   - POST /api/auth/refresh
 *   - GET /oauth2/**
 *   - GET /actuator/health
 * 
 * Protected (authentication required):
 *   - GET /api/auth/me
 *   - POST /api/auth/logout
 *   - All other /api/** endpoints
 * 
 * Admin Only:
 *   - POST /api/tenants
 *   - PUT /api/tenants/**
 *   - DELETE /api/users/**
 * </pre>
 * 
 * <h2>Password Encoding:</h2>
 * <pre>
 * Algorithm: BCrypt
 * Strength: 12 (2^12 = 4,096 iterations)
 * Format: $2a$12$[22-char salt][31-char hash]
 * 
 * Encoding (registration):
 *   String plainPassword = "userPassword123";
 *   String hash = passwordEncoder.encode(plainPassword);
 *   user.setPassword(hash);  // Store hash, never plain text
 * 
 * Validation (login):
 *   boolean matches = passwordEncoder.matches(
 *       plainPassword,  // From login request
 *       user.getPassword()  // Hashed password from database
 *   );
 * </pre>
 * 
 * <h2>Session Management:</h2>
 * <pre>
 * Strategy: STATELESS
 * 
 * Why stateless?
 * - JWT tokens contain all user information
 * - No server-side session storage needed
 * - Horizontally scalable (any server can handle request)
 * - Microservices-friendly
 * 
 * Traditional (Stateful):
 *   Login → Server creates session → SessionID in cookie
 *   Next request → SessionID → Lookup session in memory/Redis
 * 
 * JWT (Stateless):
 *   Login → Server creates JWT token → Token in response
 *   Next request → JWT in header → Validate token signature
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration  // Marks this as a configuration class
@EnableWebSecurity  // Enables Spring Security
@EnableMethodSecurity(prePostEnabled = true)  // Enables @PreAuthorize, @PostAuthorize annotations
public class SecurityConfig {

    /**
     * Configure HTTP security
     * 
     * <p>This method defines which endpoints require authentication and which are public.
     * It also configures CORS, CSRF, and session management.</p>
     * 
     * @param http HttpSecurity builder for configuring web-based security
     * @return SecurityFilterChain that Spring Security will apply
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF (Cross-Site Request Forgery) Protection
            // Disabled for stateless REST API (using JWT instead of cookies)
            // If using cookies, enable CSRF with: .csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrf(csrf -> csrf.disable())
            
            // CORS (Cross-Origin Resource Sharing)
            // Allow requests from different origins (frontend on different domain)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Authorization rules - define which endpoints require authentication
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers(
                    "/api/auth/register",           // User registration
                    "/api/auth/login",              // User login
                    "/api/auth/refresh",            // Refresh token
                    "/api/auth/forgot-password",    // Password reset request
                    "/api/auth/reset-password",     // Password reset with token
                    "/oauth2/**",                   // OAuth2 endpoints for social login
                    "/login/oauth2/**",             // OAuth2 callback URLs
                    "/actuator/health",             // Health check endpoint
                    "/actuator/info",               // Info endpoint
                    "/swagger-ui/**",               // Swagger UI (for development)
                    "/v3/api-docs/**"               // OpenAPI docs (for development)
                ).permitAll()
                
                // Admin-only endpoints - require ADMIN role
                .requestMatchers("/api/tenants/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Session management - stateless for JWT
            // Each request must include valid JWT token in Authorization header
            // Server doesn't maintain session state
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // OAuth2 Login configuration for social login
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/oauth2/authorization")  // Custom login page (optional)
                // After successful OAuth2 authentication, redirect here
                .defaultSuccessUrl("/api/auth/oauth2/success", true)
            );

        return http.build();
    }

    /**
     * Password Encoder Bean - BCrypt password hashing
     * 
     * <p>BCrypt is an adaptive hash function designed for passwords.
     * It automatically handles salt generation and is resistant to
     * brute force attacks due to configurable work factor.</p>
     * 
     * <p><b>Why BCrypt?</b></p>
     * <ul>
     *   <li><b>Adaptive:</b> Can increase iterations as computers get faster</li>
     *   <li><b>Salted:</b> Each password has unique random salt (prevents rainbow table attacks)</li>
     *   <li><b>One-way:</b> Cannot reverse hash to get original password</li>
     *   <li><b>Industry Standard:</b> Widely used and trusted</li>
     * </ul>
     * 
     * <p><b>Strength 12:</b></p>
     * <ul>
     *   <li>2^12 = 4,096 iterations</li>
     *   <li>Balance between security and performance</li>
     *   <li>Takes ~0.1-0.3 seconds to hash (acceptable for login/registration)</li>
     *   <li>Prevents brute force attacks (each attempt takes time)</li>
     * </ul>
     * 
     * <p><b>Usage Example:</b></p>
     * <pre>
     * // Registration - encode plain password
     * @Autowired
     * private PasswordEncoder passwordEncoder;
     * 
     * public void registerUser(String email, String plainPassword) {
     *     User user = new User();
     *     user.setEmail(email);
     *     user.setPassword(passwordEncoder.encode(plainPassword));  // Hash password
     *     userRepository.save(user);
     * }
     * 
     * // Login - verify password
     * public boolean authenticate(String email, String plainPassword) {
     *     User user = userRepository.findByEmail(email).orElseThrow();
     *     return passwordEncoder.matches(plainPassword, user.getPassword());
     * }
     * </pre>
     * 
     * @return BCryptPasswordEncoder with strength 12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with strength 12 (2^12 iterations)
        // Higher strength = more secure but slower
        // 12 is good balance for most applications
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Authentication Manager Bean
     * 
     * <p>The AuthenticationManager is responsible for authenticating credentials.
     * It delegates to AuthenticationProvider(s) to perform actual authentication.</p>
     * 
     * <p><b>Authentication Flow:</b></p>
     * <pre>
     * 1. User submits credentials (email + password)
     * 2. Create Authentication object (UsernamePasswordAuthenticationToken)
     * 3. Pass to AuthenticationManager.authenticate()
     * 4. AuthenticationManager delegates to DaoAuthenticationProvider
     * 5. DaoAuthenticationProvider:
     *    - Loads user from database (via UserDetailsService)
     *    - Compares passwords (via PasswordEncoder)
     * 6. If valid, returns authenticated Authentication object
     * 7. If invalid, throws AuthenticationException
     * </pre>
     * 
     * <p><b>Usage in AuthService:</b></p>
     * <pre>
     * @Autowired
     * private AuthenticationManager authenticationManager;
     * 
     * public TokenResponse login(String email, String password) {
     *     try {
     *         // Create authentication token with credentials
     *         Authentication authentication = authenticationManager.authenticate(
     *             new UsernamePasswordAuthenticationToken(email, password)
     *         );
     *         
     *         // Authentication successful - generate JWT
     *         String jwt = jwtTokenService.generateToken(authentication);
     *         return new TokenResponse(jwt);
     *         
     *     } catch (AuthenticationException e) {
     *         // Authentication failed - invalid credentials
     *         throw new BadCredentialsException("Invalid email or password");
     *     }
     * }
     * </pre>
     * 
     * @param authConfig Authentication configuration
     * @return AuthenticationManager instance
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        // Get the default authentication manager from Spring Security
        // This is pre-configured with DaoAuthenticationProvider
        return authConfig.getAuthenticationManager();
    }

    /**
     * CORS Configuration Source
     * 
     * <p>CORS (Cross-Origin Resource Sharing) allows frontend applications
     * hosted on different domains to make requests to this API.</p>
     * 
     * <p><b>Why CORS?</b></p>
     * <ul>
     *   <li>Frontend: http://localhost:3000 (React app)</li>
     *   <li>Backend: http://localhost:8080 (Spring Boot API)</li>
     *   <li>Different ports = different origins</li>
     *   <li>Browser blocks cross-origin requests by default (security)</li>
     *   <li>CORS configuration tells browser which origins are allowed</li>
     * </ul>
     * 
     * <p><b>CORS Request Flow:</b></p>
     * <pre>
     * 1. Browser sends preflight request (OPTIONS method):
     *    OPTIONS /api/auth/login
     *    Origin: http://localhost:3000
     *    Access-Control-Request-Method: POST
     * 
     * 2. Server responds with allowed origins/methods:
     *    Access-Control-Allow-Origin: http://localhost:3000
     *    Access-Control-Allow-Methods: GET, POST, PUT, DELETE
     *    Access-Control-Allow-Headers: Authorization, Content-Type
     * 
     * 3. Browser allows actual request:
     *    POST /api/auth/login
     *    Origin: http://localhost:3000
     *    Content-Type: application/json
     * </pre>
     * 
     * <p><b>Production Configuration:</b></p>
     * <pre>
     * In production, replace "*" with specific domains:
     * - allowedOrigins: https://yourdomain.com, https://app.yourdomain.com
     * - allowCredentials: true (for cookies/authentication)
     * </pre>
     * 
     * @return CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Create CORS configuration
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins - which domains can make requests
        // "*" allows all origins (development only)
        // In production, specify exact domains: ["https://yourdomain.com"]
        configuration.setAllowedOrigins(Arrays.asList("*"));
        
        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Allowed headers - which headers can be sent in requests
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Exposed headers - which headers can be read by JavaScript
        // Needed for custom headers like X-Total-Count for pagination
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Total-Count", "X-Total-Pages"));
        
        // Allow credentials (cookies, authorization headers)
        // Set to true if using cookies for authentication
        // Note: If true, allowedOrigins cannot be "*", must specify exact domains
        configuration.setAllowCredentials(false);
        
        // Max age for preflight requests (how long browser can cache CORS response)
        // 3600 seconds = 1 hour
        configuration.setMaxAge(3600L);
        
        // Apply CORS configuration to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

