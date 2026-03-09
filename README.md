# Arthabit

A microservices-based smart expense tracking application with AI-powered bank SMS parsing, built with Spring Boot, Flask, Kotlin (Jetpack Compose), Apache Kafka, and Google Gemini.

---

## Table of Contents

- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Tech Stack](#tech-stack)
- [Services](#services)
  - [AuthService](#authservice--authentication--authorization)
  - [UserService](#userservice--user-profile-management)
  - [ExpenseService](#expenseservice--expense-management)
  - [DsService](#dsservice--ai-powered-sms-parsing)
  - [Arthabit Android App](#arthabit-android-app--kotlin-frontend)
- [API Reference](#api-reference)
  - [Auth Service Endpoints](#auth-service-endpoints)
  - [User Service Endpoints](#user-service-endpoints)
  - [Expense Service Endpoints](#expense-service-endpoints)
  - [DS Service Endpoints](#ds-service-endpoints)
- [Database Schemas](#database-schemas)
- [Event-Driven Communication (Kafka)](#event-driven-communication-kafka)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Running with Docker Compose](#running-with-docker-compose)
  - [Running Services Individually](#running-services-individually)
  - [Running the Android App](#running-the-android-app)
- [Environment Variables](#environment-variables)
- [Project Structure](#project-structure)

---

## Overview

**Arthabit** is a full-stack expense tracking platform that lets users:

- **Sign up and authenticate** with JWT-based security (access + refresh tokens)
- **Send bank SMS messages** to an AI service that automatically extracts expense details (amount, merchant, currency) using **Google Gemini 2.5 Flash Lite**
- **Track expenses** on a native Android app with a clean, neo-brutalist UI
- **Manage user profiles** with customizable personal information

The system is composed of four backend microservices and a native Android client. All backend services are containerized with Docker and orchestrated via Docker Compose. Services communicate asynchronously through Apache Kafka for event-driven workflows and synchronously via REST APIs for client-facing operations.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     Arthabit — Android App (Kotlin / Jetpack Compose)       │
└──────┬──────────────────┬────────────────┬──────────────────┬───────────────┘
       │ REST             │ REST           │ REST             │ REST
       ▼                  ▼                ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌───────────────┐  ┌──────────────┐
│ Auth Service │  │ User Service │  │Expense Service│  │  DS Service  │
│ Spring Boot  │  │ Spring Boot  │  │ Spring Boot   │  │ Flask + AI   │
│  Java 21     │  │  Java 21     │  │  Java 21      │  │ Python 3.11  │
│    :9898     │  │    :9810     │  │    :9820      │  │    :8010     │
└──────┬───────┘  └──────▲───────┘  └──────▲────────┘  └──────┬───────┘
       │                 │                 │                  │
       │  Kafka          │ Kafka           │  Kafka           │
       │ (user_service)  │                 │ (expense_service)│
       └─────────────────┘                 └──────────────────┘
                              │
                     ┌────────▼────────┐
                     │   MySQL 8.3.0   │
                     │     :3306       │
                     └─────────────────┘
                     ┌─────────────────┐
                     │  Apache Kafka   │
                     │  + Zookeeper    │
                     │  :9092 / :2181  │
                     └─────────────────┘
```

### Communication Patterns

| Pattern | From | To | Mechanism | Topic / Details |
|---------|------|----|-----------|-----------------|
| User sync on registration | Auth Service | User Service | Kafka | `user_service` — publishes `UserInfoEvent` on signup |
| AI-extracted expense ingestion | DS Service | Expense Service | Kafka | `expense_service` — publishes parsed expense data |
| Client authentication | Android App | Auth Service | REST | Login, signup, token refresh, ping |
| Expense operations | Android App | Expense Service | REST | Add/get expenses (with `X-User-Id` header) |
| Profile retrieval | Android App | User Service | REST | Get/update user profile |
| SMS processing | Android App | DS Service | REST | Send bank SMS for AI parsing |

### Data Ownership Boundaries

| Field | Owned By | Writable via User Service REST? |
|-------|----------|---------------------------------|
| `userId`, `email`, `phoneNumber` | Auth Service | No (synced via Kafka only) |
| `firstName`, `lastName`, `profilePic` | User Service | Yes |

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Android App** | Kotlin + Jetpack Compose | Kotlin 2.0.21 / Compose BOM 2024.12.01 |
| **DI** | Dagger Hilt | 2.53.1 |
| **Networking** | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| **Navigation** | Jetpack Navigation Compose | 2.8.5 |
| **Local Storage** | DataStore Preferences | 1.1.1 |
| **Backend** | Spring Boot (Java 21) | 3.3.5 / 3.2.2 / 3.5.10 |
| **AI Service** | Flask + LangChain | Flask 3.x |
| **LLM** | Google Gemini 2.5 Flash Lite | via langchain-google-genai |
| **Database** | MySQL | 8.3.0 |
| **Message Broker** | Apache Kafka (Confluent) | 7.4.4 |
| **Authentication** | JWT (jjwt) | 0.12.6 |
| **ORM** | Spring Data JPA / Hibernate | — |
| **Containerization** | Docker + Docker Compose | — |
| **Build (Java)** | Gradle | — |
| **Build (Android)** | Gradle (Kotlin DSL) / AGP | 8.13.2 |

---

## Services

### Auth Service — Authentication & Authorization

**Port:** `9898` · **Framework:** Spring Boot 3.3.5 · **Language:** Java 21

Handles user registration, login, JWT token generation/validation, and refresh token management. Issues short-lived **access tokens** (JWT, HS256) and long-lived **refresh tokens** (UUID). On signup, publishes a `UserInfoEvent` to the Kafka `user_service` topic so downstream services can react accordingly.

**Key Capabilities:**
- User signup with BCrypt password hashing and default `ROLE_USER` assignment
- Username/password authentication via Spring Security `AuthenticationManager`
- JWT access token generation and validation (via `JwtAuthFilter`)
- Refresh token lifecycle management (creation, verification, expiry)
- Stateless session management (`SessionCreationPolicy.STATELESS`)
- Fire-and-forget Kafka publishing (signup events don't block on Kafka failures)
- Swagger UI available at `/swagger-ui.html`

**Key Dependencies:** Spring Security 6, Spring Data JPA, Spring Kafka, jjwt 0.12.6, MySQL Connector, Lombok, SpringDoc OpenAPI 2.6.0, JTE 3.1.16, Google Guava 33.5

**Database:** `auth_service_db` — 3 tables (`users`, `roles`, `tokens`) + `user_roles` join table

---

### User Service — User Profile Management

**Port:** `9810` · **Framework:** Spring Boot 3.2.2 · **Language:** Java 21

Manages user profile data. Serves two primary functions:
1. **Consumes Kafka events** from Auth Service to automatically create/update user records on registration (upsert pattern)
2. **Exposes REST APIs** for fetching and updating profile-specific fields (`firstName`, `lastName`, `profilePic`)

Fields like `userId`, `email`, and `phoneNumber` are owned by Auth Service and are only written via Kafka event consumption.

**Key Dependencies:** Spring Data JPA, Spring Kafka, Jackson, MySQL Connector, Lombok, SpringDoc OpenAPI 2.3.0

**Database:** `user_service` — `users` table with columns: `id`, `user_id`, `first_name`, `last_name`, `phone_number`, `email`, `profile_pic`

---

### Expense Service — Expense Management

**Port:** `9820` · **Framework:** Spring Boot 3.5.10 · **Language:** Java 21

CRUD service for expense records. Provides REST APIs to create and retrieve expenses. Includes a Kafka consumer setup for event-driven expense creation from the DS Service (currently WIP — listener is commented out).

Each expense is assigned a public-facing `external_id` (UUID) via `@PrePersist`, keeping internal database IDs unexposed.

**Key Dependencies:** Spring Data JPA, Spring Kafka, MySQL Connector, Lombok

**Database:** `expense_service` — `expense` table with columns: `id`, `external_id` (UUID), `user_id`, `amount`, `merchant`, `currency` (default: `"inr"`), `created_at`

---

### DS Service — AI-Powered SMS Parsing

**Port:** `8010` · **Framework:** Flask · **Language:** Python 3.11

The intelligence layer of Arthabit. Accepts raw bank SMS text, filters it using regex-based keyword heuristics, then sends qualifying messages to **Google Gemini 2.5 Flash Lite** via LangChain for structured extraction. The extracted expense data is published to the Kafka `expense_service` topic.

**AI Pipeline:**

```
Bank SMS Text
     │
     ▼
┌─────────────────────────────────┐
│  Regex Filter (messagesUtil)    │  Keywords: "spent", "bank", "card"
│  Is it a bank SMS?              │  If NO → return 400 error
└──────────────┬──────────────────┘
               │ YES
               ▼
┌─────────────────────────────────┐
│  LLM Service (llmService)       │  Google Gemini 2.5 Flash Lite
│  ChatPromptTemplate → LLM       │  via LangChain
│  with_structured_output(Expense)│  Forces Pydantic schema output
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│  Expense (Pydantic Model)       │  { amount, merchant, currency }
│  + user_id from x-user-id header│
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│  Kafka Producer                 │  Topic: "expense_service"
│  JSON-encoded message           │
└─────────────────────────────────┘
```

**Why an LLM instead of regex?** Bank SMS formats vary wildly across banks and countries. The LLM generalizes across formats it has never seen, while `with_structured_output` guarantees a clean, validated response.

**Key Dependencies:** LangChain Core, langchain-google-genai, Pydantic, kafka-python, Flask, python-dotenv, Gunicorn

---

### Arthabit Android App — Kotlin Frontend

**Module:** `AppFrontend/kotlin_app` · **Language:** Kotlin 2.0.21 · **Min SDK:** Android 9 (API 28)

Native Android client built with **Jetpack Compose**, **Hilt**, and **Retrofit**. Follows **Clean Architecture** (UI → Domain → Data layers) with the **MVVM** pattern and unidirectional data flow via `StateFlow`.

**Screens:**

| Screen | Description |
|--------|-------------|
| **Login** | Username/password auth with automatic session restoration (ping → refresh → manual login fallback) |
| **Signup** | Full registration form (first name, last name, username, email, password, phone) |
| **Home** | Recent expenses in a lazy list with FAB to add new expenses via bottom sheet |
| **Profile** | User info display (name, email, phone), settings placeholders, logout |

**Key Features:**
- **Auto-login flow:** On launch, validates stored access token via ping, falls back to refresh token, then shows login form
- **Token persistence:** JWT access token, refresh token, and userId stored in Jetpack DataStore Preferences
- **Neo-brutalist UI:** Custom `CustomBox` composable with solid black shadow offset, bold borders, and rounded corners
- **Per-service Retrofit instances:** Separate named `@Qualifier` Retrofit clients for Auth, User, and Expense services
- **Currency toggle:** INR / USD selection in the Add Expense bottom sheet

**Backend Integration:**

| Service | Base URL | Purpose |
|---------|----------|---------|
| Auth Service | `http://<host>:9898` | Login, signup, token refresh, ping |
| User Service | `http://<host>:9810` | User profile retrieval |
| Expense Service | `http://<host>:9820` | Expense CRUD |
| DS Service | `http://<host>:8010` | Configured but not yet consumed in-app |

> **Note:** `10.0.2.2` is used as the default host (Android emulator alias for host machine localhost). For physical devices, update `ApiConfig.kt` with the machine's LAN IP.

**Key Dependencies:** Jetpack Compose (BOM 2024.12.01), Dagger Hilt 2.53.1, Retrofit 2.11.0, OkHttp 4.12.0, Navigation Compose 2.8.5, DataStore 1.1.1, Coil 2.7.0, Kotlin Coroutines 1.9.0

---

## API Reference

### Auth Service Endpoints

**Base URL:** `http://localhost:9898` · **Swagger UI:** `http://localhost:9898/swagger-ui.html`

#### Public Endpoints (No Auth Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/auth/v1/signup` | Register a new user |
| `POST` | `/auth/v1/login` | Authenticate and receive JWT + refresh token |
| `POST` | `/auth/v1/refreshToken` | Issue a new access token from a valid refresh token |
| `GET` | `/health` | Health check (returns `true`) |

#### Protected Endpoints (JWT Required)

| Method | Endpoint | Headers | Description |
|--------|----------|---------|-------------|
| `GET` | `/auth/v1/ping` | `Authorization: Bearer <token>` | Verify token validity, returns user ID |

**Signup Request:**
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

**Auth Response (`login` / `signup` / `refreshToken`):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "token": "a8f5f167-f44f-4598-87b9-c89a5d5f27a8",
  "userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Error Response Format:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid username or password",
  "path": "/auth/v1/login",
  "timestamp": "2026-03-09 14:30:00"
}
```

| Status | Condition |
|--------|-----------|
| `400` | Missing/invalid fields or malformed JSON |
| `401` | Invalid credentials, expired/tampered JWT |
| `403` | Refresh token expired or not found |
| `409` | Username already exists |

**Token Details:**

| Token | Mechanism | Lifetime | Storage |
|-------|-----------|----------|---------|
| Access Token | JWT (HS256) | ~100 minutes | Client-side |
| Refresh Token | UUID string | ~100 minutes | `tokens` table (DB) |

---

### User Service Endpoints

**Base URL:** `http://localhost:9810` · **Swagger UI:** `http://localhost:9810/swagger-ui.html`

| Method | Endpoint | Params | Description |
|--------|----------|--------|-------------|
| `GET` | `/user/v1/getUser` | `?userId={userId}` | Get user profile (200 OK / 404 Not Found) |
| `PUT` | `/user/v1/updateProfile` | `?userId={userId}` | Update profile fields (firstName, lastName, profilePic) |
| `GET` | `/health` | — | Health check (returns `true`) |

**Get User Response:**
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

**Update Profile Request** (null fields are ignored):
```json
{
  "first_name": "Jane",
  "last_name": "Smith",
  "profile_pic": "https://example.com/new-pic.jpg"
}
```

---

### Expense Service Endpoints

**Base URL:** `http://localhost:9820`

| Method | Endpoint | Headers | Description |
|--------|----------|---------|-------------|
| `GET` | `/expense/v1/getExpense` | `X-User-Id: <userId>` | Get all expenses for user (200 OK) |
| `POST` | `/expense/v1/addExpense` | `X-User-Id: <userId>` | Create a new expense (201 Created) |

**Add Expense Request:**
```json
{
  "amount": 150.00,
  "merchant": "Amazon",
  "currency": "usd"
}
```

**Expense Response:**
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

### DS Service Endpoints

**Base URL:** `http://localhost:8010`

| Method | Endpoint | Headers | Body | Description |
|--------|----------|---------|------|-------------|
| `POST` | `/v1/ds/message` | `x-user-id: <userId>` | `{"message": "..."}` | Process bank SMS, extract expense, publish to Kafka |
| `GET` | `/` | — | — | Sanity check (returns `"Hello world"`) |
| `GET` | `/health` | — | — | Health probe (returns `"OK"`) |

**Example Request:**
```bash
curl -X POST http://localhost:8010/v1/ds/message \
  -H "Content-Type: application/json" \
  -H "x-user-id: user123" \
  -d '{"message": "Rs 500 spent at Amazon using HDFC credit card"}'
```

**Success Response:**
```json
{
  "amount": "500",
  "merchant": "Amazon",
  "currency": "Rs",
  "user_id": "user123"
}
```

---

## Database Schemas

All backend services share a single MySQL 8.3.0 instance with separate databases. Schema is auto-managed by Hibernate (`ddl-auto=update`).

### `auth_service_db`

| Table | Column | Type | Constraints | Description |
|-------|--------|------|-------------|-------------|
| **users** | `user_id` | VARCHAR | PK (UUID) | Unique user identifier |
| | `username` | VARCHAR | NOT NULL | Login username |
| | `password` | VARCHAR | NOT NULL | BCrypt hash |
| | `email` | VARCHAR | NOT NULL | Email address |
| | `phone_number` | BIGINT | NOT NULL | Phone number |
| **roles** | `role_id` | BIGINT | PK, auto-increment | Role identifier |
| | `name` | VARCHAR | — | e.g. `ROLE_USER` |
| **user_roles** | `user_id` | VARCHAR | FK → users | M:N join table |
| | `role_id` | BIGINT | FK → roles | |
| **tokens** | `id` | INT | PK, auto-increment | Token identifier |
| | `token` | VARCHAR | — | UUID refresh token |
| | `expiry_date` | DATETIME | — | Expiration timestamp |
| | `user_id` | VARCHAR | FK → users | Token owner |

### `user_service`

| Table | Column | Type | Constraints | Description |
|-------|--------|------|-------------|-------------|
| **users** | `id` | BIGINT | PK, auto-increment | Internal surrogate key |
| | `user_id` | VARCHAR | UNIQUE, NOT NULL | From Auth Service |
| | `first_name` | VARCHAR | NOT NULL | User's first name |
| | `last_name` | VARCHAR | NOT NULL | User's last name |
| | `phone_number` | BIGINT | NOT NULL | Phone number |
| | `email` | VARCHAR | NOT NULL | Email address |
| | `profile_pic` | VARCHAR | nullable | Profile picture URL |

### `expense_service`

| Table | Column | Type | Constraints | Description |
|-------|--------|------|-------------|-------------|
| **expense** | `id` | BIGINT | PK, auto-increment | Internal database ID |
| | `external_id` | VARCHAR | UUID, auto-generated | Public-facing identifier |
| | `user_id` | VARCHAR | — | Expense owner |
| | `amount` | DECIMAL | — | Expense amount |
| | `merchant` | VARCHAR | — | Merchant/vendor name |
| | `currency` | VARCHAR | default `"inr"` | Currency code |
| | `created_at` | TIMESTAMP | default current time | Creation timestamp |

---

## Event-Driven Communication (Kafka)

### Topics

| Topic | Producer | Consumer | Payload |
|-------|----------|----------|---------|
| `user_service` | Auth Service | User Service | `UserInfoEvent` |
| `expense_service` | DS Service | Expense Service (WIP) | Expense JSON |

### `user_service` Topic

Published on every successful signup. Fire-and-forget pattern — Kafka failures do not block user registration.

```json
{
  "first_name": "John",
  "last_name": "Doe",
  "email": "john@example.com",
  "phone_number": 9876543210,
  "user_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

| Property | Value |
|----------|-------|
| Key Serializer | `StringSerializer` |
| Value Serializer | Custom `UserInfoSerializer` (JSON) |
| Acks | `all` |
| Retries | `3` |

### `expense_service` Topic

Published by DS Service after successfully extracting expense data from bank SMS via LLM.

```json
{
  "amount": "500",
  "merchant": "Amazon",
  "currency": "Rs",
  "user_id": "user123"
}
```

| Property | Value |
|----------|-------|
| Serialization | `json.dumps().encode('utf-8')` |
| Consumer Group | `expense-info-consumer-group` |
| Consumer Status | WIP (`@KafkaListener` currently commented out) |

---

## Getting Started

### Prerequisites

- **Docker** & **Docker Compose**
- **Java 21** (Eclipse Temurin recommended, for local development)
- **Python 3.11+** (for DS Service local development)
- **Android Studio** Ladybug or newer (for the Android app)
- **Android SDK** API 35 (compileSdk), API 28+ device/emulator
- A **Google API Key** with Gemini API access

### Running with Docker Compose

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd Expense_Tracker_App
   ```

2. **Build all Java service JARs:**
   ```bash
   cd AuthService && ./gradlew bootJar && cd ..
   cd UserService && ./gradlew bootJar && cd ..
   cd ExpenseService && ./gradlew bootJar && cd ..
   ```

3. **Set your Google API Key:**
   ```bash
   export GOOGLE_API_KEY="your-gemini-api-key"
   ```
   The `services.yml` references `${GOOGLE_API_KEY}` from the environment.

4. **Start all services:**
   ```bash
   docker compose -f services.yml up --build
   ```
   This spins up: Zookeeper, Kafka, MySQL 8.3.0, Auth Service, User Service, Expense Service, and DS Service.

5. **Verify services are running:**
   ```bash
   curl http://localhost:9898/health   # Auth Service
   curl http://localhost:9810/health   # User Service
   curl http://localhost:9820/health   # Expense Service
   curl http://localhost:8010/health   # DS Service
   ```

**Service Startup Order (handled by Docker Compose):**
1. Zookeeper → Kafka → MySQL (infrastructure)
2. Auth Service (depends on Kafka + MySQL)
3. User Service (depends on Kafka + MySQL + Auth Service)
4. Expense Service (depends on Kafka + MySQL)
5. DS Service (depends on Kafka + MySQL + Expense Service + User Service)

### Running Services Individually

Each service can be run standalone for local development:

```bash
# Auth Service (requires MySQL on localhost:3306, Kafka on localhost:9092)
cd AuthService && ./gradlew bootRun    # http://localhost:9898

# User Service
cd UserService && ./gradlew bootRun    # http://localhost:9810

# Expense Service
cd ExpenseService && ./gradlew bootRun # http://localhost:9820

# DS Service (requires Kafka, Google API Key in .env file)
cd DsService
source dsenv/bin/activate
pip install -e .
echo "GOOGLE_API_KEY=your-key" > .env
flask --app src/app run --port 8010    # http://localhost:8010
```

### Running the Android App

1. Open `AppFrontend/kotlin_app/` in **Android Studio**.
2. Sync Gradle (automatic on first open).
3. Ensure backend services are running (Docker Compose or individually).
4. Select a device/emulator (API 28+).
5. Click **Run** or press `Shift + F10`.

> **Emulator:** Uses `10.0.2.2` as the default host (Android emulator alias for host machine localhost).
> **Physical device:** Update `HOST` in `ApiConfig.kt` to your machine's LAN IP address.

---

## Environment Variables

### Java Services (Auth, User, Expense)

| Variable | Default | Used By | Description |
|----------|---------|---------|-------------|
| `MYSQL_HOST` | `localhost` | All | MySQL server hostname |
| `MYSQL_PORT` | `3306` | All | MySQL server port |
| `MYSQL_DB` | varies | All | Database name (`auth_service_db`, `user_service`, `expense_service`) |
| `KAFKA_HOST` | `localhost` | All | Kafka broker hostname |
| `KAFKA_PORT` | `9092` | All | Kafka broker port |

### DS Service (Python)

| Variable | Default | Description |
|----------|---------|-------------|
| `KAFKA_HOST` | `localhost` | Kafka broker hostname |
| `KAFKA_PORT` | `9092` | Kafka broker port |
| `GOOGLE_API_KEY` | — (required) | API key for Google Gemini |

### Docker Compose (`services.yml`)

All environment variables are set automatically in `services.yml`. The only external variable you need to provide is `GOOGLE_API_KEY`.

---

## Project Structure

```
Expense_Tracker_App/
├── services.yml                     # Docker Compose — all services + infrastructure
├── README.md                        # This file
│
├── AuthService/                     # Authentication & authorization microservice
│   ├── src/main/java/com/nstrange/authservice/
│   │   ├── auth/                    #   JWT filter, security config
│   │   ├── controller/              #   REST controllers
│   │   ├── entities/                #   JPA entities (User, Role, RefreshToken)
│   │   ├── eventProducer/           #   Kafka producer (UserInfoEvent)
│   │   ├── exception/               #   Global exception handling
│   │   ├── service/                 #   JWT, auth, refresh token services
│   │   └── ...
│   ├── build.gradle
│   ├── Dockerfile
│   └── README.md                    #   Detailed service documentation
│
├── UserService/                     # User profile management microservice
│   ├── src/main/java/com/nstrange/userservice/
│   │   ├── consumer/               #   Kafka consumer + REST controller
│   │   ├── entities/                #   JPA entity + DTOs
│   │   ├── service/                 #   Business logic
│   │   └── ...
│   ├── build.gradle
│   ├── Dockerfile
│   └── README.md
│
├── ExpenseService/                  # Expense CRUD microservice
│   ├── src/main/java/com/nstrange/expenseservice/
│   │   ├── consumer/               #   Kafka consumer (WIP)
│   │   ├── controller/             #   REST controller
│   │   ├── dto/                    #   Request/response DTOs
│   │   ├── service/                #   Business logic
│   │   └── ...
│   ├── build.gradle
│   ├── Dockerfile
│   └── README.md
│
├── DsService/                       # AI/ML data science service
│   ├── src/app/
│   │   ├── __init__.py             #   Flask app + routes + Kafka producer
│   │   ├── service/
│   │   │   ├── messageService.py   #   Orchestrator (filter → LLM)
│   │   │   ├── llmService.py       #   Google Gemini integration
│   │   │   └── Expense.py          #   Pydantic model
│   │   └── utils/
│   │       └── messagesUtil.py     #   Bank SMS regex filter
│   ├── requirements.txt
│   ├── Dockerfile
│   └── README.md
│
└── AppFrontend/
    └── kotlin_app/                  # Native Android app (Kotlin / Jetpack Compose)
        ├── app/src/main/java/com/nstrange/arthabit/
        │   ├── data/               #   Retrofit APIs, DTOs, repositories, TokenManager
        │   ├── di/                 #   Hilt modules (Network, Repository)
        │   ├── domain/             #   Repository interfaces, domain models
        │   ├── navigation/         #   NavGraph, Routes
        │   ├── ui/                 #   Screens, ViewModels, components, theme
        │   └── util/               #   Resource sealed class
        ├── build.gradle.kts
        └── README.md
```

---

## Screenshots

<!-- Add screenshots here -->
<!-- ![Login](screenshots/login.png) -->
<!-- ![Home](screenshots/home.png) -->
<!-- ![Profile](screenshots/profile.png) -->

---

## License

This project is for educational and portfolio purposes.
