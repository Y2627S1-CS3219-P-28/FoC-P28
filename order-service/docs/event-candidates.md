# D1-Supported Event Candidate Registry

This registry preserves D1-supported communication candidates for architecture review. It is a proposal catalogue, not an implementation plan, approved event contract, transport selection, or broker approval.

## Updated overall design source

The updated source originally declared separate completion event flows. The current effective Order contract has three typed events across Sequences 5-7: one `OrderCompletionTaskEvent` for every completion with overdue facts, plus open and expired accepted-cancellation events. CHANGE-056 records completion unification. CHANGE-063/ADR-013 replaces publish-before-status with atomic state/outbox commit and after-commit dispatch plus cron recovery. CHANGE-064/ADR-014 says an unexpired accepted cancellation uses synchronous Credit hold and publishes no cancellation event. Delivery is at least once and consumers deduplicate by stable event ID. Subscriber actions are documented in FEEDBACK-002.

## Status and ID rules

- `PROPOSED` means the interaction must be analysed and explicitly approved before implementation.
- `APPROVED_SYNCHRONOUS_BOUNDARY` means the communication style is already constrained to synchronous request-response; it does not approve an event.
- Candidate IDs `EV-1` through `EV-7` are stable references and must not be renumbered.
- Architecture decision IDs use the separate `EV-DEC-NNN` namespace.
- Peer-API mismatch decisions use `API-DEC-NNN`; missing/unsuitable dependency feedback uses `FEEDBACK-NNN` in `docs/peer-service-api-feedback.md`.
- Never infer a blank decision. A candidate remains unapproved until an explicit decision is recorded in an ADR or equivalent approved decision record.

## Candidate catalogue

| Candidate ID | D1 source | Possible interaction/event | Consumer | Status | Notes |
|---|---|---|---|---|---|
| EV-1 | F1.4, F1.4.1 | `OrderCreated` | Requester notification component | PROPOSED | Compare synchronous notification with an internal or durable event. |
| EV-2 | NTH2 | `PenaltyRelevantOrderOutcome` | User Service | PROPOSED | Preferred event-driven candidate because penalty evaluation must not block a committed Order outcome; payload carries facts, never a penalty decision. |
| EV-3 | NTH1 | `OrderStatusChanged` or `OrderOutcomeRecorded` | Admin Service | APPROVED_QUERY_FOR_ADMIN_ORDER_LIST | CHANGE-057/ADR-012 approve synchronous paginated Order Service reads for all orders and optional status. This decision does not approve an event or Admin Service implementation. |
| EV-4 | NTH3 | `OrderIncidentReported` | Admin Service | PROPOSED | Requires explicit communication and failure-semantics approval. |
| EV-5 | F4.1.6, F4.1.8, F4.1.10, F4.1.11, F10.1 | Lifecycle trigger or internal event | Order Service lifecycle processing | PROPOSED | A broker is not assumed; compare timer/direct invocation, polling, and in-process event first. |
| EV-6 | NTH4 | `OrderExpiryReached` or `RepostEligible` | Order Service reposting logic | PROPOSED | A broker is not assumed; automatic and manual reposting remain idempotent Order-owned behavior. |
| EV-7 | F7.1, F10.1.4, F11.1.2, F11.2.3 | Credit-related outcome | Credit Service | APPROVED_ORDER_EVENT; ORDER_PRODUCER_IMPLEMENTED | CHANGE-053/054 approve typed flows, publish-first ordering, and Google Cloud Pub/Sub for Order. Live delivery and peer consumers remain unverified. |

`OrderOpened` or real-time courier notification is not currently an approved D1 requirement. Treat it as unsupported unless a specific D1 requirement or later approved amendment establishes the need.

## Required comparison

For each applicable candidate, follow `docs/architecture-review-playbook.md` and compare:

1. Synchronous request-response.
2. Query-based refresh or polling.
3. Internal in-process domain event.
4. Durable broker-based event.

Analyse coupling, response time, consistency, reliability, duplicates, ordering, retry/dead-letter recovery, idempotency, schema versioning, debugging/testing complexity, infrastructure/operations, security/privacy, whether the interaction blocks the operation, and whether D1 requires it.

## Decision worksheet

Do not fill the Decision column without explicit user approval.

| Decision ID | Candidate | Interaction | Initial position | Alternatives | Decision |
|---|---|---|---|---|---|
| EV-DEC-001 | EV-1 | `OrderCreated` requester notification | Awaiting analysis and approval | Synchronous / internal event / durable event | |
| EV-DEC-002 | EV-2 | Penalty-relevant facts to User Service | Non-blocking event candidate | Synchronous / internal event / durable event | |
| EV-DEC-003 | EV-3 | Admin monitoring facts | User-approved synchronous query for paginated order listing | Query/polling / synchronous / event | APPROVED: `GET /api/orders`, paged and optionally filtered by status, admin-only; CHANGE-057/ADR-012, user, 2026-10-02. No dashboard or event implementation. |
| EV-DEC-004 | EV-4 | Incident report to Admin Service | Awaiting analysis and approval | Synchronous / event | |
| EV-DEC-005 | EV-5 | Lifecycle trigger | Internal mechanism preferred for analysis; broker not assumed | Timer/direct invocation / polling / internal event / broker | |
| EV-DEC-006 | EV-6 | Expiry/reposting trigger | Internal mechanism preferred for analysis; broker not assumed | Timer/direct invocation / polling / internal event / broker | |
| EV-DEC-007 | EV-7 | Credit-related outcomes | User-approved typed events with transactional outbox | Typed task publishers / synchronous / hybrid | CHANGE-053/054/063; Pub/Sub and post-commit outbox dispatch selected |
| API-DEC-001 | N/A | Applicable peer-service API mismatch | Awaiting mismatch discovery | Order-side adapter / provider API change / reject | |

Allowed user responses include approve synchronous communication, approve an internal event, approve a durable broker event, approve query/polling, approve a hybrid, reject, request more analysis, or request a peer-service API change.

## Recording an approved event decision

Use the existing ADR system. Create `docs/decisions/event-driven-decisions.md` only if no suitable record exists. Use the stable decision ID, not the candidate ID, as the decision record identifier.

```markdown
## Event Decision: EV-DEC-001

- Candidate ID:
- Feature:
- Event or interaction:
- D1 reference:
- Producer:
- Consumers:
- Sprint or milestone:
- Status: PROPOSED
- Chosen communication style:
- Approved by:
- Date:
- Related diagrams:
- Related contracts:

### Problem being solved
### Why this is required by D1
### Alternatives considered
### Trade-offs
### Approved decision
### Event payload
### Delivery guarantee
### Ordering requirements
### Idempotency strategy
### Retry and failure handling
### Dead-letter or recovery behavior
### Security and privacy considerations
### Required documentation updates
### Required test updates
```

Do not implement a decision whose status is `PROPOSED`. If a durable broker is approved, explicitly decide transactional publication/outbox needs, event versioning, idempotent consumers, retries, dead-letter recovery, monitoring, schema compatibility, security/privacy, and contract tests before implementation.
