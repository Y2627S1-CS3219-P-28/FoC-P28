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
It also implements the merged Order Service contract for courier assignment, reopen holds,
refunds, and completion settlement.

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
Credit resolves application roles independently through User Service. Courier assignment and
hold-for-reopen require `ROLE_COURIER` in addition to their existing identity and reservation
ownership checks. Local Compose uses mock roles; cloud deployments use
`GET /api/users/role-context` with the caller's bearer token and fail closed when role lookup is
unavailable. These remain caller-authenticated gateway endpoints; restricting them to trusted
Order-to-Credit service calls is separate work.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/credits/registration-facts` | Idempotently allocate 50 credits after registration. |
| `GET` | `/api/credits/me` | Return the authenticated user's total, reserved and usable balances. |
| `GET` | `/api/credits/me/transactions?page=1&size=20` | Return the authenticated user's newest credit transactions. |
| `PUT` | `/api/credits/orders/{orderId}/reservation` | Atomically reserve credits, using `orderId` as the idempotency key. |
| `GET` | `/api/credits/orders/{orderId}/reservation` | Recover the authenticated requester's reservation status. |
| `PUT` | `/api/credits/orders/{orderId}/courier-assignment` | Idempotently associate the authenticated courier before Order persists acceptance. |
| `POST` | `/api/credits/orders/{orderId}/hold-for-reopen` | Clear the assigned courier without releasing the requester's held credits. |

Reservations use `RESERVED`, `REFUNDED` and `PAID`.
There is no gift, withdrawal, top-up, direct balance update, or generic CRUD API.

## Order outcome events

Google Cloud Pub/Sub delivers each finalized Order event stream to its authenticated, typed push
endpoint:

| Event | Push endpoint | Credit action |
|---|---|---|
| `OpenOrderRefundTaskEvent` | `POST /api/credits/internal/order-events/open-refund` | Release the requester's reservation after `CANCELLED` or `EXPIRED`. |
| `OrderCompletionTaskEvent` | `POST /api/credits/internal/order-events/completion` | Debit the requester's reserved/total balance and credit the recorded courier. |

These endpoints use a separate Google OIDC security chain from the Firebase-authenticated user API.
It validates the push service-account email, token issuer and audience. Each endpoint invokes its
designated handler directly, while checking the subscription and declared event type as routing
guardrails. The receiver requires compact schema version 2 in the Pub/Sub attributes and verifies
the attribute event ID/type against the seven-field body. Credit resolves the requester from its
reservation and verifies the event amount and courier against that authoritative record.

`AcceptedOrderCancellationTaskEvent` remains an Order/User penalty event and is not consumed by
Credit. When an expired accepted-order cancellation requires a financial refund, Order emits a
separate `OpenOrderRefundTaskEvent` with `orderStatus=EXPIRED`.

The service records `eventId` and a payload hash in the same PostgreSQL transaction as reservation,
account, and ledger mutations. It returns HTTP 204 only after that transaction succeeds; any
non-success response is retried by Pub/Sub. After ten unsuccessful deliveries, the subscription
forwards the message to the environment's shared Credit dead-letter topic for inspection/recovery.

An infrastructure owner provisions staging after the Cloud Run service exists:

```bash
infra/gcp/configure-order-pubsub.sh staging    # the environment's own Order topics
infra/gcp/configure-credit-pubsub.sh staging
```

Production provisioning remains disabled in `infra/gcp/project.env` until staging verification
passes and the production-promotion change explicitly enables it.

## Data model

Credit Service exclusively owns a PostgreSQL database for each environment. Flyway creates and
validates the schema at application startup:

| Table | Primary key | Purpose |
|---|---|---|
| `credit_accounts` | `user_id` | Authoritative total and reserved balances. Usable balance is derived. |
| `credit_reservations` | `order_id` | One idempotent reservation per order. |
| `credit_idempotency_records` | `(operation, idempotency_key)` | Event/command source, payload hash, resource, and replay protection. |
| `credit_ledger` | `entry_id` | Immutable evidence of every successful balance change. |

Refund ledger rows persist `CANCELLATION` or `EXPIRY`. Historical rows without a reason are exposed
as a generic refund. Transaction history is ordered by occurrence time, creation time, then entry ID.

Account, reservation/idempotency, and ledger writes commit in one PostgreSQL transaction.
Account rows are locked during balance mutations so concurrent reservations cannot overdraw an account.

## Test

```bash
./mvnw verify
```

Integration tests use a Testcontainers PostgreSQL instance. The build enforces at least 80%
line and branch coverage and writes `target/openapi.json`.
