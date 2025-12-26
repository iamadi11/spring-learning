# Multi-stage Dockerfile for Auth Service
# Stage 1: Build stage using Gradle
FROM gradle:8.5-jdk21-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files first (for caching)
COPY gradle gradle
COPY gradlew .
COPY settings.gradle .
COPY build.gradle .

# Copy shared libraries (dependencies)
COPY shared shared

# Copy auth-service source
COPY services/auth-service services/auth-service

# Build the application (skip tests - they run in CI)
RUN gradle :services:auth-service:build -x test --no-daemon

# Stage 2: Runtime stage with minimal JRE
FROM eclipse-temurin:21-jre-alpine

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/services/auth-service/build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8086

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8086/actuator/health || exit 1

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

