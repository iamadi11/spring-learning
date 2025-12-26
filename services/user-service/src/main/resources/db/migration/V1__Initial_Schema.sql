-- =====================================================
-- E-Commerce Microservices - User Service Database Schema
-- =====================================================
-- Version: 1.0
-- Description: Initial database schema for user service
-- Author: E-commerce Platform Team
-- Date: 2024-01-01
--
-- This migration creates all tables needed for user profiles:
-- - user_profiles: Extended user information
-- - addresses: Shipping and billing addresses
-- - user_preferences: User settings and preferences
-- =====================================================

-- =====================================================
-- User Profiles Table
-- =====================================================
-- Stores extended user profile information beyond auth data
CREATE TABLE user_profiles (
    -- Primary key: user_id from Auth Service
    user_id BIGINT PRIMARY KEY,
    
    -- Email (duplicated from Auth Service for convenience)
    email VARCHAR(100) NOT NULL UNIQUE,
    
    -- Profile information
    bio TEXT,  -- User biography (up to 1000 characters)
    avatar_url VARCHAR(500),  -- Profile picture URL
    phone_number VARCHAR(20),  -- Phone with country code
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Personal information
    date_of_birth DATE,  -- For age verification and promotions
    gender VARCHAR(20),  -- MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
    
    -- Profile metrics
    profile_completion INTEGER DEFAULT 0,  -- Percentage (0-100)
    last_login TIMESTAMP,  -- Last login timestamp (from Auth Service)
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_user_profiles_email ON user_profiles(email);
CREATE INDEX idx_user_profiles_phone ON user_profiles(phone_number);
CREATE INDEX idx_user_profiles_completion ON user_profiles(profile_completion);

-- Add table comment
COMMENT ON TABLE user_profiles IS 'Extended user profile information beyond authentication data';

-- =====================================================
-- Addresses Table
-- =====================================================
-- Stores shipping and billing addresses
CREATE TABLE addresses (
    -- Primary key: auto-incrementing ID
    id BIGSERIAL PRIMARY KEY,
    
    -- Foreign key: user_id references user_profiles
    user_id BIGINT NOT NULL,
    
    -- Address type: SHIPPING, BILLING, BOTH
    type VARCHAR(20) NOT NULL,
    
    -- Recipient information
    full_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    
    -- Address fields
    address_line1 VARCHAR(200) NOT NULL,  -- Street address
    address_line2 VARCHAR(200),  -- Apartment, suite, unit (optional)
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL,
    
    -- Default address flag
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) 
        REFERENCES user_profiles(user_id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_addresses_user_id ON addresses(user_id);
CREATE INDEX idx_addresses_is_default ON addresses(is_default);
CREATE INDEX idx_addresses_type ON addresses(type);

-- Add table comment
COMMENT ON TABLE addresses IS 'User shipping and billing addresses';

-- =====================================================
-- User Preferences Table
-- =====================================================
-- Stores user settings and preferences
CREATE TABLE user_preferences (
    -- Primary key: same as user_id (shared primary key)
    user_id BIGINT PRIMARY KEY,
    
    -- Localization settings
    language VARCHAR(10) NOT NULL DEFAULT 'en',  -- ISO 639-1 code
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',  -- ISO 4217 code
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',  -- IANA timezone
    
    -- Email notification settings
    email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    email_order_updates BOOLEAN NOT NULL DEFAULT TRUE,
    email_promotions BOOLEAN NOT NULL DEFAULT TRUE,
    email_newsletter BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- SMS notification settings
    sms_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    sms_shipping_updates BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Push notification settings
    push_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Display settings
    theme VARCHAR(10) NOT NULL DEFAULT 'AUTO',  -- LIGHT, DARK, AUTO
    items_per_page INTEGER NOT NULL DEFAULT 20,
    product_view VARCHAR(10) NOT NULL DEFAULT 'GRID',  -- GRID, LIST
    
    -- Privacy settings
    public_profile BOOLEAN NOT NULL DEFAULT TRUE,
    searchable_profile BOOLEAN NOT NULL DEFAULT TRUE,
    show_online_status BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_preferences_user FOREIGN KEY (user_id) 
        REFERENCES user_profiles(user_id) ON DELETE CASCADE
);

-- Add table comment
COMMENT ON TABLE user_preferences IS 'User settings and preferences for personalization';

-- =====================================================
-- Database Functions and Triggers
-- =====================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for user_profiles
CREATE TRIGGER update_user_profiles_updated_at 
    BEFORE UPDATE ON user_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger for addresses
CREATE TRIGGER update_addresses_updated_at 
    BEFORE UPDATE ON addresses
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger for user_preferences
CREATE TRIGGER update_user_preferences_updated_at 
    BEFORE UPDATE ON user_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- Constraints and Checks
-- =====================================================

-- Profile completion must be between 0 and 100
ALTER TABLE user_profiles 
    ADD CONSTRAINT chk_profile_completion 
    CHECK (profile_completion >= 0 AND profile_completion <= 100);

-- Address type must be valid
ALTER TABLE addresses 
    ADD CONSTRAINT chk_address_type 
    CHECK (type IN ('SHIPPING', 'BILLING', 'BOTH'));

-- Gender must be valid
ALTER TABLE user_profiles 
    ADD CONSTRAINT chk_gender 
    CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'));

-- Theme must be valid
ALTER TABLE user_preferences 
    ADD CONSTRAINT chk_theme 
    CHECK (theme IN ('LIGHT', 'DARK', 'AUTO'));

-- Product view must be valid
ALTER TABLE user_preferences 
    ADD CONSTRAINT chk_product_view 
    CHECK (product_view IN ('GRID', 'LIST'));

-- Items per page must be reasonable
ALTER TABLE user_preferences 
    ADD CONSTRAINT chk_items_per_page 
    CHECK (items_per_page > 0 AND items_per_page <= 100);

