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
- `GET /api/reports?page=1&size=20` — admin-only, newest first.
- `GET /api/reports/{reportId}/attachments/{attachmentId}` — admin-only private image stream.
- `PATCH /api/reports/{id}/status` — admin-only idempotent resolution.
- Swagger UI: `/api/reports/docs`; OpenAPI JSON: `/api/reports/v3/api-docs`.

`ORDER` and `COURIER` submissions require an order; `CREDITS` and `GENERAL` may include one. Association is validated only with `GET /api/orders/mine/associated/{orderId}` using the caller's bearer token. The service intentionally performs no lifecycle filtering and no client-ID ownership comparison.

`REPORTING-ORDER-001` is a blocking Order Service dependency. Reporting integration is not complete until those endpoints are on `main` and this branch is rebased onto them.

## Configuration

In addition to the repository-standard runtime variables, set `REPORTING_STORAGE_BUCKET`, `STORAGE_PROJECT_ID`, `STORAGE_EMULATOR_HOST` (local only), `ORDER_SERVICE_URL`, and optionally `ORDER_SERVICE_TIMEOUT`. Compose uses Firestore `reporting-local`, Storage on port 9199, and service port 8086.

## Development

Run `./mvnw verify`. Start the full local stack with `docker compose up --build`; all browser traffic still goes through the gateway.
