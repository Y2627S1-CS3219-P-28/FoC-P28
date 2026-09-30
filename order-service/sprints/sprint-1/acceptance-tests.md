# Sprint 1 Acceptance Tests

These are planned tests, not executed results. For each authorized feature, write the failing test first and capture red/green evidence.

## Sequence 7 - Confirm completion

- Original requester changes `DELIVERED` to `COMPLETED`.
- Non-requester is unauthorized and state is unchanged.
- Any status other than `DELIVERED` is rejected.
- Stale version conflicts; repeated command ID is idempotent.
- Exactly one valid status remains.
- No completion checkpoint, settlement, overdue evaluation, or auto-completion is triggered in this Sprint 1 slice.

## Sequence 8 - Cancel OPEN

- Original requester changes `OPEN` to `CANCELLED`.
- Non-requester and unauthenticated callers are rejected.
- Accepted or otherwise non-`OPEN` orders cannot be cancelled by this command.
- Stale version conflicts; repeated command ID is idempotent.
- No cancellation checkpoint or Credit outcome processing occurs in this slice.

## Sequence 9 - Expire OPEN

- A due, unassigned `OPEN` order becomes `EXPIRED` and receives one expiry checkpoint.
- An unexpired, assigned, or non-`OPEN` order remains unchanged.
- Equality at the expiry boundary is treated as reached.
- Repeated processing produces no duplicate transition/checkpoint.
- Only trusted lifecycle identity is accepted.
- Credit release/outcome processing is not invoked in this slice.

## Sequence 10 - Automatic repost

- Eligible configured `EXPIRED` original creates one linked `OPEN` repost after supplier validation and confirmed reservation.
- The repost copies approved original data and uses planned credit/timing values.
- No plan, not-yet-due plan, non-expired original, or existing repost produces no new order.
- Invalid/inactive/same supplier pair produces no reservation or repost.
- Rejected/unavailable credit reservation produces no repost; original remains `EXPIRED`.
- Retried or concurrent automatic attempts create at most one repost and at most one effective reservation.

## Sequence 11 - Manual repost

- Original requester receives an editable draft from an `EXPIRED` unreposted order.
- Draft retrieval does not persist an order or reserve credits.
- Non-requester, non-expired original, or already-reposted original is rejected.
- Valid reviewed submission validates suppliers, reserves credits, and creates one linked `OPEN` repost.
- Invalid fields, invalid supplier pair, or reservation rejection creates no repost.
- Automatic/manual race and retried submission still create at most one repost.

## NFR and contract checks

- Unit and acceptance suites enforce at least 80% line and branch coverage when tooling exists.
- Include positive, negative, equivalence-partition, and boundary tests.
- Verify machine-readable logs for actor/system action, timestamp, action, and cross-service calls without secrets.
- Add persistence integration tests with PostgreSQL and contract tests for each consumed port once the project/build and concrete contracts are approved.
- Add frontend tests only after approved UI behavior exists; do not invent screens.

For any later approved frontend slice, follow `docs/frontend-integration-workflow.md`: test the applicable `ADMIN`/`USER` role and Requester/Courier mode, unauthorized and incorrect-mode access, actual request/response/error/auth contracts, responsive behavior from 320 px through 1920 px where applicable, accessibility/loading/error behavior, lint, type checking, configured component/integration/end-to-end tests, and regression. The approved role/client context alone does not authorize a screen or mode switcher.
