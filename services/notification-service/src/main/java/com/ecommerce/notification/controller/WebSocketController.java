package com.ecommerce.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket Controller
 * 
 * <p>Handles WebSocket messages from clients.</p>
 * 
 * <h2>@MessageMapping Explained:</h2>
 * <pre>
 * Similar to @PostMapping/@GetMapping but for WebSocket
 * 
 * Client sends to: /app/markAsRead
 *                   ↓
 * @MessageMapping("/markAsRead")
 * public void markAsRead(...) {
 *     // Handle message
 * }
 * 
 * Why "/app" prefix?
 * - Configured in WebSocketConfig
 * - Routes to application endpoints
 * - Handled by @MessageMapping methods
 * </pre>
 * 
 * <h2>@SendTo Explained:</h2>
 * <pre>
 * Sends return value to destination
 * 
 * @MessageMapping("/sendMessage")
 * @SendTo("/topic/messages")
 * public Message handleMessage(Message msg) {
 *     return msg;  // Sent to /topic/messages
 * }
 * 
 * All subscribers of /topic/messages receive it
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    /**
     * Handle mark as read
     * 
     * <p>Client sends: /app/markAsRead</p>
     * 
     * <pre>
     * JavaScript example:
     * stompClient.send("/app/markAsRead", {}, 
     *     JSON.stringify({notificationId: "123"}));
     * </pre>
     */
    @MessageMapping("/markAsRead")
    public void markAsRead(@Payload Map<String, Object> payload, 
                           SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("Mark as read request from session: {} - Payload: {}", sessionId, payload);

        // In production: update notification status in database
        String notificationId = (String) payload.get("notificationId");
        log.info("Notification {} marked as read", notificationId);
    }

    /**
     * Handle typing indicator
     * 
     * <p>Client sends: /app/typing</p>
     * <p>Broadcasts to: /topic/typing</p>
     * 
     * <pre>
     * Use case: Show "User is typing..." in chat
     * </pre>
     */
    @MessageMapping("/typing")
    @SendTo("/topic/typing")
    public Map<String, Object> handleTyping(@Payload Map<String, Object> payload) {
        log.info("Typing indicator: {}", payload);
        return payload;
    }

    /**
     * Handle presence update
     * 
     * <p>Client sends: /app/presence</p>
     * <p>Broadcasts to: /topic/presence</p>
     * 
     * <pre>
     * Use case: Show user online/offline status
     * </pre>
     */
    @MessageMapping("/presence")
    @SendTo("/topic/presence")
    public Map<String, Object> handlePresence(@Payload Map<String, Object> payload) {
        log.info("Presence update: {}", payload);
        return payload;
    }
}

