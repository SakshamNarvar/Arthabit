# ExpenseService

> **Microservice of:** [Expense Tracker App](../README.md)

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [API Endpoints](#api-endpoints)
- [Authentication](#authentication)
- [Error Handling](#error-handling)
- [Kafka Integration](#kafka-integration)
- [DTOs (Data Transfer Objects)](#dtos-data-transfer-objects)
- [Configuration](#configuration)
- [Environment Variables](#environment-variables)
- [Docker](#docker)
- [Build & Run](#build--run)
- [Future / Work-in-Progress](#future--work-in-progress)

---

## Overview

The **ExpenseService** is a microservice responsible for managing user expenses within the Expense Tracker App. It provides REST APIs to create and retrieve expenses, authenticates requests via the **AuthService** (JWT-based), and includes a Kafka consumer for event-driven expense creation from other services.

---

## Tech Stack

| Technology            | Version / Details          |
|-----------------------|----------------------------|
| **Java**              | 21                         |
| **Spring Boot**       | 3.5.10                     |
| **Spring Data JPA**   | (via starter)              |
| **Spring Kafka**      | (via starter)              |
| **Spring Validation** | (via starter)              |
| **MySQL**             | 8.x (MySQL8Dialect)        |
| **Lombok**            | 1.18.30                    |
| **Gradle**            | Wrapper included           |
| **Docker**            | eclipse-temurin:21-jre     |

---

## Architecture

```
┌──────────────────┐       REST API        ┌──────────────────────┐
│   API Gateway /  │ ───────────────────►  │   ExpenseController  │
│   Other Service  │ (Authorization header)│   /expense/v1        │
└──────────────────┘                       └──────────┬───────────┘
                                                      │
                                                      ▼
                                           ┌──────────────────────┐
                                           │     AuthClient       │
                                           │  (calls AuthService  │
                                           │   /auth/v1/ping)     │
                                           └──────────┬───────────┘
                                                      │ returns userId
                                                      ▼
┌──────────────────┐                       ┌──────────────────────┐
│   Apache Kafka   │ ─── (consumer) ────►  │   ExpenseConsumer    │
│  expense_service │                       │                      │
└──────────────────┘                       └──────────┬───────────┘
                                                      │
                                                      ▼
                                           ┌──────────────────────┐
                                           │   ExpenseService     │
                                           │   (Business Logic)   │
                                           └──────────┬───────────┘
                                                      │
                                                      ▼
                                           ┌──────────────────────┐
                                           │   ExpenseRepository  │
                                           │   (Spring Data JPA)  │
                                           └──────────┬───────────┘
                                                      │
                                                      ▼
                                           ┌──────────────────────┐
                                           │     MySQL Database   │
                                           │   (expense_service)  │
                                           └──────────────────────┘
```

---

## Project Structure

```
src/main/java/com/nstrange/expenseservice/
├── ExpenseServiceApplication.java        # Spring Boot entry point
├── client/
│   └── AuthClient.java                   # REST client – calls AuthService to validate JWTs
├── config/
│   └── AppConfig.java                    # Configuration beans (RestTemplate)
├── consumer/
│   ├── ExpenseConsumer.java              # Kafka consumer – listens on expense_service topic
│   └── ExpenseDeserializer.java          # Custom Kafka deserializer for ExpenseDto
├── controller/
│   └── ExpenseController.java            # REST controller – /expense/v1
├── dto/
│   ├── ApiErrorResponse.java            # Standardized error response body
│   ├── CreateExpenseRequestDto.java      # Request body for creating an expense (validated)
│   ├── ExpenseDto.java                   # DTO used for Kafka event deserialization
│   └── ExpenseResponseDto.java           # Response body returned by API endpoints
├── entities/
│   └── Expense.java                      # JPA entity mapped to `expense` table
├── exception/
│   ├── ExpenseServiceException.java      # Generic internal service exception
│   ├── GlobalExceptionHandler.java       # @RestControllerAdvice – centralized error handling
│   ├── InvalidExpenseRequestException.java # Bad-request / validation exception
│   └── UnauthorizedException.java        # Authentication failure exception
├── repository/
│   └── ExpenseRepository.java            # Spring Data CrudRepository
└── service/
    └── ExpenseService.java               # Business logic layer
```

---

## Database Schema

**Table:** `expense` (auto-managed by Hibernate `ddl-auto=update`)

| Column        | Type           | Constraints                      | Description                                   |
|---------------|----------------|----------------------------------|-----------------------------------------------|
| `id`          | `BIGINT`       | `PRIMARY KEY`, `AUTO_INCREMENT`  | Internal database ID                          |
| `external_id` | `VARCHAR(255)` | Auto-generated UUID              | Public-facing unique identifier               |
| `user_id`     | `VARCHAR(255)` |                                  | ID of the user who owns the expense           |
| `amount`      | `DECIMAL`      |                                  | Expense amount                                |
| `merchant`    | `VARCHAR(255)` |                                  | Merchant / vendor name                        |
| `currency`    | `VARCHAR(255)` | Defaults to `"inr"` if null     | Currency code (e.g., `inr`, `usd`)            |
| `created_at`  | `TIMESTAMP`    | Defaults to current time if null | Timestamp when the expense was created        |

> **Note:** `external_id` is auto-generated via `@PrePersist` / `@PreUpdate` using `UUID.randomUUID()`.

---

## API Endpoints

Base path: `/expense/v1`
Default port: `9820`

### 1. Get Expenses

Retrieve all expenses for the authenticated user.

```
GET /expense/v1/getExpense
```

**Headers:**

| Header          | Type     | Required | Description                     |
|-----------------|----------|----------|---------------------------------|
| `Authorization` | `String` | ✅ Yes   | `Bearer <JWT>` – forwarded to AuthService for validation |

**Response:** `200 OK`

```json
[
  {
    "external_id": "a1b2c3d4-...",
    "amount": 150.00,
    "user_id": "user-123",
    "merchant": "Amazon",
    "currency": "inr",
    "created_at": "2026-03-09T12:00:00.000+00:00"
  }
]
```

**Error Responses:**

| Status | Condition                     |
|--------|-------------------------------|
| `401`  | Missing, malformed, or invalid JWT |
| `500`  | Internal / database error     |

---

### 2. Add Expense

Create a new expense for the authenticated user.

```
POST /expense/v1/addExpense
```

**Headers:**

| Header          | Type     | Required | Description                     |
|-----------------|----------|----------|---------------------------------|
| `Authorization` | `String` | ✅ Yes   | `Bearer <JWT>` – forwarded to AuthService for validation |

**Request Body:** (`application/json`)

```json
{
  "amount": 150.00,
  "merchant": "Amazon",
  "currency": "usd"
}
```

| Field      | Type        | Required | Default | Validation                   | Description              |
|------------|-------------|----------|---------|------------------------------|--------------------------|
| `amount`   | `BigDecimal`| ✅ Yes   | —       | `@NotNull`, `@Positive`     | Expense amount           |
| `merchant` | `String`    | ✅ Yes   | —       | `@NotBlank`                  | Merchant / vendor name   |
| `currency` | `String`    | ❌ No    | `"inr"` | —                            | Currency code            |

**Response:** `201 Created`

```json
{
  "external_id": "a1b2c3d4-...",
  "amount": 150.00,
  "user_id": "user-123",
  "merchant": "Amazon",
  "currency": "usd",
  "created_at": "2026-03-09T12:00:00.000+00:00"
}
```

**Error Responses:**

| Status | Condition                                      |
|--------|------------------------------------------------|
| `400`  | Validation failed (missing/invalid fields)     |
| `401`  | Missing, malformed, or invalid JWT             |
| `500`  | Internal / database error                      |

---

## Authentication

All REST endpoints require a valid JWT in the `Authorization` header (`Bearer <token>`).

The `AuthClient` component forwards the `Authorization` header to the **AuthService** at:

```
GET {auth-service.base-url}/auth/v1/ping
```

- On success, AuthService returns the **trusted userId** which is used for all downstream operations.
- On failure (401, other HTTP errors, or connectivity issues), an `UnauthorizedException` is thrown and the request is rejected with `401 Unauthorized`.

**Configuration:**

| Property                  | Default Value                                        |
|---------------------------|------------------------------------------------------|
| `auth-service.base-url`  | `http://{AUTH_SERVICE_HOST}:{AUTH_SERVICE_PORT}`     |

> **Note:** The `RestTemplate` bean used by `AuthClient` is configured in `AppConfig`.

---

## Error Handling

The service uses a centralized `@RestControllerAdvice` (`GlobalExceptionHandler`) that produces consistent `ApiErrorResponse` JSON for all error scenarios.

### Error Response Format

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/expense/v1/addExpense",
  "timestamp": "2026-03-11T10:00:00Z",
  "validation_errors": [
    {
      "field": "amount",
      "message": "Amount is required",
      "rejected_value": null
    }
  ]
}
```

> `validation_errors` is only present for bean-validation failures; `timestamp` defaults to `Instant.now()`.

### Handled Exception Types

| Exception                              | HTTP Status | Description                                     |
|----------------------------------------|-------------|-------------------------------------------------|
| `UnauthorizedException`                | `401`       | Missing / invalid / expired JWT                 |
| `InvalidExpenseRequestException`       | `400`       | Invalid request data (null body, blank userId)  |
| `MethodArgumentNotValidException`      | `400`       | Bean-validation errors (with field details)     |
| `HandlerMethodValidationException`     | `400`       | Handler-level validation failure                |
| `MissingRequestHeaderException`        | `400`       | Required header missing (e.g., Authorization)   |
| `HttpMessageNotReadableException`      | `400`       | Malformed JSON request body                     |
| `HttpRequestMethodNotSupportedException`| `405`      | Unsupported HTTP method                         |
| `NoResourceFoundException`             | `404`       | Requested resource not found                    |
| `ExpenseServiceException`              | `500`       | Internal service / persistence error            |
| `DataAccessException`                  | `500`       | Database connectivity / query error             |
| `Exception` (catch-all)               | `500`       | Any other unexpected error                      |

---

## Kafka Integration

The service consumes messages from Apache Kafka for event-driven expense creation.

| Property             | Value                                     |
|----------------------|-------------------------------------------|
| **Topic**            | `expense_service`                         |
| **Consumer Group**   | `expense-info-consumer-group`             |
| **Key Deserializer** | `StringDeserializer`                      |
| **Value Deserializer** | `ExpenseDeserializer` (custom)          |

### Custom Deserializer

`ExpenseDeserializer` implements `org.apache.kafka.common.serialization.Deserializer<ExpenseDto>` and uses Jackson `ObjectMapper` to deserialize incoming byte arrays into `ExpenseDto` objects.

### Consumer

`ExpenseConsumer` is annotated with `@Service` and listens on the `expense_service` topic via `@KafkaListener`. When a message arrives it:

1. Deserializes the event into `ExpenseDto`.
2. Calls `ExpenseService.createExpense(ExpenseDto)` to persist the expense.
3. Logs success/failure for each event.

> **TODO:** Add transactional support and handle idempotency (duplicate event detection).

---

## DTOs (Data Transfer Objects)

### `CreateExpenseRequestDto`
Used as the **request body** for the `POST /expense/v1/addExpense` endpoint. Includes Bean Validation annotations.

| Field      | Type          | JSON Naming   | Validation                |
|------------|---------------|---------------|---------------------------|
| `amount`   | `BigDecimal`  | `amount`      | `@NotNull`, `@Positive`  |
| `merchant` | `String`      | `merchant`    | `@NotBlank`               |
| `currency` | `String`      | `currency`    | —                         |

### `ExpenseDto`
Used for **Kafka event deserialization**. Contains the full expense data including user and timestamp info.

| Field        | Type          | JSON Property  |
|--------------|---------------|----------------|
| `externalId` | `String`      | `external_id`  |
| `amount`     | `BigDecimal`  | `amount`       |
| `userId`     | `String`      | `user_id`      |
| `merchant`   | `String`      | `merchant`     |
| `currency`   | `String`      | `currency`     |
| `createdAt`  | `Timestamp`   | `created_at`   |

### `ExpenseResponseDto`
Used as the **response body** returned by all API endpoints.

| Field        | Type          | JSON Naming    |
|--------------|---------------|----------------|
| `externalId` | `String`      | `external_id`  |
| `amount`     | `BigDecimal`  | `amount`       |
| `userId`     | `String`      | `user_id`      |
| `merchant`   | `String`      | `merchant`     |
| `currency`   | `String`      | `currency`     |
| `createdAt`  | `Timestamp`   | `created_at`   |

### `ApiErrorResponse`
Used as the **error response body** returned by `GlobalExceptionHandler` for all error scenarios.

| Field              | Type                          | JSON Naming          | Description                          |
|--------------------|-------------------------------|----------------------|--------------------------------------|
| `status`           | `int`                         | `status`             | HTTP status code                     |
| `error`            | `String`                      | `error`              | HTTP status reason phrase            |
| `message`          | `String`                      | `message`            | Human-readable error message         |
| `path`             | `String`                      | `path`               | Request URI                          |
| `timestamp`        | `Instant`                     | `timestamp`          | Time of the error (defaults to now)  |
| `validationErrors` | `List<FieldValidationError>`  | `validation_errors`  | Field-level validation details (nullable) |

**`FieldValidationError`** (nested):

| Field           | Type     | JSON Naming       |
|-----------------|----------|--------------------|
| `field`         | `String` | `field`            |
| `message`       | `String` | `message`          |
| `rejectedValue` | `Object` | `rejected_value`   |

---

## Configuration

**`application.properties`**

| Property                                                   | Value / Description                                             |
|------------------------------------------------------------|-----------------------------------------------------------------|
| `server.port`                                              | `9820`                                                          |
| `spring.application.name`                                  | `service`                                                       |
| `spring.datasource.url`                                    | `jdbc:mysql://{MYSQL_HOST}:{MYSQL_PORT}/{MYSQL_DB}`            |
| `spring.datasource.driver-class-name`                      | `com.mysql.cj.jdbc.Driver`                                     |
| `spring.jpa.hibernate.ddl-auto`                            | `update` (auto-creates/updates schema)                         |
| `spring.jpa.properties.hibernate.dialect`                  | `org.hibernate.dialect.MySQL8Dialect`                          |
| `spring.jpa.show-sql`                                      | `true`                                                          |
| `spring.kafka.bootstrap-servers`                           | `{KAFKA_HOST}:{KAFKA_PORT}`                                    |
| `spring.kafka.consumer.group-id`                           | `expense-info-consumer-group`                                  |
| `spring.kafka.topic-json.name`                             | `expense_service`                                               |
| `spring.kafka.consumer.properties.spring.json.type.mapping`| `com.nstrange.expenseservice.dto.ExpenseDto`                   |
| `auth-service.base-url`                                    | `http://{AUTH_SERVICE_HOST}:{AUTH_SERVICE_PORT}`                |
| `logging.level.org.springframework.web`                    | `DEBUG`                                                         |
| `logging.level.org.springframework.web.servlet.mvc.method.annotation` | `TRACE`                                          |
| `server.error.include-message`                             | `always`                                                        |
| `server.error.include-binding-errors`                      | `always`                                                        |

---

## Environment Variables

| Variable             | Default          | Description                        |
|----------------------|------------------|------------------------------------|
| `KAFKA_HOST`         | `localhost`      | Kafka broker hostname              |
| `KAFKA_PORT`         | `9092`           | Kafka broker port                  |
| `MYSQL_HOST`         | `localhost`      | MySQL server hostname              |
| `MYSQL_PORT`         | `3306`           | MySQL server port                  |
| `MYSQL_DB`           | `expense_service`| MySQL database name                |
| `AUTH_SERVICE_HOST`  | `auth-service`   | AuthService hostname               |
| `AUTH_SERVICE_PORT`  | `9898`           | AuthService port                   |

> **Note:** MySQL credentials are currently hardcoded (`root` / `narvar007`). Consider externalizing these via environment variables for production deployments.

---

## Docker

The service includes a `Dockerfile` for containerized deployment.

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/ExpenseService-0.0.1-SNAPSHOT.jar /app/ExpenseService-0.0.1-SNAPSHOT.jar
EXPOSE 9820
ENTRYPOINT ["java", "-jar", "/app/ExpenseService-0.0.1-SNAPSHOT.jar"]
```

### Build & Run with Docker

```bash
# Build the JAR first
./gradlew bootJar

# Build Docker image
docker build -t expense-service .

# Run the container
docker run -p 9820:9820 \
  -e KAFKA_HOST=kafka \
  -e KAFKA_PORT=9092 \
  -e MYSQL_HOST=mysql \
  -e MYSQL_PORT=3306 \
  -e MYSQL_DB=expense_service \
  -e AUTH_SERVICE_HOST=auth-service \
  -e AUTH_SERVICE_PORT=9898 \
  expense-service
```

---

## Build & Run

### Prerequisites

- **Java 21**
- **MySQL 8.x** running and accessible
- **Apache Kafka** running
- **AuthService** running and reachable (for JWT validation)

### Local Development

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

The service will start on **http://localhost:9820**.

### Run Tests

```bash
./gradlew test
```

---

## Future / Work-in-Progress

- [ ] **Idempotency Handling** – Detect and skip duplicate Kafka events to prevent duplicate expense records.
- [ ] **Transactional Support** – Wrap Kafka consumer processing in a transaction.
- [ ] **Update Expense API** – `updateExpense` logic exists (commented out) in `ExpenseService`; expose it via a `PUT`/`PATCH` endpoint.
- [ ] **Date-range Query** – `findByUserIdAndCreatedAtBetween` repository method exists (commented out) for filtering expenses by date range.
- [ ] **Externalize Credentials** – Move database username/password to environment variables or a secrets manager.

---

*This document is auto-generated from the ExpenseService source code and configuration files.*
