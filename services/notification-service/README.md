# Notification Service

## Overview

The Notification Service demonstrates **advanced multithreading** and **real-time WebSocket communication** patterns. It handles multi-channel notifications (Email, SMS, Push, WebSocket) with parallel execution for optimal performance.

## 🎯 Core Concepts Demonstrated

### 1. Multithreading with @Async 🔀

**What**: Run methods asynchronously in separate threads.

**Sequential vs Async**:
```
Sequential (Bad):
User clicks "Place Order"
  → Send Email (5 seconds)
  → Send SMS (2 seconds)
  → Send Push (1 second)
  → Return response
Total: 8+ seconds 😢

Async (Good):
User clicks "Place Order"
  → Queue Email (< 1ms)
  → Queue SMS (< 1ms)
  → Queue Push (< 1ms)
  → Return response immediately!
Total: < 10ms 😊

Background threads send notifications
User doesn't wait!
```

**Code Example**:
```java
// Without @Async
public void sendEmail() {
    // Blocks for 5 seconds
    smtpClient.send(email);
}

// With @Async
@Async("emailExecutor")
public void sendEmail() {
    // Runs in separate thread
    // Caller returns immediately
    smtpClient.send(email);
}
```

**Benefits**:
- Non-blocking operations
- Better user experience
- Parallel execution
- Resource efficient

### 2. Thread Pools 🏊

**Why Thread Pools**?
```
Without Pool (Bad):
Request 1 → Create Thread (expensive) → Execute → Destroy
Request 2 → Create Thread → Execute → Destroy
Request 3 → Create Thread → Execute → Destroy

Each thread creation: ~1ms + 1MB memory
1000 requests = 1000 threads = OUT OF MEMORY!

With Pool (Good):
Create 10 threads once
Request 1 → Pick thread from pool → Execute → Return to pool
Request 2 → Pick thread from pool → Execute → Return to pool
Request 3 → Pick thread from pool → Execute → Return to pool

Thread reuse: No creation cost
1000 requests = 10 threads = Efficient!
```

**Our Thread Pools**:
```java
@Bean(name = "emailExecutor")
public Executor emailExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);        // 5 threads always alive
    executor.setMaxPoolSize(10);        // Scale up to 10 threads
    executor.setQueueCapacity(100);     // Queue 100 emails
    executor.setThreadNamePrefix("email-");
    return executor;
}
```

**Thread Pool Types**:

1. **Email Pool** (5-10 threads)
   - Slow operations (SMTP takes 3-5s)
   - Small pool because I/O bound

2. **SMS Pool** (3-8 threads)
   - Medium speed (API call 1-2s)

3. **Push Pool** (10-20 threads)
   - Fast operations (Firebase ~500ms)
   - Larger pool for high throughput

4. **Scheduled Pool** (2 threads)
   - Background jobs, cron tasks

### 3. CompletableFuture - Parallel Execution 🚀

**Sequential vs Parallel**:
```
Sequential:
Email (5s) → SMS (2s) → Push (1s) = 8 seconds total

Timeline:
0s ─[Email]─ 5s ─[SMS]─ 7s ─[Push]─ 8s
    ████████    ███        ██

Parallel:
Email (5s) ┐
SMS (2s)   ├→ All start at same time!
Push (1s)  ┘

Timeline:
0s ─[Email]─────── 5s ← Total time
    ████████████████
0s ─[SMS]── 2s
    ████████
0s ─[Push] 1s
    ████

Result: 5 seconds (3 seconds saved, 37.5% faster!)
```

**Code Example**:
```java
// Start all in parallel
CompletableFuture<Boolean> emailFuture = 
    emailService.sendEmailWithFuture(...);

CompletableFuture<Boolean> smsFuture = 
    smsService.sendSmsWithFuture(...);

CompletableFuture<Boolean> pushFuture = 
    pushService.sendPushWithFuture(...);

// Wait for ALL to complete
CompletableFuture.allOf(emailFuture, smsFuture, pushFuture)
    .join();

// Check results
boolean emailSuccess = emailFuture.join();
boolean smsSuccess = smsFuture.join();
boolean pushSuccess = pushFuture.join();
```

**Real-World Impact**:
```
100 orders/minute:

Sequential: 100 × 8s = 800 seconds = 13.3 minutes
Parallel: 100 × 5s = 500 seconds = 8.3 minutes

Saved: 5 minutes every minute!
```

### 4. WebSocket - Real-Time Communication 📡

**HTTP Polling vs WebSocket**:

```
HTTP Polling (Old Way):
Every 5 seconds:
Client: "Any new notifications?" → Server: "No"
Client: "Any new notifications?" → Server: "No"
Client: "Any new notifications?" → Server: "No"
Client: "Any new notifications?" → Server: "Yes! (delayed 5s)"

Problems:
- 720 requests/hour per user!
- High server load
- Battery drain
- Latency (up to 5s)
- Wasted bandwidth

WebSocket (New Way):
[Connection established once]
... silence (no requests) ...
Server → "New notification!" → Client (< 100ms)
... silence ...
Server → "Order shipped!" → Client (< 100ms)

Benefits:
- 2 requests/hour (handshake + close)
- Low server load
- Battery efficient
- Real-time (< 100ms)
- Efficient bandwidth
```

**WebSocket Flow**:
```
1. Client connects:
   var socket = new SockJS('http://localhost:8086/ws');
   var stompClient = Stomp.over(socket);

2. Client subscribes:
   stompClient.subscribe('/user/queue/notifications', function(msg) {
       alert("New notification: " + msg.body);
   });

3. Server sends:
   messagingTemplate.convertAndSendToUser(
       userId, "/queue/notifications", notification);

4. Client receives instantly:
   Callback triggered → Show notification
```

### 5. STOMP Protocol 📮

**What is STOMP**?
- Simple Text Oriented Messaging Protocol
- Like HTTP for WebSocket
- Provides structure and routing

**Destinations**:
```
1. /topic/* (Broadcast):
   Server → /topic/announcements
            ↓
       ┌────┼────┐
       ↓    ↓    ↓
   User1 User2 User3
   ALL receive

2. /user/*/queue/* (Point-to-Point):
   Server → /user/123/queue/notifications
            ↓
         User123
   ONLY User123 receives

3. /app/* (Application):
   Client → /app/markAsRead
            ↓
   @MessageMapping("/markAsRead")
   Server handles
```

### 6. Event-Driven Architecture 🎪

**Synchronous vs Event-Driven**:

```
Synchronous (Coupled):
Order Service ──HTTP──→ Notification Service

Problems:
- Coupling (Order knows about Notification)
- Failure propagation (Notification down = Order fails)
- Blocking (Order waits)

Event-Driven (Decoupled):
Order Service → Kafka Event → Notification Service

Benefits:
- Decoupling (Order doesn't know about Notification)
- Resilience (Event queued if Notification down)
- Non-blocking (Order completes immediately)
- Scalability (Multiple consumers)
```

**Event Flow**:
```
1. User places order
2. Order Service creates order
3. Order Service publishes "order.created" to Kafka
4. Order Service returns success (10ms)
5. Notification Service consumes event
6. Notification Service sends all notifications (parallel)
7. User receives notifications

Timeline:
0ms: Order created
10ms: Event published
20ms: User gets response ← FAST!
100ms: Event consumed
5000ms: All notifications sent
```

## 🏗️ Architecture

### System Design

```
Kafka Topics
    ↓
order.created → [Notification Consumer]
order.shipped            ↓
order.delivered   [NotificationOrchestrator]
                         ↓
        ┌────────────────┼────────────────┐
        ↓                ↓                ↓
    [Email Pool]     [SMS Pool]      [Push Pool]
    (5 threads)      (3 threads)     (10 threads)
        ↓                ↓                ↓
    SMTP Server      Twilio API      Firebase
    (5 seconds)      (2 seconds)     (500ms)
        ↓                ↓                ↓
    User Email       User SMS        User Phone

[WebSocket Service]
        ↓
    Connected Clients
    (Real-time, < 100ms)
```

### Database Schema (MongoDB)

```javascript
{
  _id: ObjectId("..."),
  userId: 123,
  recipient: "user@email.com",
  type: "EMAIL",              // EMAIL, SMS, PUSH, IN_APP, WEBSOCKET
  priority: "NORMAL",         // LOW, NORMAL, HIGH, URGENT
  status: "SENT",             // PENDING, SENT, FAILED, DELIVERED, READ
  subject: "Order Confirmation",
  message: "Your order has been confirmed!",
  data: {...},                // Additional metadata
  templateId: "order-confirm",
  retryCount: 0,
  errorMessage: null,
  threadName: "email-1",      // Which thread processed
  processingTimeMs: 4523,     // Performance tracking
  sentAt: ISODate("..."),
  deliveredAt: ISODate("..."),
  readAt: ISODate("..."),
  createdAt: ISODate("..."),
  updatedAt: ISODate("...")
}
```

## 🚀 API Endpoints

### 1. Send All Notifications (Parallel)

**POST** `/api/notifications/send-all`

**Request**:
```json
{
  "userId": 123,
  "email": "user@email.com",
  "phone": "+1234567890",
  "deviceToken": "firebase-token-123",
  "subject": "Order Confirmation",
  "message": "Your order has been confirmed!"
}
```

**Response**: `200 OK` "Notifications sent"

**What Happens**:
```
1. Request received (< 1ms)
2. All services start in parallel:
   - Email queued → email pool
   - SMS queued → SMS pool
   - Push queued → push pool
   - WebSocket sent immediately
3. Response returned (< 10ms)
4. Background threads send notifications
5. User gets instant response!
```

### 2. Send Email Only

**POST** `/api/notifications/email`

### 3. Send SMS Only

**POST** `/api/notifications/sms`

### 4. Send Push Only

**POST** `/api/notifications/push`

### 5. Send WebSocket

**POST** `/api/notifications/websocket`

### 6. Broadcast to All Users

**POST** `/api/notifications/broadcast`

### 7. Get User Notifications

**GET** `/api/notifications/user/{userId}?page=0&size=20`

## 🔌 WebSocket API

### Connect

```javascript
// Create WebSocket connection
const socket = new SockJS('http://localhost:8086/ws');
const stompClient = Stomp.over(socket);

// Connect
stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // Subscribe to personal notifications
    stompClient.subscribe('/user/queue/notifications', function(message) {
        const notification = JSON.parse(message.body);
        showNotification(notification);
    });
    
    // Subscribe to broadcasts
    stompClient.subscribe('/topic/announcements', function(message) {
        const announcement = JSON.parse(message.body);
        showAnnouncement(announcement);
    });
});
```

### Send Message

```javascript
// Mark notification as read
stompClient.send("/app/markAsRead", {}, 
    JSON.stringify({notificationId: "123"}));
```

### Disconnect

```javascript
stompClient.disconnect();
```

## 🧪 Testing Multithreading

### Test Sequential vs Parallel

```bash
# Sequential (slow)
curl -X POST http://localhost:8086/api/notifications/email -d '{...}'
curl -X POST http://localhost:8086/api/notifications/sms -d '{...}'
curl -X POST http://localhost:8086/api/notifications/push -d '{...}'
# Total: ~8 seconds

# Parallel (fast)
curl -X POST http://localhost:8086/api/notifications/send-all -d '{...}'
# Total: ~5 seconds
```

### Observe Thread Pools

```bash
# Check thread dump
curl http://localhost:8086/actuator/threaddump

# Look for:
# - email-1, email-2, ... (email threads)
# - sms-1, sms-2, ... (SMS threads)
# - push-1, push-2, ... (push threads)

# All running in parallel!
```

### Load Testing

```bash
# Send 100 notifications simultaneously
for i in {1..100}; do
  curl -X POST http://localhost:8086/api/notifications/send-all \
    -H "Content-Type: application/json" \
    -d '{"userId":'$i', ...}' &
done

# Observe:
# - Thread pools handle load efficiently
# - No out-of-memory errors
# - Requests queue properly
# - Thread reuse (no constant creation)
```

## 📊 Performance Comparison

### Sequential Execution

```
Single Request:
Email: 5s + SMS: 2s + Push: 1s = 8 seconds

100 Requests:
100 × 8s = 800 seconds = 13.3 minutes

1000 Users/hour:
1000 × 8s = 8000 seconds = 2.2 hours
```

### Parallel Execution

```
Single Request:
max(Email:5s, SMS:2s, Push:1s) = 5 seconds

100 Requests:
With thread pools (20 total threads):
~5-6 seconds (parallel processing)

1000 Users/hour:
With proper threading: ~1-2 minutes
```

**Improvement**: **95% faster!**

## 🎓 Learning Points

### 1. Why Separate Thread Pools?

**Problem with Single Pool**:
```
Single pool (10 threads):
100 emails arrive (slow, 5s each)
→ All 10 threads blocked for 5s
→ SMS request arrives
→ No threads available!
→ SMS must wait
→ Bad user experience
```

**Solution: Separate Pools**:
```
Email pool: 5 threads
SMS pool: 3 threads
Push pool: 10 threads

100 emails arrive
→ Only email pool affected
→ SMS pool still available
→ SMS sent immediately
→ Good user experience!
```

### 2. When to Use @Async vs CompletableFuture?

**@Async (Fire and Forget)**:
```java
@Async
public void sendEmail() {
    // Don't care about result
    // Just send and forget
}
```

**CompletableFuture (Need Result)**:
```java
@Async
public CompletableFuture<Boolean> sendEmail() {
    // Can wait for result
    // Can chain operations
    // Better error handling
    return CompletableFuture.supplyAsync(...);
}
```

### 3. WebSocket Best Practices

1. **Heartbeat/Ping-Pong**: Keep connection alive
2. **Reconnection Logic**: Handle disconnects gracefully
3. **Message Acknowledgment**: Confirm delivery
4. **Authentication**: Secure connections
5. **Rate Limiting**: Prevent spam
6. **Compression**: Reduce bandwidth

### 4. Thread Pool Sizing

**CPU-Bound Tasks** (calculations):
```
Optimal threads = Number of CPU cores
8-core CPU → 8 threads
```

**I/O-Bound Tasks** (network, database):
```
Optimal threads = Cores × (1 + Wait/CPU time)

Email: 95% waiting for SMTP
8 cores × (1 + 19) = 160 threads

But we use 5-10 because:
- SMTP limits connections
- Memory constraints
- Diminishing returns
```

### 5. Real-World Scenarios

**Scenario 1: Black Friday Sale**
```
10,000 orders/minute
Sequential: 10,000 × 8s = 133 minutes
Parallel: Handled in real-time with thread pools
```

**Scenario 2: Network Issue**
```
Without Retry:
Email fails → User not notified → Bad experience

With Retry:
Email fails → Retry 3 times → Success → Good experience
```

**Scenario 3: SMTP Server Down**
```
Without Queue:
All emails fail → Lost

With Kafka:
Events queued → Service recovers → Process queue → No loss
```

## 🏃 Running the Service

### Prerequisites

- MongoDB running on port 27017
- Kafka running on port 9092
- Eureka Server running on port 8761

### Build & Run

```bash
# Build
./gradlew :services:notification-service:build

# Run
./gradlew :services:notification-service:bootRun

# Or with Docker
docker-compose up notification-service
```

### Test WebSocket

Open `test-websocket.html` in browser:

```html
<!DOCTYPE html>
<html>
<head>
    <title>WebSocket Test</title>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
</head>
<body>
    <h1>WebSocket Notifications</h1>
    <div id="notifications"></div>
    
    <script>
        var socket = new SockJS('http://localhost:8086/ws');
        var stompClient = Stomp.over(socket);
        
        stompClient.connect({}, function(frame) {
            console.log('Connected: ' + frame);
            
            stompClient.subscribe('/user/queue/notifications', function(message) {
                var notification = JSON.parse(message.body);
                document.getElementById('notifications').innerHTML += 
                    '<p>' + notification.title + ': ' + notification.message + '</p>';
            });
        });
    </script>
</body>
</html>
```

## 📚 Further Reading

- [CompletableFuture Guide](https://www.baeldung.com/java-completablefuture)
- [Spring @Async](https://spring.io/guides/gs/async-method/)
- [WebSocket vs HTTP](https://ably.com/topic/websockets-vs-http)
- [Thread Pool Best Practices](https://www.baeldung.com/thread-pool-java-and-guava)

---

**Next Service**: Review Service (Phase 8) - gRPC Communication

