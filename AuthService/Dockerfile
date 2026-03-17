# Stage 1: Build the JAR
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Copy gradle wrapper and build files first (for layer cachine)
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Download dependencies (cached if build files didn't change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source and build the fat JAR
COPY src src
RUN ./gradlew bootJar --no-daemon

# Stage 2: Run the JAR

# Use Eclipse Temurin 21 JRE on Alpine for a minimal footprint
FROM eclipse-temurin:21-jre

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the host to the container
COPY --from=builder /app/build/libs/AuthService-0.0.1-SNAPSHOT.jar app.jar
# COPY build/libs/AuthService-0.0.1-SNAPSHOT.jar /app/AuthService-0.0.1-SNAPSHOT.jar

# Expose the port that your Java service listens on
EXPOSE 9898

# Set the entry point for the container
ENTRYPOINT ["java", "-jar", "/app/AuthService-0.0.1-SNAPSHOT.jar"]