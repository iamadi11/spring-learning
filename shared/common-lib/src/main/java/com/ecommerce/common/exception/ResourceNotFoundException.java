package com.ecommerce.common.exception;

/**
 * Resource Not Found Exception
 * 
 * <p>This exception is thrown when a requested resource (user, product, order, etc.)
 * cannot be found in the database. It typically results in HTTP 404 Not Found response.</p>
 * 
 * <h2>When to Use:</h2>
 * <ul>
 *   <li>User with ID not found</li>
 *   <li>Product with ID not found</li>
 *   <li>Order does not exist</li>
 *   <li>Any "Get by ID" operation that returns no results</li>
 * </ul>
 * 
 * <h2>Usage Example:</h2>
 * <pre>
 * public User getUserById(String userId) {
 *     return userRepository.findById(userId)
 *         .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
 * }
 * </pre>
 * 
 * <h2>HTTP Response:</h2>
 * <pre>
 * Status: 404 Not Found
 * Body:
 * {
 *   "success": false,
 *   "message": "User not found with ID: 123",
 *   "data": null,
 *   "timestamp": "2024-01-01T12:00:00"
 * }
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class ResourceNotFoundException extends RuntimeException {
    
    /**
     * Default constructor
     */
    public ResourceNotFoundException() {
        super();
    }
    
    /**
     * Constructor with message
     * 
     * @param message Description of what resource was not found
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    /**
     * Constructor with message and cause
     * 
     * @param message Description of what resource was not found
     * @param cause Original exception that caused this
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

