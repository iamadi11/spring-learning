package com.ecommerce.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Config Server Application - Centralized Configuration Management
 * 
 * <p>This server provides centralized configuration management for all microservices.
 * Instead of having configuration files bundled in each service, configurations are
 * externalized and stored in a central location (Git repository or file system).</p>
 * 
 * <h2>Why Centralized Configuration?</h2>
 * <ul>
 *   <li><b>Consistency:</b> Same configuration source for all environments (dev, test, prod)</li>
 *   <li><b>Security:</b> Sensitive data (passwords, API keys) can be encrypted</li>
 *   <li><b>Dynamic Updates:</b> Change configuration without redeploying services</li>
 *   <li><b>Version Control:</b> Track configuration changes using Git history</li>
 *   <li><b>Environment-Specific:</b> Different configs for dev, test, prod with profiles</li>
 * </ul>
 * 
 * <h2>How Config Server Works:</h2>
 * <pre>
 * 1. Configuration Storage:
 *    - Configurations stored in Git repository (or local file system)
 *    - Directory structure:
 *      config-repo/
 *        ├── application.yml              (common config for all services)
 *        ├── auth-service.yml             (auth service specific config)
 *        ├── auth-service-dev.yml         (auth service dev profile)
 *        ├── auth-service-prod.yml        (auth service prod profile)
 *        └── ...
 * 
 * 2. Service Startup:
 *    - Auth Service starts up
 *    - Reads bootstrap.yml to find Config Server URL
 *    - Calls: GET http://localhost:8888/auth-service/dev
 *    - Config Server fetches: application.yml + auth-service.yml + auth-service-dev.yml
 *    - Returns merged configuration
 *    - Auth Service applies configuration
 * 
 * 3. Runtime Configuration Refresh:
 *    - Update configuration in Git
 *    - Call: POST http://auth-service:9001/actuator/refresh
 *    - Service reloads configuration without restart
 *    - Or use Spring Cloud Bus for cluster-wide refresh
 * </pre>
 * 
 * <h2>Configuration URL Pattern:</h2>
 * <pre>
 * /{application}/{profile}[/{label}]
 * 
 * Examples:
 * - http://localhost:8888/auth-service/dev         (dev profile)
 * - http://localhost:8888/auth-service/prod        (prod profile)
 * - http://localhost:8888/order-service/test       (test profile)
 * </pre>
 * 
 * <h2>Encryption/Decryption:</h2>
 * <pre>
 * Encrypt sensitive data:
 *   POST http://localhost:8888/encrypt
 *   Body: my-secret-password
 *   Response: {cipher}AQA3dF8jK9mN7pQ...
 * 
 * Store in config file:
 *   database:
 *     password: '{cipher}AQA3dF8jK9mN7pQ...'
 * 
 * Config Server automatically decrypts before sending to services
 * </pre>
 * 
 * <h2>Configuration:</h2>
 * <ul>
 *   <li><b>Port:</b> 8888 (standard Config Server port)</li>
 *   <li><b>Storage:</b> Local file system (classpath:/config-repo)</li>
 *   <li><b>Alternative:</b> Can use Git, SVN, or Vault for production</li>
 * </ul>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication      // Marks this as a Spring Boot application
@EnableConfigServer         // Enables Spring Cloud Config Server functionality
public class ConfigServerApplication {

    /**
     * Main method - Entry point for the Config Server application
     * 
     * <p>SpringApplication.run() performs the following:</p>
     * <ol>
     *   <li>Creates Spring ApplicationContext</li>
     *   <li>Auto-configures Config Server based on @EnableConfigServer</li>
     *   <li>Registers with Eureka Server (if enabled)</li>
     *   <li>Starts embedded Tomcat server on port 8888</li>
     *   <li>Exposes REST endpoints for configuration retrieval</li>
     *   <li>Monitors configuration source (Git/file system) for changes</li>
     * </ol>
     * 
     * @param args Command-line arguments (not used in this application)
     */
    public static void main(String[] args) {
        // Start the Spring Boot application
        SpringApplication.run(ConfigServerApplication.class, args);
        
        // Config Server is now ready to serve configuration to all microservices
        // Services can query: http://localhost:8888/{service-name}/{profile}
    }
}

