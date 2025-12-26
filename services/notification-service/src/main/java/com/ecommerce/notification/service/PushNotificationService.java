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
 * Push Notification Service
 * 
 * <p>Demonstrates @Async with push notification thread pool.</p>
 * 
 * @author E-commerce Platform Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Send push notification asynchronously
     * 
     * <p>Uses push thread pool (10-20 threads).</p>
     * <p>Fastest notification type (200-500ms).</p>
     */
    @Async("pushExecutor")
    public void sendPushAsync(Long userId, String deviceToken, String title, String body) {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        
        log.info("[{}] Sending push notification to: {} - Title: {}", 
                 threadName, deviceToken, title);

        Notification notification = Notification.builder()
            .userId(userId)
            .recipient(deviceToken)
            .type(NotificationType.PUSH)
            .priority(NotificationPriority.NORMAL)
            .status(NotificationStatus.PENDING)
            .subject(title)
            .message(body)
            .threadName(threadName)
            .createdAt(LocalDateTime.now())
            .retryCount(0)
            .build();

        try {
            // Simulate FCM/APNS API call
            simulatePushSending(deviceToken, title, body);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            notificationRepository.save(notification);
            
            log.info("[{}] Push sent successfully to: {} in {}ms", 
                     threadName, deviceToken, notification.getProcessingTimeMs());

        } catch (Exception e) {
            log.error("[{}] Failed to send push to: {} - {}", 
                     threadName, deviceToken, e.getMessage());
            
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            notificationRepository.save(notification);
        }
    }

    /**
     * Send push with CompletableFuture
     */
    @Async("pushExecutor")
    public CompletableFuture<Boolean> sendPushWithFuture(
            Long userId, String deviceToken, String title, String body) {
        
        return CompletableFuture.supplyAsync(() -> {
            String threadName = Thread.currentThread().getName();
            log.info("[{}] Sending push with Future to: {}", threadName, deviceToken);

            try {
                simulatePushSending(deviceToken, title, body);
                
                Notification notification = createNotification(
                    userId, deviceToken, title, body, NotificationStatus.SENT, threadName);
                notificationRepository.save(notification);
                
                return true;
            } catch (Exception e) {
                log.error("[{}] Push failed: {}", threadName, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Simulate push notification API call
     */
    private void simulatePushSending(String deviceToken, String title, String body) {
        try {
            // Simulate FCM/APNS latency (200-500ms)
            int delay = 200 + (int)(Math.random() * 300);
            Thread.sleep(delay);
            
            // In production: use Firebase Cloud Messaging
            // firebaseMessaging.send(message);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Push sending interrupted", e);
        }
    }

    private Notification createNotification(
            Long userId, String deviceToken, String title, String body, 
            NotificationStatus status, String threadName) {
        
        return Notification.builder()
            .userId(userId)
            .recipient(deviceToken)
            .type(NotificationType.PUSH)
            .priority(NotificationPriority.NORMAL)
            .status(status)
            .subject(title)
            .message(body)
            .threadName(threadName)
            .sentAt(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .build();
    }
}

