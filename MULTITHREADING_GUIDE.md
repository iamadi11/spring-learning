# Complete Multithreading & Concurrency Guide

## 🧵 Overview

This comprehensive guide covers **multithreading fundamentals, thread pools, async programming, CompletableFuture, Virtual Threads (Java 21+), and synchronization** with real-world examples from the Notification Service.

---

## 📚 Table of Contents

1. [Thread Fundamentals](#1-thread-fundamentals)
2. [Thread Pools & ExecutorService](#2-thread-pools--executorservice)
3. [Spring @Async](#3-spring-async)
4. [CompletableFuture](#4-completablefuture)
5. [Virtual Threads (Java 21+)](#5-virtual-threads-java-21)
6. [Synchronization](#6-synchronization)
7. [Thread-Safe Collections](#7-thread-safe-collections)
8. [Real-World Examples](#8-real-world-examples)
9. [Best Practices](#9-best-practices)
10. [Common Pitfalls](#10-common-pitfalls)

---

## 1. Thread Fundamentals

### What is a Thread?

**Thread**: Smallest unit of execution within a process

**Process vs Thread**:
```
Process = Program in execution
  ├─ Thread 1 (main)
  ├─ Thread 2 (worker)
  └─ Thread 3 (worker)
  
All threads share:
  ├─ Memory (heap)
  ├─ Code
  └─ Resources

Each thread has own:
  ├─ Stack
  ├─ Program Counter
  └─ Local Variables
```

### Thread Lifecycle

```
NEW → RUNNABLE → RUNNING → BLOCKED/WAITING → TERMINATED

NEW: Thread created but not started
RUNNABLE: Ready to run, waiting for CPU
RUNNING: Executing on CPU
BLOCKED: Waiting for monitor lock
WAITING: Waiting for another thread
TERMINATED: Execution completed
```

### Creating Threads

**Method 1: Extending Thread**
```java
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Thread running: " + Thread.currentThread().getName());
    }
}

// Usage
MyThread thread = new MyThread();
thread.start(); // NOT thread.run()!
```

**Method 2: Implementing Runnable** (Preferred)
```java
class MyTask implements Runnable {
    @Override
    public void run() {
        System.out.println("Task running");
    }
}

// Usage
Thread thread = new Thread(new MyTask());
thread.start();

// Lambda
Thread thread = new Thread(() -> {
    System.out.println("Task running");
});
thread.start();
```

**Why Runnable over Thread?**
- ✅ Can extend other classes
- ✅ Better separation of concerns
- ✅ Can be used with ExecutorService

---

## 2. Thread Pools & ExecutorService

### Why Thread Pools?

**Without Thread Pool**:
```java
// Creating thread for each task
for (int i = 0; i < 1000; i++) {
    new Thread(() -> sendEmail()).start();
}

Problems:
❌ 1000 threads created (expensive)
❌ System resource exhaustion
❌ Context switching overhead
❌ No task queueing
```

**With Thread Pool**:
```java
ExecutorService executor = Executors.newFixedThreadPool(10);

for (int i = 0; i < 1000; i++) {
    executor.submit(() -> sendEmail());
}

Benefits:
✅ Only 10 threads created (reused)
✅ Tasks queued if all threads busy
✅ Controlled resource usage
✅ Graceful shutdown
```

### Thread Pool Types

#### 1. Fixed Thread Pool

```java
// Fixed number of threads (e.g., 10)
ExecutorService executor = Executors.newFixedThreadPool(10);

// Submit tasks
executor.submit(() -> {
    System.out.println("Task executed");
});

// Shutdown
executor.shutdown(); // No new tasks accepted
executor.awaitTermination(60, TimeUnit.SECONDS); // Wait for completion
```

**Use When**: Know exact number of threads needed

#### 2. Cached Thread Pool

```java
// Creates new threads as needed, reuses idle threads
ExecutorService executor = Executors.newCachedThreadPool();

// Good for: Short-lived tasks
// Bad for: Long-running tasks (can create too many threads)
```

**Use When**: Many short-lived async tasks

#### 3. Scheduled Thread Pool

```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

// Run once after delay
scheduler.schedule(() -> {
    System.out.println("Executed after 5 seconds");
}, 5, TimeUnit.SECONDS);

// Run periodically with fixed rate
scheduler.scheduleAtFixedRate(() -> {
    System.out.println("Runs every 10 seconds");
}, 0, 10, TimeUnit.SECONDS);

// Run periodically with fixed delay
scheduler.scheduleWithFixedDelay(() -> {
    System.out.println("Runs 10 seconds after previous completion");
}, 0, 10, TimeUnit.SECONDS);
```

**Use When**: Need scheduling (cron jobs, periodic tasks)

#### 4. Single Thread Executor

```java
// Only 1 thread, processes tasks sequentially
ExecutorService executor = Executors.newSingleThreadExecutor();

// Guaranteed execution order
executor.submit(() -> System.out.println("Task 1"));
executor.submit(() -> System.out.println("Task 2"));
executor.submit(() -> System.out.println("Task 3"));
// Output: Task 1, Task 2, Task 3 (always in order)
```

**Use When**: Need sequential execution but async

### Custom Thread Pool

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,                              // corePoolSize: minimum threads
    10,                             // maximumPoolSize: max threads
    60,                             // keepAliveTime: idle thread timeout
    TimeUnit.SECONDS,               // keepAliveTime unit
    new LinkedBlockingQueue<>(100), // workQueue: task queue
    new ThreadFactory() {           // threadFactory: custom thread creation
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("CustomWorker-" + thread.getId());
            thread.setDaemon(false);
            return thread;
        }
    },
    new ThreadPoolExecutor.CallerRunsPolicy() // rejectionHandler
);
```

**Rejection Policies**:
1. **AbortPolicy**: Throw exception (default)
2. **CallerRunsPolicy**: Run in caller's thread
3. **DiscardPolicy**: Silently discard task
4. **DiscardOldestPolicy**: Discard oldest task in queue

---

## 3. Spring @Async

### Configuration

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }
}
```

### Usage

```java
@Service
public class EmailService {
    
    // Async method (returns void)
    @Async("taskExecutor")
    public void sendEmail(String to, String subject, String body) {
        // This runs in separate thread
        System.out.println("Sending email in thread: " + 
            Thread.currentThread().getName());
        
        // Simulate email sending
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Email sent!");
    }
    
    // Async method (returns Future)
    @Async("taskExecutor")
    public Future<String> sendEmailWithResult(String to) {
        // Long operation
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return new AsyncResult<>("Email sent to " + to);
    }
    
    // Async method (returns CompletableFuture)
    @Async("taskExecutor")
    public CompletableFuture<String> sendEmailAsync(String to) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return CompletableFuture.completedFuture("Email sent!");
    }
}
```

**Calling Async Methods**:
```java
@RestController
public class NotificationController {
    
    @Autowired
    private EmailService emailService;
    
    @PostMapping("/send-email")
    public ResponseEntity<String> sendEmail() {
        // This returns immediately (doesn't wait for email)
        emailService.sendEmail("user@example.com", "Hello", "Body");
        
        return ResponseEntity.ok("Email queued for sending");
    }
    
    @PostMapping("/send-email-wait")
    public ResponseEntity<String> sendEmailWait() throws Exception {
        // This waits for result
        Future<String> future = emailService.sendEmailWithResult("user@example.com");
        
        String result = future.get(); // Blocks until complete
        
        return ResponseEntity.ok(result);
    }
}
```

---

## 4. CompletableFuture

### What is CompletableFuture?

**CompletableFuture**: Asynchronous computation that can be composed and chained

### Basic Usage

```java
// Create completed future
CompletableFuture<String> future = CompletableFuture.completedFuture("Hello");

// Supply async
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    // This runs in ForkJoinPool.commonPool()
    return "Hello from async";
});

// Run async (no return value)
CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    System.out.println("Task executed");
});

// Get result (blocks)
String result = future.get();

// Get result with timeout
String result = future.get(5, TimeUnit.SECONDS);
```

### Chaining Operations

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    // Step 1: Fetch user
    return fetchUser(userId);
})
.thenApply(user -> {
    // Step 2: Transform user to DTO (same thread or common pool)
    return userToDTO(user);
})
.thenApplyAsync(dto -> {
    // Step 3: Enrich with additional data (async in separate thread)
    return enrichDTO(dto);
})
.thenAccept(enrichedDTO -> {
    // Step 4: Consume result (doesn't return anything)
    sendNotification(enrichedDTO);
})
.exceptionally(ex -> {
    // Handle errors
    log.error("Error processing user", ex);
    return null;
});
```

### Combining Multiple Futures

```java
// Example: Load user, orders, and preferences in parallel
CompletableFuture<User> userFuture = CompletableFuture.supplyAsync(() -> 
    fetchUser(userId)
);

CompletableFuture<List<Order>> ordersFuture = CompletableFuture.supplyAsync(() -> 
    fetchOrders(userId)
);

CompletableFuture<Preferences> prefsFuture = CompletableFuture.supplyAsync(() -> 
    fetchPreferences(userId)
);

// Wait for all to complete
CompletableFuture<Void> allFutures = CompletableFuture.allOf(
    userFuture, ordersFuture, prefsFuture
);

allFutures.thenRun(() -> {
    // All completed, combine results
    User user = userFuture.join();
    List<Order> orders = ordersFuture.join();
    Preferences prefs = prefsFuture.join();
    
    UserDashboard dashboard = new UserDashboard(user, orders, prefs);
    sendDashboard(dashboard);
});

// Or wait for any to complete
CompletableFuture<Object> anyFuture = CompletableFuture.anyOf(
    future1, future2, future3
);
```

### Real-World Example: Parallel Notifications

```java
@Service
public class NotificationOrchestrator {
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private SmsService smsService;
    
    @Autowired
    private PushService pushService;
    
    public void sendAllNotifications(User user, String message) {
        CompletableFuture<Void> emailFuture = CompletableFuture.runAsync(() -> 
            emailService.send(user.getEmail(), message)
        );
        
        CompletableFuture<Void> smsFuture = CompletableFuture.runAsync(() -> 
            smsService.send(user.getPhone(), message)
        );
        
        CompletableFuture<Void> pushFuture = CompletableFuture.runAsync(() -> 
            pushService.send(user.getDeviceToken(), message)
        );
        
        // Wait for all (don't block main thread)
        CompletableFuture.allOf(emailFuture, smsFuture, pushFuture)
            .thenRun(() -> {
                log.info("All notifications sent for user: {}", user.getId());
            })
            .exceptionally(ex -> {
                log.error("Error sending notifications", ex);
                return null;
            });
    }
}
```

---

## 5. Virtual Threads (Java 21+)

### What are Virtual Threads?

**Traditional Threads** (Platform Threads):
- Heavyweight (~1MB stack)
- Limited by OS (few thousand)
- Expensive to create

**Virtual Threads**:
- Lightweight (~1KB stack)
- Millions possible
- Cheap to create
- Managed by JVM (not OS)

### Usage

```java
// Create virtual thread
Thread vThread = Thread.ofVirtual().start(() -> {
    System.out.println("Running in virtual thread");
});

// Virtual thread executor
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// Submit 1 million tasks (would be impossible with platform threads!)
for (int i = 0; i < 1_000_000; i++) {
    executor.submit(() -> {
        // Task logic
    });
}
```

### When to Use Virtual Threads?

✅ **Use for**:
- I/O-bound tasks (network, database)
- High concurrency requirements
- Simple blocking code

❌ **Don't use for**:
- CPU-intensive tasks
- Tasks that need thread-local storage
- Tasks with synchronized blocks (can pin virtual thread)

### Example: High-Concurrency HTTP Server

```java
@Service
public class HighConcurrencyService {
    
    private final ExecutorService virtualExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    public void handleManyRequests(List<Request> requests) {
        // Handle 100,000 concurrent requests with virtual threads
        for (Request request : requests) {
            virtualExecutor.submit(() -> {
                // This blocks, but it's OK with virtual threads
                String response = httpClient.get(request.getUrl());
                processResponse(response);
            });
        }
    }
}
```

---

## 6. Synchronization

### Race Condition Problem

```java
class Counter {
    private int count = 0;
    
    public void increment() {
        count++; // NOT atomic! (read, increment, write)
    }
    
    public int getCount() {
        return count;
    }
}

// Problem:
Counter counter = new Counter();

Thread t1 = new Thread(() -> {
    for (int i = 0; i < 1000; i++) {
        counter.increment();
    }
});

Thread t2 = new Thread(() -> {
    for (int i = 0; i < 1000; i++) {
        counter.increment();
    }
});

t1.start();
t2.start();
t1.join();
t2.join();

System.out.println(counter.getCount()); // Expected: 2000, Actual: ~1850 (varies!)
```

### Solution 1: synchronized Keyword

```java
class Counter {
    private int count = 0;
    
    public synchronized void increment() {
        count++;
    }
    
    public synchronized int getCount() {
        return count;
    }
}

// Now thread-safe! Output: 2000 ✅
```

### Solution 2: ReentrantLock

```java
class Counter {
    private int count = 0;
    private final Lock lock = new ReentrantLock();
    
    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock(); // Always unlock in finally!
        }
    }
}
```

**ReentrantLock vs synchronized**:
- ✅ More flexible (tryLock, lockInterruptibly)
- ✅ Can check if locked
- ✅ Fair locking option
- ❌ Must manually unlock
- ❌ More verbose

### Solution 3: Atomic Classes

```java
class Counter {
    private final AtomicInteger count = new AtomicInteger(0);
    
    public void increment() {
        count.incrementAndGet(); // Thread-safe, lock-free
    }
    
    public int getCount() {
        return count.get();
    }
}
```

**Atomic Classes**: AtomicInteger, AtomicLong, AtomicBoolean, AtomicReference

### Synchronization Tools

**1. Semaphore** (Limit concurrent access):
```java
// Allow only 5 concurrent database connections
Semaphore semaphore = new Semaphore(5);

public void accessDatabase() {
    try {
        semaphore.acquire(); // Blocks if 5 threads already using
        
        // Access database
        queryDatabase();
        
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        semaphore.release(); // Release permit
    }
}
```

**2. CountDownLatch** (Wait for N operations):
```java
// Wait for 3 services to initialize before starting
CountDownLatch latch = new CountDownLatch(3);

// Service 1
new Thread(() -> {
    initService1();
    latch.countDown();
}).start();

// Service 2
new Thread(() -> {
    initService2();
    latch.countDown();
}).start();

// Service 3
new Thread(() -> {
    initService3();
    latch.countDown();
}).start();

// Main thread waits
latch.await(); // Blocks until count reaches 0
System.out.println("All services initialized!");
```

**3. CyclicBarrier** (Coordinate threads):
```java
// All threads wait at barrier, continue together when all arrive
CyclicBarrier barrier = new CyclicBarrier(3, () -> {
    System.out.println("All threads reached barrier!");
});

for (int i = 0; i < 3; i++) {
    new Thread(() -> {
        System.out.println(Thread.currentThread().getName() + " working...");
        // Do work
        try {
            barrier.await(); // Wait for others
            System.out.println(Thread.currentThread().getName() + " continuing...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }).start();
}
```

---

## 7. Thread-Safe Collections

### Concurrent Collections

| Non-Thread-Safe | Thread-Safe Alternative |
|-----------------|-------------------------|
| ArrayList | CopyOnWriteArrayList |
| HashMap | ConcurrentHashMap |
| HashSet | ConcurrentHashMap.newKeySet() |
| LinkedList | ConcurrentLinkedQueue |
| PriorityQueue | PriorityBlockingQueue |
| Stack | ConcurrentLinkedDeque |

**Example**:
```java
// NOT thread-safe
Map<String, Integer> map = new HashMap<>();

// Thread-safe
Map<String, Integer> map = new ConcurrentHashMap<>();

// Atomic operations
map.putIfAbsent("key", 1);
map.computeIfAbsent("key", k -> expensiveComputation());
map.merge("key", 1, Integer::sum); // Increment by 1
```

---

## 8. Real-World Examples

### Example 1: Bulk Email Sending

```java
@Service
public class BulkEmailService {
    
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    public void sendBulkEmails(List<String> recipients, String message) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (String recipient : recipients) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                sendEmail(recipient, message);
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all emails to be sent
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .join();
        
        System.out.println("All emails sent!");
    }
}
```

### Example 2: Rate Limiting with Semaphore

```java
@Service
public class RateLimitedService {
    
    // Allow 100 concurrent requests
    private final Semaphore semaphore = new Semaphore(100);
    
    public void processRequest(Request request) {
        try {
            // Try to acquire permit (wait if limit reached)
            semaphore.acquire();
            
            // Process request
            handleRequest(request);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for permit");
        } finally {
            // Always release permit
            semaphore.release();
        }
    }
}
```

---

## 9. Best Practices

1. **Always use Thread Pools** (don't create raw threads)
2. **Shut down executors** properly
3. **Handle InterruptedException** correctly
4. **Use CompletableFuture** for composable async operations
5. **Avoid shared mutable state** (prefer immutable objects)
6. **Use thread-safe collections**
7. **Always release locks** in finally block
8. **Don't call wait/notify** without holding lock
9. **Avoid deadlocks** (always acquire locks in same order)
10. **Use Virtual Threads** for I/O-bound high-concurrency tasks

---

## 10. Common Pitfalls

### Pitfall 1: Forgetting to start() thread
```java
Thread thread = new Thread(() -> System.out.println("Hello"));
thread.run(); // ❌ Runs in current thread, not new thread!
thread.start(); // ✅ Correct
```

### Pitfall 2: Not handling InterruptedException
```java
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    // ❌ DON'T ignore or swallow
    e.printStackTrace();
}

// ✅ Correct
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt(); // Restore interrupt status
    throw new RuntimeException("Interrupted", e);
}
```

### Pitfall 3: Deadlock
```java
// ❌ Can deadlock if both threads acquire locks in different order
Object lock1 = new Object();
Object lock2 = new Object();

Thread t1 = new Thread(() -> {
    synchronized (lock1) {
        synchronized (lock2) {
            // Work
        }
    }
});

Thread t2 = new Thread(() -> {
    synchronized (lock2) { // Different order!
        synchronized (lock1) {
            // Work
        }
    }
});

// ✅ Always acquire in same order
```

---

## 🎯 Summary

- ✅ Threads enable parallel execution
- ✅ Thread pools reuse threads efficiently
- ✅ @Async simplifies async method calls
- ✅ CompletableFuture enables composable async operations
- ✅ Virtual Threads enable millions of concurrent tasks
- ✅ Synchronization prevents race conditions
- ✅ Use thread-safe collections for shared data

**Master multithreading for high-performance applications!** 🧵

