<!--
AI Assistance Disclosure:
Tool: OpenAI Codex (GPT-5), date: 2026-10-13
Mode: Documentation generation.
Scope: Documented team-specified Reporting Service behavior, APIs, dependencies, storage, configuration, and verification.
Author review: I reviewed for correctness.
-->

# Reporting Service

Reporting Service owns report records and their private photo attachments. Firestore stores report and attachment metadata; Cloud Storage stores the photo bytes. Admin Service is only a future consumer and does not own or mutate this data.

See [HANDOFF.md](HANDOFF.md) for the complete implementation summary, API payloads, Order Service contract, dependencies, known gaps, and pre-merge checklist.

## API

- `POST /api/reports` — authenticated multipart submission (`report` JSON plus zero to five `photos`).
- `GET /api/reports/mine?view=submitted|against-me&page=1&size=20` — caller-scoped history.
- `GET /api/reports/{id}` — details for the reporter, reported courier, or an administrator.
- `POST /api/reports/{id}/response` — one multipart courier response within 24 hours.
- `GET /api/reports?page=1&size=20` — admin-only, newest first.
- `GET /api/reports/{reportId}/attachments/{attachmentId}` — participant/admin private image stream.
- `PATCH /api/reports/{id}/status` — admin-only resolution after the response window closes or a response arrives.
- Swagger UI: `/api/reports/docs`; OpenAPI JSON: `/api/reports/v3/api-docs`.

`ORDER` and `COURIER` submissions require an active order. `CREDITS` and `GENERAL` reject `orderId`. Reporting generates the report ID first, then uses the atomic Order Service hold operation documented in `HANDOFF.md`; it never fetches an order and validates it separately. Assigned couriers have one 24-hour response opportunity. Legacy persisted `OPEN` reports are read as `READY_FOR_REVIEW`.

`REPORTING-ORDER-001` is a blocking Order Service dependency. The required active-order list and atomic validate-and-hold APIs are documented in [HANDOFF.md](HANDOFF.md). Reporting integration is not complete until both are secured, documented in Order Service OpenAPI, contract-tested, and merged.

## Configuration

In addition to the repository-standard runtime variables, set `REPORTING_STORAGE_BUCKET`, `STORAGE_PROJECT_ID`, `STORAGE_EMULATOR_HOST` (local only), `ORDER_SERVICE_URL`, and optionally `ORDER_SERVICE_TIMEOUT`. Compose uses Firestore `reporting-local`, Storage on port 9199, and service port 8086.

## Development

Run `./mvnw verify`. Start the full local stack with `docker compose up --build`; all browser traffic still goes through the gateway.
