package com.ecommerce.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration
 * 
 * <p>Configures WebSocket for real-time notifications using STOMP protocol.</p>
 * 
 * <h2>What is WebSocket?</h2>
 * <pre>
 * Traditional HTTP:
 * - Client requests → Server responds
 * - Connection closes after response
 * - For each message: open connection, send, close
 * - High overhead!
 * 
 * Client                Server
 *   │                     │
 *   │──── Request 1 ────→│
 *   │←─── Response 1 ────│
 *   │                     │
 *   │──── Request 2 ────→│
 *   │←─── Response 2 ────│
 * 
 * WebSocket:
 * - Single persistent connection
 * - Bidirectional communication
 * - Low overhead after handshake
 * - Real-time data flow
 * 
 * Client                Server
 *   │                     │
 *   │──── Handshake ────→│
 *   │←─── Upgrade ───────│
 *   │═════════════════════│ ← Persistent connection
 *   │←─── Message 1 ─────│
 *   │──── Message 2 ────→│
 *   │←─── Message 3 ─────│
 *   │═════════════════════│ ← Stays open
 * </pre>
 * 
 * <h2>What is STOMP?</h2>
 * <pre>
 * STOMP = Simple Text Oriented Messaging Protocol
 * 
 * Why STOMP over raw WebSocket?
 * - Raw WebSocket: Just send/receive messages
 * - STOMP: Adds structure (like HTTP for WebSocket)
 * - Publish/Subscribe pattern
 * - Topic routing
 * - Message headers
 * 
 * STOMP Commands:
 * - CONNECT: Establish connection
 * - SUBSCRIBE: Listen to topic/queue
 * - SEND: Send message to destination
 * - DISCONNECT: Close connection
 * 
 * Destinations:
 * 1. /topic/* : Broadcast to all subscribers
 *    Example: /topic/notifications
 *    Use case: System announcements
 * 
 * 2. /user/{username}/queue/* : Point-to-point
 *    Example: /user/alice/queue/notifications
 *    Use case: Personal notifications
 * 
 * 3. /app/* : Application endpoints (handled by @MessageMapping)
 *    Example: /app/sendMessage
 *    Use case: Client sends message to server
 * </pre>
 * 
 * <h2>Message Flow:</h2>
 * <pre>
 * Broadcast (All users):
 * 
 * Server sends to: /topic/announcements
 *                      ↓
 *         ┌────────────┼────────────┐
 *         ↓            ↓            ↓
 *     Client A     Client B     Client C
 *     (receives)   (receives)   (receives)
 * 
 * Point-to-Point (Specific user):
 * 
 * Server sends to: /user/bob/queue/notifications
 *                      ↓
 *                  Client B (Bob)
 *                  (receives)
 * 
 * Client A (Alice) → NO
 * Client C (Carol) → NO
 * Only Bob receives!
 * 
 * Client to Server:
 * 
 * Client → /app/markAsRead → Server
 *                             ↓
 *                     @MessageMapping("/markAsRead")
 *                     public void markAsRead(...) {
 *                         // Handle message
 *                     }
 * </pre>
 * 
 * <h2>Real-World Use Cases:</h2>
 * <pre>
 * 1. Order Notifications:
 *    Order placed →
 *    Server → /user/customer123/queue/orders
 *    → "Order confirmed! Tracking: ABC123"
 * 
 * 2. Chat Messages:
 *    User types message →
 *    Client → /app/sendMessage
 *    Server processes →
 *    Server → /topic/chatroom/123
 *    → All users in chatroom receive
 * 
 * 3. Live Updates:
 *    Product stock changes →
 *    Server → /topic/products/456
 *    → All users viewing product see update
 * 
 * 4. System Alerts:
 *    Maintenance scheduled →
 *    Server → /topic/alerts
 *    → All connected users notified
 * </pre>
 * 
 * <h2>Configuration Explained:</h2>
 * <pre>
 * 1. Enable STOMP:
 *    @EnableWebSocketMessageBroker
 *    - Enables STOMP over WebSocket
 *    - Sets up message broker
 * 
 * 2. Endpoint:
 *    registry.addEndpoint("/ws")
 *    - Client connects to: ws://localhost:8086/ws
 *    - WebSocket handshake happens here
 * 
 * 3. CORS:
 *    .setAllowedOrigins("*")
 *    - Allow connections from any origin
 *    - In production: specify exact origins
 * 
 * 4. SockJS:
 *    .withSockJS()
 *    - Fallback for browsers without WebSocket
 *    - Uses long polling if needed
 * 
 * 5. Message Broker:
 *    enableSimpleBroker("/topic", "/queue")
 *    - Enables in-memory message broker
 *    - Routes messages to destinations
 * 
 * 6. Application Prefix:
 *    setApplicationDestinationPrefixes("/app")
 *    - Messages to /app/* handled by @MessageMapping
 * 
 * 7. User Prefix:
 *    setUserDestinationPrefix("/user")
 *    - Personal messages use /user/* prefix
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.websocket.endpoint}")
    private String websocketEndpoint;

    @Value("${app.websocket.topic-prefix}")
    private String topicPrefix;

    @Value("${app.websocket.user-prefix}")
    private String userPrefix;

    /**
     * Register STOMP endpoints
     * 
     * <p>Clients connect to: ws://localhost:8086/ws</p>
     * 
     * <p>Example (JavaScript):
     * <pre>
     * const socket = new SockJS('http://localhost:8086/ws');
     * const stompClient = Stomp.over(socket);
     * stompClient.connect({}, function(frame) {
     *     console.log('Connected: ' + frame);
     * });
     * </pre>
     * </p>
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(websocketEndpoint)
                .setAllowedOrigins("*")  // Allow all origins (for development)
                .withSockJS();  // Enable SockJS fallback
        
        log.info("WebSocket endpoint registered: {}", websocketEndpoint);
    }

    /**
     * Configure message broker
     * 
     * <p>Broker routes messages to subscribers based on destination.</p>
     * 
     * <p>Destinations:
     * - /topic/* : Broadcast (all subscribers receive)
     * - /queue/* : Point-to-point (specific user receives)
     * - /app/* : Application endpoints (@MessageMapping)
     * </p>
     * 
     * <p>Example subscriptions:
     * <pre>
     * // Subscribe to all notifications (broadcast)
     * stompClient.subscribe('/topic/notifications', function(message) {
     *     console.log('Received: ' + message.body);
     * });
     * 
     * // Subscribe to personal notifications
     * stompClient.subscribe('/user/queue/notifications', function(message) {
     *     console.log('Personal notification: ' + message.body);
     * });
     * </pre>
     * </p>
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple broker for /topic and /queue destinations
        registry.enableSimpleBroker(topicPrefix, "/queue");
        
        // Messages to /app/* are routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        
        // User-specific destinations use /user prefix
        registry.setUserDestinationPrefix(userPrefix);
        
        log.info("Message broker configured - Topic: {}, User: {}", topicPrefix, userPrefix);
    }
}

