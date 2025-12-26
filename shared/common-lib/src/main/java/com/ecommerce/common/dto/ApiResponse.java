package com.ecommerce.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard API Response Wrapper
 * 
 * <p>This class provides a consistent response structure for all API endpoints
 * across all microservices. It wraps the actual data with metadata about the
 * request execution.</p>
 * 
 * <h2>Why Standard Response Format?</h2>
 * <ul>
 *   <li><b>Consistency:</b> All APIs return the same response structure</li>
 *   <li><b>Client Simplicity:</b> Clients can parse responses uniformly</li>
 *   <li><b>Metadata:</b> Include additional information (timestamp, status, message)</li>
 *   <li><b>Error Handling:</b> Consistent error response format</li>
 * </ul>
 * 
 * <h2>Response Structure:</h2>
 * <pre>
 * {
 *   "success": true,
 *   "message": "User retrieved successfully",
 *   "data": {
 *     "id": "123",
 *     "name": "John Doe",
 *     "email": "john@example.com"
 *   },
 *   "timestamp": "2024-01-01T12:00:00"
 * }
 * </pre>
 * 
 * <h2>Usage Examples:</h2>
 * <pre>
 * // Success response with data
 * return ApiResponse.success("User created successfully", user);
 * 
 * // Success response without data
 * return ApiResponse.success("Operation completed successfully");
 * 
 * // Error response
 * return ApiResponse.error("User not found");
 * </pre>
 * 
 * @param <T> Type of the data being returned
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data  // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: generates no-args constructor
@AllArgsConstructor  // Lombok: generates all-args constructor
public class ApiResponse<T> {
    
    /**
     * Indicates whether the request was successful
     * true = success, false = error
     */
    private boolean success;
    
    /**
     * Human-readable message describing the result
     * Examples: "User created successfully", "Invalid email format"
     */
    private String message;
    
    /**
     * The actual data being returned
     * Can be a single object, list, or null for operations without return data
     * Generic type T allows flexibility for different data types
     */
    private T data;
    
    /**
     * Timestamp when the response was generated
     * Useful for tracking, debugging, and auditing
     */
    private LocalDateTime timestamp;
    
    /**
     * Create a successful response with data
     * 
     * <p>Use this when operation succeeds and you have data to return.</p>
     * 
     * @param message Success message
     * @param data Data to return
     * @param <T> Type of data
     * @return ApiResponse with success=true and data
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
            true,  // success
            message,  // message
            data,  // data
            LocalDateTime.now()  // current timestamp
        );
    }
    
    /**
     * Create a successful response without data
     * 
     * <p>Use this when operation succeeds but there's no data to return.
     * Example: DELETE operations, void methods.</p>
     * 
     * @param message Success message
     * @param <T> Type of data (will be null)
     * @return ApiResponse with success=true and no data
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(
            true,  // success
            message,  // message
            null,  // no data
            LocalDateTime.now()  // current timestamp
        );
    }
    
    /**
     * Create an error response
     * 
     * <p>Use this when operation fails due to validation, business logic,
     * or any other error condition.</p>
     * 
     * @param message Error message describing what went wrong
     * @param <T> Type of data (will be null)
     * @return ApiResponse with success=false and error message
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(
            false,  // failed
            message,  // error message
            null,  // no data
            LocalDateTime.now()  // current timestamp
        );
    }
    
    /**
     * Create an error response with error data
     * 
     * <p>Use this when you want to include additional error details
     * like validation errors, field-specific messages, etc.</p>
     * 
     * @param message Error message
     * @param errorData Additional error details
     * @param <T> Type of error data
     * @return ApiResponse with success=false and error data
     */
    public static <T> ApiResponse<T> error(String message, T errorData) {
        return new ApiResponse<>(
            false,  // failed
            message,  // error message
            errorData,  // error details
            LocalDateTime.now()  // current timestamp
        );
    }
}

