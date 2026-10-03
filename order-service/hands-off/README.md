# Order Service Sprint 1 Sequences 1–11 — Handoff to Yao Xiang

**Branch:** `sprint-1/seq-1-to-seq-11`
**Repository area:** `order-service/` plus the approved shared `frontend/` vertical slice
**Handoff owner:** Vincent
**Receiving developer:** Yao Xiang
**Sprint:** Sprint 1

This document is a working handoff, not a replacement for the requirements,
architecture diagrams, contracts, or the peer-service feedback record. Read the
authoritative files before changing code.

## Latest implementation handoff

Yao Xiang's 2026-10-03 transactional-outbox implementation summary for Vincent
is in [CHANGE-063-transactional-outbox-to-vincent.md](CHANGE-063-transactional-outbox-to-vincent.md).
It records the completion/cancellation event flow, implementation locations,
102-test verification result, and remaining Pub/Sub consumer and Cloud Run
recovery caveats. CHANGE-063/ADR-013 and the updated diagrams are authoritative.

## 1. Read these sources first

### Order Service workflow and state

1. `order-service/AGENTS.md`
2. `AGENTS.md` at repository root
3. `order-service/docs/ai-project-context.md`
4. `order-service/docs/ai-project-context.toml`
5. `order-service/docs/current-sprint.md`
6. `order-service/docs/project-d1-reference.md`
7. `order-service/docs/architecture-overall.md`
8. `order-service/docs/architecture-order-service.md`
9. `order-service/docs/architecture-evolution.md`
10. `order-service/docs/architecture-review-playbook.md`
11. `order-service/docs/service-contracts.md`
12. `order-service/docs/peer-service-api-feedback.md`
13. `order-service/docs/requirements-traceability.md`
14. `order-service/docs/change-log.md`
15. `order-service/docs/active-work/vincent.md`
16. `order-service/docs/active-work/yao-xiang.md`
17. Every applicable file in `order-service/docs/decisions/`

### Sprint 1 authority

- `order-service/sprints/sprint-1/scope.md`
- `order-service/sprints/sprint-1/requirements.md`
- `order-service/sprints/sprint-1/architecture-context.md`
- `order-service/sprints/sprint-1/contracts.md`
- `order-service/sprints/sprint-1/acceptance-tests.md`
- `order-service/sprints/sprint-1/class-diagrams/README.md`
- `order-service/sprints/sprint-1/sequence-diagrams/README.md`
- Referenced source pack: `Sprint 1/Order Service Sprint 1 Doc.pdf`

### Frontend authority

- `frontend/AGENTS.md`
- `order-service/docs/frontend-integration-workflow.md`
- `order-service/docs/frontend-ui-style-guide.md`
- `frontend/package.json`
- Current routes/components under `frontend/src/app/`,
  `frontend/src/components/orders/`, `frontend/src/hooks/`, and
  `frontend/src/lib/`

## 2. Current branch and ownership context

This is the user-approved combined implementation branch for all Sprint 1
sequences 1–11. It contains Yao Xiang's sequence 1–6 foundation and Vincent's
sequence 7–11 work in one branch. Do not reset or overwrite the other
developer's commits.

The repository workflow still records the historical allocation as Yao Xiang
owning sequences 1–6 and Vincent owning sequences 7–11. That allocation is
useful for provenance, but this handoff authorizes Yao Xiang to continue the
combined branch after coordinating changes and preserving existing work.

Boundaries remain important:

- Modify Order Service implementation, tests, records, and the approved shared
  frontend slice only.
- Do not modify `user-service/`, `supplier-service/`, `credit-service/`,
  `admin-service/`, or other peer-service source.
- Use `docs/peer-service-api-feedback.md` for missing or unsuitable peer APIs.
- Preserve peer-owned sections in shared Compose, gateway, CI, and environment
  files. Add only Order-specific blocks or comment incompatible peer lines with
  a restoration note.
- Keep small atomic commits with explicit paths and staged `git diff --check`.

## 3. What has already been implemented

### Backend foundation

- Spring Boot 4.1.1 / Java 21 Order Service.
- PostgreSQL persistence with Flyway migrations; Hibernate validates rather
  than silently creating schema.
- `Order` is the lifecycle aggregate root. It owns status, requester/courier
  references, supplier IDs, checkpoints, optimistic version, and two-way repost
  links.
- Command receipts provide command-ID idempotency.
- Repository row locking and expected-version checks protect transitions and
  acceptance races.
- Standard error envelopes, pagination envelopes, OpenAPI bearer metadata, and
  structured local audit logging were added.
- Order Service is configured for PostgreSQL/Cloud SQL and Cloud Run in the
  approved deployment direction; local verification uses PostgreSQL Docker.

### Peer adapter boundary

The application uses ports rather than embedding peer logic in the domain:

- `UserServicePort`: identity, requester role, courier role/eligibility.
- `SupplierServicePort`: supplier-pair validation and supplier lookup.
- `CreditServicePort`: reservation, settlement, and release boundary.
- `OrderOutcomePublisher`: reserved for approved facts; repost event
  publication is deferred for Sprint 1.

`HttpPeerAdapters` forwards the Firebase bearer for real HTTP-peer calls. The
default local `MockPeerAdapters` is deterministic and in-memory; it is not a
production replacement for peer persistence.

### Frontend vertical slice

The shared Next.js frontend already includes:

- Post Request, Browse Errands, My Requests, and My Errands.
- Accept, start, pickup, deliver, complete, cancel, and repost controls.
- Creation-time automatic repost choice; later configuration of an `OPEN`
  order is rejected.
- Manual repost only for an eligible requester-owned `EXPIRED` order.
- Supplier dropdowns showing supplier names/buildings while submitting IDs.
- Order cards showing human-readable locations and hiding internal Order IDs and
  opaque supplier IDs.
- A unified dashboard with no client-side requester/courier mode switch.
- Firebase-authenticated calls through the gateway, including the hard-refresh
  token gate.
- Credit-balance invalidation after successful order creation, repost,
  cancellation, and completion.

## 4. Approved architecture and behavior decisions

Treat these as the effective decisions for this branch. If new evidence
requires a material change, record it in an architecture-evolution/change
record and obtain approval before changing behavior.

- Order persistence is PostgreSQL. Production direction is one Cloud SQL
  PostgreSQL instance with separate staging and production databases, exposed
  to Cloud Run through the approved public-IP Java Connector approach. Local
  development uses the PostgreSQL container in Compose.
- Credit reservation is synchronous: an order is not created as `OPEN` unless
  the Credit Service (or the explicitly documented local mock) accepts the
  reservation. Credit Service remains the owner of balances and ledger state.
- Completion, cancellation, and expiry call the Credit outcome boundary
  synchronously. The local mock performs deterministic balance changes, but
  the real settlement/release provider is still pending in `FEEDBACK-001`.
- Repost event publication is deferred for Sprint 1. Do not introduce a
  broker merely to refresh the browser credit summary; the browser uses a
  post-mutation invalidation/refetch of the authoritative Credit endpoint.
- Automatic repost is selected during initial order creation. An `OPEN` order
  cannot be changed later into an automatic-repost plan. Manual repost is
  available only for an eligible requester-owned expired order.
- The frontend uses one unified dashboard. Client-side requester/courier mode
  switching was removed; backend identity and authorization remain
  authoritative.
- The UI follows the staging baseline: Geist Sans, a white/near-black
  monochrome palette, thin neutral borders, restrained shadows, rounded
  panels, Lucide icons, generous whitespace, and responsive layouts. Internal
  UUIDs, supplier IDs, command IDs, versions, and service URLs are hidden
  unless an operational error genuinely needs them.

## 5. Sequence-by-sequence handoff

Read the corresponding Sprint 1 sequence diagrams before modifying a flow.
The following is the current high-level implementation map.

### Sequence 1 — create an order

`OrderController` authenticates the request and passes a command to the
creation application service. The service verifies the requester through
`UserServicePort`, validates the pickup/delivery pair through
`SupplierServicePort`, validates the aggregate invariants, reserves credits
through `CreditServicePort`, then persists the `Order`, checkpoint, and command
receipt in PostgreSQL. The frontend sends the Firebase UID as the requester
identity and displays supplier names while retaining IDs internally.

Production gate: replace any mock peer call with the real peer contract and
capture authenticated, error, idempotency, and persistence evidence.

### Sequence 2 — view available/open errands

The query controller reads paginated open orders. Supplier references are
resolved through the approved Supplier lookup adapter and rendered as names or
locations. The requester’s own open orders are filtered from the courier-facing
available list. Order UUIDs remain internal to action links and are not shown
as user-facing labels.

Production gate: verify pagination, authentication, supplier lookup failure
fallbacks, loading/error states, and responsive rendering.

### Sequence 3 — accept an order

The command service verifies courier identity/eligibility with User Service,
locks the target row, checks `OPEN`, checks that the actor is not the
requester, checks the expected version, and asks the aggregate to assign the
courier. It persists the assignment, checkpoint, and command receipt.

Production gate: exercise two concurrent acceptors, stale versions, duplicate
command IDs, an unauthorized requester, and a successful courier acceptance.

### Sequences 4–6 — start, pick up, and deliver

The transition service verifies the assigned courier, expected version, and
current status before asking the aggregate to move `ACCEPTED -> IN_PROGRESS`,
`IN_PROGRESS -> PICKED_UP`, and `PICKED_UP -> DELIVERED`. Each transition
persists the new immutable aggregate state, checkpoint, and receipt.

Production gate: verify the assigned-courier rule, illegal status transitions,
stale versions, duplicate commands, and the authenticated UI action path.

### Sequence 7 — complete

The requester-authenticated completion path requires `DELIVERED`, asks Credit
Service to settle/transfer the reserved amount, then persists `COMPLETED` and
the checkpoint. The current local mock models this outcome; the real Credit
settlement endpoint remains unavailable, so this sequence is not yet a live
peer-verified production path.

### Sequence 8 — cancel

The requester-authenticated cancellation path requires an eligible `OPEN`
order, asks Credit Service to release the reservation, then persists
`CANCELLED` and the checkpoint. The local mock models release. Verify the real
release contract before marking this live.

### Sequence 9 — expire

The trusted lifecycle trigger selects due `OPEN` orders whose `courierId IS
NULL`, verifies the lifecycle credential, asks Credit Service to release the
reservation, and persists `EXPIRED` plus the checkpoint. This is a service
trigger, not a browser action. Keep the explicit unassigned predicate; an
accepted order must never be expired by this query.

### Sequence 10 — automatic repost

Lifecycle processing selects an eligible expired order whose creation-time
repost plan is enabled and due, confirms no linked repost already exists,
validates the supplier pair, reserves the future repost credits, creates one
linked `OPEN` repost, and records the link/receipt. A failed validation or
reservation must not create the repost.

### Sequence 11 — manual repost

The requester first receives a draft for an eligible expired order. Draft
creation has no persistence, credit, or supplier side effects. On explicit
submit, the service re-verifies requester ownership/version, validates the
selected supplier pair, reserves credits, creates one linked `OPEN` repost,
and records the command. The UI should expose this only when the order is
expired and eligible; it must not be an edit path for an active order.

## 6. Current integration modes and reported status

### Real HTTP-peer smoke profile

Use the separate override when validating the real User, Supplier, and Credit
reservation calls:

```bash
docker compose -f compose.yaml -f compose.http-peers.yaml up -d --build --force-recreate
```

Vincent reports that sequences 1–6 were exercised against the real local HTTP
peers. Treat that as handoff evidence to reproduce, not as a new `[x]` claim
until the requests, logs, and test results are captured on this branch.

### Deterministic mock profile

Use the default stack for local testing of sequences 7–11 while Credit outcome
endpoints are missing:

```bash
docker compose up -d --build --force-recreate
```

The mock reserves, settles, and releases deterministic in-memory balances for
the Order Service process. It is useful for flow and UI testing, but it is not
evidence that the real Credit ledger was updated.

### Honest branch status

- Sequences 1–6: implementation exists; Vincent reports successful real HTTP
  local testing, pending reproducible evidence and full completion-gate review.
- Sequences 7–11: implementation and local mock behavior exist; real Credit
  settlement/release integration is not verified.
- Overall Sprint 1 status remains `[~]`, not `[x]`, until peer contracts,
  automated tests, browser flows, Docker runtime evidence, and traceability
  are all complete.

## 7. Credit Service handoff and `FEEDBACK-001`

Read `order-service/docs/peer-service-api-feedback.md` before changing an
adapter. `FEEDBACK-001` is the communication channel to Annablee, the Credit
Service owner. Do not edit `credit-service/` or silently invent a provider
contract.

The current Order boundary needs provider-owned, authenticated, idempotent
outcome operations in addition to reservation:

```http
POST /api/credits/orders/{orderId}/settlement
```

Suggested request facts (the provider must confirm the final schema):

```json
{
  "commandId": "unique-order-command-id",
  "requesterId": "firebase-uid",
  "courierId": "firebase-uid",
  "amount": 1,
  "expectedOrderVersion": 7
}
```

Settlement must atomically transfer the reserved amount to the courier and be
safe to repeat with the same command ID.

```http
POST /api/credits/orders/{orderId}/release
```

Suggested request facts:

```json
{
  "commandId": "unique-order-command-id",
  "requesterId": "firebase-uid",
  "amount": 1,
  "outcome": "CANCELLED",
  "expectedOrderVersion": 7
}
```

The provider must confirm whether `outcome` also accepts `EXPIRED`, the exact
error envelope, authentication/service credential, idempotency semantics,
amount representation, and whether expected-order-version is part of the
contract. Add provider contract tests after agreement. Until then, retain the
mock only in explicitly local profiles and label live completion/cancellation/
expiry verification as pending.

## 8. UI polishing direction

Polish the existing vertical slice without changing approved behavior:

- Hide order UUIDs, supplier IDs, command IDs, optimistic versions, raw peer
  URLs, and stack traces from normal cards and forms.
- Keep human-readable supplier/location names, status, expiry, credits, and
  actionable errors.
- Keep the creation-time automatic-repost choice visible only while creating;
  after creation show its state as read-only. Show the manual Repost action
  only for an eligible expired order.
- Reuse AppShell, existing semantic tokens, shadcn/Base UI primitives, and
  Lucide icons. Do not introduce a second visual system.
- Verify keyboard focus, labels, disabled/loading states, error recovery,
  empty states, and widths from 320px through desktop.

## 9. Test and verification plan

Run the deterministic checks from the repository root and record exact output.

### Backend

```bash
cd order-service
./mvnw -B -ntp clean verify
```

Keep the JaCoCo gate at the configured 80% line and branch thresholds. Cover
domain transitions, controller authorization and error envelopes, PostgreSQL/
Flyway persistence, peer adapter contracts, stale-version/concurrency races,
command idempotency, expiry’s explicit `courierId IS NULL` selection, and
automatic/manual repost failure atomicity.

### Frontend

```bash
cd frontend
npm test -- --run
npm run typecheck
npm run lint
npm run build
```

Add RTL/Vitest coverage for auth readiness, creation-time repost choice,
supplier-name rendering, self-order filtering, completion/cancel/repost
actions, credit invalidation, and error/loading/empty states.

### UI and integration walkthrough

With a valid Firebase-emulator account, test every sequence in the browser:
create, list, accept, start, pickup, deliver, complete, cancel, expire,
automatic repost, and manual repost. Capture the relevant request/response,
status, and UI result. Test hard refreshes so protected calls wait for a bearer
token. Use the HTTP-peer profile for real User/Supplier/Credit reservation
checks and the default mock profile for the temporary Credit outcome flows.

### Runtime evidence

Record `docker compose ps`, service logs for failures, and any gateway/network
responses. Do not claim authenticated browser or Docker verification from
static source inspection alone.

## 10. Completion gate

Do not change sequence markers to `[x]` merely because classes or pages exist.
For each sequence, require the mapped requirement/diagram, passing relevant
automated tests, authenticated UI evidence, correct peer contract behavior,
error/idempotency/concurrency checks, and traceability/change-log updates.

Sequences 7–9 and dependent repost paths remain production-incomplete until
the real Credit settlement/release APIs are agreed, implemented by Credit
Service, and verified end to end. A temporary `[~]` exception is acceptable
only while `FEEDBACK-001` is openly tracked and the mock boundary is explicit.

## 11. Git discipline

Keep commits small and focused, for example: backend behavior/tests, frontend
behavior/tests, handoff/docs, and verification records. Stage explicit paths,
run `git diff --check` and `git diff --cached --check`, and inspect `git status`
before committing. Never stage the two existing untracked learning files
unless Vincent explicitly requests that policy change.

## 12. Developer and local workflow files

The workflow currently uses `docs/local/developer-profile.md` and
`docs/active-work/*.md` to rehydrate identity, ownership, blockers, and the
next action on every relevant turn. The same branch does not make those files
safe to delete: removing them would erase workflow state and make future AI
turns ambiguous. They are therefore retained in this handoff.

If the team wants shared-branch identity instead, make that a separate,
approved workflow change: define a replacement identity mechanism, update the
ignore/index policy, migrate active-work state, and have all developers agree
before untracking anything. No developer/local file was deleted or untracked
as part of this handoff.

## 13. Immediate next actions for Yao Xiang

1. Read the source-of-truth files in Section 1 and inspect the current branch;
   do not reset or recreate the existing foundation.
2. Reproduce the reported sequences 1–6 HTTP-peer smoke run and capture the
   exact Docker, gateway, peer, and UI evidence.
3. Run the backend and frontend commands in Section 9; fix regressions with
   tests first and preserve the 80% JaCoCo gate.
4. Contact Annablee through `FEEDBACK-001`, agree the settlement/release
   contract, and add provider contract tests. Keep the local mock until the
   real provider is available and verified.
5. Polish the UI using Section 8, especially hiding technical identifiers and
   keeping repost controls creation-time/expired-only as specified.
6. Verify sequences 7–11 first with the deterministic mock, then repeat the
   relevant flows with real Credit once the peer API is live.
7. Update requirements traceability, active-work records, change records,
   change-log, and AI usage disclosure with exact results. Only then propose
   changing `[~]` to `[x]` for each independently verified sequence.

This handoff is complete when the receiving developer can reproduce the
current branch, identify every mock/real boundary, communicate the open Credit
contract, and continue toward production-level Sprint 1 verification without
changing peer-owned source.
