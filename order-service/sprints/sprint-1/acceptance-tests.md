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
- The completion status, checkpoint, command receipt, and one resulting-Order snapshot outbox event commit atomically; the serialized event omits checkpoint history while retaining overdue facts.
- The event uses the same `OrderCompletionTaskEvent` for overdue and on-time completion and carries `overdue`/`overdueAt` facts.
- After commit, dispatch is attempted immediately. A Pub/Sub failure leaves the completed Order and pending outbox entry intact; it does not return a transition failure.
- A failed PostgreSQL transaction leaves neither the completion transition nor its outbox event.
- Retried publication preserves the stable event ID; duplicate delivery remains possible and consumers deduplicate.
- A scheduler pass before `deliveredAt + 48 hours` leaves the order `DELIVERED` and writes no completion checkpoint, receipt, or event.
- At exactly 48 hours and after, the scheduler changes an eligible `DELIVERED` order to `COMPLETED` once, with a `lifecycle` checkpoint/receipt and one `OrderCompletionTaskEvent` in the same transaction.
- Scheduler completion uses the same overdue calculation and event payload as requester completion; no second event type is emitted.
- Repeated scheduler passes and a requester completion racing the scheduler produce no duplicate completion checkpoint, receipt, or outbox event; row locking and stable command identity preserve one transition.
- Scheduler selection filters eligible orders in the database by current `DELIVERED` status and delivered-checkpoint cutoff; it does not load all orders for in-memory filtering.

## Sequence 8 - Cancel OPEN

- Original requester changes `OPEN` to `CANCELLED`.
- Non-requester and unauthenticated callers are rejected.
- Accepted or otherwise non-`OPEN` orders cannot be cancelled by this command.
- Stale version conflicts; repeated command ID is idempotent.
- The `CANCELLED` status, checkpoint, receipt, and resulting-Order snapshot cancellation event commit atomically; checkpoint history is not embedded in the event.
- A Pub/Sub failure leaves the committed cancellation and pending outbox entry intact for retry.

## Updated overall Sequence 7 - Cancel ACCEPTED

- Only the courier whose authenticated ID matches `courierId` may cancel the `ACCEPTED` order.
- Before `expiresAt`, Order synchronously calls Credit to hold/reset the existing transaction without refunding. A successful response must precede `ACCEPTED -> OPEN`; clear `courierId`, write an `OPEN` checkpoint and receipt, and publish no accepted-cancellation event.
- If Credit rejects/fails, keep status `ACCEPTED`, retain the assignment, and write no checkpoint, receipt, or cancellation event.
- At/after `expiresAt`, skip the hold, change `ACCEPTED -> ABORTED`, clear assignment, and atomically write checkpoint, receipt, and accepted-cancellation event intent.
- If expiry passes during the synchronous Credit call, recheck after its response and use the expired event path rather than reopening.
- The expired cancellation event asks Credit to refund and User to apply the courier cancellation penalty. The unexpired path has no subscribers because it publishes no event.
- Requester and a different courier are forbidden; no event, checkpoint, receipt, or status write occurs.
- A Pub/Sub failure leaves the expired `ABORTED` transition committed and the event pending for retry.
- The after-commit listener attempts immediately; the cron recovery poller reclaims due pending or expired-lease entries.
- A crash after broker acceptance may cause duplicate delivery; event ID remains stable.
- In Courier mode, My Errands offers cancellation only for an `ACCEPTED` assigned errand, alongside the start action; requester mode and other statuses do not show it.
- Require confirmation before calling `POST /api/orders/{id}/cancel-accepted` with the current actor and expected version. On success, remove either reopened (`OPEN`, no courier) or aborted order from the former courier's list and show a response-state-specific success message; on API failure, retain it and show the error.

## Courier ownership - Accept and progress

- Courier acceptance requires an unassigned `OPEN` order and rejects the order's requester.
- After local validation, acceptance waits for Credit to confirm the reservation assignment to the same courier before changing the Order to `ACCEPTED`.
- Credit failure or a mismatched/incomplete HTTP confirmation leaves the Order `OPEN` and writes no acceptance checkpoint or command receipt.
- Acceptance rechecks expiry after Credit responds; an order that expires during the call is not accepted.
- The mock supports a single assignment, idempotent repeats for the same Order/courier pair, rejection of a different courier, settlement only to the assigned courier, and idempotent hold/reset by Order ID.
- HTTP contract tests verify assignment sends only `courierId`, hold sends no body, authorization forwarding, and synchronous success handling. Live Credit HTTP behavior is not verified because the provider endpoints are missing.
- After local validation, acceptance waits for Credit to confirm the reservation assignment to the same courier before changing the Order to `ACCEPTED`.
- Credit failure or a mismatched/incomplete HTTP confirmation leaves the Order `OPEN` and writes no acceptance checkpoint or command receipt.
- Acceptance rechecks expiry after Credit responds; an order that expires during the call is not accepted.
- The mock supports a single assignment, idempotent repeats for the same Order/courier pair, rejection of a different courier, settlement only to the assigned courier, and idempotent hold/reset by Order ID.
- HTTP contract tests verify assignment sends only `courierId`, hold sends no body, authorization forwarding, and synchronous success handling. Live Credit HTTP behavior is not verified because the provider endpoints are missing.
- A non-null existing `courierId` prevents acceptance even if the status is `OPEN`.
- Only the assigned courier may start, pick up, deliver, or cancel an accepted order.
- Completion and `OPEN` cancellation remain restricted to the original requester.

## Sequence 9 - Expire OPEN

- A Spring scheduled pass finds due, unassigned `OPEN` orders and makes each `EXPIRED` with one expiry checkpoint.
- The `EXPIRED` status, checkpoint, and one `OpenOrderRefundTaskEvent` outbox row commit atomically; the event includes the resulting Order fields but omits checkpoint history.
- The event is dispatched after commit through its typed publisher; publication failure leaves the expired Order committed and the event pending for retry.
- Credit consumes `OpenOrderRefundTaskEvent` and refunds/releases the reserved transaction; Order does not call Credit's release endpoint synchronously. Requester cancellation uses the same event/topic with `order.status=CANCELLED`.
- An unexpired, assigned, or non-`OPEN` order remains unchanged.
- Equality at the expiry boundary is treated as reached.
- Repeated or concurrent scheduler passes produce no duplicate transition, checkpoint, or event.
- Scheduler exceptions are logged and a later scheduled pass retries eligible records.

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
