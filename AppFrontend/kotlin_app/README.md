# Arthabit — Android (Kotlin) Frontend

> **Module:** `AppFrontend/kotlin_app`
> Native Android client for the **Expense Tracker App** microservices ecosystem, built with **Jetpack Compose**, **Hilt**, and **Retrofit**.

---

## Table of Contents

1. [Overview](#overview)
2. [Tech Stack](#tech-stack)
3. [Architecture](#architecture)
4. [Project Structure](#project-structure)
5. [Screens & Navigation](#screens--navigation)
6. [Backend Service Integration](#backend-service-integration)
7. [API Endpoints Consumed](#api-endpoints-consumed)
8. [Dependency Injection](#dependency-injection)
9. [Data Flow](#data-flow)
10. [Domain Models](#domain-models)
11. [DTOs (Data Transfer Objects)](#dtos-data-transfer-objects)
12. [Token Management & Auto-Login](#token-management--auto-login)
13. [UI Components](#ui-components)
14. [Theming](#theming)
15. [Configuration](#configuration)
16. [Prerequisites](#prerequisites)
17. [Build & Run](#build--run)
18. [Environment Setup](#environment-setup)
19. [Dependencies](#dependencies)

---

## Overview

**Arthabit** is the native Android frontend for the Expense Tracker App. It provides users with the ability to:

- **Sign up** for a new account
- **Log in** with existing credentials (with automatic session restoration)
- **View** a list of recent expenses
- **Add** new expenses with amount, merchant, and currency
- **View** their user profile (name, email, phone)
- **Log out** and clear session data

The app communicates with multiple Spring Boot microservices via REST APIs through Retrofit HTTP clients.

---

## Tech Stack

| Category | Technology | Version |
|---|---|---|
| **Language** | Kotlin | 2.0.21 |
| **UI Framework** | Jetpack Compose | BOM 2024.12.01 |
| **DI Framework** | Dagger Hilt | 2.53.1 |
| **Networking** | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| **Navigation** | Navigation Compose | 2.8.5 |
| **Local Storage** | DataStore Preferences | 1.1.1 |
| **Image Loading** | Coil Compose | 2.7.0 |
| **Async** | Kotlin Coroutines | 1.9.0 |
| **Build Tool** | Gradle (Kotlin DSL) | AGP 8.13.2 |
| **Min SDK** | Android 9 (API 28) | — |
| **Target SDK** | Android 15 (API 35) | — |
| **Compile SDK** | 35 | — |
| **JVM Target** | 11 | — |

---

## Architecture

The application follows **Clean Architecture** with a clear separation into three layers:

```
┌───────────────────────────────────────────────────────┐
│                      UI Layer                         │
│  (Screens, ViewModels, Components, Theme)             │
├───────────────────────────────────────────────────────┤
│                    Domain Layer                       │
│  (Repository Interfaces, Domain Models, Resource)     │
├───────────────────────────────────────────────────────┤
│                     Data Layer                        │
│  (Repository Impls, Retrofit APIs, DTOs, TokenManager)│
└───────────────────────────────────────────────────────┘
```

- **UI Layer** — Jetpack Compose screens, Hilt-injected ViewModels with `StateFlow`-based UI state, reusable composable components.
- **Domain Layer** — Pure Kotlin interfaces and models with no Android framework dependencies. Defines contracts (`AuthRepository`, `ExpenseRepository`, `UserRepository`) and domain models (`AuthTokens`, `Expense`, `User`).
- **Data Layer** — Concrete implementations of domain repository interfaces, Retrofit API interface definitions, DTO classes for JSON serialization, and local token persistence via DataStore.

**Pattern:** MVVM (Model-View-ViewModel) with unidirectional data flow.

---

## Project Structure

```
kotlin_app/
├── build.gradle.kts                          # Root build script (plugin declarations)
├── settings.gradle.kts                       # Project settings (name: "Arthabit")
├── gradle.properties                         # Gradle JVM & AndroidX config
├── gradle/
│   └── libs.versions.toml                    # Version catalog (all dependency versions)
└── app/
    ├── build.gradle.kts                      # App module build script
    └── src/main/
        ├── AndroidManifest.xml               # Manifest (permissions, activities)
        └── java/com/nstrange/arthabit/
            ├── ArthabitApp.kt                # Application class (@HiltAndroidApp)
            ├── MainActivity.kt               # Single activity entry point (@AndroidEntryPoint)
            │
            ├── data/                         # ── Data Layer ──
            │   ├── local/
            │   │   └── TokenManager.kt       # DataStore-based JWT & userId persistence
            │   ├── remote/
            │   │   ├── ApiConfig.kt          # Centralized base URLs for all microservices
            │   │   ├── AuthApi.kt            # Retrofit interface → Auth Service
            │   │   ├── ExpenseApi.kt         # Retrofit interface → Expense Service
            │   │   ├── UserApi.kt            # Retrofit interface → User Service
            │   │   └── dto/
            │   │       └── Dtos.kt           # All request/response DTOs
            │   └── repository/
            │       ├── AuthRepositoryImpl.kt     # Auth repository implementation
            │       ├── ExpenseRepositoryImpl.kt  # Expense repository implementation
            │       └── UserRepositoryImpl.kt     # User repository implementation
            │
            ├── di/                           # ── Dependency Injection ──
            │   ├── NetworkModule.kt          # Provides OkHttp, Retrofit instances, API impls
            │   └── RepositoryModule.kt       # Binds repository interfaces to implementations
            │
            ├── domain/                       # ── Domain Layer ──
            │   ├── model/
            │   │   ├── AuthTokens.kt         # Access token, refresh token, userId
            │   │   ├── Expense.kt            # Expense domain model (with formatting)
            │   │   └── User.kt               # User domain model (with formatting)
            │   └── repository/
            │       ├── AuthRepository.kt     # Auth operations contract
            │       ├── ExpenseRepository.kt  # Expense operations contract
            │       └── UserRepository.kt     # User operations contract
            │
            ├── navigation/                   # ── Navigation ──
            │   ├── NavGraph.kt               # Compose NavHost with all routes
            │   └── Routes.kt                 # Sealed class defining route strings
            │
            ├── ui/                           # ── UI Layer ──
            │   ├── components/
            │   │   ├── AddExpenseBottomSheet.kt  # Modal bottom sheet for adding expenses
            │   │   ├── CustomBox.kt              # Neo-brutalist card with shadow offset
            │   │   ├── ExpenseItem.kt            # Single expense row composable
            │   │   └── Heading.kt                # Section heading inside CustomBox
            │   ├── screens/
            │   │   ├── home/
            │   │   │   ├── HomeScreen.kt         # Home screen (expense list + FAB)
            │   │   │   └── HomeViewModel.kt      # Home state management
            │   │   ├── login/
            │   │   │   ├── LoginScreen.kt        # Login screen (with auto-login)
            │   │   │   └── LoginViewModel.kt     # Login state management
            │   │   ├── profile/
            │   │   │   ├── ProfileScreen.kt      # User profile screen
            │   │   │   └── ProfileViewModel.kt   # Profile state management
            │   │   └── signup/
            │   │       ├── SignupScreen.kt        # Signup screen (6 fields)
            │   │       └── SignupViewModel.kt     # Signup state management
            │   └── theme/
            │       ├── Color.kt              # Color palette (primary, dark, status, card)
            │       ├── Theme.kt              # Material3 light theme definition
            │       └── Type.kt               # Typography scale
            │
            └── util/
                └── Resource.kt               # Sealed class: Success / Error / Loading
```

---

## Screens & Navigation

The app uses **Jetpack Navigation Compose** with slide transition animations.

### Navigation Flow

```
┌─────────┐    auto-login     ┌──────────┐
│  Login   │ ──────success───► │   Home   │
│  Screen  │                   │  Screen  │
│          │ ◄──── logout ──── │          │
└────┬─────┘                   └────┬─────┘
     │                              │
     │ navigate                     │ navigate
     ▼                              ▼
┌─────────┐                   ┌──────────┐
│  Signup  │ ──── success ───►│ Profile  │
│  Screen  │                  │  Screen  │
└──────────┘                  └──────────┘
```

| Route | Screen | Description |
|---|---|---|
| `login` | `LoginScreen` | Username/password login with auto-login check on launch. **Start destination.** |
| `signup` | `SignupScreen` | User registration (first name, last name, username, email, password, phone number). |
| `home` | `HomeScreen` | Displays recent expenses in a lazy list. FAB opens `AddExpenseBottomSheet`. |
| `profile` | `ProfileScreen` | Shows user personal information, settings placeholders, and a logout button. |

### Screen Details

#### Login Screen
- On launch, automatically attempts session restoration: first via `ping` (validates existing access token), then via `refreshToken`.
- If auto-login succeeds, the user is navigated directly to Home without seeing the login form.
- Manual login with username and password.
- Input validation (non-empty fields).
- Navigation to Signup screen.

#### Signup Screen
- Six input fields: First Name, Last Name, Username, Email, Password, Phone Number.
- Client-side validation for all fields (non-empty, valid phone number format).
- On success, tokens are saved and user is navigated to Home.

#### Home Screen
- Top navigation bar with app name ("Arthabit") and a tappable profile avatar.
- Recent expenses displayed in a `LazyColumn` with loading/error/empty states.
- Floating Action Button (FAB) to open the Add Expense bottom sheet.
- Dark-themed spends section with rounded top corners.

#### Profile Screen
- Profile picture with camera edit button overlay.
- Personal information section (Name, Phone, Email).
- Settings section (Notifications, Privacy, Dark Mode — placeholders).
- Logout button that clears tokens and navigates to Login.

---

## Backend Service Integration

The app communicates with **four** backend microservices:

| Service | Default Base URL | Port | Purpose |
|---|---|---|---|
| **Auth Service** | `http://10.0.2.2:9898` | 9898 | Authentication (login, signup, token refresh, ping) |
| **User Service** | `http://10.0.2.2:9810` | 9810 | User profile retrieval |
| **Expense Service** | `http://10.0.2.2:9820` | 9820 | Expense CRUD operations |
| **DS Service** | `http://10.0.2.2:8010` | 8010 | Data service (configured but not yet consumed) |

> **Note:** `10.0.2.2` is the Android emulator's alias for the host machine's `localhost`. For physical device testing, replace with the machine's LAN IP address in `ApiConfig.kt`.

Each service gets its own named `Retrofit` instance via Hilt's `@Named` qualifier (`"auth"`, `"user"`, `"expense"`).

---

## API Endpoints Consumed

### Auth Service (`port 9898`)

| Method | Endpoint | Headers | Body | Response |
|---|---|---|---|---|
| `GET` | `/auth/v1/ping` | `Authorization: Bearer <token>` | — | Plain text containing userId UUID |
| `POST` | `/auth/v1/login` | — | `LoginRequest` (username, password) | `AuthResponse` (accessToken, token, userId) |
| `POST` | `/auth/v1/signup` | — | `SignupRequest` (first_name, last_name, email, phone_number, password, username) | `AuthResponse` |
| `POST` | `/auth/v1/refreshToken` | — | `RefreshTokenRequest` (token) | `AuthResponse` |

### User Service (`port 9810`)

| Method | Endpoint | Query Params | Response |
|---|---|---|---|
| `GET` | `/user/v1/getUser` | `userId` | `UserDto` (user_id, first_name, last_name, phone_number, email, profile_pic) |

### Expense Service (`port 9820`)

| Method | Endpoint | Headers | Body | Response |
|---|---|---|---|---|
| `GET` | `/expense/v1/getExpense` | `Authorization: Bearer <token>`, `X-User-Id: <userId>` | — | `List<ExpenseDto>` |
| `POST` | `/expense/v1/addExpense` | `Authorization: Bearer <token>`, `X-User-Id: <userId>` | `AddExpenseRequest` (amount, merchant, currency) | `ResponseBody` |

---

## Dependency Injection

Hilt is configured with two modules installed in `SingletonComponent`:

### `NetworkModule` (object module — `@Provides`)
- **`OkHttpClient`** — Shared HTTP client with:
  - Body-level logging interceptor
  - Default headers (`Accept`, `Content-Type`, `X-Requested-With`)
  - 30-second connect/read/write timeouts
- **`Retrofit` (auth)** — `@Named("auth")` instance pointing to Auth Service
- **`Retrofit` (user)** — `@Named("user")` instance pointing to User Service
- **`Retrofit` (expense)** — `@Named("expense")` instance pointing to Expense Service
- **`AuthApi`**, **`UserApi`**, **`ExpenseApi`** — Created from their respective Retrofit instances

### `RepositoryModule` (abstract module — `@Binds`)
- `AuthRepositoryImpl` → `AuthRepository`
- `UserRepositoryImpl` → `UserRepository`
- `ExpenseRepositoryImpl` → `ExpenseRepository`

### Other Injectable Components
- **`TokenManager`** — `@Singleton`, injected with `@ApplicationContext`
- **ViewModels** — All annotated with `@HiltViewModel` and use `@Inject constructor`

---

## Data Flow

```
User Action
    │
    ▼
Composable Screen (collects StateFlow)
    │
    ▼
ViewModel (updates MutableStateFlow)
    │
    ▼
Repository Interface (domain layer)
    │
    ▼
Repository Implementation (data layer)
    │
    ├──► Retrofit API (network call)
    │        │
    │        ▼
    │    Backend Microservice
    │
    └──► TokenManager (DataStore read/write)
             │
             ▼
         Local Preferences Storage
```

Each ViewModel exposes a `StateFlow<UiState>` that the screen collects. The UI state is a data class containing all display-related fields (data, loading flags, error messages). ViewModels call repository methods in `viewModelScope.launch` coroutines and update state based on `Resource` results.

---

## Domain Models

### `AuthTokens`
```
accessToken: String      — JWT access token for API authorization
refreshToken: String     — Refresh token for session renewal
userId: String?          — User's unique identifier (UUID)
```

### `Expense`
```
key: Int                 — Index-based key for LazyColumn
amount: Double           — Expense amount
merchant: String         — Merchant/vendor name
currency: String         — Currency code (e.g., "INR", "USD")
createdAt: LocalDateTime — Timestamp of expense creation
formattedDate: String    — Computed: "MMM d" format (e.g., "Jan 5")
formattedAmount: String  — Computed: "CUR 0.00" format (e.g., "INR 150.00")
```

### `User`
```
userId: String           — Unique user identifier (UUID)
firstName: String        — First name
lastName: String         — Last name
phoneNumber: Long        — Phone number (numeric)
email: String            — Email address
profilePic: String?      — Profile picture URL (nullable)
fullName: String         — Computed: "firstName lastName"
formattedPhone: String   — Computed: "(xxx) xxx-xxxx" for 10-digit numbers
```

---

## DTOs (Data Transfer Objects)

All DTOs are defined in `data/remote/dto/Dtos.kt` and use `@SerializedName` for JSON field mapping with Gson.

| DTO | Direction | Fields |
|---|---|---|
| `LoginRequest` | Request → Auth Service | `username`, `password` |
| `SignupRequest` | Request → Auth Service | `first_name`, `last_name`, `email`, `phone_number`, `password`, `username` |
| `RefreshTokenRequest` | Request → Auth Service | `token` |
| `AuthResponse` | Response ← Auth Service | `accessToken`, `token` (refresh), `userId?` |
| `UserDto` | Response ← User Service | `user_id`, `first_name`, `last_name`, `phone_number`, `email`, `profile_pic?` |
| `ExpenseDto` | Response ← Expense Service | `amount`, `merchant`, `currency`, `created_at` |
| `AddExpenseRequest` | Request → Expense Service | `amount`, `merchant`, `currency` |

---

## Token Management & Auto-Login

### Storage
Tokens are persisted using **Jetpack DataStore Preferences** (`arthabit_prefs`):
- `accessToken` — JWT access token
- `refreshToken` — JWT refresh token
- `userId` — User's UUID

### Auto-Login Flow (on app launch)
1. **Ping** — Send a `GET /auth/v1/ping` with the stored access token. If the response contains a valid UUID, the token is still valid → navigate to Home.
2. **Refresh** — If ping fails, attempt `POST /auth/v1/refreshToken` with the stored refresh token. If successful, save new tokens → navigate to Home.
3. **Manual Login** — If both fail, show the login form.

### Logout
Calling `logout()` clears all three DataStore keys (`accessToken`, `refreshToken`, `userId`) and navigates to the Login screen with a full back stack clear.

---

## UI Components

### `CustomBox`
A **neo-brutalist** card composable with a solid black shadow offset (5dp right, 5dp down), white background, black border, and rounded corners. Used for section headings and the login/signup form containers.

### `Heading`
A section heading wrapped in a `CustomBox`. Displays bold, centered `headlineMedium` text.

### `ExpenseItem`
A single expense row with a dark surface background:
- Shopping cart icon in a rounded container
- Merchant name and formatted date
- Formatted currency amount (right-aligned)

### `AddExpenseBottomSheet`
A Material3 `ModalBottomSheet` for adding new expenses:
- Amount input (decimal keyboard)
- Merchant name input
- Currency toggle (INR / USD)
- Client-side validation (positive amount, non-empty merchant)
- Loading state on the submit button

---

## Theming

### Color Palette

| Token | Hex | Usage |
|---|---|---|
| `Primary` | `#007AFF` | iOS Blue — buttons, icons, FAB, links |
| `Secondary` | `#5856D6` | iOS Purple |
| `DarkBackground` | `#000000` | Expense list section, profile screen |
| `DarkSurface` | `#1C1C1E` | Expense item cards, profile info cards |
| `DarkSurfaceVariant` | `#2C2C2E` | Icon containers, dividers |
| `DarkTextSecondary` | `#8E8E93` | Secondary labels, dates |
| `StatusError` | `#FF3B30` | Error messages, logout button |
| `StatusSuccess` | `#34C759` | Success indicators |
| `StatusWarning` | `#FF9500` | Warning indicators |
| `CardBorder` / `CardShadow` | `#000000` | Neo-brutalist card styling |
| `CurrencySelected` | `#007AFF` | Selected currency toggle |
| `CurrencyUnselected` | `#E5E5EA` | Unselected currency toggle |

### Theme
- **Light color scheme** (Material3 `lightColorScheme`) applied globally.
- Dark colors are used manually within specific composables (expense list, profile) for a mixed light/dark aesthetic.

### Typography
Custom `Typography` scale with `FontFamily.Default`:
- `displayLarge` — 36sp Bold
- `displayMedium` — 30sp Bold
- `headlineLarge` — 24sp SemiBold
- `headlineMedium` — 20sp SemiBold
- `titleLarge` — 18sp Medium
- `bodyLarge` — 16sp Normal
- `bodyMedium` — 14sp Normal
- `bodySmall` — 12sp Normal
- `labelLarge` — 16sp SemiBold
- `labelMedium` — 14sp Medium
- `labelSmall` — 12sp Medium

---

## Configuration

### `ApiConfig.kt`
Centralized backend service URLs. Modify the `HOST` constant to switch between emulator and physical device:

```kotlin
object ApiConfig {
    private const val HOST = "10.0.2.2"         // Emulator → host localhost
    // private const val HOST = "192.168.x.x"   // Physical device → LAN IP

    const val AUTH_SERVICE_URL    = "http://$HOST:9898"
    const val USER_SERVICE_URL   = "http://$HOST:9810"
    const val EXPENSE_SERVICE_URL = "http://$HOST:9820"
    const val DS_SERVICE_URL     = "http://$HOST:8010"
}
```

### Android Manifest
- **`INTERNET`** permission declared
- **`usesCleartextTraffic = true`** — Allows HTTP (non-HTTPS) traffic for local development
- Single `MainActivity` as the launcher activity
- Custom `Application` class: `ArthabitApp` (Hilt entry point)

---

## Prerequisites

- **Android Studio** Ladybug or newer (with Kotlin 2.0+ support)
- **JDK 11+**
- **Android SDK** — API 35 (compileSdk), API 28+ device/emulator
- **Backend services running** — Auth (9898), User (9810), Expense (9820)

---

## Build & Run

### Using Android Studio
1. Open the `kotlin_app/` directory in Android Studio.
2. Sync Gradle (automatic on first open).
3. Ensure the backend microservices are running.
4. Select a device/emulator (API 28+).
5. Click **Run ▶** or press `Shift + F10`.

### Using Command Line
```bash
# Navigate to project root
cd kotlin_app/

# Build debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Run all unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

---

## Environment Setup

### For Android Emulator
No changes needed — the default `HOST = "10.0.2.2"` in `ApiConfig.kt` maps to the host machine's `localhost`.

### For Physical Device
1. Ensure the device and development machine are on the same network.
2. Update `ApiConfig.kt`:
   ```kotlin
   private const val HOST = "192.168.x.x"  // Your machine's LAN IP
   ```
3. Rebuild and run.

### Backend Services Required
Make sure the following Spring Boot microservices are running before using the app:

| Service | Port | Status Required |
|---|---|---|
| Auth Service | `9898` | ✅ Required for login, signup, token refresh |
| User Service | `9810` | ✅ Required for profile screen |
| Expense Service | `9820` | ✅ Required for expense list & add |
| DS Service | `8010` | ⬜ Optional (not yet consumed) |

---

## Dependencies

Full dependency catalog managed via `gradle/libs.versions.toml`:

| Dependency | Version | Purpose |
|---|---|---|
| `androidx.core:core-ktx` | 1.15.0 | Kotlin extensions for Android core |
| `androidx.appcompat:appcompat` | 1.7.0 | Backward-compatible Android components |
| `com.google.android.material:material` | 1.12.0 | Material Design components |
| `androidx.compose:compose-bom` | 2024.12.01 | Compose Bill of Materials |
| `androidx.compose.material3:material3` | (BOM) | Material 3 composables |
| `androidx.compose.material:material-icons-extended` | (BOM) | Extended Material icons |
| `androidx.activity:activity-compose` | 1.9.3 | Compose integration with Activity |
| `androidx.navigation:navigation-compose` | 2.8.5 | Compose navigation |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.8.7 | Lifecycle-aware Compose utilities |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.7 | ViewModel integration with Compose |
| `com.google.dagger:hilt-android` | 2.53.1 | Dependency injection framework |
| `com.google.dagger:hilt-android-compiler` | 2.53.1 | Hilt annotation processor (KSP) |
| `androidx.hilt:hilt-navigation-compose` | 1.2.0 | Hilt + Navigation Compose integration |
| `com.squareup.retrofit2:retrofit` | 2.11.0 | Type-safe HTTP client |
| `com.squareup.retrofit2:converter-gson` | 2.11.0 | Gson JSON converter for Retrofit |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | HTTP client |
| `com.squareup.okhttp3:logging-interceptor` | 4.12.0 | HTTP request/response logging |
| `androidx.datastore:datastore-preferences` | 1.1.1 | Key-value local storage |
| `io.coil-kt:coil-compose` | 2.7.0 | Image loading for Compose |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.9.0 | Android coroutines dispatcher |

### Gradle Plugins

| Plugin | Version |
|---|---|
| Android Gradle Plugin (AGP) | 8.13.2 |
| Kotlin Android | 2.0.21 |
| Kotlin Compose | 2.0.21 |
| Dagger Hilt Android | 2.53.1 |
| KSP (Kotlin Symbol Processing) | 2.0.21-1.0.27 |

---

*This document is part of the Expense Tracker App project documentation. See the parent README for an overview of all microservices and their interactions.*

