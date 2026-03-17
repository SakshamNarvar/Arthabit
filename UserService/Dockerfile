# Stage 1: Build the JAR
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Copy build files first (for layer caching)
COPY build.gradle settings.gradle ./

# Download dependencies (cached if build files didn't change)
RUN gradle dependencies --no-daemon || true

# Copy source and build the fat JAR
COPY src src
RUN gradle bootJar --no-daemon

# Stage 2: Run the JAR

# Use Eclipse Temurin 21 JRE for a minimal footprint
FROM eclipse-temurin:21-jre

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the builder stage
COPY --from=builder /app/build/libs/userservice-0.0.1-SNAPSHOT.jar app.jar

# Expose the port that your Java service listens on
EXPOSE 9810

# Set the entry point for the container
ENTRYPOINT ["java", "-jar", "app.jar"]