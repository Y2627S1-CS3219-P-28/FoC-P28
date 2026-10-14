<!--
AI Assistance Disclosure:
Tool: OpenAI Codex (GPT-5), date: 2026-10-14
Mode: Source inspection, contract documentation, and verification.
Scope: Prepared the Reporting Service implementation and pre-merge handoff from the branch source, tests, configuration, and current Order Service implementation.
Author review: Pending receiving-developer review.
-->

# Reporting Service implementation and pre-merge handoff

**Branch:** `feat/reporting-service`  
**Prepared:** 2026-10-14  
**Status:** Reporting implementation is locally verified. End-to-end integration is blocked by `REPORTING-ORDER-001`, because the two required Order Service association endpoints do not exist on `main`.

## 1. Purpose and current state

This branch adds a dedicated Reporting Service and an authenticated user-facing report form. Reporting Service owns report records and private photo attachments. It validates any submitted order reference against Order Service using the caller's Firebase bearer token; it does not trust a user ID, requester ID, courier ID, or role supplied by the browser.

The branch currently contains the infrastructure/setup commit `9fe321b` and the backend/frontend implementation commit `d26e8ce`. Before merge, rebase onto the Order Service change that implements the association contract in section 6, rerun the checks in section 11, and complete an authenticated end-to-end smoke test.

## 2. What was implemented

### Reporting backend

- Spring Boot 4.1.1 / Java 21 service under `reporting-service/`.
- Firebase JWT validation and public health/OpenAPI routes.
- User Service-backed admin role lookup with a short cache; mock admin mode remains available locally.
- Multipart report submission for `ORDER`, `COURIER`, `CREDITS`, and `GENERAL` categories.
- Authoritative order association lookup before storing a report with an `orderId`.
- Firestore report and attachment-metadata persistence in the `reports` collection.
- Private photo bytes in Google Cloud Storage; object names use generated report and attachment IDs rather than user filenames.
- Admin-only report listing, attachment download, and idempotent resolution.
- Standard API error envelopes and OpenAPI documentation.
- Unit, security, storage, HTTP client, controller, and Firestore Testcontainers tests with 80% line and branch coverage gates.

### Frontend

- `/reporting` authenticated form and sidebar navigation entry.
- Category, description, optional/required associated-order selection, and up to five image previews.
- The form loads every page of the caller's associated orders and deliberately does not filter by lifecycle status.
- A completed requester order displays a link that opens `/reporting?category=ORDER&orderId=<id>`.
- Multipart support in the shared API client; the browser sets the multipart boundary.
- Failed submissions preserve the entered description, order, and photos for retry.

### Routing, local runtime, and cloud infrastructure

- Gateway route for `/api/reports` with a 30 MiB request limit.
- Compose service on host port `8086`, Firestore database `reporting-local`, and Firebase Storage emulator on port `9199`.
- Reporting Service runtime identity, per-environment Firestore database detection, and private attachment buckets.
- Staging/production buckets are named `${PROJECT_ID}-foc-reporting-${ENVIRONMENT}`, enforce public-access prevention, and grant only the Reporting Service identity bucket-scoped object access.
- Deployment configuration uses internal Order and User Service URLs and Direct VPC egress.

## 3. Runtime dependency map

| Dependency | Why Reporting Service needs it | Required before startup or use |
| --- | --- | --- |
| Firebase Auth | Validates the bearer token and supplies `sub` as `reporterId` | Required for every protected endpoint |
| Order Service | Lists orders associated with the caller and authoritatively validates a selected order | **Blocking for the report form and every submission containing `orderId`** |
| User Service | Resolves the caller's roles for admin-only endpoints when `USER_SERVICE_MODE=http` | Required for admin list/download/resolve operations |
| Firestore | Stores report records and attachment metadata | Required for create, list, resolve, and download metadata lookup |
| Cloud Storage | Stores and retrieves private image bytes | Required when a report has photos and when an admin downloads one |
| Gateway | Routes browser `/api/reports` calls and accepts requests up to 30 MiB | Required for the normal browser/cloud path |
| Admin Service/UI | Future consumer of the admin APIs | Not required for service startup; an operational admin workflow is not implemented in this branch |

`GET /actuator/health` checks that the application is running; it does not proactively verify Order Service, User Service, Firestore, or Cloud Storage. A healthy container therefore does not prove that report submission or administration works end to end.

## 4. Reporting Service endpoints

| Method and path | Access | Request | Success response |
| --- | --- | --- | --- |
| `POST /api/reports` | Any authenticated caller | `multipart/form-data`; required JSON part `report`, optional repeated file part `photos` | `201` with `ReportResponse` |
| `GET /api/reports?page=1&size=20` | Admin only | 1-based `page`; `size` 1-100 | `200` with `PageResponse<ReportResponse>`, newest first |
| `GET /api/reports/{reportId}/attachments/{attachmentId}` | Admin only | No body | `200` image bytes with the saved media type, inline filename, and private/no-store caching |
| `PATCH /api/reports/{id}/status` | Admin only | JSON `{ "status": "RESOLVED" }` | `200` with the resolved report; repeating the request preserves the first `resolvedAt` |
| `GET /api/reports/docs` | Public | No body | Swagger UI |
| `GET /api/reports/v3/api-docs` | Public | No body | OpenAPI JSON |
| `GET /actuator/health` | Public | No body | Spring Boot health response |

There is intentionally no unauthenticated submission, requester-facing report history, delete operation, attachment URL, reopen transition, or admin UI in this branch.

## 5. Reporting request and response contracts

### Create report multipart request

The `report` part must have `Content-Type: application/json` and this structure:

```json
{
  "category": "ORDER",
  "description": "The delivered item was damaged.",
  "orderId": "order-123"
}
```

Rules:

- `category` is required and is exactly one of `ORDER`, `COURIER`, `CREDITS`, or `GENERAL`.
- `description` is required, must contain non-whitespace text, and is at most 2,000 characters. It is trimmed before persistence.
- `orderId` is required for `ORDER` and `COURIER`; it is optional for `CREDITS` and `GENERAL`.
- A nonblank `orderId` is never accepted on browser trust alone. Reporting Service calls Order Service as described in section 6 before persisting it.
- `photos` may occur zero to five times. Each file must be non-empty, at most 5 MiB, and have matching file bytes and MIME type for `image/jpeg`, `image/png`, or `image/webp`.
- The application accepts at most 30 MiB for the whole multipart request. Do not set the multipart `Content-Type` header manually in browser code; the browser must add the boundary.

Example:

```bash
curl -X POST "${GATEWAY_URL}/api/reports" \
  -H "Authorization: Bearer ${ID_TOKEN}" \
  -F 'report={"category":"ORDER","description":"The delivered item was damaged.","orderId":"order-123"};type=application/json' \
  -F 'photos=@damage.jpg;type=image/jpeg'
```

### Report response

```json
{
  "id": "f69fd4b1-cb45-4e6b-a3ac-e0f37b6628c4",
  "reporterId": "firebase-uid",
  "category": "ORDER",
  "description": "The delivered item was damaged.",
  "orderId": "order-123",
  "courierId": "courier-uid",
  "status": "OPEN",
  "createdAt": "2026-10-14T06:00:00Z",
  "resolvedAt": null,
  "attachments": [
    {
      "id": "35dde4dd-220b-42a4-9734-42801a7e7e4d",
      "filename": "damage.jpg",
      "contentType": "image/jpeg",
      "size": 284931,
      "uploadedAt": "2026-10-14T06:00:00Z"
    }
  ]
}
```

`courierId` is copied from the authoritative Order Service response at submission time and may be `null`. Attachment metadata never exposes the private object key; admins fetch bytes through the attachment endpoint.

### Page response

```json
{
  "items": [],
  "page": 1,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

### Error response

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "An order is required for this category.",
  "path": "/api/reports",
  "timestamp": "2026-10-14T06:00:00Z",
  "details": [
    { "field": "orderId", "message": "An order is required for this category." }
  ]
}
```

Expected codes are `400 VALIDATION_ERROR`, `401 UNAUTHENTICATED`, `403 FORBIDDEN`, `404 NOT_FOUND`, `503 SERVICE_UNAVAILABLE`, and `500 INTERNAL_ERROR`. A missing/unassociated order becomes Reporting Service `404`; Order, User, Firestore, or Storage dependency failures become `503`.

## 6. Required Order Service changes (`REPORTING-ORDER-001`)

The current Order Service on `main` exposes `GET /api/orders/mine?mode=requester|courier&userId=...`. That endpoint is not sufficient because the reporting workflow needs a bearer-derived union of requester and courier associations and must not trust client-supplied identity or mode.

Order Service must add both endpoints below, document them in OpenAPI, and cover them with positive and negative contract/security tests.

### 6.1 List the caller's associated orders

```http
GET /api/orders/mine/associated?page=1&size=100
Authorization: Bearer <Firebase ID token>
Accept: application/json
```

Required behavior:

- Derive the caller solely from the validated bearer token. Do not accept `userId`, `requesterId`, `courierId`, or `mode` from the client.
- Return orders where the caller is the requester or an associated courier.
- Include every reportable status; Reporting Service deliberately applies no lifecycle filtering. The current frontend expects `OPEN`, `ACCEPTED`, `IN_PROGRESS`, `PICKED_UP`, `DELIVERED`, `COMPLETED`, `CANCELLED`, `ABORTED`, and `EXPIRED` when applicable.
- Use 1-based pagination, allow `size` up to 100, and return the standard page envelope.
- The frontend follows `totalPages` until all pages are loaded.
- Return newest first unless the Order Service contract deliberately standardizes another stable order.

The branch currently consumes the existing `OrderPageResponse` shape. Each item should therefore remain an `OrderResponse`. At runtime the reporting selector specifically reads `id`, `itemDescription`, and `status`; keeping the complete response avoids a second nearly-identical public DTO.

```json
{
  "items": [
    {
      "id": "order-123",
      "attemptId": null,
      "requesterId": "requester-uid",
      "courierId": "courier-uid",
      "itemDescription": "Collect a parcel",
      "pickupSupplierId": "supplier-a",
      "deliverySupplierId": "supplier-b",
      "offeredCredits": 10,
      "status": "COMPLETED",
      "createdAt": "2026-10-13T03:00:00Z",
      "expiresAt": "2026-10-13T05:00:00Z",
      "deliveryTimeLimitMinutes": 30,
      "version": 5,
      "originalOrderId": null,
      "repostedOrderId": null
    }
  ],
  "page": 1,
  "size": 100,
  "totalItems": 1,
  "totalPages": 1
}
```

### 6.2 Validate one association authoritatively

```http
GET /api/orders/mine/associated/{orderId}
Authorization: Bearer <Firebase ID token>
Accept: application/json
```

Required behavior:

- Derive the caller from the token and return `200` only when that caller is the requester or an associated courier for the requested order.
- Return `404 NOT_FOUND` when the order does not exist or is not associated with the caller. Using the same response prevents order-ID enumeration.
- Return the requested canonical order ID and the authoritative courier ID. `courierId` is nullable.
- Do not add lifecycle eligibility filtering; all statuses remain reportable.

Exact response consumed by Reporting Service:

```json
{
  "id": "order-123",
  "courierId": "courier-uid"
}
```

Reporting Service forwards the original `Authorization` header, uses a five-second default connect/read timeout, maps Order Service `404` to Reporting `404`, and maps every other Order Service HTTP/client failure to Reporting `503`.

### 6.3 Order contract decision still needed for aborted courier attempts

Order Service can expose an immutable `ABORTED` courier attempt with the same business `id` as a reopened current order and distinguish it with `attemptId`. The current report submission payload carries only `orderId`, and the authoritative single-order response carries only `id` and `courierId`.

Before declaring the association integration complete, the Order and Reporting owners must decide and test one of these behaviors:

1. Reports attach only to the business order, and the association endpoint de-duplicates attempts by `id` with a clearly defined courier attribution rule; or
2. Reports may target a historical courier attempt, in which case the create payload and validation endpoint must add `attemptId` and the Reporting data model must persist it.

Until this is resolved, selecting an `ABORTED` history item can be ambiguous if the same business order also has a current row or a later courier.

## 7. Persistence and private attachment structure

Firestore collection: `reports`. Each document ID equals the generated report ID and contains:

```text
id, reporterId, category, description, orderId, courierId,
status, createdAt, resolvedAt, attachments[]
```

Each attachment metadata entry contains:

```text
id, objectKey, filename, contentType, size, uploadedAt
```

Cloud Storage object keys use `reports/{reportId}/{attachmentId}`. Buckets are private and the service streams objects to authorized admins. If upload or Firestore persistence fails, the service attempts to delete uploaded objects; cleanup is best effort, so operations should still monitor for rare orphaned objects after infrastructure failures.

## 8. Configuration

| Variable | Purpose | Local/default behavior |
| --- | --- | --- |
| `PORT` | HTTP port | `8080`; Compose maps host `8086` |
| `FIRESTORE_PROJECT_ID` | Firestore project | `demo-foc` |
| `FIRESTORE_DATABASE_ID` | Reporting database | `reporting-local` |
| `FIRESTORE_EMULATOR_HOST` | Local Firestore emulator | Set by Compose |
| `STORAGE_PROJECT_ID` | Storage project | Falls back to Firestore project |
| `REPORTING_STORAGE_BUCKET` | Private attachment bucket | `demo-foc-reporting-local` |
| `STORAGE_EMULATOR_HOST` | Local Storage emulator | `http://firebase-emulator:9199` in Compose |
| `FIREBASE_AUTH_PROJECT_ID` | Expected Firebase issuer/audience project | Repository standard |
| `FIREBASE_AUTH_EMULATOR_HOST` | Local Auth emulator | Set by Compose |
| `USER_SERVICE_MODE` | `mock` or `http` role lookup | Compose uses `http` |
| `USER_SERVICE_URL` | User Service base URL | Required in HTTP mode |
| `MOCK_ADMIN_EMAILS` | Admin emails in mock mode | Repository standard |
| `USER_SERVICE_TIMEOUT` | Role lookup timeout | `3s` |
| `USER_SERVICE_ROLE_CACHE_TTL` | Per-user role cache | `30s` |
| `ORDER_SERVICE_URL` | Order Service base URL | `http://order-service:8080` in Compose |
| `ORDER_SERVICE_TIMEOUT` | Order lookup timeout | `5s` |
| `CORS_ORIGINS` | Browser origins | Repository standard |

## 9. Security and ownership notes

- All report APIs except health and API documentation require a valid Firebase bearer token.
- Any authenticated user can submit a report. The server takes `reporterId` from JWT `sub`.
- List, download, and resolve operations require the `ADMIN` role returned by `GET /api/users/role-context` or mock admin configuration.
- Roles sent by the frontend are ignored.
- Order ownership/association remains Order Service-owned; Reporting Service never reads the Order database.
- Report records and attachment metadata remain Reporting Service-owned in Firestore.
- Attachment bytes remain Reporting Service-owned in its private bucket.
- Admin Service is a future API consumer and must not write the Reporting database or bucket directly.

## 10. Known follow-ups and operational limitations

- **Merge blocker:** Implement and merge the two Order association endpoints, resolve the aborted-attempt identity decision, rebase this branch, and verify the real contract.
- **Cloud prerequisite:** The CI/CD owner must run `infra/gcp/bootstrap.sh` so the Reporting runtime identity, `reporting-staging`/`reporting-production` Firestore databases, and both private attachment buckets exist before the cloud infrastructure check can pass.
- **Admin workflow:** Backend admin APIs exist, but no admin page/service integration is included.
- **Pagination scalability:** The current Firestore repository reads and sorts the full `reports` collection in memory before slicing a page. The wire contract is paginated, but large production volume should move to an indexed Firestore query plus an appropriate count strategy.
- **Readiness depth:** Health can be green while lazy dependencies are unavailable; use functional smoke tests during rollout.
- **Attachment consistency:** Cross-service/object-store rollback is best effort rather than transactional.

## 11. Pre-merge and rollout checklist

- [ ] Order Service implements both endpoints in section 6 with Firebase authentication, OpenAPI summaries/responses, and positive/negative tests.
- [ ] Order and Reporting owners resolve the `ABORTED` attempt ambiguity and update both contracts/tests if `attemptId` is required.
- [ ] Order endpoint changes are merged to `main`; this branch is rebased and conflicts are resolved.
- [ ] Add or run a real Reporting-to-Order contract/integration test for list, single lookup, unassociated `404`, invalid token, and dependency failure.
- [ ] Run `cd reporting-service && ./mvnw verify` with Docker available.
- [ ] Run `cd frontend && npm test && npm run lint && npm run typecheck && npm run build`.
- [ ] Run `docker compose config` and build Reporting Service, frontend, and gateway images.
- [ ] Start the local stack and verify health, OpenAPI, authenticated form loading, all association pages/statuses, submission with and without photos, admin listing, private download, and idempotent resolution.
- [ ] Confirm non-admin `403`, missing/invalid token `401`, unrelated order `404`, invalid image `400`, and stopped Order/Storage dependency `503` paths.
- [ ] Have the CI/CD owner provision cloud infrastructure and rerun the Cloud infrastructure job.
- [ ] Deploy to staging; verify gateway-only access, private bucket policy, real User role lookup, real Order association, report creation, attachment download, and logs.
- [ ] Obtain human review of API/security/data ownership and only then merge through a PR with CI passing.

## 12. Verification captured for this handoff

Fresh on 2026-10-14:

- `reporting-service/./mvnw -B -ntp verify`: **29 tests passed**, including Firestore Testcontainers; JaCoCo line and branch gates passed.
- Reporting frontend focused suites: **4 files / 8 tests passed**.
- Scoped ESLint for changed reporting/API files: passed.
- Full frontend `npm run typecheck`: passed.

The earlier branch implementation record also reports the full frontend suite, production frontend build, service/frontend/gateway image builds, Compose validation, and patch validation as passing. Those broader checks should still be rerun after the Order Service integration and final rebase rather than treated as final merge evidence.
