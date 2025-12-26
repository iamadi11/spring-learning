package com.ecommerce.order.saga;

import java.util.List;

/**
 * Saga Abstract Class
 * 
 * <p>Base class for all saga implementations.
 * Defines the structure of a saga as a sequence of steps.</p>
 * 
 * <h2>Saga Structure:</h2>
 * <pre>
 * A Saga is a sequence of steps:
 * 
 * Saga = Step1 → Step2 → Step3 → Step4
 * 
 * Each step can succeed or fail:
 * 
 * Success Path:
 * Step1 ✓ → Step2 ✓ → Step3 ✓ → Step4 ✓ → COMPLETED
 * 
 * Failure Path (Step 3 fails):
 * Step1 ✓ → Step2 ✓ → Step3 ✗ → Compensate:
 * Step2' ✓ → Step1' ✓ → COMPENSATED
 * 
 * Note: Don't compensate Step 3 (it didn't complete)
 * Only compensate successfully completed steps
 * </pre>
 * 
 * <h2>Creating a Saga:</h2>
 * <pre>
 * public class CreateOrderSaga extends Saga<CreateOrderContext> {
 *     
 *     private final CreateOrderStep createOrderStep;
 *     private final ReserveInventoryStep reserveInventoryStep;
 *     private final ProcessPaymentStep processPaymentStep;
 *     private final ConfirmOrderStep confirmOrderStep;
 *     
 *     @Override
 *     public List<SagaStep<CreateOrderContext>> getSteps() {
 *         return List.of(
 *             createOrderStep,
 *             reserveInventoryStep,
 *             processPaymentStep,
 *             confirmOrderStep
 *         );
 *     }
 *     
 *     @Override
 *     public String getSagaName() {
 *         return "CreateOrderSaga";
 *     }
 * }
 * </pre>
 * 
 * <h2>Saga Context:</h2>
 * <pre>
 * Context carries data between steps:
 * 
 * public class CreateOrderContext {
 *     private Order order;
 *     private List<String> reservationIds;
 *     private String paymentTransactionId;
 *     
 *     // Getters and setters...
 * }
 * 
 * Usage in Steps:
 * 
 * Step 1: Creates order, stores in context
 * Step 2: Reads order from context, reserves inventory, stores reservationIds
 * Step 3: Reads order and reservationIds, processes payment
 * Step 4: Reads all data, confirms order
 * </pre>
 * 
 * @param <T> Saga context type
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public abstract class Saga<T> {

    /**
     * Get the list of steps in this saga
     * 
     * <p>Steps are executed in order.
     * Compensation executes in reverse order.</p>
     * 
     * @return Ordered list of saga steps
     */
    public abstract List<SagaStep<T>> getSteps();

    /**
     * Get saga name for logging and monitoring
     * 
     * @return Saga name (e.g., "CreateOrderSaga")
     */
    public abstract String getSagaName();

    /**
     * Get total number of steps
     * 
     * @return Number of steps in this saga
     */
    public int getTotalSteps() {
        return getSteps().size();
    }

    /**
     * Get specific step by index
     * 
     * @param index Step index (0-based)
     * @return Saga step at index
     */
    public SagaStep<T> getStep(int index) {
        List<SagaStep<T>> steps = getSteps();
        if (index < 0 || index >= steps.size()) {
            throw new IllegalArgumentException("Invalid step index: " + index);
        }
        return steps.get(index);
    }
}

