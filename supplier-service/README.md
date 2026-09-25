# supplier-service

Owner: Zheng Jiongjie

The catalogue of campus stores, facilities and landmarks (FoC calls them *suppliers*) that
errands are picked up from and delivered to. Spring Boot 4.1.1 on Java 21 with Firestore.
It follows the repo conventions in [AGENTS.md](../AGENTS.md).

- **API docs (OpenAPI):** `/api/suppliers/docs` (Swagger UI) and `/api/suppliers/v3/api-docs`
- **Health:** `/actuator/health` (with readiness and liveness groups)

## Run

```bash
# Whole system (recommended), from the repo root: UI + gateway at http://localhost:8080
docker compose up --build

# Just this service from source, against the containerised emulators
docker compose up -d firebase-emulator
PORT=8082 FIRESTORE_EMULATOR_HOST=localhost:8090 FIRESTORE_DATABASE_ID=supplier-local \
FIREBASE_AUTH_EMULATOR_HOST=localhost:9099 MOCK_ADMIN_EMAILS=admin@u.nus.edu \
SUPPLIER_SEED_FILE=../data/csv/supplier-seed-data.csv ./mvnw spring-boot:run
```

### Calling the API without the UI

```bash
# Sign up / sign in against the Auth emulator and keep the ID token
TOKEN=$(curl -s -X POST 'http://localhost:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=demo-api-key' \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@u.nus.edu","password":"Passw0rd!","returnSecureToken":true}' | jq -r .idToken)

curl -H "Authorization: Bearer $TOKEN" 'http://localhost:8080/api/suppliers?q=coffee'
curl -H "Authorization: Bearer $TOKEN" 'http://localhost:8080/api/suppliers?lat=1.2942&lng=103.774&radius=300'
```

Accounts listed in `MOCK_ADMIN_EMAILS` are admins; everyone else is a requester and a courier.

## Test

```bash
./mvnw verify    # unit + integration tests (Testcontainers Firestore emulator) + JaCoCo >= 80% gate
```

You need Docker for the integration tests. The coverage report is written to
`target/site/jacoco/index.html`.

## API

All endpoints need `Authorization: Bearer <Firebase ID token>`. Errors use the shared
envelope (`status`, `error`, `message`, `path`, `timestamp`, `details`).

| Method | Path | Who | Purpose (backlog) |
| --- | --- | --- | --- |
| GET | `/api/suppliers` | any role | Search, filter, sort, paginate (F3.1, F4) |
| GET | `/api/suppliers/{id}` | any role | One supplier, including inactive ones (F3.2, F6.3.3) |
| GET | `/api/suppliers/types` | any role | Types in use, for filters |
| GET | `/api/suppliers/permissions` | any role | Caller's roles, so the UI can hide admin actions |
| GET | `/api/suppliers/{id}/status` | any role / services | Does the supplier exist, and is it active? (F5.3) |
| POST | `/api/suppliers/validate` | any role / services | Validate an errand's pickup + delivery pair (F5.1) |
| POST | `/api/suppliers/lookup` | any role / services | Resolve many IDs at once, e.g. for order history (F5.2) |
| POST | `/api/suppliers` | admin | Create (F6.1) |
| PATCH | `/api/suppliers/{id}` | admin | Partial update, including reactivation (F6.2) |
| DELETE | `/api/suppliers/{id}` | admin | Deactivate; records are never hard-deleted (F6.3) |

`GET /api/suppliers` query parameters:

| Parameter | Meaning |
| --- | --- |
| `q` | Partial, case-insensitive match on name, building or location description |
| `type` | Exact type (case-insensitive); repeat it for several types |
| `building` | Partial, case-insensitive building match |
| `openNow` | `true` for suppliers open at request time (Singapore time) |
| `lat`, `lng`, `radius` | Distance from a point, optionally limited to `radius` metres |
| `status` | `active` (default), or `inactive` / `all` (admin only, otherwise 403) |
| `sort`, `order` | `name` (default), `type`, `building`, `distance`, `updatedAt`; `asc` or `desc` |
| `page`, `size` | 1-based page number; `size` is 1–100 (default 12) |

An empty result is `200` with `items: []` and a `message` (F4.4).

## Data model (Firestore, database `supplier-<environment>`)

`suppliers/{id}` holds one document per supplier, keyed by an auto-generated ID that is never reused:

| Field | Type | Notes |
| --- | --- | --- |
| `name`, `type`, `building` | string | required; `type` follows the seed CSV (Food, Food/Coffee, Printing, Shopping, ...) |
| `floor`, `locationDescription`, `imageUrl` | string / null | optional |
| `latitude`, `longitude` | number | required |
| `openingTime`, `closingTime` | `"HH:mm"` | Singapore time; a closing time before the opening time means it closes after midnight |
| `active` | boolean | only active suppliers can be used for new errands |
| `source` | `SEED` / `ADMIN` | who created it |
| `createdAt`, `updatedAt` | timestamp | |

`supplierKeys/{sha256(name + building)}` → `{ supplierId }` is a uniqueness index. Firestore
has no unique constraints, so creates and renames claim this document in the same
transaction as the supplier write (F1.1.2). Names are compared ignoring case, extra spaces
and curly quotes.

## Design notes

- **Queries:** the whole catalogue (tens to hundreds of records) is cached in memory for at
  most 60 seconds (NFR5.3.2), and writes clear the cache immediately. Search, filtering,
  distance and sorting run over this cached list, because Firestore cannot do
  case-insensitive substring search or distance sorting. Reads by ID always go to the
  database, so other services see current data (F5.2.2).
- **Seeding (F2):** `SUPPLIER_SEED_FILE` is loaded before the web server starts, so readiness
  means "catalogue loaded".
  - Rows upsert by name + building, or by an optional `Id` column.
  - Reloading the same file changes nothing.
  - Invalid rows are skipped and logged with their row number.
  - A missing or malformed file stops startup.
  - The provided CSV contains Windows-1252 apostrophes, and GitHub page links for images. The
    loader handles both: it decodes the apostrophes and rewrites the links to direct image URLs.
- **Deactivating seed records:** seed records missing from the file are deactivated, never deleted.
  Admin-created records are left alone, so a restart can't undo an administrator's changes.
- **Access control:**
  - Spring Security validates the Firebase ID token (issuer, audience, expiry, Google signing keys).
  - Roles come from the User Service and become `ROLE_*` authorities.
  - Management endpoints use `@PreAuthorize("hasRole('ADMIN')")`.
  - Until the User Service role API exists, `USER_SERVICE_MODE=mock` gives everyone
    requester + courier and grants admin to `MOCK_ADMIN_EMAILS`.
  - Set `USER_SERVICE_MODE=http` to call `GET {USER_SERVICE_URL}/api/users/{uid}/roles` instead.
- **Deployment:** Cloud Run as `supplier-service-<environment>`, running as its own service
  account, which can only access its own Firestore databases. The seed CSV is uploaded to the
  environment's config bucket and mounted read-only (F2.1.1). See [docs/ci-cd.md](../docs/ci-cd.md).

## Configuration

| Variable | Default | Meaning |
| --- | --- | --- |
| `PORT` | 8080 | HTTP port |
| `FIRESTORE_PROJECT_ID` / `FIRESTORE_DATABASE_ID` | `demo-foc` / `(default)` | Where suppliers are stored |
| `FIRESTORE_EMULATOR_HOST` | – | Use the Firestore emulator |
| `FIREBASE_AUTH_PROJECT_ID` | `demo-foc` | Firebase project that issues ID tokens |
| `FIREBASE_AUTH_EMULATOR_HOST` | – | Accept Auth emulator tokens (local only) |
| `USER_SERVICE_MODE` / `USER_SERVICE_URL` | `mock` / – | Role lookup |
| `MOCK_ADMIN_EMAILS` | – | Admins while in mock mode |
| `SUPPLIER_SEED_FILE` | – | CSV loaded at startup |
| `SUPPLIER_CACHE_TTL` | `60s` | Catalogue cache TTL (capped at 60 s) |
| `CORS_ORIGINS` | `http://localhost:3000,http://localhost:8080` | Allowed browser origins |
