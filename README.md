# Arthabit

A microservices-based smart expense tracking application with AI-powered bank SMS parsing, built with Spring Boot, Flask, React Native, Apache Kafka, and Google Gemini.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Services](#services)
  - [AuthService](#authservice)
  - [UserService](#userservice)
  - [ExpenseService](#expenseservice)
  - [DsService (AI/ML)](#dsservice-aiml)
  - [AppFrontend (Mobile)](#appfrontend-mobile)
- [API Reference](#api-reference)
- [Database Schema](#database-schema)
- [Event-Driven Communication](#event-driven-communication)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Running with Docker Compose](#running-with-docker-compose)
  - [Running the Mobile App](#running-the-mobile-app)
- [Project Structure](#project-structure)
- [Screenshots](#screenshots)
- [License](#license)

---

## Overview

**Arthabit** is a full-stack expense tracking platform that lets users:

- **Sign up and authenticate** with JWT-based security (access + refresh tokens)
- **Send bank SMS notifications** to an AI service that automatically extracts expense details (amount, merchant, currency) using **Google Gemini 2.5 Flash Lite**
- **Track and visualize expenses** on a cross-platform mobile app with animated charts and spending insights
- **Manage profiles** with customizable settings

All backend services are containerized with Docker and communicate asynchronously through Apache Kafka.

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                        Mobile App (React Native)                     │
│                     iOS & Android — TypeScript                       │
└──────┬──────────────┬──────────────┬──────────────┬──────────────────┘
       │ REST          │ REST          │ REST          │ REST
       ▼              ▼              ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ AuthService  │ │ UserService  │ │ExpenseService│ │  DsService   │
│ Spring Boot  │ │ Spring Boot  │ │ Spring Boot  │ │ Flask + AI   │
│    :9898     │ │    :9810     │ │    :9820     │ │    :8010     │
└──────┬───────┘ └──────▲───────┘ └──────▲───────┘ └──────┬───────┘
       │  Kafka          │ Kafka          │ Kafka          │
       │  (user_service) │               │(expense_service)│
       └────────────────┘               └────────────────┘
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

**Communication Patterns:**
| Pattern | From → To | Mechanism | Topic |
|---------|-----------|-----------|-------|
| User sync on registration | AuthService → UserService | Kafka | `user_service` |
| AI-extracted expense ingestion | DsService → ExpenseService | Kafka | `expense_service` |
| All client requests | Mobile App → Services | REST/HTTP | — |

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Mobile | React Native + TypeScript | 0.84.0 / 5.8.3 |
| Backend | Spring Boot (Java 21) | 3.3.5 / 3.2.2 / 3.5.10 |
| AI/ML Service | Flask + LangChain | Flask 3.1.2 |
| LLM | Google Gemini 2.5 Flash Lite | via langchain-google-genai 2.0.5 |
| Database | MySQL | 8.3.0 |
| Message Broker | Apache Kafka (Confluent) | 7.4.4 |
| Authentication | JWT (jjwt + Auth0 java-jwt) | 0.12.6 / 4.5.0 |
| ORM | Spring Data JPA / Hibernate | — |
| Containerization | Docker + Docker Compose | — |
| Build (Java) | Gradle | — |
| Build (JS) | Metro Bundler | — |
| Navigation | React Navigation | 7.x |

---

## Services

### AuthService

**Port:** 9898 · **Framework:** Spring Boot 3.3.5 · **Language:** Java 21

Handles user registration, login, JWT token generation/validation, and refresh token management. On signup, publishes a user-created event to Kafka for downstream services.

**Key Dependencies:** Spring Security, Spring Data JPA, Spring Kafka, jjwt, Auth0 java-jwt, MySQL Connector, Lombok

---

### UserService

**Port:** 9810 · **Framework:** Spring Boot 3.2.2 · **Language:** Java 21

Manages user profiles (name, email, phone, profile picture). Consumes Kafka events from AuthService to automatically create user profiles on registration.

**Key Dependencies:** Spring Data JPA, Spring Kafka, Jackson, MySQL Connector, Lombok

---

### ExpenseService

**Port:** 9820 · **Framework:** Spring Boot 3.5.10 · **Language:** Java 21

CRUD service for expense records. Consumes AI-extracted expense data from DsService via Kafka. Supports querying expenses by user and by date ranges.

**Key Dependencies:** Spring Data JPA, Spring Kafka, MySQL Connector, Lombok

---

### DsService (AI/ML)

**Port:** 8010 · **Framework:** Flask 3.1.2 · **Language:** Python 3.11

The intelligence layer of Arthabit. Accepts raw bank SMS text, filters it using regex-based heuristics, then sends it to **Google Gemini 2.5 Flash Lite** via LangChain for structured extraction. The extracted expense (amount, merchant, currency) is published to Kafka for ExpenseService to persist.

**AI Pipeline:**
1. Incoming SMS → regex filter (keywords: "spent", "bank", "card")
2. Filtered message → LangChain `ChatGoogleGenerativeAI` with structured output prompt
3. Pydantic model extraction → `{amount, merchant, currency}`
4. Kafka publish → topic `expense_service`

**Key Dependencies:** LangChain Core, langchain-google-genai, Pydantic, kafka-python, Gunicorn

---

### AppFrontend (Mobile)

**Framework:** React Native 0.84.0 · **Language:** TypeScript 5.8.3 · **Platforms:** iOS + Android

Cross-platform mobile app with the following screens:

| Screen | Description |
|--------|-------------|
| **Login** | Username/password auth with auto token validation and refresh |
| **Sign Up** | Full registration form |
| **Home** | Dashboard with animated circular expense graph, spending insights, and recent transactions |
| **Spends** | Detailed expense list fetched from ExpenseService |
| **Profile** | User info display with settings (notifications, privacy, dark mode) |

**Key Dependencies:** React Navigation, AsyncStorage, react-native-svg, react-native-gesture-handler, Lucide icons, GlueStack UI

---

## API Reference

### AuthService — `:9898`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/auth/v1/signup` | Public | Register a new user |
| `POST` | `/auth/v1/login` | Public | Login, returns JWT + refresh token |
| `POST` | `/auth/v1/refreshToken` | Public | Refresh an expired JWT |
| `GET` | `/auth/v1/ping` | Bearer | Validate token, returns user ID |
| `GET` | `/health` | Public | Health check |

### UserService — `:9810`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/user/v1/getUser` | Get user profile by user ID |
| `POST` | `/user/v1/createUpdate` | Create or update a user profile |
| `GET` | `/health` | Health check |

### ExpenseService — `:9820`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/expense/v1/getExpense?user_id=` | Get all expenses for a user |
| `POST` | `/expense/v1/addExpense` | Add expense (requires `X-User-Id` header) |

### DsService — `:8010`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1/ds/message` | Process bank SMS (requires `x-user-id` header) |
| `GET` | `/health` | Health check |

---

## Database Schema

All services share a single MySQL 8.3.0 instance with separate schemas:

**`auth_service_db`**
| Table | Key Columns |
|-------|-------------|
| `user_info` | id, username, password (BCrypt), roles |
| `user_role` | id, name |
| `refresh_token` | id, token, expiry_date, user_id |

**`user_service`**
| Table | Key Columns |
|-------|-------------|
| `user_info` | user_id, first_name, last_name, email, phone_number, profile_pic |

**`expense_service`**
| Table | Key Columns |
|-------|-------------|
| `expense` | id, external_id (UUID), user_id, amount, merchant, currency, created_at |

---

## Event-Driven Communication

| Kafka Topic | Producer | Consumer | Payload |
|-------------|----------|----------|---------|
| `user_service` | AuthService | UserService | `{ userId, firstName, lastName, email, phoneNumber }` |
| `expense_service` | DsService | ExpenseService | `{ amount, merchant, currency, user_id }` |

---

## Getting Started

### Prerequisites

- **Docker** & **Docker Compose**
- **Java 21** (for local development)
- **Python 3.11+** (for DsService local development)
- **Node.js >= 22.11.0** & **npm**
- **Xcode** (for iOS) / **Android Studio** (for Android)
- **CocoaPods** (for iOS)
- A **Google API Key** with Gemini API access

### Running with Docker Compose

1. **Clone the repository:**
   ```bash
   git clone git@github.com:<your-username>/Arthabit.git
   cd Arthabit
   ```

2. **Build all Java services:**
   ```bash
   cd AuthService && ./gradlew bootJar && cd ..
   cd UserService && ./gradlew bootJar && cd ..
   cd ExpenseService && ./gradlew bootJar && cd ..
   ```

3. **Set your Google API Key** in `services.yml` under `ds-service.environment.GOOGLE_API_KEY`.

4. **Start all services:**
   ```bash
   docker compose -f services.yml up --build
   ```

5. **Verify services are running:**
   ```bash
   curl http://localhost:9898/health   # AuthService
   curl http://localhost:9810/health   # UserService
   curl http://localhost:9820/health   # ExpenseService
   curl http://localhost:8010/health   # DsService
   ```

### Running the Mobile App

1. **Install dependencies:**
   ```bash
   cd AppFrontend/expenseTrackerApp
   npm install
   ```

2. **Install iOS pods:**
   ```bash
   cd ios && pod install && cd ..
   ```

3. **Run on iOS:**
   ```bash
   npx react-native run-ios
   ```

4. **Run on Android:**
   ```bash
   npx react-native run-android
   ```

> **Note:** The mobile app connects to `localhost` by default. For physical devices, update the service URLs in `src/app/config/apiConfig.ts` to your machine's local IP.

---

## Project Structure

```
Arthabit/
├── services.yml                 # Docker Compose — all services + infra
├── README.md
│
├── AuthService/                 # JWT authentication microservice
│   ├── src/main/java/...        #   Spring Boot (Java 21)
│   ├── build.gradle
│   └── Dockerfile
│
├── UserService/                 # User profile microservice
│   ├── src/main/java/...        #   Spring Boot (Java 21)
│   ├── build.gradle
│   └── Dockerfile
│
├── ExpenseService/              # Expense CRUD microservice
│   ├── src/main/java/...        #   Spring Boot (Java 21)
│   ├── build.gradle
│   └── Dockerfile
│
├── DsService/                   # AI/ML data science service
│   ├── src/app/                 #   Flask + LangChain + Gemini
│   ├── requirements.txt
│   └── Dockerfile
│
└── AppFrontend/
    └── expenseTrackerApp/       # React Native mobile app
        ├── src/app/             #   Pages, components, navigation
        ├── ios/                 #   iOS project (Xcode)
        ├── android/             #   Android project (Gradle)
        └── package.json
```

---

## Screenshots

<!-- Add screenshots of your app here -->
<!-- ![Login Screen](screenshots/login.png) -->
<!-- ![Dashboard](screenshots/dashboard.png) -->
<!-- ![Expense List](screenshots/spends.png) -->

---

## License

This project is for educational and portfolio purposes.
