# 🔐 AuthService — Authentication & Authorization Microservice

Part of the Arthabit Expense Tracker application. This service is responsible for handling user registration, authentication, JWT issuance and validation, refresh token management, and publishing user events to a Kafka message broker.

## 🚀 Features

- **Stateless Authentication**: Uses JSON Web Tokens (JWT) for secure, stateless API communication.
- **Refresh Token Mechanism**: Secure token rotation using database-backed refresh tokens.
- **Event-Driven Architecture**: Publishes user creation events to Kafka for downstream microservices.
- **Robust Security**: Passwords are cryptographically hashed using BCrypt.
- **Containerized**: Ready to be deployed via Docker.

## 🛠️ Tech Stack

- **Java 21**, **Spring Boot 3.3.x**, **Spring Security 6**
- **Database**: MySQL 8+ with Spring Data JPA & Hibernate
- **Messaging**: Apache Kafka
- **Build Tool**: Gradle
- **Deployment**: Docker

---

## 🏃‍♂️ Getting Started

### Prerequisites
- Java 21 JDK
- MySQL 8+
- Apache Kafka (Local or Dockerized)
- Docker (Optional, for containerized deployment)

### Running Locally

You can run the application directly using the Gradle wrapper:

```bash
# Start the application
./gradlew bootRun

# Build an executable JAR
./gradlew bootJar
```

### Running with Docker

Build the Docker image and run it by injecting the required environment variables:

```bash
# Build the image
docker build -t auth-service .

# Run the container
docker run -p 9898:9898 \
  -e MYSQL_HOST=host.docker.internal \
  -e MYSQL_PORT=3306 \
  -e MYSQL_DB=auth_service_db \
  -e KAFKA_HOST=host.docker.internal \
  -e KAFKA_PORT=9092 \
  auth-service
```

---

## ⚙️ Configuration

The application is configured via `src/main/resources/application.properties`. You can override these using environment variables:

| Environment Variable / Property | Default Value | Description |
|---|---|---|
| `SERVER_PORT` (`server.port`) | `9898` | The HTTP port the service listens on. |
| `MYSQL_HOST` | `localhost` | MySQL Database host. |
| `MYSQL_PORT` | `3306` | MySQL Database port. |
| `MYSQL_DB` | `auth_service_db` | MySQL Database name. |
| `SPRING_DATASOURCE_USERNAME` | `root` | Database user. |
| `SPRING_DATASOURCE_PASSWORD` | `narvar007` | Database password. |
| `KAFKA_HOST` | `localhost` | Kafka broker host. |
| `KAFKA_PORT` | `9092` | Kafka broker port. |
| `SPRING_KAFKA_TOPIC_JSON_NAME` | `user_service` | Topic for user signup events. |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | Database schema management strategy. |

---

## 📡 API Endpoints

**Local Base URL:** `http://localhost:9898`  
**Public AWS Base URL:** `http://arthabit-api.sakshamnarvar.tech/auth-service`

| Method | Endpoint | Description | Auth Required? |
|---|---|---|---|
| `POST` | `/auth/v1/signup` | Register a new user. Returns `{ accessToken, token, userId }`. | ❌ No |
| `POST` | `/auth/v1/login` | Authenticate an existing user. Returns `{ accessToken, token, userId }`. | ❌ No |
| `POST` | `/auth/v1/refreshToken` | Get a new access token using a valid refresh token. | ❌ No |
| `GET`  | `/auth/v1/ping` | Verify an access token. Returns the valid `userId`. | 🔑 Yes |
| `GET`  | `/health` | Health check probe. | ❌ No |

*Note: Payloads generally use `snake_case` JSON fields (e.g., `first_name`, `phone_number`).*

---

## 🏗️ Architecture & Internals

### Typical Workflow

1. **Signup**: User data is validated, the password is encrypted, and the user is saved with a default `ROLE_USER`. A refresh token is generated, and a `UserInfoEvent` is published to Kafka (fire-and-forget).
2. **Access token**: Access tokens are valid for ~100 minutes (HS256).
3. **Refresh token**: Persisted in the database. Operates on a one-to-one relationship with the user.

### Data Model 

Tables are auto-updated via Hibernate DDL:
- **`users`**: `user_id` (PK, UUID), `username`, `password`, `password_hint`, `email`, `phone_number`.
- **`roles`**: `role_id` (PK), `name`.
- **`user_roles`**: Join table mapping users to their roles.
- **`tokens`**: `id` (PK), `token`, `expiry_date`, `user_id` (FK to `users`).

### Kafka Integration

- **Topic**: `user_service` 
- **Serialization**: `StringSerializer` for keys, `JsonSerializer` for values.
- **Resilience**: Configured with `acks=all` and `3` retries.
- **Sample Payload**:
  ```json
  {
    "username": "john007",
    "first_name": "John",
    "last_name": "Doe",
    "email": "john@example.com",
    "phone_number": "+919876543210",
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "account_creation_date": "2026-04-16T10:15:30.000+00:00"
  }
  ```

---

## 📁 Project Structure

Provide a quick glance into the project boundaries:

- `com.nstrange.authservice.controller` - HTTP endpoint definitions.
- `com.nstrange.authservice.config` / `auth` - Spring Security configs and JWT mapping.
- `com.nstrange.authservice.service` - Core business logic and token workflows.
- `com.nstrange.authservice.repository` - Data access layer (Spring Data JPA).
- `com.nstrange.authservice.entities` - JPA Entity definitions.
- `com.nstrange.authservice.model` / `request` / `response` - Data Transfer Objects (DTOs).
- `com.nstrange.authservice.eventProducer` - Kafka publishing logic.
- `com.nstrange.authservice.exception` - Global exception handlers.

---

## ⚠️ Error Handling

The service implements a unified JSON error format governed by `GlobalExceptionHandler`. Example HTTP status codes:
- `400 Bad Request`: Form validation failures.
- `401 Unauthorized`: Invalid credentials (often returns a password hint via `InvalidCredentialsException`) or expired JWT.
- `403 Forbidden`: Refresh token has expired or is unrecognized.
- `409 Conflict`: Attempting to sign up with an existing username.
- `500 Internal Server Error`: Unhandled exceptions.
