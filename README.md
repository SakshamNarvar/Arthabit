# Arthabit - Smart Expense Tracker

A microservices-based expense tracking application with AI-powered bank SMS parsing. Built with Spring Boot, Python (Flask), Apache Kafka, and Nginx API Gateway. Deployed on **AWS EC2**.

*Note: The frontend client is maintained in a separate repository. Check out the [Arthabit Frontend Repo](https://github.com/SakshamNarvar/Arthabit_Expense_Tracker_App) for the mobile app.*

## Live Demo & Public Endpoints 🚀
The backend is currently deployed and functional on a public **AWS EC2** instance. Traffic is routed via an Nginx reverse proxy. You can interact with the live microservices using these base URLs:

- **Auth Service:** `https://arthabit-api.sakshamnarvar.tech/auth-service`
- **User Service:** `https://arthabit-api.sakshamnarvar.tech/user-service`
- **Expense Service:** `https://arthabit-api.sakshamnarvar.tech/expense-service`
- **DS (AI) Service:** `https://arthabit-api.sakshamnarvar.tech/ds-service`

> ⚠️ **Disclaimer:** *These live endpoints are provided primarily for portfolio/demonstration purposes. The services may be temporarily spun down at times to manage AWS infrastructure costs. Rate-limiting is strictly enforced on the AI parser endpoint to prevent abuse of Gemini API usage.*

## Architecture & Tech Stack
- **Backend Services**: Java 21, Spring Boot 3.x, MySQL 8.3.0, Kafka (KRaft mode).
- **AI Service (DS)**: Python, Flask, LangChain, Google Gemini 2.5 Flash Lite.
- **Gateway**: Nginx Reverse Proxy.
- **Infrastructure**: Docker Compose, AWS EC2.

## Services Overview

### 1. Auth Service (`:9898`)
- Handles stateless authentication (JWT), user registration, and refresh token rotation (database-backed).
- Publishes `UserInfoEvent` to Kafka (`user_service` topic) on signup for downstream services.
- Tech Stack: Java 21, Spring Boot 3.3.x, Spring Security 6.
- Database: `auth_service_db` (users, roles, tokens).

### 2. User Service (`:9810`)
- Manages user profiles (`firstName`, `lastName`, `profilePic`). Core identification fields are read-only (owned by Auth).
- Consumes Kafka events from the Auth Service for idempotently auto-creating user records.
- Tech Stack: Java 21, Spring Boot 3.2.
- Database: `user_service`.

### 3. Expense Service (`:9820`)
- CRUD service for storing and retrieving user expenses. Uses header `X-User-ID` for identity context.
- Consumes AI-parsed structured expenses from Kafka (`expense_service` topic) to persist data.
- Tech Stack: Java 21, Spring Boot 3.5.x.
- Database: `expense_service`. Defaults currency to `INR`. Generates a public UUID (`external_id`).

### 4. DS Service (`:8010`)
- AI-powered SMS parser using Python/Flask.
- Flow: Regex filtration -> LLM Extraction (Google Gemini) -> publishes structured entity to Kafka (`expense_service` topic).
- Requires `GOOGLE_API_KEY`.

### 5. Nginx (API Gateway) (`:80`)
- Serves as the single entry point for all upstream backend microservices (Auth, User, Expense, DS).
- Configured in the `nginx/` directory.

## Running the Project

### Prerequisites
- Docker & Docker Compose
- Java 21 & Python 3.9+ (for local development)
- Google Gemini API Key

### Build and Run Backend (Docker Compose)
The entire multi-container environment (MySQL 8.3.0, Kafka 7.7.8 KRaft, Nginx, and the 4 backend services) can be run using the local Docker Compose configuration:

```bash
export GOOGLE_API_KEY="your-gemini-api-key"
docker compose -f services-local.yml up --build -d
```
*Note: Wait until the database and Kafka become healthy before all microservices correctly register dependencies.*

## API Integration Highlights
*(Routed primarily through Nginx port `80`)*
- **Auth**: `POST /auth/v1/signup`, `POST /auth/v1/login`, `POST /auth/v1/refreshToken`
- **User**: `GET /user/v1/users/{userId}`, `PUT /user/v1/users/{userId}` (Updates profile info)
- **Expense**: `GET /expense/v1/getExpense`, `POST /expense/v1/addExpense` (Headers: `X-User-Id`)
- **DS**: `POST /v1/ds/message` (Extracts structured expense JSON from raw SMS via Gemini)

## Event-Driven Flow (Kafka)
- **`user_service` topic**: Auth Service (Producer) -> User Service (Consumer). Triggers profile creation on signup.
- **`expense_service` topic**: DS Service (Producer) -> Expense Service (Consumer). Persists AI-parsed SMS expenses.
