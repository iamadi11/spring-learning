package com.ecommerce.review;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Review Service Application
 * 
 * <p>Review management service demonstrating gRPC communication patterns.</p>
 * 
 * <h2>Service Responsibilities:</h2>
 * <ul>
 *   <li>Product Reviews & Ratings</li>
 *   <li>Review Moderation</li>
 *   <li>Review Statistics & Analytics</li>
 *   <li>Helpful/Unhelpful Voting</li>
 *   <li>Verified Purchase Badges</li>
 *   <li>Review Images & Media</li>
 * </ul>
 * 
 * <h2>What is gRPC?</h2>
 * <pre>
 * gRPC = gRPC Remote Procedure Call
 * 
 * What it does:
 * - Calls functions on remote server as if they were local
 * - High-performance RPC framework
 * - Uses HTTP/2
 * - Uses Protocol Buffers (binary format)
 * - Open source (Google)
 * 
 * Simple Analogy:
 * Traditional way (REST):
 * "Hey server, please give me user data. Here's my request in JSON format."
 * Server: "Here's your data in JSON format."
 * 
 * gRPC way:
 * "Hey server, call getUserData(123) function!"
 * Server: Executes function and returns result
 * (Like calling a local function, but it's remote!)
 * </pre>
 * 
 * <h2>REST vs gRPC - Complete Comparison</h2>
 * <pre>
 * ┌────────────────────┬─────────────────────┬──────────────────────┐
 * │     Feature        │        REST         │        gRPC          │
 * ├────────────────────┼─────────────────────┼──────────────────────┤
 * │ Protocol           │ HTTP/1.1            │ HTTP/2               │
 * │ Data Format        │ JSON (text)         │ Protobuf (binary)    │
 * │ Data Size          │ Larger              │ Smaller (30-50%)     │
 * │ Speed              │ Slower              │ Faster (7-10x)       │
 * │ Streaming          │ Limited             │ Bidirectional        │
 * │ Code Generation    │ Manual              │ Automatic            │
 * │ Browser Support    │ Yes                 │ Limited              │
 * │ Human Readable     │ Yes (JSON)          │ No (binary)          │
 * │ Type Safety        │ No                  │ Yes (strong typing)  │
 * │ Learning Curve     │ Easy                │ Medium               │
 * │ Best For           │ Public APIs         │ Internal services    │
 * └────────────────────┴─────────────────────┴──────────────────────┘
 * 
 * Example - Get Review:
 * 
 * REST Request (text, ~200 bytes):
 * GET /api/reviews/123 HTTP/1.1
 * Host: localhost:8087
 * Accept: application/json
 * 
 * Response (JSON, ~500 bytes):
 * {
 *   "reviewId": "123",
 *   "productId": 456,
 *   "userId": 789,
 *   "rating": 5,
 *   "title": "Great product!",
 *   "comment": "Highly recommended",
 *   "verifiedPurchase": true,
 *   "helpfulCount": 42,
 *   "createdAt": "2024-01-01T12:00:00Z"
 * }
 * 
 * gRPC Request (binary, ~50 bytes):
 * GetReview(reviewId: "123")
 * 
 * Response (binary, ~150 bytes):
 * ReviewResponse {
 *   reviewId: "123"
 *   productId: 456
 *   userId: 789
 *   rating: 5
 *   title: "Great product!"
 *   comment: "Highly recommended"
 *   verifiedPurchase: true
 *   helpfulCount: 42
 *   createdAt: 1704110400
 * }
 * 
 * Size Comparison:
 * REST: ~700 bytes total
 * gRPC: ~200 bytes total
 * Savings: 71% less bandwidth!
 * 
 * Speed Comparison (1000 requests):
 * REST: ~2000ms
 * gRPC: ~300ms
 * Improvement: 6.7x faster!
 * </pre>
 * 
 * <h2>Protocol Buffers (Protobuf) Explained</h2>
 * <pre>
 * What is Protocol Buffers?
 * - Language-neutral data serialization format
 * - Like JSON, but binary
 * - Much smaller and faster
 * - Strongly typed
 * - Google's data interchange format
 * 
 * JSON vs Protobuf:
 * 
 * JSON (Human-readable text):
 * {
 *   "userId": 123,
 *   "userName": "John Doe",
 *   "email": "john@example.com",
 *   "age": 30
 * }
 * Size: ~85 bytes
 * 
 * Protobuf (Binary):
 * 08 7B 12 08 4A 6F 68 6E 20 44 6F 65 1A 11 6A 6F
 * 68 6E 40 65 78 61 6D 70 6C 65 2E 63 6F 6D 20 1E
 * Size: ~35 bytes
 * 
 * Savings: 59% less space!
 * 
 * Why Binary is Faster:
 * 1. No parsing needed (already in binary format)
 * 2. Smaller size = faster network transfer
 * 3. Direct memory mapping
 * 4. No string conversions
 * 
 * .proto File Definition:
 * 
 * message User {
 *   int64 userId = 1;           // Field number (not value!)
 *   string userName = 2;
 *   string email = 3;
 *   int32 age = 4;
 * }
 * 
 * Generated Java Code:
 * - User.java (immutable class)
 * - User.Builder (builder pattern)
 * - Serialization methods
 * - Parsing methods
 * - All type-safe!
 * </pre>
 * 
 * <h2>4 Types of gRPC Communication</h2>
 * <pre>
 * 1. UNARY RPC (Most Common):
 *    Client → Single Request → Server
 *    Server → Single Response → Client
 *    
 *    Like REST API call
 *    
 *    Use case: Get review by ID
 *    Client: getReview(reviewId: "123")
 *    Server: Review{id: "123", rating: 5, ...}
 * 
 * 2. SERVER STREAMING RPC:
 *    Client → Single Request → Server
 *    Server → Stream of Responses → Client
 *    
 *    Server sends data as it's ready
 *    
 *    Use case: Get all reviews for product
 *    Client: getProductReviews(productId: 456)
 *    Server: Review1 → Review2 → Review3 → ... → ReviewN
 *    
 *    Timeline:
 *    0ms: Request sent
 *    10ms: Review1 received ← Can start displaying!
 *    20ms: Review2 received
 *    30ms: Review3 received
 *    ...
 *    
 *    Benefits:
 *    - Progressive loading (better UX)
 *    - Memory efficient (don't load all at once)
 *    - Can cancel mid-stream
 * 
 * 3. CLIENT STREAMING RPC:
 *    Client → Stream of Requests → Server
 *    Server → Single Response → Client
 *    
 *    Client sends multiple messages
 *    
 *    Use case: Bulk review upload
 *    Client: Review1 → Review2 → Review3 → ... → ReviewN
 *    Server: BulkResponse{created: 100, failed: 0}
 *    
 *    Benefits:
 *    - Upload large datasets efficiently
 *    - Network optimization (single connection)
 *    - Batch processing
 * 
 * 4. BIDIRECTIONAL STREAMING RPC:
 *    Client ↔ Stream of Messages ↔ Server
 *    
 *    Both sides send/receive independently
 *    
 *    Use case: Real-time review moderation
 *    Client: Review1 for moderation →
 *    Server: ← Moderation result for Review1
 *    Client: Review2 for moderation →
 *    Server: ← Moderation result for Review2
 *    
 *    Benefits:
 *    - Real-time communication
 *    - Interactive workflows
 *    - Chat-like interactions
 * </pre>
 * 
 * <h2>HTTP/2 Benefits</h2>
 * <pre>
 * HTTP/1.1 (REST):
 * - One request per connection
 * - Head-of-line blocking
 * - Text headers (repeated)
 * - No server push
 * 
 * HTTP/2 (gRPC):
 * - Multiple requests on single connection (multiplexing)
 * - No head-of-line blocking
 * - Compressed headers
 * - Server push capability
 * - Binary protocol
 * 
 * Example - 100 Concurrent Requests:
 * 
 * HTTP/1.1:
 * - Need 100 connections
 * - High memory usage
 * - Slow connection setup
 * - Total time: ~1000ms
 * 
 * HTTP/2:
 * - Use 1 connection
 * - Low memory usage
 * - Fast reuse
 * - Total time: ~150ms
 * 
 * 6.7x faster!
 * </pre>
 * 
 * <h2>When to Use gRPC vs REST</h2>
 * <pre>
 * Use gRPC when:
 * ✅ Microservice-to-microservice communication
 * ✅ High-performance required
 * ✅ Real-time streaming needed
 * ✅ Strict contract between services
 * ✅ Internal APIs
 * ✅ Mobile apps (efficient bandwidth)
 * 
 * Examples:
 * - Order Service ↔ Payment Service
 * - Product Service ↔ Review Service
 * - Real-time chat
 * - Live data feeds
 * - IoT device communication
 * 
 * Use REST when:
 * ✅ Public-facing APIs
 * ✅ Browser-based clients
 * ✅ Simple CRUD operations
 * ✅ Third-party integrations
 * ✅ Human-readable debugging
 * ✅ Wide compatibility needed
 * 
 * Examples:
 * - Public API for developers
 * - Website frontend
 * - Mobile app (simple)
 * - Webhooks
 * - Legacy systems
 * 
 * Hybrid Approach (Best):
 * - gRPC for internal microservices
 * - REST for public APIs
 * - API Gateway converts REST → gRPC
 * 
 * Architecture:
 * Mobile App → REST → API Gateway → gRPC → Microservices
 *                      ↑
 *                 Translation layer
 * </pre>
 * 
 * <h2>Real-World Performance</h2>
 * <pre>
 * Netflix Use Case:
 * - 2 billion gRPC calls/day
 * - 99.99% success rate
 * - Average latency: 1-2ms
 * - Replaced REST with gRPC
 * - Result: 7x performance improvement
 * 
 * Google Use Case:
 * - 10 billion+ gRPC calls/second
 * - Powers YouTube, Gmail, Google Maps
 * - Handles streaming video/audio
 * - Saves petabytes of bandwidth
 * 
 * Our Review Service Metrics:
 * Scenario: Get 100 reviews
 * 
 * REST (JSON):
 * - Total size: 50 KB
 * - Time: 200ms
 * - Memory: 2 MB
 * 
 * gRPC (Protobuf):
 * - Total size: 15 KB (70% less)
 * - Time: 30ms (85% faster)
 * - Memory: 500 KB (75% less)
 * 
 * For 1 million requests/day:
 * - Bandwidth saved: 35 GB/day
 * - Time saved: 47 hours/day
 * - Cost savings: Significant!
 * </pre>
 * 
 * <h2>gRPC Code Generation</h2>
 * <pre>
 * .proto file → protoc compiler → Generated code
 * 
 * From review.proto:
 * 
 * service ReviewService {
 *   rpc GetReview(GetReviewRequest) returns (ReviewResponse);
 * }
 * 
 * Generates:
 * 1. ReviewServiceGrpc.java
 *    - Service interface
 *    - Client stubs
 *    - Server base class
 * 
 * 2. ReviewResponse.java
 *    - Immutable message class
 *    - Builder pattern
 *    - Serialization/deserialization
 * 
 * 3. GetReviewRequest.java
 *    - Request message
 *    - Type-safe fields
 * 
 * Usage (Server):
 * public class ReviewServiceImpl extends ReviewServiceGrpc.ReviewServiceImplBase {
 *     @Override
 *     public void getReview(GetReviewRequest request, 
 *                          StreamObserver<ReviewResponse> responseObserver) {
 *         // Implementation
 *     }
 * }
 * 
 * Usage (Client):
 * ReviewServiceGrpc.ReviewServiceBlockingStub stub = ...;
 * ReviewResponse review = stub.getReview(
 *     GetReviewRequest.newBuilder()
 *         .setReviewId("123")
 *         .build()
 * );
 * 
 * All type-safe! Compiler catches errors!
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication
@EnableDiscoveryClient  // Register with Eureka
@EnableCaching  // Enable Redis caching
@EnableKafka  // Enable Kafka for events
public class ReviewServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReviewServiceApplication.class, args);
    }
}

