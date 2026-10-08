# Service Contracts

## Effective Sprint 2-3 amendments (CHANGE-082 / ADR-025)

This section supersedes historical abort/reopen descriptions below. Every ACCEPTED-only abort first calls bodyless `POST /api/credits/orders/{orderId}/hold-for-reopen`, requiring 200 after courierId reset and reservation retention. Current Order keeps its business ID and becomes OPEN/EXPIRED after a fresh deadline check; an immutable ABORTED courier attempt is stored separately. EVERY abort emits the accepted-cancellation event for User; only EXPIRED additionally emits the shared refund event for Credit. New refunds are not requested through the User penalty event. Peer feedback documents legacy reconciliation, missing APIs/subscribers, payload/status/authentication/retry requirements.

Courier `/mine` uses database-paginated current assignments plus ONLY that courier's immutable attempts. Existing response adds nullable `attemptId`; it is non-null for read-only ABORTED snapshots, null for current Orders. Both may share business `id`; clients key history by attemptId and never mutate a history snapshot. Requester `/mine` excludes EXPIRED originals only after successful repost linkage, before pagination/counting. Repost reserves a NEW ID, keeps old/new rows, and authenticated manual receipt replay validates requester/original linkage. No event schema/topic rename.

Acceptance still waits synchronously for bodyless 200 from `PUT /api/credits/orders/{orderId}/courier-assignment` with only courierId in JSON. Assignment/reset are Order-side approved stubs, not verified peer providers. No HTTP fallback to mocks. Automatic repost's current lifecycle credential is not a verified peer Firebase identity; FEEDBACK-005 remains open. Full NTH3 report/hold/admin contracts listed later are logical future scope, not implemented by CHANGE-082.

These are logical approved contracts. CHANGE-053/054 approve Order event snapshots and Google Cloud Pub/Sub; CHANGE-067/ADR-016 clarifies that snapshots omit checkpoint history. CHANGE-056 defines one `OrderCompletionTaskEvent` with `overdue` and `overdueAt` facts. CHANGE-063/ADR-013 approves atomic Order/outbox persistence, after-commit dispatch, and cron recovery with at-least-once delivery. CHANGE-064/ADR-014 adds a synchronous Credit hold before an unexpired accepted errand returns to `OPEN`; expired accepted cancellation remains event-driven. CHANGE-065/ADR-015 added Spring-scheduled OPEN expiry; CHANGE-071/ADR-019 supersedes only its separate event type so requester cancellation and scheduled expiry both emit `OpenOrderRefundTaskEvent` on one topic, distinguished by the resulting Order status.

CHANGE-073/ADR-021 configures real Pub/Sub with one GCP project and environment-specific topics. Local topic IDs are `order-completion-dev-v1`, `open-order-refund-dev-v1`, and `accepted-order-cancellation-dev-v1`; production topic IDs are deployment configuration. Grant publisher access at topic scope only. Local Compose uses each developer's ADC; Cloud Run uses its service identity.

CHANGE-068/ADR-017 adds the Order-side courier-assignment contract stub: Credit must confirm the reservation courier synchronously before Order persists acceptance. CHANGE-069/ADR-018 keeps Order versions local and makes assignment/hold requests minimal, while published events retain `orderVersion`. FEEDBACK-003's bodyless hold-by-Order-ID contract is agreed but not implemented by Credit; FEEDBACK-004's assignment contract and route remain open.

## Order Service consumes

### User Service

- `verifyIdentity(accessContext)`: authenticate protected order actions.
- Requester-only completion and `OPEN` cancellation use a User Service-confirmed requester identity that must match `Order.requesterId`.
- Courier-only acceptance, progress, and accepted-order cancellation use a User Service-confirmed courier identity. Acceptance requires no assigned courier; progress and accepted cancellation require a match with `Order.courierId`.
- `getRoleContext(userId)`: return requester/courier/admin role context.
- `getCourierEligibility(userId)`: return whether a courier may accept new errands.
- `getUserSummary(userId)`: minimal display identity for authorized views.
- User consumes `AcceptedOrderCancellationTaskEvent` for EVERY courier abort (current snapshot OPEN/EXPIRED): apply the configured penalty to actorId, not cleared courierId. User does not subscribe to requester OPEN-cancellation refunds.
- User consumes `OrderCompletionTaskEvent` for every completion: use `overdue` and `overdueAt` to apply the configured penalty when late or decrease the score when on time. Penalty/score policy remains User-owned.
- These are event subscriptions, not blocking Order-to-User requests. Payloads carry `eventId`, `eventVersion`, top-level `orderId`/`orderVersion`, the current Order/repost snapshot without checkpoint history, `actorId`, and `occurredAt`; consumers deduplicate at-least-once delivery. Completion carries overdue facts derived internally from checkpoints.

### Supplier Service

- `validateSupplierPair(pickupId, deliveryId)`: both IDs exist, both are active for creation, and they are distinct.
- `resolveSupplierDetails(supplierIds)`: current names, categories, and campus locations, including inactive suppliers for historical orders.

### Credit Service

- `reserveCredits(orderId, requesterId, amount)`: reserve before an original or repost becomes `OPEN`.
- `assignCourier(orderId, courierId)`: proposed synchronous operation during courier acceptance. `orderId` is in the path and `courierId` is the only request-body field. Order waits for `200 OK` before persisting `ACCEPTED`; Credit validates transaction details from its own record. See FEEDBACK-004.
- `holdForReopen(orderId)`: synchronously clear courierId while retaining funds on EVERY abort, including expired outcomes; no request body. Order waits for `200 OK` before saving OPEN/EXPIRED/history/events; failure leaves ACCEPTED. See FEEDBACK-003/CHANGE-082.
- Order checks its version locally before these calls; it does not send that version to Credit. Published Order events retain `orderVersion`.
- Credit consumes `OpenOrderRefundTaskEvent`: refund/release the reserved amount for either requester cancellation of an `OPEN` order (`order.status=CANCELLED`) or scheduled expiry of an unassigned `OPEN` order (`order.status=EXPIRED`). User is not involved. This event replaces Order's former synchronous Credit release call and the formerly separate expiration event.
- Credit consumes `OpenOrderRefundTaskEvent` also for expired courier aborts. Legacy ABORTED accepted-cancellation payloads require coordinated refund reconciliation; NEW accepted-cancellation events are User penalty facts, not Credit refund instructions.
- Credit consumes every `OrderCompletionTaskEvent`: transfer/settle the reserved credits to the courier.
- Same-order current ACCEPTED resolves directly OPEN/EXPIRED while ABORTED survives in immutable courier history (ADR-025 supersedes the prior prohibition). The hold endpoint is missing from the inspected Credit API; see FEEDBACK-003.
- `getReservationStatus(orderId)`: recovery/idempotency query after uncertain reservation responses.

## Order Service provides

- Admin all-orders query (CHANGE-057): `GET /api/orders`, optional `status`, one-based `page`/`size` pagination, `OrderPageResponse`; admin role required. Omitted status returns every status. This supports NTH1/Admin Service but does not implement its dashboard.

- Commands: `createOrder`, `acceptOrder`, `startTask`, `markPickedUp`, `markDelivered`, `confirmCompletion`, `cancelOpen`, `cancelAccepted`.
- Queries: `getOrder`, `listOrders`, `listAvailableOrders`, `getCheckpointHistory`, `getOrderFacts`.
- Reposting: creation-time automatic plan, `getManualRepostDraft`, and
  `requestManualRepost`. The legacy `configureReposting` route is retained for
  compatibility but rejects post-creation mutation.
- Lifecycle: `processExpiry`, `processAutoRepost`, and `autoCompleteDue`. Spring scheduler selects `DELIVERED` orders with a delivered checkpoint at or before `now - 48 hours`; the transition rechecks under lock and commits the ordinary completion checkpoint/receipt and `OrderCompletionTaskEvent` outbox row atomically.
- Admin integration: `placeCompletionHold`, `releaseCompletionHold`, `applyAdministrativeResolution`.
- Facts: `subscribeToOrderOutcomes`, including typed completion, shared OPEN-refund, and accepted cancellation event contracts.

Only approved active-workstream contracts are implementable. See `sprints/sprint-2-3/README.md` for the current slice and `sprints/sprint-1/contracts.md` for historical scope.

## Common guarantees

- Authenticated actor context or trusted service identity.
- Command ID for idempotent create, reserve, transition, expiry, and repost operations; globally unique event ID and event version for each published event.
- Expected order version for state changes and explicit conflict on stale commands.
- Stable order, user, and supplier identifiers; never exchange database entities.
- Distinct validation, unauthorized, not found, conflict, dependency unavailable, reservation rejection, and accepted-for-processing failures.
- Event payloads contain event ID/version, order ID/version, actor IDs, event-specific facts, and occurred-at time without unnecessary private data.
- Completion/refund/abort commit state/checkpoints/history where applicable, receipt and event intent together; dispatch follows commit and recovery retries due outbox rows. Every abort waits synchronously for Credit reset, then emits User penalty, plus Credit refund only for EXPIRED. Pub/Sub subscribers are asynchronous; Order does not wait for their replies. Delivery is at least once and requires durable consumer deduplication. Provider/consumer guarantees are not verified by this Order-only milestone.


## UI time selection and background cadence

CHANGE-077/ADR-022 restricts Requester creation/repost UI clock minutes to 00/15/30/45 and rounds suggestions upward. It does not add a backend/API quarter-hour constraint or change ISO timestamp/event fields. Expiry scans all DB-due OPEN unassigned orders every 15 minutes, outbox recovery runs hourly, and 48-hour delivered auto-completion stays every minute. Every committed outcome still attempts immediate publication. These intervals are defaults rather than guaranteed delivery/expiry SLAs, especially under the existing Cloud Run idle CPU settings.


## CHANGE-079 / ADR-024: Central role annotations (approved 2026-10-08)

Production Firebase validation resolves User Service roles once per request, verifies response identity against JWT subject, and enforces RequireRequesterRole/RequireCourierRole/RequireAdminRole. Order-specific production role mode defaults to HTTP; local anonymous/mock behavior remains. Adapters reuse verified roles/identity, retaining fresh courier eligibility and locked domain ownership/state guards before mutation. Shared reads accept any confirmed requester/courier/admin role; /mine uses its selected mode. Internal lifecycle/scheduler authorization, API bodies, event payloads and schema remain unchanged. See ADR-024 for endpoint policy, inspected peer contracts and positive/negative test obligations.
