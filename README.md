# Arthabit - Smart Expense Tracker

A microservices-based expense tracking application with AI-powered bank SMS parsing. Built with Spring Boot, Flask, Kotlin (Jetpack Compose), Apache Kafka, and Google Gemini.

## Architecture & Tech Stack
- **Android App (Frontend)**: Kotlin, Jetpack Compose, MVVM, clean architecture, Hilt, Retrofit.
- **Backend Services**: Java 21, Spring Boot 3.x, MySQL 8, Kafka.
- **AI Service (DS)**: Python, Flask, LangChain, Google Gemini 2.5 Flash Lite.
- **Infrastructure**: Docker Compose, Zookeeper.

## Services Overview

### 1. Auth Service (`:9898`)
- Handles signup, login, JWT (HS256) issuance/validation, and refresh tokens.
- Publishes `UserInfoEvent` to Kafka (`user_service` topic) on signup (fire-and-forget).
- Database: `auth_service_db` (users, roles, tokens).

### 2. User Service (`:9810`)
- Manages user profiles (`firstName`, `lastName`, `profilePic`).
- Consumes Kafka events from Auth Service for auto-creating user records. 
- Database: `user_service`. Fields like `email` and `phoneNumber` are read-only here (owned by Auth).

### 3. Expense Service (`:9820`)
- CRUD service for expense records.
- Consumes AI-parsed structured expenses from Kafka (`expense_service` topic) to persist data.
- Database: `expense_service`. Assigns public UUIDs (`external_id`) to expenses. Defaults currency to `INR`.

### 4. DS Service (`:8010`)
- AI-powered SMS parser (Python/Flask). 
- Regex filtration -> LLM Extraction (Gemini) -> Kafka Producer (`expense_service` topic).
- Requires `GOOGLE_API_KEY`.

### 5. Arthabit Android App (Frontend)
- Located in `AppFrontend/kotlin_app`. Native client using Neo-brutalist UI.
- Features: Auto-login flow, JWT persistence (DataStore), currency toggles.
- Communicates directly with backend REST APIs via per-service Retrofit instances. Use `10.0.2.2` for emulators or LAN IP for physical device testing.

## Running the Project

### Prerequisites
- Docker & Docker Compose
- Java 21 & Python 3.11+ (for local development)
- Android Studio & SDK API 35
- Google Gemini API Key

### 1. Build and Run Backend (Docker Compose)
Set your API key and spin up the environment (MySQL, Kafka, and 4 microservices):
```bash
export GOOGLE_API_KEY="your-gemini-api-key"
docker compose -f services.yml up --build
```
*Note: Auth, User, and Expense services use Gradle (`bootJar`). The DS service uses Python/Flask.*

### 2. Run Android App
1. Open `AppFrontend/kotlin_app/` in Android Studio.
2. Sync Gradle and run on an emulator (API 28+).

## API Integration Highlights
- **Auth**: `POST /auth/v1/signup`, `POST /auth/v1/login`, `POST /auth/v1/refreshToken`
- **User**: `GET /user/v1/users/{userId}`, `PUT /user/v1/users/{userId}` (Updates profile info)
- **Expense**: `GET /expense/v1/getExpense`, `POST /expense/v1/addExpense` (Headers: `X-User-Id`)
- **DS**: `POST /v1/ds/message` (Extracts expense JSON from raw SMS via Gemini)

## Event-Driven Flow (Kafka)
- **`user_service` topic**: Auth Service (Producer) -> User Service (Consumer). Triggers profile creation on signup.
- **`expense_service` topic**: DS Service (Producer) -> Expense Service (Consumer). Persists AI-parsed SMS expenses.
