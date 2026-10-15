# Order completion-hold design

CHANGE-127 / ADR-040. Owner: Yao Xiang, order-service/penalty-integration,
2026-10-14. Approved placement and funding guards implemented; release deferred.

## Requirement and approved boundary

D1 NTH2 (PDF38) and NTH3 (PDF41) require report-related completion holds: prevent
requester and 48-hour automatic completion and final funding during investigation.
General enquiries need no Order hold. Placement does not change Order status,
settle credits or apply penalties.

User selects forwarded requester Firebase authorization: the verified requester
can hold their own active Order. This is not proof of Report Service identity.
Courier penalty-report eligibility belongs to future Report policy. Release is
explicitly deferred; no release endpoint, Report UI or settlement implemented.

## Implemented contract

PUT /api/orders/internal/{orderId}/completion-holds/{reportId}

JSON body: {"requesterId":"forwarded-Firebase-UID"}.

Production requires requester Firebase Bearer authorization. Existing JWT,
Requester role and UID binding reused; local mock/emulator behavior unchanged.
Report IDs are nonblank, <=128 characters, without surrounding whitespace.

Response DTO: {"orderId":"order-id","reportId":"report-id","isCompletionHold":true,"version":1}.
Traditional DTOs/MapStruct; ordinary Order responses expose isCompletionHold too.

Active states: OPEN, ACCEPTED, IN_PROGRESS, PICKED_UP, DELIVERED. Terminal states
reject placement. 200 success/replay; 400 invalid input; 401 unauthenticated;
403 wrong role/identity/owner; 404 missing Order; 409 terminal state, unresolved
command or report binding conflict; 503 authorization dependency unavailable.
No DELETE/release operation.

## Atomic persistence and concurrency

V9 adds orders.completion_hold default false and order_completion_holds
(report_id primary key, order_id foreign key, requester_id, placed_at). V1..V8
unchanged. The service locks the Order and verifies ownership/state, then inserts
the report binding and updates the flag in one transaction. Same-ID replay is
a no-op; multiple reports can hold one Order. A report cannot move across Orders
or requesters.

Infrastructure uses insert-only SQL rather than JPA merge: separate Order locks
cannot protect a global report ID. Unique insertion rejects a concurrent
conflicting binding and rolls back the losing Order flag. Placement increments
Order version, but creates no checkpoint, receipt, outbox or Credit call.

## Guards, scheduler and frontend

Requester completion verifies ownership then hold before version: a stale
pre-hold browser version still receives 409 COMPLETION_ON_HOLD:

"This order has an ongoing report. You can complete it after the report is resolved."

The DELIVERED requester action displays this and disables Confirm completion;
a stale cached flag still shows the backend report message through the toast.

Expiry and auto-completion due queries exclude holds in SQL, with a locked
recheck per independent Order transaction. Held work is skipped and other jobs
proceed. Aggregate guards also block completion, OPEN cancellation/expiry and
accepted abort funding outcomes before Credit reset, refund/transfer outbox or
status change. Courier progress and successful command replay remain unchanged.
A hold is not an OrderStatus.

Existing publication stays after commit. Hold and completion use the same Order
lock; terminal completion cannot be held retrospectively or undo its event.

## Integration and remaining scope

Report Service is absent. FEEDBACK-013 requires its caller to persist/retry the
same report identity, forward requester authorization and wait for placement
confirmation. Order does not verify report existence in Report storage.
Report creation and hold placement are not a distributed atomic transaction.

Holds persist until an approved release/resolution flow. That design must
preserve report replay safety, retain other reports' holds and coordinate
settlement with already-due completion. Drain old Order revisions before enabling
Report traffic. Class/sequence diagrams: ADR-040. Verification: CHANGE-127.
Full NTH2/NTH3 and Sprint remain partial.
