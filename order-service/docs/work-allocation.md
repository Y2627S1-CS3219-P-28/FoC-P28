# Order Service Work Allocation

## Current Vincent implementation extension — 2026-10-10

Vincent explicitly approves CREATE/ACCEPT/CANCEL_ACCEPTED recovery across Order
and companion frontend on sprint-2-3-credit-service-concurrency, including the
shared foundation/schema/interface refinements in ADR-033. Stub-only milestone;
no peer edits, live recovery or repost recovery. Historical allocations remain.

## Current concurrency verification branch (2026-10-09)

Vincent additionally approves the fifth pair, courier acceptance versus requester
OPEN cancellation, in CHANGE-093: same isolated PostgreSQL harness and both
winner orderings. Order test-only scope and all peer/recovery boundaries retained.

Vincent explicitly authorizes Order-only real PostgreSQL race tests on
`sprint-2-3-credit-service-concurrency` (CHANGE-092): two acceptances,
cancellation/expiry, acceptance/expiry and manual/automatic completion.
Preserve existing lifecycle/deadline rules; peer calls are test doubles.
Cross-service recovery is deferred. No peer/source/schema/UI design changes.

## One-time approved Credit security exception (2026-10-09)

Vincent explicitly authorizes CHANGE-090 on sprint-2-3-credit: isolate Credit's
Pub/Sub push authentication converter and add its regression tests, plus an
interference handoff. This supersedes the documentation-only restriction solely
for those two Credit source/test paths. Annablee retains Credit ownership; no
business logic, contract, schema, other peer, cloud/IAM or blanket edit approval.

## Credit integration review branch (2026-10-09)

Vincent explicitly requests the Order-side feedback review on `sprint-2-3-credit`
(CHANGE-087), after integrating Credit in `09e04a0`. Scope: inspect peer source,
update Order dependency/context records, no Credit/User/Supplier source or cloud
changes. This does not approve a new architecture or reassign a peer's ownership.
Prior `sprint-2-3` implementation allocation below remains historical context.

## Current Sprint 2-3 workstream (2026-10-08)

Vincent (Developer 2) is assigned the Order-owned Sprint 2-3 work on `sprint-2-3`,
explicitly authorized by the user in CHANGE-081. Source: Project D1 and the local
`Order Service Overall Doc.pdf`. Scope includes the requested lifecycle/history,
repost and completion work; it does not authorize sibling-service implementation.
The one-current-Order plus immutable courier-attempt design, synchronous reset,
V3 migration and requester repost visibility are approved and implemented in
CHANGE-082 / ADR-025. Local test/coverage gates pass, but real providers/consumers,
browser and cloud gates remain; status is `[~]`, not Sprint completion.
The approved shared frontend slice touches only My Errands/My Requests and Order
helpers/tests; no auth/layout/peer feature edits. This allocation does not reassign Yao Xiang's historical work or
grant blanket ownership of shared frontend/configuration files.

## Historical Sprint 1 allocation

| Developer | Developer number | Assigned scope | Branch |
|---|---|---|---|
| Yao Xiang | Developer 1 | Order Service sequence diagrams/features 1-6 | `sprint-1/seq-1-to-6` |
| Vincent | Developer 2 | Order Service sequence diagrams/features 7-11 | `sprint-1/seq-7-to-11` |

## Collaboration rules

- Both developers follow the same Project D1, architecture, contracts, diagrams, specification-driven workflow, and TDD rules.
- Yao Xiang owns sequence diagrams/features 1-6.
- Vincent owns sequence diagrams/features 7-11.
- A developer must not modify the other developer's assigned implementation without agreement.
- Shared contract, architecture, requirement, domain-model, test-fixture, interface, or database changes must be discussed and recorded before implementation.
- Each developer must work on the correct Git branch.
- Each developer must update only their own active-work file.

## Shared frontend allocation

- `../frontend/` is a shared top-level Next.js application; no developer has blanket ownership of it.
- When an approved Order Service feature requires frontend work, feature ownership follows the sequence allocation above, but the developer must recheck frontend Git state and all relevant active-work records before editing.
- Shared API clients, authentication, permissions, layouts, navigation, runtime configuration, and design-system components require coordination and recorded agreement before modification.
- A developer must not alter another developer's frontend feature or overwrite unrelated frontend work without agreement.
- No frontend files are currently allocated or recorded as in progress by this Order Service workflow. Recheck this on every coding turn.

## Feature status values

- `[ ]` Not started.
- `[~]` In progress.
- `[!]` Blocked.
- `[x]` Completed and verified through the non-negotiable completion gate in `AGENTS.md`.

This file records shared team allocation only. The ignored `docs/local/developer-profile.md` identifies the current developer in a local workspace.

## Current Yao Xiang UI/query scope extension — 2026-10-09

The user explicitly assigns CHANGE-094 on order-service/sprint-1/yx-sprint-2-and-3: existing Order frontend My Requests/My Errands status filters, abort wording and Order-only five-second polling, with optional status support in the Order-owned personal query. Initial frontend diff is clean; preserve Vincent history/repost logic and all peer/auth/shared layout/client source. Only feature components/hooks are affected; no blanket shared ownership.

## Narrow scheduler reliability extension — 2026-10-10

Yao Xiang explicitly requests DB-selected and transactionally independent scheduled items on order-service/sprint-1/yx-sprint-2-and-3. CHANGE-096 covers Order lifecycle verification and active outbox selection/per-event transaction/error isolation only. No reassignment of Vincent/peer work, paused background repost retries, frontend or shared configuration.
