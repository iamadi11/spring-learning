package com.ecommerce.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Server Application - Service Discovery Server
 * 
 * <p>This is the central service registry for the microservices architecture.
 * All microservices register themselves with Eureka on startup and send periodic
 * heartbeats to indicate they are alive and healthy.</p>
 * 
 * <h2>Key Responsibilities:</h2>
 * <ul>
 *   <li><b>Service Registration:</b> Microservices register their network location (host, port) on startup</li>
 *   <li><b>Service Discovery:</b> Clients query Eureka to find available instances of services</li>
 *   <li><b>Health Monitoring:</b> Eureka tracks the health of each service instance via heartbeats</li>
 *   <li><b>Load Balancing Metadata:</b> Provides metadata for client-side load balancing</li>
 * </ul>
 * 
 * <h2>How Service Discovery Works:</h2>
 * <pre>
 * 1. Service Startup:
 *    - Service registers with Eureka: "I am USER-SERVICE at localhost:9002"
 *    - Eureka stores this information in its registry
 * 
 * 2. Service Lookup:
 *    - API Gateway needs to call USER-SERVICE
 *    - Gateway asks Eureka: "Where is USER-SERVICE?"
 *    - Eureka responds: "USER-SERVICE has 3 instances at: localhost:9002, localhost:9003, localhost:9004"
 *    - Gateway picks one instance (load balancing) and makes the call
 * 
 * 3. Health Checks:
 *    - Services send heartbeat every 30 seconds (default)
 *    - If Eureka doesn't receive heartbeat for 90 seconds, it removes the instance
 *    - This ensures clients don't call dead services
 * </pre>
 * 
 * <h2>Configuration Details:</h2>
 * <ul>
 *   <li><b>Port:</b> 8761 (standard Eureka port)</li>
 *   <li><b>URL:</b> http://localhost:8761</li>
 *   <li><b>Dashboard:</b> http://localhost:8761 (visual representation of registered services)</li>
 * </ul>
 * 
 * <h2>Annotations Explained:</h2>
 * <ul>
 *   <li><b>@SpringBootApplication:</b> Enables Spring Boot auto-configuration and component scanning</li>
 *   <li><b>@EnableEurekaServer:</b> Configures this application as a Eureka Server (not a client)</li>
 * </ul>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication  // Marks this as a Spring Boot application
@EnableEurekaServer     // Enables Eureka Server functionality
public class EurekaServerApplication {

    /**
     * Main method - Entry point for the Eureka Server application
     * 
     * <p>SpringApplication.run() performs the following:</p>
     * <ol>
     *   <li>Creates Spring ApplicationContext</li>
     *   <li>Scans for @Component, @Service, @Repository, @Controller beans</li>
     *   <li>Auto-configures Eureka Server based on classpath dependencies</li>
     *   <li>Starts embedded Tomcat server on port 8761</li>
     *   <li>Initializes Eureka registry and begins accepting service registrations</li>
     * </ol>
     * 
     * @param args Command-line arguments (not used in this application)
     */
    public static void main(String[] args) {
        // Start the Spring Boot application
        // This method blocks until the application is shut down
        SpringApplication.run(EurekaServerApplication.class, args);
        
        // Log message to indicate successful startup (Spring Boot logs this automatically)
        // Console will show: "Started EurekaServerApplication in X seconds"
    }
}

