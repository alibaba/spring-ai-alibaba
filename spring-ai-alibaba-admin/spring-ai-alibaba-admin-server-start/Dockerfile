# Stage 1: Build the application
# Note: This Dockerfile should be built from the project root directory
# Example: docker build -f spring-ai-alibaba-admin-server-start/Dockerfile -t spring-ai-admin-server .
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy root pom.xml first for better layer caching
COPY pom.xml .

# Copy all module pom.xml files
COPY spring-ai-alibaba-admin-server-core/pom.xml ./spring-ai-alibaba-admin-server-core/
COPY spring-ai-alibaba-admin-server-openapi/pom.xml ./spring-ai-alibaba-admin-server-openapi/
COPY spring-ai-alibaba-admin-server-runtime/pom.xml ./spring-ai-alibaba-admin-server-runtime/
COPY spring-ai-alibaba-admin-server-start/pom.xml ./spring-ai-alibaba-admin-server-start/

# Download dependencies (this layer will be cached if pom files don't change)
RUN mvn dependency:go-offline -B || true

# Copy all source code
COPY spring-ai-alibaba-admin-server-core/src ./spring-ai-alibaba-admin-server-core/src
COPY spring-ai-alibaba-admin-server-openapi/src ./spring-ai-alibaba-admin-server-openapi/src
COPY spring-ai-alibaba-admin-server-runtime/src ./spring-ai-alibaba-admin-server-runtime/src
COPY spring-ai-alibaba-admin-server-start/src ./spring-ai-alibaba-admin-server-start/src

# Build the application (skip tests for faster build, remove -DskipTests if you want to run tests)
RUN mvn clean package -DskipTests -pl spring-ai-alibaba-admin-server-start -am

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install wget for health check
RUN apk add --no-cache wget

# Create a non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the JAR file from builder stage
COPY --from=builder /app/spring-ai-alibaba-admin-server-start/target/spring-ai-alibaba-admin-server-start.jar app.jar

# Change ownership
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose port (adjust if your application uses a different port)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

