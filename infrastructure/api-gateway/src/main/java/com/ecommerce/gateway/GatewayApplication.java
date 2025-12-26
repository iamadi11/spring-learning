package com.ecommerce.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway Application - Single Entry Point for All Microservices
 * 
 * <p>This is the edge service that acts as a single entry point for all client requests.
 * It provides routing, authentication, rate limiting, load balancing, and circuit breaking.</p>
 * 
 * <h2>Why API Gateway?</h2>
 * <ul>
 *   <li><b>Single Entry Point:</b> Clients only need to know one URL instead of multiple service URLs</li>
 *   <li><b>Cross-Cutting Concerns:</b> Handle authentication, logging, monitoring in one place</li>
 *   <li><b>Protocol Translation:</b> Convert HTTP to gRPC, or handle different API versions</li>
 *   <li><b>Security:</b> Centralized authentication and authorization</li>
 *   <li><b>Rate Limiting:</b> Protect backend services from abuse</li>
 *   <li><b>Load Balancing:</b> Distribute requests across multiple service instances</li>
 *   <li><b>Circuit Breaking:</b> Prevent cascade failures in distributed systems</li>
 * </ul>
 * 
 * <h2>Gateway Responsibilities:</h2>
 * <pre>
 * 1. Request Routing:
 *    - Client calls: http://localhost:8080/api/users/123
 *    - Gateway routes to: http://user-service:9002/api/users/123
 *    - Based on path matching rules defined in configuration
 * 
 * 2. Load Balancing:
 *    - If user-service has 3 instances (9002, 9003, 9004)
 *    - Gateway distributes requests using Round Robin or Least Connections
 *    - Queries Eureka for available instances
 * 
 * 3. Authentication & Authorization:
 *    - Extract JWT token from Authorization header
 *    - Validate token signature and expiry
 *    - Extract user roles and permissions
 *    - Add user info to request headers for downstream services
 *    - Reject requests with invalid/expired tokens
 * 
 * 4. Rate Limiting:
 *    - Limit requests per user/API key
 *    - Token Bucket algorithm: 100 requests per minute
 *    - Return 429 (Too Many Requests) when limit exceeded
 *    - Prevent DoS attacks and resource exhaustion
 * 
 * 5. Circuit Breaking:
 *    - Monitor service health
 *    - If service fails repeatedly (e.g., 50% failure rate)
 *    - Open circuit: Fast-fail without calling service
 *    - After timeout, try again (half-open state)
 *    - Close circuit if service recovers
 * 
 * 6. Request/Response Transformation:
 *    - Add correlation IDs for distributed tracing
 *    - Remove sensitive headers before forwarding
 *    - Add custom headers (user-id, tenant-id)
 *    - Modify response (add metadata, filter fields)
 * </pre>
 * 
 * <h2>Request Flow Example:</h2>
 * <pre>
 * Client → API Gateway → User Service
 *   ↓         ↓              ↓
 * 1. POST /api/auth/login
 * 2. Extract JWT token
 * 3. Validate token
 * 4. Check rate limit
 * 5. Route to auth-service
 * 6. Add headers
 * 7. Forward request
 * 8. Receive response
 * 9. Return to client
 * </pre>
 * 
 * <h2>Gateway Patterns Implemented:</h2>
 * <ul>
 *   <li><b>Gateway Routing:</b> Route requests based on path patterns</li>
 *   <li><b>Gateway Aggregation:</b> Combine multiple service calls into one response</li>
 *   <li><b>Gateway Offloading:</b> Handle cross-cutting concerns (auth, logging, CORS)</li>
 * </ul>
 * 
 * <h2>Configuration:</h2>
 * <ul>
 *   <li><b>Port:</b> 8080 (standard HTTP port)</li>
 *   <li><b>Framework:</b> Spring Cloud Gateway (reactive, non-blocking)</li>
 *   <li><b>Load Balancer:</b> Spring Cloud Load Balancer</li>
 * </ul>
 * 
 * <h2>Advantages of Spring Cloud Gateway:</h2>
 * <ul>
 *   <li><b>Reactive:</b> Built on Spring WebFlux (non-blocking, high throughput)</li>
 *   <li><b>Predicates:</b> Flexible routing based on headers, paths, methods, etc.</li>
 *   <li><b>Filters:</b> Pre and post-processing of requests/responses</li>
 *   <li><b>Integration:</b> Works seamlessly with Spring Cloud ecosystem</li>
 * </ul>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication      // Marks this as a Spring Boot application
@EnableDiscoveryClient      // Enables service discovery (Eureka) client
public class GatewayApplication {

    /**
     * Main method - Entry point for the API Gateway application
     * 
     * <p>SpringApplication.run() performs the following:</p>
     * <ol>
     *   <li>Creates Spring ApplicationContext (reactive WebFlux context)</li>
     *   <li>Registers with Eureka Server for health checks</li>
     *   <li>Fetches configuration from Config Server</li>
     *   <li>Initializes Gateway routes and filters</li>
     *   <li>Sets up circuit breakers and rate limiters</li>
     *   <li>Starts Netty server (reactive, non-blocking) on port 8080</li>
     *   <li>Begins accepting and routing client requests</li>
     * </ol>
     * 
     * <p>Gateway Health Check:</p>
     * <ul>
     *   <li>Endpoint: http://localhost:8080/actuator/health</li>
     *   <li>Gateway Routes: http://localhost:8080/actuator/gateway/routes</li>
     * </ul>
     * 
     * @param args Command-line arguments (not used in this application)
     */
    public static void main(String[] args) {
        // Start the Spring Boot application
        // Gateway is now ready to route requests to microservices
        SpringApplication.run(GatewayApplication.class, args);
        
        // All client requests should now go through: http://localhost:8080/api/...
        // Gateway will route to appropriate microservices based on path
    }
}

