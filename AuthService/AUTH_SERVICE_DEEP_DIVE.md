# AuthService — Complete Deep Dive & Workflow Guide

> **If you're looking at this service for the first time, start from Section 1 and read top-down. Everything is ordered so that each section builds on the previous one.**

---

## Table of Contents

1. [What Is This Service?](#1-what-is-this-service)
2. [Where to Start Reading the Code (Reading Order)](#2-where-to-start-reading-the-code)
3. [High-Level Architecture Diagram](#3-high-level-architecture-diagram)
4. [The 3 Main User Flows (Step by Step)](#4-the-3-main-user-flows)
   - [4.1 Signup Flow](#41-signup-flow)
   - [4.2 Login Flow](#42-login-flow)
   - [4.3 Refresh Token Flow](#43-refresh-token-flow)
   - [4.4 Accessing a Protected Endpoint (e.g., /auth/v1/ping)](#44-accessing-a-protected-endpoint)
5. [Package-by-Package Breakdown](#5-package-by-package-breakdown)
6. [Class-by-Class Explanation](#6-class-by-class-explanation)
7. [How Data Flows Between Components (Wiring Diagram)](#7-how-data-flows-between-components)
8. [Database Schema](#8-database-schema)
9. [Configuration Explained](#9-configuration-explained)
10. [Key Concepts You Need to Understand](#10-key-concepts-you-need-to-understand)

---

## 1. What Is This Service?

This is a **JWT-based Authentication Microservice** built with **Spring Boot 3.3.5** and **Java 21**. It is part of a larger **Expense Tracker Application**.

**Its job is simple — it answers 3 questions:**
1. **"Who are you?"** → Signup (`POST /auth/v1/signup`)
2. **"Prove you are who you say you are"** → Login (`POST /auth/v1/login`)
3. **"My access token expired, give me a new one"** → Refresh (`POST /auth/v1/refreshToken`)

It uses:
- **MySQL** to store users and refresh tokens
- **JWT (JSON Web Tokens)** for stateless authentication
- **Refresh Tokens** (UUID strings stored in DB) for getting new JWTs without re-login
- **Apache Kafka** to publish a "new user created" event so other microservices (like a User Profile Service) know about it
- **Spring Security** to protect endpoints

---

## 2. Where to Start Reading the Code

**If you read in this exact order, everything will make sense:**

| Order | File | Why Read It |
|-------|------|-------------|
| 1 | `application.properties` | See the DB, Kafka, and port config — gives you the big picture of what external systems this service talks to |
| 2 | `entities/UserInfo.java` | The core user table — everything revolves around this |
| 3 | `entities/UserRole.java` | Roles associated with users |
| 4 | `entities/RefreshToken.java` | How refresh tokens are stored |
| 5 | `repository/UserRepository.java` | How users are fetched from DB |
| 6 | `repository/RefreshTokenRepository.java` | How refresh tokens are fetched from DB |
| 7 | `model/UserInfoDto.java` | The DTO that comes in from HTTP requests during signup |
| 8 | `request/AuthRequestDTO.java` | Login request body |
| 9 | `request/RefreshTokenRequestDTO.java` | Refresh token request body |
| 10 | `response/JwtResponseDTO.java` | What the API sends back (access token + refresh token) |
| 11 | `service/JwtService.java` | **The brain** — creates and validates JWT tokens |
| 12 | `service/CustomUserDetails.java` | Adapter that makes `UserInfo` work with Spring Security |
| 13 | `service/UserDetailsServiceImpl.java` | Signup logic + loads users for Spring Security |
| 14 | `service/RefreshTokenService.java` | Creates and verifies refresh tokens |
| 15 | `controller/AuthController.java` | Signup + ping endpoints |
| 16 | `controller/TokenController.java` | Login + refresh token endpoints |
| 17 | `controller/SecurityConfig.java` | **The gatekeeper** — defines which URLs need auth and which don't |
| 18 | `auth/JwtAuthFilter.java` | Intercepts EVERY request to check for JWT in the header |
| 19 | `auth/UserConfig.java` | Provides the password encoder bean |
| 20 | `eventProducer/UserInfoEvent.java` | Kafka event model |
| 21 | `eventProducer/UserInfoProducer.java` | Sends events to Kafka |
| 22 | `serializer/UserInfoSerializer.java` | Converts Kafka events to bytes |

---

## 3. High-Level Architecture Diagram

```
                          ┌──────────────────────────────────────────────────┐
                          │              AuthService (port 9898)             │
                          │                                                  │
   HTTP Request           │  ┌────────────┐    ┌──────────────────┐          │
──────────────────────────┼─▶│ JwtAuth    │───▶│  SecurityConfig  │          │
  (with/without JWT)      │  │ Filter     │    │  (filter chain)  │          │
                          │  └────────────┘    └────────┬─────────┘          │
                          │                             │                    │
                          │              ┌──────────────┼──────────────┐     │
                          │              ▼              ▼              ▼     │
                          │     ┌──────────────┐ ┌────────────┐ ┌────────┐  │
                          │     │ AuthController│ │TokenControl│ │ /health│  │
                          │     │  /signup      │ │ /login     │ │        │  │
                          │     │  /ping        │ │ /refresh   │ │        │  │
                          │     └──────┬────────┘ └─────┬──────┘ └────────┘  │
                          │            │                │                    │
                          │            ▼                ▼                    │
                          │  ┌──────────────────────────────────────┐        │
                          │  │           SERVICE LAYER              │        │
                          │  │  UserDetailsServiceImpl              │        │
                          │  │  JwtService                          │        │
                          │  │  RefreshTokenService                 │        │
                          │  └──────────────┬───────────────────────┘        │
                          │                 │                                │
                          │       ┌─────────┼──────────┐                     │
                          │       ▼                    ▼                     │
                          │  ┌──────────┐      ┌──────────────┐              │
                          │  │  MySQL   │      │    Kafka     │              │
                          │  │  (users, │      │ (user_service│              │
                          │  │  tokens, │      │   topic)     │              │
                          │  │  roles)  │      │              │              │
                          │  └──────────┘      └──────────────┘              │
                          └──────────────────────────────────────────────────┘
```

---

## 4. The 3 Main User Flows

### 4.1 Signup Flow

**Endpoint:** `POST /auth/v1/signup`
**Request Body (JSON):**
```json
{
  "username": "john",
  "password": "secret123",
  "first_name": "John",
  "last_name": "Doe",
  "email": "john@example.com",
  "phone_number": 1234567890
}
```

**Step-by-step data flow:**

```
Client                          AuthController                UserDetailsServiceImpl          UserRepository          Kafka (UserInfoProducer)
  │                                   │                              │                            │                          │
  │  POST /auth/v1/signup             │                              │                            │                          │
  │  (UserInfoDto in body)            │                              │                            │                          │
  │──────────────────────────────────▶│                              │                            │                          │
  │                                   │  signupUser(userInfoDto)     │                            │                          │
  │                                   │─────────────────────────────▶│                            │                          │
  │                                   │                              │  encode password (BCrypt)  │                          │
  │                                   │                              │─────────┐                  │                          │
  │                                   │                              │◀────────┘                  │                          │
  │                                   │                              │                            │                          │
  │                                   │                              │  findByUsername(username)   │                          │
  │                                   │                              │  (check duplicate)         │                          │
  │                                   │                              │───────────────────────────▶│                          │
  │                                   │                              │◀───────────────────────────│                          │
  │                                   │                              │                            │                          │
  │                                   │                              │  Generate UUID as userId   │                          │
  │                                   │                              │  save(new UserInfo)        │                          │
  │                                   │                              │───────────────────────────▶│  (INSERT into users)     │
  │                                   │                              │◀───────────────────────────│                          │
  │                                   │                              │                            │                          │
  │                                   │                              │  sendEventToKafka(event)   │                          │
  │                                   │                              │─────────────────────────────────────────────────────▶│
  │                                   │                              │                            │                          │
  │                                   │  return userId               │                            │                          │
  │                                   │◀─────────────────────────────│                            │                          │
  │                                   │                              │                            │                          │
  │                                   │  createRefreshToken(username)│                            │                          │
  │                                   │──────▶ RefreshTokenService   │                            │                          │
  │                                   │◀──────  (saved to DB)       │                            │                          │
  │                                   │                              │                            │                          │
  │                                   │  generateToken(username)     │                            │                          │
  │                                   │──────▶ JwtService            │                            │                          │
  │                                   │◀──────  (JWT string)        │                            │                          │
  │                                   │                              │                            │                          │
  │  Response: JwtResponseDTO         │                              │                            │                          │
  │  { accessToken, token, userId }   │                              │                            │                          │
  │◀──────────────────────────────────│                              │                            │                          │
```

**What happens under the hood:**
1. `AuthController.SignUp()` receives the `UserInfoDto` (deserialized from JSON using snake_case naming)
2. Calls `userDetailsService.signupUser()` which:
   - Hashes the password with BCrypt
   - Checks if username already exists → if yes, returns `null` → controller returns 400
   - Generates a UUID as the `userId`
   - Creates a `UserInfo` entity and saves to MySQL (`users` table)
   - Publishes a `UserInfoEvent` to Kafka topic `user_service` (so other microservices know a user was created)
3. Creates a refresh token (UUID string, stored in `tokens` table, expires in 100 minutes)
4. Generates a JWT access token (expires in ~100 seconds × 60 = ~100 minutes)
5. Returns both tokens + userId to the client

---

### 4.2 Login Flow

**Endpoint:** `POST /auth/v1/login`
**Request Body:**
```json
{
  "username": "john",
  "password": "secret123"
}
```

**Step-by-step data flow:**

```
Client                    TokenController            AuthenticationManager       UserDetailsServiceImpl       JwtService         RefreshTokenService
  │                            │                           │                           │                        │                      │
  │  POST /auth/v1/login       │                           │                           │                        │                      │
  │  (AuthRequestDTO)          │                           │                           │                        │                      │
  │───────────────────────────▶│                           │                           │                        │                      │
  │                            │  authenticate(            │                           │                        │                      │
  │                            │    username, password)     │                           │                        │                      │
  │                            │──────────────────────────▶│                           │                        │                      │
  │                            │                           │  loadUserByUsername()      │                        │                      │
  │                            │                           │──────────────────────────▶│                        │                      │
  │                            │                           │  (returns CustomUserDetails)                       │                      │
  │                            │                           │◀──────────────────────────│                        │                      │
  │                            │                           │  compare BCrypt passwords │                        │                      │
  │                            │                           │─────────┐                 │                        │                      │
  │                            │  Authentication OK        │◀────────┘                 │                        │                      │
  │                            │◀──────────────────────────│                           │                        │                      │
  │                            │                           │                           │                        │                      │
  │                            │  createRefreshToken()     │                           │                        │                      │
  │                            │──────────────────────────────────────────────────────────────────────────────▶│
  │                            │◀──────────────────────────────────────────────────────────────────────────────│
  │                            │                           │                           │                        │                      │
  │                            │  generateToken(username)  │                           │                        │                      │
  │                            │──────────────────────────────────────────────────────▶│                      │
  │                            │◀──────────────────────────────────────────────────────│                      │
  │                            │                           │                           │                        │                      │
  │  Response: JwtResponseDTO  │                           │                           │                        │                      │
  │  { accessToken, token }    │                           │                           │                        │                      │
  │◀───────────────────────────│                           │                           │                        │                      │
```

**What happens under the hood:**
1. `TokenController.AuthenticateAndGetToken()` receives `AuthRequestDTO`
2. Uses Spring Security's `AuthenticationManager.authenticate()` which internally:
   - Calls `UserDetailsServiceImpl.loadUserByUsername()` to fetch user from DB
   - Wraps user in `CustomUserDetails` (Spring Security's `UserDetails` interface)
   - Compares the submitted password with the BCrypt hash using `PasswordEncoder`
   - If mismatch → throws exception → controller returns 500
3. If authenticated, creates a new refresh token and saves it
4. Generates a JWT access token
5. Returns both tokens to the client

---

### 4.3 Refresh Token Flow

**Endpoint:** `POST /auth/v1/refreshToken`
**Request Body:**
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Step-by-step data flow:**

```
Client                   TokenController          RefreshTokenService        RefreshTokenRepository       JwtService
  │                            │                        │                          │                        │
  │  POST /auth/v1/refreshToken│                        │                          │                        │
  │  (RefreshTokenRequestDTO)  │                        │                          │                        │
  │───────────────────────────▶│                        │                          │                        │
  │                            │  findByToken(token)    │                          │                        │
  │                            │───────────────────────▶│                          │                        │
  │                            │                        │  findByToken(token)      │                        │
  │                            │                        │─────────────────────────▶│                        │
  │                            │                        │  RefreshToken entity     │                        │
  │                            │                        │◀─────────────────────────│                        │
  │                            │◀───────────────────────│                          │                        │
  │                            │                        │                          │                        │
  │                            │  verifyExpiration()    │                          │                        │
  │                            │───────────────────────▶│                          │                        │
  │                            │  (check expiryDate)    │                          │                        │
  │                            │◀───────────────────────│                          │                        │
  │                            │                        │                          │                        │
  │                            │  getUserInfo()         │                          │                        │
  │                            │  → get username        │                          │                        │
  │                            │                        │                          │                        │
  │                            │  generateToken(username)│                         │                        │
  │                            │───────────────────────────────────────────────────────────────────────────▶│
  │                            │◀───────────────────────────────────────────────────────────────────────────│
  │                            │                        │                          │                        │
  │  Response: JwtResponseDTO  │                        │                          │                        │
  │  { accessToken, token }    │                        │                          │                        │
  │◀───────────────────────────│                        │                          │                        │
```

**What happens:**
1. Client sends their refresh token
2. Service looks it up in DB
3. Checks if it's expired → if expired, deletes it and throws error
4. Gets the associated `UserInfo` from the refresh token entity (via `@OneToOne` relationship)
5. Generates a new JWT access token for that user
6. Returns the new access token + same refresh token

---

### 4.4 Accessing a Protected Endpoint

**Endpoint:** `GET /auth/v1/ping` (requires valid JWT)

```
Client                     JwtAuthFilter              JwtService           UserDetailsServiceImpl         AuthController
  │                            │                        │                          │                          │
  │  GET /auth/v1/ping         │                        │                          │                          │
  │  Header: Bearer <JWT>      │                        │                          │                          │
  │───────────────────────────▶│                        │                          │                          │
  │                            │  extractUsername(jwt)   │                          │                          │
  │                            │───────────────────────▶│                          │                          │
  │                            │  "john"                │                          │                          │
  │                            │◀───────────────────────│                          │                          │
  │                            │                        │                          │                          │
  │                            │  loadUserByUsername()   │                          │                          │
  │                            │───────────────────────────────────────────────────▶│                          │
  │                            │  CustomUserDetails     │                          │                          │
  │                            │◀───────────────────────────────────────────────────│                          │
  │                            │                        │                          │                          │
  │                            │  validateToken(jwt,    │                          │                          │
  │                            │     userDetails)       │                          │                          │
  │                            │───────────────────────▶│                          │                          │
  │                            │  true                  │                          │                          │
  │                            │◀───────────────────────│                          │                          │
  │                            │                        │                          │                          │
  │                            │  Set SecurityContext   │                          │                          │
  │                            │  (user is authenticated)                          │                          │
  │                            │                        │                          │                          │
  │                            │  Continue filter chain → Reaches Controller       │                          │
  │                            │──────────────────────────────────────────────────────────────────────────────▶│
  │                            │                        │                          │                          │
  │  Response: "Ping           │                        │                          │                          │
  │  Successful for user: xyz" │                        │                          │                          │
  │◀──────────────────────────────────────────────────────────────────────────────────────────────────────────│
```

---

## 5. Package-by-Package Breakdown

```
com.nstrange.authservice/
│
├── App.java                     ← Entry point. Boots up everything.
│
├── auth/                        ← SECURITY FILTERS & CONFIG BEANS
│   ├── JwtAuthFilter.java       ← Intercepts every HTTP request, checks JWT
│   └── UserConfig.java          ← Provides BCryptPasswordEncoder bean
│
├── controller/                  ← HTTP ENDPOINTS (what the outside world calls)
│   ├── AuthController.java      ← /signup, /ping, /health
│   ├── TokenController.java     ← /login, /refreshToken
│   └── SecurityConfig.java      ← Defines which URLs need auth, wires filter chain
│
├── entities/                    ← DATABASE TABLES (JPA entities)
│   ├── UserInfo.java            ← `users` table
│   ├── UserRole.java            ← `roles` table
│   └── RefreshToken.java        ← `tokens` table
│
├── eventProducer/               ← KAFKA (publishing events to other services)
│   ├── UserInfoEvent.java       ← The event payload model
│   └── UserInfoProducer.java    ← Sends the event to Kafka
│
├── model/                       ← DTOs for incoming data
│   └── UserInfoDto.java         ← Extends UserInfo, used for signup request body
│
├── repository/                  ← DATABASE ACCESS (Spring Data JPA)
│   ├── UserRepository.java      ← CRUD for users
│   └── RefreshTokenRepository.java ← CRUD for refresh tokens
│
├── request/                     ← REQUEST BODY DTOs
│   ├── AuthRequestDTO.java      ← Login request { username, password }
│   └── RefreshTokenRequestDTO.java ← Refresh request { token }
│
├── response/                    ← RESPONSE BODY DTOs
│   └── JwtResponseDTO.java      ← { accessToken, token, userId }
│
├── serializer/                  ← KAFKA SERIALIZATION
│   └── UserInfoSerializer.java  ← Converts UserInfoEvent → bytes for Kafka
│
├── service/                     ← BUSINESS LOGIC
│   ├── JwtService.java          ← JWT creation, validation, parsing
│   ├── CustomUserDetails.java   ← Adapts UserInfo for Spring Security
│   ├── UserDetailsServiceImpl.java ← Signup, user lookup, Kafka event publishing
│   └── RefreshTokenService.java ← Create/verify/find refresh tokens
│
└── utils/
    └── ValidationUtil.java      ← Empty placeholder (not used yet)
```

---

## 6. Class-by-Class Explanation

### 🔵 `App.java` — The Entry Point

| Aspect | Details |
|--------|---------|
| **What it does** | Bootstraps the Spring Boot application. `@EnableJpaRepositories` tells Spring where to scan for repository interfaces. |
| **Why it's necessary** | Without it, the application literally cannot start. It's the `main()` method. |
| **If it didn't exist** | The service wouldn't run. Period. |

---

### 🔵 `entities/UserInfo.java` — The User Entity

| Aspect | Details |
|--------|---------|
| **What it does** | Maps to the `users` table in MySQL. Stores `userId` (UUID string, primary key), `username`, `password` (BCrypt hashed), `firstName`, `lastName`, `email`, `phoneNumber`. Has a many-to-many relationship with `UserRole` through a `user_roles` join table. |
| **Why it's necessary** | This is the **core data model** of the entire service. Every operation (signup, login, token generation) revolves around this entity. |
| **If it didn't exist** | No users could be stored or authenticated. The entire service would be useless. |

---

### 🔵 `entities/UserRole.java` — The Role Entity

| Aspect | Details |
|--------|---------|
| **What it does** | Maps to the `roles` table. Stores role names (e.g., "ADMIN", "USER"). Linked to `UserInfo` via the `user_roles` join table. |
| **Why it's necessary** | Enables role-based access control (RBAC). Spring Security uses these roles as `GrantedAuthority` objects. |
| **If it didn't exist** | Users would have no roles/permissions. The `CustomUserDetails.getAuthorities()` would return empty, and any `@PreAuthorize` or role-based checks would fail. |

---

### 🔵 `entities/RefreshToken.java` — The Refresh Token Entity

| Aspect | Details |
|--------|---------|
| **What it does** | Maps to the `tokens` table. Stores a UUID token string, expiry date, and a `@OneToOne` link to `UserInfo`. |
| **Why it's necessary** | JWTs expire quickly (~100 min here). Without refresh tokens, users would have to **re-enter their password** every time the JWT expires. Refresh tokens allow silent re-authentication. |
| **If it didn't exist** | Users would be forced to login again every time their JWT expires. Very bad UX. |

---

### 🔵 `repository/UserRepository.java` — User Database Access

| Aspect | Details |
|--------|---------|
| **What it does** | Extends `CrudRepository`. Provides `findByUsername(String username)` to look up users. Spring Data JPA auto-generates the SQL query from the method name. |
| **Why it's necessary** | Without it, there's no way to read/write user data to/from MySQL. |
| **If it didn't exist** | Signup would fail (can't save users). Login would fail (can't find users). Everything breaks. |

---

### 🔵 `repository/RefreshTokenRepository.java` — Refresh Token Database Access

| Aspect | Details |
|--------|---------|
| **What it does** | Extends `CrudRepository`. Provides `findByToken(String token)` to look up refresh tokens. |
| **Why it's necessary** | Needed to save, retrieve, and delete refresh tokens from the `tokens` table. |
| **If it didn't exist** | Refresh token flow would completely break — can't create, verify, or look up tokens. |

---

### 🔵 `model/UserInfoDto.java` — Signup Request DTO

| Aspect | Details |
|--------|---------|
| **What it does** | Extends `UserInfo` and adds `@JsonNaming(SnakeCaseStrategy)` so that JSON fields like `first_name` map to Java's `firstName`. It's the deserialization target for signup requests. |
| **Why it's necessary** | Separates the **API contract** (snake_case JSON) from the **database entity** (camelCase Java). This is a clean architecture practice. |
| **If it didn't exist** | You'd have to use `UserInfo` directly for both API and DB, which couples your API format to your DB format. Also, snake_case JSON deserialization wouldn't work without the annotation. |

---

### 🔵 `request/AuthRequestDTO.java` — Login Request Body

| Aspect | Details |
|--------|---------|
| **What it does** | Simple POJO with `username` and `password` fields. Used to deserialize the login request JSON body. |
| **Why it's necessary** | Defines the shape of the login request. Without it, Spring can't deserialize the JSON into a Java object. |
| **If it didn't exist** | The login endpoint wouldn't be able to read the username/password from the request body. |

---

### 🔵 `request/RefreshTokenRequestDTO.java` — Refresh Token Request Body

| Aspect | Details |
|--------|---------|
| **What it does** | Simple POJO with a `token` field (snake_case). Used to deserialize the refresh token request. |
| **Why it's necessary** | Defines the shape of the refresh token request. |
| **If it didn't exist** | The refresh token endpoint couldn't parse the request body. |

---

### 🔵 `response/JwtResponseDTO.java` — API Response

| Aspect | Details |
|--------|---------|
| **What it does** | Contains `accessToken` (the JWT), `token` (the refresh token UUID), and `userId`. This is what the client receives after signup/login. |
| **Why it's necessary** | Standardizes the response format. The client knows exactly what to expect. |
| **If it didn't exist** | You'd have to return raw strings or maps — messy, error-prone, and hard to maintain. |

---

### 🔵 `service/JwtService.java` — JWT Token Engine 🧠

| Aspect | Details |
|--------|---------|
| **What it does** | **The heart of authentication.** It: (1) generates JWT tokens signed with HMAC-SHA256, (2) extracts the username from a token, (3) validates tokens (checks signature + expiration + username match). Uses a hardcoded Base64 secret key. |
| **Why it's necessary** | JWT is the mechanism that allows **stateless authentication**. The server doesn't need to store sessions — the token itself contains the user's identity, signed so it can't be tampered with. |
| **If it didn't exist** | No tokens could be created or validated. Users could never authenticate. The entire auth system collapses. |
| **Key details** | Token expiration: `100000 * 60` ms ≈ 100 minutes. Secret key is hardcoded (should be in env vars in production). |

---

### 🔵 `service/CustomUserDetails.java` — Spring Security Adapter

| Aspect | Details |
|--------|---------|
| **What it does** | Wraps `UserInfo` into Spring Security's `UserDetails` interface. Converts `UserRole` set into `GrantedAuthority` list. Returns `true` for all account status checks (non-expired, non-locked, etc.). |
| **Why it's necessary** | Spring Security **requires** `UserDetails` objects. Your `UserInfo` entity doesn't implement that interface. This class is the **bridge/adapter** between your domain model and Spring Security's requirements. |
| **If it didn't exist** | Spring Security wouldn't know how to get username, password, or roles from your user. Authentication would completely fail. `AuthenticationProvider` wouldn't be able to compare passwords. |

---

### 🔵 `service/UserDetailsServiceImpl.java` — User Business Logic

| Aspect | Details |
|--------|---------|
| **What it does** | Implements `UserDetailsService` (required by Spring Security). Has 3 main responsibilities: (1) `loadUserByUsername()` — fetches user from DB and wraps in `CustomUserDetails`, (2) `signupUser()` — hashes password, saves user, publishes Kafka event, (3) `getUserByUsername()` — returns userId for a given username. |
| **Why it's necessary** | This is the **central service** for all user-related operations. Spring Security calls `loadUserByUsername()` during authentication. Controllers call `signupUser()` and `getUserByUsername()`. |
| **If it didn't exist** | Nobody can sign up. Spring Security can't load users. Login breaks. Kafka events don't get published. Other microservices don't know about new users. |

---

### 🔵 `service/RefreshTokenService.java` — Refresh Token Logic

| Aspect | Details |
|--------|---------|
| **What it does** | (1) `createRefreshToken()` — generates a UUID token string, sets expiry to 100 minutes (`6000000` ms), links it to the user, saves to DB. (2) `verifyExpiration()` — checks if token is expired; if yes, deletes it and throws error. (3) `findByToken()` — looks up token in DB. |
| **Why it's necessary** | Manages the lifecycle of refresh tokens. Without it, users can't get new JWTs without logging in again. |
| **If it didn't exist** | No refresh tokens would exist. When a JWT expires, the user must re-authenticate with username/password. |

---

### 🔵 `controller/AuthController.java` — Signup & Ping Endpoints

| Aspect | Details |
|--------|---------|
| **What it does** | Exposes: (1) `POST /auth/v1/signup` — creates a new user and returns JWT + refresh token, (2) `GET /auth/v1/ping` — a protected endpoint that returns the user's ID if authenticated, (3) `GET /health` — simple health check. |
| **Why it's necessary** | Without it, there's no HTTP API for signup. The `/ping` endpoint is useful for other services to verify if a JWT is valid. |
| **If it didn't exist** | No one can register. No way to verify authentication is working. |

---

### 🔵 `controller/TokenController.java` — Login & Refresh Endpoints

| Aspect | Details |
|--------|---------|
| **What it does** | Exposes: (1) `POST /auth/v1/login` — authenticates credentials and returns JWT + refresh token, (2) `POST /auth/v1/refreshToken` — takes a refresh token and returns a new JWT. |
| **Why it's necessary** | Without it, users can't log in or refresh expired tokens. |
| **If it didn't exist** | Authentication is impossible for existing users. Token refresh doesn't work. |

---

### 🔵 `controller/SecurityConfig.java` — The Security Gatekeeper 🔒

| Aspect | Details |
|--------|---------|
| **What it does** | **The most important configuration class.** It: (1) Defines the `SecurityFilterChain` — which URLs are public (`/login`, `/signup`, `/refreshToken`, `/health`) and which require auth (everything else), (2) Disables CSRF and CORS (appropriate for a stateless API), (3) Sets session management to STATELESS (no server-side sessions), (4) Adds the `JwtAuthFilter` before Spring's default `UsernamePasswordAuthenticationFilter`, (5) Configures `DaoAuthenticationProvider` with `UserDetailsServiceImpl` and `BCryptPasswordEncoder`, (6) Exposes `AuthenticationManager` bean. |
| **Why it's necessary** | Without it, Spring Security uses defaults — ALL endpoints would require basic auth, no JWT support, sessions enabled. Your entire auth architecture depends on this config. |
| **If it didn't exist** | Spring Security would block everything or use defaults that don't match your JWT-based approach. Login/signup endpoints would be protected (catch-22). JWT filter wouldn't be in the chain. |

---

### 🔵 `auth/JwtAuthFilter.java` — The Request Interceptor

| Aspect | Details |
|--------|---------|
| **What it does** | Extends `OncePerRequestFilter` (runs once per HTTP request). For every incoming request: (1) Extracts the `Authorization: Bearer <token>` header, (2) Extracts the username from the JWT, (3) Loads the user from DB, (4) Validates the token, (5) If valid, sets the `SecurityContextHolder` authentication — telling Spring Security "this request is from an authenticated user." |
| **Why it's necessary** | This is **how JWTs are actually checked**. Without this filter, even if you send a valid JWT, Spring Security wouldn't know about it. It's the bridge between the JWT in the HTTP header and Spring Security's internal authentication context. |
| **If it didn't exist** | Protected endpoints would **always** return 401 Unauthorized, even with a valid JWT. The JWT would be ignored because nothing reads it. |

---

### 🔵 `auth/UserConfig.java` — Password Encoder Provider

| Aspect | Details |
|--------|---------|
| **What it does** | Provides a `@Bean` of `BCryptPasswordEncoder`. BCrypt is a one-way hashing algorithm — passwords are hashed before storage and compared during login. |
| **Why it's necessary** | Spring Security requires a `PasswordEncoder` bean. BCrypt is the industry standard for password hashing. |
| **If it didn't exist** | Application would fail to start — Spring can't inject `PasswordEncoder` anywhere (used in `SecurityConfig`, `UserDetailsServiceImpl`). |

---

### 🔵 `eventProducer/UserInfoEvent.java` — Kafka Event Model

| Aspect | Details |
|--------|---------|
| **What it does** | A simple POJO representing the data published to Kafka when a new user signs up: `firstName`, `lastName`, `email`, `phoneNumber`, `userId`. Uses snake_case JSON naming. |
| **Why it's necessary** | Defines the contract for the Kafka message. Other microservices consuming this topic expect this exact structure. |
| **If it didn't exist** | Other services wouldn't know about new users. In a microservice architecture, this breaks cross-service communication. |

---

### 🔵 `eventProducer/UserInfoProducer.java` — Kafka Publisher

| Aspect | Details |
|--------|---------|
| **What it does** | Uses `KafkaTemplate` to send a `UserInfoEvent` message to the `user_service` Kafka topic (configured in `application.properties`). |
| **Why it's necessary** | This is the **event-driven communication** mechanism. When a user signs up in AuthService, other services (like a User Profile Service) need to know — Kafka is the messenger. |
| **If it didn't exist** | AuthService would still work in isolation, but **other microservices would never learn about new users**. The Expense Tracker's other services would have no user data. |

---

### 🔵 `serializer/UserInfoSerializer.java` — Kafka Serializer

| Aspect | Details |
|--------|---------|
| **What it does** | Implements Kafka's `Serializer<UserInfoEvent>`. Converts `UserInfoEvent` to bytes using Jackson's `ObjectMapper`. |
| **Why it's necessary** | Kafka transmits bytes, not Java objects. This serializer converts your event to JSON bytes. |
| **If it didn't exist** | Kafka wouldn't know how to serialize `UserInfoEvent`. Message publishing would fail. *Note: In the current config, `JsonSerializer` from Spring Kafka is used in `application.properties`, so this custom serializer may be a leftover/backup.* |

---

### 🔵 `utils/ValidationUtil.java` — Empty Utility Class

| Aspect | Details |
|--------|---------|
| **What it does** | Nothing. It's an empty class — a placeholder for future validation logic. |
| **Why it's necessary** | It's not currently necessary. Likely planned for input validation (e.g., email format, password strength). |
| **If it didn't exist** | No impact on the current application. |

---

## 7. How Data Flows Between Components

### The Component Dependency Graph

```
                    ┌──────────────────┐
                    │   HTTP Client    │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │  JwtAuthFilter   │ ←── Reads JWT from header
                    │  (runs first)    │
                    └────────┬─────────┘
                             │ uses
                    ┌────────▼─────────┐
                    │  SecurityConfig  │ ←── Decides: permit or block?
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼                              ▼
    ┌──────────────────┐          ┌──────────────────┐
    │  AuthController  │          │ TokenController   │
    │  (signup, ping)  │          │ (login, refresh)  │
    └────────┬─────────┘          └────────┬──────────┘
             │                              │
             │         uses                 │ uses
             ▼                              ▼
    ┌────────────────────────────────────────────────┐
    │              SERVICE LAYER                      │
    │                                                 │
    │  UserDetailsServiceImpl ◄──── loadUserByUsername│
    │    │                            (Spring Security│
    │    ├── signupUser()              calls this)    │
    │    ├── getUserByUsername()                       │
    │    └── publishes to ──► UserInfoProducer ──► Kafka
    │                                                 │
    │  JwtService                                     │
    │    ├── generateToken()                          │
    │    ├── validateToken()                          │
    │    └── extractUsername()                         │
    │                                                 │
    │  RefreshTokenService                            │
    │    ├── createRefreshToken()                     │
    │    ├── verifyExpiration()                        │
    │    └── findByToken()                            │
    │                                                 │
    │  CustomUserDetails                              │
    │    └── Wraps UserInfo for Spring Security       │
    └────────────────────┬───────────────────────────┘
                         │ uses
                         ▼
    ┌──────────────────────────────────────┐
    │          REPOSITORY LAYER            │
    │  UserRepository  RefreshTokenRepo    │
    └────────────────────┬─────────────────┘
                         │
                         ▼
    ┌──────────────────────────────────────┐
    │             MySQL Database            │
    │  Tables: users, roles, user_roles,   │
    │          tokens                       │
    └──────────────────────────────────────┘
```

---

## 8. Database Schema

```
┌──────────────────────┐       ┌──────────────────┐       ┌──────────────────┐
│       users          │       │    user_roles     │       │      roles       │
├──────────────────────┤       ├──────────────────┤       ├──────────────────┤
│ user_id (PK, VARCHAR)│◄──────│ user_id (FK)     │       │ role_id (PK, INT)│
│ username             │       │ role_id (FK)     │──────▶│ name             │
│ password (BCrypt)    │       └──────────────────┘       └──────────────────┘
│ first_name           │
│ last_name            │       ┌──────────────────┐
│ email                │       │      tokens      │
│ phone_number         │       ├──────────────────┤
│                      │◄──────│ id (PK, FK→user_id)
└──────────────────────┘       │ token (UUID)     │
                               │ expiry_date      │
                               └──────────────────┘
```

---

## 9. Configuration Explained (`application.properties`)

| Property | Value | Meaning |
|----------|-------|---------|
| `server.port` | `9898` | The service runs on port 9898 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/auth_service_db` | MySQL database name. Supports env vars `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DB` |
| `spring.datasource.username/password` | `root` / `narvar007` | DB credentials |
| `spring.jpa.hibernate.ddl-auto` | `update` | Hibernate auto-creates/updates tables based on entities |
| `spring.kafka.producer.bootstrap-servers` | `localhost:9092` | Kafka broker address. Supports `KAFKA_HOST`, `KAFKA_PORT` env vars |
| `spring.kafka.topic-json.name` | `user_service` | Kafka topic where user signup events are published |
| `spring.kafka.producer.value-serializer` | `JsonSerializer` | Uses Spring Kafka's JSON serializer for Kafka messages |

---

## 10. Key Concepts You Need to Understand

### JWT (JSON Web Token)
- A **stateless** token. The server doesn't store it. It's self-contained — it includes the username, issue time, expiry time, and a cryptographic signature.
- The server signs it with a secret key. When a client sends it back, the server verifies the signature to ensure it hasn't been tampered with.
- **Analogy:** A JWT is like a stamped wristband at a concert. You show it at any gate, and they let you through without calling the ticket office.

### Refresh Token
- A **stateful** token stored in the database. It's a random UUID string with an expiry date.
- When the JWT expires, instead of forcing the user to log in again, the client sends the refresh token to get a brand-new JWT.
- **Analogy:** Your concert wristband (JWT) is only valid for 2 hours. But you have a paper ticket (refresh token) that lets you get a new wristband without going to the box office again.

### Spring Security Filter Chain
- Every HTTP request passes through a **chain of filters** before reaching your controller.
- `JwtAuthFilter` is inserted **before** Spring's default `UsernamePasswordAuthenticationFilter`.
- The order matters: JWT is checked first → if valid, user is authenticated → controller runs. If JWT is missing/invalid on a protected route → 401 Unauthorized.

### Kafka Event-Driven Architecture
- When a user signs up, other microservices need to know (e.g., to create a profile, set up defaults).
- Instead of making HTTP calls to each service (tight coupling), AuthService publishes a message to Kafka.
- Other services subscribe to the `user_service` topic and react independently (loose coupling).

### BCrypt Password Hashing
- Passwords are **never** stored in plain text.
- BCrypt produces a one-way hash — you can verify a password against it, but can't reverse it.
- Each hash includes a random salt, so identical passwords produce different hashes.

---

## Quick Reference: API Endpoints

| Method | Endpoint | Auth Required? | Purpose |
|--------|----------|---------------|---------|
| `POST` | `/auth/v1/signup` | ❌ No | Register a new user |
| `POST` | `/auth/v1/login` | ❌ No | Login with username + password |
| `POST` | `/auth/v1/refreshToken` | ❌ No | Get new JWT using refresh token |
| `GET` | `/auth/v1/ping` | ✅ Yes (JWT) | Verify authentication, returns userId |
| `GET` | `/health` | ❌ No | Health check |

---

## TL;DR — The 30-Second Summary

1. **User signs up** → password hashed → saved to MySQL → Kafka event sent → JWT + refresh token returned
2. **User logs in** → credentials verified against DB → JWT + refresh token returned
3. **User accesses protected endpoint** → `JwtAuthFilter` reads JWT from header → validates → allows/denies access
4. **JWT expires** → user sends refresh token → service issues a new JWT → no re-login needed
5. **Other services** listen on Kafka for new user events → stay in sync without direct API calls

