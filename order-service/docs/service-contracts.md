# Service Contracts

These are logical approved contracts. CHANGE-053/054 approve complete Order snapshots, placeholder topics, and Google Cloud Pub/Sub. CHANGE-056 defines three event types; every completion uses one `OrderCompletionTaskEvent` with `overdue` and `overdueAt` facts. CHANGE-063/ADR-013 approves atomic Order/outbox persistence, after-commit dispatch, and cron recovery with at-least-once delivery.

## Order Service consumes

### User Service

- `verifyIdentity(accessContext)`: authenticate protected order actions.
- Requester-only completion and `OPEN` cancellation use a User Service-confirmed requester identity that must match `Order.requesterId`.
- Courier-only acceptance, progress, and accepted-order cancellation use a User Service-confirmed courier identity. Acceptance requires no assigned courier; progress and accepted cancellation require a match with `Order.courierId`.
- `getRoleContext(userId)`: return requester/courier/admin role context.
- `getCourierEligibility(userId)`: return whether a courier may accept new errands.
- `getUserSummary(userId)`: minimal display identity for authorized views.
- User consumes `AcceptedOrderCancellationTaskEvent`: evaluate cancellation policy from the full Order snapshot, actor, and event time.
- User consumes `OrderCompletionTaskEvent` for every completion: use `overdue` and `overdueAt` to apply the configured penalty when late or decrease the score when on time. Penalty/score policy remains User-owned.
- These are event subscriptions, not blocking Order-to-User requests. Payloads carry `eventId`, `eventVersion`, top-level `orderId`/`orderVersion`, the complete Order/repost/checkpoint snapshot, `actorId`, and `occurredAt`; consumers deduplicate at-least-once delivery.

### Supplier Service

- `validateSupplierPair(pickupId, deliveryId)`: both IDs exist, both are active for creation, and they are distinct.
- `resolveSupplierDetails(supplierIds)`: current names, categories, and campus locations, including inactive suppliers for historical orders.

### Credit Service

- `reserveCredits(orderId, requesterId, amount)`: reserve before an original or repost becomes `OPEN`.
- `evaluateOpenEntry(orderId)`: future-sprint credit condition for `ABORTED` reopening.
- Credit consumes `OpenOrderCancellationTaskEvent`: release the full reserved amount after a requester cancels an `OPEN` order.
- Credit consumes `AcceptedOrderCancellationTaskEvent`: apply `ABORTED` rules, retain the reservation while same-order reopening remains possible, and release it when the order expires.
- Credit consumes `OrderCompletionTaskEvent` for every completion and uses its overdue facts to settle according to the applicable overdue policy.
- `getReservationStatus(orderId)`: recovery/idempotency query after uncertain reservation responses.

## Order Service provides

- Admin all-orders query (CHANGE-057): `GET /api/orders`, optional `status`, one-based `page`/`size` pagination, `OrderPageResponse`; admin role required. Omitted status returns every status. This supports NTH1/Admin Service but does not implement its dashboard.

- Commands: `createOrder`, `acceptOrder`, `startTask`, `markPickedUp`, `markDelivered`, `confirmCompletion`, `cancelOpen`, `cancelAccepted`.
- Queries: `getOrder`, `listOrders`, `listAvailableOrders`, `getCheckpointHistory`, `getOrderFacts`.
- Reposting: creation-time automatic plan, `getManualRepostDraft`, and
  `requestManualRepost`. The legacy `configureReposting` route is retained for
  compatibility but rejects post-creation mutation.
- Lifecycle: `processExpiry`, `processAutoRepost`; future scope may include auto-completion.
- Admin integration: `placeCompletionHold`, `releaseCompletionHold`, `applyAdministrativeResolution`.
- Facts: `subscribeToOrderOutcomes`, including the three typed completion/cancellation event contracts.

Only contracts listed by the active sprint are implementable. See `sprints/sprint-1/contracts.md` for the current subset.

## Common guarantees

- Authenticated actor context or trusted service identity.
- Command ID for idempotent create, reserve, transition, expiry, and repost operations; globally unique event ID and event version for each published event.
- Expected order version for state changes and explicit conflict on stale commands.
- Stable order, user, and supplier identifiers; never exchange database entities.
- Distinct validation, unauthorized, not found, conflict, dependency unavailable, reservation rejection, and accepted-for-processing failures.
- Event payloads contain event ID/version, order ID/version, actor IDs, event-specific facts, and occurred-at time without unnecessary private data.
- For Sequences 5-7, Order commits state/checkpoint/receipt and event intent together, then dispatches after commit and retries due rows through cron. Order does not wait for peer-consumer replies. Delivery is at least once; consumers deduplicate stable event IDs. Future consumer guarantees are not verified by this Order-only milestone.
