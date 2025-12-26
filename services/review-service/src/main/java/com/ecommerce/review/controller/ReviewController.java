package com.ecommerce.review.controller;

import com.ecommerce.review.dto.ReviewRequest;
import com.ecommerce.review.dto.ReviewResponse;
import com.ecommerce.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Review REST Controller
 * 
 * <p>REST API endpoints for review management.</p>
 * 
 * <h2>Hybrid Architecture: REST + gRPC</h2>
 * <pre>
 * This service provides BOTH:
 * 
 * 1. REST API (this controller):
 *    - For public-facing APIs
 *    - For browser/mobile clients
 *    - Human-readable JSON
 *    - Port: 8087
 * 
 * 2. gRPC API (ReviewServiceGrpcImpl):
 *    - For internal microservices
 *    - For high-performance communication
 *    - Binary Protocol Buffers
 *    - Port: 9090
 * 
 * Architecture:
 * 
 * Mobile App → REST (8087) → Review Service → Database
 *                                    ↓
 * Product Service → gRPC (9090) ────┘
 * 
 * Benefits:
 * - Best of both worlds
 * - Public APIs use REST (accessible)
 * - Internal APIs use gRPC (fast)
 * - Same business logic
 * - Different protocols
 * 
 * Example:
 * - User creates review via REST
 * - Product Service gets stats via gRPC
 * - Both work seamlessly!
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Create review
     * 
     * POST /api/reviews
     */
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
        log.info("REST: Create review for product: {}", request.getProductId());

        ReviewResponse response = reviewService.createReview(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get review by ID
     * 
     * GET /api/reviews/{reviewId}
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable String reviewId) {
        log.info("REST: Get review: {}", reviewId);

        ReviewResponse response = reviewService.getReview(reviewId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get product reviews (paginated)
     * 
     * GET /api/reviews/product/{productId}?page=0&size=20
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("REST: Get reviews for product: {} (page={}, size={})", productId, page, size);

        Page<ReviewResponse> reviews = reviewService.getProductReviews(productId, page, size);

        return ResponseEntity.ok(reviews);
    }

    /**
     * Update review
     * 
     * PUT /api/reviews/{reviewId}
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable String reviewId,
            @Valid @RequestBody ReviewRequest request) {
        
        log.info("REST: Update review: {}", reviewId);

        ReviewResponse response = reviewService.updateReview(reviewId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete review
     * 
     * DELETE /api/reviews/{reviewId}
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable String reviewId) {
        log.info("REST: Delete review: {}", reviewId);

        reviewService.deleteReview(reviewId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Mark review as helpful
     * 
     * POST /api/reviews/{reviewId}/helpful
     */
    @PostMapping("/{reviewId}/helpful")
    public ResponseEntity<Void> markHelpful(@PathVariable String reviewId) {
        log.info("REST: Mark review as helpful: {}", reviewId);

        reviewService.markHelpful(reviewId);

        return ResponseEntity.ok().build();
    }

    /**
     * Health check
     * 
     * GET /api/reviews/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Review Service is running");
    }
}

