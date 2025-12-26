# Postman Setup Guide for Local Testing

## 📦 Quick Setup

### 1. Import the Collection

1. Open **Postman**
2. Click **Import** button (top-left)
3. Select **File** tab
4. Import: `postman/E-commerce-Microservices.postman_collection.json`
5. Import: `postman/Local.postman_environment.json`

### 2. Activate the Local Environment

1. Click the **Environment dropdown** (top-right corner)
2. Select **"Local Environment"**
3. Verify the base URL is set to: `http://localhost:8080`

---

## 🚀 Getting Started

### Step 1: Start Your Services

Before using Postman, make sure your local services are running:

```bash
./start-local.sh
```

This will start all microservices on `localhost:8080`.

### Step 2: Register a New User

1. Navigate to: **Authentication** → **Register**
2. Click **Send**
3. ✅ The test script will automatically save the `userId` to your environment

**Sample Request:**
```json
{
  "email": "john.doe@example.com",
  "password": "SecureP@ss123",
  "username": "johndoe",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Step 3: Login

1. Navigate to: **Authentication** → **Login**
2. Use the same credentials from registration
3. Click **Send**
4. ✅ The JWT token will be **automatically saved** to your environment
5. ✅ All subsequent requests will automatically use this token

**Sample Request:**
```json
{
  "email": "john.doe@example.com",
  "password": "SecureP@ss123"
}
```

### Step 4: Test Protected Endpoints

Now you're authenticated! Try these endpoints:

- **User Service** → **Get My Profile**
- **Product Service** → **List Products**
- **Order Service** → **List My Orders**

---

## 🔑 Authentication Features

### Auto-Refresh Token

The collection includes an **automatic token refresh** mechanism:

- If your access token expires, it will automatically refresh using the refresh token
- No manual intervention needed!
- Check the **Collection Pre-request Script** for details

### Manual Token Refresh

If needed, you can manually refresh your token:

1. Navigate to: **Authentication** → **Refresh Token**
2. Click **Send**
3. New tokens will be saved automatically

### Logout

To logout and clear tokens:

1. Navigate to: **Authentication** → **Logout**
2. Click **Send**
3. Manually clear environment variables if needed

---

## 🧪 Testing Workflows

### Complete Order Flow

Follow this sequence to test a full order lifecycle:

#### 1. Register & Login
- **Authentication** → **Register**
- **Authentication** → **Login**

#### 2. Setup User Profile
- **User Service** → **Add Address**
  - ✅ Saves `testAddressId` automatically

#### 3. Browse Products
- **Product Service** → **List Products**
  - ✅ Saves `testProductId` from first product

#### 4. Create Order
- **Order Service** → **Create Order**
  - Uses saved `testProductId` and `testAddressId`
  - ✅ Saves `testOrderId` automatically

#### 5. Check Order Status
- **Order Service** → **Get Order**
  - Uses saved `testOrderId`

#### 6. Process Payment
- **Payment Service** → **Process Payment**
  - Uses saved `testOrderId`
  - ✅ Saves `testPaymentId` automatically

#### 7. Review Product
- **Review Service** → **Create Review**
  - Uses saved `testProductId`

---

## 📝 Environment Variables

The **Local Environment** tracks these variables automatically:

| Variable | Purpose | Set By |
|----------|---------|--------|
| `baseUrl` | API base URL | Pre-configured |
| `token` | JWT access token | Login request |
| `refreshToken` | JWT refresh token | Login request |
| `tokenExpiry` | Token expiration time | Login request |
| `userId` | Current user ID | Register request |
| `testProductId` | Sample product ID | List/Create Product |
| `testOrderId` | Sample order ID | Create Order |
| `testPaymentId` | Sample payment ID | Process Payment |
| `testAddressId` | Sample address ID | Add Address |
| `testReviewId` | Sample review ID | Create Review |

### View/Edit Variables

1. Click the **👁️ eye icon** next to environment dropdown
2. View current values
3. Edit manually if needed

---

## 🎯 Tips & Best Practices

### 1. Use the Collection Runner

Test multiple endpoints in sequence:

1. Click **Collections** → **...** → **Run collection**
2. Select the folder or entire collection
3. Choose your **Local Environment**
4. Click **Run**
5. View results for all requests

### 2. Check Test Results

Each request has built-in tests:

- ✅ Green = Test passed
- ❌ Red = Test failed
- View details in the **Test Results** tab

### 3. View Console Logs

For debugging:

1. Open **Postman Console** (View → Show Postman Console)
2. See request/response details
3. View console.log outputs from scripts

### 4. Modify Sample Data

Feel free to edit the request bodies:

- Change user details
- Modify product information
- Update order items
- Adjust payment amounts

### 5. Save Responses

Right-click on any request → **Save Response** → **Save as example**

This helps document your API responses.

---

## 🐛 Troubleshooting

### Issue: "Connection refused"

**Solution:**
- Ensure services are running: `./start-local.sh`
- Check if port 8080 is available
- Verify `baseUrl` in environment settings

### Issue: "Unauthorized" errors

**Solution:**
- Re-login using: **Authentication** → **Login**
- Check if token is saved in environment variables
- Verify Collection-level auth is set to use `{{token}}`

### Issue: "Token expired"

**Solution:**
- Use: **Authentication** → **Refresh Token**
- Or logout and login again
- Auto-refresh should handle this automatically

### Issue: "Product/Order not found"

**Solution:**
- Run **Product Service** → **List Products** first
- Check environment variables are populated
- Manually set IDs in environment if needed

### Issue: Variables not saving

**Solution:**
- Check **Test Scripts** tab for each request
- Ensure environment is selected (not "No Environment")
- Verify test scripts are enabled

---

## 🔧 Advanced Features

### Pre-request Scripts

The collection includes automatic token refresh in the pre-request script:

- Checks if token is expired before each request
- Automatically refreshes if needed
- Runs silently in the background

### Response Tests

Each endpoint includes validation tests:

- Status code checks
- Response body validation
- Automatic variable extraction

### Variables Chaining

Requests automatically pass data to subsequent requests:

```
Register → saves userId
Login → saves token
List Products → saves testProductId
Create Order → uses testProductId
```

---

## 📚 Additional Resources

### Related Documentation

- [README.md](README.md) - Project overview
- [AUTH_GUIDE.md](AUTH_GUIDE.md) - Authentication details
- [MULTITHREADING_GUIDE.md](MULTITHREADING_GUIDE.md) - Concurrency guide

### Service Management

- **Start services:** `./start-local.sh`
- **Stop services:** `./stop-local.sh`

### Postman Documentation

- [Postman Learning Center](https://learning.postman.com/)
- [Variables Documentation](https://learning.postman.com/docs/sending-requests/variables/)
- [Test Scripts Guide](https://learning.postman.com/docs/writing-scripts/test-scripts/)

---

## 🎉 Quick Start Checklist

- [ ] Import collection JSON file
- [ ] Import environment JSON file
- [ ] Select "Local Environment" from dropdown
- [ ] Start local services (`./start-local.sh`)
- [ ] Run **Register** request
- [ ] Run **Login** request (saves token automatically)
- [ ] Test any protected endpoint
- [ ] Run complete order flow

---

**Happy Testing! 🚀**

For issues or questions, check the troubleshooting section or review the main [README.md](README.md).

