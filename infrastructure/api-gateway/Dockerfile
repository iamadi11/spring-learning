# Multi-stage Dockerfile for API Gateway
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY gradle gradle
COPY gradlew .
COPY settings.gradle .
COPY build.gradle .
COPY infrastructure/api-gateway infrastructure/api-gateway
RUN gradle :infrastructure:api-gateway:build -x test --no-daemon

FROM eclipse-temurin:21-jre
RUN groupadd -r spring && useradd -r -g spring spring
WORKDIR /app
COPY --from=builder /app/infrastructure/api-gateway/build/libs/*.jar app.jar
RUN chown -R spring:spring /app
USER spring:spring
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

