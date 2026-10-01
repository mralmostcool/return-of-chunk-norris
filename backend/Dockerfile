# syntax=docker/dockerfile:1

# -------------------------------------------------------------
# Stage 1: Build stage
# -------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy build descriptor and wrapper
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Pre-fetch dependencies where possible to leverage Docker layer caching
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copy source and compile application
COPY src/ src/
RUN ./mvnw clean package -DskipTests

# Extract layers using Spring Boot tools
RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination target/extracted

# -------------------------------------------------------------
# Stage 2: Minimal runtime image
# -------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Install curl for reliable healthchecks
RUN apk add --no-cache curl tzdata

# Run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

# Copy extracted layers in order of least to most frequently changing
COPY --from=builder /workspace/target/extracted/dependencies/ ./
COPY --from=builder /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/target/extracted/application/ ./

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
