# 🔐 AuthService — Authentication & Authorization Microservice

Part of the Expense Tracker App. Handles signup, login, JWT issuance/validation, refresh tokens, and Kafka user events.

## Quick Facts

- Java 21, Spring Boot 3.3.x, Spring Security 6 + JWT
- MySQL via Spring Data JPA; Kafka for signup events (`user_service` topic)
- Gradle build; Docker image based on Eclipse Temurin 21

## Run It

Prereqs: Java 21, MySQL 8+, Kafka, Gradle wrapper.

```bash
# Local run
./gradlew bootRun

# Build jar
./gradlew bootJar

# Docker
docker build -t auth-service .
docker run -p 9898:9898 \
  -e MYSQL_HOST=host.docker.internal \
  -e MYSQL_PORT=3306 \
  -e MYSQL_DB=auth_service_db \
  -e KAFKA_HOST=host.docker.internal \
  -e KAFKA_PORT=9092 \
  auth-service
```

## Configuration

Edit `src/main/resources/application.properties` or env vars:

| Property / Env | Default | Purpose |
|---|---|---|
| `server.port` | `9898` | HTTP port |
| `MYSQL_HOST` | `localhost` | DB host |
| `MYSQL_PORT` | `3306` | DB port |
| `MYSQL_DB` | `auth_service_db` | DB name |
| `spring.datasource.username` | `root` | DB user |
| `spring.datasource.password` | `narvar007` | DB password |
| `KAFKA_HOST` | `localhost` | Kafka host |
| `KAFKA_PORT` | `9092` | Kafka port |
| `spring.kafka.topic-json.name` | `user_service` | Signup event topic |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema management |

## API (Base `http://localhost:9898`)

- `POST /auth/v1/signup` — create user (`username`, `password`, `first_name`, `email`, `phone_number`; snake_case JSON). Returns `{ accessToken, token, userId }`. `409` if username exists.
- `POST /auth/v1/login` — credentials in body; returns `{ accessToken, token, userId }`. `401` on invalid creds.
- `POST /auth/v1/refreshToken` — body `{ token }`; returns new `accessToken`. `403` if refresh expired/missing.
- `GET /auth/v1/ping` — requires `Authorization: Bearer <accessToken>`; confirms auth.
- `GET /health` — health probe.

## How It Works

- Stateless JWT auth; no HTTP sessions. Access token ~100 minutes (HS256). Refresh token persisted in `tokens` table with similar lifetime.
- Signup flow: store user (BCrypt password), assign `ROLE_USER`, create refresh token, publish `UserInfoEvent` to Kafka (fire-and-forget).
- Security: `JwtAuthFilter` validates Bearer tokens; CORS allows all origins.

## Data Model (DDL auto-update)

- users:`user_id`(PK), `username`, `password`, `password_hint`, `email`, `phone_number`, `account_creation_date`; `account_creation_date` set on insert; roles eager many-to-many via `user_roles`.
- roles: `role_id` (PK), `name` plus join table `user_roles: `user_id`, `role_id`.
- tokens: `id` (PK), `token`, `expiry_date`, `user_id` (FK) — one-to-one with `users` (one refresh token per user).

## Kafka

- Topic `user_service`; key `StringSerializer`; value `org.springframework.kafka.support.serializer.JsonSerializer`; `acks=all`, retries `3`.
- Payload example:

```json
{
  "first_name": "John",
  "last_name": "Doe",
  "email": "john@example.com",
  "phone_number": 9876543210,
  "user_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

## Project Layout

- `App.java` — Spring Boot entry
- `controller/` — `AuthController` (signup, ping, health), `TokenController` (login, refresh)
- `auth/` — `JwtAuthFilter`, `UserConfig` (BCrypt)
- `service/` — `JwtService`, `RefreshTokenService`, `UserDetailsServiceImpl`, `CustomUserDetails`
- `entities/` — `UserInfo`, `UserRole`, `RefreshToken`
- `eventProducer/` — `UserInfoEvent`, `UserInfoProducer`
- `repository/` — JPA repos for users, roles, tokens
- `exception/` — centralized handlers and custom exceptions
- `model/request/response/` — DTOs
- `utils/` — utilities placeholder

## Error Handling

Unified JSON error body via `GlobalExceptionHandler` with HTTP status, message, path, timestamp. Common cases: `400` validation, `401` bad credentials (returns specific reason and password hint if available) or expired JWT, `403` refresh token invalid, `409` duplicate username, `500` fallback.
