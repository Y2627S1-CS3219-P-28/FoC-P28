# Service Contracts

These are logical approved contracts. Endpoint paths, transport, serialization, retry policy, and concrete DTO schemas remain unapproved unless a sprint file defines them.

## Order Service consumes

### User Service

- `verifyIdentity(accessContext)`: authenticate protected order actions.
- `getRoleContext(userId)`: return requester/courier/admin role context.
- `getCourierEligibility(userId)`: return whether a courier may accept new errands.
- `getUserSummary(userId)`: minimal display identity for authorized views.
- `acceptCourierCompleted(eventId, orderId, courierId, occurredAt, orderVersion)`: accept a courier completion notification; User Service applies its own policy.
- `acceptCourierOverdue(eventId, orderId, courierId, occurredAt, orderVersion)`: accept a courier overdue notification; User Service applies its own policy.
- `acceptCourierAborted(eventId, orderId, courierId, occurredAt, orderVersion)`: accept a courier aborted notification; User Service applies its own policy.

The courier outcome is encoded by the operation name. These contracts must not use an `outcomeType` discriminator or a generic `facts` property bag. A completed order may also be overdue, so `acceptCourierCompleted` and `acceptCourierOverdue` are independent notifications rather than mutually exclusive enum branches.

### Supplier Service

- `validateSupplierPair(pickupId, deliveryId)`: both IDs exist, both are active for creation, and they are distinct.
- `resolveSupplierDetails(supplierIds)`: current names, categories, and campus locations, including inactive suppliers for historical orders.

### Credit Service

- `reserveCredits(orderId, requesterId, amount)`: reserve before an original or repost becomes `OPEN`.
- `evaluateOpenEntry(orderId)`: future-sprint credit condition for `ABORTED` reopening.
- `acceptOrderOutcome(outcome)`: Credit-owned processing of `COMPLETED`, `EXPIRED`, `CANCELLED`, or `ABORTED` facts.
- `getReservationStatus(orderId)`: recovery/idempotency query after uncertain reservation responses.

## Order Service provides

- Commands: `createOrder`, `acceptOrder`, `startTask`, `markPickedUp`, `markDelivered`, `confirmCompletion`, `cancelOpen`.
- Queries: `getOrder`, `listOrders`, `listAvailableOrders`, `getCheckpointHistory`, `getOrderFacts`.
- Reposting: `configureReposting`, `getManualRepostDraft`, `requestManualRepost`.
- Lifecycle: `processExpiry`, `processAutoRepost`; future scope may include auto-completion.
- Admin integration: `placeCompletionHold`, `releaseCompletionHold`, `applyAdministrativeResolution`.
- Facts: `subscribeToOrderOutcomes`.

Only contracts listed by the active sprint are implementable. See `sprints/sprint-1/contracts.md` for the current subset.

## Common guarantees

- Authenticated actor context or trusted service identity.
- Command ID for idempotent create, reserve, transition, outcome, expiry, and repost operations.
- Expected order version for state changes and explicit conflict on stale commands.
- Stable order, user, and supplier identifiers; never exchange database entities.
- Distinct validation, unauthorized, not found, conflict, dependency unavailable, reservation rejection, and accepted-for-processing failures.
- Event ID and order version on published facts.
- Outcome-specific courier operations use their method name as the semantic discriminator and carry only the common correlation fields defined above.
