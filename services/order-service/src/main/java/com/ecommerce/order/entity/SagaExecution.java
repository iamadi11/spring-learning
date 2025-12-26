package com.ecommerce.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Saga Execution Entity
 * 
 * <p>Tracks the state of a saga execution for recovery and monitoring.</p>
 * 
 * <h2>Why Persist Saga State?</h2>
 * <pre>
 * Problems Without Persistence:
 * 1. Application Crash:
 *    - Saga in progress
 *    - Application crashes
 *    - Lost track of saga state
 *    - Can't resume or compensate
 *    - Data inconsistency!
 * 
 * 2. No Visibility:
 *    - Can't see what sagas are running
 *    - Can't debug failures
 *    - No monitoring
 * 
 * Solution: Persist Saga State
 * 
 * Recovery Process:
 * 1. Application starts
 * 2. Load incomplete sagas (IN_PROGRESS, COMPENSATING)
 * 3. Check last completed step
 * 4. Resume from next step
 * 5. Continue execution or compensation
 * 
 * Example:
 * Saga crashed at Step 3:
 * 
 * Database State:
 * {
 *   "sagaId": "saga-123",
 *   "sagaType": "CreateOrderSaga",
 *   "status": "IN_PROGRESS",
 *   "currentStep": 2,
 *   "totalSteps": 4,
 *   "orderId": "order-456"
 * }
 * 
 * On Recovery:
 * 1. Load saga-123
 * 2. See: Step 2 completed, Step 3 in progress
 * 3. Retry Step 3
 * 4. Continue to Step 4
 * 5. Complete saga
 * </pre>
 * 
 * <h2>Saga Execution Lifecycle:</h2>
 * <pre>
 * STARTED → IN_PROGRESS → COMPLETED (Success)
 *                ↓
 *         COMPENSATING → COMPENSATED (Failure, compensated)
 *                ↓
 *            FAILED (Compensation failed)
 * 
 * State Transitions:
 * 
 * STARTED:
 * - Saga created and persisted
 * - Initial state
 * 
 * IN_PROGRESS:
 * - Steps executing
 * - currentStep incremented after each success
 * 
 * COMPLETED:
 * - All steps succeeded
 * - Terminal success state
 * 
 * COMPENSATING:
 * - A step failed
 * - Executing compensations in reverse order
 * 
 * COMPENSATED:
 * - All compensations succeeded
 * - Terminal failure state (but consistent)
 * 
 * FAILED:
 * - Compensation failed
 * - Requires manual intervention
 * - Terminal failure state (inconsistent!)
 * </pre>
 * 
 * <h2>Monitoring and Debugging:</h2>
 * <pre>
 * Queries:
 * 
 * 1. Active Sagas:
 *    SELECT * FROM saga_executions 
 *    WHERE status IN ('STARTED', 'IN_PROGRESS', 'COMPENSATING')
 * 
 * 2. Failed Sagas:
 *    SELECT * FROM saga_executions 
 *    WHERE status = 'FAILED'
 * 
 * 3. Long-Running Sagas:
 *    SELECT * FROM saga_executions 
 *    WHERE status = 'IN_PROGRESS' 
 *    AND created_at < NOW() - INTERVAL '5 minutes'
 * 
 * 4. Saga Success Rate:
 *    SELECT 
 *      saga_type,
 *      COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed,
 *      COUNT(CASE WHEN status = 'COMPENSATED' THEN 1 END) as compensated,
 *      COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed
 *    FROM saga_executions
 *    GROUP BY saga_type
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "saga_executions")
public class SagaExecution {

    /**
     * Saga ID - unique identifier
     */
    @Id
    @Column(name = "saga_id", length = 36)
    private String sagaId;

    /**
     * Saga Type - which saga is executing
     * 
     * <p>Examples:
     * - CreateOrderSaga
     * - CancelOrderSaga
     * - UpdateOrderSaga</p>
     */
    @Column(name = "saga_type", nullable = false, length = 100)
    private String sagaType;

    /**
     * Saga Status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SagaStatus status;

    /**
     * Current Step Index (0-based)
     * 
     * <p>Points to the step currently executing or last completed.</p>
     */
    @Column(name = "current_step", nullable = false)
    private Integer currentStep;

    /**
     * Total Steps in this saga
     */
    @Column(name = "total_steps", nullable = false)
    private Integer totalSteps;

    /**
     * Order ID - the order this saga is processing
     */
    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    /**
     * Payload - saga input data (JSON)
     * 
     * <p>Stores the initial saga request data.
     * Allows replay if needed.</p>
     */
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    /**
     * Error Message - if saga failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Retry Count - number of retries attempted
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Last Error At - timestamp of last error
     */
    @Column(name = "last_error_at")
    private LocalDateTime lastErrorAt;

    /**
     * Creation timestamp
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Completed At timestamp
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // ============= Helper Methods =============

    /**
     * Increment current step
     */
    public void nextStep() {
        this.currentStep++;
    }

    /**
     * Decrement current step (for compensation)
     */
    public void previousStep() {
        this.currentStep--;
    }

    /**
     * Check if saga is complete
     * 
     * @return true if all steps executed
     */
    public boolean isComplete() {
        return currentStep >= totalSteps - 1;
    }

    /**
     * Check if saga can be recovered
     * 
     * @return true if in recoverable state
     */
    public boolean isRecoverable() {
        return status == SagaStatus.IN_PROGRESS || 
               status == SagaStatus.COMPENSATING;
    }

    /**
     * Increment retry count
     */
    public void incrementRetry() {
        this.retryCount++;
        this.lastErrorAt = LocalDateTime.now();
    }

    /**
     * Mark as failed with error
     * 
     * @param errorMessage Error message
     */
    public void markFailed(String errorMessage) {
        this.status = SagaStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark as completed
     */
    public void markCompleted() {
        this.status = SagaStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark as compensated
     */
    public void markCompensated() {
        this.status = SagaStatus.COMPENSATED;
        this.completedAt = LocalDateTime.now();
    }
}

