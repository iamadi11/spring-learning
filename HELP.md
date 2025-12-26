# E-commerce Microservices Platform

This is a comprehensive microservices-based e-commerce platform built with Spring Boot and Spring Cloud.

## Project Structure

This is a **multi-module Gradle project**. The root project is a parent project that doesn't contain application code. All services are organized as subprojects:

### Infrastructure Services
- **service-discovery** - Eureka Server for service registration and discovery
- **config-server** - Centralized configuration management
- **api-gateway** - Single entry point for all client requests

### Business Services
- **auth-service** - Authentication & Authorization (OAuth2, JWT)
- **user-service** - User management (CQRS pattern)
- **product-service** - Product catalog (Event Sourcing with MongoDB)
- **order-service** - Order management (Saga pattern)
- **payment-service** - Payment processing (Circuit Breaker pattern)
- **notification-service** - Notifications (Multithreading, WebSocket)
- **review-service** - Reviews & Ratings (gRPC communication)

### Shared Libraries
- **common-lib** - Common utilities, DTOs, exceptions
- **event-lib** - Event definitions for Kafka messaging

## Quick Start

### Prerequisites
- Java 21 or higher
- Docker & Docker Compose
- Gradle (included via wrapper)

### Running Locally

1. **Start minimal infrastructure** (recommended for development):
   ```bash
   ./start-minimal.sh
   ```

2. **Start with lightweight monitoring**:
   ```bash
   ./start-local-light.sh
   ```

3. **Start full stack** (requires more resources):
   ```bash
   ./start-local.sh
   ```

### Building All Services

```bash
# Build all services (skipping tests)
./gradlew build -x test

# Build specific service
./gradlew :services:auth-service:build

# Run tests
./gradlew test
```

## Documentation

- [Quick Start Guide](QUICK_START.md) - Get started quickly
- [Architecture Overview](ARCHITECTURE.md) - System design and patterns
- [API Documentation](API_DOCUMENTATION.md) - REST API endpoints
- [Authentication Guide](AUTH_GUIDE.md) - OAuth2 and JWT setup
- [Deployment Guide](DEPLOYMENT.md) - Deploy to various environments
- [Postman Setup](POSTMAN_SETUP_GUIDE.md) - API testing with Postman

## Reference Documentation

### Spring Boot & Spring Cloud
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/3.2.0/reference/)
- [Spring Cloud Documentation](https://docs.spring.io/spring-cloud/docs/2023.0.0/reference/)
- [Spring Security OAuth2](https://spring.io/projects/spring-security-oauth)

### Microservices Patterns
- [Microservices Patterns](https://microservices.io/patterns/index.html)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)
- [Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker)
- [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)

### Tools & Technologies
- [Gradle Multi-Project Builds](https://docs.gradle.org/current/userguide/multi_project_builds.html)
- [Docker Compose](https://docs.docker.com/compose/)
- [Kubernetes](https://kubernetes.io/docs/home/)
- [Apache Kafka](https://kafka.apache.org/documentation/)

## Need Help?

- Check the [START_HERE.md](START_HERE.md) for a complete getting started guide
- Review [LEARNING_PATH.md](LEARNING_PATH.md) for a structured learning approach
- See [PROJECT_STATUS.md](PROJECT_STATUS.md) for current implementation status

## Support

For issues or questions, please refer to the documentation files in the project root.
