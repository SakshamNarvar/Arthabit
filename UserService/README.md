# User Service

Microservice that stores user profiles for the Expense Tracker app.

## Overview
- Consumes Kafka events from Auth Service to **create** user records (idempotent; no updates from events).
- Exposes REST APIs to fetch profiles and update profile-specific fields only.
- `userId`, `email`, `phoneNumber` are owned by Auth Service and are not writable via REST.

## Tech Stack
- Java 21, Spring Boot 3.2, Spring Data JPA, Spring Kafka, MySQL, Lombok, Gradle.

## Key Components
- `consumer/AuthServiceConsumer` → listens to `user_service` topic and calls `UserService.createUserFromEvent`.
- `controller/UserController` → REST endpoints for read/update.
- `service/UserService` → business logic and persistence.

## API
Base URL: `http://localhost:9810`

- `GET /user/v1/users/{userId}` → fetch a user.
- `PUT /user/v1/users/{userId}` → update `firstName`, `lastName`, `profilePic` (nulls ignored; other fields immutable).
- `GET /health` → liveness check.

## Kafka Flow
- Topic: `user_service`, group: `userinfo-consumer-group`.
- Value deserializer: Spring Kafka `JsonDeserializer` (JSON to `UserInfoDto`).
- Event handling: if `userId` exists, event is ignored; otherwise a new user is created.

## Data Ownership
- Auth Service owns: `userId`, `email`, `phoneNumber` (not writable via REST).
- User Service owns: `firstName`, `lastName`, `profilePic` (writable via REST).

## Configuration Highlights (`application.properties`)
- `server.port=9810`
- `spring.datasource.url=jdbc:mysql://localhost:3306/user_service`
- `spring.kafka.bootstrap-servers=localhost:9092`
- `spring.kafka.topic-json.name=user_service`
- `spring.kafka.consumer.group-id=userinfo-consumer-group`

## Run Locally
```bash
cd UserService
./gradlew clean build
./gradlew bootRun
```

Service starts on `http://localhost:9810`.

