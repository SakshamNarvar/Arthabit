# 🔐 AuthService — Authentication & Authorization Microservice

> Part of the **Expense Tracker App** microservice ecosystem.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Configuration](#configuration)
  - [Running Locally](#running-locally)
  - [Running with Docker](#running-with-docker)
- [API Reference](#api-reference)
  - [Public Endpoints (No Auth Required)](#public-endpoints-no-auth-required)
  - [Protected Endpoints (JWT Required)](#protected-endpoints-jwt-required)
- [Authentication Flow](#authentication-flow)
- [Database Schema](#database-schema)
- [Kafka Integration](#kafka-integration)
- [Project Structure](#project-structure)
- [Error Handling](#error-handling)
- [Environment Variables](#environment-variables)

---

## Overview

The **AuthService** is responsible for user registration, authentication, and JWT-based authorization within the Expense Tracker application. It issues short-lived **access tokens** (JWT) and long-lived **refresh tokens**, and publishes user-registration events to Apache Kafka so that other downstream microservices (e.g. User Service) can react accordingly.

### Key Responsibilities

| Responsibility | Description |
|---|---|
| **User Signup** | Registers new users with BCrypt-hashed passwords and assigns the default `ROLE_USER` role |
| **User Login** | Authenticates credentials and returns JWT access + refresh tokens |
| **Token Refresh** | Issues a new access token given a valid refresh token |
| **JWT Validation** | Validates Bearer tokens on every protected request via a security filter |
| **Event Publishing** | Publishes `UserInfoEvent` to Kafka topic `user_service` on successful signup |

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.3.5 |
| **Security** | Spring Security 6 + JWT (jjwt 0.12.6) |
| **Database** | MySQL (via Spring Data JPA + Hibernate) |
| **Messaging** | Apache Kafka (kafka-clients 4.1.1 + Spring Kafka) |
| **Build Tool** | Gradle (Groovy DSL) |
| **API Docs** | SpringDoc OpenAPI / Swagger UI (2.6.0) |
| **Template Engine** | JTE 3.1.16 |
| **Containerization** | Docker (Eclipse Temurin 21 JRE) |
| **Other Libraries** | Lombok, Google Guava 33.5, Jakarta Validation |

---

## Architecture

```
┌─────────────┐        ┌──────────────┐        ┌────────────────┐
│   Client     │──HTTP──▶  AuthService  │──JPA──▶   MySQL DB     │
│  (Frontend)  │◀──JWT──│  :9898       │        │ auth_service_db│
└─────────────┘        └──────┬───────┘        └────────────────┘
                              │
                              │ Kafka (user_service topic)
                              ▼
                       ┌──────────────┐
                       │ User Service  │
                       │ (Consumer)    │
                       └──────────────┘
```

- **Stateless** — No HTTP sessions; every request is authenticated via JWT Bearer tokens.
- **Event-Driven** — On signup, a `UserInfoEvent` is published to Kafka as a fire-and-forget operation. Kafka failures do **not** block user registration.

---

## Getting Started

### Prerequisites

- **Java 21** (Eclipse Temurin recommended)
- **MySQL 8+** running on `localhost:3306` (or via Docker)
- **Apache Kafka** running on `localhost:9092` (or via Docker)
- **Gradle 8+** (wrapper included)

### Configuration

Key properties in `src/main/resources/application.properties`:

| Property | Default | Description |
|---|---|---|
| `server.port` | `9898` | HTTP listen port |
| `MYSQL_HOST` | `localhost` | MySQL hostname (env var) |
| `MYSQL_PORT` | `3306` | MySQL port (env var) |
| `MYSQL_DB` | `auth_service_db` | MySQL database name (env var) |
| `spring.datasource.username` | `root` | DB username |
| `spring.datasource.password` | `narvar007` | DB password |
| `KAFKA_HOST` | `localhost` | Kafka broker hostname (env var) |
| `KAFKA_PORT` | `9092` | Kafka broker port (env var) |
| `spring.kafka.topic-json.name` | `user_service` | Kafka topic for user registration events |
| `spring.jpa.hibernate.ddl-auto` | `update` | Auto-create/update tables on startup |

### Running Locally

```bash
# From the AuthService root directory
./gradlew bootRun
```

The service will start on **http://localhost:9898**.

### Running with Docker

```bash
# 1. Build the JAR
./gradlew bootJar

# 2. Build Docker image
docker build -t auth-service .

# 3. Run container
docker run -p 9898:9898 \
  -e MYSQL_HOST=host.docker.internal \
  -e MYSQL_PORT=3306 \
  -e MYSQL_DB=auth_service_db \
  -e KAFKA_HOST=host.docker.internal \
  -e KAFKA_PORT=9092 \
  auth-service
```

---

## API Reference

**Base URL:** `http://localhost:9898`

**Swagger UI:** [http://localhost:9898/swagger-ui.html](http://localhost:9898/swagger-ui.html)

### Public Endpoints (No Auth Required)

#### `POST /auth/v1/signup` — Register a new user

**Request Body** (`application/json`):

```json
{
  "username": "johndoe",
  "password": "secureP@ss123",
  "first_name": "John",
  "last_name": "Doe",
  "email": "john@example.com",
  "phone_number": 9876543210
}
```

> **Note:** JSON uses `snake_case` naming strategy.

| Field | Type | Required | Validation |
|---|---|---|---|
| `username` | String | ✅ | Not blank |
| `password` | String | ✅ | Not blank |
| `first_name` | String | ✅ | Not blank |
| `last_name` | String | ❌ | Optional |
| `email` | String | ✅ | Valid email format |
| `phone_number` | Long | ✅ | Not null |

**Success Response** `200 OK`:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "token": "a8f5f167-f44f-4598-87b9-c89a5d5f27a8",
  "userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Error Responses:**

| Status | Condition |
|---|---|
| `400 Bad Request` | Validation failure (missing/invalid fields) |
| `409 Conflict` | Username already exists |

---

#### `POST /auth/v1/login` — Authenticate user

**Request Body** (`application/json`):

```json
{
  "username": "johndoe",
  "password": "secureP@ss123"
}
```

| Field | Type | Required |
|---|---|---|
| `username` | String | ✅ |
| `password` | String | ✅ |

**Success Response** `200 OK`:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "token": "a8f5f167-f44f-4598-87b9-c89a5d5f27a8",
  "userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Error Responses:**

| Status | Condition |
|---|---|
| `400 Bad Request` | Missing username or password |
| `401 Unauthorized` | Invalid credentials |

---

#### `POST /auth/v1/refreshToken` — Refresh access token

**Request Body** (`application/json`):

```json
{
  "token": "a8f5f167-f44f-4598-87b9-c89a5d5f27a8"
}
```

| Field | Type | Required |
|---|---|---|
| `token` | String | ✅ |

**Success Response** `200 OK`:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...(new)",
  "token": "a8f5f167-f44f-4598-87b9-c89a5d5f27a8"
}
```

**Error Responses:**

| Status | Condition |
|---|---|
| `400 Bad Request` | Missing token field |
| `403 Forbidden` | Refresh token expired or not found |

---

#### `GET /health` — Health check

**Success Response** `200 OK`:

```json
true
```

---

### Protected Endpoints (JWT Required)

> Include header: `Authorization: Bearer <accessToken>`

#### `GET /auth/v1/ping` — Verify authentication

**Success Response** `200 OK`:

```
Ping Successful for user: 550e8400-e29b-41d4-a716-446655440000
```

**Error Response** `401 Unauthorized` — Missing or invalid JWT.

---

## Authentication Flow

```
 ┌──────┐                       ┌─────────────┐                ┌───────┐
 │Client│                       │ AuthService  │                │  DB   │
 └──┬───┘                       └──────┬───────┘                └───┬───┘
    │  POST /auth/v1/signup            │                            │
    │──────────────────────────────────▶│  Save user (BCrypt hash)  │
    │                                  │───────────────────────────▶│
    │                                  │  Assign ROLE_USER          │
    │                                  │───────────────────────────▶│
    │                                  │  Create refresh token      │
    │                                  │───────────────────────────▶│
    │                                  │                            │
    │                                  │── Kafka: UserInfoEvent ──▶ (async)
    │                                  │                            │
    │◀─────── { accessToken, token, userId } ──────────────────────│
    │                                  │                            │
    │  POST /auth/v1/login             │                            │
    │──────────────────────────────────▶│  Authenticate via         │
    │                                  │  AuthenticationManager     │
    │                                  │───────────────────────────▶│
    │                                  │  Generate JWT + refresh    │
    │◀─────── { accessToken, token, userId } ──────────────────────│
    │                                  │                            │
    │  GET /auth/v1/ping               │                            │
    │  Authorization: Bearer <jwt>     │                            │
    │──────────────────────────────────▶│                            │
    │                                  │ JwtAuthFilter validates    │
    │                                  │ token, sets SecurityContext│
    │◀─────── "Ping Successful" ───────│                            │
    │                                  │                            │
    │  POST /auth/v1/refreshToken      │                            │
    │──────────────────────────────────▶│  Verify refresh token     │
    │                                  │───────────────────────────▶│
    │                                  │  Generate new JWT          │
    │◀─────── { accessToken, token } ──│                            │
```

### Token Details

| Token Type | Mechanism | Lifetime | Storage |
|---|---|---|---|
| **Access Token** | JWT (HS256 signed) | ~100 minutes (`100000 * 60` ms) | Client-side |
| **Refresh Token** | UUID string | ~100 minutes (`6000000` ms) | `tokens` table (DB) |

---

## Database Schema

The service uses **3 tables** with Hibernate `ddl-auto=update` (auto-created on startup):

### `users`

| Column | Type | Constraints |
|---|---|---|
| `user_id` | VARCHAR (PK) | UUID string |
| `username` | VARCHAR | NOT NULL |
| `password` | VARCHAR | NOT NULL (BCrypt hash) |
| `email` | VARCHAR | NOT NULL, valid email |
| `phone_number` | BIGINT | NOT NULL |

### `roles`

| Column | Type | Constraints |
|---|---|---|
| `role_id` | BIGINT (PK) | Auto-increment |
| `name` | VARCHAR | e.g. `ROLE_USER` |

### `tokens`

| Column | Type | Constraints |
|---|---|---|
| `id` | INT (PK) | Auto-increment |
| `token` | VARCHAR | UUID string |
| `expiry_date` | DATETIME | Expiration timestamp |
| `user_id` | VARCHAR (FK) | References `users.user_id` |

### `user_roles` (Join Table)

| Column | Type | Constraints |
|---|---|---|
| `user_id` | VARCHAR (FK) | References `users.user_id` |
| `role_id` | BIGINT (FK) | References `roles.role_id` |

### ER Diagram

```
 ┌─────────┐       ┌────────────┐       ┌─────────┐
 │  users   │──M:N──│ user_roles  │──M:N──│  roles   │
 │          │       └────────────┘       │          │
 │ user_id  │                            │ role_id  │
 │ username │                            │ name     │
 │ password │       ┌────────────┐       └──────────┘
 │ email    │──1:1──│  tokens    │
 │ phone_no │       │ id         │
 └──────────┘       │ token      │
                    │ expiry_date│
                    │ user_id(FK)│
                    └────────────┘
```

---

## Kafka Integration

On every successful **signup**, the service publishes a `UserInfoEvent` to the Kafka topic `user_service`.

### Topic

| Property | Value |
|---|---|
| **Topic Name** | `user_service` |
| **Key Serializer** | `StringSerializer` |
| **Value Serializer** | `UserInfoSerializer` (custom JSON) |
| **Acks** | `all` |
| **Retries** | `3` |

### Event Payload (`UserInfoEvent`)

```json
{
  "first_name": "John",
  "last_name": "Doe",
  "email": "john@example.com",
  "phone_number": 9876543210,
  "user_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

> **Fire-and-forget pattern:** If Kafka is unreachable, the signup still succeeds. The failure is logged but does not propagate to the client.

---

## Project Structure

```
src/main/java/com/nstrange/authservice/
├── App.java                          # Spring Boot entry point
├── auth/
│   ├── JwtAuthFilter.java            # OncePerRequestFilter — validates JWT on protected routes
│   └── UserConfig.java               # BCryptPasswordEncoder bean
├── controller/
│   ├── AuthController.java           # /auth/v1/signup, /auth/v1/ping, /health
│   ├── TokenController.java          # /auth/v1/login, /auth/v1/refreshToken
│   └── SecurityConfig.java           # Spring Security filter chain, CORS, auth provider
├── entities/
│   ├── UserInfo.java                 # JPA entity — users table
│   ├── UserRole.java                 # JPA entity — roles table
│   └── RefreshToken.java             # JPA entity — tokens table
├── eventProducer/
│   ├── UserInfoEvent.java            # Kafka event payload DTO
│   └── UserInfoProducer.java         # Kafka producer service
├── exception/
│   ├── ErrorResponse.java            # Standardized error response body
│   ├── GlobalExceptionHandler.java   # @RestControllerAdvice — centralized exception handling
│   ├── InvalidCredentialsException.java
│   ├── TokenRefreshException.java
│   └── UserAlreadyExistsException.java
├── model/
│   └── UserInfoDto.java              # Signup request DTO with validation
├── repository/
│   ├── UserRepository.java           # JPA repo for UserInfo
│   ├── RoleRepository.java           # JPA repo for UserRole
│   └── RefreshTokenRepository.java   # JPA repo for RefreshToken
├── request/
│   ├── AuthRequestDTO.java           # Login request DTO
│   └── RefreshTokenRequestDTO.java   # Refresh token request DTO
├── response/
│   └── JwtResponseDTO.java           # JWT + refresh token response DTO
├── serializer/
│   └── UserInfoSerializer.java       # Custom Kafka serializer for UserInfoEvent
├── service/
│   ├── JwtService.java               # JWT generation, parsing, validation (HS256)
│   ├── RefreshTokenService.java      # Refresh token CRUD + expiration check
│   ├── UserDetailsServiceImpl.java   # UserDetailsService impl + signup logic
│   └── CustomUserDetails.java        # Spring Security UserDetails adapter
└── utils/
    └── ValidationUtil.java           # (Reserved for future validation utilities)
```

---

## Error Handling

All errors follow a **consistent JSON structure** via `GlobalExceptionHandler`:

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid username or password",
  "path": "/auth/v1/login",
  "timestamp": "2026-03-09 14:30:00"
}
```

### Error Code Summary

| HTTP Status | Exception | Trigger |
|---|---|---|
| `400 Bad Request` | `MethodArgumentNotValidException` | Missing/invalid request fields |
| `400 Bad Request` | `HttpMessageNotReadableException` | Malformed JSON body |
| `401 Unauthorized` | `InvalidCredentialsException` | Wrong username/password |
| `401 Unauthorized` | `UsernameNotFoundException` | User does not exist |
| `401 Unauthorized` | `ExpiredJwtException` | JWT access token expired |
| `401 Unauthorized` | `SignatureException` / `MalformedJwtException` | Tampered/invalid JWT |
| `403 Forbidden` | `TokenRefreshException` | Refresh token expired or not found |
| `409 Conflict` | `UserAlreadyExistsException` | Duplicate username on signup |
| `500 Internal Server Error` | `Exception` (catch-all) | Unexpected server error |

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `MYSQL_HOST` | `localhost` | MySQL server hostname |
| `MYSQL_PORT` | `3306` | MySQL server port |
| `MYSQL_DB` | `auth_service_db` | MySQL database name |
| `KAFKA_HOST` | `localhost` | Kafka broker hostname |
| `KAFKA_PORT` | `9092` | Kafka broker port |

---

## Inter-Service Communication

| Direction | Target Service | Mechanism | Details |
|---|---|---|---|
| **Outbound** | User Service | Kafka (topic: `user_service`) | Publishes `UserInfoEvent` on signup |
| **Inbound** | Any service / API Gateway | HTTP REST | Other services can call `/health` or validate tokens |

---

## Security Highlights

- **Password Hashing** — BCrypt via Spring Security's `PasswordEncoder`
- **Stateless Sessions** — `SessionCreationPolicy.STATELESS`; no server-side session storage
- **JWT Filter** — `JwtAuthFilter` (extends `OncePerRequestFilter`) intercepts all protected routes
- **CORS** — All origins allowed (`*`), exposes `Authorization` header
- **Role-Based Access** — Users are assigned roles via `user_roles` join table (default: `ROLE_USER`)
- **Swagger/OpenAPI** endpoints are fully public (excluded from security filter)

---

> **📌 This document is intended to be consumed by a parent-level project README generator to compose the full Expense Tracker App documentation.**

