-- =====================================================
-- E-Commerce Microservices - Auth Service Seed Data
-- =====================================================
-- Version: 2.0
-- Description: Initial seed data for authentication service
-- Author: E-commerce Platform Team
-- Date: 2024-01-01
--
-- This migration populates the database with:
-- - Default tenant
-- - Default roles (ADMIN, USER, SELLER, SUPPORT)
-- - Default permissions
-- - Role-permission mappings
-- =====================================================

-- =====================================================
-- Seed Default Tenant
-- =====================================================
INSERT INTO tenants (name, domain, contact_email, active, created_at, updated_at)
VALUES ('default', 'default.ecommerce.local', 'admin@ecommerce.local', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =====================================================
-- Seed Roles
-- =====================================================
-- ADMIN: Full system access, can manage all resources
INSERT INTO roles (name, description, created_at, updated_at)
VALUES ('ADMIN', 'Administrator with full system access', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- USER: Regular user, can browse and purchase
INSERT INTO roles (name, description, created_at, updated_at)
VALUES ('USER', 'Regular user with basic access', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- SELLER: Can manage their own products and orders
INSERT INTO roles (name, description, created_at, updated_at)
VALUES ('SELLER', 'Seller who can manage products and view orders', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- SUPPORT: Can view and assist users, cannot modify system settings
INSERT INTO roles (name, description, created_at, updated_at)
VALUES ('SUPPORT', 'Support staff with limited administrative access', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =====================================================
-- Seed Permissions
-- =====================================================
-- Permission naming convention: ACTION_RESOURCE
-- Examples: READ_PRODUCT, WRITE_PRODUCT, DELETE_USER

-- User Management Permissions
INSERT INTO permissions (name, description, created_at, updated_at)
VALUES 
    ('READ_USER', 'View user information', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('WRITE_USER', 'Create and update user information', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DELETE_USER', 'Delete user accounts', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Product Management Permissions
INSERT INTO permissions (name, description, created_at, updated_at)
VALUES 
    ('READ_PRODUCT', 'View product information', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('WRITE_PRODUCT', 'Create and update products', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DELETE_PRODUCT', 'Delete products', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Order Management Permissions
INSERT INTO permissions (name, description, created_at, updated_at)
VALUES 
    ('READ_ORDER', 'View order information', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('WRITE_ORDER', 'Create and update orders', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DELETE_ORDER', 'Cancel orders', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Payment Management Permissions
INSERT INTO permissions (name, description, created_at, updated_at)
VALUES 
    ('READ_PAYMENT', 'View payment information', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('WRITE_PAYMENT', 'Process payments', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('REFUND_PAYMENT', 'Issue refunds', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Review Management Permissions
INSERT INTO permissions (name, description, created_at, updated_at)
VALUES 
    ('READ_REVIEW', 'View product reviews', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('WRITE_REVIEW', 'Create and update reviews', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DELETE_REVIEW', 'Delete reviews', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- System Administration Permissions
INSERT INTO permissions (name, description, created_at, updated_at)
VALUES 
    ('MANAGE_ROLES', 'Manage roles and permissions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MANAGE_TENANTS', 'Manage tenants', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('VIEW_LOGS', 'View system logs', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MANAGE_SETTINGS', 'Manage system settings', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =====================================================
-- Assign Permissions to ADMIN Role
-- =====================================================
-- ADMIN has all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    (SELECT id FROM roles WHERE name = 'ADMIN'),
    id
FROM permissions;

-- =====================================================
-- Assign Permissions to USER Role
-- =====================================================
-- USER can:
-- - View products
-- - View and write their own orders
-- - View and write their own reviews
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    (SELECT id FROM roles WHERE name = 'USER'),
    id
FROM permissions
WHERE name IN (
    'READ_PRODUCT',
    'READ_ORDER',
    'WRITE_ORDER',
    'READ_REVIEW',
    'WRITE_REVIEW'
);

-- =====================================================
-- Assign Permissions to SELLER Role
-- =====================================================
-- SELLER can:
-- - View all products
-- - Write/delete their own products
-- - View orders for their products
-- - View reviews for their products
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    (SELECT id FROM roles WHERE name = 'SELLER'),
    id
FROM permissions
WHERE name IN (
    'READ_PRODUCT',
    'WRITE_PRODUCT',
    'DELETE_PRODUCT',
    'READ_ORDER',
    'READ_REVIEW'
);

-- =====================================================
-- Assign Permissions to SUPPORT Role
-- =====================================================
-- SUPPORT can:
-- - View users, products, orders, payments, reviews
-- - Write orders (to assist customers)
-- - View system logs
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    (SELECT id FROM roles WHERE name = 'SUPPORT'),
    id
FROM permissions
WHERE name IN (
    'READ_USER',
    'READ_PRODUCT',
    'READ_ORDER',
    'WRITE_ORDER',
    'READ_PAYMENT',
    'READ_REVIEW',
    'VIEW_LOGS'
);

-- =====================================================
-- Verification Queries (for debugging)
-- =====================================================
-- Uncomment these to verify seed data after migration

-- Count roles
-- SELECT 'Roles Created:' AS info, COUNT(*) AS count FROM roles;

-- Count permissions
-- SELECT 'Permissions Created:' AS info, COUNT(*) AS count FROM permissions;

-- Count role-permission mappings
-- SELECT 'Role-Permission Mappings:' AS info, COUNT(*) AS count FROM role_permissions;

-- Show ADMIN permissions
-- SELECT r.name AS role, p.name AS permission
-- FROM roles r
-- JOIN role_permissions rp ON r.id = rp.role_id
-- JOIN permissions p ON rp.permission_id = p.id
-- WHERE r.name = 'ADMIN'
-- ORDER BY p.name;

-- Show USER permissions
-- SELECT r.name AS role, p.name AS permission
-- FROM roles r
-- JOIN role_permissions rp ON r.id = rp.role_id
-- JOIN permissions p ON rp.permission_id = p.id
-- WHERE r.name = 'USER'
-- ORDER BY p.name;

