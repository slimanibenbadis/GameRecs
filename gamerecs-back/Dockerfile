# Build stage
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /workspace/app

# Copy only the files needed for dependency resolution
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Download dependencies with better caching
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -Dmaven.repo.local=/root/.m2/repository

# Copy source code
COPY src src

# Build with parallel execution and optimized memory settings
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -T 1C \
    -Dmaven.repo.local=/root/.m2/repository \
    -Dmaven.test.skip=true && \
    mv target/*-0.0.1-SNAPSHOT.jar target/app.jar

# Runtime stage with minimal image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Add non-root user with minimal permissions
RUN useradd -r -u 1001 -g root gamerecs \
    && mkdir -p /app/logs \
    && chown -R gamerecs:root /app

# Copy only the built jar
COPY --from=build --chown=gamerecs:root /workspace/app/target/app.jar app.jar

# Switch to non-root user
USER 1001

# Configure JVM options for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod

# Add health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# Use array format for better command handling
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"] 
