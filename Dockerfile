# Multi-stage build for optimized image
FROM maven:3.9.4-openjdk-17-slim AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jre-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app user
RUN groupadd -r opsguide && useradd -r -g opsguide opsguide

# Set working directory
WORKDIR /app

# Copy the JAR file from build stage
COPY --from=build /app/target/ops-guide-java-*.jar app.jar

# Change ownership
RUN chown -R opsguide:opsguide /app

# Switch to non-root user
USER opsguide

# Expose port
EXPOSE 8093

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8093/v1/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
