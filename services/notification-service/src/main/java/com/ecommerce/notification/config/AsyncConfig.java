package com.ecommerce.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async Configuration - Thread Pool Setup
 * 
 * <p>Configures multiple thread pools for different types of notifications.
 * This is a CRITICAL concept in production systems.</p>
 * 
 * <h2>Why Multiple Thread Pools?</h2>
 * <pre>
 * Problem with Single Pool:
 * - Email sending is slow (5s each)
 * - 100 emails arrive → 100 threads blocked for 5s
 * - SMS request arrives → No threads available!
 * - SMS must wait for email to finish
 * - Bad user experience
 * 
 * Solution: Separate Pools:
 * ┌─────────────────────────────────┐
 * │  Email Pool (5 threads)         │ ← Only for emails
 * ├─────────────────────────────────┤
 * │  SMS Pool (3 threads)           │ ← Only for SMS
 * ├─────────────────────────────────┤
 * │  Push Pool (10 threads)         │ ← Only for push
 * └─────────────────────────────────┘
 * 
 * Now:
 * - 100 emails → Only use email pool
 * - SMS request → Use SMS pool (always available!)
 * - No blocking between different notification types
 * - Better isolation and reliability
 * </pre>
 * 
 * <h2>Thread Pool Parameters Explained:</h2>
 * <pre>
 * Core Pool Size:
 * - Minimum threads always alive
 * - Even if idle
 * - Example: 5 threads
 * 
 * Max Pool Size:
 * - Maximum threads ever created
 * - Scale up when needed
 * - Example: 10 threads
 * 
 * Queue Capacity:
 * - Tasks waiting for threads
 * - Bounded queue prevents memory overflow
 * - Example: 100 tasks
 * 
 * Thread Lifecycle:
 * 1. Task arrives
 * 2. If threads < corePoolSize → Create new thread
 * 3. If threads = corePoolSize → Add to queue
 * 4. If queue full → Create thread up to maxPoolSize
 * 5. If threads = maxPoolSize and queue full → REJECT
 * 
 * Example (Email Pool: core=5, max=10, queue=100):
 * 
 * Tasks 1-5:   Create 5 threads (core size reached)
 * Tasks 6-105: Add to queue (queue size = 100)
 * Tasks 106-110: Create 5 more threads (max size reached)
 * Task 111:    REJECTED! (all resources exhausted)
 * 
 * Rejection Policy:
 * - AbortPolicy: Throw exception (default)
 * - CallerRunsPolicy: Run in caller thread (throttling)
 * - DiscardPolicy: Silently discard
 * - DiscardOldestPolicy: Discard oldest in queue
 * 
 * We use CallerRunsPolicy:
 * - Provides throttling (slow down caller)
 * - No data loss
 * - Natural backpressure
 * </pre>
 * 
 * <h2>Sizing Thread Pools:</h2>
 * <pre>
 * Rules of Thumb:
 * 
 * CPU-Bound Tasks (calculations, processing):
 * Optimal threads = Number of CPU cores
 * Example: 8-core CPU → 8 threads
 * More threads = wasteful (context switching overhead)
 * 
 * I/O-Bound Tasks (network, database, file):
 * Optimal threads = Number of cores × (1 + Wait time / CPU time)
 * Example: Email (95% waiting for SMTP):
 *   Wait time = 4.75s, CPU time = 0.25s
 *   4.75 / 0.25 = 19
 *   8 cores × (1 + 19) = 160 threads!
 * 
 * Our Configuration:
 * - Email: I/O bound, slow (SMTP) → 5-10 threads
 * - SMS: I/O bound, medium (API) → 3-8 threads
 * - Push: I/O bound, fast (Firebase) → 10-20 threads
 * - Scheduled: Background jobs → 2 threads
 * 
 * Total: 20-40 threads vs 1000+ sequential
 * Huge resource savings!
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    // Email Thread Pool Configuration
    @Value("${app.thread-pool.email.core-pool-size}")
    private int emailCorePoolSize;

    @Value("${app.thread-pool.email.max-pool-size}")
    private int emailMaxPoolSize;

    @Value("${app.thread-pool.email.queue-capacity}")
    private int emailQueueCapacity;

    // SMS Thread Pool Configuration
    @Value("${app.thread-pool.sms.core-pool-size}")
    private int smsCorePoolSize;

    @Value("${app.thread-pool.sms.max-pool-size}")
    private int smsMaxPoolSize;

    @Value("${app.thread-pool.sms.queue-capacity}")
    private int smsQueueCapacity;

    // Push Thread Pool Configuration
    @Value("${app.thread-pool.push.core-pool-size}")
    private int pushCorePoolSize;

    @Value("${app.thread-pool.push.max-pool-size}")
    private int pushMaxPoolSize;

    @Value("${app.thread-pool.push.queue-capacity}")
    private int pushQueueCapacity;

    // General Async Pool Configuration
    @Value("${app.thread-pool.async.core-pool-size}")
    private int asyncCorePoolSize;

    @Value("${app.thread-pool.async.max-pool-size}")
    private int asyncMaxPoolSize;

    @Value("${app.thread-pool.async.queue-capacity}")
    private int asyncQueueCapacity;

    /**
     * Email Executor - For slow SMTP operations
     * 
     * <p>Configuration:
     * - Core: 5 threads (always alive)
     * - Max: 10 threads (scale up when busy)
     * - Queue: 100 emails (buffer for spikes)
     * </p>
     * 
     * <p>Why these numbers?
     * - Email sending takes 3-5 seconds
     * - 5 threads = 1 email/second sustained
     * - 10 threads = 2 emails/second peak
     * - Queue handles burst traffic
     * </p>
     */
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core configuration
        executor.setCorePoolSize(emailCorePoolSize);
        executor.setMaxPoolSize(emailMaxPoolSize);
        executor.setQueueCapacity(emailQueueCapacity);
        
        // Thread naming (helps debugging)
        executor.setThreadNamePrefix("email-");
        
        // Rejection policy (run in caller thread = natural throttling)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("Email Executor initialized - Core: {}, Max: {}, Queue: {}", 
                 emailCorePoolSize, emailMaxPoolSize, emailQueueCapacity);
        
        return executor;
    }

    /**
     * SMS Executor - For medium-speed API calls
     * 
     * <p>Configuration:
     * - Core: 3 threads
     * - Max: 8 threads
     * - Queue: 50 SMS
     * </p>
     * 
     * <p>Why these numbers?
     * - SMS API takes 1-2 seconds
     * - 3 threads = 1.5-3 SMS/second sustained
     * - 8 threads = 4-8 SMS/second peak
     * </p>
     */
    @Bean(name = "smsExecutor")
    public Executor smsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(smsCorePoolSize);
        executor.setMaxPoolSize(smsMaxPoolSize);
        executor.setQueueCapacity(smsQueueCapacity);
        executor.setThreadNamePrefix("sms-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("SMS Executor initialized - Core: {}, Max: {}, Queue: {}", 
                 smsCorePoolSize, smsMaxPoolSize, smsQueueCapacity);
        
        return executor;
    }

    /**
     * Push Notification Executor - For fast Firebase/FCM calls
     * 
     * <p>Configuration:
     * - Core: 10 threads
     * - Max: 20 threads
     * - Queue: 200 notifications
     * </p>
     * 
     * <p>Why these numbers?
     * - Push API is fast (200-500ms)
     * - 10 threads = 20-50 push/second sustained
     * - 20 threads = 40-100 push/second peak
     * - Larger pool because operations are fast
     * </p>
     */
    @Bean(name = "pushExecutor")
    public Executor pushExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(pushCorePoolSize);
        executor.setMaxPoolSize(pushMaxPoolSize);
        executor.setQueueCapacity(pushQueueCapacity);
        executor.setThreadNamePrefix("push-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("Push Executor initialized - Core: {}, Max: {}, Queue: {}", 
                 pushCorePoolSize, pushMaxPoolSize, pushQueueCapacity);
        
        return executor;
    }

    /**
     * Default Async Executor - For general @Async methods
     * 
     * <p>Used when no specific executor is specified in @Async annotation.</p>
     */
    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(asyncCorePoolSize);
        executor.setMaxPoolSize(asyncMaxPoolSize);
        executor.setQueueCapacity(asyncQueueCapacity);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("Async Executor initialized - Core: {}, Max: {}, Queue: {}", 
                 asyncCorePoolSize, asyncMaxPoolSize, asyncQueueCapacity);
        
        return executor;
    }

    /**
     * Scheduled Executor - For @Scheduled tasks
     * 
     * <p>Used for cron jobs, batch processing, reminders.</p>
     */
    @Bean(name = "scheduledExecutor")
    public Executor scheduledExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Scheduled tasks don't need large pool
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("scheduled-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        
        log.info("Scheduled Executor initialized");
        
        return executor;
    }
}

