package com.ecommerce.auth.exception;

import com.ecommerce.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler
 * 
 * <p>Centralized exception handling for the Auth Service.
 * Catches exceptions thrown by controllers and services, then returns
 * consistent error responses to clients.</p>
 * 
 * <h2>Why Global Exception Handling?</h2>
 * <pre>
 * Without global handler:
 * - Each controller method needs try-catch blocks
 * - Inconsistent error response formats
 * - Duplicate error handling code
 * - Harder to maintain
 * 
 * With global handler:
 * - Single place for all exception handling
 * - Consistent error response format
 * - Less code in controllers (cleaner)
 * - Easier to maintain and test
 * </pre>
 * 
 * <h2>How it Works:</h2>
 * <pre>
 * 1. Exception thrown anywhere in application:
 *    authService.login() → throws BadCredentialsException
 * 
 * 2. Spring catches exception
 * 
 * 3. Spring looks for matching @ExceptionHandler method
 * 
 * 4. Handler method executes:
 *    - Logs error
 *    - Creates ApiResponse
 *    - Returns ResponseEntity
 * 
 * 5. Client receives consistent error response
 * </pre>
 * 
 * <h2>Exception Handling Strategy:</h2>
 * <pre>
 * Validation Errors (400):
 * - MethodArgumentNotValidException
 * - Bean validation failures
 * - Return field-specific errors
 * 
 * Authentication Errors (401):
 * - BadCredentialsException
 * - UsernameNotFoundException
 * - JWT validation failures
 * - Generic message for security
 * 
 * Authorization Errors (403):
 * - AccessDeniedException
 * - Insufficient permissions
 * 
 * Resource Not Found (404):
 * - ResourceNotFoundException
 * - Entity not found in database
 * 
 * Business Logic Errors (409):
 * - Duplicate email/username
 * - Constraint violations
 * 
 * Server Errors (500):
 * - Unexpected exceptions
 * - Database errors
 * - Log full stack trace
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestControllerAdvice  // Marks this as global exception handler for REST controllers
public class GlobalExceptionHandler {

    // Logger for error tracking and debugging
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle Validation Errors
     * 
     * <p>Catches validation exceptions from @Valid annotation on request DTOs.
     * Returns field-specific error messages.</p>
     * 
     * <p><b>Example Error Response:</b></p>
     * <pre>
     * HTTP 400 Bad Request
     * {
     *   "timestamp": "2024-01-01T10:00:00",
     *   "status": 400,
     *   "message": "Validation failed",
     *   "errors": {
     *     "email": "Email should be valid",
     *     "password": "Password must be at least 8 characters long"
     *   },
     *   "path": "/api/auth/register"
     * }
     * </pre>
     * 
     * @param ex Validation exception
     * @param request HTTP request
     * @return ResponseEntity with validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        // Log validation failure
        logger.warn("Validation failed for request: {}", request.getRequestURI());
        
        // Extract field errors into map
        // Map<field name, error message>
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            // Get field name (e.g., "email", "password")
            String fieldName = ((FieldError) error).getField();
            
            // Get error message (e.g., "Email should be valid")
            String errorMessage = error.getDefaultMessage();
            
            // Add to errors map
            errors.put(fieldName, errorMessage);
        });
        
        // Create custom API response with errors
        ApiResponse<Map<String, String>> response = ApiResponse.error("Validation failed", errors);
        
        // Return 400 Bad Request
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle Authentication Errors
     * 
     * <p>Catches authentication failures (invalid credentials).
     * Returns generic error message for security (don't reveal if email exists).</p>
     * 
     * <p><b>Example Error Response:</b></p>
     * <pre>
     * HTTP 401 Unauthorized
     * {
     *   "timestamp": "2024-01-01T10:00:00",
     *   "status": 401,
     *   "message": "Invalid email or password",
     *   "path": "/api/auth/login"
     * }
     * </pre>
     * 
     * @param ex Authentication exception
     * @param request HTTP request
     * @return ResponseEntity with error message
     */
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {
        
        // Log authentication failure (for security monitoring)
        logger.warn("Authentication failed for request: {} - {}", 
                request.getRequestURI(), ex.getMessage());
        
        // Create generic error response (security best practice)
        // Don't reveal if email exists or which part failed
        ApiResponse<Void> response = ApiResponse.error("Invalid email or password");
        
        // Return 401 Unauthorized
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle Runtime Exceptions
     * 
     * <p>Catches business logic exceptions (duplicate email, etc.).
     * Returns appropriate HTTP status based on exception message.</p>
     * 
     * @param ex Runtime exception
     * @param request HTTP request
     * @return ResponseEntity with error message
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {
        
        // Log error with stack trace
        logger.error("Runtime exception for request: {}", request.getRequestURI(), ex);
        
        // Determine HTTP status based on exception message
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = ex.getMessage();
        
        // Check for specific business logic errors
        if (message != null) {
            if (message.contains("already") || message.contains("exists")) {
                // Duplicate resource (email, username, etc.)
                status = HttpStatus.CONFLICT;  // 409
            } else if (message.contains("not found")) {
                // Resource not found
                status = HttpStatus.NOT_FOUND;  // 404
            } else if (message.contains("Invalid") || message.contains("expired")) {
                // Invalid request or expired token
                status = HttpStatus.BAD_REQUEST;  // 400
            }
        }
        
        // Create error response
        ApiResponse<Void> response = ApiResponse.error(
                message != null ? message : "An error occurred"
        );
        
        // Return ResponseEntity with appropriate status
        return new ResponseEntity<>(response, status);
    }

    /**
     * Handle All Other Exceptions
     * 
     * <p>Catches any unhandled exceptions.
     * Returns 500 Internal Server Error with generic message.</p>
     * 
     * @param ex Any exception
     * @param request HTTP request
     * @return ResponseEntity with error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {
        
        // Log error with full stack trace (for debugging)
        logger.error("Unhandled exception for request: {}", request.getRequestURI(), ex);
        
        // Create generic error response (don't expose internal details)
        ApiResponse<Void> response = ApiResponse.error(
                "An unexpected error occurred. Please try again later."
        );
        
        // Return 500 Internal Server Error
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

