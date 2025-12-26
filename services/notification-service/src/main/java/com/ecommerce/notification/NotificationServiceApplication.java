package com.ecommerce.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Notification Service Application
 * 
 * <p>Notification delivery service demonstrating advanced multithreading
 * and real-time communication patterns.</p>
 * 
 * <h2>Service Responsibilities:</h2>
 * <ul>
 *   <li>Email Notifications (SMTP)</li>
 *   <li>SMS Notifications (Twilio simulation)</li>
 *   <li>Push Notifications (FCM simulation)</li>
 *   <li>Real-time WebSocket Notifications</li>
 *   <li>In-app Notifications</li>
 *   <li>Notification History & Preferences</li>
 * </ul>
 * 
 * <h2>MULTITHREADING IN JAVA - COMPLETE GUIDE</h2>
 * <pre>
 * What is Multithreading?
 * - Running multiple tasks simultaneously
 * - Better resource utilization
 * - Improved responsiveness
 * - Parallel processing
 * 
 * Why Multithreading for Notifications?
 * 
 * Without Threading (Sequential):
 * User places order →
 *   Send Email (5s) →
 *   Send SMS (3s) →
 *   Send Push (2s) →
 *   Send WebSocket (1s) →
 *   Total: 11 seconds! 😢
 * 
 * With Threading (Parallel):
 * User places order →
 *   ├─ Send Email (5s) ────┐
 *   ├─ Send SMS (3s) ───────┤
 *   ├─ Send Push (2s) ──────┼→ All complete in 5s! 😊
 *   └─ Send WebSocket (1s)──┘
 * 
 * Result: 6 seconds saved!
 * Better user experience!
 * </pre>
 * 
 * <h2>THREAD BASICS</h2>
 * <pre>
 * What is a Thread?
 * - Smallest unit of execution
 * - Independent path of code execution
 * - Shares process memory
 * - Has own stack, program counter
 * 
 * Process vs Thread:
 * 
 * PROCESS (Heavy):
 * ┌──────────────────┐
 * │  Application     │
 * │  ┌────────────┐  │
 * │  │   Memory   │  │ ← Own memory
 * │  └────────────┘  │
 * │  ┌────┐ ┌────┐  │
 * │  │ T1 │ │ T2 │  │ ← Threads share memory
 * │  └────┘ └────┘  │
 * └──────────────────┘
 * 
 * - Creating process: Expensive
 * - Context switch: Slow
 * - Memory: Isolated
 * 
 * THREAD (Light):
 * - Creating thread: Cheap
 * - Context switch: Fast
 * - Memory: Shared
 * 
 * Thread Lifecycle:
 * 
 * NEW → RUNNABLE → RUNNING → TERMINATED
 *          ↓
 *       WAITING
 *       BLOCKED
 * 
 * NEW: Thread created but not started
 * RUNNABLE: Ready to run, waiting for CPU
 * RUNNING: Executing code
 * WAITING: Waiting for another thread
 * BLOCKED: Waiting for lock
 * TERMINATED: Finished execution
 * </pre>
 * 
 * <h2>THREAD POOLS - CRITICAL CONCEPT</h2>
 * <pre>
 * Why Thread Pools?
 * 
 * Without Pool (Bad):
 * Request 1 → Create Thread → Execute → Destroy ╳ Expensive!
 * Request 2 → Create Thread → Execute → Destroy ╳
 * Request 3 → Create Thread → Execute → Destroy ╳
 * 
 * With Pool (Good):
 * 
 * ┌─────────────────────────────────┐
 * │       Thread Pool (10 threads)  │
 * │  ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐     │
 * │  │T1│ │T2│ │T3│ │T4│ │T5│ ... │
 * │  └──┘ └──┘ └──┘ └──┘ └──┘     │
 * └─────────────────────────────────┘
 *           ↑        ↑
 *           │        │
 *    Task Queue: [T6, T7, T8, ...]
 * 
 * Benefits:
 * 1. Reuse threads (no create/destroy cost)
 * 2. Control concurrency (limit threads)
 * 3. Queue management (handle overload)
 * 4. Better performance
 * 
 * Thread Pool Types:
 * 
 * 1. FixedThreadPool(n):
 *    - Fixed number of threads
 *    - Good for: Known workload
 *    - Example: Email service (5 threads)
 * 
 * 2. CachedThreadPool:
 *    - Creates threads as needed
 *    - Reuses idle threads
 *    - Good for: Short-lived tasks
 *    - Example: Quick notifications
 * 
 * 3. ScheduledThreadPool(n):
 *    - Schedule tasks for future
 *    - Periodic execution
 *    - Good for: Cron jobs, reminders
 *    - Example: Daily digest emails
 * 
 * 4. SingleThreadExecutor:
 *    - Only 1 thread
 *    - Tasks execute sequentially
 *    - Good for: Order matters
 *    - Example: Audit logs
 * 
 * 5. WorkStealingPool:
 *    - Uses all CPU cores
 *    - Threads steal work from each other
 *    - Good for: CPU-intensive tasks
 * </pre>
 * 
 * <h2>SPRING @Async - SIMPLE MULTITHREADING</h2>
 * <pre>
 * What is @Async?
 * - Spring annotation for async execution
 * - Method runs in separate thread
 * - Caller doesn't wait
 * 
 * Without @Async (Synchronous):
 * 
 * controller.sendNotification() {
 *     service.sendEmail();      // Wait 5s
 *     service.sendSMS();        // Wait 3s
 *     service.sendPush();       // Wait 2s
 *     return "Success";         // Total: 10s
 * }
 * 
 * User waits 10 seconds! 😢
 * 
 * With @Async (Asynchronous):
 * 
 * controller.sendNotification() {
 *     service.sendEmail();      // Fire and forget
 *     service.sendSMS();        // Fire and forget
 *     service.sendPush();       // Fire and forget
 *     return "Success";         // Immediate!
 * }
 * 
 * User waits < 1 second! 😊
 * Notifications sent in background
 * 
 * Configuration:
 * 
 * @EnableAsync ← Enable async support
 * 
 * @Async("emailExecutor")
 * public void sendEmail(...) {
 *     // Runs in email thread pool
 * }
 * 
 * @Async("smsExecutor")
 * public void sendSMS(...) {
 *     // Runs in SMS thread pool
 * }
 * 
 * Benefits:
 * 1. Non-blocking operations
 * 2. Better response times
 * 3. Parallel execution
 * 4. Easy to use (just add @Async!)
 * </pre>
 * 
 * <h2>CompletableFuture - ADVANCED ASYNC</h2>
 * <pre>
 * What is CompletableFuture?
 * - Java 8+ async programming
 * - Represents future result
 * - Can chain operations
 * - Can combine multiple futures
 * 
 * Example 1: Simple Async
 * 
 * CompletableFuture<String> future = 
 *     CompletableFuture.supplyAsync(() -> {
 *         // Runs in separate thread
 *         return sendEmail();
 *     });
 * 
 * String result = future.get(); // Wait for result
 * 
 * Example 2: Chaining
 * 
 * CompletableFuture.supplyAsync(() -> sendEmail())
 *     .thenApply(result -> logResult(result))
 *     .thenAccept(log -> saveToDatabase(log))
 *     .exceptionally(ex -> handleError(ex));
 * 
 * Example 3: Parallel Execution
 * 
 * CompletableFuture<Void> email = 
 *     CompletableFuture.runAsync(() -> sendEmail());
 * 
 * CompletableFuture<Void> sms = 
 *     CompletableFuture.runAsync(() -> sendSMS());
 * 
 * CompletableFuture<Void> push = 
 *     CompletableFuture.runAsync(() -> sendPush());
 * 
 * // Wait for ALL to complete
 * CompletableFuture.allOf(email, sms, push).join();
 * 
 * Example 4: First Winner
 * 
 * CompletableFuture<String> gateway1 = 
 *     CompletableFuture.supplyAsync(() -> sendViaGateway1());
 * 
 * CompletableFuture<String> gateway2 = 
 *     CompletableFuture.supplyAsync(() -> sendViaGateway2());
 * 
 * // Use whichever completes first
 * String result = CompletableFuture.anyOf(gateway1, gateway2)
 *     .get();
 * </pre>
 * 
 * <h2>VIRTUAL THREADS (Java 21+) - THE FUTURE</h2>
 * <pre>
 * What are Virtual Threads?
 * - Lightweight threads
 * - Managed by JVM (not OS)
 * - Millions of threads possible
 * - No context switching overhead
 * 
 * Traditional Threads:
 * - 1 Java Thread = 1 OS Thread
 * - Limited: ~5,000 threads max
 * - Each thread: ~1 MB memory
 * - Context switch: Expensive
 * 
 * Virtual Threads:
 * - 1 OS Thread = Many Virtual Threads
 * - Unlimited: Millions possible
 * - Each thread: Few KB memory
 * - Context switch: Cheap
 * 
 * Comparison:
 * 
 * Traditional:
 * 10,000 requests → 10,000 threads → OUT OF MEMORY!
 * 
 * Virtual:
 * 10,000 requests → 10,000 virtual threads → No problem!
 * 
 * Usage:
 * 
 * // Old way (platform threads)
 * Thread thread = new Thread(() -> {
 *     // Heavy
 * });
 * 
 * // New way (virtual threads)
 * Thread thread = Thread.ofVirtual().start(() -> {
 *     // Lightweight!
 * });
 * 
 * // With executor
 * ExecutorService executor = 
 *     Executors.newVirtualThreadPerTaskExecutor();
 * </pre>
 * 
 * <h2>WEBSOCKET - REAL-TIME COMMUNICATION</h2>
 * <pre>
 * What is WebSocket?
 * - Full-duplex communication protocol
 * - Persistent connection
 * - Real-time bidirectional data
 * 
 * HTTP vs WebSocket:
 * 
 * HTTP (Request-Response):
 * Client → Request  → Server
 * Client ← Response ← Server
 * [Connection closed]
 * Repeat for each message (overhead!)
 * 
 * WebSocket (Persistent):
 * Client ←──────────→ Server
 *        Handshake
 * Client ←──────────→ Server
 *     Real-time messages
 * [Connection stays open]
 * 
 * Why WebSocket for Notifications?
 * 
 * Polling (Bad):
 * Every 5 seconds:
 * Client: "Any new notifications?"
 * Server: "No"
 * Client: "Any new notifications?"
 * Server: "No"
 * Client: "Any new notifications?"
 * Server: "Yes! Here: order shipped"
 * 
 * Problems:
 * - Many unnecessary requests
 * - Server load high
 * - Battery drain (mobile)
 * - Delayed updates (5s interval)
 * 
 * WebSocket (Good):
 * [Connection established]
 * ... silence ...
 * Server: "Order shipped!" → Client
 * ... silence ...
 * Server: "Payment received!" → Client
 * 
 * Benefits:
 * - No unnecessary requests
 * - Low server load
 * - Battery efficient
 * - Instant updates (< 100ms)
 * 
 * STOMP Protocol:
 * - Simple Text Oriented Messaging Protocol
 * - Works over WebSocket
 * - Publish/Subscribe pattern
 * - Topic-based routing
 * 
 * Architecture:
 * 
 * Client 1 ────┐
 * Client 2 ────┼─→ /topic/notifications → All subscribed clients
 * Client 3 ────┘
 * 
 * Client A ───→ /user/alice/queue → Only Alice
 * Client B ───→ /user/bob/queue   → Only Bob
 * </pre>
 * 
 * <h2>NOTIFICATION SERVICE ARCHITECTURE</h2>
 * <pre>
 * Event Flow:
 * 
 * 1. Order Service: Order created
 *    ↓
 * 2. Kafka: order.created event
 *    ↓
 * 3. Notification Service: Consume event
 *    ↓
 * 4. Parallel Execution (Thread Pools):
 *    ├─ Email Thread Pool → Send email
 *    ├─ SMS Thread Pool → Send SMS
 *    ├─ Push Thread Pool → Send push notification
 *    └─ WebSocket Thread → Send real-time notification
 *    ↓
 * 5. Save notification history (MongoDB)
 *    ↓
 * 6. User receives all notifications simultaneously!
 * 
 * Thread Pool Configuration:
 * 
 * ┌─────────────────────────────────┐
 * │   Email Executor (5 threads)    │ ← Slow operations
 * ├─────────────────────────────────┤
 * │   SMS Executor (3 threads)      │ ← Medium speed
 * ├─────────────────────────────────┤
 * │   Push Executor (10 threads)    │ ← Fast operations
 * ├─────────────────────────────────┤
 * │   Scheduled Executor (2 threads)│ ← Cron jobs
 * └─────────────────────────────────┘
 * 
 * Total: 20 threads (vs 1000+ requests)
 * Efficient resource usage!
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication
@EnableDiscoveryClient  // Register with Eureka
@EnableKafka  // Enable Kafka consumers
@EnableAsync  // Enable @Async support
@EnableScheduling  // Enable @Scheduled support
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}

