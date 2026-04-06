# =============================================================================
#  Talent Performance Pulse – Multi-Stage Dockerfile
#
#  Stage 1 (builder) : compiles and packages the fat JAR via Maven
#  Stage 2 (runtime) : minimal JRE image – no build tools in production
#
#  Build:  docker build -t performance-pulse:latest .
#  Run:    docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev performance-pulse:latest
# =============================================================================

# ── Stage 1: Builder ──────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy dependency descriptor first → layer is cached until pom.xml changes
COPY pom.xml .

# Download all dependencies (cached layer; only re-downloads when pom.xml changes)
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copy source and build – skipping tests here; tests run in CI pipeline
COPY src ./src
RUN mvn package -DskipTests -B --no-transfer-progress

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

# Security: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy only the fat JAR from the builder stage (no Maven, no sources)
COPY --from=builder /build/target/performancepulse-*.jar app.jar

# Fix ownership so the non-root user can read the JAR
RUN chown appuser:appgroup app.jar

USER appuser

# ── JVM tuning ────────────────────────────────────────────────────────────────
# -XX:+UseContainerSupport        : respect cgroup CPU/memory limits (Docker)
# -XX:MaxRAMPercentage=75.0       : use 75% of container memory for heap
# -Djava.security.egd=...urandom  : faster SecureRandom startup
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -Djava.security.egd=file:/dev/./urandom"

# Spring profile injected at runtime via docker-compose or -e flag
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
