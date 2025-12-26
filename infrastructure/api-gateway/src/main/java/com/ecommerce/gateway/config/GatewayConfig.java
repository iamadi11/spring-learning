package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway Configuration - Programmatic Route Configuration
 * 
 * <p>This class provides programmatic configuration of Gateway routes as an alternative
 * to YAML configuration. It demonstrates how to define routes, predicates, and filters
 * using Java code.</p>
 * 
 * <h2>Why Programmatic Configuration?</h2>
 * <ul>
 *   <li><b>Type Safety:</b> Compile-time checking of configuration</li>
 *   <li><b>Dynamic Routes:</b> Create routes based on runtime conditions</li>
 *   <li><b>Complex Logic:</b> Implement complex routing logic not possible in YAML</li>
 *   <li><b>Testing:</b> Easier to unit test route configuration</li>
 * </ul>
 * 
 * <h2>Route Components:</h2>
 * <pre>
 * Route = ID + URI + Predicates + Filters
 * 
 * - ID: Unique identifier for the route
 * - URI: Destination address (can be lb:// for load-balanced)
 * - Predicates: Conditions to match (path, header, method, etc.)
 * - Filters: Transformations to apply (add header, rewrite path, etc.)
 * </pre>
 * 
 * <h2>Predicate Types:</h2>
 * <ul>
 *   <li><b>Path:</b> Match request path - path("/api/users/**")</li>
 *   <li><b>Method:</b> Match HTTP method - method("GET", "POST")</li>
 *   <li><b>Header:</b> Match header value - header("X-Request-Id", "\\d+")</li>
 *   <li><b>Query:</b> Match query parameter - query("version", "v2")</li>
 *   <li><b>Host:</b> Match host header - host("*.example.com")</li>
 *   <li><b>Cookie:</b> Match cookie - cookie("session", "\\w+")</li>
 *   <li><b>Before/After:</b> Match time window - before/after(ZonedDateTime)</li>
 * </ul>
 * 
 * <h2>Filter Types:</h2>
 * <ul>
 *   <li><b>AddRequestHeader:</b> Add header to request</li>
 *   <li><b>AddResponseHeader:</b> Add header to response</li>
 *   <li><b>RewritePath:</b> Modify request path</li>
 *   <li><b>StripPrefix:</b> Remove path segments</li>
 *   <li><b>SetStatus:</b> Set response status</li>
 *   <li><b>RedirectTo:</b> Redirect to different URL</li>
 *   <li><b>Retry:</b> Retry failed requests</li>
 *   <li><b>CircuitBreaker:</b> Apply circuit breaker pattern</li>
 * </ul>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration  // Marks this class as a configuration class
public class GatewayConfig {

    /**
     * Define custom routes programmatically
     * 
     * <p>This bean creates additional routes beyond those defined in application.yml.
     * It demonstrates how to create routes with complex predicates and filters.</p>
     * 
     * <p>Note: Routes defined here are merged with YAML-defined routes.
     * If you want to replace YAML routes entirely, remove them from application.yml</p>
     * 
     * @param builder RouteLocatorBuilder - DSL for building routes
     * @return RouteLocator containing all programmatically defined routes
     */
    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        // Return a RouteLocator with custom route definitions
        return builder.routes()
            
            // Example 1: Route with multiple predicates
            // This route only matches GET requests to /api/products with query param "featured=true"
            .route("featured_products_route", route -> route
                .path("/api/products")                    // Match path
                .and()                                     // AND condition
                .method("GET")                             // Match method
                .and()                                     // Another AND condition
                .query("featured", "true")                 // Match query parameter
                .filters(filter -> filter
                    .addRequestHeader("X-Featured-Request", "true")  // Add custom header
                    .addResponseHeader("X-Processed-By", "API-Gateway")  // Add response header
                )
                .uri("lb://product-service")               // Load-balanced URI
            )
            
            // Example 2: Route with path rewriting
            // Rewrite /api/v2/users to /api/users with version header
            .route("api_versioning_route", route -> route
                .path("/api/v2/**")                        // Match versioned API
                .filters(filter -> filter
                    .rewritePath("/api/v2/(?<segment>.*)", "/api/${segment}")  // Remove version from path
                    .addRequestHeader("API-Version", "v2")  // Add version as header
                )
                .uri("lb://user-service")
            )
            
            // Example 3: Route with custom headers for admin
            // Add special headers for admin endpoints
            .route("admin_route", route -> route
                .path("/api/admin/**")                     // Match admin paths
                .filters(filter -> filter
                    .addRequestHeader("X-Admin-Request", "true")
                    .addRequestHeader("X-Require-Role", "ADMIN")
                )
                .uri("lb://user-service")
            )
            
            // Example 4: Health check aggregation
            // Aggregate health checks from all services
            .route("health_check_route", route -> route
                .path("/health")
                .filters(filter -> filter
                    .setPath("/actuator/health")           // Rewrite to actuator endpoint
                )
                .uri("lb://user-service")                  // Can be any service
            )
            
            // Build and return the RouteLocator
            .build();
    }
    
    /**
     * Custom filter configuration example
     * 
     * <p>This method would define custom gateway filter factories.
     * Gateway filters can be applied globally or to specific routes.</p>
     * 
     * <p>Example filters we might create:</p>
     * <ul>
     *   <li>AuthenticationFilter - Validate JWT tokens</li>
     *   <li>RateLimitFilter - Apply rate limiting per user</li>
     *   <li>LoggingFilter - Log requests and responses</li>
     *   <li>MetricsFilter - Collect metrics for monitoring</li>
     * </ul>
     * 
     * @return Configuration for custom filters
     */
    // Custom filters are defined as separate classes (see filter package)
    
    /**
     * Load Balancer configuration
     * 
     * <p>Spring Cloud Load Balancer is used for client-side load balancing.
     * When URI starts with lb://, Gateway uses Eureka to find service instances
     * and applies load balancing algorithm (default: Round Robin).</p>
     * 
     * <p>Load Balancing Strategies:</p>
     * <ul>
     *   <li><b>Round Robin:</b> Distribute requests evenly across instances</li>
     *   <li><b>Random:</b> Randomly select instance</li>
     *   <li><b>Weighted:</b> Instances with higher weight get more requests</li>
     *   <li><b>Least Connections:</b> Route to instance with fewest active connections</li>
     * </ul>
     * 
     * <p>Default is Round Robin, which works well for most use cases.</p>
     */
    // Load balancer configuration is automatic with Eureka integration
}

