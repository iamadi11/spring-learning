package com.ecommerce.notification.service;

import com.ecommerce.notification.entity.*;
import com.ecommerce.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Email Service
 * 
 * <p>Demonstrates @Async multithreading for email notifications.</p>
 * 
 * <h2>@Async Explained:</h2>
 * <pre>
 * Without @Async (Synchronous):
 * 
 * controller.sendOrderConfirmation() {
 *     emailService.sendEmail(...);  ← Blocks here for 5 seconds
 *     return "Success";             ← User waits 5 seconds
 * }
 * 
 * With @Async (Asynchronous):
 * 
 * controller.sendOrderConfirmation() {
 *     emailService.sendEmail(...);  ← Returns immediately
 *     return "Success";             ← User gets response < 1ms
 * }
 * 
 * Email sent in background thread pool!
 * 
 * How It Works:
 * 1. Spring creates proxy for EmailService
 * 2. When @Async method called → Spring submits to executor
 * 3. Executor picks thread from pool (emailExecutor)
 * 4. Method runs in separate thread
 * 5. Caller continues immediately
 * 
 * Thread Pool Used:
 * @Async("emailExecutor") → Uses email thread pool (5-10 threads)
 * 
 * Benefits:
 * - Non-blocking
 * - Better response times
 * - Parallel execution
 * - Resource efficient
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.from-name}")
    private String fromName;

    /**
     * Send email asynchronously
     * 
     * <p>Runs in separate thread from email pool.</p>
     * 
     * <h2>Thread Behavior:</h2>
     * <pre>
     * Main Thread:
     * 1. controller receives request
     * 2. calls sendEmailAsync()
     * 3. returns immediately → "Email queued"
     * 
     * Email Thread (from pool):
     * 1. picks up task from queue
     * 2. connects to SMTP server
     * 3. sends email (3-5 seconds)
     * 4. saves to database
     * 5. returns to pool (ready for next task)
     * 
     * User Experience:
     * - Response: < 1ms (instant!)
     * - Email: 3-5s (background)
     * - Happy user! 😊
     * </pre>
     * 
     * @param to Recipient email
     * @param subject Email subject
     * @param body Email body
     */
    @Async("emailExecutor")  // Use email thread pool
    public void sendEmailAsync(Long userId, String to, String subject, String body) {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        
        log.info("[{}] Sending email to: {} - Subject: {}", threadName, to, subject);

        // Create notification record
        Notification notification = Notification.builder()
            .userId(userId)
            .recipient(to)
            .type(NotificationType.EMAIL)
            .priority(NotificationPriority.NORMAL)
            .status(NotificationStatus.PENDING)
            .subject(subject)
            .message(body)
            .threadName(threadName)
            .createdAt(LocalDateTime.now())
            .retryCount(0)
            .build();

        try {
            // Simulate SMTP sending (in production: actually send email)
            simulateEmailSending(to, subject, body);

            // Update notification status
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            
            long processingTime = System.currentTimeMillis() - startTime;
            notification.setProcessingTimeMs(processingTime);
            
            notificationRepository.save(notification);
            
            log.info("[{}] Email sent successfully to: {} in {}ms", 
                     threadName, to, processingTime);

        } catch (Exception e) {
            log.error("[{}] Failed to send email to: {} - {}", threadName, to, e.getMessage());
            
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            notificationRepository.save(notification);
        }
    }

    /**
     * Send email with CompletableFuture
     * 
     * <p>Returns future for better control and chaining.</p>
     * 
     * <h2>CompletableFuture vs void @Async:</h2>
     * <pre>
     * void @Async:
     * - Fire and forget
     * - No return value
     * - Can't wait for result
     * - Can't chain operations
     * 
     * CompletableFuture @Async:
     * - Can get result
     * - Can wait if needed
     * - Can chain operations
     * - Better error handling
     * 
     * Example:
     * CompletableFuture<Boolean> future = sendEmailWithFuture(...);
     * 
     * // Option 1: Don't wait (fire and forget)
     * future.thenAccept(success -> log.info("Email sent: {}", success));
     * 
     * // Option 2: Wait for result
     * Boolean success = future.get();
     * 
     * // Option 3: Chain operations
     * future
     *     .thenApply(success -> logResult(success))
     *     .thenAccept(log -> saveLog(log))
     *     .exceptionally(ex -> handleError(ex));
     * </pre>
     */
    @Async("emailExecutor")
    public CompletableFuture<Boolean> sendEmailWithFuture(
            Long userId, String to, String subject, String body) {
        
        return CompletableFuture.supplyAsync(() -> {
            String threadName = Thread.currentThread().getName();
            log.info("[{}] Sending email with Future to: {}", threadName, to);

            try {
                simulateEmailSending(to, subject, body);
                
                // Save notification
                Notification notification = createNotification(
                    userId, to, subject, body, NotificationStatus.SENT, threadName);
                notificationRepository.save(notification);
                
                return true;
            } catch (Exception e) {
                log.error("[{}] Email failed: {}", threadName, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Simulate email sending via SMTP
     * 
     * <p>In production: use JavaMailSender to actually send email.</p>
     */
    private void simulateEmailSending(String to, String subject, String body) {
        try {
            // Simulate SMTP latency (3-5 seconds)
            int delay = 3000 + (int)(Math.random() * 2000);
            Thread.sleep(delay);
            
            // In production: actually send email
            // SimpleMailMessage message = new SimpleMailMessage();
            // message.setFrom(fromEmail);
            // message.setTo(to);
            // message.setSubject(subject);
            // message.setText(body);
            // mailSender.send(message);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Email sending interrupted", e);
        }
    }

    /**
     * Helper to create notification
     */
    private Notification createNotification(
            Long userId, String to, String subject, String body, 
            NotificationStatus status, String threadName) {
        
        return Notification.builder()
            .userId(userId)
            .recipient(to)
            .type(NotificationType.EMAIL)
            .priority(NotificationPriority.NORMAL)
            .status(status)
            .subject(subject)
            .message(body)
            .threadName(threadName)
            .sentAt(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .build();
    }
}

