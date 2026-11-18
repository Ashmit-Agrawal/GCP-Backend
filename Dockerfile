# ==============================================================================
# OCI & Rootless Podman Compliant Multi-Stage Dockerfile
# Green Credit Programme (GCP) — Spring Boot 3.3.5 Backend Service
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Stage (Explicit Fully-Qualified OCI Registry Name)
# ------------------------------------------------------------------------------
FROM docker.io/library/eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

# Copy Maven wrapper and POM metadata first to maximize OCI layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ensure wrapper script has Unix LF line endings and executable bit, then pre-fetch dependencies
RUN sed -i 's/\r$//' ./mvnw \
    && chmod +x ./mvnw \
    && ./mvnw dependency:go-offline -B

# Copy application source tree and build fat JAR
COPY src/ ./src/
RUN ./mvnw clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Minimal Unprivileged JRE Runtime Stage
# ------------------------------------------------------------------------------
FROM docker.io/library/eclipse-temurin:17-jre-jammy AS runtime

LABEL org.opencontainers.image.title="gcp-backend" \
      org.opencontainers.image.description="Green Credit Programme (GCP) National Portal Backend API" \
      org.opencontainers.image.vendor="MoEFCC GCP Pilot Engineering" \
      org.opencontainers.image.version="1.0.0"

# Install curl for container HEALTHCHECK probes and clean apt cache
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Create unprivileged system group and user with explicit UID/GID 10001 for rootless Podman
RUN groupadd -g 10001 -r gcp \
    && useradd -u 10001 -g 10001 -r -s /sbin/nologin -d /app gcp \
    && mkdir -p /app \
    && chown -R 10001:10001 /app

WORKDIR /app

# Copy compiled Spring Boot artifact with non-root ownership
COPY --from=build --chown=10001:10001 /workspace/target/*.jar /app/app.jar

# Drop root privileges permanently
USER 10001:10001

# Unprivileged application port (>1024 required for rootless Podman netns)
ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=dev

EXPOSE 8080 8081

# OCI / Docker healthcheck probing the Spring Boot liveness endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fsS "http://localhost:${SERVER_PORT:-8080}/api/health" || exit 1

# Container-aware JVM entrypoint
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
