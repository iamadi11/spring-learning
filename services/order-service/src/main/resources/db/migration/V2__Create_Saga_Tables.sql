-- =====================================================
-- Saga Execution Tables
-- =====================================================
-- Version: 2.0
-- Description: Tables for saga orchestration and state management
-- =====================================================

-- =====================================================
-- Saga Executions Table
-- =====================================================
CREATE TABLE saga_executions (
    saga_id VARCHAR(36) PRIMARY KEY,
    saga_type VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    
    current_step INTEGER NOT NULL DEFAULT 0,
    total_steps INTEGER NOT NULL,
    
    order_id VARCHAR(36) NOT NULL,
    payload TEXT,
    error_message TEXT,
    
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error_at TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    
    CONSTRAINT fk_saga_executions_order FOREIGN KEY (order_id) 
        REFERENCES orders(order_id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_saga_executions_order_id ON saga_executions(order_id);
CREATE INDEX idx_saga_executions_status ON saga_executions(status);
CREATE INDEX idx_saga_executions_saga_type ON saga_executions(saga_type);
CREATE INDEX idx_saga_executions_created_at ON saga_executions(created_at);

-- Index for recovery queries
CREATE INDEX idx_saga_executions_incomplete ON saga_executions(status, created_at) 
    WHERE status IN ('IN_PROGRESS', 'COMPENSATING');

-- =====================================================
-- Update Trigger for Saga Executions
-- =====================================================
CREATE TRIGGER update_saga_executions_updated_at
    BEFORE UPDATE ON saga_executions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

