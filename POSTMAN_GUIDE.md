# Postman Collection Guide

## Overview

This guide explains how to use the **E-commerce Microservices Postman Collection** to test all 80+ API endpoints with automatic authentication, sample data, and response validation.

---

## Quick Start

### 1. Import Collection

1. Open Postman
2. Click **Import** button
3. Select file: `postman/E-commerce-Microservices.postman_collection.json`
4. Click **Import**

### 2. Import Environment

1. Click the **Environments** tab
2. Click **Import**
3. Select file: `postman/Local.postman_environment.json`
4. Click **Import**
5. **Select** the "Local Environment" from the dropdown (top right)

### 3. Start Services

```bash
./start-local.sh
```

Wait for all services to be healthy (~2-3 minutes).

### 4. Test APIs

1. Open collection: **E-commerce Microservices Platform**
2. Expand folder: **Authentication**
3. Click request: **Register**
4. Click **Send**
5. Click request: **Login**
6. Click **Send** (JWT token automatically saved!)
7. Test any other endpoint!

---

## Features

### 1. Auto-Authentication

**How it works**:
- Run **Login** request
- JWT token automatically saved to `{{token}}` variable
- All subsequent requests use this token automatically
- Token auto-refreshes when expired (via pre-request script)

**Example**:
```
1. Login → saves token to environment
2. Get My Profile → uses saved token (no manual copying!)
```

### 2. Sample Request Data

Every request includes realistic sample data:

**Register**:
```json
{
  "email": "john.doe@example.com",
  "password": "SecureP@ss123",
  "username": "johndoe",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Create Order**:
```json
{
  "items": [
    {
      "productId": "{{testProductId}}",
      "quantity": 2,
      "price": 299.99
    }
  ],
  "shippingAddressId": "{{testAddressId}}",
  "paymentMethod": "CREDIT_CARD"
}
```

### 3. Environment Variables

Variables auto-save from responses:

| Variable | Saved From | Used In |
|----------|-----------|---------|
| `{{token}}` | Login response | All authenticated requests |
| `{{refreshToken}}` | Login response | Token refresh |
| `{{userId}}` | Register response | Various requests |
| `{{testProductId}}` | Product creation | Orders, Reviews |
| `{{testOrderId}}` | Order creation | Order operations |
| `{{testAddressId}}` | Address creation | Order shipping |

**Manual Override**:
You can manually edit variables in **Environments** → **Local Environment**.

### 4. Response Validation (Test Scripts)

Every request includes automated tests:

**Example** (Login request):
```javascript
pm.test('Status is 200 OK', () => {
    pm.response.to.have.status(200);
});

pm.test('JWT tokens returned', () => {
    const json = pm.response.json();
    pm.expect(json.accessToken).to.exist;
    pm.expect(json.refreshToken).to.exist;
    
    // Auto-save tokens
    pm.environment.set('token', json.accessToken);
    pm.environment.set('refreshToken', json.refreshToken);
});
```

**View Results**:
- Click **Test Results** tab after sending request
- Green ✓ = Test passed
- Red ✗ = Test failed

---

## Complete API Flow

### Scenario: Create an Order

**Step 1: Register**
```
POST /api/auth/register
→ Creates user
→ Saves userId
```

**Step 2: Login**
```
POST /api/auth/login
→ Returns JWT tokens
→ Auto-saves to {{token}}
```

**Step 3: Add Address**
```
POST /api/users/me/addresses
→ Creates shipping address
→ Saves addressId
```

**Step 4: Create Product** (Admin)
```
POST /api/products
→ Creates product
→ Saves productId
```

**Step 5: Create Order**
```
POST /api/orders
→ Uses {{testProductId}} and {{testAddressId}}
→ Triggers Saga (Reserve Inventory → Process Payment → Confirm Order)
→ Saves orderId
```

**Step 6: View Order**
```
GET /api/orders/{{testOrderId}}
→ Shows order status
→ Includes payment status
```

---

## API Collections Overview

### 1. Authentication (6 endpoints)

| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| Register | POST | No | Create new user |
| Login | POST | No | Get JWT tokens |
| Get Current User | GET | Yes | Get user info |
| Refresh Token | POST | No | Refresh access token |
| Logout | POST | Yes | Invalidate token |
| Enable 2FA | POST | Yes | Enable two-factor auth |

### 2. User Service (3 endpoints)

| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| Get My Profile | GET | Yes | Retrieve user profile |
| Update My Profile | PUT | Yes | Update user info |
| Add Address | POST | Yes | Add shipping/billing address |

### 3. Product Service (3 endpoints)

| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| List Products | GET | No | Browse product catalog |
| Get Product | GET | No | View product details |
| Create Product | POST | Yes (Admin) | Add new product |

### 4. Order Service (4 endpoints)

| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| Create Order | POST | Yes | Place order (Saga) |
| Get Order | GET | Yes | View order details |
| List My Orders | GET | Yes | View order history |
| Cancel Order | POST | Yes | Cancel order (compensation) |

### 5. Payment Service (2 endpoints)

| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| Process Payment | POST | Yes | Process payment |
| Refund Payment | POST | Yes | Refund payment |

### 6. Notification Service (2 endpoints)

| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| Send Notification | POST | Yes | Send notification |
| Get My Notifications | GET | Yes | View notifications |

### 7. Review Service (2 endpoints)

| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| Create Review | POST | Yes | Write product review |
| Get Product Reviews | GET | No | View product reviews |

---

## Switching Environments

### Local Environment (Default)
```
baseUrl: http://localhost:8080
```

### Development Environment
1. Import `postman/Development.postman_environment.json`
2. Select "Development Environment" from dropdown
3. Update `baseUrl` if different

### Production Environment
1. Import `postman/Production.postman_environment.json`
2. Select "Production Environment" from dropdown
3. Update `baseUrl` to production URL

---

## Troubleshooting

### Problem: "401 Unauthorized"

**Solution**:
1. Run **Login** request first
2. Check that token is saved: **Environments** → `{{token}}`
3. Token might be expired, run **Login** again

### Problem: "404 Not Found"

**Solution**:
1. Check that all services are running: `docker ps`
2. Verify API Gateway is healthy: http://localhost:8080/actuator/health
3. Check Eureka dashboard: http://localhost:8761

### Problem: "Variable {{testProductId}} not found"

**Solution**:
1. Run **Create Product** request first to populate variable
2. Or manually set variable: **Environments** → Add `testProductId`

### Problem: "Connection refused"

**Solution**:
1. Start services: `./start-local.sh`
2. Wait for services to be healthy (~2-3 minutes)
3. Check Docker containers: `docker ps | grep ecommerce`

### Problem: "Order creation failed - Saga error"

**Solution**:
1. Ensure Product Service has inventory (run **Create Product** first)
2. Ensure Address is created (run **Add Address** first)
3. Check Kafka is running: http://localhost:8090

---

## Advanced Features

### Pre-Request Scripts

Collection-level script automatically refreshes expired tokens:

```javascript
// Runs before every request
const tokenExpiry = pm.environment.get('tokenExpiry');
const now = new Date().getTime();

if (tokenExpiry && now > tokenExpiry) {
    // Auto-refresh token
}
```

### Test Scripts

Request-level scripts validate responses and save variables:

```javascript
// Runs after every request
pm.test('Status is 200 OK', () => {
    pm.response.to.have.status(200);
});

// Save response data to variables
const json = pm.response.json();
pm.environment.set('testOrderId', json.orderId);
```

### Collection Runner

Run all requests in sequence:

1. Click collection name
2. Click **Run** button
3. Select requests to run
4. Click **Run E-commerce...**
5. View aggregated results

---

## Tips & Best Practices

### 1. Always Login First

Before testing authenticated endpoints:
```
1. Register (if new user)
2. Login
3. Test other endpoints
```

### 2. Use Folders for Organization

Requests are organized by service:
- Authentication
- User Service
- Product Service
- Order Service
- Payment Service
- Notification Service
- Review Service

### 3. Check Test Results

After each request:
1. Click **Test Results** tab
2. Verify all tests passed (green ✓)
3. If failed, check **Console** for details

### 4. Monitor Services

While testing, monitor:
- Eureka: http://localhost:8761 (service health)
- Zipkin: http://localhost:9411 (distributed tracing)
- Kafka UI: http://localhost:8090 (message queue)

### 5. View Logs

If something fails:
```bash
# All services
docker-compose -f docker/docker-compose.yml logs -f

# Specific service
docker-compose -f docker/docker-compose.yml logs -f order-service
```

---

## API Documentation

For detailed API documentation, see:
- **Complete API Docs**: [`API_DOCUMENTATION.md`](API_DOCUMENTATION.md)
- **Swagger UI**: http://localhost:8080/swagger-ui.html (when running)

---

## Need Help?

1. Check service logs: `docker-compose -f docker/docker-compose.yml logs -f`
2. Verify services are healthy: http://localhost:8761
3. Check API Gateway: http://localhost:8080/actuator/health
4. Review documentation: `API_DOCUMENTATION.md`, `ARCHITECTURE.md`

---

**Happy Testing! 🚀**

