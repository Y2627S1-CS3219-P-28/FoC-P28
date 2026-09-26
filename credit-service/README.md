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

# Service from source against the emulators
docker compose up -d firebase-emulator
PORT=8084 FIRESTORE_EMULATOR_HOST=localhost:8090 FIRESTORE_DATABASE_ID=credit-local \
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

Credit Service exclusively owns the `credit-<environment>` Firestore database:

| Collection | Document ID | Purpose |
|---|---|---|
| `creditAccounts` | `userId` | Authoritative total and reserved balances. Usable balance is derived. |
| `creditReservations` | `orderId` | One idempotent reservation per order. |
| `processedEvents` | `eventId` | Registration and future outcome-event replay protection. |
| `creditLedger` | `entryId` | Immutable evidence of every successful balance change. |

Account, reservation/event, and ledger writes commit in one Firestore transaction.

## Test

```bash
./mvnw verify
```

Integration tests use a Testcontainers Firestore emulator. The build enforces at least 80%
line and branch coverage and writes `target/openapi.json`.
