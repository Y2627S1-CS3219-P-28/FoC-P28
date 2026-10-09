# Peer Service API Feedback

Current handoff: **2026-10-09**, Vincent, `sprint-2-3-credit`, CHANGE-087.
Inspected integrated revision: `09e04a0` (`pull: credit service`).

This user-requested rewrite removes already-implemented Credit routes and
refund/completion handlers from the **missing implementation** list. Section 6
retains a small evidence register, not requests to build those capabilities again.
Remaining gaps include incompatible behavior, not just absent route names.
Source inspection and existing test source are NOT executed tests or confirmation
that GCP subscriptions/IAM/deployed consumers are working. No entry is upgraded
to VERIFIED solely because its implementation exists.

CHANGE-086 follow-up: Order now stores explicit automatic new expiry and latest
safe manual/automatic attempt failure code/message/time; UI reload reads them.
These are Order API/storage fields, NOT new peer request/event fields. Existing
reservation/assignment/reset/topic contracts below are unchanged. No new provider
route is needed for latest-outcome persistence. Trusted background credentials,
same-candidate durable tasks and all background retries remain paused; FEEDBACK
005/006 still require peer agreement. This does not verify auto repost in HTTP mode.
User explicitly requested replacing the whole document. Earlier proposals remain
recoverable in Git (including c993591); stable FEEDBACK IDs are retained below.
This is a peer-work request, NOT permission for Order to edit peer source, approve
contracts for peer owners, or claim live integration from stub tests.

## Status and boundaries

Inspection evidence: CreditController/DTOs, CreditService, JpaCreditRepository,
CreditOrderEventController, CreditOrderEventConsumer/OrderEventMessage,
CreditPushProperties, both security chains/role provider/push-token validator,
relevant API/persistence/consumer/security test source, application/deployment
configuration and infra/gcp/configure-credit-pubsub.sh; compared with Order's
HttpPeerAdapters, event DTOs/mapper and OrderTransitionService under ADR-025/028.
User event-consumer source search found no matching handlers. Supplier contracts
below retain the earlier inspection; no Supplier implementation change is claimed.

| Owner | Needed capability | Classification / stopping point |
| --- | --- | --- |
| Credit (Annablee) | Restrict cleared-reservation reset replay to the authorized attempt/caller | INCOMPLETE_OR_INCOMPATIBLE, FEEDBACK-003; route EXISTS |
| Credit (Annablee), platform owner | Align accepted-cancellation subscription with current User-only penalty flow | INCOMPLETE_OR_INCOMPATIBLE, FEEDBACK-007; legacy Credit refund handler EXISTS but is unsuitable |
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

## 1. Credit Service — Annablee: remaining work only

### FEEDBACK-003: reset replay authorization and stale-attempt protection

Status: OPEN. Classification: INCOMPLETE_OR_INCOMPATIBLE for replay security.
The endpoint and core reset/hold behavior are implemented; do NOT recreate them.

~~~http
POST /api/credits/orders/{orderId}/hold-for-reopen
Authorization: Bearer <assigned courier Firebase ID token>
~~~

Request: **NO body**. Expected response: **exactly 200 OK, NO body**.

- Evidence: CreditController requires ROLE_COURIER; JpaCreditRepository locks the
  reservation and requires RESERVED. An assigned caller mismatch is rejected.
  BUT it returns success immediately when courierId is null, before checking
  callerId. Thus any caller with the courier role can obtain a successful reset
  response for an unassigned reservation, not only the courier replaying a reset.
- Required: retain enough Credit-owned authorization/replay evidence to authorize
  the legitimate cleared-assignment replay; deny an unrelated courier. Agree
  protection against stale resets after reassignment, including the same courier
  accepting again. Current path/token alone cannot distinguish same-courier
  attempts; any new command/attempt/generation field requires owner/user approval.
  This review does NOT add a field or select that design.
- Preserve the existing no-body request and bodyless 200 unless a coordinated
  contract amendment is approved. No refund in this call; retain reserved amount.
  Unauthorized replay should return 403; incompatible state/generation 409;
  invalid token 401; missing reservation 404; persistence failure 503, using the
  existing envelope in section 4. No tokens may be saved as replay proof.
- Provider tests must cover unrelated caller after null reset, legitimate lost-
  response replay, reassignment, repeated same-courier attempts and concurrency.
  Existing tests cover wrong caller while assigned, not the cleared-state gap.
- Order still calls this BEFORE every abort. On failure it remains ACCEPTED with
  no new history/outcome event. On success current state resolves OPEN/EXPIRED;
  every abort emits User penalty facts, and EXPIRED additionally emits refund.

### FEEDBACK-007: Credit's accepted-cancellation consumer uses superseded semantics

Status: OPEN. Classification: INCOMPLETE_OR_INCOMPATIBLE.
Owners: Annablee plus subscription/platform owner; coordinate User owner.

Approved current contract (ADR-025):

- Topic `accepted-order-cancellation-dev-v1` / production
  `accepted-order-cancellation-prod-v1` (ORDER_ACCEPTED_CANCELLATION_TOPIC).
- Event `AcceptedOrderCancellationTaskEvent`, version 1. Section 4 full envelope;
  actorId is aborting courier; order.courierId=null; order.status is OPEN or
  EXPIRED. This is **User penalty signaling, NOT a Credit refund instruction**.
- OPEN keeps its same reservation. EXPIRED receives a separate
  `OpenOrderRefundTaskEvent` on `open-order-refund-dev-v1` / prod-v1 for Credit.

Actual implementation: CreditOrderEventConsumer routes accepted cancellation
to CreditService, whose validateOutcome requires ABORTED and whose processOutcome
calls repository.refund. Its tests use ABORTED snapshots. The provisioning script
also creates `credit-accepted-order-cancellation-dev-v1` / prod-v1 subscriptions.
Consequently current Order OPEN/EXPIRED penalty events are rejected (400), rather
than implementing the approved flow. Simply allowing those statuses while still
refunding is UNSAFE: an OPEN reopened order must retain its reservation.

Required peer action: align Credit subscription provisioning/routing with the
current financial topics (refund and completion only); keep accepted-cancellation
penalties in User Service. Coordinate any legacy ABORTED messages/subscription
cutover and dead-letter recovery explicitly; do not silently delete queued work
or rewrite Order snapshots to ABORTED. No extra Credit endpoint or response is
required by this current penalty flow. How to handle legacy deliveries is an
owner-agreed rollout decision, not an implemented assumption in this review.

Required tests: abort before expiry -> reservation remains RESERVED; abort at/
after expiry -> separate refund event releases once; delivery order/duplicates do
not double-release; User separately receives every abort. Do NOT ACK incompatible
legacy financial messages without a reviewed reconciliation plan.

### FEEDBACK-006: reservation confirmation and future retry safety

Status: OPEN. Classification: INCOMPLETE_OR_INCOMPATIBLE for future retry safety;
the existing PUT/GET routes are implemented, not missing.

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
  JpaCreditRepository.replayReservation currently checks requester/amount but
  not status, returning an existing REFUNDED/PAID reservation with 200 as well.
  Agree terminal-state behavior and test it; do not silently reactivate/pay twice.
- One candidate NEW business UUID per saved attempt, reused across uncertain
  writes via GET/idempotent PUT. Candidate IDs are NOT identity credentials.
- Compensation/reconciliation for remote success + local rollback, late success
  after expiry, missing/invalid/mismatched confirmations and concurrent attempts.
  This includes courier assignment succeeding remotely before Order acceptance
  fails, and reset succeeding before Order abort commits. FEEDBACK-004's route
  is implemented, but a multi-database recovery protocol is not established by it.
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
Current event snapshot has NO repostExpiresAt. CHANGE-086 implements that field
in Order storage/API/UI, but intentionally does NOT add it to the peer event
snapshot. Credit's RepostPlanSnapshot therefore still matches these five fields.

Order commits status and event intent atomically, attempts immediate after-commit
publication, then recovers pending/failed rows every 15 minutes for ALL three
events. Pub/Sub message-ID success means broker acceptance, NOT completed refund,
settlement or penalty. Consumer owners provision separate environment-matched
subscriptions, least-privilege subscriber IAM, durable deduplication, retry/
dead-letter monitoring and recovery. Pull ACK or authenticated push/function
handler is for owners to agree/provision; no callback endpoint is invented here.
Push wraps JSON as base64 message.data; decode before validating type/version,
envelope/snapshot IDs. Duplicate delivery is expected.

Existing Credit HTTP error envelope (example assignment state conflict):

~~~json
{
  "status": 409,
  "error": "RESERVATION_CONFLICT",
  "message": "Reservation state does not permit this operation.",
  "path": "/api/credits/orders/business-order-id/courier-assignment",
  "timestamp": "2026-10-09T04:00:00Z",
  "details": []
}
~~~

Reservation INSUFFICIENT_CREDITS/RESERVATION_CONFLICT are already provider-defined;
do not infer from error text. Credit also distinguishes ACCOUNT_NOT_FOUND and
RESERVATION_NOT_FOUND (404), EVENT_CONFLICT (409), VALIDATION_ERROR (400),
UNAUTHENTICATED (401), FORBIDDEN (403), SERVICE_UNAVAILABLE (503). Replay/recovery
gaps require peer agreement. Events return ACK, NOT a JSON result to Order.

## 5. FEEDBACK-005: trusted background authorization — documentation ONLY

**DO NOT IMPLEMENT YET. ALL background retry implementation is PAUSED by user.**
Involved: **Order, User, Supplier, Credit, platform/deployment IAM owner**.

Automatic repost and saved manual retries would run without a browser. Firebase
user ID tokens expire. A requesterId, candidateId, lifecycle secret or
verified=true flag is not authenticated delegated authority. Foreground Firebase
token forwarding remains unchanged.

Proposal to discuss, NOT a selected credential protocol:

Credit now authenticates **Pub/Sub incoming push** with Google OIDC, but that
is a different trust boundary from Order making delegated outbound reservation
calls. Its business routes still use Firebase caller UID, and reservation still
requires UID=requesterId. Push authentication does NOT close FEEDBACK-005.

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

## 6. Implemented Credit capabilities — evidence, NOT outstanding build requests

These have been ruled out as absent endpoints/handlers on `09e04a0`.
Status: READY_FOR_VERIFICATION where compatible; NOT live VERIFIED. Their source
tests were inspected, not executed in this documentation-only review.

| Feedback / capability | Actual implemented contract | Source comparison |
| --- | --- | --- |
| FEEDBACK-004: courier assignment | PUT /api/credits/orders/{orderId}/courier-assignment; Firebase courier bearer; JSON {"courierId":"courier-uid"}; exactly 200, empty body | MATCHES_APPROVED_CONTRACT for the synchronous route and basic guards; recovery remains FEEDBACK-006 |
| FEEDBACK-003: core reset | POST /api/credits/orders/{orderId}/hold-for-reopen; Firebase courier bearer; no body; exactly 200, empty body | Route/reset exists; replay security remains INCOMPLETE_OR_INCOMPATIBLE in section 1 |
| FEEDBACK-002 Credit: refund | OpenOrderRefundTaskEvent v1; CANCELLED/EXPIRED and null courier; old order ID/requester/amount checked; release once | MATCHES_APPROVED_CONTRACT for current refund handling in source; separate legacy topic mismatch is FEEDBACK-007 |
| FEEDBACK-002 Credit: completion | OrderCompletionTaskEvent v1; COMPLETED, matching courier and overdue facts; debit requester reserved/total and credit courier once | MATCHES_APPROVED_CONTRACT for current completion handling in source |
| Existing reservation/read/balance/signup | PUT/GET reservation; GET /me; POST /registration-facts | Present; no duplicate route requested; terminal replay safety remains FEEDBACK-006 |

### Concrete implementation/test evidence

Paths below are relative to repository root; peer files remain read-only.

- `credit-service/src/main/java/sg/edu/nus/foc/credit/api/CreditController.java`:
  route method, role/self checks and bodyless 200; `CourierAssignmentRequest`
  validates a nonblank maximum-128-character courierId.
- `credit-service/src/main/java/sg/edu/nus/foc/credit/persistence/JpaCreditRepository.java`:
  row-locked RESERVED assignment, courier account existence, same-assignment
  replay/different-assignment conflict; reset retains reservation; transactional
  refund/settlement, event ID+payload hash deduplication, ledger/account updates.
- `credit-service/src/main/java/sg/edu/nus/foc/credit/messaging/CreditOrderEventConsumer.java`
  and `OrderEventMessage.java`: matching current refund/completion envelope and
  snapshot fields, subscription/type matching and completion-only overdue fact.
- `credit-service/src/main/java/sg/edu/nus/foc/credit/api/CreditOrderEventController.java`:
  POST /api/credits/internal/order-events decodes wrapped Base64 data and returns
  **204 NO BODY after processing/transaction commit**. This is the push ACK,
  not a refund/completion JSON response or callback to Order.
- `credit-service/src/main/java/sg/edu/nus/foc/credit/security/SecurityConfig.java`
  and `PubSubPushTokenValidator.java`: separate push chain checks Google-signed
  issuer/audience/configured service-account email/email_verified; business
  chain remains Firebase. `HttpUserServiceRoleProvider` forwards the user token
  and validates response userId. Local mock role configuration is not proof of
  live User role integration.
- Existing test source: `credit-service/src/test/java/sg/edu/nus/foc/credit/api/CreditApiIntegrationTest.java`,
  `persistence/JpaCreditRepositoryIntegrationTest.java`,
  `messaging/CreditOrderEventConsumerTest.java`,
  `api/CreditOrderEventControllerTest.java`, `security/SecurityComponentsTest.java`.
  Positive, negative and duplicate cases exist; legacy ABORTED consumer cases do
  NOT establish ADR-025 compliance. Missing replay cases are listed in section 1.
- Order comparison: `order-service/src/main/java/sg/edu/nus/foc/order/adapter/HttpPeerAdapters.java`,
  `order-service/src/main/java/sg/edu/nus/foc/order/application/OrderTransitionService.java`
  and Order's `messagingpublisher/dto/` and `messagingpublisher/mapper/` files.
  No Order HTTP adapter change
  is needed just to discover these implemented routes.

### Topic/subscription provisioning exists; deployed delivery remains unverified

| Current required financial stream | Dev topic -> Credit subscription | Production topic -> Credit subscription |
| --- | --- | --- |
| Refund | open-order-refund-dev-v1 -> credit-open-order-refund-dev-v1 | open-order-refund-prod-v1 -> credit-open-order-refund-prod-v1 |
| Completion | order-completion-dev-v1 -> credit-order-completion-dev-v1 | order-completion-prod-v1 -> credit-order-completion-prod-v1 |

Project: `protean-vigil-509704-q4`. Settings are in CreditPushProperties,
application.yaml, deploy/env.yaml and infra/environments/{staging,production}.env.
Push envelope received by the existing Credit endpoint:

~~~json
{
  "message": {
    "data": "BASE64_OF_FULL_ORDER_EVENT_JSON_FROM_SECTION_4",
    "messageId": "pubsub-message-id"
  },
  "subscription": "projects/protean-vigil-509704-q4/subscriptions/credit-open-order-refund-dev-v1"
}
~~~

The separate `infra/gcp/configure-credit-pubsub.sh staging` script creates/updates
authenticated push subscriptions to the deployed Credit URL plus
`/api/credits/internal/order-events`, matching OIDC audience/service identity,
retry settings, DLQ/recovery subscription and IAM. Bootstrap documents this
separate step; merely running bootstrap/Compose does not prove it was run.
The current script ALSO provisions the incompatible accepted-cancellation stream
listed in FEEDBACK-007; resolve that before using it for the current workflow.
Production is intentionally disabled by CREDIT_PUBSUB_PRODUCTION_ENABLED=false;
enabling/provisioning requires the platform owner's approval and staging checks.

Local Compose settings use an internal Docker hostname/local push identity;
they do not create a publicly reachable authenticated Cloud Pub/Sub push path
to a developer's machine. A configured subscription name is not a live subscriber.
No gcloud executable was found in this runner, and no live GCP/ledger/browser
test was performed; cloud subscription existence, IAM, actual ACK and balances
remain runtime gates, not missing Java consumer implementations.

**FEEDBACK-001 remains SUPERSEDED:** synchronous release/settlement endpoints
are not required by the effective event flow. Order's unused legacy settle
adapter method does not create a new provider requirement. No CreditsRefunded
callback/event or refund-confirmation gate is requested by this review.

## Owner handoff / closing criteria

1. Annablee addresses remaining reset replay protection (003), terminal replay/
   cross-service reconciliation (006) and legacy accepted-cancellation routing
   (007). Do NOT ask her to recreate the implemented assignment/reset routes or
   refund/completion handlers. Runtime test the compatible implemented contracts.
2. User owner agrees/implements abort and completion subscriptions separately.
3. User/Supplier/Credit/platform agree FEEDBACK-005 before background work resumes.
4. Peer completion reports are READY_FOR_VERIFICATION, not VERIFIED. Reinspect
   source/DTO/security/tests, run authenticated positive/negative/idempotency/
   concurrency/timeout/consumer tests against isolated data before closing.
5. Platform owner verifies compatible subscriptions/IAM/push identity/DLQ and
   authenticated refund/settlement balances after the 007 cutover. Production
   promotion remains a separate approved action.
6. Order stub/publisher/RTL tests and this source review do not prove live money
   movement, penalties, deployed subscriptions or Sprint completion. Peer source,
   infrastructure, application code and databases were NOT changed in this review.
