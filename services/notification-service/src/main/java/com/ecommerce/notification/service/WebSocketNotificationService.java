package com.ecommerce.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket Notification Service
 * 
 * <p>Sends real-time notifications via WebSocket.</p>
 * 
 * <h2>WebSocket vs Traditional Polling:</h2>
 * <pre>
 * Polling (Old Way):
 * Client polls every 5 seconds:
 * "Any new notifications?" → No
 * "Any new notifications?" → No
 * "Any new notifications?" → No
 * "Any new notifications?" → Yes! (delayed by up to 5s)
 * 
 * Problems:
 * - Many unnecessary requests
 * - Server load high
 * - Battery drain (mobile)
 * - Latency (5s delay)
 * - Wasted bandwidth
 * 
 * WebSocket (New Way):
 * [Connection established]
 * ... silence (no requests) ...
 * Server → "New notification!" → Client (< 100ms)
 * ... silence ...
 * Server → "Order shipped!" → Client (< 100ms)
 * 
 * Benefits:
 * - No polling overhead
 * - Instant delivery
 * - Low server load
 * - Battery efficient
 * - Real-time experience
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send notification to specific user
     * 
     * <p>Uses user-specific queue: /user/{userId}/queue/notifications</p>
     * 
     * <h2>How it works:</h2>
     * <pre>
     * 1. Client subscribes:
     *    stompClient.subscribe('/user/queue/notifications', callback);
     * 
     * 2. Server sends:
     *    sendToUser(userId, message);
     *    ↓
     *    SimpMessagingTemplate resolves user session
     *    ↓
     *    Message sent to /user/{userId}/queue/notifications
     *    ↓
     *    Only that user's client receives
     * 
     * 3. Client callback triggered:
     *    function callback(message) {
     *        alert("New notification: " + message.body);
     *    }
     * </pre>
     */
    public void sendToUser(Long userId, String title, String message) {
        String threadName = Thread.currentThread().getName();
        log.info("[{}] Sending WebSocket notification to user: {}", threadName, userId);

        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", LocalDateTime.now());
        notification.put("type", "NOTIFICATION");

        // Send to specific user
        // Spring converts userId to user session automatically
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/notifications",
            notification
        );

        log.info("[{}] WebSocket notification sent to user: {}", threadName, userId);
    }

    /**
     * Broadcast to all connected users
     * 
     * <p>Uses topic: /topic/announcements</p>
     * 
     * <h2>Broadcast vs Point-to-Point:</h2>
     * <pre>
     * Broadcast (/topic/*):
     * Server → /topic/announcements
     *          ↓
     *    ┌─────┼─────┐
     *    ↓     ↓     ↓
     * User1 User2 User3
     * ALL receive
     * 
     * Point-to-Point (/user/*):
     * Server → /user/123/queue/notifications
     *          ↓
     *       User123
     * ONLY User123 receives
     * </pre>
     */
    public void broadcastToAll(String title, String message) {
        log.info("Broadcasting notification to all users: {}", title);

        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", LocalDateTime.now());
        notification.put("type", "ANNOUNCEMENT");

        // Send to all subscribers of /topic/announcements
        messagingTemplate.convertAndSend("/topic/announcements", notification);

        log.info("Broadcast notification sent");
    }

    /**
     * Send order update via WebSocket
     */
    public void sendOrderUpdate(Long userId, String orderId, String status) {
        log.info("Sending order update to user: {} - Order: {} - Status: {}", 
                 userId, orderId, status);

        Map<String, Object> update = new HashMap<>();
        update.put("type", "ORDER_UPDATE");
        update.put("orderId", orderId);
        update.put("status", status);
        update.put("timestamp", LocalDateTime.now());

        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/orders",
            update
        );
    }

    /**
     * Send payment notification
     */
    public void sendPaymentNotification(Long userId, String transactionId, boolean success) {
        log.info("Sending payment notification to user: {} - Transaction: {} - Success: {}", 
                 userId, transactionId, success);

        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "PAYMENT");
        notification.put("transactionId", transactionId);
        notification.put("success", success);
        notification.put("message", success ? "Payment successful" : "Payment failed");
        notification.put("timestamp", LocalDateTime.now());

        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/payments",
            notification
        );
    }
}

