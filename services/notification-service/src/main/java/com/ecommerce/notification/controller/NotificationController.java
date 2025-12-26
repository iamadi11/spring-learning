package com.ecommerce.notification.controller;

import com.ecommerce.notification.dto.NotificationRequest;
import com.ecommerce.notification.entity.Notification;
import com.ecommerce.notification.repository.NotificationRepository;
import com.ecommerce.notification.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Notification Controller
 * 
 * <p>REST API for notification management.</p>
 * 
 * @author E-commerce Platform Team
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final EmailService emailService;
    private final SmsService smsService;
    private final PushNotificationService pushService;
    private final NotificationOrchestrator orchestrator;
    private final WebSocketNotificationService webSocketService;
    private final NotificationRepository notificationRepository;

    /**
     * Send all notifications (parallel)
     * 
     * POST /api/notifications/send-all
     */
    @PostMapping("/send-all")
    public ResponseEntity<String> sendAll(@Valid @RequestBody NotificationRequest request) {
        log.info("Received request to send all notifications for user: {}", request.getUserId());

        orchestrator.sendAllWithErrorHandling(
            request.getUserId(),
            request.getEmail(),
            request.getPhone(),
            request.getDeviceToken(),
            request.getSubject(),
            request.getMessage()
        );

        return ResponseEntity.ok("Notifications sent");
    }

    /**
     * Send email only
     * 
     * POST /api/notifications/email
     */
    @PostMapping("/email")
    public ResponseEntity<String> sendEmail(@Valid @RequestBody NotificationRequest request) {
        log.info("Sending email to: {}", request.getEmail());

        emailService.sendEmailAsync(
            request.getUserId(),
            request.getEmail(),
            request.getSubject(),
            request.getMessage()
        );

        return ResponseEntity.ok("Email queued");
    }

    /**
     * Send SMS only
     */
    @PostMapping("/sms")
    public ResponseEntity<String> sendSms(@Valid @RequestBody NotificationRequest request) {
        log.info("Sending SMS to: {}", request.getPhone());

        smsService.sendSmsAsync(
            request.getUserId(),
            request.getPhone(),
            request.getMessage()
        );

        return ResponseEntity.ok("SMS queued");
    }

    /**
     * Send push notification only
     */
    @PostMapping("/push")
    public ResponseEntity<String> sendPush(@Valid @RequestBody NotificationRequest request) {
        log.info("Sending push to device: {}", request.getDeviceToken());

        pushService.sendPushAsync(
            request.getUserId(),
            request.getDeviceToken(),
            request.getSubject(),
            request.getMessage()
        );

        return ResponseEntity.ok("Push notification queued");
    }

    /**
     * Send WebSocket notification
     */
    @PostMapping("/websocket")
    public ResponseEntity<String> sendWebSocket(@Valid @RequestBody NotificationRequest request) {
        log.info("Sending WebSocket notification to user: {}", request.getUserId());

        webSocketService.sendToUser(
            request.getUserId(),
            request.getSubject(),
            request.getMessage()
        );

        return ResponseEntity.ok("WebSocket notification sent");
    }

    /**
     * Broadcast to all users
     */
    @PostMapping("/broadcast")
    public ResponseEntity<String> broadcast(@Valid @RequestBody NotificationRequest request) {
        log.info("Broadcasting notification: {}", request.getSubject());

        webSocketService.broadcastToAll(request.getSubject(), request.getMessage());

        return ResponseEntity.ok("Broadcast sent");
    }

    /**
     * Get user notifications
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<Notification>> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Fetching notifications for user: {}", userId);

        Page<Notification> notifications = notificationRepository
            .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));

        return ResponseEntity.ok(notifications);
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is running");
    }
}

