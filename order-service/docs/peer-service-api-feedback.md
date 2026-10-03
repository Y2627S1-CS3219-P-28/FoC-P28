# Peer-Service API Feedback

This is the single shared record for missing, unsuitable, incomplete, or incompatible peer-service APIs that block an Order Service feature. Read it before every cross-service design, implementation, resumption, or verification turn. Append entries; never create a separate Markdown file for one API issue and never overwrite earlier feedback.

No peer-service API feedback entry has been recorded yet.

## FEEDBACK-001: Credit Service — outcome settlement and release operations

- Status: OPEN
- Date: 2026-09-30
- Requesting service: Order Service
- Responsible service: Credit Service
- Affected Order Service feature: Credit consequences for completed, cancelled, and expired orders
- Affected sequence: 7, 8, and 9
- Related requirement: Project D1 F7.1, F10.1.4, F11.1.2, F11.2.3; updated overall design `CHANGE-051` typed event boundary
- Priority: High
- Related API decision: Existing reservation API is usable for Sequence 1 and repost creation; outcome operations remain missing
- Supersedes: None
- Superseded by: None

### Required capability

The superseded local exception expected synchronous, idempotent Credit Service
operations before finalizing credit-related outcomes. The updated overall design
now requires Order to commit state/checkpoint/outbox atomically and publish typed
events; Credit consumes completion/cancellation events and owns settlement/release.
Reservation before `OPEN` remains synchronous. FEEDBACK-003 separately requests
the synchronous hold/reset endpoint required before an unexpired accepted order
can transition directly back to `OPEN`. The provider still must agree on event
schemas, subscription authentication, idempotency, retries, and failure handling.

### Expected contract

- Operation or endpoint: Provider-owned synchronous endpoints for `COMPLETED`, `CANCELLED`, and `EXPIRED` outcomes; exact paths require peer/project-owner agreement.
- Request: `commandId`, `orderId`, requester ID, courier ID when applicable, amount/reservation reference, occurred-at timestamp, and expected order version.
- Response: Idempotent outcome status and resulting reservation/account state.
- Errors: Validation, unauthenticated/forbidden, reservation not found, duplicate/conflicting command, insufficient/invalid state, and dependency failure.
- Authorization: Authenticated Order Service trusted identity or approved service-to-service credential.
- Data semantics: Credit Service owns reservation release, settlement, transfer, balances, and ledger records; Order Service does not calculate credit policy.
- Synchronous or asynchronous behavior: Typed event delivery after the Order state/checkpoint/outbox transaction; at-least-once consumer processing with deduplication. Reservation remains synchronous; the missing hold/reset call is tracked separately in FEEDBACK-003.

### Current peer-service status

The inspected Credit Service currently exposes `POST /api/credits/registration-facts`,
`GET /api/credits/me`, `PUT /api/credits/orders/{orderId}/reservation`, and
`GET /api/credits/orders/{orderId}/reservation`. No completed-order settlement,
cancellation release, or expiry release endpoint was found in its controller,
service, README, or integration tests.

### Approved temporary Sprint 1 stub contract

Vincent approved a temporary Order-owned mock boundary on 2026-09-30 so the
Order Service can exercise Sequences 7-9 locally while the Credit Service owner
implements the real operations. The mock is not evidence that the peer API is
implemented, and FEEDBACK-001 remains `OPEN`.

The concrete contract requested from the Credit Service owner is:

- `POST /api/credits/orders/{orderId}/settlement`
  - Request JSON: `commandId`, `requesterId`, `courierId`, `amount`, and
    `expectedOrderVersion`.
  - Semantics: atomically transfer the reserved amount to the courier and
    record a ledger settlement; repeat calls with the same `commandId` must
    return the original result without a second transfer.
- `POST /api/credits/orders/{orderId}/release`
  - Request JSON: `commandId`, `requesterId`, `amount`, `outcome` (`CANCELLED`
    or `EXPIRED`), and `expectedOrderVersion`.
  - Semantics: release/refund the reservation and record the outcome; repeat
    calls with the same `commandId` must be idempotent.
- Both operations should return a documented success body containing the
  order/command identity, outcome, and resulting reservation/account state.
  The provider owns the final response field names.
- Both operations must define validation, unauthenticated/forbidden,
  reservation-not-found, duplicate/conflicting-command, invalid-state, and
  dependency-failure responses using the provider's standard error envelope.
- Authentication must accept the approved service-to-service credential and
  must not trust a caller-supplied user ID as proof of identity.

The Order Service currently maps these shapes in `HttpPeerAdapters` and uses a
validating no-op implementation in `MockPeerAdapters`. The lifecycle trigger
validates `X-Lifecycle-Token` and forwards an explicit bearer-form internal
credential to automatic peer calls; this is a local/temporary bridge only.
Production completion requires the Credit Service owner to agree to the
credential, response, idempotency, and error details and then implement and
verify the endpoints. No Credit Service source was modified.

### Why the existing API is unsuitable or missing

The reservation and lookup operations cannot safely express the required
outcome-side transfer/release semantics. Reusing reservation lookup or inventing
an Order-side balance update would violate Credit Service ownership. The current
`CreditServicePort.settle` and `release` calls also implement the superseded
synchronous boundary and cannot be treated as the updated event contract.

### Suggested implementation for the peer developer

Propose explicit idempotent Credit Service operations for completed settlement
and cancelled/expired release, with request/response/error/authentication
contracts and contract tests. The exact public paths and payloads require project
owner approval before implementation.

### Impact if unresolved

Order Service can reserve credits for creation and reposting, but cannot claim
full D1-compliant credit processing for Sequences 7–9. Sprint 1 may defer these
operations only if that narrower scope remains the effective approved decision.

### Implementation stopping point

Order Service retains only the existing reservation integration. No missing
outcome endpoint was invented and no peer-service source was modified.

### Next action

Have the Credit Service owner implement the documented event subscriptions,
idempotency, authentication, retry, and dead-letter contracts. Reservation and
the hold/reset operation in FEEDBACK-003 are synchronous.

### Verification notes

Inspected Credit Service controller, service, README, security configuration,
and integration tests on 2026-09-30. Reservation and registration endpoints are
implemented; outcome settlement/release endpoints were not found.

### Resolution notes

None.

## FEEDBACK-002: Credit and User Service â€” typed outcome event subscriptions

- Status: OPEN
- Date: 2026-10-02
- Requesting service: Order Service
- Responsible services: Credit Service and User Service
- Affected Order Service feature: Completion and cancellation consequence delivery
- Affected updated sequences: 5, 6, 7, and 8
- Related design: CHANGE-051, CHANGE-052, CHANGE-053, ADR-009
- Priority: High

### Required capability

Under CHANGE-056, Credit Service must subscribe to `OrderCompletionTaskEvent`, `OpenOrderCancellationTaskEvent`, and `AcceptedOrderCancellationTaskEvent`. User Service must subscribe to `AcceptedOrderCancellationTaskEvent` and every `OrderCompletionTaskEvent`; its completion policy uses the event's `overdue` and `overdueAt` facts. Consumers must process at least once, deduplicate, acknowledge after success, retry independently, and provide dead-letter/recovery handling under the approved event contract. These consumers remain absent in the inspected peer implementations and are not verified.

Subscriber actions: `OpenOrderCancellationTaskEvent` asks Credit to refund/release the reservation; User does not subscribe. `AcceptedOrderCancellationTaskEvent` is emitted only when cancellation occurs at/after `order.expiresAt`; Credit refunds/releases and User applies the cancellation penalty to the courier in `actorId`. Every `OrderCompletionTaskEvent` asks Credit to transfer/settle credits to the courier and asks User to apply the overdue penalty when `overdue` is true or the existing penalty-score reduction when false. Subscribers deduplicate by `eventId` and own retry, acknowledgment, and recovery.

### Inspected peer implementation

- Credit Service `CreditController` currently exposes registration facts, user balance, reservation, and reservation lookup HTTP operations. No event subscriber or completion/cancellation outcome handler was found.
- User Service source contains profile/identity HTTP components and no event subscriber or penalty-fact handler was found.
- Compatibility status: `MISSING` for both requested consumer integrations. Existing synchronous Credit reservation and reservation lookup do not implement completion/cancellation event subscriptions.

### Contract questions for peer owners

- Confirm event envelope, payload fields, schema/version compatibility, and stable deduplication key.
- Confirm how subscribers authenticate and validate publisher/service identity.
- Define acknowledgements, timeouts, retry/backoff, poison-message handling, dead-letter ownership, monitoring, and recovery/replay.
- Confirm Credit refund/transfer semantics for each event; unexpired accepted cancellation does not publish an event and instead uses the synchronous API proposed in FEEDBACK-003.
- Confirm User's expired accepted-cancellation penalty and completion overdue/on-time score handling without moving penalty policy into Order Service.

### Implementation stopping point

No peer source was modified. The user explicitly authorized the Order-side publisher milestone to assume future Credit/User consumers, so missing consumer implementations do not block the Order producer. This authorization does not verify either peer integration; keep this entry `OPEN` until consumer code/contracts are independently inspected.

### Next action

Peer owners can implement the corresponding consumers later as directed by the user. Before labeling either integration verified, inspect actual consumer code and tests against CHANGE-053/054's event payload and Pub/Sub delivery contract.

## FEEDBACK-003: Credit Service — synchronous hold/reset before reopening an unexpired accepted order

- Status: OPEN
- Date: 2026-10-03
- Requesting service: Order Service
- Responsible service: Credit Service
- Affected Order Service feature: Assigned courier cancels an accepted errand before its expiry
- Affected sequence: Updated overall Sequence 7
- Related requirement: CHANGE-064 / ADR-014
- Priority: High
- Related API decision: No matching operation exists in the inspected Credit API; local Order mock supports the flow
- Supersedes: None
- Superseded by: None

### Required capability

When the assigned courier cancels an `ACCEPTED` order before `expiresAt`, Order must wait synchronously for Credit to hold/reset the existing transaction state without refunding or transferring reserved credits. Order changes the same order to `OPEN` only after success, preventing another courier from accepting before Credit confirms the reservation remains valid.

### Expected contract

- Operation or endpoint: `POST /api/credits/orders/{orderId}/hold-for-reopen` (proposed path; Credit owner must confirm).
- Request: `commandId`, `requesterId`, `courierId`, `amount`, and `expectedOrderVersion`.
- Response: Synchronous success confirming the matching order/command and transaction state `HELD_FOR_REOPEN`; exact response envelope requires peer agreement.
- Errors: Missing/mismatched reservation or order, invalid/conflicting command, invalid transaction state, unauthorized/forbidden, and dependency unavailable. Non-success prevents Order from reopening.
- Authorization: Trusted Order-to-Credit service identity/credential agreed by both owners; caller-supplied IDs are not authentication.
- Data semantics: Preserve the reservation and requester balance; do not refund, release, or transfer. Idempotent by `commandId`, including already-held matching state for later accepted/reopen cycles.
- Synchronous or asynchronous behavior: Synchronous HTTP request/response. Order waits for confirmation before persisting `ACCEPTED -> OPEN`. Retry/reconciliation must account for Credit succeeding before a later Order database failure.

### Current peer-service status

Credit currently exposes registration facts, balance, reservation, and reservation lookup; no hold/reset endpoint was found. Order's default mock adapter now models an idempotent hold without refund, and the HTTP adapter targets the proposed path. These are not Credit implementation or production verification.

### Why the existing API is unsuitable or missing

Reservation lookup is read-only and cannot safely hold/reset transaction state. An event is asynchronous and cannot guarantee Credit processed the state change before Order exposes the order to another courier.

### Suggested implementation for the peer developer

Implement an authenticated, idempotent operation with a documented response/error envelope and contract tests. Confirm how later completion or refund consumes a held transaction. No Credit source change was made.

### Impact if unresolved

The local Order mock can exercise this workflow, but Order's HTTP-peer profile cannot safely reopen unexpired accepted orders until Credit implements and agrees to the contract. Expired cancellation remains event-driven and does not call this endpoint.

### Implementation stopping point

Order-side port, in-memory mock, proposed HTTP request, and tests are implemented. Credit endpoint, authentication agreement, and live HTTP integration remain missing.

### Next action

Credit owner: confirm the contract, implement the operation and tests, and report a revision ready for Order-side verification. Keep this feedback `OPEN` until actual peer code is inspected.

### Verification notes

Credit API was inspected on 2026-10-03; no hold/reset route was present. Order's mock and HTTP adapter do not verify the live peer endpoint.

### Resolution notes

None.

## Status lifecycle

Supported statuses:

- `OPEN`: issue recorded; responsible service action or decision is pending.
- `IN_PROGRESS`: the peer owner reports work has started.
- `READY_FOR_VERIFICATION`: the peer owner reports an implementation is ready; Order Service has not verified it yet.
- `VERIFIED`: Order Service re-read the actual peer implementation and confirmed request, response, errors, authorization, semantics, and communication behavior against the feedback entry.
- `REJECTED`: the requested capability or proposed resolution was rejected.
- `BLOCKED`: progress requires an external decision or dependency beyond the current owner.
- `SUPERSEDED`: a newer feedback/decision record replaces this entry; link it explicitly.

Only evidence from the actual peer-service implementation can move an entry to `VERIFIED`. A planned contract, prototype, stub, mock, message, or peer claim is not implementation verification.

## Entry template

```markdown
## FEEDBACK-NNN: <Peer Service Name> — <Short Capability Name>

- Status: OPEN
- Date:
- Requesting service: Order Service
- Responsible service:
- Affected Order Service feature:
- Affected sequence:
- Related requirement:
- Priority:
- Related API decision:
- Supersedes:
- Superseded by:

### Required capability

Describe what Order Service needs the peer service to provide.

### Expected contract

- Operation or endpoint:
- Request:
- Response:
- Errors:
- Authorization:
- Data semantics:
- Synchronous or asynchronous behavior:

### Current peer-service status

Describe the inspected routes/controllers/interfaces/DTOs/events/tests and what currently exists.

### Why the existing API is unsuitable or missing

Explain why no existing or similar API can safely satisfy the feature.

### Suggested implementation for the peer developer

Describe the API, function, event, or contract to implement or modify. This is feedback, not authorization to edit the peer service.

### Impact if unresolved

Identify the blocked Order Service feature and sequence.

### Implementation stopping point

State exactly what the Order Service agent completed and where it stopped.

### Next action

State what the peer developer or project owner must implement, approve, or confirm.

### Verification notes

Leave empty until the peer implementation is re-read. Record inspected files/commit or revision, observed request/response/errors/authorization/semantics, tests run, result, verifier, and date before setting `VERIFIED`.

### Resolution notes

Leave empty until the peer developer or project owner responds.
```

## Rules

1. Read existing entries before allocating a new stable `FEEDBACK-NNN` ID.
2. Do not modify peer-service source without explicit authorization.
3. Do not implement an Order Service integration against a missing or unsuitable API.
4. Copy the stopping-point facts into the current developer's active-work blocker so the next conversation resumes safely.
5. After peer confirmation, re-read the peer code and tests; compare the actual implementation with this entry; keep the feature blocked if any material difference remains.
6. A user-approved prototype may support a separately approved contract-stub milestone, but record that the API is not implemented, the assumed shapes/semantics, stubs/tests, compatibility requirement, and remaining risk. Do not mark the integration complete until actual peer implementation is verified unless the user explicitly approves that milestone as the endpoint.
