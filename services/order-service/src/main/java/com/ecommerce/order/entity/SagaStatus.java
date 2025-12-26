package com.ecommerce.order.entity;

/**
 * Saga Status Enum
 * 
 * <p>Tracks the execution state of a saga.</p>
 * 
 * @author E-commerce Platform Team
 */
public enum SagaStatus {
    /**
     * STARTED - Saga has been initiated
     */
    STARTED,
    
    /**
     * IN_PROGRESS - Saga steps are executing
     */
    IN_PROGRESS,
    
    /**
     * COMPENSATING - Saga failed, executing compensation
     */
    COMPENSATING,
    
    /**
     * COMPENSATED - Compensation completed successfully
     */
    COMPENSATED,
    
    /**
     * COMPLETED - Saga completed successfully
     */
    COMPLETED,
    
    /**
     * FAILED - Saga failed (compensation also failed)
     */
    FAILED
}

