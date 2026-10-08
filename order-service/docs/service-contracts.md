# Service Contracts

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
- User consumes `AcceptedOrderCancellationTaskEvent` only for expired accepted cancellation: apply the configured penalty to the cancelling courier (`actorId`). User does not subscribe to open cancellation.
- User consumes `OrderCompletionTaskEvent` for every completion: use `overdue` and `overdueAt` to apply the configured penalty when late or decrease the score when on time. Penalty/score policy remains User-owned.
- These are event subscriptions, not blocking Order-to-User requests. Payloads carry `eventId`, `eventVersion`, top-level `orderId`/`orderVersion`, the current Order/repost snapshot without checkpoint history, `actorId`, and `occurredAt`; consumers deduplicate at-least-once delivery. Completion carries overdue facts derived internally from checkpoints.

### Supplier Service

- `validateSupplierPair(pickupId, deliveryId)`: both IDs exist, both are active for creation, and they are distinct.
- `resolveSupplierDetails(supplierIds)`: current names, categories, and campus locations, including inactive suppliers for historical orders.

### Credit Service

- `reserveCredits(orderId, requesterId, amount)`: reserve before an original or repost becomes `OPEN`.
- `assignCourier(orderId, courierId)`: proposed synchronous operation during courier acceptance. `orderId` is in the path and `courierId` is the only request-body field. Order waits for `200 OK` before persisting `ACCEPTED`; Credit validates transaction details from its own record. See FEEDBACK-004.
- `holdForReopen(orderId)`: synchronously hold/reset the transaction found by `orderId`, without refunding it. It has no request body. Order waits for `200 OK` before changing the accepted Order to `OPEN`; failure leaves it `ACCEPTED`. See FEEDBACK-003.
- Order checks its version locally before these calls; it does not send that version to Credit. Published Order events retain `orderVersion`.
- Credit consumes `OpenOrderRefundTaskEvent`: refund/release the reserved amount for either requester cancellation of an `OPEN` order (`order.status=CANCELLED`) or scheduled expiry of an unassigned `OPEN` order (`order.status=EXPIRED`). User is not involved. This event replaces Order's former synchronous Credit release call and the formerly separate expiration event.
- Credit consumes `AcceptedOrderCancellationTaskEvent` only when cancellation occurs at or after `expiresAt`: refund/release the reserved amount.
- Credit consumes every `OrderCompletionTaskEvent`: transfer/settle the reserved credits to the courier.
- Same-order direct `ACCEPTED -> OPEN` is distinct from prohibited `ABORTED -> OPEN`. The hold endpoint is missing from the inspected Credit API; see FEEDBACK-003.
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

Only contracts listed by the active sprint are implementable. See `sprints/sprint-1/contracts.md` for the current subset.

## Common guarantees

- Authenticated actor context or trusted service identity.
- Command ID for idempotent create, reserve, transition, expiry, and repost operations; globally unique event ID and event version for each published event.
- Expected order version for state changes and explicit conflict on stale commands.
- Stable order, user, and supplier identifiers; never exchange database entities.
- Distinct validation, unauthorized, not found, conflict, dependency unavailable, reservation rejection, and accepted-for-processing failures.
- Event payloads contain event ID/version, order ID/version, actor IDs, event-specific facts, and occurred-at time without unnecessary private data.
- Completion, both OPEN-refund outcomes, and expired accepted cancellation commit state/checkpoint (and command receipt where triggered) plus event intent together, then dispatch after commit and retry due rows through the outbox cron. For unexpired accepted cancellation, Order synchronously waits for Credit's hold confirmation before persisting `OPEN` and emits no cancellation event. Pub/Sub consumers are asynchronous; Order does not wait for event subscriber replies. Delivery is at least once; consumers deduplicate stable event IDs. Future consumer guarantees are not verified by this Order-only milestone.
