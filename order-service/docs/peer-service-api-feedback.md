# Peer Service API Feedback

Current handoff: **2026-10-09**, Vincent, sprint-2-3, CHANGE-085.
User explicitly requested replacing the whole document. Earlier proposals remain
recoverable in Git (including c993591); stable FEEDBACK IDs are retained below.
This is a peer-work request, NOT permission for Order to edit peer source, approve
contracts for peer owners, or claim live integration from stub tests.

## Status and boundaries

Inspection evidence: Credit api/CreditController and reservation/balance DTOs;
User role-context, courier-eligibility and Firebase authentication; Supplier pair
validation/lookup; Order adapters, event DTO/mapper, transitions, lifecycle/outbox.
Assignment/reset routes and outcome subscribers were not found in the inspected
peer src trees. Source inspection is NOT a deployed-service verification.

| Owner | Needed capability | Classification / stopping point |
| --- | --- | --- |
| Credit (Annablee) | Assign courier before ACCEPTED | MISSING, FEEDBACK-004; Order stub exists |
| Credit (Annablee) | Clear courier on EVERY abort, retain reservation | MISSING, FEEDBACK-003; Order stub exists |
| Credit (Annablee) | Refund and completion consumers | MISSING, FEEDBACK-002; Order publisher/outbox exist |
| User | Abort penalty and completion consumers | MISSING, FEEDBACK-002; Order publisher exists |
| Credit (Annablee) | Active reservation confirmation/reconciliation | INCOMPLETE_OR_INCOMPATIBLE for durable retries, FEEDBACK-006 |
| User, Supplier, Credit | Trusted delegated background calls | MISSING, FEEDBACK-005; documentation ONLY |
| Supplier | Pair validation and batch lookup | Existing routes; no extra foreground business API needed |

**Latest user decision: pause ALL background retry implementation until peers agree.**
No durable retry task, candidate-ID persistence or retry worker is implemented.
Existing automatic repost code is not thereby a verified trusted-peer integration.
Foreground manual repost uses the current Firebase user token; HTTP mode NEVER
falls back to mocks. Mock mode is a separate test configuration.
Visible-page polling reads lists/balance every 15 seconds and on focus/mutation;
it does not trigger reposts, perform refunds or replace a peer consumer.

## 1. Credit Service — Annablee

### FEEDBACK-004: synchronous courier assignment

Order-side approved requirement; provider agreement/implementation pending.

~~~http
PUT /api/credits/orders/{orderId}/courier-assignment
Authorization: Bearer <accepting courier Firebase ID token>
Content-Type: application/json
~~~

~~~json
{ "courierId": "courier-firebase-uid" }
~~~

- orderId is the existing business ID with a requester reservation. No Order
  optimistic-lock version is sent in this request.
- Authenticate the accepting courier and bind token UID to courierId. Verify
  their Credit account exists and reservation is active/assignable. Do NOT apply
  the reservation PUT's requester-only rule to this accepting courier.
- Atomically persist courier assignment with Credit's state checks.
- Expected response: **exactly 200 OK, EMPTY body**, after confirmation. Order
  then rechecks local deadline/version before persisting ACCEPTED.
- Same assignment replay must be idempotent. Reject incompatible assignments and
  terminal reservations; replacement after an agreed reset must be supported.
- Agree errors: 400 invalid input, 401 invalid token, 403 wrong caller, 404 missing
  account/reservation, 409 state/assignment conflict, 503 temporary failure.
  Section 4 gives the proposed envelope; no failure may masquerade as success.
- Timeout/local rollback after Credit success can leave assignment while Order
  remains OPEN. Agree reconciliation for this separate-database boundary before
  production; synchronous calling alone does not make the writes atomic.

### FEEDBACK-003: synchronous clear-courier / hold-for-reopen

Order-side agreed bodyless contract; provider implementation pending.

~~~http
POST /api/credits/orders/{orderId}/hold-for-reopen
Authorization: Bearer <assigned courier Firebase ID token>
~~~

Request: **NO body**. Expected response: **exactly 200 OK, NO body**.

- Called for EVERY accepted-order abort BEFORE committing Order history/outcome.
  Authorize the caller against the reservation's assigned courier.
- Atomically set courierId=null and retain the SAME active reservation/amount.
  Replay must be safe/idempotent. Agree replay authorization after courierId is
  null; do not grant arbitrary callers permission to clear assignments.
- No synchronous refund here. If original acceptance expiry is future, current
  Order becomes OPEN with the SAME business ID/reservation. If passed, it becomes
  EXPIRED and separately queues the old-ID refund event.
- Failure leaves Order ACCEPTED with no new ABORTED attempt/outcome event.
- Every successful abort creates immutable courier history and a User penalty
  event, whether the current outcome is OPEN or EXPIRED.
- Agree 400/401/403/404/409/503 errors, lost-response recovery and concurrent
  assignment/start/reset semantics. Do not fall back to mocks in HTTP mode.

### FEEDBACK-002 (Credit): refund subscriber

| Item | Required value |
| --- | --- |
| Development topic | open-order-refund-dev-v1 |
| Project | protean-vigil-509704-q4 |
| Production topic | Configured ORDER_OPEN_REFUND_TOPIC; coordinate actual environment |
| Event type/version | OpenOrderRefundTaskEvent / 1 |
| Intended consumer | Credit Service |

Exact full envelope/snapshot is in section 4. Distinguishing-field excerpt:

~~~json
{
  "eventId": "stable-event-uuid",
  "eventType": "OpenOrderRefundTaskEvent",
  "eventVersion": 1,
  "orderId": "old-business-order-id",
  "orderVersion": 4,
  "occurredAt": "2026-10-09T04:00:00Z",
  "actorId": "lifecycle",
  "order": { "id": "old-business-order-id", "status": "EXPIRED" }
}
~~~

Other snapshot fields are omitted ONLY in this explanatory excerpt; section 4
defines the full message. Publish conditions: requester cancels OPEN (CANCELLED),
minute scheduler expires unassigned OPEN (EXPIRED), or courier abort resolves
EXPIRED. Refund the reservation identified by **this old ID** exactly once;
restore requester availability and atomically persist Credit ledger/reservation.
Abort returning OPEN is NOT refunded. A repost reserves a DIFFERENT new ID.

Expected return: **Pub/Sub ACK after Credit transaction commits**, not JSON to
Order. Duplicate delivery must ACK without another refund. Temporary failures
must retry; malformed/conflicting permanent events need observable dead-letter/
reconciliation handling, not silently successful ACK.

### FEEDBACK-002 (Credit): completion subscriber

Development topic **order-completion-dev-v1**, production from
ORDER_COMPLETION_TOPIC. Event OrderCompletionTaskEvent, version 1, full
COMPLETED snapshot, assigned courierId and top-level overdue/overdueAt.

Settle/transfer the reserved credits to that courier exactly once in an atomic
balance/ledger/reservation transaction. Validate recipient, amount and reservation
state against Credit-owned records. Credit owns financial policy; Order sends
facts, not deduction calculations. ACK AFTER durable effects commit; no
synchronous settlement HTTP response is required in this effective flow.

**FEEDBACK-001 is SUPERSEDED:** old synchronous release/settlement proposals are
not the current required outcome APIs. A legacy settle adapter method does not
make it part of the effective workflow. Implement consumers, not two mechanisms
for the same financial outcome. Do NOT consume new accepted-cancellation events
as refunds: they are User penalty facts. Coordinate earlier ABORTED-event refund
consumers/queued legacy messages so rollout does not double-refund. Do not delete
history/outbox to avoid reconciliation.

### FEEDBACK-006: reservation confirmation and future retry safety

These provider endpoints already exist:

~~~http
PUT /api/credits/orders/{newOrderId}/reservation
Authorization: Bearer <requester Firebase ID token>
Content-Type: application/json
~~~

~~~json
{ "requesterId": "requester-firebase-uid", "amount": 5 }
~~~

Current response: 201 for new, 200 for identical replay:

~~~json
{
  "orderId": "new-business-order-id",
  "requesterId": "requester-firebase-uid",
  "courierId": null,
  "amount": 5,
  "status": "RESERVED",
  "balance": {
    "userId": "requester-firebase-uid",
    "totalBalance": 50,
    "reservedBalance": 5,
    "usableBalance": 45,
    "version": 2,
    "asOf": "2026-10-09T04:00:00Z"
  },
  "createdAt": "2026-10-09T04:00:00Z",
  "updatedAt": "2026-10-09T04:00:00Z",
  "refundedAt": null,
  "paidAt": null
}
~~~

GET /api/credits/orders/{newOrderId}/reservation exists, requester-authenticated;
200 returns the same reservation fields, but **balance is omitted** on GET.
404 means not observed, not proof an in-flight PUT cannot commit later.

Before implementing durable retries, agree and verify:

- Matching ID/requester/amount and ACTIVE RESERVED must be confirmed before OPEN.
  An identical replay of REFUNDED/PAID must not be treated as a fresh active hold.
  Inspected provider currently returns existing matching reservations; agree
  terminal-state behavior and add negative tests.
- One candidate NEW business UUID per saved attempt, reused across uncertain
  writes via GET/idempotent PUT. Candidate IDs are NOT identity credentials.
- Compensation/reconciliation for remote success + local rollback, late success
  after expiry, missing/invalid/mismatched confirmations and concurrent attempts.
- Current Order reserve adapter ignores success body; CHANGE-085 fixes semantic
  errors only. Confirmation/recovery remains NOT implemented; no new unapproved
  recovery route is invented here.

Existing semantic distinction: **409 INSUFFICIENT_CREDITS** versus
**409 RESERVATION_CONFLICT**. Order preserves the first code for manual UI;
other 409s remain conflicts. 401/403 stop; temporary transport/server failure
does not prove low funds. Delayed refund and inadequate funds can cause the SAME
balance error: do not label refund pending without authoritative Credit facts.
No new CreditsRefunded event or refund-confirmation gate is approved here.

GET /api/credits/me already serves balance polling; no new API is required for
this UI. Missing accounts require the existing signup registration-facts flow.

## 2. User Service

### FEEDBACK-002 (User): courier-abort penalty subscriber

Development topic **accepted-order-cancellation-dev-v1**; production from
ORDER_ACCEPTED_CANCELLATION_TOPIC. Event AcceptedOrderCancellationTaskEvent,
version 1, exact envelope/snapshot in section 4.

- EVERY successful ACCEPTED-only courier abort emits this after synchronous
  Credit reset: both OPEN and EXPIRED current outcomes.
- **actorId is the aborting courier**. Snapshot courierId is already null.
  Do not charge the requester or try to identify the offender using null.
- Snapshot status is OPEN or EXPIRED, NOT ABORTED. Immutable ABORTED attempt
  history is separate and is not embedded in these messages.
- Deduplicate eventId and apply User-owned penalty/score/suspension policy once.
  Do not calculate refunds or financial deductions on this topic.
- ACK after durable User effects commit. Retry temporary failures and provide
  monitored dead-letter/reconciliation handling for permanent failures.

### FEEDBACK-002 (User): completion-facts subscriber

Create a **separate User subscription** to order-completion-dev-v1 (production
ORDER_COMPLETION_TOPIC). Do not share a competing-consumer subscription with
Credit: BOTH services need every completion.

OrderCompletionTaskEvent carries the COMPLETED snapshot and overdue boolean/
timestamp. Identify courier via snapshot courierId, not actorId (which may be
requester or lifecycle). Apply User-owned on-time/late policy once by eventId,
ACK after commit. Order does not prescribe a new numeric penalty rule.

Existing GET /api/users/role-context returns userId/roles from authenticated
Firebase UID and User-owned role records:

~~~json
{ "userId": "firebase-uid", "roles": ["requester", "courier"] }
~~~

Existing GET /api/users/courier-eligibility provides eligibility. These are not
delegated requester APIs for a service identity; FEEDBACK-005 covers that gap,
not a candidate-ID approval lookup.

## 3. Supplier Service

No missing foreground business API found for current Order needs:

~~~http
POST /api/suppliers/validate
Authorization: Bearer <Firebase user ID token>
Content-Type: application/json
~~~

~~~json
{ "pickupSupplierId": "pickup-id", "deliverySupplierId": "delivery-id" }
~~~

200 valid: { "valid": true, "problems": [] }.
200 invalid:

~~~json
{
  "valid": false,
  "problems": [
    { "field": "pickupSupplierId", "supplierId": "pickup-id", "reason": "INACTIVE" }
  ]
}
~~~

Reasons include NOT_FOUND, INACTIVE, SAME_SUPPLIER. Order already requires
explicit valid:true; false/missing confirmation fails closed (CHANGE-083).
Existing authenticated POST /api/suppliers/lookup with
{ "ids": ["pickup-id", "delivery-id"] } supplies display resolution.
No new subscription or catalogue-write permission is requested.
Only a restricted trusted validation/read authorization extension is needed
for unattended calls, if peers approve FEEDBACK-005.

## 4. Exact event data, ACK and error formats

Authoritative serialization: messagingpublisher/dto/*TaskEvent.java,
OrderEventSnapshot, RepostPlanEventSnapshot, OrderTaskEventMapper.

Full completion JSON message data:

~~~json
{
  "eventId": "stable-event-uuid",
  "eventType": "OrderCompletionTaskEvent",
  "eventVersion": 1,
  "orderId": "business-order-id",
  "orderVersion": 7,
  "occurredAt": "2026-10-09T04:00:00Z",
  "actorId": "requester-firebase-uid",
  "order": {
    "id": "business-order-id",
    "requesterId": "requester-firebase-uid",
    "courierId": "courier-firebase-uid",
    "itemDescription": "Collect a parcel",
    "pickupSupplierId": "pickup-id",
    "deliverySupplierId": "delivery-id",
    "offeredCredits": 5,
    "status": "COMPLETED",
    "createdAt": "2026-10-09T01:00:00Z",
    "expiresAt": "2026-10-09T02:00:00Z",
    "deliveryTimeLimitMinutes": 15,
    "version": 7,
    "originalOrderId": null,
    "repostedOrderId": null,
    "repostPlan": {
      "enabled": true,
      "dueAt": "2026-10-09T02:15:00Z",
      "creditAmount": 6,
      "deliveryDurationMinutes": 30,
      "used": false
    }
  },
  "overdue": false,
  "overdueAt": null
}
~~~

Refund and abort use the SAME envelope/snapshot keys, but **omit overdue and
overdueAt entirely**. Change eventType, resulting status/courier/actor according
to the sections above. repostPlan may be null; links/IDs may be null as applicable.
There is NO checkpoint list, row UUID, attemptId, generic facts or outcomeType.
Current snapshot has NO repostExpiresAt; explicit next-expiry remains an
unimplemented approved timing target, not a new wire field in this handoff.

Order commits status and event intent atomically, attempts immediate after-commit
publication, then recovers pending/failed rows every 15 minutes for ALL three
events. Pub/Sub message-ID success means broker acceptance, NOT completed refund,
settlement or penalty. Consumer owners provision separate environment-matched
subscriptions, least-privilege subscriber IAM, durable deduplication, retry/
dead-letter monitoring and recovery. Pull ACK or authenticated push/function
handler is for owners to agree/provision; no callback endpoint is invented here.
Push wraps JSON as base64 message.data; decode before validating type/version,
envelope/snapshot IDs. Duplicate delivery is expected.

Proposed standard error for the missing HTTP routes:

~~~json
{
  "status": 409,
  "error": "CONFLICT",
  "message": "Reservation state does not permit this operation.",
  "path": "/api/credits/orders/business-order-id/courier-assignment",
  "timestamp": "2026-10-09T04:00:00Z",
  "details": []
}
~~~

Reservation INSUFFICIENT_CREDITS/RESERVATION_CONFLICT are already provider-defined;
do not infer from error text. Missing-route errors/replay semantics require peer
agreement. Events return ACK, NOT a JSON result to Order.

## 5. FEEDBACK-005: trusted background authorization — documentation ONLY

**DO NOT IMPLEMENT YET. ALL background retry implementation is PAUSED by user.**
Involved: **Order, User, Supplier, Credit, platform/deployment IAM owner**.

Automatic repost and saved manual retries would run without a browser. Firebase
user ID tokens expire. A requesterId, candidateId, lifecycle secret or
verified=true flag is not authenticated delegated authority. Foreground Firebase
token forwarding remains unchanged.

Proposal to discuss, NOT a selected credential protocol:

| Service | Required work after agreement |
| --- | --- |
| Order | Authenticate using renewable restricted service identity; prove saved consent/ownership; send delegated requester through agreed contract; no saved user refresh tokens |
| User | Verify trusted Order caller AND authorize requester role lookup/delegated action using current roles/revocation; current token-derived role-context cannot treat service subject as requester UID |
| Supplier | Verify trusted caller; permit only agreed pair validation/read operations; keep activation rules; no writes |
| Credit | Verify trusted caller; authorize only agreed on-behalf-of reservation/read/reset/assignment; enforce ownership/account/state/idempotency without arbitrary requesterId bypass |
| Platform | Agree identities, issuer/audience, token transport, least privilege, renewal/rotation, local testing and secret handling; no shared service-account keys |

Peers can implement service-verification middleware/function plus restricted
delegated endpoints, or extend existing routes with explicitly distinguished
service authorization. **Exact routes, headers, claims, audience and additional
JSON are TBD with peers.** Cloud Run IAM identity is a candidate proof, not
sufficient application delegation by itself. A restricted User lookup would take
delegated requesterId and return agreed userId/roles facts only to authorized
callers; no unapproved route is prescribed.

Agree wrong-service/audience/expired-token rejection, requester/operation scope,
revoked roles/consent, renewal, replay, audit attribution and error semantics.
Tests must cover positive consent, forged requester, unpermitted actions, wrong
caller, revocation, token renewal, duplicate operations and local/cloud runtime.

Until agreement: NO retry worker (including mock mode), anonymous peer calls,
indefinite cached Firebase token, bypass, or HTTP fallback to mocks.
Same-candidate persistence/backoff/reconciliation remains an approved design on
hold. Current manual errors and authenticated read polling work independently.

## Owner handoff / closing criteria

1. Annablee agrees/implements FEEDBACK-003/004 and both Credit subscriptions;
   coordinate FEEDBACK-006 before durable retry work.
2. User owner agrees/implements abort and completion subscriptions separately.
3. User/Supplier/Credit/platform agree FEEDBACK-005 before background work resumes.
4. Peer completion reports are READY_FOR_VERIFICATION, not VERIFIED. Reinspect
   source/DTO/security/tests, run authenticated positive/negative/idempotency/
   concurrency/timeout/consumer tests against isolated data before closing.
5. Order stub/publisher/RTL tests do not prove peer money movement, penalties,
   IAM/subscriptions or Sprint completion. Peer source/infrastructure unchanged.
