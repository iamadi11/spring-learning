package com.ecommerce.notification.consumer;

import com.ecommerce.notification.service.NotificationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Order Event Consumer
 * 
 * <p>Consumes order events from Kafka and triggers notifications.</p>
 * 
 * <h2>Event-Driven Architecture:</h2>
 * <pre>
 * Traditional (Synchronous):
 * Order Service → Direct HTTP call → Notification Service
 * 
 * Problems:
 * - Coupling (Order knows about Notification)
 * - Failure propagation (Notification down = Order fails)
 * - Blocking (Order waits for Notification)
 * - Scalability issues
 * 
 * Event-Driven (Asynchronous):
 * Order Service → Kafka Event → Notification Service
 * 
 * Benefits:
 * - Decoupling (Order doesn't know about Notification)
 * - Resilience (Notification down = Event queued)
 * - Non-blocking (Order completes immediately)
 * - Better scalability
 * 
 * Flow:
 * 1. User places order
 * 2. Order Service creates order
 * 3. Order Service publishes "order.created" event to Kafka
 * 4. Order Service returns success to user (fast!)
 * 5. Notification Service consumes event (async)
 * 6. Notification Service sends all notifications (parallel)
 * 7. User receives notifications
 * 
 * Timeline:
 * 0ms: Order created
 * 10ms: Event published
 * 20ms: User gets response (FAST!)
 * 100ms: Event consumed
 * 5000ms: All notifications sent
 * 
 * User experience: Instant!
 * </pre>
 * 
 * <h2>Kafka Consumer Explained:</h2>
 * <pre>
 * @KafkaListener:
 * - Subscribes to Kafka topic
 * - Automatically polls for messages
 * - Calls method for each message
 * 
 * Manual Acknowledgment:
 * - ackMode = MANUAL
 * - Message not removed until acknowledged
 * - Prevents data loss
 * - If processing fails → message redelivered
 * 
 * Example:
 * 1. Message arrives
 * 2. Process message
 * 3. If success → ack.acknowledge()
 * 4. If failure → don't ack → redelivered
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationOrchestrator notificationOrchestrator;

    /**
     * Handle order created events
     * 
     * <p>Sends notifications when order is created.</p>
     * 
     * <h2>Multi-channel Notification:</h2>
     * <pre>
     * Order Created Event Received
     *         ↓
     * NotificationOrchestrator.sendAll()
     *         ↓
     *    ┌────┴────┬────────┬────────┐
     *    ↓         ↓        ↓        ↓
     *  Email      SMS     Push   WebSocket
     * (5s)       (2s)     (1s)   (instant)
     *    ↓         ↓        ↓        ↓
     * Parallel execution → Complete in 5s!
     * 
     * Without parallel: 8+ seconds
     * With parallel: 5 seconds
     * Improvement: 37.5% faster!
     * </pre>
     */
    @KafkaListener(
        topics = "order.created",
        groupId = "notification-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCreated(Map<String, Object> event, Acknowledgment ack) {
        String threadName = Thread.currentThread().getName();
        log.info("[{}] Received order.created event: {}", threadName, event);

        try {
            // Extract event data
            Long userId = ((Number) event.get("userId")).longValue();
            String orderId = (String) event.get("orderId");
            String userEmail = (String) event.get("email");
            String userPhone = (String) event.get("phone");
            String deviceToken = (String) event.get("deviceToken");

            // Send all notifications in parallel
            notificationOrchestrator.sendAllWithErrorHandling(
                userId,
                userEmail,
                userPhone,
                deviceToken,
                "Order Confirmation",
                "Your order " + orderId + " has been confirmed!"
            );

            // Acknowledge message (remove from queue)
            ack.acknowledge();
            
            log.info("[{}] Order created event processed successfully", threadName);

        } catch (Exception e) {
            log.error("[{}] Failed to process order.created event: {}", 
                     threadName, e.getMessage());
            // Don't acknowledge → message will be redelivered
        }
    }

    /**
     * Handle order shipped events
     */
    @KafkaListener(
        topics = "order.shipped",
        groupId = "notification-service"
    )
    public void handleOrderShipped(Map<String, Object> event, Acknowledgment ack) {
        log.info("Received order.shipped event: {}", event);

        try {
            Long userId = ((Number) event.get("userId")).longValue();
            String orderId = (String) event.get("orderId");
            String trackingNumber = (String) event.get("trackingNumber");
            String userEmail = (String) event.get("email");
            String userPhone = (String) event.get("phone");
            String deviceToken = (String) event.get("deviceToken");

            // Send shipping notifications
            notificationOrchestrator.sendAllWithErrorHandling(
                userId,
                userEmail,
                userPhone,
                deviceToken,
                "Order Shipped",
                "Your order " + orderId + " has been shipped! Tracking: " + trackingNumber
            );

            ack.acknowledge();
            log.info("Order shipped event processed successfully");

        } catch (Exception e) {
            log.error("Failed to process order.shipped event: {}", e.getMessage());
        }
    }

    /**
     * Handle order delivered events
     */
    @KafkaListener(
        topics = "order.delivered",
        groupId = "notification-service"
    )
    public void handleOrderDelivered(Map<String, Object> event, Acknowledgment ack) {
        log.info("Received order.delivered event: {}", event);

        try {
            Long userId = ((Number) event.get("userId")).longValue();
            String orderId = (String) event.get("orderId");
            String userEmail = (String) event.get("email");
            String userPhone = (String) event.get("phone");
            String deviceToken = (String) event.get("deviceToken");

            // Send delivery notifications
            notificationOrchestrator.sendAllWithErrorHandling(
                userId,
                userEmail,
                userPhone,
                deviceToken,
                "Order Delivered",
                "Your order " + orderId + " has been delivered! Enjoy!"
            );

            ack.acknowledge();
            log.info("Order delivered event processed successfully");

        } catch (Exception e) {
            log.error("Failed to process order.delivered event: {}", e.getMessage());
        }
    }
}

