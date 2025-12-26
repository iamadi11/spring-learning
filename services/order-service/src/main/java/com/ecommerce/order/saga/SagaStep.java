package com.ecommerce.order.saga;

/**
 * Saga Step Interface
 * 
 * <p>Defines the contract for a single step in a saga.
 * Each step must implement forward execution and compensation.</p>
 * 
 * <h2>Saga Step Pattern:</h2>
 * <pre>
 * Each step has two operations:
 * 
 * 1. execute(): Forward action
 *    - Performs the business operation
 *    - Returns success or throws exception
 *    - Example: Reserve inventory, Process payment
 * 
 * 2. compensate(): Reverse action
 *    - Undoes the effect of execute()
 *    - Called when saga fails
 *    - Example: Release inventory, Refund payment
 * 
 * Important Properties:
 * 
 * 1. Idempotent:
 *    - Safe to execute multiple times
 *    - Same result regardless of retries
 *    - Example: Setting status to "CANCELLED" (already cancelled = OK)
 * 
 * 2. Reversible:
 *    - Compensation undoes execute()
 *    - Semantic undo, not database rollback
 *    - Example: Reserve inventory ↔ Release inventory
 * 
 * 3. Retriable:
 *    - Can retry on transient failures
 *    - Network timeout → retry
 *    - Database lock → retry
 * </pre>
 * 
 * <h2>Implementation Example:</h2>
 * <pre>
 * public class ReserveInventoryStep implements SagaStep<CreateOrderContext> {
 *     
 *     private final ProductServiceClient productClient;
 *     
 *     @Override
 *     public void execute(CreateOrderContext context) throws Exception {
 *         // Forward action: Reserve inventory
 *         for (OrderItem item : context.getOrder().getItems()) {
 *             ReserveRequest request = ReserveRequest.builder()
 *                 .productId(item.getProductId())
 *                 .quantity(item.getQuantity())
 *                 .orderId(context.getOrder().getOrderId())
 *                 .build();
 *             
 *             productClient.reserveInventory(request);
 *         }
 *         
 *         log.info("Inventory reserved for order: {}", context.getOrderId());
 *     }
 *     
 *     @Override
 *     public void compensate(CreateOrderContext context) throws Exception {
 *         // Reverse action: Release inventory
 *         for (OrderItem item : context.getOrder().getItems()) {
 *             ReleaseRequest request = ReleaseRequest.builder()
 *                 .productId(item.getProductId())
 *                 .quantity(item.getQuantity())
 *                 .orderId(context.getOrder().getOrderId())
 *                 .build();
 *             
 *             productClient.releaseInventory(request);
 *         }
 *         
 *         log.info("Inventory released for order: {}", context.getOrderId());
 *     }
 *     
 *     @Override
 *     public String getStepName() {
 *         return "ReserveInventory";
 *     }
 * }
 * </pre>
 * 
 * <h2>Error Handling:</h2>
 * <pre>
 * Execute Fails:
 * - Throw exception
 * - Saga orchestrator catches
 * - Starts compensation
 * 
 * Compensate Fails:
 * - Retry with exponential backoff
 * - If max retries exceeded → alert ops team
 * - Mark saga as FAILED
 * - Requires manual intervention
 * 
 * Idempotency Example:
 * 
 * Execute called 3 times (due to retries):
 * 1. Reserve 10 items → Success (inventory: 100 → 90)
 * 2. Reserve 10 items → Already reserved (inventory: 90) → Success
 * 3. Reserve 10 items → Already reserved (inventory: 90) → Success
 * 
 * Result: Inventory correctly at 90, not 70!
 * 
 * How? Use order ID as reservation ID:
 * - First call: Create reservation with order ID
 * - Retry: Check if reservation exists with order ID
 * - If exists: Return success without changing inventory
 * </pre>
 * 
 * @param <T> Saga context type (carries data between steps)
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface SagaStep<T> {

    /**
     * Execute the forward action of this step
     * 
     * <p>Performs the business operation for this step.
     * Must be idempotent (safe to retry).</p>
     * 
     * @param context Saga context containing shared data
     * @throws Exception if step execution fails
     */
    void execute(T context) throws Exception;

    /**
     * Compensate (undo) this step
     * 
     * <p>Reverses the effect of execute().
     * Called when saga fails to maintain consistency.
     * Must be idempotent (safe to retry).</p>
     * 
     * @param context Saga context containing shared data
     * @throws Exception if compensation fails
     */
    void compensate(T context) throws Exception;

    /**
     * Get step name for logging and monitoring
     * 
     * @return Step name (e.g., "ReserveInventory", "ProcessPayment")
     */
    String getStepName();
}

