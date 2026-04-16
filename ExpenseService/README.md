# ExpenseService

Spring Boot microservice that stores and retrieves user expenses. Exposes a small REST API, consumes Kafka events, and persists to MySQL. Default port: `9820`.

## Overview
- REST: list expenses and create/update records; headers drive identity (no built-in auth, rely on `X-User-ID` from upstream). Structured error responses via `GlobalExceptionHandler`.
- Kafka consumer: listens on `expense_service` and writes events to MySQL.
- JPA/Hibernate manages the `expense` table; currencies default to `INR` when omitted.

## Tech Stack
| Component | Details |
|-----------|---------|
| Java | 21 |
| Spring Boot | 3.5.10 |
| Spring Data JPA, Validation | starters |
| Spring Kafka | starter with custom deserializer |
| MySQL | 8.x (MySQL8Dialect) |
| Build | Gradle wrapper |
| Container | eclipse-temurin:21-jre base |

## API (base `/expense/v1`)
**Public AWS Base URL:** `http://arthabit-api.sakshamnarvar.tech/expense-service`  
**Local Base URL:** `http://localhost:9820`

- `GET /getExpense` — header `X-User-ID` required; returns all expenses for that user.
- `POST /addExpense` — header `X-User-ID`; body `{ amount (required), merchant (required), currency (optional, default INR), notes (optional), category (optional), fund_source (optional) }`; returns created expense with `external_id`.
- `POST /updateExpense` — header `X-External-ID` (existing expense external id); body may include `amount`, `merchant`, `currency`, `created_at`, `notes`, `category`, `fund_source` to patch fields.

Example create request:
```json
{
  "amount": 150.00,
  "merchant": "Amazon",
  "currency": "usd",
  "notes": "work lunch",
  "category": "meals",
  "fund_source": "corporate-card"
}
```

## Data Model
`expense` table (managed by JPA): `id` (PK), `external_id` (UUID), `user_id`, `amount`, `merchant`, `currency` (defaults to `INR`), `notes`, `category`, `fund_source`, `created_at` (timestamp).

## Kafka
- Topic: `expense_service`
- Group: `expense-info-consumer-group`
- Deserializer: custom `ExpenseDeserializer` for `ExpenseDto`
- Event payload shape (from upstream service): `{ amount, user_id, merchant, currency, fund_source, created_at }`. `user_id` and `amount` are mandatory. Other fields like `external_id`, `notes`, `category` remain null or get auto-populated when persisted.
- Behavior: `ExpenseConsumer` persists each event via `ExpenseService`; idempotency/transactions still TODO.

## Configuration
Key application properties (defaults shown):
- `server.port=9820`
- `spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DB:expense_service}`
- `spring.datasource.username=root` / `spring.datasource.password=narvar007` (externalize for prod)
- `spring.jpa.hibernate.ddl-auto=update`
- `spring.kafka.bootstrap-servers=${KAFKA_HOST:localhost}:${KAFKA_PORT:9092}`
- `spring.kafka.topic-json.name=expense_service`
- `auth-service.base-url=http://${AUTH_SERVICE_HOST:auth-service}:${AUTH_SERVICE_PORT:9898}` (currently unused in controllers)

Env vars that override defaults: `KAFKA_HOST`, `KAFKA_PORT`, `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DB`, `AUTH_SERVICE_HOST`, `AUTH_SERVICE_PORT`.

## Run Locally
Prereqs: Java 21, MySQL 8, Kafka broker running.

```bash
./gradlew build
./gradlew bootRun
```

The app listens on `http://localhost:9820`.

## Tests
```bash
./gradlew test
```

## Docker
```bash
docker build -t expense-service .
docker run -p 9820:9820 \
  -e KAFKA_HOST=kafka -e KAFKA_PORT=9092 \
  -e MYSQL_HOST=mysql -e MYSQL_PORT=3306 -e MYSQL_DB=expense_service \
  expense-service
```

## Maintenance / TODO
- Add idempotency + transactional handling for Kafka consumer.
- Externalize DB credentials and secrets.
- Add date-range filtering and richer update support if needed.
