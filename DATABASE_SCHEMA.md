# Database Schema Documentation

## 📊 Overview

Complete database schema documentation for PostgreSQL and MongoDB databases used across all microservices.

---

## 🗄️ Database Distribution

| Service | Database | Type | Purpose |
|---------|----------|------|---------|
| Auth Service | PostgreSQL | Relational | User authentication, roles, permissions |
| User Service | PostgreSQL | Relational | User profiles, addresses, preferences |
| Order Service | PostgreSQL | Relational | Orders, order items, saga state |
| Payment Service | PostgreSQL | Relational | Payments, transactions, refunds |
| Product Service | MongoDB | Document | Products, event sourcing |
| Review Service | MongoDB | Document | Reviews, ratings |
| Notification Service | MongoDB | Document | Notifications, templates |

---

## 1. Auth Service (PostgreSQL)

### Table: users

Primary table for user authentication.

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    
    -- Authentication provider
    auth_provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    provider_id VARCHAR(255),
    
    -- Status and flags
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    last_login_at TIMESTAMP,
    
    -- Two-Factor Authentication
    tfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    tfa_secret VARCHAR(255),
    
    -- Multi-tenancy
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    
    CONSTRAINT chk_auth_provider CHECK (auth_provider IN ('LOCAL', 'GOOGLE', 'GITHUB', 'FACEBOOK')),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED'))
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_provider_id ON users(auth_provider, provider_id);
```

**Sample Data**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "john@example.com",
  "username": "johndoe",
  "password_hash": "$2a$12$abcd...",
  "first_name": "John",
  "last_name": "Doe",
  "auth_provider": "LOCAL",
  "status": "ACTIVE",
  "email_verified": true,
  "tfa_enabled": false,
  "tenant_id": "tenant-uuid",
  "created_at": "2024-01-15T10:00:00Z"
}
```

---

### Table: roles

Role-based access control.

```sql
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_role_name CHECK (name IN ('ADMIN', 'USER', 'MANAGER', 'SUPPORT'))
);

-- Sample roles
INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Full system access'),
    ('USER', 'Regular user access'),
    ('MANAGER', 'Manager-level access'),
    ('SUPPORT', 'Customer support access');
```

---

### Table: permissions

Fine-grained permissions.

```sql
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_action CHECK (action IN ('CREATE', 'READ', 'UPDATE', 'DELETE'))
);

-- Sample permissions
INSERT INTO permissions (name, resource, action, description) VALUES
    ('read:products', 'PRODUCT', 'READ', 'Read product information'),
    ('write:products', 'PRODUCT', 'CREATE', 'Create/update products'),
    ('delete:products', 'PRODUCT', 'DELETE', 'Delete products'),
    ('read:orders', 'ORDER', 'READ', 'Read order information'),
    ('write:orders', 'ORDER', 'CREATE', 'Create orders');
```

---

### Table: user_roles

Many-to-many relationship between users and roles.

```sql
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
```

---

### Table: role_permissions

Many-to-many relationship between roles and permissions.

```sql
CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    
    PRIMARY KEY (role_id, permission_id)
);
```

---

### Table: api_keys

API key management for service-to-service auth.

```sql
CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_value VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Scopes (array of strings)
    scopes TEXT[] NOT NULL DEFAULT '{}',
    
    -- Metadata
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_api_keys_key_value ON api_keys(key_value);
CREATE INDEX idx_api_keys_user_id ON api_keys(user_id);
```

---

### Table: tenants

Multi-tenancy support.

```sql
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    domain VARCHAR(255) UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    
    -- Subscription info
    plan VARCHAR(50) NOT NULL DEFAULT 'FREE',
    max_users INT NOT NULL DEFAULT 10,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
    CONSTRAINT chk_plan CHECK (plan IN ('FREE', 'BASIC', 'PRO', 'ENTERPRISE'))
);
```

---

### Table: refresh_tokens

Refresh token management.

```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
```

---

## 2. User Service (PostgreSQL)

### Table: user_profiles

Extended user profile information (CQRS pattern).

```sql
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE, -- References Auth Service
    
    -- Personal information
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    date_of_birth DATE,
    gender VARCHAR(20),
    
    -- Profile metadata
    avatar_url VARCHAR(500),
    bio TEXT,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    
    CONSTRAINT chk_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'))
);

CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX idx_user_profiles_email ON user_profiles(email);
```

---

### Table: addresses

User addresses for shipping and billing.

```sql
CREATE TABLE addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    
    -- Address type
    type VARCHAR(20) NOT NULL,
    
    -- Address details
    street VARCHAR(255) NOT NULL,
    apartment VARCHAR(100),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    zip_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL,
    
    -- Default address
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_type CHECK (type IN ('SHIPPING', 'BILLING', 'BOTH'))
);

CREATE INDEX idx_addresses_user_profile_id ON addresses(user_profile_id);
```

---

### Table: user_preferences

User notification and display preferences.

```sql
CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_profile_id UUID NOT NULL UNIQUE REFERENCES user_profiles(id) ON DELETE CASCADE,
    
    -- Notification preferences
    email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    sms_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    push_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Newsletter
    newsletter_subscribed BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_emails BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Display preferences
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## 3. Order Service (PostgreSQL)

### Table: orders

Order header information.

```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    
    -- Order status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    -- Payment information
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    
    -- Amounts
    subtotal DECIMAL(10, 2) NOT NULL,
    tax DECIMAL(10, 2) NOT NULL DEFAULT 0,
    shipping DECIMAL(10, 2) NOT NULL DEFAULT 0,
    discount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    
    -- Shipping address (denormalized for performance)
    shipping_street VARCHAR(255),
    shipping_city VARCHAR(100),
    shipping_state VARCHAR(100),
    shipping_zip_code VARCHAR(20),
    shipping_country VARCHAR(100),
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    
    -- Optimistic locking
    version INT NOT NULL DEFAULT 0,
    
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED'))
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
```

---

### Table: order_items

Line items within an order.

```sql
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    
    -- Product information (denormalized)
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100),
    
    -- Pricing
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_quantity CHECK (quantity > 0),
    CONSTRAINT chk_unit_price CHECK (unit_price >= 0)
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
```

---

### Table: saga_executions

Saga orchestration state (Saga Pattern).

```sql
CREATE TABLE saga_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    saga_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    
    -- Saga state
    status VARCHAR(20) NOT NULL DEFAULT 'STARTED',
    current_step INT NOT NULL DEFAULT 0,
    total_steps INT NOT NULL,
    
    -- Execution data (JSON)
    context JSONB NOT NULL,
    
    -- Error handling
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    
    CONSTRAINT chk_status CHECK (status IN ('STARTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'COMPENSATING', 'COMPENSATED'))
);

CREATE INDEX idx_saga_executions_aggregate_id ON saga_executions(aggregate_id);
CREATE INDEX idx_saga_executions_status ON saga_executions(status);
```

---

## 4. Payment Service (PostgreSQL)

### Table: payments

Payment transactions.

```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    
    -- Amount
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    
    -- Payment method
    payment_method VARCHAR(50) NOT NULL,
    payment_provider VARCHAR(50) NOT NULL,
    
    -- Transaction details
    transaction_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    -- External gateway info
    gateway_transaction_id VARCHAR(255),
    gateway_response TEXT,
    
    -- Idempotency
    idempotency_key VARCHAR(255) UNIQUE,
    
    -- Error handling
    error_code VARCHAR(50),
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    
    CONSTRAINT chk_transaction_type CHECK (transaction_type IN ('CHARGE', 'REFUND', 'AUTHORIZATION', 'CAPTURE')),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED', 'CANCELLED')),
    CONSTRAINT chk_amount CHECK (amount > 0)
);

CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_idempotency_key ON payments(idempotency_key);
```

---

## 5. Product Service (MongoDB)

### Collection: products

Product catalog with Event Sourcing.

```javascript
{
  _id: ObjectId("507f1f77bcf86cd799439011"),
  
  // Product information
  name: "Wireless Headphones",
  description: "High-quality noise-cancelling headphones",
  slug: "wireless-headphones-audiotech",
  
  // Pricing
  price: 299.99,
  currency: "USD",
  compareAtPrice: 349.99,
  
  // Inventory
  stockQuantity: 50,
  sku: "AUD-WH-001",
  
  // Category
  category: {
    id: "cat-uuid",
    name: "Electronics",
    slug: "electronics"
  },
  
  // Images
  images: [
    {
      url: "https://cdn.example.com/headphones-1.jpg",
      alt: "Front view",
      isPrimary: true
    },
    {
      url: "https://cdn.example.com/headphones-2.jpg",
      alt: "Side view",
      isPrimary: false
    }
  ],
  
  // Specifications (flexible schema)
  specifications: {
    brand: "AudioTech",
    color: "Black",
    weight: "250g",
    bluetooth: "5.0",
    batteryLife: "30 hours",
    noiseCancellation: true
  },
  
  // SEO
  metaTitle: "AudioTech Wireless Headphones - Noise Cancelling",
  metaDescription: "Experience premium sound quality...",
  metaKeywords: ["headphones", "wireless", "noise-cancelling"],
  
  // Status
  status: "ACTIVE",
  published: true,
  
  // Ratings (denormalized for performance)
  averageRating: 4.5,
  reviewCount: 120,
  
  // Event Sourcing
  version: 5,
  
  // Timestamps
  createdAt: ISODate("2024-01-15T10:00:00Z"),
  updatedAt: ISODate("2024-01-20T15:30:00Z"),
  deletedAt: null
}

// Indexes
db.products.createIndex({ "slug": 1 }, { unique: true });
db.products.createIndex({ "category.id": 1 });
db.products.createIndex({ "status": 1, "published": 1 });
db.products.createIndex({ "price": 1 });
db.products.createIndex({ "createdAt": -1 });
```

---

### Collection: product_events

Event store for Event Sourcing.

```javascript
{
  _id: ObjectId("507f1f77bcf86cd799439012"),
  
  // Event identification
  eventId: "event-uuid",
  eventType: "ProductCreatedEvent",
  aggregateId: "507f1f77bcf86cd799439011",
  version: 1,
  
  // Event data
  data: {
    name: "Wireless Headphones",
    price: 299.99,
    stockQuantity: 50,
    // ... all product fields at creation
  },
  
  // Metadata
  userId: "user-uuid",
  timestamp: ISODate("2024-01-15T10:00:00Z"),
  
  // Correlation
  correlationId: "corr-uuid",
  causationId: "cause-uuid"
}

// Event types: ProductCreatedEvent, ProductUpdatedEvent, PriceChangedEvent, StockChangedEvent, ProductDeletedEvent

// Indexes
db.product_events.createIndex({ "aggregateId": 1, "version": 1 }, { unique: true });
db.product_events.createIndex({ "eventType": 1 });
db.product_events.createIndex({ "timestamp": -1 });
```

---

### Collection: categories

Product categories (hierarchical).

```javascript
{
  _id: ObjectId("507f1f77bcf86cd799439013"),
  
  name: "Electronics",
  slug: "electronics",
  description: "Electronic devices and accessories",
  
  // Hierarchy
  parentId: null,  // Top-level category
  level: 0,
  path: "electronics",
  
  // Metadata
  imageUrl: "https://cdn.example.com/categories/electronics.jpg",
  displayOrder: 1,
  
  // SEO
  metaTitle: "Electronics - Shop Online",
  metaDescription: "Browse our wide selection of electronics",
  
  // Status
  active: true,
  
  // Statistics (denormalized)
  productCount: 1250,
  
  // Timestamps
  createdAt: ISODate("2024-01-01T00:00:00Z"),
  updatedAt: ISODate("2024-01-15T10:00:00Z")
}

// Indexes
db.categories.createIndex({ "slug": 1 }, { unique: true });
db.categories.createIndex({ "parentId": 1 });
db.categories.createIndex({ "path": 1 });
```

---

## 6. Review Service (MongoDB)

### Collection: reviews

Product reviews and ratings.

```javascript
{
  _id: ObjectId("507f1f77bcf86cd799439014"),
  
  // References
  productId: "product-uuid",
  userId: "user-uuid",
  
  // User information (denormalized)
  userName: "John Doe",
  userAvatar: "https://cdn.example.com/avatars/user.jpg",
  verifiedPurchase: true,
  
  // Review content
  rating: 5,
  title: "Amazing product!",
  comment: "Best headphones I've ever used. Sound quality is incredible.",
  
  // Media
  images: [
    "https://cdn.example.com/reviews/img1.jpg",
    "https://cdn.example.com/reviews/img2.jpg"
  ],
  
  // Moderation
  status: "APPROVED",
  moderatedBy: "moderator-uuid",
  moderatedAt: ISODate("2024-01-15T11:00:00Z"),
  moderationNote: null,
  
  // Engagement
  helpfulCount: 25,
  unhelpfulCount: 2,
  reportedCount: 0,
  
  // Response from seller
  sellerResponse: {
    message: "Thank you for your review!",
    respondedAt: ISODate("2024-01-16T09:00:00Z")
  },
  
  // Timestamps
  createdAt: ISODate("2024-01-15T10:00:00Z"),
  updatedAt: ISODate("2024-01-15T11:00:00Z")
}

// Indexes
db.reviews.createIndex({ "productId": 1 });
db.reviews.createIndex({ "userId": 1 });
db.reviews.createIndex({ "status": 1 });
db.reviews.createIndex({ "rating": 1 });
db.reviews.createIndex({ "createdAt": -1 });
```

---

## 7. Notification Service (MongoDB)

### Collection: notifications

User notifications.

```javascript
{
  _id: ObjectId("507f1f77bcf86cd799439015"),
  
  // Recipient
  userId: "user-uuid",
  
  // Notification content
  type: "ORDER_CONFIRMATION",
  title: "Order Confirmed",
  message: "Your order #ORD-2024-001234 has been confirmed and is being processed.",
  
  // Channels
  channels: ["EMAIL", "PUSH"],
  
  // Status per channel
  channelStatus: {
    EMAIL: {
      status: "SENT",
      sentAt: ISODate("2024-01-15T10:31:00Z"),
      error: null
    },
    PUSH: {
      status: "SENT",
      sentAt: ISODate("2024-01-15T10:31:00Z"),
      error: null
    }
  },
  
  // Metadata
  data: {
    orderNumber: "ORD-2024-001234",
    total: 668.97,
    orderId: "order-uuid"
  },
  
  // Action
  actionUrl: "/orders/order-uuid",
  actionText: "View Order",
  
  // Priority
  priority: "HIGH",
  
  // Read status
  read: false,
  readAt: null,
  
  // Timestamps
  createdAt: ISODate("2024-01-15T10:31:00Z"),
  expiresAt: ISODate("2024-02-15T10:31:00Z")
}

// Indexes
db.notifications.createIndex({ "userId": 1, "createdAt": -1 });
db.notifications.createIndex({ "read": 1 });
db.notifications.createIndex({ "type": 1 });
db.notifications.createIndex({ "expiresAt": 1 }, { expireAfterSeconds: 0 }); // TTL index
```

---

## 🔐 Database Security

### PostgreSQL Security

**1. Role-Based Access**:
```sql
-- Read-only user for replicas
CREATE ROLE readonly_user WITH LOGIN PASSWORD 'secure_password';
GRANT CONNECT ON DATABASE ecommerce_db TO readonly_user;
GRANT USAGE ON SCHEMA public TO readonly_user;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO readonly_user;

-- Service-specific user
CREATE ROLE order_service WITH LOGIN PASSWORD 'secure_password';
GRANT CONNECT ON DATABASE ecommerce_order_db TO order_service;
GRANT ALL PRIVILEGES ON DATABASE ecommerce_order_db TO order_service;
```

**2. Row-Level Security (Multi-tenancy)**:
```sql
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON users
    USING (tenant_id = current_setting('app.current_tenant')::UUID);
```

### MongoDB Security

**1. Authentication**:
```javascript
db.createUser({
  user: "productService",
  pwd: "secure_password",
  roles: [
    { role: "readWrite", db: "ecommerce_product_db" }
  ]
});
```

**2. Field-Level Encryption**:
```javascript
// Encrypt sensitive fields
const encryptionSchema = {
  "properties": {
    "paymentDetails": {
      "encrypt": {
        "bsonType": "object",
        "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic"
      }
    }
  }
};
```

---

## 📊 Database Performance

### PostgreSQL Optimization

**1. Connection Pooling** (HikariCP):
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
```

**2. Query Optimization**:
- Use EXPLAIN ANALYZE for slow queries
- Add indexes on frequently queried columns
- Use materialized views for complex aggregations

### MongoDB Optimization

**1. Indexing Strategy**:
```javascript
// Compound index for common query
db.products.createIndex({ "category.id": 1, "price": 1, "status": 1 });

// Text index for search
db.products.createIndex({ "name": "text", "description": "text" });
```

**2. Aggregation Pipeline**:
- Use $match early to filter documents
- Use indexes with $match and $sort
- Limit result sets with $limit

---

## 🔄 Database Migrations

### Flyway (PostgreSQL)

Migrations in `src/main/resources/db/migration/`:
- `V1__Initial_Schema.sql`
- `V2__Add_Tenants_Table.sql`
- `V3__Add_2FA_Columns.sql`

### MongoDB Migrations

Use Mongock for schema migrations:
```java
@ChangeSet(order = "001", id = "create-products-collection", author = "admin")
public void createProductsCollection(MongoDatabase db) {
    db.createCollection("products");
}
```

---

## 📈 Statistics

- **Total Tables** (PostgreSQL): 15
- **Total Collections** (MongoDB): 5
- **Total Indexes**: 50+
- **Estimated Row Count**: 10M+
- **Estimated Document Count**: 5M+

**Production-ready schemas with proper indexing, constraints, and security!** 🗄️

