# ===================================================
# Stage 1: Build the Spring Boot Application
# ===================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application
COPY src ./src
RUN mvn clean package -DskipTests

# ===================================================
# Stage 2: Production Runtime
# ===================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for healthcheck / debugging
RUN apk add --no-cache curl

# Security: Run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring
RUN mkdir -p /app/storage && chown -R spring:spring /app

# Copy artifact from builder
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
