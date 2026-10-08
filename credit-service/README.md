<!--
AI Assistance Disclosure:
Tool: OpenAI Codex (GPT-5), date: 2026-09-25
Mode: Documentation generation and formatting.
Scope: Documented team-finalized Credit Service behavior, interfaces, data contracts, and operating instructions.
Author review: I reviewed for correctness and edited where needed.
-->

# Credit Service

The Credit Service owns Friend on Campus credit accounts, balances, reservations and the
immutable credit ledger. Sprint 1 allocates exactly 50 credits after registration, lets an
authenticated user view their own balances, and reserves usable credits before an order becomes open.

## Run

```bash
# Whole system through the gateway at http://localhost:8080
docker compose up --build

# Service from source against local PostgreSQL and the Auth emulator
docker compose up -d credit-postgres firebase-emulator
PORT=8084 SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/credit_service \
SPRING_DATASOURCE_USERNAME=credit_dev SPRING_DATASOURCE_PASSWORD=credit_dev_password \
FIREBASE_AUTH_EMULATOR_HOST=localhost:9099 ./mvnw spring-boot:run
```

- Swagger UI: `/api/credits/docs`
- OpenAPI JSON: `/api/credits/v3/api-docs`
- Health: `/actuator/health`

## Sprint 1 API

Every business endpoint requires a Firebase ID token. The token subject is used directly for
`GET /api/credits/me` and must match the `userId` or `requesterId` supplied to write operations.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/credits/registration-facts` | Idempotently allocate 50 credits after registration. |
| `GET` | `/api/credits/me` | Return the authenticated user's total, reserved and usable balances. |
| `PUT` | `/api/credits/orders/{orderId}/reservation` | Atomically reserve credits, using `orderId` as the idempotency key. |
| `GET` | `/api/credits/orders/{orderId}/reservation` | Recover the authenticated requester's reservation status. |

Reservations use `RESERVED`, `REFUNDED` and `PAID`; Sprint 1 creates only `RESERVED`.
There is no gift, withdrawal, top-up, direct balance update, or generic CRUD API.

## Data model

Credit Service exclusively owns a PostgreSQL database for each environment. Flyway creates and
validates the schema at application startup:

| Table | Primary key | Purpose |
|---|---|---|
| `credit_accounts` | `user_id` | Authoritative total and reserved balances. Usable balance is derived. |
| `credit_reservations` | `order_id` | One idempotent reservation per order. |
| `credit_idempotency_records` | `(operation, idempotency_key)` | Event and command replay protection. |
| `credit_ledger` | `entry_id` | Immutable evidence of every successful balance change. |

Account, reservation/idempotency, and ledger writes commit in one PostgreSQL transaction.
Account rows are locked during balance mutations so concurrent reservations cannot overdraw an account.

## Test

```bash
./mvnw verify
```

Integration tests use a Testcontainers PostgreSQL instance. The build enforces at least 80%
line and branch coverage and writes `target/openapi.json`.
