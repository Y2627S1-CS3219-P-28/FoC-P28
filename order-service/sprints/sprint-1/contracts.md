# Sprint 1 Contracts

Only the following logical contract subset applies. Concrete HTTP paths, methods, schemas, status codes, event broker, and retry schedule are open questions.

## Consumed contracts

- User: `verifyIdentity(accessContext)` and `getRoleContext(userId)`.
- Supplier: `validateSupplierPair(pickupId, deliveryId)` and `resolveSupplierDetails(supplierIds)`.
- Credit: `reserveCredits(orderId, requesterId, amount)`.

## Provided contracts for the initial slice

- `confirmCompletion(orderId, actorContext, commandId, expectedVersion)`.
- `cancelOpen(orderId, actorContext, commandId, expectedVersion)`.
- `processExpiry(now, commandId, triggerIdentity)`.
- `processAutoRepost(now, commandId, triggerIdentity)`.
- `configureReposting(originalOrderId, repostConfiguration, actorContext, commandId, expectedVersion)` when required by sequence 10 prerequisites.
- `getManualRepostDraft(originalOrderId, actorContext)`.
- `requestManualRepost(originalOrderId, reviewedDetails, actorContext, commandId, expectedVersion)`.
- `subscribeToOrderOutcomes()` for expiry/repost facts only if an event transport is approved.

## Contract invariants

- Protected calls carry authenticated actor context; lifecycle calls carry trusted trigger identity.
- Command IDs prevent duplicate status changes, checkpoints, reservations, or reposts.
- Stale expected versions produce conflict.
- At most one repost exists per original across automatic and manual paths.
- Reservation rejection or dependency failure cannot produce an `OPEN` repost.
- Failure categories remain distinguishable: validation, unauthorized, not found, conflict, dependency unavailable, reservation rejection.
- Published facts contain event ID and order version.

## Missing details requiring approval before relevant code

- Concrete transport and endpoint/event names.
- Exact request/response DTO fields and validation-error format.
- Authentication propagation/trusted-trigger mechanism.
- Transaction/outbox boundary for persistence, reservation, and fact publication.
- Retry/time-out policy and uncertain reservation recovery.
- PostgreSQL schema, identifier types, timestamps, and version representation.
