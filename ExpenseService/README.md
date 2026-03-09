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
- [Kafka Integration](#kafka-integration)
- [DTOs (Data Transfer Objects)](#dtos-data-transfer-objects)
- [Configuration](#configuration)
- [Environment Variables](#environment-variables)
- [Docker](#docker)
- [Build & Run](#build--run)
- [Future / Work-in-Progress](#future--work-in-progress)

---

## Overview

The **ExpenseService** is a microservice responsible for managing user expenses within the Expense Tracker App. It provides REST APIs to create and retrieve expenses, and includes a Kafka consumer setup for event-driven expense creation from other services.

---

## Tech Stack

| Technology            | Version / Details          |
|-----------------------|----------------------------|
| **Java**              | 21                         |
| **Spring Boot**       | 3.5.10                     |
| **Spring Data JPA**   | (via starter)              |
| **Spring Kafka**      | (via starter)              |
| **MySQL**             | 8.x (MySQL8Dialect)        |
| **Lombok**            | 1.18.30                    |
| **Gradle**            | Wrapper included           |
| **Docker**            | eclipse-temurin:21-jre     |

---

## Architecture

```
┌──────────────────┐       REST API        ┌──────────────────────┐
│   API Gateway /  │ ───────────────────►  │   ExpenseController  │
│   Other Service  │   (X-User-Id header)  │   /expense/v1        │
└──────────────────┘                       └──────────┬───────────┘
                                                      │
                                                      ▼
┌──────────────────┐                       ┌──────────────────────┐
│   Apache Kafka   │ ─── (consumer) ────►  │   ExpenseConsumer    │
│  expense_service │   (currently WIP)     │                      │
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
├── consumer/
│   ├── ExpenseConsumer.java              # Kafka consumer (WIP – listener commented out)
│   └── ExpenseDeserializer.java          # Custom Kafka deserializer for ExpenseDto
├── controller/
│   └── ExpenseController.java            # REST controller – /expense/v1
├── dto/
│   ├── CreateExpenseRequestDto.java      # Request body for creating an expense
│   ├── ExpenseDto.java                   # DTO used for Kafka event deserialization
│   └── ExpenseResponseDto.java           # Response body returned by API endpoints
├── entities/
│   └── Expense.java                      # JPA entity mapped to `expense` table
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

| Header       | Type     | Required | Description       |
|--------------|----------|----------|-------------------|
| `X-User-Id`  | `String` | ✅ Yes   | Authenticated user ID |

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

---

### 2. Add Expense

Create a new expense for the authenticated user.

```
POST /expense/v1/addExpense
```

**Headers:**

| Header       | Type     | Required | Description       |
|--------------|----------|----------|-------------------|
| `X-User-Id`  | `String` | ✅ Yes   | Authenticated user ID |

**Request Body:** (`application/json`)

```json
{
  "amount": 150.00,
  "merchant": "Amazon",
  "currency": "usd"
}
```

| Field      | Type        | Required | Default | Description              |
|------------|-------------|----------|---------|--------------------------|
| `amount`   | `BigDecimal`| ✅ Yes   | —       | Expense amount           |
| `merchant` | `String`    | ❌ No    | `null`  | Merchant / vendor name   |
| `currency` | `String`    | ❌ No    | `"inr"` | Currency code            |

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

---

## Kafka Integration

The service is configured to consume messages from Apache Kafka, though the **listener is currently commented out** (work-in-progress).

| Property             | Value                                     |
|----------------------|-------------------------------------------|
| **Topic**            | `expense_service`                         |
| **Consumer Group**   | `expense-info-consumer-group`             |
| **Key Deserializer** | `StringDeserializer`                      |
| **Value Deserializer** | `ExpenseDeserializer` (custom)          |

### Custom Deserializer

`ExpenseDeserializer` implements `org.apache.kafka.common.serialization.Deserializer<ExpenseDto>` and uses Jackson `ObjectMapper` to deserialize incoming byte arrays into `ExpenseDto` objects.

### Consumer (WIP)

`ExpenseConsumer` is annotated as a `@Service` and is wired with `ExpenseService`. The `@KafkaListener` method is currently commented out but is designed to:
1. Listen to the `expense_service` topic.
2. Deserialize the event into `ExpenseDto`.
3. Call `ExpenseService.createExpense(...)` to persist the expense.

> **TODO:** Enable the Kafka listener, add transactional support, and handle idempotency (duplicate event detection).

---

## DTOs (Data Transfer Objects)

### `CreateExpenseRequestDto`
Used as the **request body** for the `POST /expense/v1/addExpense` endpoint.

| Field      | Type          | JSON Naming   |
|------------|---------------|---------------|
| `amount`   | `BigDecimal`  | `amount`      |
| `merchant` | `String`      | `merchant`    |
| `currency` | `String`      | `currency`    |

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

---

## Configuration

**`application.properties`**

| Property                                   | Value / Description                                             |
|--------------------------------------------|-----------------------------------------------------------------|
| `server.port`                              | `9820`                                                          |
| `spring.application.name`                  | `service`                                                       |
| `spring.datasource.url`                    | `jdbc:mysql://{MYSQL_HOST}:{MYSQL_PORT}/{MYSQL_DB}`            |
| `spring.datasource.driver-class-name`      | `com.mysql.cj.jdbc.Driver`                                     |
| `spring.jpa.hibernate.ddl-auto`            | `update` (auto-creates/updates schema)                         |
| `spring.jpa.properties.hibernate.dialect`  | `org.hibernate.dialect.MySQL8Dialect`                          |
| `spring.jpa.show-sql`                      | `true`                                                          |
| `spring.kafka.bootstrap-servers`           | `{KAFKA_HOST}:{KAFKA_PORT}`                                    |
| `spring.kafka.consumer.group-id`           | `expense-info-consumer-group`                                  |
| `spring.kafka.topic-json.name`             | `expense_service`                                               |
| `logging.level.org.springframework.web`    | `DEBUG`                                                         |

---

## Environment Variables

| Variable      | Default     | Description                        |
|---------------|-------------|------------------------------------|
| `KAFKA_HOST`  | `localhost` | Kafka broker hostname              |
| `KAFKA_PORT`  | `9092`      | Kafka broker port                  |
| `MYSQL_HOST`  | `localhost` | MySQL server hostname              |
| `MYSQL_PORT`  | `3306`      | MySQL server port                  |
| `MYSQL_DB`    | `expense_service` | MySQL database name          |

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
  expense-service
```

---

## Build & Run

### Prerequisites

- **Java 21**
- **MySQL 8.x** running and accessible
- **Apache Kafka** running (if Kafka consumer is enabled)

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

- [ ] **Enable Kafka Consumer** – Uncomment `@KafkaListener` in `ExpenseConsumer` and wire it with the updated `createExpense` method signature.
- [ ] **Idempotency Handling** – Detect and skip duplicate Kafka events to prevent duplicate expense records.
- [ ] **Transactional Support** – Wrap Kafka consumer processing in a transaction.
- [ ] **Update Expense API** – `updateExpense` logic exists (commented out) in `ExpenseService`; expose it via a `PUT`/`PATCH` endpoint.
- [ ] **Date-range Query** – `findByUserIdAndCreatedAtBetween` repository method exists (commented out) for filtering expenses by date range.
- [ ] **Externalize Credentials** – Move database username/password to environment variables or a secrets manager.
- [ ] **Input Validation** – Add `@Valid` / Bean Validation annotations to request DTOs.
- [ ] **Error Handling** – Add a global `@ControllerAdvice` exception handler for consistent error responses.

---

*This document is auto-generated from the ExpenseService source code and configuration files.*

