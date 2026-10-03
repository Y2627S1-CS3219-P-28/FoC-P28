# Overall Architecture

Authoritative diagram: `../../../High Level Architecture Diagram - FOC.png`.

## Runtime topology

The existing shared top-level Next.js application is the single responsive web client for desktop and mobile browsers. It supports the `ADMIN` application role and the `USER` role with Requester/Courier modes. `USER` modes are functions of one account; do not assume `ADMIN` can use them. The source diagram visually separates web/mobile and administrator clients, but ADR-004 records the approved implementation interpretation that both experiences live in this shared application. Each backend service still owns and persists only its own data, and backend authorization remains authoritative. Observability consumes machine-readable logs, metrics, and audit events. NTH5 CI/CD and cloud deployment builds, tests, deploys, configures, and supports recovery for the platform.

## Cross-service responsibilities

- Order Service asks User Service for identity, roles, and courier eligibility and publishes order facts such as `COMPLETED`, overdue completion facts, and `ABORTED`.
- Order Service asks Supplier Service to validate supplier pairs and resolve current supplier details.
- Order Service asks Credit Service to reserve/query and evaluate same-order reopening synchronously. For Sequences 5-7, it commits typed completion/cancellation event intent atomically with matching Order status/checkpoint/receipt, then dispatches after commit; Credit Service owns settlement, release, and resulting policy. CHANGE-063/ADR-013 records the effective outbox behavior.
- Admin Service monitors orders and may place/release completion holds or apply supported case resolutions through explicit Order Service contracts.
- Admin Service calls User and Supplier administrative contracts; it does not write their databases.
- By CHANGE-057/ADR-012, Order Service exposes an admin-authorized paginated all-orders query (optional status filter) to support future NTH1 dashboard integration; the dashboard remains Admin Service-owned.

The updated overall design currently has one broker and three typed event flows. Every completion is published once as `OrderCompletionTaskEvent`, with overdue facts for both Credit and User; CHANGE-056 supersedes the former separate overdue completion flow. CHANGE-063/ADR-013 supersedes CHANGE-053's publish-first implementation ordering: Order status, checkpoint, command receipt, and event intent commit atomically; an after-commit listener attempts prompt delivery and Spring cron recovers pending entries. CHANGE-054 selects Google Cloud Pub/Sub; topic IDs remain placeholders. Delivery is at least once, so consumers deduplicate stable event IDs. Cloud Run scale-to-zero/request-based CPU does not guarantee cron while idle.
