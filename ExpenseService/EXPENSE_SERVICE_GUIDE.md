# 🧾 Expense Service — Complete Workflow & Architecture Guide

> **Read this if you're new to this service and don't know where to start.**

---

## Table of Contents

1. [What Does This Service Do? (The Big Picture)](#1-what-does-this-service-do)
2. [Where to Start Reading the Code](#2-where-to-start-reading-the-code)
3. [Technology Stack](#3-technology-stack)
4. [Architecture at a Glance](#4-architecture-at-a-glance)
5. [Complete Data Flow — Step by Step](#5-complete-data-flow--step-by-step)
6. [Every Class Explained (Role, Necessity, What Breaks Without It)](#6-every-class-explained)
7. [How the Layers Connect (Dependency Chain)](#7-how-the-layers-connect)
8. [Configuration Explained](#8-configuration-explained)
9. [Commented-Out / Future Features](#9-commented-out--future-features)
10. [Quick Reference: API Endpoints](#10-quick-reference-api-endpoints)

---

## 1. What Does This Service Do?

This is a **Spring Boot microservice** that manages **expenses** for users. It allows:

- **Creating** an expense (amount, merchant, currency) for a specific user.
- **Retrieving** all expenses belonging to a specific user.

It stores expense data in a **MySQL** database. It also has scaffolding (currently commented out) for consuming expense events from **Apache Kafka**, meaning this service is designed to be part of a larger event-driven microservices architecture.

---

## 2. Where to Start Reading the Code

If you're lost, follow this exact reading order:

| Order | File | Why Read It |
|-------|------|-------------|
| 1️⃣ | `ExpenseServiceApplication.java` | Entry point — the app starts here |
| 2️⃣ | `application.properties` | Tells you what DB, what port, what Kafka config is used |
| 3️⃣ | `Expense.java` (entity) | The core data model — this is what gets stored in the DB |
| 4️⃣ | `ExpenseRepository.java` | How the app talks to the DB |
| 5️⃣ | `CreateExpenseRequestDto.java` | What the client sends when creating an expense |
| 6️⃣ | `ExpenseResponseDto.java` | What the client receives back |
| 7️⃣ | `ExpenseService.java` (service) | The business logic — connects controller to repository |
| 8️⃣ | `ExpenseController.java` | The REST API — HTTP endpoints the outside world calls |
| 9️⃣ | `ExpenseDto.java` | DTO used for Kafka messaging (mostly future use) |
| 🔟 | `ExpenseConsumer.java` + `ExpenseDeserializer.java` | Kafka consumer (currently disabled) |

---

## 3. Technology Stack

| Technology | Purpose |
|-----------|---------|
| **Java 21** | Programming language |
| **Spring Boot 3.5.10** | Application framework |
| **Spring Web** | REST API (controllers, request/response handling) |
| **Spring Data JPA** | Database access (ORM layer over MySQL) |
| **Hibernate** | JPA implementation (auto-creates/updates tables) |
| **MySQL** | Relational database for storing expenses |
| **Apache Kafka** | Message broker for event-driven communication (currently disabled) |
| **Lombok** | Reduces boilerplate (auto-generates getters, setters, builders, constructors) |
| **Jackson** | JSON serialization/deserialization |
| **Docker** | Containerization for deployment |

---

## 4. Architecture at a Glance

```
┌─────────────────────────────────────────────────────────────────┐
│                        OUTSIDE WORLD                            │
│                                                                 │
│   HTTP Client (Postman, Frontend, API Gateway, etc.)            │
│       │                                                         │
│       │  POST /expense/v1/addExpense   (with X-User-Id header)  │
│       │  GET  /expense/v1/getExpense   (with X-User-Id header)  │
│       ▼                                                         │
├─────────────────────────────────────────────────────────────────┤
│                     CONTROLLER LAYER                            │
│                                                                 │
│   ExpenseController.java                                        │
│   - Receives HTTP requests                                      │
│   - Extracts userId from X-User-Id header                       │
│   - Delegates to ExpenseService                                 │
│   - Converts Expense entity → ExpenseResponseDto                │
│   - Returns HTTP response                                       │
│       │                                                         │
│       ▼                                                         │
├─────────────────────────────────────────────────────────────────┤
│                      SERVICE LAYER                              │
│                                                                 │
│   ExpenseService.java                                           │
│   - Business logic (set defaults, convert DTOs)                 │
│   - Calls repository to persist/read data                       │
│       │                                                         │
│       ▼                                                         │
├─────────────────────────────────────────────────────────────────┤
│                    REPOSITORY LAYER                             │
│                                                                 │
│   ExpenseRepository.java                                        │
│   - Interface — Spring auto-implements it                       │
│   - Provides findByUserId() and save()                          │
│       │                                                         │
│       ▼                                                         │
├─────────────────────────────────────────────────────────────────┤
│                       DATABASE                                  │
│                                                                 │
│   MySQL (expense_service database)                              │
│   Table: expense                                                │
│   Columns: id, external_id, user_id, amount, merchant,         │
│            currency, created_at                                 │
└─────────────────────────────────────────────────────────────────┘

       ┌──────────────────────────────────┐
       │  KAFKA (Currently Disabled)      │
       │                                  │
       │  ExpenseConsumer.java            │
       │  - Would listen to Kafka topic   │
       │  - Would call ExpenseService     │
       │    to create expenses from       │
       │    events                        │
       │                                  │
       │  ExpenseDeserializer.java        │
       │  - Converts Kafka byte[] →       │
       │    ExpenseDto                    │
       └──────────────────────────────────┘
```

---

## 5. Complete Data Flow — Step by Step

### Flow 1: Creating an Expense (POST)

```
Step 1  →  Client sends HTTP POST to /expense/v1/addExpense
            Headers:  X-User-Id: "user123"
            Body:     { "amount": 500.00, "merchant": "Amazon", "currency": "usd" }

Step 2  →  Spring deserializes the JSON body into a CreateExpenseRequestDto object
            (fields: amount, merchant, currency)

Step 3  →  ExpenseController.addExpenses() is invoked
            - It extracts userId from the X-User-Id header
            - Calls expenseService.createExpense(requestDto, userId)

Step 4  →  ExpenseService.createExpense() runs the business logic:
            a) Converts CreateExpenseRequestDto → Expense entity (using Jackson ObjectMapper)
            b) Sets userId on the Expense (from the header — not from the request body)
            c) If currency is null → defaults it to "inr"
            d) If createdAt is null → defaults it to current timestamp

Step 5  →  ExpenseService calls expenseRepository.save(expense)
            - Before saving, JPA triggers @PrePersist on the Expense entity
            - The generateExternalId() method creates a UUID as the external_id
            - Hibernate converts the Expense object into an INSERT SQL query
            - MySQL stores the row in the expense table
            - The auto-generated id (primary key) is populated back into the Expense object

Step 6  →  The saved Expense entity (with id, externalId, etc.) is returned
            back up to the controller

Step 7  →  ExpenseController.mapToDto() converts the Expense entity into
            an ExpenseResponseDto (hiding internal fields like id)

Step 8  →  Controller returns HTTP 201 (Created) with the ExpenseResponseDto as JSON:
            {
              "external_id": "a1b2c3d4-...",
              "amount": 500.00,
              "user_id": "user123",
              "merchant": "Amazon",
              "currency": "usd",
              "created_at": "2026-02-27T..."
            }
```

### Flow 2: Getting Expenses (GET)

```
Step 1  →  Client sends HTTP GET to /expense/v1/getExpense
            Headers:  X-User-Id: "user123"

Step 2  →  ExpenseController.getExpense() is invoked
            - Extracts userId from X-User-Id header
            - Calls expenseService.getExpenses(userId)

Step 3  →  ExpenseService.getExpenses() calls expenseRepository.findByUserId(userId)
            - Spring Data JPA auto-generates the query: SELECT * FROM expense WHERE user_id = ?
            - Returns a List<Expense>

Step 4  →  Controller maps each Expense → ExpenseResponseDto using mapToDto()

Step 5  →  Controller returns HTTP 200 with a JSON array of ExpenseResponseDto objects
```

---

## 6. Every Class Explained

### 📁 `ExpenseServiceApplication.java`

**Package:** `com.nstrange.expenseservice`

**What it does:**
- This is the **entry point** of the entire application. The `main()` method boots up the Spring container.
- The `@SpringBootApplication` annotation triggers:
  - **Component scanning** — finds all `@RestController`, `@Service`, `@Repository`, etc.
  - **Auto-configuration** — sets up database connections, Kafka, web server, etc.

**Why it's necessary:**
- Without this class, the application **cannot start**. Period. There is no Spring context, no beans, no web server. Nothing runs.

**What happens if you remove it:**
- ❌ The application will not compile as an executable Spring Boot app.
- ❌ `./gradlew bootRun` will fail with "no main class found".

---

### 📁 `Expense.java` (Entity)

**Package:** `com.nstrange.expenseservice.entities`

**What it does:**
- Represents the **database table** `expense`. Each instance = one row.
- Fields map to columns: `id`, `external_id`, `user_id`, `amount`, `merchant`, `currency`, `created_at`.
- `@Id` + `@GeneratedValue(IDENTITY)` → The `id` column is an auto-increment primary key managed by MySQL.
- `@PrePersist` / `@PreUpdate` → Before saving, if `externalId` is null, it generates a UUID. This gives each expense a unique public-facing ID (so you never expose the database `id` to the outside world).
- `@JsonIgnoreProperties(ignoreUnknown = true)` → When converting from JSON/DTO to this entity, unknown fields are silently ignored instead of causing an error.

**Why it's necessary:**
- This is the **core data model**. Every other class exists to create, read, or transform this object.
- JPA/Hibernate uses this to auto-create/update the MySQL table.

**What happens if you remove it:**
- ❌ No database table is created.
- ❌ `ExpenseRepository` will have nothing to operate on — compilation fails.
- ❌ `ExpenseService` cannot save or retrieve anything.
- ❌ The entire service is useless.

---

### 📁 `ExpenseRepository.java` (Repository)

**Package:** `com.nstrange.expenseservice.repository`

**What it does:**
- An **interface** that extends `CrudRepository<Expense, Long>`.
- Spring Data JPA **auto-generates the implementation** at runtime — you never write SQL.
- Provides inherited methods: `save()`, `findById()`, `findAll()`, `delete()`, etc.
- Adds a custom query method: `findByUserId(String userId)` → Spring generates `SELECT * FROM expense WHERE user_id = ?` automatically just from the method name.

**Why it's necessary:**
- This is the **only way** the application talks to the MySQL database.
- Without it, you'd have to write raw JDBC or SQL manually.

**What happens if you remove it:**
- ❌ `ExpenseService` cannot save or retrieve expenses → compilation error.
- ❌ No data persistence at all.

---

### 📁 `ExpenseService.java` (Service)

**Package:** `com.nstrange.expenseservice.service`

**What it does:**
- Contains **business logic** — the rules of how expenses are created and retrieved.
- `createExpense(CreateExpenseRequestDto, userId)`:
  1. Converts the DTO to an `Expense` entity using `ObjectMapper.convertValue()`.
  2. Sets the `userId` (comes from the HTTP header, not the request body — security!).
  3. Defaults `currency` to `"inr"` if not provided.
  4. Defaults `createdAt` to now if not provided.
  5. Saves via the repository.
- `getExpenses(userId)`: Simply delegates to `expenseRepository.findByUserId()`.

**Why it's necessary:**
- Separates **business logic** from the controller (HTTP handling) and repository (database).
- If the logic lived in the controller, you couldn't reuse it from the Kafka consumer.
- If it lived in the repository, you'd be mixing concerns.

**What happens if you remove it:**
- ❌ The controller and Kafka consumer would need to directly call the repository and duplicate all the business logic (defaults, DTO conversion, etc.).
- ❌ Breaks separation of concerns — future changes become risky.
- ❌ Compilation errors in `ExpenseController` and `ExpenseConsumer`.

---

### 📁 `ExpenseController.java` (Controller)

**Package:** `com.nstrange.expenseservice.controller`

**What it does:**
- The **REST API layer** — the front door of the service.
- `@RestController` + `@RequestMapping("/expense/v1")` → registers HTTP endpoints.
- **GET `/expense/v1/getExpense`**: Reads the `X-User-Id` header, fetches all expenses for that user, converts each `Expense` entity to an `ExpenseResponseDto`, and returns them.
- **POST `/expense/v1/addExpense`**: Reads the `X-User-Id` header + JSON body (`CreateExpenseRequestDto`), delegates creation to the service, and returns the created expense as `ExpenseResponseDto` with HTTP 201.
- Contains a private `mapToDto()` helper that converts `Expense` → `ExpenseResponseDto` (cherry-picks only the fields that should be visible externally).

**Why it's necessary:**
- Without a controller, there are **no HTTP endpoints**. No one can interact with the service over the network.
- It's the translation layer between the HTTP world (headers, JSON, status codes) and the internal Java world.

**What happens if you remove it:**
- ❌ The service starts but is completely unreachable via HTTP.
- ❌ No REST API. The only remaining entry point would be Kafka (which is currently disabled).

---

### 📁 `CreateExpenseRequestDto.java` (DTO — Request)

**Package:** `com.nstrange.expenseservice.dto`

**What it does:**
- A simple **data carrier** for the incoming HTTP POST request body.
- Fields: `amount`, `merchant`, `currency` — only what the client should provide.
- `@JsonNaming(SnakeCaseStrategy.class)` → accepts JSON fields like `"amount"`, `"merchant"`, `"currency"` (maps snake_case JSON to camelCase Java fields).
- Notably, it does **not** have `userId`, `externalId`, or `createdAt` — those are set by the server.

**Why it's necessary:**
- **Security & validation**: The client should NOT be able to set `userId` or `externalId`. This DTO restricts what the client can send.
- Without it, you'd have to accept the raw `Expense` entity from the client, and they could inject their own `id`, `userId`, or `externalId`.

**What happens if you remove it:**
- ❌ The `addExpense` endpoint would need a different request type.
- ⚠️ If you replaced it with `Expense` directly, clients could set internal fields — a security risk.

---

### 📁 `ExpenseResponseDto.java` (DTO — Response)

**Package:** `com.nstrange.expenseservice.dto`

**What it does:**
- A **data carrier** for the HTTP response sent back to the client.
- Fields: `externalId`, `amount`, `userId`, `merchant`, `currency`, `createdAt`.
- Uses `@Builder` → the controller creates it using `ExpenseResponseDto.builder().field(...).build()`.
- `@JsonNaming(SnakeCaseStrategy.class)` → JSON output uses snake_case (e.g., `external_id`, `user_id`, `created_at`).

**Why it's necessary:**
- Hides the internal database `id` field — only `externalId` is exposed to clients.
- Decouples the API response shape from the database entity shape. If you add a column to the database, the API doesn't automatically expose it.

**What happens if you remove it:**
- ❌ The controller would need to return `Expense` directly, exposing the internal `id` (primary key) to clients.
- ⚠️ Any database schema change would directly change the API response — breaking clients.

---

### 📁 `ExpenseDto.java` (DTO — Kafka)

**Package:** `com.nstrange.expenseservice.dto`

**What it does:**
- A DTO designed for **Kafka message consumption** (event-driven architecture).
- Has all fields: `externalId`, `amount`, `userId`, `merchant`, `currency`, `createdAt`.
- Used by `ExpenseDeserializer` to convert Kafka message bytes into a Java object.
- Currently **not actively used** in any live code path (the Kafka listener is commented out).

**Why it's necessary:**
- When another microservice publishes an expense event to Kafka, this DTO defines the shape of that message.
- The separate DTO exists because Kafka messages may have a different structure than HTTP requests.

**What happens if you remove it:**
- ⚠️ Currently no impact (Kafka listener is disabled).
- ❌ When Kafka is enabled, `ExpenseDeserializer` won't compile.

---

### 📁 `ExpenseConsumer.java` (Kafka Consumer)

**Package:** `com.nstrange.expenseservice.consumer`

**What it does:**
- Intended to listen for Kafka messages on the `expense_service` topic.
- The `@KafkaListener` method is **commented out** — so it's currently a no-op.
- When enabled, it would:
  1. Receive an `ExpenseDto` from Kafka.
  2. Call `expenseService.createExpense()` to persist it.
- This provides an **alternative entry point** — expenses can be created via HTTP (controller) OR via Kafka events (consumer).

**Why it's necessary:**
- In a microservices architecture, other services (e.g., an API Gateway or an Order Service) might publish expense events to Kafka. This consumer would pick them up and store them.
- Enables **asynchronous, decoupled** communication between services.

**What happens if you remove it:**
- ⚠️ Currently no impact (listener is disabled).
- ❌ When Kafka is enabled, the service would lose the ability to consume events — only HTTP would work.

---

### 📁 `ExpenseDeserializer.java` (Kafka Deserializer)

**Package:** `com.nstrange.expenseservice.consumer`

**What it does:**
- Implements Kafka's `Deserializer<ExpenseDto>` interface.
- Converts the raw `byte[]` from a Kafka message into an `ExpenseDto` object using Jackson `ObjectMapper`.
- Referenced in `application.properties` as the `value-deserializer` for the Kafka consumer.

**Why it's necessary:**
- Kafka messages are raw bytes. Without a deserializer, the consumer cannot interpret the message content.
- This custom deserializer ensures the JSON in Kafka is properly mapped to `ExpenseDto`.

**What happens if you remove it:**
- ⚠️ Currently no impact (Kafka listener is disabled).
- ❌ When Kafka is enabled, the consumer would fail to start — Spring wouldn't know how to deserialize incoming messages.

---

## 7. How the Layers Connect

```
                ┌──────────────────┐
                │  HTTP Request    │
                └────────┬─────────┘
                         │
                         ▼
              ┌─────────────────────┐      ┌──────────────────────────┐
              │  ExpenseController  │      │  CreateExpenseRequestDto │ ◄── incoming JSON body
              │  (REST API Layer)   │      │  ExpenseResponseDto      │ ──► outgoing JSON body
              └──────────┬──────────┘      └──────────────────────────┘
                         │
                         │ calls
                         ▼
              ┌─────────────────────┐
              │   ExpenseService    │
              │  (Business Logic)   │
              └──────────┬──────────┘
                         │
                         │ calls
                         ▼
              ┌─────────────────────┐
              │  ExpenseRepository  │
              │  (Data Access)      │
              └──────────┬──────────┘
                         │
                         │ SQL queries (auto-generated)
                         ▼
              ┌─────────────────────┐
              │      MySQL DB       │
              │  Table: expense     │
              └─────────────────────┘


  (Future / Currently Disabled)

              ┌─────────────────────┐
              │   Kafka Topic       │
              │  "expense_service"  │
              └──────────┬──────────┘
                         │
                         │ raw bytes
                         ▼
              ┌──────────────────────┐
              │  ExpenseDeserializer │ ──► converts bytes to ExpenseDto
              └──────────┬───────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │   ExpenseConsumer   │ ──► calls ExpenseService.createExpense()
              └─────────────────────┘
```

### Dependency Summary

| Class | Depends On |
|-------|-----------|
| `ExpenseController` | `ExpenseService`, `ObjectMapper`, `CreateExpenseRequestDto`, `ExpenseResponseDto`, `Expense` |
| `ExpenseService` | `ExpenseRepository`, `ObjectMapper`, `CreateExpenseRequestDto`, `Expense` |
| `ExpenseRepository` | `Expense` (the entity it manages) |
| `ExpenseConsumer` | `ExpenseService`, `ExpenseDto` |
| `ExpenseDeserializer` | `ExpenseDto`, `ObjectMapper` |
| `Expense` | Nothing (it's the foundation) |
| DTOs | Nothing (pure data holders) |

---

## 8. Configuration Explained

### `application.properties` — Line by Line

```properties
# ====== KAFKA ======
spring.kafka.bootstrap-servers=${KAFKA_HOST:localhost}:${KAFKA_PORT:9092}
# ↑ Kafka broker address. Uses env vars with defaults: localhost:9092

spring.kafka.consumer.group-id=expense-info-consumer-group
# ↑ Consumer group — Kafka uses this to track which messages have been consumed

spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
# ↑ Message keys are deserialized as plain strings

spring.kafka.consumer.value-deserializer=com.nstrange.expenseservice.consumer.ExpenseDeserializer
# ↑ Message values use the custom ExpenseDeserializer → ExpenseDto

spring.kafka.consumer.properties.spring.json.trusted.packages=*
# ↑ Trusts all packages for JSON deserialization (security setting)

spring.kafka.topic-json.name=expense_service
# ↑ The Kafka topic name this service listens to

spring.kafka.consumer.properties.spring.json.type.mapping=com.nstrange.expenseservice.dto.ExpenseDto
# ↑ Maps Kafka message type to ExpenseDto class

# ====== DATABASE (MySQL) ======
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
# ↑ MySQL JDBC driver

spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DB:expense_service}?useSSL=false&useUnicode=yes&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
# ↑ Database URL — connects to expense_service database. Uses env vars with defaults.

spring.datasource.username=root
spring.datasource.password=narvar007
# ↑ DB credentials (⚠️ hardcoded — should use env vars in production)

spring.jpa.show-sql=true
# ↑ Logs every SQL query Hibernate executes (useful for debugging)

spring.jpa.hibernate.ddl-auto=create
# ↑ ⚠️ DROPS and RECREATES all tables on every app start! (dangerous in production)

spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
# ↑ Tells Hibernate to generate MySQL 8-compatible SQL

spring.jpa.properties.hibernate.hbm2ddl.auto=update
# ↑ Conflicting with ddl-auto=create above (this says "update" instead of "create")

# ====== LOGGING ======
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.web.servlet.mvc.method.annotation=TRACE
# ↑ Verbose logging for debugging HTTP requests

# ====== SERVER ======
server.error.include-message=always
server.error.include-binding-errors=always
# ↑ Error responses include detailed messages (helpful during development)

spring.application.name=service
server.port=9820
# ↑ App runs on port 9820
```

### `build.gradle` — Key Dependencies

| Dependency | Purpose |
|-----------|---------|
| `spring-boot-starter-web` | Provides embedded Tomcat + Spring MVC for REST APIs |
| `spring-boot-starter-data-jpa` | Provides JPA + Hibernate for database access |
| `spring-kafka` | Kafka client library for producing/consuming messages |
| `mysql-connector-j` | MySQL JDBC driver (runtime only) |
| `lombok` | Annotation processor that generates getters, setters, builders, constructors at compile time |

### `Dockerfile`

- Uses `eclipse-temurin:21-jre` (lightweight JRE image — no full JDK needed in production).
- Copies the built JAR into `/app/`.
- Exposes port `9820`.
- Runs the JAR with `java -jar`.

---

## 9. Commented-Out / Future Features

The codebase has several commented-out sections that reveal **planned features**:

| Location | What's Commented Out | What It Would Do |
|----------|---------------------|------------------|
| `ExpenseConsumer.java` | `@KafkaListener` method | Enable Kafka event consumption → create expenses from Kafka messages |
| `ExpenseService.java` | `updateExpense()` method | Allow updating an existing expense (find by userId + externalId, update fields) |
| `ExpenseService.java` | `setCurrency()` helper | A reusable currency-defaulting method |
| `ExpenseRepository.java` | `findByUserIdAndCreatedAtBetween()` | Query expenses within a date range (e.g., monthly reports) |
| `ExpenseRepository.java` | `findByUserIdAndExternalId()` | Find a single expense by its public ID (needed for the update feature) |
| `ExpenseDto.java` | Constructor from JSON string | Allow creating an ExpenseDto by passing a raw JSON string |

---

## 10. Quick Reference: API Endpoints

### Create an Expense

```
POST /expense/v1/addExpense
```

**Headers:**
| Header | Required | Description |
|--------|----------|-------------|
| `X-User-Id` | ✅ Yes | The ID of the user creating the expense |

**Request Body (JSON):**
```json
{
  "amount": 250.50,
  "merchant": "Starbucks",
  "currency": "usd"
}
```

**Response (201 Created):**
```json
{
  "external_id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "amount": 250.50,
  "user_id": "user123",
  "merchant": "Starbucks",
  "currency": "usd",
  "created_at": "2026-02-27T10:30:00.000+00:00"
}
```

---

### Get All Expenses for a User

```
GET /expense/v1/getExpense
```

**Headers:**
| Header | Required | Description |
|--------|----------|-------------|
| `X-User-Id` | ✅ Yes | The ID of the user whose expenses to retrieve |

**Response (200 OK):**
```json
[
  {
    "external_id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "amount": 250.50,
    "user_id": "user123",
    "merchant": "Starbucks",
    "currency": "usd",
    "created_at": "2026-02-27T10:30:00.000+00:00"
  }
]
```

---

## 🎯 TL;DR — The Mental Model

Think of this service as a **sandwich**:

```
🍞  Controller        — talks to the outside world (HTTP)
🥬  DTOs              — shape of data going in / coming out
🥩  Service           — the actual business rules
🧀  Repository        — talks to the database
🍞  Entity + Database — where data lives permanently
```

**Data flows down** (request → controller → service → repository → DB) and **results flow back up** (DB → repository → service → controller → response).

The **Kafka consumer** is a side door — another way to push data into the service without going through HTTP. It plugs directly into the **service layer**, bypassing the controller entirely.

