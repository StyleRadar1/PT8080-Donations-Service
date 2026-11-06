# ==============================================================================
# Stage 1: Build stage with Maven and Eclipse Temurin 21 Alpine
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Pre-cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy project source tree and package application artifact
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# Stage 2: Minimal Production Runtime with Eclipse Temurin 21 JRE Alpine
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine

# Non-root unprivileged system user and group (UID/GID 10001)
RUN addgroup -g 10001 -S appgroup && \
    adduser -u 10001 -S appuser -G appgroup

WORKDIR /app

# Copy executable jar from builder stage with proper ownership
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

# Switch to non-privileged user
USER appuser:appgroup

# Official microservice assigned port
EXPOSE 8520

# Microservice entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]
