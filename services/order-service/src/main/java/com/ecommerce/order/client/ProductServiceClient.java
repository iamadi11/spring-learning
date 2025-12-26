package com.ecommerce.order.client;

import com.ecommerce.order.dto.ReserveInventoryRequest;
import com.ecommerce.order.dto.ReleaseInventoryRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Product Service Client
 * 
 * <p>Feign client for communicating with Product Service.
 * Used by saga steps to reserve and release inventory.</p>
 * 
 * <h2>Feign Client Features:</h2>
 * <pre>
 * 1. Service Discovery:
 *    @FeignClient(name = "product-service")
 *    - Looks up service from Eureka
 *    - Load balances across instances
 *    - No hardcoded URLs
 * 
 * 2. Circuit Breaker:
 *    @CircuitBreaker(name = "productService", fallbackMethod = "...")
 *    - Opens after 50% failure rate
 *    - Prevents cascading failures
 *    - Fast-fail when service is down
 * 
 * 3. Retry:
 *    @Retry(name = "productService")
 *    - Retries on transient failures
 *    - Exponential backoff
 *    - Max 3 attempts
 * 
 * 4. Timeout:
 *    - 10 seconds per call
 *    - Fails fast if service slow
 * </pre>
 * 
 * <h2>Circuit Breaker Pattern:</h2>
 * <pre>
 * States:
 * 
 * CLOSED (Normal):
 * - Requests go through
 * - Track failures
 * - Open if threshold exceeded
 * 
 * OPEN (Service Down):
 * - Fail fast
 * - Don't call service
 * - Try again after wait period
 * 
 * HALF_OPEN (Testing):
 * - Allow limited requests
 * - Close if success
 * - Re-open if failure
 * 
 * Example:
 * 10 requests → 6 failures → Circuit OPENS
 * Wait 10 seconds
 * Try 3 requests → All success → Circuit CLOSES
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@FeignClient(
    name = "product-service",  // Service name from Eureka
    fallback = ProductServiceClientFallback.class  // Fallback on circuit open
)
public interface ProductServiceClient {

    /**
     * Reserve Inventory
     * 
     * <p>Reserves product inventory for an order.
     * Prevents overselling by locking inventory.</p>
     * 
     * <p><b>Idempotent:</b> Can be called multiple times with same orderId</p>
     * 
     * @param request Reservation request
     */
    @PostMapping("/api/products/reserve")
    @CircuitBreaker(name = "productService")
    @Retry(name = "productService")
    void reserveInventory(@RequestBody ReserveInventoryRequest request);

    /**
     * Release Inventory
     * 
     * <p>Releases previously reserved inventory.
     * Called during saga compensation.</p>
     * 
     * <p><b>Idempotent:</b> Safe to call even if not reserved</p>
     * 
     * @param request Release request
     */
    @PostMapping("/api/products/release")
    @CircuitBreaker(name = "productService")
    @Retry(name = "productService")
    void releaseInventory(@RequestBody ReleaseInventoryRequest request);
}

