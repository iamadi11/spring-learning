-- =====================================================
-- E-Commerce Microservices - Auth Service Database Schema
-- =====================================================
-- Version: 1.0
-- Description: Initial database schema for authentication service
-- Author: E-commerce Platform Team
-- Date: 2024-01-01
--
-- This migration creates all tables needed for authentication:
-- - tenants: Multi-tenancy support
-- - users: User accounts
-- - roles: User roles (ADMIN, USER, SELLER, etc.)
-- - permissions: Granular permissions
-- - user_roles: Many-to-many relationship between users and roles
-- - role_permissions: Many-to-many relationship between roles and permissions
-- - api_keys: API keys for service-to-service authentication
-- - refresh_tokens: Refresh tokens for JWT token refresh
-- =====================================================

-- =====================================================
-- Tenants Table
-- =====================================================
-- Stores tenant information for multi-tenancy
-- Each tenant has isolated data and configuration
CREATE TABLE tenants (
    -- Primary key: auto-incrementing ID
    id BIGSERIAL PRIMARY KEY,
    
    -- Tenant name: unique identifier (e.g., "acme-corp")
    name VARCHAR(100) NOT NULL UNIQUE,
    
    -- Tenant domain: associated domain (e.g., "acme.ecommerce.com")
    domain VARCHAR(150) NOT NULL UNIQUE,
    
    -- Contact email: primary contact for tenant
    contact_email VARCHAR(100),
    
    -- Active flag: whether tenant is active
    active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Timestamps: track creation and updates
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on tenant name for faster lookups
CREATE INDEX idx_tenants_name ON tenants(name);

-- Create index on tenant domain for faster lookups
CREATE INDEX idx_tenants_domain ON tenants(domain);

-- Add comment to table
COMMENT ON TABLE tenants IS 'Multi-tenancy: Stores tenant information and configuration';

-- =====================================================
-- Users Table
-- =====================================================
-- Stores user account information
-- Includes support for:
-- - Local authentication (email/password)
-- - OAuth2 authentication (Google, GitHub, etc.)
-- - Multi-tenancy
-- - Two-factor authentication
CREATE TABLE users (
    -- Primary key: auto-incrementing ID
    id BIGSERIAL PRIMARY KEY,
    
    -- Username: unique within tenant
    username VARCHAR(50) NOT NULL,
    
    -- Email: globally unique, used for login
    email VARCHAR(100) NOT NULL UNIQUE,
    
    -- Password: BCrypt hash (nullable for OAuth users)
    password VARCHAR(255) NOT NULL,
    
    -- Name fields
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    
    -- Authentication provider: LOCAL, GOOGLE, GITHUB, FACEBOOK, TWITTER
    auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    
    -- Provider ID: user ID from OAuth provider
    provider_id VARCHAR(255),
    
    -- Email verification flag
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Profile image URL
    image_url VARCHAR(255),
    
    -- Account status: PENDING, ACTIVE, INACTIVE, SUSPENDED, BANNED
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    -- Tenant ID: for multi-tenancy
    tenant_id VARCHAR(100) NOT NULL,
    
    -- Two-factor authentication flag
    using_2fa BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Two-factor authentication secret (TOTP)
    secret_2fa VARCHAR(255),
    
    -- Timestamps: track creation and updates
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Unique constraint: username unique per tenant
    CONSTRAINT uk_users_username_tenant UNIQUE (username, tenant_id)
);

-- Create index on email for faster login lookups
CREATE INDEX idx_users_email ON users(email);

-- Create index on username and tenant for faster lookups
CREATE INDEX idx_users_username_tenant ON users(username, tenant_id);

-- Create index on tenant_id for multi-tenancy queries
CREATE INDEX idx_users_tenant_id ON users(tenant_id);

-- Create index on auth_provider for OAuth queries
CREATE INDEX idx_users_auth_provider ON users(auth_provider);

-- Create index on status for filtering active users
CREATE INDEX idx_users_status ON users(status);

-- Add comment to table
COMMENT ON TABLE users IS 'User accounts with support for local and OAuth authentication';

-- =====================================================
-- Roles Table
-- =====================================================
-- Stores role definitions (ADMIN, USER, SELLER, etc.)
-- Roles group permissions for easier management
CREATE TABLE roles (
    -- Primary key: auto-incrementing ID
    id BIGSERIAL PRIMARY KEY,
    
    -- Role name: unique identifier (e.g., "ADMIN", "USER", "SELLER")
    name VARCHAR(50) NOT NULL UNIQUE,
    
    -- Description: human-readable description
    description VARCHAR(255),
    
    -- Timestamps: track creation and updates
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on role name for faster lookups
CREATE INDEX idx_roles_name ON roles(name);

-- Add comment to table
COMMENT ON TABLE roles IS 'Role definitions for role-based access control (RBAC)';

-- =====================================================
-- Permissions Table
-- =====================================================
-- Stores granular permission definitions
-- Permissions are assigned to roles, not directly to users
CREATE TABLE permissions (
    -- Primary key: auto-incrementing ID
    id BIGSERIAL PRIMARY KEY,
    
    -- Permission name: unique identifier (e.g., "READ_PRODUCT", "WRITE_ORDER")
    name VARCHAR(100) NOT NULL UNIQUE,
    
    -- Description: human-readable description
    description VARCHAR(255),
    
    -- Timestamps: track creation and updates
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on permission name for faster lookups
CREATE INDEX idx_permissions_name ON permissions(name);

-- Add comment to table
COMMENT ON TABLE permissions IS 'Granular permission definitions for fine-grained access control';

-- =====================================================
-- User-Roles Junction Table
-- =====================================================
-- Many-to-many relationship between users and roles
-- A user can have multiple roles
-- A role can be assigned to multiple users
CREATE TABLE user_roles (
    -- Foreign key: references users table
    user_id BIGINT NOT NULL,
    
    -- Foreign key: references roles table
    role_id BIGINT NOT NULL,
    
    -- Composite primary key: combination of user_id and role_id
    PRIMARY KEY (user_id, role_id),
    
    -- Foreign key constraint: user_id references users(id)
    -- ON DELETE CASCADE: delete user-role mapping when user is deleted
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Foreign key constraint: role_id references roles(id)
    -- ON DELETE CASCADE: delete user-role mapping when role is deleted
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) 
        REFERENCES roles(id) ON DELETE CASCADE
);

-- Create index on user_id for faster lookups
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

-- Create index on role_id for faster lookups
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- Add comment to table
COMMENT ON TABLE user_roles IS 'Many-to-many relationship between users and roles';

-- =====================================================
-- Role-Permissions Junction Table
-- =====================================================
-- Many-to-many relationship between roles and permissions
-- A role can have multiple permissions
-- A permission can be assigned to multiple roles
CREATE TABLE role_permissions (
    -- Foreign key: references roles table
    role_id BIGINT NOT NULL,
    
    -- Foreign key: references permissions table
    permission_id BIGINT NOT NULL,
    
    -- Composite primary key: combination of role_id and permission_id
    PRIMARY KEY (role_id, permission_id),
    
    -- Foreign key constraint: role_id references roles(id)
    -- ON DELETE CASCADE: delete role-permission mapping when role is deleted
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) 
        REFERENCES roles(id) ON DELETE CASCADE,
    
    -- Foreign key constraint: permission_id references permissions(id)
    -- ON DELETE CASCADE: delete role-permission mapping when permission is deleted
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) 
        REFERENCES permissions(id) ON DELETE CASCADE
);

-- Create index on role_id for faster lookups
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);

-- Create index on permission_id for faster lookups
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

-- Add comment to table
COMMENT ON TABLE role_permissions IS 'Many-to-many relationship between roles and permissions';

-- =====================================================
-- API Keys Table
-- =====================================================
-- Stores API keys for service-to-service authentication
-- Each API key is owned by a user and has an expiration date
CREATE TABLE api_keys (
    -- Primary key: auto-incrementing ID
    id BIGSERIAL PRIMARY KEY,
    
    -- API key value: unique random string (e.g., UUID)
    key_value VARCHAR(255) NOT NULL UNIQUE,
    
    -- Description: purpose of the API key
    description VARCHAR(255) NOT NULL,
    
    -- Expiration date: when the API key expires
    expires_at TIMESTAMP NOT NULL,
    
    -- Enabled flag: whether the API key is active
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Foreign key: references users table (owner of the API key)
    user_id BIGINT NOT NULL,
    
    -- Timestamps: track creation and updates
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint: user_id references users(id)
    -- ON DELETE CASCADE: delete API key when user is deleted
    CONSTRAINT fk_api_keys_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

-- Create index on key_value for faster API key lookups
CREATE INDEX idx_api_keys_key_value ON api_keys(key_value);

-- Create index on user_id for querying user's API keys
CREATE INDEX idx_api_keys_user_id ON api_keys(user_id);

-- Create index on enabled for filtering active API keys
CREATE INDEX idx_api_keys_enabled ON api_keys(enabled);

-- Add comment to table
COMMENT ON TABLE api_keys IS 'API keys for service-to-service authentication';

-- =====================================================
-- Refresh Tokens Table
-- =====================================================
-- Stores refresh tokens for JWT token refresh
-- Refresh tokens are long-lived and can be revoked
CREATE TABLE refresh_tokens (
    -- Primary key: auto-incrementing ID
    id BIGSERIAL PRIMARY KEY,
    
    -- Token value: JWT refresh token string
    token VARCHAR(500) NOT NULL UNIQUE,
    
    -- Expiration date: when the refresh token expires
    expiry_date TIMESTAMP NOT NULL,
    
    -- Foreign key: references users table (owner of the token)
    user_id BIGINT NOT NULL,
    
    -- Created timestamp: when the token was created
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint: user_id references users(id)
    -- ON DELETE CASCADE: delete refresh token when user is deleted
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

-- Create unique index on token for faster lookups and uniqueness
CREATE UNIQUE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

-- Create index on user_id for querying user's refresh tokens
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Create index on expiry_date for cleaning up expired tokens
CREATE INDEX idx_refresh_tokens_expiry_date ON refresh_tokens(expiry_date);

-- Add comment to table
COMMENT ON TABLE refresh_tokens IS 'Refresh tokens for JWT token refresh and revocation';

