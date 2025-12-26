-- Payment Service Database Schema
-- Creates tables for payment processing with comprehensive tracking

-- ====================================================================
-- PAYMENTS TABLE
-- ====================================================================
-- Stores all payment transactions with full audit trail
CREATE TABLE payments (
    -- Primary Key: Transaction ID from payment gateway
    transaction_id VARCHAR(100) PRIMARY KEY,
    
    -- Idempotency Key: Order ID (unique)
    -- Prevents duplicate charges for same order
    order_id VARCHAR(36) NOT NULL UNIQUE,
    
    -- User reference
    user_id BIGINT NOT NULL,
    
    -- Payment amount
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    
    -- Payment method
    payment_method VARCHAR(30) NOT NULL,
    
    -- Payment status
    status VARCHAR(30) NOT NULL,
    
    -- Gateway information
    gateway VARCHAR(50),
    gateway_transaction_id VARCHAR(100),
    gateway_response TEXT,
    gateway_response_code VARCHAR(50),
    
    -- Failure tracking
    failure_reason VARCHAR(200),
    retry_count INTEGER NOT NULL DEFAULT 0,
    
    -- Refund tracking
    refund_amount DECIMAL(10, 2) CHECK (refund_amount >= 0),
    refunded_at TIMESTAMP,
    
    -- Metadata (JSON)
    metadata TEXT,
    
    -- Audit timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ====================================================================
-- INDEXES
-- ====================================================================

-- Index on order_id for idempotency checks (most frequent query)
CREATE INDEX idx_payments_order_id ON payments(order_id);

-- Index on user_id for user payment history
CREATE INDEX idx_payments_user_id ON payments(user_id);

-- Index on status for filtering
CREATE INDEX idx_payments_status ON payments(status);

-- Composite index for fraud detection queries
CREATE INDEX idx_payments_user_status_created 
ON payments(user_id, status, created_at);

-- Index on gateway_transaction_id for reconciliation
CREATE INDEX idx_payments_gateway_txn_id ON payments(gateway_transaction_id);

-- ====================================================================
-- COMMENTS
-- ====================================================================

COMMENT ON TABLE payments IS 
'Payment transactions with comprehensive tracking for idempotency, audit, and fraud detection';

COMMENT ON COLUMN payments.order_id IS 
'Idempotency key - same order ID will not be charged twice';

COMMENT ON COLUMN payments.retry_count IS 
'Number of retry attempts - used for monitoring resilience patterns';

COMMENT ON COLUMN payments.metadata IS 
'JSON metadata: IP address, user agent, device ID for fraud detection';

