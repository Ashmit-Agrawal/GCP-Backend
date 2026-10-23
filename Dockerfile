# ====================================================================
# Temporary Dockerfile for OCI (Oracle Cloud Infrastructure) Instance
# Green Credit Programme (GCP) - Backend Service
# ====================================================================

# Stage 1: Build JAR using Maven with Temurin JDK 17
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

# Copy Maven wrapper and POM first for better layer caching
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

# Make wrapper executable and resolve dependencies
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy application source code
COPY src ./src

# Build production jar
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal, secure JRE runtime container
FROM eclipse-temurin:17-jre-jammy AS runtime

# Install curl for container healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*

# Run as non-root user for security best practices
RUN groupadd -r gcp && useradd -r -g gcp -u 1001 -m gcp
WORKDIR /app

# Copy executable jar from builder stage
COPY --from=builder --chown=gcp:gcp /workspace/target/*.jar /app/app.jar

USER gcp:gcp

# Default service port
EXPOSE 8080

# Environment defaults (override via docker run -e or OCI container instance env vars)
ENV SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/gcpdb" \
  SPRING_DATASOURCE_USERNAME="postgres" \
  SPRING_DATASOURCE_PASSWORD="postgres" \
  SPRING_DATA_MONGODB_URI="mongodb://localhost:27017/gcpdb" \
  CORS_ALLOWED_ORIGINS="http://localhost:5173,http://localhost:3000" \
  JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Container healthcheck using backend's /api/health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
