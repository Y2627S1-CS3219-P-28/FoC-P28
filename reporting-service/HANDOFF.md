<!--
AI Assistance Disclosure:
Tool: OpenAI Codex, date: 2026-10-27
Mode: Implementation and contract documentation.
Scope: Documented active-order disputes, courier responses, and blocking Order Service APIs.
Author review: Pending receiving-developer review.
-->

# Reporting Service handoff: active-order disputes

**Branch:** `feat/reporting-service`

**Prepared:** 2026-10-27

**Integration status:** blocked by `REPORTING-ORDER-001` until both Order Service APIs in section 2 are implemented, secured, included in Order Service OpenAPI, and contract-tested.

## 1. Implemented behavior

- `ORDER` and `COURIER` reports can be created only against an authenticated requester's active order.
- Reporting generates a stable report ID before asking Order Service to atomically validate the order and place that report's completion hold.
- An assigned courier receives one response opportunity ending exactly 24 hours after report creation.
- Reports move through `AWAITING_COURIER_RESPONSE`, `READY_FOR_REVIEW`, and `RESOLVED`.
- A legacy Firestore record whose persisted status is `OPEN` is read as `READY_FOR_REVIEW`; no data migration is required.
- An `OPEN` `ORDER` report has no respondent/deadline and enters `READY_FOR_REVIEW` directly.
- `CREDITS` and `GENERAL` reports reject `orderId`, do not call Order Service, and enter `READY_FOR_REVIEW` directly.
- Expired response windows are reconciled using the Reporting Service clock during list, detail, response, and resolution operations. At the deadline, a response is rejected with `409 CONFLICT`.
- Admin resolution is allowed only from `READY_FOR_REVIEW`. For order-linked reports, Reporting releases the existing report-specific Order hold before persisting `RESOLVED`.
- Report details and both reporter/courier attachments are limited to the reporter, reported courier, or an administrator.

The frontend `/reporting` hub contains `Report issue`, `Submitted`, and `Against me` tabs. Its order selector uses only the active-order list API. Visible history views poll every five seconds. Delivered-order completion now opens a confirmation explaining that issues must be reported before completion; completed-order report links/prefill were removed.

## 2. Blocking Order Service APIs (`REPORTING-ORDER-001`)

These contracts are owned by the separate Order Service sprint. No Order Service source changes are included here. Authenticated end-to-end verification must remain deferred until both contracts land.

### 2.1 List the authenticated requester's active orders

```http
GET /api/orders/mine/active?page=1&size=100
Authorization: Bearer <Firebase ID token>
Accept: application/json
```

Required behavior:

- Derive the requester solely from the validated token. Accept no client-supplied user/requester ID.
- Return only that requester's `OPEN`, `ACCEPTED`, `IN_PROGRESS`, `PICKED_UP`, and `DELIVERED` orders.
- Exclude terminal orders before pagination; do not return complete history for consumers to filter.
- Use the standard 1-based page envelope, `size <= 100`, and stable newest-first ordering.
- Return normal display fields including `id`, `requesterId`, nullable `courierId`, `itemDescription`, and `status`.
- Document the operation and its 2xx/error responses in Order Service OpenAPI.
- Contract-test authentication, caller scoping, active/terminal filtering before pagination, stable ordering, and page bounds.

Example response:

```json
{
  "items": [
    {
      "id": "order-123",
      "requesterId": "requester-uid",
      "courierId": "courier-uid",
      "itemDescription": "Collect a parcel",
      "status": "DELIVERED"
    }
  ],
  "page": 1,
  "size": 100,
  "totalItems": 1,
  "totalPages": 1
}
```

The reporting frontend uses only this endpoint for its selector and follows `totalPages`. It does not call `/api/orders/mine`, `/associated`, or any order-history endpoint and does not locally filter terminal orders.

### 2.2 Atomically validate ownership/status and place the report hold

```http
PUT /api/orders/mine/active/{orderId}/report-holds/{reportId}
Authorization: Bearer <Firebase ID token>
Content-Type: application/json

{
  "requireAssignedCourier": true
}
```

Required behavior:

- In one database transaction, lock/read the order and verify that:
  - the authenticated caller is its requester;
  - the current status is one of `OPEN`, `ACCEPTED`, `IN_PROGRESS`, `PICKED_UP`, or `DELIVERED`;
  - a courier is assigned when `requireAssignedCourier` is `true`;
  - `reportId` is not bound to a different order.
- Place the report-specific completion hold in that same transaction.
- Treat an exact retry for the same requester, order, and report ID as idempotent and return the same authoritative snapshot.
- Use indistinguishable `404 NOT_FOUND` responses for missing, unowned, or inactive orders.
- Return `409 CONFLICT` for a missing required courier or a conflicting report binding.
- Document the operation and `200`, `401`, `404`, `409`, and dependency responses in Order Service OpenAPI.
- Contract-test ownership, every active/terminal state, courier-required behavior, idempotent retry, conflicting bindings, and hold persistence.

Authoritative success response:

```json
{
  "id": "order-123",
  "requesterId": "requester-uid",
  "courierId": "courier-uid",
  "status": "DELIVERED",
  "reportId": "report-123",
  "isCompletionHold": true
}
```

Reporting calls this endpoint with:

- `requireAssignedCourier: false` for `ORDER`, so an `OPEN` order issue can go directly to administrator review.
- `requireAssignedCourier: true` for `COURIER`.

Reporting returns `201 Created` only after validating this snapshot and persisting the report. Order `404` maps to Reporting `404 NOT_FOUND`; Order `409` maps to Reporting `409 CONFLICT`; other HTTP/client failures map to `503 SERVICE_UNAVAILABLE` using the standard Reporting error envelope.

There is deliberately no `GET /api/orders/mine/active/{orderId}` dependency. A separate read would introduce a validation-to-hold race in which the order could complete or change ownership-relevant state between validation and hold placement.

### Existing release operation

Admin resolution continues to use the existing report-specific release operation:

```http
PUT /api/orders/admin/{orderId}/completion-holds/{reportId}/release
Authorization: Bearer <administrator Firebase ID token>
```

Multiple unresolved reports may exist for one order. Completion remains blocked until every report-specific hold has been released.

## 3. Reporting API

| Method and path | Access | Behavior |
| --- | --- | --- |
| `POST /api/reports` | Authenticated | Multipart `report` JSON plus up to five JPEG/PNG/WebP `photos`; returns `201` after any required hold is confirmed and the report is saved |
| `GET /api/reports/mine?view=submitted\|against-me&page=1&size=20` | Authenticated | Caller-scoped, newest-first page with expiry reconciliation |
| `GET /api/reports/{id}` | Reporter, reported courier, admin | Reconciled report details |
| `POST /api/reports/{id}/response` | Reported courier | Multipart `response` JSON plus up to five photos; saves one timely response and moves the report to review |
| `GET /api/reports/{reportId}/attachments/{attachmentId}` | Reporter, reported courier, admin | Streams private reporter or response evidence |
| `GET /api/reports?page=1&size=20` | Admin | All reports, newest first, with expiry reconciliation |
| `PATCH /api/reports/{id}/status` | Admin | Accepts only `RESOLVED`; requires `READY_FOR_REVIEW` and releases an order hold first |
| `GET /api/reports/docs` | Public | Swagger UI |
| `GET /api/reports/v3/api-docs` | Public | OpenAPI JSON |

Create request JSON part:

```json
{
  "category": "ORDER",
  "description": "The delivered item was damaged.",
  "orderId": "order-123"
}
```

Courier response JSON part:

```json
{
  "explanation": "The parcel was intact at handoff."
}
```

Both multipart operations accept zero to five non-empty JPEG, PNG, or WebP files, each at most 5 MiB. The browser must allow `FormData` to set its own multipart boundary.

Representative response fields:

```json
{
  "id": "report-123",
  "reporterId": "requester-uid",
  "category": "COURIER",
  "description": "The parcel was damaged.",
  "orderId": "order-123",
  "courierId": "courier-uid",
  "status": "AWAITING_COURIER_RESPONSE",
  "createdAt": "2026-10-27T00:00:00Z",
  "responseDeadlineAt": "2026-10-28T00:00:00Z",
  "courierResponse": null,
  "respondedAt": null,
  "resolvedAt": null,
  "attachments": [],
  "responseAttachments": []
}
```

## 4. Persistence compatibility

Firestore collection `reports` stores:

```text
id, reporterId, category, description, orderId, courierId, status,
createdAt, responseDeadlineAt, courierResponse, respondedAt, resolvedAt,
attachments[], responseAttachments[]
```

Each attachment stores `id`, private `objectKey`, original `filename`, validated `contentType`, `size`, and `uploadedAt`. New object paths distinguish `submission` and `response` evidence. Missing new fields in older documents are treated as null/empty, and persisted legacy `OPEN` maps to `READY_FOR_REVIEW` while existing `RESOLVED` history remains unchanged.

## 5. Verification and remaining work

Before merge:

- [ ] Order Service implements, secures, OpenAPI-documents, and contract-tests both blocker APIs in section 2.
- [ ] Rebase onto those contracts and run authenticated end-to-end checks for list scoping, active/terminal behavior, hold creation/idempotency, courier-required conflict, and hold release.
- [x] Run `cd reporting-service && ./mvnw verify` with Docker available (34 tests; JaCoCo line and branch gates passed on 2026-10-27).
- [x] Run `cd frontend && npm test && npm run typecheck && npm run lint && npm run build` (255 tests and all remaining checks passed on 2026-10-27).
- [ ] Exercise responsive reporting/completion flows from 320 px through 1920 px.
- [ ] Verify reporter/courier/admin attachment access and unrelated-user `403` responses.
- [ ] Verify response behavior immediately before and exactly at the 24-hour deadline.

Infrastructure configuration and deployment topology are unchanged from the existing Reporting Service setup.
