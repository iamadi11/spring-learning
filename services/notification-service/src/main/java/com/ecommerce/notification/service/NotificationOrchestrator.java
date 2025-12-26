package com.ecommerce.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Notification Orchestrator
 * 
 * <p>Demonstrates PARALLEL EXECUTION with CompletableFuture.</p>
 * 
 * <h2>Sequential vs Parallel Execution:</h2>
 * <pre>
 * SEQUENTIAL (Bad):
 * 
 * sendAll() {
 *     sendEmail();    // 5 seconds
 *     sendSMS();      // 2 seconds
 *     sendPush();     // 1 second
 *     Total: 8 seconds
 * }
 * 
 * Timeline:
 * 0s ──[Email]──── 5s ──[SMS]── 7s ─[Push]─ 8s
 *      ▓▓▓▓▓▓▓▓▓▓       ▓▓▓▓       ▓▓
 * 
 * User waits 8 seconds! 😢
 * 
 * 
 * PARALLEL (Good):
 * 
 * sendAllParallel() {
 *     CompletableFuture email = sendEmail();  // Start immediately
 *     CompletableFuture sms = sendSMS();      // Start immediately
 *     CompletableFuture push = sendPush();    // Start immediately
 *     
 *     CompletableFuture.allOf(email, sms, push).join();
 *     Total: 5 seconds (longest task)
 * }
 * 
 * Timeline:
 * 0s ──[Email]──────── 5s
 *      ▓▓▓▓▓▓▓▓▓▓▓▓▓▓
 * 0s ──[SMS]── 2s
 *      ▓▓▓▓
 * 0s ─[Push] 1s
 *     ▓▓
 * 
 * User waits 5 seconds! 😊
 * 3 seconds saved! (37.5% faster)
 * </pre>
 * 
 * <h2>CompletableFuture.allOf() Explained:</h2>
 * <pre>
 * What does it do?
 * - Combines multiple futures
 * - Completes when ALL complete
 * - Runs tasks in parallel
 * 
 * Usage:
 * 
 * CompletableFuture<Void> future1 = doTask1();
 * CompletableFuture<Void> future2 = doTask2();
 * CompletableFuture<Void> future3 = doTask3();
 * 
 * // Wait for ALL to complete
 * CompletableFuture.allOf(future1, future2, future3)
 *     .join();  // Blocks until all done
 * 
 * System.out.println("All tasks completed!");
 * </pre>
 * 
 * <h2>Real-World Example:</h2>
 * <pre>
 * Order Placed Event:
 * 
 * Without Orchestrator:
 * 1. Send email (5s)
 * 2. Send SMS (2s)
 * 3. Send push (1s)
 * 4. Send WebSocket (instant)
 * Total: 8+ seconds
 * 
 * With Orchestrator:
 * 1. Start all in parallel:
 *    ├─ Email thread  (5s)
 *    ├─ SMS thread    (2s)
 *    ├─ Push thread   (1s)
 *    └─ WebSocket     (instant)
 * 2. Wait for all to complete
 * Total: 5 seconds (60% faster!)
 * 
 * 100 orders/minute:
 * Sequential: 800 seconds = 13.3 minutes
 * Parallel: 500 seconds = 8.3 minutes
 * Saved: 5 minutes every minute!
 * </pre>
 * 
 * <h2>Error Handling:</h2>
 * <pre>
 * What if one fails?
 * 
 * Option 1: Fail Fast (allOf)
 * - One fails → all cancel
 * - Use when all must succeed
 * 
 * Option 2: Continue (handle individually)
 * - One fails → others continue
 * - Use for independent notifications
 * 
 * We use Option 2:
 * - Email fails → Still send SMS, Push
 * - Better user experience
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOrchestrator {

    private final EmailService emailService;
    private final SmsService smsService;
    private final PushNotificationService pushService;
    private final WebSocketNotificationService webSocketService;

    /**
     * Send all notifications in PARALLEL
     * 
     * <p>This method demonstrates the POWER of parallel execution!</p>
     * 
     * <h2>How it works:</h2>
     * <pre>
     * Step 1: Start all futures immediately
     * - All return instantly (non-blocking)
     * - Tasks run in separate thread pools
     * 
     * Step 2: CompletableFuture.allOf()
     * - Combines all futures
     * - Waits for ALL to complete
     * - Returns when slowest finishes
     * 
     * Step 3: Handle results
     * - Check success/failure
     * - Log results
     * - Return summary
     * 
     * Thread allocation:
     * - Email: email-thread-1 (from email pool)
     * - SMS: sms-thread-1 (from SMS pool)
     * - Push: push-thread-1 (from push pool)
     * - WebSocket: main thread or async pool
     * 
     * All run simultaneously!
     * </pre>
     */
    public void sendAllNotifications(
            Long userId,
            String email,
            String phone,
            String deviceToken,
            String subject,
            String message) {
        
        long startTime = System.currentTimeMillis();
        log.info("Starting parallel notification sending for user: {}", userId);

        try {
            // STEP 1: Start all futures (non-blocking, return immediately)
            CompletableFuture<Boolean> emailFuture = 
                emailService.sendEmailWithFuture(userId, email, subject, message);
            
            CompletableFuture<Boolean> smsFuture = 
                smsService.sendSmsWithFuture(userId, phone, message);
            
            CompletableFuture<Boolean> pushFuture = 
                pushService.sendPushWithFuture(userId, deviceToken, subject, message);

            // Send WebSocket immediately (very fast)
            webSocketService.sendToUser(userId, subject, message);

            log.info("All notification tasks started in parallel");

            // STEP 2: Wait for ALL to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                emailFuture,
                smsFuture,
                pushFuture
            );

            // Block until all complete
            allFutures.join();

            // STEP 3: Check results
            boolean emailSuccess = emailFuture.join();
            boolean smsSuccess = smsFuture.join();
            boolean pushSuccess = pushFuture.join();

            long totalTime = System.currentTimeMillis() - startTime;

            log.info("All notifications completed in {}ms - Email: {}, SMS: {}, Push: {}", 
                     totalTime, emailSuccess, smsSuccess, pushSuccess);

        } catch (Exception e) {
            log.error("Error in parallel notification sending: {}", e.getMessage());
        }
    }

    /**
     * Send notifications with individual error handling
     * 
     * <p>If one fails, others continue (better for independent notifications).</p>
     * 
     * <h2>Error Handling Strategy:</h2>
     * <pre>
     * Each future handles its own errors:
     * - exceptionally() catches exceptions
     * - Returns default value (false)
     * - Other futures continue
     * 
     * Example:
     * Email fails (SMTP down)
     * → emailFuture returns false
     * → SMS and Push continue normally
     * → User still gets SMS and Push!
     * 
     * Better than:
     * Email fails → Cancel everything
     * → User gets nothing!
     * </pre>
     */
    public void sendAllWithErrorHandling(
            Long userId,
            String email,
            String phone,
            String deviceToken,
            String subject,
            String message) {
        
        long startTime = System.currentTimeMillis();
        log.info("Starting parallel notifications with error handling for user: {}", userId);

        // Start all futures with error handling
        CompletableFuture<Boolean> emailFuture = 
            emailService.sendEmailWithFuture(userId, email, subject, message)
                .exceptionally(ex -> {
                    log.error("Email failed: {}", ex.getMessage());
                    return false;  // Continue even if email fails
                });
        
        CompletableFuture<Boolean> smsFuture = 
            smsService.sendSmsWithFuture(userId, phone, message)
                .exceptionally(ex -> {
                    log.error("SMS failed: {}", ex.getMessage());
                    return false;
                });
        
        CompletableFuture<Boolean> pushFuture = 
            pushService.sendPushWithFuture(userId, deviceToken, subject, message)
                .exceptionally(ex -> {
                    log.error("Push failed: {}", ex.getMessage());
                    return false;
                });

        // Send WebSocket
        try {
            webSocketService.sendToUser(userId, subject, message);
        } catch (Exception e) {
            log.error("WebSocket failed: {}", e.getMessage());
        }

        // Wait for all (even if some failed)
        CompletableFuture.allOf(emailFuture, smsFuture, pushFuture)
            .thenRun(() -> {
                // All completed (success or failure)
                long totalTime = System.currentTimeMillis() - startTime;
                
                boolean emailSuccess = emailFuture.join();
                boolean smsSuccess = smsFuture.join();
                boolean pushSuccess = pushFuture.join();
                
                int successCount = (emailSuccess ? 1 : 0) + 
                                   (smsSuccess ? 1 : 0) + 
                                   (pushSuccess ? 1 : 0);
                
                log.info("Notifications completed in {}ms - Success: {}/3", 
                         totalTime, successCount);
            })
            .exceptionally(ex -> {
                log.error("Unexpected error: {}", ex.getMessage());
                return null;
            });
    }

    /**
     * Send notifications with timeout
     * 
     * <p>Don't wait forever - fail fast if taking too long.</p>
     */
    public void sendAllWithTimeout(
            Long userId,
            String email,
            String phone,
            String deviceToken,
            String subject,
            String message) {
        
        log.info("Starting parallel notifications with timeout for user: {}", userId);

        // Start all futures
        CompletableFuture<Boolean> emailFuture = 
            emailService.sendEmailWithFuture(userId, email, subject, message);
        
        CompletableFuture<Boolean> smsFuture = 
            smsService.sendSmsWithFuture(userId, phone, message);
        
        CompletableFuture<Boolean> pushFuture = 
            pushService.sendPushWithFuture(userId, deviceToken, subject, message);

        // Wait with timeout (10 seconds max)
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            emailFuture, smsFuture, pushFuture);

        try {
            // Wait max 10 seconds
            allFutures.get(10, java.util.concurrent.TimeUnit.SECONDS);
            log.info("All notifications completed within timeout");
        } catch (java.util.concurrent.TimeoutException e) {
            log.warn("Notification timeout - some may still be processing");
        } catch (Exception e) {
            log.error("Error in timed notification sending: {}", e.getMessage());
        }
    }
}

