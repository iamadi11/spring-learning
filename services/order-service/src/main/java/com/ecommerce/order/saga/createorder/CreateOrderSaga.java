package com.ecommerce.order.saga.createorder;

import com.ecommerce.order.saga.Saga;
import com.ecommerce.order.saga.SagaStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Create Order Saga
 * 
 * <p>Orchestrates the process of creating an order across multiple services.</p>
 * 
 * <h2>Saga Steps:</h2>
 * <pre>
 * Step 1: CreateOrder
 * - Create order entity (PENDING)
 * - Calculate totals
 * - Save to database
 * 
 * Step 2: ReserveInventory
 * - Call Product Service
 * - Reserve stock for each item
 * - Store reservation IDs
 * 
 * Step 3: ProcessPayment
 * - Call Payment Service
 * - Charge customer
 * - Store transaction ID
 * 
 * Step 4: ConfirmOrder
 * - Update order status (CONFIRMED)
 * - Order ready for fulfillment
 * </pre>
 * 
 * <h2>Success Flow:</h2>
 * <pre>
 * CreateOrder ✓ → ReserveInventory ✓ → ProcessPayment ✓ → ConfirmOrder ✓
 * Result: Order CONFIRMED, ready to ship
 * </pre>
 * 
 * <h2>Failure Flow (Payment fails):</h2>
 * <pre>
 * CreateOrder ✓ → ReserveInventory ✓ → ProcessPayment ✗
 * 
 * Compensation:
 * ReleaseInventory ✓ → CancelOrder ✓
 * 
 * Result: Order CANCELLED, inventory released, no charge
 * </pre>
 * 
 * <h2>Distributed Transaction:</h2>
 * <pre>
 * Services Involved:
 * - Order Service (local database)
 * - Product Service (inventory management)
 * - Payment Service (payment processing)
 * 
 * Challenge:
 * - No shared transaction
 * - Different databases
 * - Network failures possible
 * 
 * Solution:
 * - Saga pattern with compensation
 * - Eventual consistency
 * - Idempotent operations
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class CreateOrderSaga extends Saga<CreateOrderContext> {

    private final CreateOrderStep createOrderStep;
    private final ReserveInventoryStep reserveInventoryStep;
    private final ProcessPaymentStep processPaymentStep;
    private final ConfirmOrderStep confirmOrderStep;

    @Autowired
    public CreateOrderSaga(
            CreateOrderStep createOrderStep,
            ReserveInventoryStep reserveInventoryStep,
            ProcessPaymentStep processPaymentStep,
            ConfirmOrderStep confirmOrderStep) {
        this.createOrderStep = createOrderStep;
        this.reserveInventoryStep = reserveInventoryStep;
        this.processPaymentStep = processPaymentStep;
        this.confirmOrderStep = confirmOrderStep;
    }

    @Override
    public List<SagaStep<CreateOrderContext>> getSteps() {
        return List.of(
            createOrderStep,
            reserveInventoryStep,
            processPaymentStep,
            confirmOrderStep
        );
    }

    @Override
    public String getSagaName() {
        return "CreateOrderSaga";
    }
}

