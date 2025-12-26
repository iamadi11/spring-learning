-- =====================================================
-- E-Commerce Microservices - Order Service Database Schema
-- =====================================================
-- Version: 1.0
-- Description: Tables for orders and order items
-- Author: E-commerce Platform Team
-- Date: 2024-01-01
-- =====================================================

-- =====================================================
-- Orders Table
-- =====================================================
CREATE TABLE orders (
    order_id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    
    total_amount DECIMAL(10, 2) NOT NULL,
    shipping_fee DECIMAL(10, 2) NOT NULL,
    tax_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(10, 2),
    final_amount DECIMAL(10, 2) NOT NULL,
    
    payment_method VARCHAR(50),
    payment_transaction_id VARCHAR(100),
    
    shipping_address TEXT NOT NULL,
    billing_address TEXT,
    notes TEXT,
    
    tracking_number VARCHAR(100),
    carrier VARCHAR(50),
    
    saga_id VARCHAR(36),
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_saga_id ON orders(saga_id);

-- =====================================================
-- Order Items Table
-- =====================================================
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    
    product_id VARCHAR(36) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    sku VARCHAR(50) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    product_image_url VARCHAR(500),
    
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) 
        REFERENCES orders(order_id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- =====================================================
-- Update Trigger for Orders
-- =====================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

