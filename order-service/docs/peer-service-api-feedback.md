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
- Related requirement: Project D1 F7.1, F10.1.4, F11.1.2, F11.2.3; approved synchronous Credit boundary
- Priority: High
- Related API decision: Existing reservation API is usable for Sequence 1 and repost creation; outcome operations remain missing
- Supersedes: None
- Superseded by: None

### Required capability

Order Service needs synchronous, idempotent Credit Service operations before
finalizing credit-related outcomes: settle a completed order to the courier,
release/refund a reservation for cancellation, and release/refund a reservation
for expiry. The operation must be owned and decided by Credit Service.

### Expected contract

- Operation or endpoint: Provider-owned synchronous endpoints for `COMPLETED`, `CANCELLED`, and `EXPIRED` outcomes; exact paths require peer/project-owner agreement.
- Request: `commandId`, `orderId`, requester ID, courier ID when applicable, amount/reservation reference, occurred-at timestamp, and expected order version.
- Response: Idempotent outcome status and resulting reservation/account state.
- Errors: Validation, unauthenticated/forbidden, reservation not found, duplicate/conflicting command, insufficient/invalid state, and dependency failure.
- Authorization: Authenticated Order Service trusted identity or approved service-to-service credential.
- Data semantics: Credit Service owns reservation release, settlement, transfer, balances, and ledger records; Order Service does not calculate credit policy.
- Synchronous or asynchronous behavior: Synchronous before the Order outcome is committed, according to the approved project boundary.

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
an Order-side balance update would violate Credit Service ownership and the
approved synchronous boundary.

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

Resolve the Sprint 1 deferment versus the broader approved synchronous Credit
boundary, then have the Credit Service owner propose and implement the approved
outcome contracts.

### Verification notes

Inspected Credit Service controller, service, README, security configuration,
and integration tests on 2026-09-30. Reservation and registration endpoints are
implemented; outcome settlement/release endpoints were not found.

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
