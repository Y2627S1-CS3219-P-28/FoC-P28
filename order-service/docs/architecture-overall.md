# Overall Architecture

CHANGE-088 / ADR-029 refines only the local test deployment: temporary restricted
HTTPS connects real isolated financial Pub/Sub subscriptions to local Docker Credit.
Current contracts/OIDC/data ownership and production topology are unchanged.
See [local runbook](local-live-testing.md); not a User consumer or retry implementation.

CHANGE-086 (Vincent, 2026-10-09) adds explicit automatic deadlines and latest
safe repost outcomes ONLY to Order's API/PostgreSQL and its shared frontend
slice. No platform topology, peer data ownership, topic/event payload, credential
or deployment changes. Trusted delegation/background retries remain paused.


CHANGE-084 / ADR-027 records approved HTTP polling (not WebSocket/SSE) and
Order-owned fixed-ID bounded repost retry rules. Trusted Order-to-peer delegated
authorization is a documentation-only proposal awaiting peer agreement, not a
new platform credential/API, deployment or implemented topology change.

Current cadence override (CHANGE-083 / ADR-026): one minute-based Order job
checks OPEN expiry and >=48-hour DELIVERED completion; all-three-event outbox
recovery every 15 minutes, immediate dispatch retained. No peer/cloud resources
changed. Repost target/gaps: docs/diagrams/order-lifecycle-reconciliation.md.

Authoritative diagram: `../../../High Level Architecture Diagram - FOC.png`.

## Runtime topology

The existing shared top-level Next.js application is the single responsive web client for desktop and mobile browsers. It supports the `ADMIN` application role and the `USER` role with Requester/Courier modes. `USER` modes are functions of one account; do not assume `ADMIN` can use them. The source diagram visually separates web/mobile and administrator clients, but ADR-004 records the approved implementation interpretation that both experiences live in this shared application. Each backend service still owns and persists only its own data, and backend authorization remains authoritative. Observability consumes machine-readable logs, metrics, and audit events. NTH5 CI/CD and cloud deployment builds, tests, deploys, configures, and supports recovery for the platform.

## Cross-service responsibilities

- Order Service asks User Service for identity, roles, and courier eligibility and publishes order facts such as `COMPLETED`, overdue completion facts, and `ABORTED`.
- Order Service asks Supplier Service to validate supplier pairs and resolve current supplier details.
- Order Service asks Credit Service to reserve credits for creation/repost and synchronously hold/reset the transaction before an unexpired accepted order transitions directly back to `OPEN`. For completion, open cancellation, and expired accepted cancellation it commits typed event intent atomically with matching status/checkpoint/receipt, then dispatches after commit; Credit owns settlement/refund and User owns penalty/score policy. CHANGE-063/ADR-013 and CHANGE-064/ADR-014 record these rules.
- On acceptance, Order Service synchronously asks Credit to associate the active reservation with the courier and persists `ACCEPTED` only after confirmation. The Order-side mock and proposed HTTP route are CHANGE-068/ADR-017; Credit implementation and service authentication remain open in FEEDBACK-004.
- Event subscribers: Credit refunds `OpenOrderRefundTaskEvent` for both requester cancellation and scheduled expiry of an OPEN order, refunds `AcceptedOrderCancellationTaskEvent` only for expired accepted cancellation, and transfers credits for every `OrderCompletionTaskEvent`. User applies the expired accepted-cancellation penalty and, for every completion, the overdue penalty or on-time score reduction. User is not involved in OPEN-order refunds.
- Admin Service monitors orders and may place/release completion holds or apply supported case resolutions through explicit Order Service contracts.
- Admin Service calls User and Supplier administrative contracts; it does not write their databases.
- By CHANGE-057/ADR-012, Order Service exposes an admin-authorized paginated all-orders query (optional status filter) to support future NTH1 dashboard integration; the dashboard remains Admin Service-owned.

The updated overall design uses one broker and typed completion/cancellation events. Sequence 6 uses `OpenOrderRefundTaskEvent` for both requester-triggered OPEN cancellation (`CANCELLED`) and Spring-scheduled OPEN expiry (`EXPIRED`); Credit refunds for either and distinguishes the outcome from the resulting Order status in the event snapshot. CHANGE-071 supersedes the separate expiration event approved by CHANGE-065/ADR-015. Every requester-confirmed completion and every 48-hour scheduled auto-completion publishes one `OrderCompletionTaskEvent`, with overdue facts for both Credit and User; CHANGE-056/ADR-011 unifies completion events and CHANGE-072/ADR-020 adds the scheduled completion trigger. Event snapshots contain resulting Order/repost fields but omit checkpoint history; completion overdue facts remain calculated internally (CHANGE-067/ADR-016). CHANGE-063/ADR-013 supersedes CHANGE-053's publish-first implementation ordering: Order status, checkpoint, command receipt where applicable, and event intent commit atomically; an after-commit listener attempts prompt delivery and Spring cron recovers pending entries. CHANGE-054 selects Google Cloud Pub/Sub; CHANGE-073/ADR-021 uses the same project with separate dev/prod topics, personal ADC locally, and a Cloud Run service identity in production. Delivery is at least once, so consumers deduplicate stable event IDs. Cloud Run scale-to-zero/request-based CPU does not guarantee cron while idle.
