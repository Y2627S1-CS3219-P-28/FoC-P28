# Sprint 1 Acceptance Tests

## CHANGE-057 - Admin all-orders query supporting NTH1

- Only a caller whose User Service role-context includes `admin` may list orders; anonymous callers receive 401 and authenticated non-admin callers receive 403.
- Omitted `status` returns orders across every lifecycle status; a supplied status returns only that status.
- `page` is one-based, defaults to 1, and `size` defaults to 20 and is capped at 100.
- Results are ordered newest-created first and use the existing `OrderPageResponse` envelope with page, size, totalItems, and totalPages.
- Invalid status/page parameters return 400; User Service role lookup failure fails closed.

The criteria below define expected behavior. CHANGE-063 tests include a PostgreSQL rollback assertion proving the order and event intent disappear together; implementation and verification results are recorded in the active-work record and change record.

## Sequence 7 - Confirm completion

- Original requester changes `DELIVERED` to `COMPLETED`.
- Non-requester is unauthorized and state is unchanged.
- Any status other than `DELIVERED` is rejected.
- Stale version conflicts; repeated command ID is idempotent.
- Exactly one valid status remains.
- The completion status, checkpoint, command receipt, and one full-resulting-snapshot outbox event commit atomically.
- The event uses the same `OrderCompletionTaskEvent` for overdue and on-time completion and carries `overdue`/`overdueAt` facts.
- After commit, dispatch is attempted immediately. A Pub/Sub failure leaves the completed Order and pending outbox entry intact; it does not return a transition failure.
- A failed PostgreSQL transaction leaves neither the completion transition nor its outbox event.
- Retried publication preserves the stable event ID; duplicate delivery remains possible and consumers deduplicate.

## Sequence 8 - Cancel OPEN

- Original requester changes `OPEN` to `CANCELLED`.
- Non-requester and unauthenticated callers are rejected.
- Accepted or otherwise non-`OPEN` orders cannot be cancelled by this command.
- Stale version conflicts; repeated command ID is idempotent.
- The `CANCELLED` status, checkpoint, receipt, and full-resulting-snapshot cancellation event commit atomically.
- A Pub/Sub failure leaves the committed cancellation and pending outbox entry intact for retry.

## Updated overall Sequence 7 - Cancel ACCEPTED

- Assigned courier changes `ACCEPTED` to `ABORTED`; clear assignment and commit checkpoint, receipt, and cancellation event intent atomically.
- Requester and a different courier are forbidden; no event, checkpoint, receipt, or status write occurs.
- An accepted cancellation requires the authenticated courier ID to match the order's `courierId`.
- A Pub/Sub failure leaves the `ABORTED` transition committed and the event pending for retry.
- The after-commit listener attempts immediately; the cron recovery poller reclaims due pending or expired-lease entries.
- A crash after broker acceptance may cause duplicate delivery; event ID remains stable.
- In `USER` Courier mode, My Errands offers cancellation only for an `ACCEPTED` assigned errand, alongside the start action; requester mode and other statuses do not show it.
- Require confirmation before calling `POST /api/orders/{id}/cancel-accepted` with the current actor and expected version. On success, remove the now-`ABORTED` order from My Errands; on API failure, retain it and show the existing error feedback.

## Courier ownership - Accept and progress

- Courier acceptance requires an unassigned `OPEN` order and rejects the order's requester.
- A non-null existing `courierId` prevents acceptance even if the status is `OPEN`.
- Only the assigned courier may start, pick up, deliver, or cancel an accepted order.
- Completion and `OPEN` cancellation remain restricted to the original requester.

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
