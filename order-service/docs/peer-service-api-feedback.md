# Peer Service API Feedback

## Effective compact-event amendment — CHANGE-092 / ADR-031 (2026-10-09)

Yao Xiang approved the exact seven-field Order-only payload: eventId, eventType, orderId, orderStatus, creditAmount, occurredAt, courierId. This replaces full snapshots and overdue facts ONLY for OpenOrderRefundTaskEvent and OrderCompletionTaskEvent. AcceptedOrderCancellationTaskEvent retains its existing v1 envelope/snapshot. Internal outbox versions and Pub/Sub eventVersion attribute remain (compact schema v2); topic names and DB schema unchanged. Old pending snapshot rows normalize at dispatch from their saved facts, with stable IDs. Lifecycle/outbox scheduling, locks, synchronous Credit assignment/reset and ADR-025 abort behavior remain. Historical v1 descriptions below are superseded for these two bodies. Credit currently requires the old snapshot and overdue; FEEDBACK-009 is INCOMPLETE_OR_INCOMPATIBLE. User completion penalties need an agreed separate overdue source. User approved implementing Order-only and documenting peer work; live integration remains blocked, Sprint [~].


CHANGE-088 / ADR-029 now implements an opt-in [local real broker connector](local-live-testing.md)
with isolated refund/completion push subscriptions and existing Google auth.
This resolves the absent local ingress configuration, NOT peer semantic gaps or
live financial verification. 60 config/safety and 16 nginx/fixture assertions pass;
gcloud/ADC/live tests outstanding. No missing endpoint request or feedback status
removed/upgraded on this basis. No Credit subscription to User penalty topic;
003/005/006/007 and User consumers remain as documented below.

Current handoff: **2026-10-09**, Vincent, `sprint-2-3-credit`, CHANGE-087.
Pre-CHANGE-092 evidence (refund/completion schema compatibility is now superseded by FEEDBACK-009).
Inspected integrated revision: `09e04a0` (`pull: credit service`).

Follow-up financial verification (2026-10-09, source unchanged): existing focused
Credit tests ran in Java 21 Docker from a read-only source copy, with isolated
Testcontainers PostgreSQL/credit_test (Flyway V1/V2). **27 tests passed, 0 failures,
0 errors, 0 skipped**, including 12 persistence integration tests. Refund for
CANCELLED/EXPIRED and completion transfer/deduplication are implemented and locally
tested at Credit's layers. This is NOT live Pub/Sub delivery, deployed IAM/OIDC,
full Order-to-Credit or coverage verification. Accepted-cancellation code/config
is intentionally unchanged at the user's request; 007 remains a separate gap.

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
| Credit/User | Compact refund/completion consumer migration and overdue data source | INCOMPLETE_OR_INCOMPATIBLE, FEEDBACK-009 |
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
- Current Order safeguards must not be omitted when explaining this risk:
  cancelAccepted locks and validates the current ACCEPTED order/version, waits
  for Credit's exact 200, then commits OPEN/EXPIRED and a command receipt. A
  failed call leaves it ACCEPTED, preventing new acceptance; a committed command
  replay returns before another Credit call. Thus a normal serialized abort/
  acceptance does NOT exhibit the simplified stale-clear example. Credit-side
  replay authorization and delayed/direct duplicate requests or a future retry
  mechanism remain separate hardening concerns, not an observed normal-flow bug.
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
version 1, unchanged envelope/snapshot in section 4.

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

OrderCompletionTaskEvent now carries only the seven-field COMPLETED body in section 4.
Identify courier via courierId. Apply User-owned on-time/late policy once by eventId
only after agreeing the authoritative separate overdue source (FEEDBACK-009);
this body has no overdue or deadline facts. ACK after durable effects commit.
Order does not prescribe a new numeric penalty rule.

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

Effective compact refund/completion JSON message data (schema v2):

~~~json
{
  "eventId": "stable-event-uuid",
  "eventType": "OrderCompletionTaskEvent",
  "orderId": "business-order-id",
  "orderStatus": "COMPLETED",
  "creditAmount": 5,
  "occurredAt": "2026-10-09T04:00:00Z",
  "courierId": "courier-firebase-uid"
}
~~~

Refund has the same seven keys, eventType OpenOrderRefundTaskEvent and orderStatus
CANCELLED or EXPIRED. courierId may be null and is still present. No requester's ID,
versions, snapshot/history, actor, overdue or deadline is sent. Credit looks up
its own transaction by orderId; User overdue-source agreement remains FEEDBACK-009.
Pub/Sub attributes retain eventId/eventType/eventVersion=2. See FEEDBACK-009 for
consumer incompatibility, rollout and required tests.

Accepted cancellation JSON message data remains version 1:

~~~json
{
  "eventId": "stable-event-uuid",
  "eventType": "AcceptedOrderCancellationTaskEvent",
  "eventVersion": 1,
  "orderId": "business-order-id",
  "orderVersion": 7,
  "occurredAt": "2026-10-09T04:00:00Z",
  "actorId": "courier-firebase-uid",
  "order": {
    "id": "business-order-id",
    "requesterId": "requester-firebase-uid",
    "courierId": null,
    "itemDescription": "Collect a parcel",
    "pickupSupplierId": "pickup-id",
    "deliverySupplierId": "delivery-id",
    "offeredCredits": 5,
    "status": "OPEN",
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
  }
}
~~~

Accepted cancellation retains the envelope/snapshot keys above, without overdue or
overdueAt. Resulting status is OPEN or EXPIRED, actorId is the aborting courier,
and snapshot courierId is cleared. Its User-only penalty subscription remains.
repostPlan and links may be null; no checkpoints, internal row UUID, attemptId,
generic facts, outcomeType or repostExpiresAt is included.

Order commits status and event intent atomically, attempts immediate after-commit
publication, then recovers pending/failed rows every 15 minutes for ALL three
events. Pub/Sub message-ID success means broker acceptance, NOT completed refund,
settlement or penalty. Consumer owners provision separate environment-matched
subscriptions, least-privilege subscriber IAM, durable deduplication, retry/
dead-letter monitoring and recovery. Pull ACK or authenticated push/function
handler is for owners to agree/provision; no callback endpoint is invented here.
Push wraps JSON as base64 message.data; decode before validating type/version,
the appropriate body schema and stable event ID. Snapshot ID/version checks apply
only to the unchanged accepted event. Duplicate delivery is expected.

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
Status: READY_FOR_VERIFICATION for live integration where compatible; NOT live
VERIFIED. Initial review inspected test source only; the subsequent focused
execution below verifies Credit-local behavior, not deployed subscriptions.

| Feedback / capability | Actual implemented contract | Source comparison |
| --- | --- | --- |
| FEEDBACK-004: courier assignment | PUT /api/credits/orders/{orderId}/courier-assignment; Firebase courier bearer; JSON {"courierId":"courier-uid"}; exactly 200, empty body | MATCHES_APPROVED_CONTRACT for the synchronous route and basic guards; recovery remains FEEDBACK-006 |
| FEEDBACK-003: core reset | POST /api/credits/orders/{orderId}/hold-for-reopen; Firebase courier bearer; no body; exactly 200, empty body | Route/reset exists; replay security remains INCOMPLETE_OR_INCOMPATIBLE in section 1 |
| FEEDBACK-002 Credit: refund | OpenOrderRefundTaskEvent v1; CANCELLED/EXPIRED and null courier; old order ID/requester/amount checked; release once | Historical v1 match; current compact v2 INCOMPLETE_OR_INCOMPATIBLE, FEEDBACK-009; accepted-topic gap remains 007 |
| FEEDBACK-002 Credit: completion | OrderCompletionTaskEvent v1; COMPLETED, matching courier and overdue facts; debit requester reserved/total and credit courier once | Historical v1 match; current compact v2 INCOMPLETE_OR_INCOMPATIBLE, FEEDBACK-009 |
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
  and `OrderEventMessage.java`: matching former v1 refund/completion envelope and
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
- Executed follow-up: ./mvnw -B -ntp clean test
  -Dtest=CreditOrderEventConsumerTest,CreditOrderEventControllerTest,CreditServiceTest,JpaCreditRepositoryIntegrationTest
  in Java 21 with isolated PostgreSQL and source mounted read-only. PASS:
  consumer 5, controller 3, service 7, persistence 12 (27 total, no failures/errors/
  skips). Tested persistence settlement moved requester 50 -> 40, reserved 10 ->
  0, courier 50 -> 60; replay did not transfer again. Expired refund released
  reserved funds and marked REFUNDED; CANCELLED service/consumer mapping is also
  covered. This split-layer run does not exercise cloud transport or live Order
  publishing. Some existing cases test the legacy accepted-cancellation handler;
  their success is NOT current 007 compliance. Maven test, not verify: no coverage
  gate or full Credit test-suite pass is claimed.
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
    "data": "BASE64_OF_ORDER_EVENT_JSON_FROM_SECTION_4",
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

## FEEDBACK-008: Pub/Sub push incorrectly inherits Firebase user-role conversion

Date: 2026-10-09. Owner: Credit (Annablee). Status: READY_FOR_VERIFICATION.
Classification: INCOMPLETE_OR_INCOMPATIBLE. Existing endpoint, not a missing API.
CHANGE-088 / ADR-029 live verification blocker; no new contract proposed.

- Expected: POST /api/credits/internal/order-events receives the wrapped envelope
  in section 6 and Authorization: Bearer <Google push service-account OIDC token>.
  Validate Google signature, expiry, issuer, configured audience, verified email
  and approved service account. This identity is NOT a Firebase user and must
  not be sent to /api/users/role-context. Process the financial event and return
  204 with no body only after durable effects commit; invalid identity stays 401.
- Observed: real local ingress POSTs return 500. Matching Credit errors at
  06:48:12Z, 06:48:26Z, 06:48:43Z and 06:49:01Z show
  FirebaseRoleAuthoritiesConverter -> HttpUserServiceRoleProvider -> User's
  /api/users/role-context -> 401 Invalid authentication token, before the
  Credit event controller can process the refund. User's rejection is correct.
- Source before CHANGE-090: credit-service/security/SecurityConfig.java (under its Java package),
  push chain line 68 supplies a dedicated decoder but no dedicated authentication
  converter. The global JwtAuthenticationConverter bean at lines 137-140 uses
  FirebaseRoleAuthoritiesConverter. Spring Security 7.1.1 automatically selects
  that bean when a chain does not explicitly provide a converter:
  https://github.com/spring-projects/spring-security/blob/7.1.1/config/src/main/java/org/springframework/security/config/annotation/web/configurers/oauth2/server/resource/OAuth2ResourceServerConfigurer.java
- Required provider correction: explicitly isolate push authentication conversion
  from Firebase user-role conversion, retaining the existing Google push decoder
  and all token/identity checks. Preserve Firebase role lookup for ordinary Credit
  user APIs. Do not enable mock roles, strip bearer tokens or permit anonymous push.
- Required regression: signed valid push identity reaches the handler with ZERO
  RoleProvider calls even when User role lookup is unavailable; wrong signature,
  issuer, expiry, audience, email or email_verified fails; user APIs still use
  Firebase roles/ownership guards. Then test live CANCELLED/EXPIRED refunds and
  COMPLETED transfers, plus duplicate delivery, with ledger/balance evidence.
- Initial evidence: logs and source inspection, before a fixed/tested implementation.
  Earlier 27 Credit outcome tests did not verify this live security-chain path.
  Initial Check's unauthenticated 401 and ingress 404/405 are expected negative
  probes; they cannot establish authenticated financial delivery. CANCELLED Order
  state and broker publication are not confirmation of refund completion.
- Initial stopping point: peer source remained unchanged while explicit separate
  Credit-edit authorization was pending clarification. Superseded by approval below.
  Preserve queued/DLQ events for recovery; do not reset databases or republish with
  new event IDs. After a fix, rebuild only Credit, retain the tunnel URL, and verify
  retry/DLQ recovery and exactly-once financial effects before marking VERIFIED.

### Approved narrow repair - CHANGE-090 (2026-10-09)

Vincent explicitly approved the Credit security fix and regression tests for this
one task, with an interference Markdown. Only SecurityConfig's push-chain converter
selection and new PubSubPushSecurityTest change. Existing Google decoder/validators,
Firebase user chain, business logic, schemas, consumers, endpoints and cloud
configuration remain unchanged. Annablee's ownership and all other peer boundaries
are retained. See changes/CHANGE-090-credit-push-security-interference.md.

Observed red: valid locally signed push fails through FirebaseRoleAuthoritiesConverter.
After the explicit independent converter, all 15 new bearer-filter cases and the
full 80-test Credit suite pass (no failures/errors/skips), including a fresh clean
source-only Java 21 run: 95.48% lines / 85.56% branches, existing 80% gates unchanged.
Source/test commit 198cd7c. A local JWKS replaces Google's public-key URL only for tests. Role lookup
and financial collaborators are mocked in the security slice, not live integrations.

Still required: owner review, rebuild the local Credit image, real Google push 204,
CANCELLED/EXPIRED refund and COMPLETED transfer balance/ledger evidence, duplicate
delivery and queued/DLQ recovery. Local tests do not close this live gate or prove
the user's existing cancelled reservation has been refunded. Status is deliberately
not VERIFIED. Background repost credentials/retries remain paused.

## 7. Current Order-to-Credit payload audit (2026-10-09)

Read-only audit requested by Vincent on sprint-2-3-credit. No application, event
schema, peer, database or infrastructure changes are approved by this audit.
Expected contracts: ADR-011 full completion/refund snapshots, ADR-018 minimal
assignment/reset requests, ADR-025 User-only accepted-cancellation penalties.
Evidence: Order HttpPeerAdapters/CreditServicePort and all production call sites;
event DTOs/factory/mapper/publishers; CreditController/request DTOs,
CreditOrderEventController/OrderEventMessage/CreditOrderEventConsumer,
CreditService/JpaCreditRepository; existing test source and local-live/cloud
provisioning configuration. Field-name-only runtime outbox SELECTs confirm the
persisted envelope/snapshot shapes for all three types, without exposing raw
descriptions, user IDs or credentials. This does not prove financial delivery.

### HTTP requests: active shapes match and are already small

All backend paths below start with `/api/credits`. Real foreground requests
forward the current Firebase bearer header; caller identity is not established
by an ID in JSON. Credit owns balances and existing reservation details.

| Operation/caller | Method/path | Exact request body | Provider result / assessment |
| --- | --- | --- | --- |
| Create and both repost paths | PUT `/orders/{orderId}/reservation` | `{"requesterId":"requester-uid","amount":5}` | 201 new/200 replay with ReservationResponse; both fields are used; MATCHES_APPROVED_CONTRACT for payload shape |
| Accept | PUT `/orders/{orderId}/courier-assignment` | `{"courierId":"courier-uid"}` | Exactly 200, no body; Credit checks authenticated self, active reservation and courier account; MATCHES_APPROVED_CONTRACT for payload shape |
| Every assigned courier abort | POST `/orders/{orderId}/hold-for-reopen` | NO body | Exactly 200, no body; Credit gets caller from JWT and stored reservation; retains funds, clears courier; MATCHES_APPROVED_CONTRACT for shape, existing FEEDBACK-003 security/replay gap retained |
| Frontend signup, not an Order backend call | POST `/registration-facts` | `{"eventId":"uuid","userId":"user-uid","occurredAt":"ISO-UTC"}` | 201 new/200 replay, AccountResponse; fields are used for self-check, provisioning and deduplication |
| Frontend balance polling | GET `/me` | NO body | 200 BalanceResponse, authenticated UID selects account; 404 for missing account |

Order ID is already in the path. No need to add description, pickup/delivery,
penalty points, balances, expected Order version or the full Order to these
requests. The adapter currently ignores reservation response content; active-
state replay/reconciliation remains FEEDBACK-006, not a reason to add outbound
fields or claim verified recovery. Assignment/reset enforce bodyless 200.

Dormant code: `CreditServicePort.settle`/HttpPeerAdapters.settle constructs
`POST /orders/{orderId}/settlement` with commandId/requesterId/courierId/amount.
Credit has no corresponding controller route and no Order production call site
invokes this method. Current completion uses its event instead. This is obsolete
adapter surface, NOT an active missing-provider requirement. An old adapter unit
test mocks the missing route; it does not verify that Credit implements it.
Removing that surface is a separate approved cleanup, not performed here.

### Historical v1 audit — superseded by CHANGE-092 compact bodies

Current envelope fields consumed for decoding/validation, ledger attribution or
duplicate-payload hashing: `eventId`, `eventType`, `eventVersion`, `orderId`,
`orderVersion`, `occurredAt`, `actorId`, and `order`. Current consumer explicitly
requires `order.id == orderId` and `order.version == orderVersion`; these repeated
fields are intentional consistency checks, not safe deletion candidates under v1.

Inside `order`, Credit reads exactly:

- `id` and `version`: envelope/snapshot consistency.
- `requesterId` and `offeredCredits`: must match Credit's stored reservation.
- `courierId`: must be null for refund, assigned/matching for completion.
- `status`: refund permits CANCELLED/EXPIRED; transfer requires COMPLETED.

Completion additionally carries `overdue` (required by the consumer) and
`overdueAt`. Credit includes them in its duplicate-payload hash, but settlement
transfers the reserved amount regardless of these facts. User Service owns
penalty policy and also needs completion facts; do not add penalty calculations
or a deduction amount to Order's Credit request on this basis.

The nine additional snapshot fields are NOT accessed by Credit's consumer or
passed to its CreditOutcomeEvent/financial repository:

`itemDescription`, `pickupSupplierId`, `deliverySupplierId`, `createdAt`,
`expiresAt`, `deliveryTimeLimitMinutes`, `originalOrderId`, `repostedOrderId`,
and `repostPlan` (including its nested scheduling/amount/duration/used data).

These fields satisfy the approved broader snapshot contract and Credit's incoming
DTO mirrors them, but they are unnecessary for its current refund/transfer work.
In particular, free-form request descriptions increase data exposure without
helping financial processing. Thus the events MATCH the approved v1 shape and
contain enough data, yet do NOT meet a strict Credit-only minimal-payload goal.
No new missing field was found for the current refund/completion consumer.

If Vincent chooses minimization, coordinate with Credit and User owners first:
decide shared minimal facts versus a separate financial contract, preserve
authentication/identity/version/idempotency, test serialization/deserialization
and both consumers, and agree old-v1 outbox/backlog/DLQ compatibility before
rollout. Do not mutate queued payloads or generate new event IDs for old refunds.
No new schema/version/topic or provider implementation is selected by this audit.

### Routing and return behavior

| Result | Published event / configured topic family | Intended consumer / required meaning |
| --- | --- | --- |
| Requester OPEN cancellation or unassigned OPEN expiry; expired abort after reset | OpenOrderRefundTaskEvent / `open-order-refund-*` | Credit releases the OLD order reservation exactly once; current snapshot CANCELLED/EXPIRED, courier null |
| Requester or >=48-hour scheduled completion | OrderCompletionTaskEvent / `order-completion-*` | Credit transfers reserved funds to the recorded courier; User separately processes completion facts |
| Every assigned courier abort | AcceptedOrderCancellationTaskEvent / `accepted-order-cancellation-*` | User penalties ONLY; current OPEN/EXPIRED, actorId is aborting courier; not a Credit refund command |

Google Pub/Sub supplies the authenticated wrapped push to
`POST /api/credits/internal/order-events`: message.data is Base64 event JSON,
plus subscription and optional messageId. Credit returns bodyless 204 after
processing, including an idempotent replay. Order's publisher instead receives a
Pub/Sub message ID; that is publication confirmation, not Credit completion.
Event data/attributes contain no Firebase token, password, roles or account
balances; HTTP/transport authentication remains separate from business payload.

Local-live helper correctly creates only Credit refund/completion subscriptions
and sets the accepted-cancellation subscription name to an unused placeholder.
Credit's older accepted-cancellation consumer/cloud provisioning still expects
ABORTED and refunds, so FEEDBACK-007 remains INCOMPLETE_OR_INCOMPATIBLE. Keep User
penalty routing separate; never adapt OPEN penalty events into Credit refunds.
Actual cloud subscription inventory and ledger effects were not verified here.

Verification limits: runtime field-name/publication-state SELECTs and static
producer/consumer/DTO/test-source comparisons performed; no tests rerun, financial
mutation, cloud write, message replay or source edit. Publication states observed
included a pending refund; PUBLISHED is not proof of a refund/transfer. Existing
003/005/006/007 entries and paused background-auth/retry scope remain unchanged.


### FEEDBACK-009 — Compact refund/completion consumers

- Status: OPEN; classification INCOMPLETE_OR_INCOMPATIBLE; approved Order-only schema milestone, not integration verification.
- Evidence: Credit OrderEventMessage and CreditOrderEventConsumer currently require nested order, envelope/order matching versions, and completion overdue. These will reject the new bodies. User completion consumer/overdue source remains unverified. Do not mark these flows done.
- Credit action: consume OpenOrderRefundTaskEvent and OrderCompletionTaskEvent using the schema below; lookup its existing transaction/requester by orderId, refund CANCELLED/EXPIRED, transfer COMPLETED to the supplied courierId, verify amount/assignment against Credit-owned state and deduplicate eventId. Do not recreate already implemented reservation/assignment/reset endpoints.
- User action: completion still requires the existing overdue penalty / on-time score deduction policy, but this message supplies no overdue/deadline data. Agree and verify an authoritative separate source before implementing that policy; occurredAt alone cannot determine overdue. Accepted cancellation keeps its existing payload/penalty behavior.
- Order behavior: writes intent atomically; publishes after commit with Pub/Sub attributes eventId/eventType/eventVersion (2 for these bodies). Topics retain existing dev-v1/prod-v1 names; their suffix does not imply the new body is v1. Coordinate consumer rollout before using current topics for live financial tests. No peer code or cloud changes made.
- Request: implement/verify compact parsing, null courier refund, terminal statuses, duplicate/replay/amount/courier checks and failures. Next verification re-read peer schemas/tests, then real refund/completion and duplicate-delivery integration.

Compact body (both event types, exactly seven keys):

```json
{
  "eventId": "stable-event-uuid",
  "eventType": "OpenOrderRefundTaskEvent",
  "orderId": "public-order-id",
  "orderStatus": "EXPIRED",
  "creditAmount": 10,
  "occurredAt": "2026-10-09T12:00:00Z",
  "courierId": null
}
```

Completion uses eventType OrderCompletionTaskEvent, orderStatus COMPLETED and its assigned courierId. Refund uses CANCELLED or EXPIRED; courierId is the saved resulting Order value, including null. creditAmount is the integer offeredCredits. No requester, snapshot, actor, history, versions or overdue fields appear in either body. AcceptedOrderCancellationTaskEvent contract is unchanged.
