package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.repository.RoleRepository;
import com.ecommerce.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Tests for Auth Controller
 * 
 * <p>Tests REST API endpoints with real database (Testcontainers)
 * and full Spring context.</p>
 * 
 * <h2>Integration Testing Strategy:</h2>
 * <pre>
 * Unit Tests:
 * - Test individual components in isolation
 * - Mock dependencies
 * - Fast execution
 * - Example: JwtTokenServiceTest
 * 
 * Integration Tests:
 * - Test components working together
 * - Real dependencies (database, etc.)
 * - Slower execution
 * - Example: This test class
 * 
 * End-to-End Tests:
 * - Test entire system from user perspective
 * - Real services, real databases
 * - Slowest execution
 * - Example: Selenium tests
 * </pre>
 * 
 * <h2>Testcontainers:</h2>
 * <pre>
 * What is Testcontainers?
 * - Library for running Docker containers in tests
 * - Provides real database for integration tests
 * - Automatically starts and stops containers
 * - Ensures test isolation and repeatability
 * 
 * Benefits:
 * - Test against real database (PostgreSQL, not H2)
 * - No need to install database locally
 * - Each test run gets fresh database
 * - Catches database-specific issues
 * 
 * How it works:
 * 1. @Testcontainers enables Testcontainers
 * 2. @Container defines container to start
 * 3. Container starts before first test
 * 4. Tests run against container
 * 5. Container stops after last test
 * </pre>
 * 
 * <h2>Test Scenarios:</h2>
 * <ul>
 *   <li>Successful registration</li>
 *   <li>Registration with validation errors</li>
 *   <li>Registration with duplicate email</li>
 *   <li>Successful login</li>
 *   <li>Login with invalid credentials</li>
 *   <li>Token refresh</li>
 *   <li>Logout</li>
 * </ul>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootTest  // Load full Spring application context
@AutoConfigureMockMvc  // Configure MockMvc for testing controllers
@Testcontainers  // Enable Testcontainers support
@DisplayName("Auth Controller Integration Tests")
class AuthControllerIntegrationTest {

    /**
     * PostgreSQL Testcontainer
     * 
     * <p>Docker container with PostgreSQL database for testing.
     * Automatically started before tests and stopped after.</p>
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("auth_test_db")
            .withUsername("test")
            .withPassword("test");

    /**
     * Dynamic Property Source
     * 
     * <p>Configures Spring to use Testcontainers database.
     * Replaces application.yml database properties with container values.</p>
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Override database connection properties to use Testcontainers
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // MockMvc for testing REST endpoints
    @Autowired
    private MockMvc mockMvc;

    // ObjectMapper for JSON serialization/deserialization
    @Autowired
    private ObjectMapper objectMapper;

    // Repositories for test data setup and cleanup
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    /**
     * Setup before each test
     * 
     * <p>Ensures USER role exists for registration tests.</p>
     */
    @BeforeEach
    void setUp() {
        // Clean up database before each test
        userRepository.deleteAll();
        
        // Ensure USER role exists (required for registration)
        if (!roleRepository.existsByName("USER")) {
            Role userRole = Role.builder()
                    .name("USER")
                    .description("Regular user")
                    .build();
            roleRepository.save(userRole);
        }
    }

    /**
     * Test: Successful Registration
     * 
     * <p>Verifies that valid registration request creates user and returns 201.</p>
     */
    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUserSuccessfully() throws Exception {
        // Given: Valid registration request
        RegisterRequest request = new RegisterRequest();
        request.setUsername("johndoe");
        request.setEmail("john@example.com");
        request.setPassword("SecurePass123!");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setTenantId("default");
        
        // When/Then: POST to /api/auth/register
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Expect: 201 Created
                .andExpect(status().isCreated())
                // Expect: Response contains user data
                .andExpect(jsonPath("$.data.username").value("johndoe"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    /**
     * Test: Registration with Validation Errors
     * 
     * <p>Verifies that invalid registration request returns 400 with error details.</p>
     */
    @Test
    @DisplayName("Should fail registration with validation errors")
    void shouldFailRegistrationWithValidationErrors() throws Exception {
        // Given: Invalid registration request (missing required fields)
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ab");  // Too short (min 3)
        request.setEmail("invalid-email");  // Invalid format
        request.setPassword("short");  // Too short (min 8)
        request.setTenantId("default");
        
        // When/Then: POST to /api/auth/register
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Expect: 400 Bad Request
                .andExpect(status().isBadRequest())
                // Expect: Response contains validation errors
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.username").exists())
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.password").exists());
    }

    /**
     * Test: Registration with Duplicate Email
     * 
     * <p>Verifies that registering with existing email returns 409 Conflict.</p>
     */
    @Test
    @DisplayName("Should fail registration with duplicate email")
    void shouldFailRegistrationWithDuplicateEmail() throws Exception {
        // Given: Register first user
        RegisterRequest firstRequest = new RegisterRequest();
        firstRequest.setUsername("user1");
        firstRequest.setEmail("duplicate@example.com");
        firstRequest.setPassword("SecurePass123!");
        firstRequest.setTenantId("default");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)));
        
        // When/Then: Try to register with same email
        RegisterRequest duplicateRequest = new RegisterRequest();
        duplicateRequest.setUsername("user2");
        duplicateRequest.setEmail("duplicate@example.com");  // Same email
        duplicateRequest.setPassword("SecurePass123!");
        duplicateRequest.setTenantId("default");
        
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                // Expect: 409 Conflict
                .andExpect(status().isConflict())
                // Expect: Error message about duplicate email
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    /**
     * Test: Successful Login
     * 
     * <p>Verifies that valid login returns tokens.</p>
     */
    @Test
    @DisplayName("Should login successfully")
    void shouldLoginSuccessfully() throws Exception {
        // Given: Register user first
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("logintest");
        registerRequest.setEmail("login@example.com");
        registerRequest.setPassword("SecurePass123!");
        registerRequest.setTenantId("default");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));
        
        // When/Then: Login with credentials
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("login@example.com");
        loginRequest.setPassword("SecurePass123!");
        
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                // Expect: 200 OK
                .andExpect(status().isOk())
                // Expect: Response contains tokens
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").exists());
    }

    /**
     * Test: Login with Invalid Credentials
     * 
     * <p>Verifies that login with wrong password returns 401.</p>
     */
    @Test
    @DisplayName("Should fail login with invalid credentials")
    void shouldFailLoginWithInvalidCredentials() throws Exception {
        // Given: Register user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("logintest2");
        registerRequest.setEmail("login2@example.com");
        registerRequest.setPassword("SecurePass123!");
        registerRequest.setTenantId("default");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));
        
        // When/Then: Login with wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("login2@example.com");
        loginRequest.setPassword("WrongPassword!");
        
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                // Expect: 401 Unauthorized
                .andExpect(status().isUnauthorized())
                // Expect: Generic error message
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}

