package com.ecommerce.notification.service;

import com.ecommerce.notification.entity.*;
import com.ecommerce.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * SMS Service
 * 
 * <p>Demonstrates @Async with SMS thread pool.</p>
 * 
 * @author E-commerce Platform Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final NotificationRepository notificationRepository;

    /**
     * Send SMS asynchronously
     * 
     * <p>Uses SMS thread pool (3-8 threads).</p>
     */
    @Async("smsExecutor")
    public void sendSmsAsync(Long userId, String phoneNumber, String message) {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        
        log.info("[{}] Sending SMS to: {} - Message: {}", threadName, phoneNumber, message);

        Notification notification = Notification.builder()
            .userId(userId)
            .recipient(phoneNumber)
            .type(NotificationType.SMS)
            .priority(NotificationPriority.NORMAL)
            .status(NotificationStatus.PENDING)
            .message(message)
            .threadName(threadName)
            .createdAt(LocalDateTime.now())
            .retryCount(0)
            .build();

        try {
            // Simulate SMS API call (Twilio, AWS SNS, etc.)
            simulateSmsSending(phoneNumber, message);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            notificationRepository.save(notification);
            
            log.info("[{}] SMS sent successfully to: {} in {}ms", 
                     threadName, phoneNumber, notification.getProcessingTimeMs());

        } catch (Exception e) {
            log.error("[{}] Failed to send SMS to: {} - {}", threadName, phoneNumber, e.getMessage());
            
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            notificationRepository.save(notification);
        }
    }

    /**
     * Send SMS with CompletableFuture
     */
    @Async("smsExecutor")
    public CompletableFuture<Boolean> sendSmsWithFuture(Long userId, String phoneNumber, String message) {
        return CompletableFuture.supplyAsync(() -> {
            String threadName = Thread.currentThread().getName();
            log.info("[{}] Sending SMS with Future to: {}", threadName, phoneNumber);

            try {
                simulateSmsSending(phoneNumber, message);
                
                Notification notification = createNotification(
                    userId, phoneNumber, message, NotificationStatus.SENT, threadName);
                notificationRepository.save(notification);
                
                return true;
            } catch (Exception e) {
                log.error("[{}] SMS failed: {}", threadName, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Simulate SMS API call
     */
    private void simulateSmsSending(String phoneNumber, String message) {
        try {
            // Simulate API latency (1-2 seconds)
            int delay = 1000 + (int)(Math.random() * 1000);
            Thread.sleep(delay);
            
            // In production: use Twilio, AWS SNS, etc.
            // twilioClient.sendSms(phoneNumber, message);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("SMS sending interrupted", e);
        }
    }

    private Notification createNotification(
            Long userId, String phoneNumber, String message, 
            NotificationStatus status, String threadName) {
        
        return Notification.builder()
            .userId(userId)
            .recipient(phoneNumber)
            .type(NotificationType.SMS)
            .priority(NotificationPriority.NORMAL)
            .status(status)
            .message(message)
            .threadName(threadName)
            .sentAt(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .build();
    }
}

