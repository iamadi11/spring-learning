package com.ecommerce.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Register Request DTO
 * 
 * <p>Data Transfer Object for user registration requests.
 * Contains all necessary information to create a new user account.</p>
 * 
 * <h2>Validation Rules:</h2>
 * <pre>
 * Username:
 * - Required (not blank)
 * - 3-50 characters
 * - Alphanumeric + underscore/hyphen recommended
 * - Unique within tenant
 * 
 * Email:
 * - Required (not blank)
 * - Valid email format
 * - Unique across all users
 * - Case-insensitive
 * 
 * Password:
 * - Required (not blank)
 * - Minimum 8 characters
 * - Should include: uppercase, lowercase, number, special char (checked in service)
 * - Never stored in plain text
 * - Hashed with BCrypt before saving
 * 
 * First/Last Name:
 * - Optional
 * - Maximum 50 characters each
 * 
 * Tenant ID:
 * - Required for multi-tenant applications
 * - References existing tenant
 * - Defaults to "default" if not provided
 * </pre>
 * 
 * <h2>Example Request:</h2>
 * <pre>
 * POST /api/auth/register
 * Content-Type: application/json
 * 
 * {
 *   "username": "johndoe",
 *   "email": "john.doe@example.com",
 *   "password": "SecurePass123!",
 *   "firstName": "John",
 *   "lastName": "Doe",
 *   "tenantId": "tenant1"
 * }
 * </pre>
 * 
 * <h2>Registration Flow:</h2>
 * <pre>
 * 1. Client sends RegisterRequest
 * 2. Controller validates request (@Valid annotation)
 * 3. If validation fails:
 *    - Return 400 Bad Request
 *    - Include validation error messages
 * 4. If validation passes:
 *    - Pass to AuthService
 * 5. Service checks:
 *    - Email uniqueness
 *    - Username uniqueness (within tenant)
 *    - Password strength
 *    - Tenant existence
 * 6. If checks pass:
 *    - Hash password with BCrypt
 *    - Create User entity
 *    - Save to database
 *    - Generate verification token
 *    - Send verification email
 * 7. Return success response with user details
 * </pre>
 * 
 * <h2>Validation Error Response:</h2>
 * <pre>
 * HTTP 400 Bad Request
 * {
 *   "timestamp": "2024-01-01T10:00:00",
 *   "status": 400,
 *   "message": "Validation failed",
 *   "errors": {
 *     "email": "Email should be valid",
 *     "password": "Password must be at least 8 characters long",
 *     "username": "Username must be between 3 and 50 characters"
 *   }
 * }
 * </pre>
 * 
 * <h2>Service Validation Errors:</h2>
 * <pre>
 * HTTP 409 Conflict
 * {
 *   "timestamp": "2024-01-01T10:00:00",
 *   "status": 409,
 *   "message": "Email already registered",
 *   "path": "/api/auth/register"
 * }
 * 
 * HTTP 404 Not Found
 * {
 *   "timestamp": "2024-01-01T10:00:00",
 *   "status": 404,
 *   "message": "Tenant not found: tenant1",
 *   "path": "/api/auth/register"
 * }
 * </pre>
 * 
 * <h2>Success Response:</h2>
 * <pre>
 * HTTP 201 Created
 * {
 *   "timestamp": "2024-01-01T10:00:00",
 *   "status": 201,
 *   "message": "Registration successful. Please check your email to verify your account.",
 *   "data": {
 *     "id": 123,
 *     "username": "johndoe",
 *     "email": "john.doe@example.com",
 *     "firstName": "John",
 *     "lastName": "Doe",
 *     "status": "PENDING",
 *     "emailVerified": false,
 *     "tenantId": "tenant1"
 *   }
 * }
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data  // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: generates no-argument constructor
@AllArgsConstructor  // Lombok: generates all-argument constructor
public class RegisterRequest {

    /**
     * Username for the new account
     * 
     * <p><b>Constraints:</b></p>
     * <ul>
     *   <li>Required: Cannot be blank</li>
     *   <li>Length: 3-50 characters</li>
     *   <li>Uniqueness: Within tenant scope</li>
     * </ul>
     * 
     * <p><b>Recommendations:</b></p>
     * <ul>
     *   <li>Alphanumeric characters (a-z, A-Z, 0-9)</li>
     *   <li>Underscores and hyphens allowed</li>
     *   <li>No spaces or special characters</li>
     *   <li>Case-sensitive but stored as-is</li>
     * </ul>
     */
    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    /**
     * Email address for the new account
     * 
     * <p><b>Constraints:</b></p>
     * <ul>
     *   <li>Required: Cannot be blank</li>
     *   <li>Format: Valid email format (user@domain.com)</li>
     *   <li>Uniqueness: Globally unique across all tenants</li>
     * </ul>
     * 
     * <p><b>Processing:</b></p>
     * <ul>
     *   <li>Stored in lowercase</li>
     *   <li>Verified before account activation</li>
     *   <li>Used as primary identifier for login</li>
     *   <li>Cannot be changed after registration (in most cases)</li>
     * </ul>
     */
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email should be valid")
    private String email;

    /**
     * Password for the new account
     * 
     * <p><b>Constraints:</b></p>
     * <ul>
     *   <li>Required: Cannot be blank</li>
     *   <li>Length: Minimum 8 characters</li>
     *   <li>Complexity: Checked in service layer</li>
     * </ul>
     * 
     * <p><b>Strength Requirements (Service Layer):</b></p>
     * <ul>
     *   <li>At least one uppercase letter (A-Z)</li>
     *   <li>At least one lowercase letter (a-z)</li>
     *   <li>At least one digit (0-9)</li>
     *   <li>At least one special character (!@#$%^&*)</li>
     * </ul>
     * 
     * <p><b>Security:</b></p>
     * <ul>
     *   <li>Never logged</li>
     *   <li>Hashed with BCrypt before storage</li>
     *   <li>Original password never stored</li>
     *   <li>Cannot be retrieved (only reset)</li>
     * </ul>
     */
    @NotBlank(message = "Password cannot be empty")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    /**
     * User's first name (optional)
     * 
     * <p><b>Usage:</b></p>
     * <ul>
     *   <li>Personalization (Welcome, John!)</li>
     *   <li>Display name in UI</li>
     *   <li>Email salutation</li>
     * </ul>
     */
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;

    /**
     * User's last name (optional)
     * 
     * <p><b>Usage:</b></p>
     * <ul>
     *   <li>Full name display</li>
     *   <li>Formal communications</li>
     *   <li>Shipping/billing information</li>
     * </ul>
     */
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;

    /**
     * Tenant identifier for multi-tenancy
     * 
     * <p><b>Multi-tenancy:</b></p>
     * <ul>
     *   <li>Each tenant has isolated data</li>
     *   <li>Username must be unique within tenant</li>
     *   <li>Email must be unique globally</li>
     *   <li>Tenant must exist before registration</li>
     * </ul>
     * 
     * <p><b>Examples:</b></p>
     * <ul>
     *   <li>SaaS: Different companies (tenant1, tenant2)</li>
     *   <li>E-commerce: Different stores (store-a, store-b)</li>
     *   <li>Single tenant: Always "default"</li>
     * </ul>
     */
    @NotBlank(message = "Tenant ID cannot be empty")
    private String tenantId = "default";
}

