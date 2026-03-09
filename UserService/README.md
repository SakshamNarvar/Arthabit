# 👤 User Service

> Microservice responsible for managing user profiles in the **Expense Tracker** application.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Data Model](#data-model)
- [API Endpoints](#api-endpoints)
- [Kafka Integration](#kafka-integration)
- [Configuration](#configuration)
- [Environment Variables](#environment-variables)
- [Running Locally](#running-locally)
- [Docker](#docker)
- [Swagger / OpenAPI](#swagger--openapi)
- [Inter-Service Communication](#inter-service-communication)

---

## Overview

The **User Service** is a Spring Boot microservice that owns and manages user profile data. It serves two primary functions:

1. **Consumes Kafka events** from the Auth Service to automatically create or update user records when a user registers or updates their credentials.
2. **Exposes REST APIs** for other services (or frontends) to fetch user profiles and update profile-specific fields (`firstName`, `lastName`, `profilePic`).

Fields like `userId`, `email`, and `phoneNumber` are **owned by the Auth Service** and are only written via Kafka event consumption — they cannot be modified through the User Service REST API.

---

## Architecture

```
┌──────────────┐    Kafka (user_service topic)    ┌──────────────────┐
│ Auth Service  │ ──────────────────────────────► │   User Service    │
└──────────────┘                                   │                  │
                                                   │  ┌────────────┐ │
                        REST API                   │  │   MySQL    │ │
  Clients / Other  ◄──────────────────────────────►│  │  (users)   │ │
    Services                                       │  └────────────┘ │
                                                   └──────────────────┘
```

- **Inbound (Kafka):** Listens on the `user_service` topic for user registration/update events from Auth Service.
- **Inbound/Outbound (REST):** Provides HTTP endpoints to query and update user profiles.
- **Database:** Persists user data in a MySQL `users` table.

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 3.2.2 | Application framework |
| Spring Data JPA | (managed) | ORM / Data access |
| Spring Kafka | (managed) | Kafka consumer |
| MySQL | — | Persistent storage |
| Lombok | 1.18.30 | Boilerplate reduction |
| Jackson | (managed) | JSON serialization/deserialization |
| SpringDoc OpenAPI | 2.3.0 | Swagger UI & API docs |
| Gradle | — | Build tool |
| Docker | Eclipse Temurin 21 JRE | Containerization |

---

## Project Structure

```
src/main/java/com/nstrange/userservice/
├── UserServiceApplication.java          # Spring Boot entry point
├── config/
│   ├── OpenApiConfig.java               # Swagger/OpenAPI configuration
│   └── UserServiceConfig.java           # Bean definitions (ObjectMapper)
├── consumer/
│   ├── AuthServiceConsumer.java          # Kafka consumer – listens for Auth Service events
│   └── UserController.java              # REST controller – user profile endpoints
├── deserializer/
│   └── UserInfoDeserializer.java        # Custom Kafka deserializer for UserInfoDto
├── entities/
│   ├── UserInfo.java                    # JPA entity (maps to `users` table)
│   ├── UserInfoDto.java                 # DTO for full user data (API & Kafka)
│   └── UserProfileUpdateDto.java        # DTO for profile update requests
├── repository/
│   └── UserRepository.java             # Spring Data repository interface
└── service/
    └── UserService.java                 # Business logic layer
```

---

## Data Model

### `users` Table (MySQL)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` (auto-increment) | PK | Internal surrogate key |
| `user_id` | `VARCHAR` | UNIQUE, NOT NULL | Unique user identifier (from Auth Service) |
| `first_name` | `VARCHAR` | NOT NULL | User's first name |
| `last_name` | `VARCHAR` | NOT NULL | User's last name |
| `phone_number` | `BIGINT` | NOT NULL | User's phone number |
| `email` | `VARCHAR` | NOT NULL | User's email address |
| `profile_pic` | `VARCHAR` | nullable | URL of the user's profile picture |

### Entity Relationship

```
UserInfo (JPA Entity)
  ├── id            : Long       (PK, auto-generated)
  ├── userId        : String     (unique, from Auth Service)
  ├── firstName     : String
  ├── lastName      : String
  ├── phoneNumber   : Long
  ├── email         : String
  └── profilePic    : String     (nullable)
```

### DTOs

| DTO | Purpose |
|---|---|
| `UserInfoDto` | Full user representation – used in API responses and Kafka events |
| `UserProfileUpdateDto` | Partial update payload – only `firstName`, `lastName`, `profilePic` |

---

## API Endpoints

Base URL: `http://localhost:9810`

### Get User

```
GET /user/v1/getUser?userId={userId}
```

| Parameter | Type | Required | Description |
|---|---|---|---|
| `userId` | `String` (query) | ✅ | Unique user identifier |

**Responses:**

| Status | Description | Body |
|---|---|---|
| `200 OK` | User found | `UserInfoDto` JSON |
| `404 Not Found` | User does not exist | — |

**Example Response (200):**
```json
{
  "user_id": "user123",
  "first_name": "John",
  "last_name": "Doe",
  "phone_number": 9876543210,
  "email": "john.doe@example.com",
  "profile_pic": "https://example.com/pic.jpg"
}
```

---

### Update User Profile

```
PUT /user/v1/updateProfile?userId={userId}
```

| Parameter | Type | Required | Description |
|---|---|---|---|
| `userId` | `String` (query) | ✅ | Unique user identifier |

**Request Body (`UserProfileUpdateDto`):**

```json
{
  "first_name": "Jane",
  "last_name": "Smith",
  "profile_pic": "https://example.com/new-pic.jpg"
}
```

> **Note:** `null` fields are ignored — only non-null fields are updated. Fields like `email`, `phoneNumber`, and `userId` are owned by the Auth Service and **cannot** be changed through this endpoint.

**Responses:**

| Status | Description | Body |
|---|---|---|
| `200 OK` | Profile updated successfully | `UserInfoDto` JSON |
| `404 Not Found` | User does not exist | — |

---

### Health Check

```
GET /health
```

**Responses:**

| Status | Description | Body |
|---|---|---|
| `200 OK` | Service is healthy | `true` |

---

## Kafka Integration

### Consumer Configuration

| Property | Value |
|---|---|
| **Topic** | `user_service` |
| **Consumer Group** | `userinfo-consumer-group` |
| **Key Deserializer** | `StringDeserializer` |
| **Value Deserializer** | `UserInfoDeserializer` (custom) |
| **Auto Offset Reset** | `earliest` |
| **Session Timeout** | 45,000 ms |
| **Max Poll Interval** | 300,000 ms |

### Event Flow

1. **Auth Service** publishes a `UserInfoDto` JSON message to the `user_service` Kafka topic when a user registers or updates credentials.
2. **`AuthServiceConsumer`** receives the event and delegates to `UserService.createOrUpdateUser()`.
3. `createOrUpdateUser()` performs an **upsert** — if a user with the given `userId` exists, it updates the record; otherwise, it creates a new one.

### Custom Deserializer

`UserInfoDeserializer` implements `org.apache.kafka.common.serialization.Deserializer<UserInfoDto>` and uses Jackson's `ObjectMapper` to deserialize the raw bytes into a `UserInfoDto` object.

---

## Configuration

Key configuration in `application.properties`:

| Property | Default Value | Description |
|---|---|---|
| `server.port` | `9810` | HTTP server port |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/user_service` | MySQL JDBC URL |
| `spring.jpa.properties.hibernate.hbm2ddl.auto` | `update` | Schema auto-update strategy |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker address |
| `spring.kafka.topic-json.name` | `user_service` | Kafka topic to consume |
| `spring.kafka.consumer.group-id` | `userinfo-consumer-group` | Kafka consumer group |
| `spring.datasource.hikari.maximum-pool-size` | `20` | Max DB connection pool size |
| `spring.datasource.hikari.minimum-idle` | `10` | Min idle DB connections |
| `springdoc.swagger-ui.path` | `/swagger-ui.html` | Swagger UI URL path |
| `springdoc.api-docs.path` | `/api-docs` | OpenAPI JSON docs path |

---

## Environment Variables

All infrastructure-related values are configurable via environment variables:

| Variable | Default | Description |
|---|---|---|
| `KAFKA_HOST` | `localhost` | Kafka broker hostname |
| `KAFKA_PORT` | `9092` | Kafka broker port |
| `MYSQL_HOST` | `localhost` | MySQL server hostname |
| `MYSQL_PORT` | `3306` | MySQL server port |
| `MYSQL_DB` | `user_service` | MySQL database name |

---

## Running Locally

### Prerequisites

- **Java 21** (JDK)
- **MySQL** running on `localhost:3306` with a database named `user_service`
- **Apache Kafka** running on `localhost:9092`

### Steps

```bash
# 1. Clone the repository and navigate to the UserService directory
cd UserService

# 2. Build the project
./gradlew clean build

# 3. Run the application
./gradlew bootRun
```

The service will start on **http://localhost:9810**.

---

## Docker

### Build the Docker Image

```bash
# From the UserService directory, first build the JAR
./gradlew clean build

# Build the Docker image
docker build -t userservice:latest .
```

### Run the Container

```bash
docker run -d \
  --name userservice \
  -p 9810:9810 \
  -e KAFKA_HOST=kafka \
  -e KAFKA_PORT=9092 \
  -e MYSQL_HOST=mysql \
  -e MYSQL_PORT=3306 \
  -e MYSQL_DB=user_service \
  userservice:latest
```

### Dockerfile Summary

- **Base image:** `eclipse-temurin:21-jre`
- **Working directory:** `/app`
- **Exposed port:** `9810`
- **Entrypoint:** `java -jar /app/userservice-0.0.1-SNAPSHOT.jar`

---

## Swagger / OpenAPI

Once the service is running, interactive API documentation is available at:

| Resource | URL |
|---|---|
| **Swagger UI** | [http://localhost:9810/swagger-ui.html](http://localhost:9810/swagger-ui.html) |
| **OpenAPI JSON** | [http://localhost:9810/api-docs](http://localhost:9810/api-docs) |

---

## Inter-Service Communication

| Direction | Service | Method | Details |
|---|---|---|---|
| **Inbound** | Auth Service → User Service | Kafka | Consumes `UserInfoDto` events from the `user_service` topic to create/update user records |
| **Inbound** | Any Client → User Service | REST | `GET /user/v1/getUser`, `PUT /user/v1/updateProfile`, `GET /health` |

### Data Ownership Boundaries

| Field | Owner | Writable via REST? |
|---|---|---|
| `userId` | Auth Service | ❌ |
| `email` | Auth Service | ❌ |
| `phoneNumber` | Auth Service | ❌ |
| `firstName` | User Service | ✅ |
| `lastName` | User Service | ✅ |
| `profilePic` | User Service | ✅ |

---

> **Part of the Expense Tracker App** — This documentation covers only the User Service microservice. See the parent project README for the full system architecture.

