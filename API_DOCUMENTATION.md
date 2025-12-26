# Complete API Documentation

## 📡 API Overview

Complete REST API documentation for all 7 business microservices with 80+ endpoints.

**Base URL**: `https://api.ecommerce.com`

---

## 🔐 Authentication

All endpoints (except public ones) require JWT authentication.

**Header**:
```
Authorization: Bearer <access_token>
```

**Obtaining Access Token**:
```bash
POST /api/auth/login
{
  "email": "user@example.com",
  "password": "password123"
}

Response:
{
  "accessToken": "eyJ...",
  "refreshToken": "abc...",
  "expiresIn": 3600
}
```

---

## 1. Auth Service APIs (Port 9001)

### POST /api/auth/register
Register new user account.

**Request**:
```json
{
  "email": "user@example.com",
  "password": "SecureP@ss123",
  "username": "johndoe",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response** (201 Created):
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "username": "johndoe",
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Errors**:
- 400: Email already exists
- 400: Invalid password (must be 8+ chars, uppercase, lowercase, number, special char)

---

### POST /api/auth/login
Authenticate user and receive JWT tokens.

**Request**:
```json
{
  "email": "user@example.com",
  "password": "SecureP@ss123"
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "abc123def456...",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

**Errors**:
- 401: Invalid credentials
- 423: Account locked (too many failed attempts)
- 403: Account suspended

---

### POST /api/auth/refresh
Refresh access token using refresh token.

**Request**:
```json
{
  "refreshToken": "abc123def456..."
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJnew...",
  "refreshToken": "new123abc...",
  "expiresIn": 3600
}
```

---

### POST /api/auth/logout
Logout user (invalidate tokens).

**Headers**: `Authorization: Bearer <token>`

**Response** (204 No Content)

---

### GET /api/auth/me
Get current authenticated user details.

**Headers**: `Authorization: Bearer <token>`

**Response** (200 OK):
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "username": "johndoe",
  "roles": ["USER"],
  "permissions": ["read:products", "write:orders"]
}
```

---

### POST /api/auth/oauth2/google
Login with Google (OAuth2).

**Request**:
```json
{
  "code": "authorization_code_from_google",
  "redirectUri": "https://yourapp.com/callback"
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "abc...",
  "expiresIn": 3600
}
```

---

### POST /api/auth/2fa/enable
Enable Two-Factor Authentication.

**Headers**: `Authorization: Bearer <token>`

**Response** (200 OK):
```json
{
  "secret": "JBSWY3DPEHPK3PXP",
  "qrCodeUrl": "data:image/png;base64,iVBORw0KGgoAAAANSU...",
  "backupCodes": ["123456", "234567", ...]
}
```

---

### POST /api/auth/2fa/verify
Verify 2FA code during login.

**Request**:
```json
{
  "email": "user@example.com",
  "password": "SecureP@ss123",
  "totpCode": "123456"
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "abc...",
  "expiresIn": 3600
}
```

---

## 2. User Service APIs (Port 9002)

### GET /api/users/me/profile
Get current user profile (CQRS - Read from replica).

**Headers**: `Authorization: Bearer <token>`

**Response** (200 OK):
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1234567890",
  "dateOfBirth": "1990-01-15",
  "addresses": [
    {
      "id": "uuid",
      "type": "SHIPPING",
      "street": "123 Main St",
      "city": "New York",
      "state": "NY",
      "zipCode": "10001",
      "country": "USA",
      "isDefault": true
    }
  ],
  "preferences": {
    "newsletter": true,
    "smsNotifications": false,
    "language": "en",
    "currency": "USD"
  }
}
```

---

### PUT /api/users/me/profile
Update user profile (CQRS - Write to primary).

**Headers**: `Authorization: Bearer <token>`

**Request**:
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1234567890",
  "dateOfBirth": "1990-01-15"
}
```

**Response** (200 OK):
```json
{
  "id": "uuid",
  "message": "Profile updated successfully"
}
```

---

### POST /api/users/me/addresses
Add new address.

**Headers**: `Authorization: Bearer <token>`

**Request**:
```json
{
  "type": "SHIPPING",
  "street": "456 Elm St",
  "city": "Los Angeles",
  "state": "CA",
  "zipCode": "90001",
  "country": "USA",
  "isDefault": false
}
```

**Response** (201 Created):
```json
{
  "id": "uuid",
  "message": "Address added successfully"
}
```

---

### DELETE /api/users/me/addresses/{addressId}
Delete address.

**Headers**: `Authorization: Bearer <token>`

**Response** (204 No Content)

---

## 3. Product Service APIs (Port 9003)

### GET /api/products
List all products (with pagination and filters).

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Page size (default: 20, max: 100)
- `category`: Filter by category
- `minPrice`: Minimum price
- `maxPrice`: Maximum price
- `search`: Search term
- `sort`: Sort field (name, price, createdAt)
- `order`: Sort order (asc, desc)

**Example**:
```
GET /api/products?page=0&size=20&category=electronics&minPrice=100&maxPrice=1000&sort=price&order=asc
```

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": "uuid",
      "name": "Wireless Headphones",
      "description": "High-quality noise-cancelling headphones",
      "price": 299.99,
      "currency": "USD",
      "stockQuantity": 50,
      "category": {
        "id": "uuid",
        "name": "Electronics",
        "slug": "electronics"
      },
      "images": [
        {
          "url": "https://cdn.example.com/products/headphones-1.jpg",
          "isPrimary": true
        }
      ],
      "rating": 4.5,
      "reviewCount": 120,
      "createdAt": "2024-01-15T10:00:00Z"
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "currentPage": 0,
  "size": 20
}
```

---

### GET /api/products/{productId}
Get product details by ID.

**Response** (200 OK):
```json
{
  "id": "uuid",
  "name": "Wireless Headphones",
  "description": "High-quality noise-cancelling headphones with 30-hour battery life",
  "price": 299.99,
  "currency": "USD",
  "stockQuantity": 50,
  "category": {
    "id": "uuid",
    "name": "Electronics",
    "slug": "electronics"
  },
  "specifications": {
    "brand": "AudioTech",
    "color": "Black",
    "weight": "250g",
    "bluetooth": "5.0"
  },
  "images": [...],
  "rating": 4.5,
  "reviewCount": 120
}
```

**Errors**:
- 404: Product not found

---

### POST /api/products
Create new product (Admin only).

**Headers**: 
- `Authorization: Bearer <admin_token>`
- `Content-Type: application/json`

**Request**:
```json
{
  "name": "Smart Watch",
  "description": "Fitness tracking smart watch",
  "price": 199.99,
  "currency": "USD",
  "stockQuantity": 100,
  "categoryId": "uuid",
  "specifications": {
    "brand": "TechBrand",
    "color": "Silver",
    "battery": "7 days"
  }
}
```

**Response** (201 Created):
```json
{
  "id": "uuid",
  "message": "Product created successfully",
  "version": 1
}
```

---

### PUT /api/products/{productId}
Update product (Admin only - Event Sourcing).

**Headers**: `Authorization: Bearer <admin_token>`

**Request**:
```json
{
  "price": 249.99,
  "stockQuantity": 75
}
```

**Response** (200 OK):
```json
{
  "id": "uuid",
  "message": "Product updated successfully",
  "version": 2,
  "event": "ProductUpdatedEvent"
}
```

---

### GET /api/products/{productId}/history
Get product event history (Event Sourcing).

**Headers**: `Authorization: Bearer <admin_token>`

**Response** (200 OK):
```json
{
  "events": [
    {
      "eventId": "uuid",
      "eventType": "ProductCreatedEvent",
      "aggregateId": "product-uuid",
      "version": 1,
      "timestamp": "2024-01-15T10:00:00Z",
      "data": {
        "name": "Smart Watch",
        "price": 199.99,
        "stockQuantity": 100
      }
    },
    {
      "eventId": "uuid",
      "eventType": "PriceChangedEvent",
      "aggregateId": "product-uuid",
      "version": 2,
      "timestamp": "2024-01-16T14:30:00Z",
      "data": {
        "oldPrice": 199.99,
        "newPrice": 249.99
      }
    }
  ]
}
```

---

## 4. Order Service APIs (Port 9004)

### POST /api/orders
Create new order (Saga Pattern).

**Headers**: `Authorization: Bearer <token>`

**Request**:
```json
{
  "items": [
    {
      "productId": "uuid",
      "quantity": 2,
      "price": 299.99
    }
  ],
  "shippingAddressId": "uuid",
  "paymentMethod": "CREDIT_CARD",
  "paymentDetails": {
    "cardNumber": "4111111111111111",
    "expiryMonth": 12,
    "expiryYear": 2025,
    "cvv": "123"
  }
}
```

**Response** (202 Accepted):
```json
{
  "orderId": "uuid",
  "status": "PENDING",
  "message": "Order is being processed",
  "sagaId": "uuid"
}
```

**Saga Steps**:
1. Reserve Inventory (Product Service)
2. Process Payment (Payment Service)
3. Confirm Order
4. Send Notification (Notification Service)

---

### GET /api/orders/{orderId}
Get order details.

**Headers**: `Authorization: Bearer <token>`

**Response** (200 OK):
```json
{
  "id": "uuid",
  "orderNumber": "ORD-2024-001234",
  "status": "CONFIRMED",
  "items": [
    {
      "productId": "uuid",
      "productName": "Wireless Headphones",
      "quantity": 2,
      "unitPrice": 299.99,
      "totalPrice": 599.98
    }
  ],
  "subtotal": 599.98,
  "tax": 53.99,
  "shipping": 15.00,
  "total": 668.97,
  "currency": "USD",
  "shippingAddress": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001"
  },
  "paymentStatus": "COMPLETED",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:31:00Z"
}
```

---

### GET /api/orders/my-orders
Get current user's orders.

**Headers**: `Authorization: Bearer <token>`

**Query Parameters**:
- `page`: Page number
- `size`: Page size
- `status`: Filter by status

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": "uuid",
      "orderNumber": "ORD-2024-001234",
      "status": "CONFIRMED",
      "total": 668.97,
      "itemCount": 2,
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ],
  "totalElements": 25,
  "totalPages": 3,
  "currentPage": 0
}
```

---

### POST /api/orders/{orderId}/cancel
Cancel order (Saga Compensation).

**Headers**: `Authorization: Bearer <token>`

**Response** (200 OK):
```json
{
  "orderId": "uuid",
  "status": "CANCELLED",
  "message": "Order cancelled successfully",
  "refundInitiated": true
}
```

**Compensation Steps**:
1. Refund Payment
2. Release Inventory
3. Update Order Status
4. Send Cancellation Notification

---

## 5. Payment Service APIs (Port 9005)

### POST /api/payments/process
Process payment (with Circuit Breaker).

**Headers**: `Authorization: Bearer <token>`

**Request**:
```json
{
  "orderId": "uuid",
  "amount": 668.97,
  "currency": "USD",
  "paymentMethod": "CREDIT_CARD",
  "cardDetails": {
    "cardNumber": "4111111111111111",
    "expiryMonth": 12,
    "expiryYear": 2025,
    "cvv": "123",
    "cardholderName": "John Doe"
  }
}
```

**Response** (200 OK):
```json
{
  "paymentId": "uuid",
  "status": "COMPLETED",
  "transactionId": "txn_abc123",
  "amount": 668.97,
  "currency": "USD",
  "paymentMethod": "CREDIT_CARD",
  "processedAt": "2024-01-15T10:31:00Z"
}
```

**Errors**:
- 402: Payment declined
- 400: Invalid card details
- 503: Payment gateway unavailable (Circuit breaker OPEN)

---

### POST /api/payments/{paymentId}/refund
Refund payment.

**Headers**: `Authorization: Bearer <token>`

**Request**:
```json
{
  "amount": 668.97,
  "reason": "Customer request"
}
```

**Response** (200 OK):
```json
{
  "refundId": "uuid",
  "status": "COMPLETED",
  "amount": 668.97,
  "processedAt": "2024-01-15T11:00:00Z"
}
```

---

## 6. Notification Service APIs (Port 9006)

### POST /api/notifications/send
Send notification (multithreading).

**Headers**: `Authorization: Bearer <token>`

**Request**:
```json
{
  "userId": "uuid",
  "type": "ORDER_CONFIRMATION",
  "channels": ["EMAIL", "SMS", "PUSH"],
  "data": {
    "orderNumber": "ORD-2024-001234",
    "total": 668.97
  }
}
```

**Response** (202 Accepted):
```json
{
  "notificationId": "uuid",
  "status": "QUEUED",
  "channels": ["EMAIL", "SMS", "PUSH"],
  "message": "Notification queued for processing"
}
```

---

### GET /api/notifications/my-notifications
Get user notifications.

**Headers**: `Authorization: Bearer <token>`

**Query Parameters**:
- `page`: Page number
- `size`: Page size
- `unreadOnly`: boolean

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": "uuid",
      "type": "ORDER_CONFIRMATION",
      "title": "Order Confirmed",
      "message": "Your order ORD-2024-001234 has been confirmed",
      "read": false,
      "createdAt": "2024-01-15T10:31:00Z"
    }
  ],
  "unreadCount": 5,
  "totalElements": 50
}
```

---

### PUT /api/notifications/{notificationId}/read
Mark notification as read.

**Headers**: `Authorization: Bearer <token>`

**Response** (204 No Content)

---

### WebSocket Endpoint
**URL**: `wss://api.ecommerce.com/ws/notifications`

**Connect**:
```javascript
const socket = new SockJS('https://api.ecommerce.com/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({
  'Authorization': 'Bearer ' + accessToken
}, function(frame) {
  stompClient.subscribe('/user/queue/notifications', function(message) {
    const notification = JSON.parse(message.body);
    console.log('Received notification:', notification);
  });
});
```

---

## 7. Review Service APIs (Port 9007)

### POST /api/reviews
Create review (REST API).

**Headers**: `Authorization: Bearer <token>`

**Request**:
```json
{
  "productId": "uuid",
  "rating": 5,
  "comment": "Excellent product! Highly recommended.",
  "verified": true
}
```

**Response** (201 Created):
```json
{
  "id": "uuid",
  "productId": "uuid",
  "userId": "uuid",
  "userName": "John Doe",
  "rating": 5,
  "comment": "Excellent product! Highly recommended.",
  "status": "APPROVED",
  "createdAt": "2024-01-15T12:00:00Z"
}
```

---

### GET /api/reviews/product/{productId}
Get product reviews (with pagination).

**Query Parameters**:
- `page`: Page number
- `size`: Page size
- `sort`: Sort by (rating, createdAt, helpful)

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": "uuid",
      "userId": "uuid",
      "userName": "John Doe",
      "rating": 5,
      "comment": "Excellent product!",
      "helpfulCount": 25,
      "createdAt": "2024-01-15T12:00:00Z"
    }
  ],
  "averageRating": 4.5,
  "totalReviews": 120,
  "ratingDistribution": {
    "5": 70,
    "4": 30,
    "3": 15,
    "2": 3,
    "1": 2
  }
}
```

---

### gRPC APIs (Port 50051)

**Protocol Buffers**:
```protobuf
service ReviewService {
  rpc GetProductRating(ProductRequest) returns (RatingResponse);
  rpc GetReviewsByProduct(ProductRequest) returns (stream ReviewResponse);
}
```

**Usage** (from Product Service):
```java
ProductRequest request = ProductRequest.newBuilder()
    .setProductId(productId)
    .build();

RatingResponse rating = reviewServiceStub.getProductRating(request);
System.out.println("Average Rating: " + rating.getAverageRating());
```

---

## 📊 Common Response Codes

| Code | Meaning | Description |
|------|---------|-------------|
| 200 | OK | Request successful |
| 201 | Created | Resource created |
| 202 | Accepted | Request accepted (async) |
| 204 | No Content | Success, no response body |
| 400 | Bad Request | Invalid request data |
| 401 | Unauthorized | Authentication required |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Resource conflict (duplicate) |
| 422 | Unprocessable Entity | Validation failed |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |
| 502 | Bad Gateway | Upstream service error |
| 503 | Service Unavailable | Service down or circuit breaker open |

---

## 🔒 Rate Limiting

**Limits per API Key/User**:
- Public endpoints: 100 requests/hour
- Authenticated endpoints: 1000 requests/hour
- Admin endpoints: 10000 requests/hour

**Headers** (included in response):
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 950
X-RateLimit-Reset: 1642248000
```

**Exceeded**:
```json
{
  "error": "rate_limit_exceeded",
  "message": "Too many requests. Try again in 15 minutes.",
  "retryAfter": 900
}
```

---

## 🎯 API Versioning

**URI Versioning** (Current):
```
/api/v1/products
/api/v2/products
```

**Header Versioning** (Alternative):
```
Accept: application/vnd.ecommerce.v1+json
Accept: application/vnd.ecommerce.v2+json
```

---

## 📝 Postman Collection

Import this collection: [Download](https://api.ecommerce.com/docs/postman-collection.json)

## 🔗 Interactive API Docs

Swagger UI: https://api.ecommerce.com/swagger-ui.html

---

**Total Endpoints**: 80+
**Services**: 7
**Authentication**: OAuth2 + JWT
**Rate Limiting**: Token Bucket
**Versioning**: URI-based

