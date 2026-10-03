# Sprint 1 Contracts

Only the following logical contract subset applies. Concrete HTTP paths, methods, schemas, and retry schedule remain open questions. CHANGE-053 approves complete Order snapshots; CHANGE-054 selects Google Cloud Pub/Sub; CHANGE-056 defines the unified completion event; CHANGE-063/ADR-013 approves transactional outbox delivery.

## Consumed contracts

- User: `verifyIdentity(accessContext)` and `getRoleContext(userId)`.
- Supplier: `validateSupplierPair(pickupId, deliveryId)` and `resolveSupplierDetails(supplierIds)`.
- Credit: `reserveCredits(orderId, requesterId, amount)`.

## Provided contracts for the initial slice

- CHANGE-057 (user-approved Order-side NTH1 support): `GET /api/orders?status={OrderStatus}&page={1-based page}&size={page size}`; status is optional and omitted means all statuses; default page 1/size 20, size capped at 100; admin role required; returns `OrderPageResponse` ordered by creation time descending.

- `confirmCompletion(orderId, actorContext, commandId, expectedVersion)`.
- `cancelOpen(orderId, actorContext, commandId, expectedVersion)`.
- `processExpiry(now, commandId, triggerIdentity)`.
- `processAutoRepost(now, commandId, triggerIdentity)`.
- `configureReposting(originalOrderId, repostConfiguration, actorContext, commandId, expectedVersion)` when required by sequence 10 prerequisites.
- `getManualRepostDraft(originalOrderId, actorContext)`.
- `requestManualRepost(originalOrderId, reviewedDetails, actorContext, commandId, expectedVersion)`.
- `subscribeToOrderOutcomes()` for the three updated typed completion/cancellation events. `OrderCompletionTaskEvent` is published for every completed order and includes overdue facts for both User and Credit consumers.

The updated publisher pairs and method names are diagrammed in CHANGE-053/056. The serialized event payload includes a complete Order snapshot; topic names are placeholders. Credit/User consumer implementations are future work assumed by the user for this Order-side scope, but remain unverified under `FEEDBACK-002`.

## Contract invariants

- Protected calls carry authenticated actor context; lifecycle calls carry trusted trigger identity.
- Command IDs prevent duplicate status changes, checkpoints, reservations, or reposts.
- Stale expected versions produce conflict.
- At most one repost exists per original across automatic and manual paths.
- Reservation rejection or dependency failure cannot produce an `OPEN` repost.
- Failure categories remain distinguishable: validation, unauthorized, not found, conflict, dependency unavailable, reservation rejection.
- Published facts contain event ID/version, order version, actor/time metadata, and the full Order snapshot.
- For Sequences 5-7, the resulting Order state, checkpoint, command receipt, and event intent commit atomically. Delivery is attempted after commit and retried from the outbox; Pub/Sub failure does not undo the transition. Delivery is at least once and consumers deduplicate stable event IDs.

## Missing details requiring approval before relevant code

- Topic IDs, serialization compatibility policy, and retry schedule.
- Exact request/response DTO fields and validation-error format.
- Authentication propagation/trusted-trigger mechanism.
- Full snapshot support requires checkpoint-history retrieval; overdue facts are computed at completion. Flyway is selected. CHANGE-063 adds the transactional outbox schema for the three effective typed outcome event flows.
- Exact operational monitoring/alerting thresholds for repeated outbox delivery failures.
- Topic/subscription names, event versioning, deduplication, retry/dead-letter monitoring, and subscriber authentication.
- PostgreSQL schema, identifier types, timestamps, and version representation.
