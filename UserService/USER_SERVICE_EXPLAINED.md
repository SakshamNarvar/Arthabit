# 🧭 UserService — Complete Workflow & Architecture Guide

> **Purpose of this document:** If you're opening this project for the first time and don't know where to start, this is your map. It explains every class, why it exists, how data flows, and what breaks if you remove it.

---

## Table of Contents

1. [What Does This Service Do? (The Big Picture)](#1-what-does-this-service-do)
2. [Where to Start Reading the Code](#2-where-to-start-reading-the-code)
3. [Architecture Diagram](#3-architecture-diagram)
4. [Complete Data Flow — Step by Step](#4-complete-data-flow--step-by-step)
5. [Class-by-Class Breakdown](#5-class-by-class-breakdown)
6. [Configuration Files Explained](#6-configuration-files-explained)
7. [Dependency Overview (build.gradle)](#7-dependency-overview-buildgradle)
8. [Docker Deployment](#8-docker-deployment)
9. [Quick Reference: What Calls What](#9-quick-reference-what-calls-what)
10. [Common Questions](#10-common-questions)

---

## 1. What Does This Service Do?

The **UserService** is a **microservice** in the Expense Tracker App responsible for **storing and managing user profile data** (name, email, phone number, profile picture, etc.).

It does **NOT** handle authentication (login/signup). That's a separate **AuthService**. When a user registers via the AuthService, the AuthService publishes a Kafka event, and this UserService **listens** for that event and saves the user's info into a MySQL database.

It also exposes **REST API endpoints** so other services (or a frontend) can fetch or update user profiles directly.

**In one sentence:** _This service owns user profile data — it receives new user events from Kafka and serves user data via REST APIs._

---

## 2. Where to Start Reading the Code

If you're confused about what to read first, follow this order:

| Step | File | Why |
|------|------|-----|
| 1️⃣ | `UserServiceApplication.java` | This is the entry point — Spring Boot starts here |
| 2️⃣ | `application.properties` | Understand what external systems this service connects to (Kafka, MySQL) |
| 3️⃣ | `UserInfo.java` | The core data — what a "user" looks like in the database |
| 4️⃣ | `UserInfoDto.java` | The data transfer shape — how user data flows between layers |
| 5️⃣ | `UserRepository.java` | How the service talks to the database |
| 6️⃣ | `UserService.java` | The brain — business logic for creating/fetching users |
| 7️⃣ | `AuthServiceConsumer.java` | How user data arrives via Kafka (from the Auth Service) |
| 8️⃣ | `UserController.java` | REST API endpoints — how external clients interact |
| 9️⃣ | `UserInfoDeserializer.java` | How raw Kafka bytes become a Java object |
| 🔟 | `UserServiceConfig.java` | Shared beans / configuration |

---

## 3. Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                        EXTERNAL WORLD                            │
│                                                                  │
│   ┌──────────────┐         ┌──────────────────────────┐         │
│   │  Auth Service │────────▶│    Apache Kafka           │         │
│   │  (signup)     │ publish │  topic: "user_service"    │         │
│   └──────────────┘         └───────────┬──────────────┘         │
│                                        │ consume                 │
│   ┌──────────────┐                     ▼                         │
│   │  Frontend /   │         ┌──────────────────────┐             │
│   │  Other Service│────────▶│   USER SERVICE        │             │
│   │  (HTTP)       │  REST   │                      │             │
│   └──────────────┘         │ ┌──────────────────┐  │             │
│                             │ │ AuthService      │  │             │
│                             │ │ Consumer         │  │             │
│                             │ │ (Kafka Listener) │  │             │
│                             │ └───────┬──────────┘  │             │
│                             │         │              │             │
│                             │ ┌───────┴──────────┐  │             │
│                             │ │ UserController    │  │             │
│                             │ │ (REST endpoints)  │  │             │
│                             │ └───────┬──────────┘  │             │
│                             │         │              │             │
│                             │ ┌───────▼──────────┐  │             │
│                             │ │ UserService       │  │             │
│                             │ │ (business logic)  │  │             │
│                             │ └───────┬──────────┘  │             │
│                             │         │              │             │
│                             │ ┌───────▼──────────┐  │             │
│                             │ │ UserRepository    │  │             │
│                             │ │ (JPA / database)  │  │             │
│                             │ └───────┬──────────┘  │             │
│                             └─────────┼──────────────┘             │
│                                       │                           │
│                              ┌────────▼────────┐                 │
│                              │   MySQL Database │                 │
│                              │  table: "users"  │                 │
│                              └─────────────────┘                 │
└──────────────────────────────────────────────────────────────────┘
```

---

## 4. Complete Data Flow — Step by Step

### Flow A: User Signup (via Kafka — the primary flow)

This is the **most important** flow. When someone registers on the app:

```
Step 1  →  A user signs up on the frontend
Step 2  →  The Auth Service handles authentication (password, token, etc.)
Step 3  →  Auth Service publishes a Kafka message to topic "user_service"
           The message is a JSON like:
           {
             "user_id": "abc123",
             "first_name": "Lovepreet",
             "last_name": "Singh",
             "phone_number": 9876543210,
             "email": "love@example.com",
             "profile_pic": "url..."
           }
Step 4  →  Kafka delivers this message to UserService
Step 5  →  UserInfoDeserializer converts the raw bytes into a UserInfoDto object
Step 6  →  AuthServiceConsumer.listen() receives the UserInfoDto
Step 7  →  AuthServiceConsumer calls userService.createOrUpdateUser(userInfoDto)
Step 8  →  UserService converts UserInfoDto → UserInfo (database entity)
Step 9  →  UserService calls userRepository.findByUserId() to check if user exists
Step 10 →  If exists → update it; If not → create it (save to MySQL)
Step 11 →  UserService returns the saved UserInfoDto back
Step 12 →  Done! User is now in the database ✅
```

### Flow B: Get User Profile (via REST API)

```
Step 1  →  A client sends GET /user/v1/getUser with JSON body containing user_id
Step 2  →  UserController.getUser() receives the request
Step 3  →  UserController calls userService.getUser(userInfoDto)
Step 4  →  UserService calls userRepository.findByUserId(userId)
Step 5  →  If found → convert UserInfo entity to UserInfoDto and return with 200 OK
Step 6  →  If not found → throw exception → controller returns 404 NOT_FOUND
```

### Flow C: Create/Update User (via REST API)

```
Step 1  →  A client sends POST /user/v1/createUpdate with user data
Step 2  →  UserController.createUpdateUser() receives the request
Step 3  →  Same logic as Kafka flow: calls userService.createOrUpdateUser()
Step 4  →  User is created or updated in MySQL
Step 5  →  Returns the saved user data with 200 OK
```

---

## 5. Class-by-Class Breakdown

### 📄 `UserServiceApplication.java`
**Location:** `src/main/java/com/nstrange/userservice/`

| Aspect | Detail |
|--------|--------|
| **What it does** | The entry point of the entire application. Contains `main()`. Spring Boot scans from this package downward for all `@Service`, `@Controller`, `@Repository`, `@Configuration`, etc. |
| **Why it's necessary** | Without it, the application literally cannot start. Spring Boot needs a class annotated with `@SpringBootApplication` and a `main()` method to bootstrap everything. |
| **What happens if removed** | ❌ **The application won't start at all.** Nothing works. |

---

### 📄 `UserServiceConfig.java`
**Location:** `config/`

| Aspect | Detail |
|--------|--------|
| **What it does** | A Spring `@Configuration` class that defines shared beans. Currently, it creates a single `ObjectMapper` bean (Jackson's JSON ↔ Java converter). |
| **Why it's necessary** | The `ObjectMapper` is used by `AuthServiceConsumer` (injected via `@Autowired`). By defining it as a `@Bean`, Spring manages its lifecycle — you get **one shared instance** across the app instead of creating new ones everywhere. |
| **What happens if removed** | ❌ `AuthServiceConsumer` will fail at startup with a `NoSuchBeanDefinitionException` because it tries to `@Autowired` an `ObjectMapper` that doesn't exist in the Spring context. (Note: Spring Boot auto-configures an `ObjectMapper` in most setups, but this explicit bean ensures it's always available with default settings.) |

---

### 📄 `AuthServiceConsumer.java`
**Location:** `consumer/`

| Aspect | Detail |
|--------|--------|
| **What it does** | A **Kafka consumer** that listens to the `user_service` Kafka topic. When the Auth Service publishes a "new user registered" event, this class receives it. It takes the incoming `UserInfoDto` and calls `userService.createOrUpdateUser()` to save it. |
| **Why it's necessary** | This is the **bridge between the Auth Service and the User Service**. In a microservices architecture, services communicate asynchronously via message brokers (Kafka). This class is *how* the UserService knows a new user was created. |
| **What happens if removed** | ❌ **New user registrations will never be saved.** The Auth Service will publish events to Kafka, but nobody will be listening. The `users` table will remain empty for Kafka-driven flows. Only manual REST API calls would work. |

**Key annotation:**
```java
@KafkaListener(topics = "${spring.kafka.topic-json.name}", groupId = "${spring.kafka.consumer.group-id}")
```
- `topics` → reads from `application.properties`: `spring.kafka.topic-json.name=user_service`
- `groupId` → reads from `application.properties`: `spring.kafka.consumer.group-id=userinfo-consumer-group`

---

### 📄 `UserController.java`
**Location:** `consumer/`

| Aspect | Detail |
|--------|--------|
| **What it does** | A **REST controller** exposing HTTP endpoints for user operations. Provides three endpoints: `GET /user/v1/getUser`, `POST /user/v1/createUpdate`, and `GET /health`. |
| **Why it's necessary** | Not all interactions come through Kafka. Other services or the frontend may need to **directly query** or **update** a user's profile via HTTP. This controller is the entry point for all synchronous (request-response) interactions. |
| **What happens if removed** | ❌ **No REST API.** You can't fetch user data via HTTP. Other services can't look up user profiles. The health check endpoint disappears, so container orchestrators (Docker/Kubernetes) can't verify the service is alive. |

**Endpoints:**

| Method | URL | Purpose |
|--------|-----|---------|
| `GET` | `/user/v1/getUser` | Fetch a user by `user_id` (passed in request body) |
| `POST` | `/user/v1/createUpdate` | Create or update a user |
| `GET` | `/health` | Health check — returns `true` if the service is running |

> ⚠️ **Note:** The `getUser` endpoint uses `@RequestBody` on a GET request, which is unconventional. Typically, GET requests use path variables or query parameters, not a request body. Some HTTP clients may not support bodies in GET requests.

---

### 📄 `UserInfo.java`
**Location:** `entities/`

| Aspect | Detail |
|--------|--------|
| **What it does** | The **JPA entity** — a direct mapping to the `users` table in MySQL. Each instance of this class represents one row in the database. Fields: `id`, `userId`, `firstName`, `lastName`, `phoneNumber`, `email`, `profilePic`. |
| **Why it's necessary** | JPA (Java Persistence API) needs an `@Entity` class to know what the database table looks like. This is the "shape" of your data in the database. Without it, Spring Data JPA can't generate SQL queries. |
| **What happens if removed** | ❌ **Complete database failure.** `UserRepository` references `UserInfo` as its entity type. JPA won't know what table to use. The entire persistence layer collapses — no reads, no writes. |

**Key details:**
- `@Id` is on `userId` (a `String`), **not** on `id` (the auto-generated `Long`). This means `userId` is the **primary key**.
- `@Builder` enables the builder pattern (used in `UserInfoDto.transformToUserInfo()`).
- `@Table(name = "users")` maps this class to a MySQL table called `users`.

---

### 📄 `UserInfoDto.java`
**Location:** `entities/`

| Aspect | Detail |
|--------|--------|
| **What it does** | A **Data Transfer Object (DTO)** — the shape of user data as it travels between layers (Kafka → Service, Controller → Service, Service → Response). It does NOT have JPA annotations — it's not tied to the database. It also has a `transformToUserInfo()` method that converts itself into a `UserInfo` entity. |
| **Why it's necessary** | **Separation of concerns.** You don't want to expose your database entity directly to the outside world. The DTO acts as a "contract" between your service and its clients. It also handles JSON serialization (with `@JsonNaming` for snake_case) independently of the database entity. |
| **What happens if removed** | ❌ **Everything breaks.** The Kafka consumer expects `UserInfoDto`. The controller endpoints accept/return `UserInfoDto`. The service layer works with `UserInfoDto`. Removing it breaks every layer of the application. |

**Key detail — `transformToUserInfo()`:**
```java
public UserInfo transformToUserInfo() {
    return UserInfo.builder()
            .firstName(firstName)
            .lastName(lastName)
            .userId(userId)
            .email(email)
            .profilePic(profilePic)
            .phoneNumber(phoneNumber).build();
}
```
This is the **DTO → Entity conversion**. It's called in `UserService.createOrUpdateUser()` before saving to the database.

---

### 📄 `UserRepository.java`
**Location:** `repository/`

| Aspect | Detail |
|--------|--------|
| **What it does** | A Spring Data **repository interface** that provides database operations for `UserInfo`. Extends `CrudRepository<UserInfo, String>` which gives you `save()`, `findById()`, `delete()`, etc. for free. Also defines a custom query method `findByUserId()`. |
| **Why it's necessary** | This is your **data access layer**. Without writing any SQL, Spring Data JPA generates the implementation at runtime. The `findByUserId(String userId)` method is automatically translated to `SELECT * FROM users WHERE user_id = ?`. |
| **What happens if removed** | ❌ **No database access.** `UserService` depends on `UserRepository` for all reads and writes. Without it, you can't save users, can't find users — the service becomes useless. |

**Key detail:** `CrudRepository<UserInfo, String>` — the `String` generic type means the primary key type is `String` (matching `userId` in `UserInfo`).

---

### 📄 `UserService.java`
**Location:** `service/`

| Aspect | Detail |
|--------|--------|
| **What it does** | The **business logic layer**. Contains two methods: `createOrUpdateUser()` (upsert logic) and `getUser()` (fetch by userId). It sits between the controllers/consumers and the repository. |
| **Why it's necessary** | This is where **business rules** live. The controller/consumer shouldn't directly talk to the repository — that would mix concerns. The service layer can add validation, transformation, error handling, and orchestration logic. |
| **What happens if removed** | ❌ Both `UserController` and `AuthServiceConsumer` depend on it. The Kafka consumer can't save users. The REST endpoints can't fetch or create users. **All business operations stop.** |

**How `createOrUpdateUser()` works (the upsert pattern):**
```
1. Look up user by userId → userRepository.findByUserId()
2. If user exists → map() is called → save updated data (updatingUser)
3. If user doesn't exist → orElseGet() is called → save new data (createUser)
4. Convert saved UserInfo entity back to UserInfoDto and return
```

> ⚠️ **Note:** Currently, both `updatingUser` and `createUser` do the same thing (`userRepository.save(userInfoDto.transformToUserInfo())`). The update path doesn't merge existing data with new data — it fully replaces. There's a TODO comment in `AuthServiceConsumer` about making this transactional.

---

### 📄 `UserInfoDeserializer.java`
**Location:** `deserializer/`

| Aspect | Detail |
|--------|--------|
| **What it does** | A custom **Kafka deserializer** that converts raw bytes from Kafka into a `UserInfoDto` Java object. Kafka messages are transmitted as byte arrays — this class tells Kafka *how* to convert those bytes into something your code can work with. |
| **Why it's necessary** | Kafka doesn't know about your Java classes. By default, it can only deserialize simple types (String, Integer, etc.). For a custom object like `UserInfoDto`, you need a custom deserializer. This is registered in `application.properties` as `spring.kafka.consumer.value-deserializer`. |
| **What happens if removed** | ❌ **Kafka consumer crashes.** When a message arrives on the `user_service` topic, Kafka won't know how to deserialize it. You'll get a `SerializationException`. The entire Kafka consumption pipeline breaks. |

**How it works:**
```
Raw bytes (from Kafka) → ObjectMapper.readValue() → UserInfoDto object
```

---

## 6. Configuration Files Explained

### `application.properties`

This file is split into logical sections:

#### Kafka Configuration
```properties
spring.kafka.bootstrap-servers=${KAFKA_HOST:localhost}:${KAFKA_PORT:9092}
```
- Where Kafka is running. Uses environment variables with localhost as default.

```properties
spring.kafka.consumer.group-id=userinfo-consumer-group
```
- The Kafka consumer group. All instances of UserService in this group share the work of reading messages.

```properties
spring.kafka.consumer.value-deserializer=com.nstrange.userservice.deserializer.UserInfoDeserializer
```
- **This is where `UserInfoDeserializer` is registered.** Kafka uses it to convert bytes → `UserInfoDto`.

```properties
spring.kafka.topic-json.name=user_service
```
- The Kafka topic to listen to. The Auth Service publishes to this same topic.

#### MySQL / JPA Configuration
```properties
spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DB:user_service}
```
- MySQL connection string. Database name: `user_service`.

```properties
spring.jpa.properties.hibernate.hbm2ddl.auto=update
```
- Hibernate will **automatically create/update** the `users` table based on the `UserInfo` entity. You don't need to write SQL DDL.

#### Server
```properties
server.port=9810
```
- The service runs on port **9810**.

---

## 7. Dependency Overview (build.gradle)

| Dependency | Why It's Needed |
|-----------|----------------|
| `spring-boot-starter` | Core Spring Boot functionality |
| `spring-boot-starter-web` | REST controllers (`@RestController`, `@GetMapping`, etc.) |
| `spring-boot-starter-data-jpa` | JPA/Hibernate for database access (`@Entity`, `CrudRepository`) |
| `spring-kafka` | Kafka consumer support (`@KafkaListener`) |
| `mysql-connector-java` | JDBC driver to connect to MySQL |
| `jackson-databind` | JSON serialization/deserialization (`ObjectMapper`) |
| `lombok` | Reduces boilerplate (`@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`) |

---

## 8. Docker Deployment

The `Dockerfile` packages the service into a container:

```
Base image: eclipse-temurin:21-jre (Java 21 runtime)
JAR file:   build/libs/userservice-0.0.1-SNAPSHOT.jar
Port:       9810
Command:    java -jar /app/userservice-0.0.1-SNAPSHOT.jar
```

In production, this container needs:
- A running **Kafka** broker (env: `KAFKA_HOST`, `KAFKA_PORT`)
- A running **MySQL** database (env: `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DB`)

---

## 9. Quick Reference: What Calls What

```
AuthService (external)
    │
    │ publishes Kafka message
    ▼
AuthServiceConsumer.listen()
    │
    │ calls
    ▼
UserService.createOrUpdateUser()
    │
    │ calls
    ▼
UserInfoDto.transformToUserInfo()  →  UserRepository.save()  →  MySQL
```

```
HTTP Client (frontend / other service)
    │
    │ GET /user/v1/getUser
    ▼
UserController.getUser()
    │
    │ calls
    ▼
UserService.getUser()
    │
    │ calls
    ▼
UserRepository.findByUserId()  →  MySQL
```

---

## 10. Common Questions

### Q: Why are there TWO entry points (Kafka + REST)?
**A:** This is the nature of microservices. The Kafka consumer handles **asynchronous** events (user registered → save profile). The REST API handles **synchronous** requests (show me this user's profile). Both are needed.

### Q: Why is `UserController` in the `consumer` package?
**A:** This is likely a naming choice by the developer — "consumer" here means "something that consumes/uses the UserService." It could arguably be in a `controller` package for clarity.

### Q: Why do we need both `UserInfo` and `UserInfoDto`?
**A:** `UserInfo` is tied to the database (has `@Entity`, `@Table`, `@Id`). `UserInfoDto` is tied to the API/Kafka (has `@JsonNaming`, `@JsonIgnoreProperties`). Separating them means you can change your database schema without breaking your API, and vice versa.

### Q: Why does `UserInfo` have both `id` (Long) and `userId` (String)?
**A:** `id` is an auto-generated numeric ID (`@GeneratedValue`), but `userId` is the actual primary key (`@Id`) — likely a UUID or external identifier from the Auth Service. The `id` field is currently **not used as the primary key** despite having `@GeneratedValue`.

### Q: What is the consumer group (`userinfo-consumer-group`)?
**A:** If you run multiple instances of UserService, Kafka distributes messages among them within the same group. This ensures each message is processed **only once** across all instances. If they had different group IDs, each instance would process every message (duplicates).

### Q: What's the `@JsonNaming(SnakeCaseStrategy.class)` on the DTO?
**A:** It tells Jackson to automatically convert Java's camelCase field names to snake_case in JSON. So `firstName` becomes `first_name` in the JSON payload. The `@JsonProperty` annotations are also explicitly set, which takes precedence.

---

## Summary: The Mental Model

```
┌─────────────────────────────────────────────────────┐
│  Think of it like a restaurant:                     │
│                                                     │
│  🚪 Doors (entry points):                          │
│     - Kafka Consumer = back door (kitchen delivery) │
│     - REST Controller = front door (customers)      │
│                                                     │
│  👨‍🍳 Chef (business logic):                         │
│     - UserService = decides what to cook            │
│                                                     │
│  🗄️ Pantry (storage):                              │
│     - UserRepository = stores/retrieves ingredients │
│     - MySQL = the actual fridge                     │
│                                                     │
│  📦 Containers (data shapes):                       │
│     - UserInfoDto = takeout box (for customers)     │
│     - UserInfo = storage container (for the fridge) │
│                                                     │
│  🔧 Tools:                                         │
│     - UserInfoDeserializer = unpacks deliveries     │
│     - UserServiceConfig = kitchen equipment setup   │
│     - ObjectMapper = the knife that cuts JSON       │
└─────────────────────────────────────────────────────┘
```

---

*Generated for the Expense Tracker App — UserService module*

