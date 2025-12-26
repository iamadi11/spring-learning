package com.ecommerce.review.grpc;

import com.ecommerce.review.entity.Review;
import com.ecommerce.review.entity.ReviewStatus;
import com.ecommerce.review.repository.ReviewRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * gRPC Review Service Implementation
 * 
 * <p>Demonstrates ALL 4 types of gRPC communication patterns.</p>
 * 
 * <h2>@GrpcService Annotation:</h2>
 * <pre>
 * What it does:
 * - Registers this class as gRPC service
 * - Makes it available on configured port (9090)
 * - Handles incoming gRPC calls
 * - Automatic service discovery
 * 
 * Similar to @RestController in REST
 * But for gRPC!
 * </pre>
 * 
 * <h2>StreamObserver Explained:</h2>
 * <pre>
 * What is StreamObserver?
 * - Interface for sending responses back to client
 * - Asynchronous response handling
 * - Three key methods:
 *   1. onNext(response)    - Send response
 *   2. onError(throwable)  - Send error
 *   3. onCompleted()       - Signal completion
 * 
 * Example Flow:
 * 1. Client makes request
 * 2. Server receives StreamObserver
 * 3. Server calls onNext() with response
 * 4. Server calls onCompleted()
 * 5. Client receives response
 * 
 * For streaming:
 * - Call onNext() multiple times
 * - Each call sends one message
 * - Call onCompleted() when done
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Slf4j
@GrpcService  // Register as gRPC service
@RequiredArgsConstructor
public class ReviewServiceGrpcImpl extends ReviewServiceGrpc.ReviewServiceImplBase {

    private final ReviewRepository reviewRepository;

    /**
     * PATTERN 1: UNARY RPC
     * 
     * <p>Single request → Single response</p>
     * 
     * <h2>How it works:</h2>
     * <pre>
     * 1. Client sends CreateReviewRequest
     * 2. Server receives request
     * 3. Server creates review in database
     * 4. Server builds ReviewResponse
     * 5. Server calls onNext(response)
     * 6. Server calls onCompleted()
     * 7. Client receives response
     * 
     * Timeline:
     * 0ms: Client sends request
     * 5ms: Server receives
     * 10ms: Database insert
     * 15ms: Response built
     * 16ms: onNext() called
     * 17ms: onCompleted() called
     * 20ms: Client receives response
     * 
     * Total: 20ms (very fast!)
     * </pre>
     * 
     * <h2>Error Handling:</h2>
     * <pre>
     * If error occurs:
     * - Don't call onNext()
     * - Call onError(throwable)
     * - Client receives error
     * - No onCompleted() needed
     * </pre>
     */
    @Override
    public void createReview(CreateReviewRequest request, 
                            StreamObserver<ReviewResponse> responseObserver) {
        log.info("gRPC: Create review for product: {}", request.getProductId());

        try {
            // Validate request
            validateCreateRequest(request);

            // Create review entity
            Review review = Review.builder()
                .id(UUID.randomUUID().toString())
                .productId(request.getProductId())
                .userId(request.getUserId())
                .userName("User " + request.getUserId())  // In production: fetch from User Service
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .verifiedPurchase(request.getVerifiedPurchase())
                .helpfulCount(0)
                .unhelpfulCount(0)
                .status(ReviewStatus.APPROVED)  // Auto-approve for demo
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

            // Save to database
            review = reviewRepository.save(review);

            // Convert to gRPC response
            ReviewResponse response = convertToGrpcResponse(review);

            // Send response
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("gRPC: Review created successfully: {}", review.getId());

        } catch (Exception e) {
            log.error("gRPC: Failed to create review: {}", e.getMessage());
            responseObserver.onError(
                io.grpc.Status.INTERNAL
                    .withDescription("Failed to create review: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    /**
     * UNARY RPC: Get single review
     */
    @Override
    public void getReview(GetReviewRequest request, 
                         StreamObserver<ReviewResponse> responseObserver) {
        log.info("gRPC: Get review: {}", request.getReviewId());

        try {
            Review review = reviewRepository.findById(request.getReviewId())
                .orElseThrow(() -> new RuntimeException("Review not found"));

            ReviewResponse response = convertToGrpcResponse(review);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC: Failed to get review: {}", e.getMessage());
            responseObserver.onError(
                io.grpc.Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    /**
     * UNARY RPC: Update review
     */
    @Override
    public void updateReview(UpdateReviewRequest request, 
                            StreamObserver<ReviewResponse> responseObserver) {
        log.info("gRPC: Update review: {}", request.getReviewId());

        try {
            Review review = reviewRepository.findById(request.getReviewId())
                .orElseThrow(() -> new RuntimeException("Review not found"));

            // Update fields
            review.setRating(request.getRating());
            review.setTitle(request.getTitle());
            review.setComment(request.getComment());
            review.setUpdatedAt(LocalDateTime.now());

            review = reviewRepository.save(review);

            ReviewResponse response = convertToGrpcResponse(review);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("gRPC: Review updated successfully");

        } catch (Exception e) {
            log.error("gRPC: Failed to update review: {}", e.getMessage());
            responseObserver.onError(
                io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    /**
     * UNARY RPC: Delete review
     */
    @Override
    public void deleteReview(DeleteReviewRequest request, 
                            StreamObserver<DeleteReviewResponse> responseObserver) {
        log.info("gRPC: Delete review: {}", request.getReviewId());

        try {
            reviewRepository.deleteById(request.getReviewId());

            DeleteReviewResponse response = DeleteReviewResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Review deleted successfully")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC: Failed to delete review: {}", e.getMessage());
            responseObserver.onError(
                io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    /**
     * PATTERN 2: SERVER STREAMING RPC
     * 
     * <p>Single request → Stream of responses</p>
     * 
     * <h2>How it works:</h2>
     * <pre>
     * 1. Client sends GetProductReviewsRequest
     * 2. Server queries database
     * 3. Server sends reviews ONE BY ONE:
     *    - onNext(review1)
     *    - onNext(review2)
     *    - onNext(review3)
     *    - ... 
     *    - onNext(reviewN)
     * 4. Server calls onCompleted()
     * 5. Client receives all reviews as stream
     * 
     * Timeline (100 reviews):
     * 0ms: Request received
     * 10ms: Database query
     * 11ms: onNext(review1)    ← Client receives!
     * 12ms: onNext(review2)    ← Client receives!
     * 13ms: onNext(review3)    ← Client receives!
     * ...
     * 110ms: onNext(review100) ← Client receives!
     * 111ms: onCompleted()
     * 
     * Benefits:
     * 1. Progressive loading
     *    - Client can display reviews as they arrive
     *    - Better UX (no waiting for all data)
     * 
     * 2. Memory efficient
     *    - Server doesn't load all reviews at once
     *    - Client doesn't need to buffer all
     * 
     * 3. Cancellable
     *    - Client can cancel mid-stream
     *    - Server stops sending
     * 
     * vs UNARY (all at once):
     * 0ms: Request
     * 10ms: Query all reviews
     * 50ms: Build huge response
     * 100ms: Send all (may timeout!)
     * 200ms: Client receives
     * 
     * Streaming: Better for large datasets!
     * </pre>
     * 
     * <h2>Java Streams Integration:</h2>
     * <pre>
     * Using Java Stream API with gRPC streaming:
     * 
     * Stream<Review> reviews = repository.findByProductId(...);
     * 
     * reviews.forEach(review -> {
     *     ReviewResponse response = convert(review);
     *     responseObserver.onNext(response);  // Stream each
     * });
     * 
     * responseObserver.onCompleted();
     * 
     * Efficient: Database streams directly to gRPC!
     * No intermediate collection!
     * </pre>
     */
    @Override
    public void getProductReviews(GetProductReviewsRequest request, 
                                 StreamObserver<ReviewResponse> responseObserver) {
        log.info("gRPC: Get product reviews (STREAMING): product={}", request.getProductId());

        try {
            // Use Java Stream for efficient processing
            Stream<Review> reviews = reviewRepository
                .findByProductIdAndStatus(request.getProductId(), ReviewStatus.APPROVED);

            // Stream each review to client
            AtomicInteger count = new AtomicInteger(0);
            
            reviews.forEach(review -> {
                ReviewResponse response = convertToGrpcResponse(review);
                
                // Send each review immediately (streaming!)
                responseObserver.onNext(response);
                
                count.incrementAndGet();
                
                // Simulate processing time (in production: remove this)
                try {
                    Thread.sleep(10);  // 10ms per review
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // Signal completion
            responseObserver.onCompleted();

            log.info("gRPC: Streamed {} reviews successfully", count.get());

        } catch (Exception e) {
            log.error("gRPC: Failed to stream reviews: {}", e.getMessage());
            responseObserver.onError(
                io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    /**
     * SERVER STREAMING RPC: Get user reviews
     */
    @Override
    public void getUserReviews(GetUserReviewsRequest request, 
                              StreamObserver<ReviewResponse> responseObserver) {
        log.info("gRPC: Get user reviews (STREAMING): user={}", request.getUserId());

        try {
            Stream<Review> reviews = reviewRepository
                .findByUserIdAndStatus(request.getUserId(), ReviewStatus.APPROVED);

            reviews.forEach(review -> {
                ReviewResponse response = convertToGrpcResponse(review);
                responseObserver.onNext(response);
            });

            responseObserver.onCompleted();

            log.info("gRPC: User reviews streamed successfully");

        } catch (Exception e) {
            log.error("gRPC: Failed to stream user reviews: {}", e.getMessage());
            responseObserver.onError(
                io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    /**
     * PATTERN 3: CLIENT STREAMING RPC
     * 
     * <p>Stream of requests → Single response</p>
     * 
     * <h2>How it works:</h2>
     * <pre>
     * 1. Client opens stream
     * 2. Client sends review1
     * 3. Client sends review2
     * 4. Client sends review3
     * 5. ...
     * 6. Client sends reviewN
     * 7. Client signals done (half-close)
     * 8. Server processes all reviews
     * 9. Server sends single BulkReviewResponse
     * 10. Server calls onCompleted()
     * 
     * Timeline (100 reviews):
     * 0ms: Stream opened
     * 1ms: Review 1 received
     * 2ms: Review 2 received
     * 3ms: Review 3 received
     * ...
     * 100ms: Review 100 received
     * 101ms: Client half-closes
     * 150ms: Server processes all
     * 200ms: Single response sent
     * 
     * Use cases:
     * - Bulk data upload
     * - CSV import
     * - Data migration
     * - Batch processing
     * 
     * Benefits:
     * 1. Single connection for all reviews
     * 2. Network efficient (reuse connection)
     * 3. Can track progress
     * 4. Transaction-like (all or nothing)
     * </pre>
     * 
     * <h2>Return Type:</h2>
     * <pre>
     * Returns StreamObserver<CreateReviewRequest>
     * 
     * This is the RECEIVER for incoming stream
     * 
     * Methods:
     * - onNext(request)     - Called for each review
     * - onError(throwable)  - Called on error
     * - onCompleted()       - Called when client done
     * 
     * We return our own StreamObserver that:
     * - Collects all reviews
     * - Processes them
     * - Sends single response
     * </pre>
     */
    @Override
    public StreamObserver<CreateReviewRequest> bulkCreateReviews(
            StreamObserver<BulkReviewResponse> responseObserver) {
        
        log.info("gRPC: Bulk create reviews (CLIENT STREAMING) started");

        // Return a StreamObserver to receive client stream
        return new StreamObserver<CreateReviewRequest>() {
            private final AtomicInteger totalReceived = new AtomicInteger(0);
            private final AtomicInteger totalCreated = new AtomicInteger(0);
            private final AtomicInteger totalFailed = new AtomicInteger(0);

            /**
             * Called for EACH review sent by client
             */
            @Override
            public void onNext(CreateReviewRequest request) {
                totalReceived.incrementAndGet();
                
                log.debug("gRPC: Received review #{} for product: {}", 
                         totalReceived.get(), request.getProductId());

                try {
                    // Create review
                    Review review = Review.builder()
                        .id(UUID.randomUUID().toString())
                        .productId(request.getProductId())
                        .userId(request.getUserId())
                        .userName("User " + request.getUserId())
                        .rating(request.getRating())
                        .title(request.getTitle())
                        .comment(request.getComment())
                        .verifiedPurchase(request.getVerifiedPurchase())
                        .helpfulCount(0)
                        .status(ReviewStatus.APPROVED)
                        .createdAt(LocalDateTime.now())
                        .build();

                    reviewRepository.save(review);
                    totalCreated.incrementAndGet();

                } catch (Exception e) {
                    log.error("gRPC: Failed to create review: {}", e.getMessage());
                    totalFailed.incrementAndGet();
                }
            }

            /**
             * Called when client encounters error
             */
            @Override
            public void onError(Throwable t) {
                log.error("gRPC: Client streaming error: {}", t.getMessage());
                responseObserver.onError(t);
            }

            /**
             * Called when client finishes sending (half-close)
             * This is where we send our response!
             */
            @Override
            public void onCompleted() {
                log.info("gRPC: Client streaming completed - Received: {}, Created: {}, Failed: {}", 
                        totalReceived.get(), totalCreated.get(), totalFailed.get());

                // Build single response with summary
                BulkReviewResponse response = BulkReviewResponse.newBuilder()
                    .setTotalReceived(totalReceived.get())
                    .setTotalCreated(totalCreated.get())
                    .setTotalFailed(totalFailed.get())
                    .build();

                // Send response
                responseObserver.onNext(response);
                responseObserver.onCompleted();

                log.info("gRPC: Bulk review response sent");
            }
        };
    }

    /**
     * PATTERN 4: BIDIRECTIONAL STREAMING RPC
     * 
     * <p>Stream of requests ↔ Stream of responses</p>
     * 
     * <h2>How it works:</h2>
     * <pre>
     * Client and Server both send/receive independently
     * 
     * Timeline:
     * 0ms: Stream opened
     * 1ms: Client → Review1 for moderation
     * 5ms: Server → Moderation result for Review1
     * 10ms: Client → Review2 for moderation
     * 12ms: Client → Review3 for moderation
     * 15ms: Server → Moderation result for Review2
     * 18ms: Server → Moderation result for Review3
     * 20ms: Client → Review4 for moderation
     * ...
     * 
     * Both streams are independent!
     * 
     * Use cases:
     * - Real-time chat
     * - Live data feeds
     * - Interactive workflows
     * - Game servers
     * - Collaborative editing
     * 
     * Benefits:
     * 1. Full-duplex communication
     * 2. Real-time interaction
     * 3. Low latency
     * 4. Both sides control flow
     * </pre>
     * 
     * <h2>Implementation:</h2>
     * <pre>
     * Return StreamObserver for receiving requests
     * Use provided StreamObserver for sending responses
     * 
     * For each incoming request:
     * 1. Process it
     * 2. Send response immediately
     * 3. Don't wait for other requests
     * 
     * Independent streams!
     * </pre>
     */
    @Override
    public StreamObserver<ReviewModerationRequest> moderateReviews(
            StreamObserver<ReviewModerationResponse> responseObserver) {
        
        log.info("gRPC: Moderate reviews (BIDIRECTIONAL STREAMING) started");

        return new StreamObserver<ReviewModerationRequest>() {
            
            /**
             * Called for each moderation request
             * Process and respond immediately!
             */
            @Override
            public void onNext(ReviewModerationRequest request) {
                log.info("gRPC: Moderating review: {} - Action: {}", 
                        request.getReviewId(), request.getAction());

                try {
                    // Find review
                    Review review = reviewRepository.findById(request.getReviewId())
                        .orElseThrow(() -> new RuntimeException("Review not found"));

                    // Apply moderation action
                    ReviewStatus newStatus = switch (request.getAction().toLowerCase()) {
                        case "approve" -> ReviewStatus.APPROVED;
                        case "reject" -> ReviewStatus.REJECTED;
                        case "flag" -> ReviewStatus.FLAGGED;
                        default -> review.getStatus();
                    };

                    review.setStatus(newStatus);
                    review.setModerationReason(request.getReason());
                    review.setUpdatedAt(LocalDateTime.now());
                    reviewRepository.save(review);

                    // Send response immediately (bidirectional!)
                    ReviewModerationResponse response = ReviewModerationResponse.newBuilder()
                        .setReviewId(request.getReviewId())
                        .setSuccess(true)
                        .setStatus(newStatus.name())
                        .setMessage("Review " + request.getAction() + "ed successfully")
                        .build();

                    responseObserver.onNext(response);

                    log.info("gRPC: Moderation response sent for review: {}", request.getReviewId());

                } catch (Exception e) {
                    log.error("gRPC: Moderation failed: {}", e.getMessage());

                    // Send error response
                    ReviewModerationResponse errorResponse = ReviewModerationResponse.newBuilder()
                        .setReviewId(request.getReviewId())
                        .setSuccess(false)
                        .setMessage("Moderation failed: " + e.getMessage())
                        .build();

                    responseObserver.onNext(errorResponse);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("gRPC: Bidirectional streaming error: {}", t.getMessage());
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                log.info("gRPC: Moderation streaming completed");
                responseObserver.onCompleted();
            }
        };
    }

    /**
     * UNARY RPC: Get review statistics
     */
    @Override
    public void getReviewStats(GetReviewStatsRequest request, 
                              StreamObserver<ReviewStatsResponse> responseObserver) {
        log.info("gRPC: Get review stats for product: {}", request.getProductId());

        try {
            Long productId = request.getProductId();

            // Count total reviews
            long totalReviews = reviewRepository.countByProductIdAndStatus(
                productId, ReviewStatus.APPROVED);

            // Count by rating
            long fiveStarCount = reviewRepository.countByProductIdAndStatusAndRating(
                productId, ReviewStatus.APPROVED, 5);
            long fourStarCount = reviewRepository.countByProductIdAndStatusAndRating(
                productId, ReviewStatus.APPROVED, 4);
            long threeStarCount = reviewRepository.countByProductIdAndStatusAndRating(
                productId, ReviewStatus.APPROVED, 3);
            long twoStarCount = reviewRepository.countByProductIdAndStatusAndRating(
                productId, ReviewStatus.APPROVED, 2);
            long oneStarCount = reviewRepository.countByProductIdAndStatusAndRating(
                productId, ReviewStatus.APPROVED, 1);

            // Count verified purchases
            long verifiedCount = reviewRepository.countByProductIdAndStatusAndVerifiedPurchase(
                productId, ReviewStatus.APPROVED, true);

            // Calculate average rating
            double avgRating = calculateAverageRating(productId);

            // Build response
            ReviewStatsResponse response = ReviewStatsResponse.newBuilder()
                .setProductId(productId)
                .setAverageRating(avgRating)
                .setTotalReviews((int) totalReviews)
                .setFiveStarCount((int) fiveStarCount)
                .setFourStarCount((int) fourStarCount)
                .setThreeStarCount((int) threeStarCount)
                .setTwoStarCount((int) twoStarCount)
                .setOneStarCount((int) oneStarCount)
                .setVerifiedPurchaseCount((int) verifiedCount)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("gRPC: Review stats sent - Avg: {}, Total: {}", avgRating, totalReviews);

        } catch (Exception e) {
            log.error("gRPC: Failed to get review stats: {}", e.getMessage());
            responseObserver.onError(
                io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    /**
     * Helper: Convert entity to gRPC response
     */
    private ReviewResponse convertToGrpcResponse(Review review) {
        return ReviewResponse.newBuilder()
            .setReviewId(review.getId())
            .setProductId(review.getProductId())
            .setUserId(review.getUserId())
            .setUserName(review.getUserName())
            .setRating(review.getRating())
            .setTitle(review.getTitle())
            .setComment(review.getComment())
            .setVerifiedPurchase(review.getVerifiedPurchase())
            .setHelpfulCount(review.getHelpfulCount())
            .setStatus(review.getStatus().name())
            .setCreatedAt(review.getCreatedAt().toEpochSecond(ZoneOffset.UTC))
            .setUpdatedAt(review.getUpdatedAt().toEpochSecond(ZoneOffset.UTC))
            .build();
    }

    /**
     * Helper: Validate create request
     */
    private void validateCreateRequest(CreateReviewRequest request) {
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        if (request.getTitle().length() > 100) {
            throw new IllegalArgumentException("Title too long");
        }
        if (request.getComment().length() > 5000) {
            throw new IllegalArgumentException("Comment too long");
        }
    }

    /**
     * Helper: Calculate average rating
     */
    private double calculateAverageRating(Long productId) {
        var reviews = reviewRepository.findRatingsByProductId(productId);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        
        double sum = reviews.stream()
            .mapToInt(Review::getRating)
            .sum();
        
        return sum / reviews.size();
    }
}

