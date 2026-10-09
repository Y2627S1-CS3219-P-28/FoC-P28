# Peer Service API Feedback

This document is the Order Service integration handoff for User, Supplier, and Credit Service owners. It lists only peer capabilities that still need implementation/agreement or an existing integration that needs adjustment; completed endpoint contracts are intentionally omitted. The remaining contracts do not authorize changes to another service.

## Current integration status

**2026-10-09 decision update:** CHANGE-084 / ADR-027 approves documenting a
trusted background-caller proposal for peer discussion; its implementation is
explicitly deferred. User approval is NOT provider agreement. FEEDBACK-005/006
remain OPEN. Same-candidate-ID bounded temporary retries and short terminal
EXPIRED-card messages are approved Order-side designs, not implemented code.
See the detailed follow-up at the end of this file to hand to peer owners.

| Peer service     | Integration                                                                       | Current status                                                 | Peer action                                                                                                              |
| ---------------- | --------------------------------------------------------------------------------- | -------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| Supplier Service | Validate pickup/delivery supplier pair | Existing provider inspected; Order handling repaired in CHANGE-083 | Order rejects valid:false and missing confirmation; stub tests are not live verification. |
| Credit Service   | Completion and shared refund consumers (including expired abort)                 | Not found in inspected peer source                             | Implement FEEDBACK-002; coordinate legacy accepted-cancellation refund replay.                                           |
| User Service     | Completion and EVERY courier-abort penalty consumer                               | Not found in inspected peer source                             | Implement the subscriptions and actions in FEEDBACK-002.                                                                 |
| Credit Service   | Synchronous hold/reset on EVERY accepted-order abort                               | Contract agreed; no matching endpoint found                    | Credit must implement the agreed FEEDBACK-003 endpoint before HTTP-peer mode supports this flow.                         |
| Credit Service   | Record the assigned courier on an order's active reservation during acceptance    | No matching endpoint found                                     | Agree and implement the synchronous operation in FEEDBACK-004; Order must wait for success before persisting `ACCEPTED`. |

Historical inspection is based on peer controllers/DTOs/services/repositories/tests as of 2026-10-06. On 2026-10-08 the assignment/reset/outcome-consumer absence was rechecked in local peer source without editing those directories. No live peer call was verified; recheck actual implementation/tests before setting `VERIFIED`.

## Existing endpoint requiring an Order-side adjustment

Supplier Service has implemented pickup/delivery pair validation. CHANGE-083 repairs Order's adapter: valid:false rejects even HTTP 200; a missing body/valid field fails closed. The old adapter discarded this body. Supplier source is unchanged; an authenticated live test remains required.

### Supplier Service: validate the pickup/delivery pair

- **Operation:** `POST /api/suppliers/validate`
- **Request JSON:**

  ```json
  {
    "pickupSupplierId": "supplier-id-1",
    "deliverySupplierId": "supplier-id-2"
  }
  ```

- **Success (`200`), valid example:**

  ```json
  {
    "valid": true,
    "problems": []
  }
  ```

- **Success (`200`), invalid-pair example:**

  ```json
  {
    "valid": false,
    "problems": [
      {
        "field": "pickupSupplierId",
        "supplierId": "supplier-id-1",
        "reason": "INACTIVE"
      }
    ]
  }
  ```

- **Semantics:** validation rejects missing suppliers, inactive suppliers, and the same supplier used for pickup and delivery. `reason` is `NOT_FOUND`, `INACTIVE`, or `SAME_SUPPLIER`; `field` identifies the input field.
- **Integration note:** the current Order HTTP adapter treats only an HTTP error as rejection and discards this response body. Because invalid pairs are represented as `200` with `valid: false`, the Order-side adapter must inspect `valid` before this integration is safe in HTTP-peer mode. This is an Order-side follow-up; Supplier Service already returns the documented result.
- **Peer implementation:** `SupplierController.validate`, `ValidatePairRequest`, and `PairValidation`.

## Event contract shared by subscribers

Order Service publishes these typed events to Google Cloud Pub/Sub through its transactional outbox. The serialized JSON uses the Java DTO field names below (camelCase). Each event has `eventVersion: 1`. Per CHANGE-067/ADR-016, this v1 contract excludes checkpoint history. No peer consumers were found when this change was approved; any discovered consumer of the earlier checkpoint-bearing shape must be coordinated before rollout. Development topics are `order-completion-dev-v1`, `open-order-refund-dev-v1`, and `accepted-order-cancellation-dev-v1` in project `protean-vigil-509704-q4`. Production uses separate topic IDs, configured for Cloud Run when provisioned. Subscriber owners should create environment-matched subscriptions and coordinate their production topic IDs with the Order Service owner. See CHANGE-073/ADR-021 for topic-level IAM and local ADC setup.

### Common event envelope

Every event contains:

| Field          | JSON type               | Meaning                                                                                                                  |
| -------------- | ----------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| `eventId`      | string                  | Stable unique event ID; unchanged when the outbox retries delivery. Use this as the consumer deduplication key.          |
| `eventType`    | string                  | One of the exact typed event names below.                                                                                |
| `eventVersion` | integer                 | Schema version; currently `1`.                                                                                           |
| `orderId`      | string                  | Order identifier.                                                                                                        |
| `orderVersion` | integer                 | Version of the resulting Order represented in `order`.                                                                   |
| `occurredAt`   | ISO-8601 instant string | Time the outcome was recorded.                                                                                           |
| `actorId`      | string                  | User who initiated the outcome; for scheduled expiration the value is `"lifecycle"`.                                     |
| `order`        | object                  | Resulting Order snapshot with its current aggregate fields and repost plan; checkpoint history is intentionally omitted. |

The `order` object contains:

| Field                      | JSON type               | Meaning                                                                                |
| -------------------------- | ----------------------- | -------------------------------------------------------------------------------------- |
| `id`                       | string                  | Order identifier (same value as top-level `orderId`).                                  |
| `requesterId`              | string                  | Requester's user ID.                                                                   |
| `courierId`                | string or null          | Assigned courier in the resulting state; accepted-cancellation events have it cleared. |
| `itemDescription`          | string                  | Errand description.                                                                    |
| `pickupSupplierId`         | string                  | Pickup supplier identifier.                                                            |
| `deliverySupplierId`       | string                  | Delivery supplier identifier.                                                          |
| `offeredCredits`           | integer                 | Credit amount offered/reserved for the errand.                                         |
| `status`                   | string                  | Resulting `OrderStatus`, e.g. `COMPLETED`, `CANCELLED`, `EXPIRED`, or `ABORTED`.       |
| `createdAt`                | ISO-8601 instant string | Order creation time.                                                                   |
| `expiresAt`                | ISO-8601 instant string | Order application/expiry deadline.                                                     |
| `deliveryTimeLimitMinutes` | integer                 | Allowed delivery duration.                                                             |
| `version`                  | integer                 | Resulting Order version (same value as top-level `orderVersion`).                      |
| `originalOrderId`          | string or null          | Source order for a repost.                                                             |
| `repostedOrderId`          | string or null          | Linked repost order, if created.                                                       |
| `repostPlan`               | object or null          | `enabled`, `dueAt`, `creditAmount`, `deliveryDurationMinutes`, and `used`.             |

Checkpoint history is not included. It remains stored and queryable from Order Service; consumers should not expect to reconstruct it from an event. Completion overdue facts are computed from the internal accepted/delivered checkpoints and are included separately on the completion event.

Example common shape (event-specific fields are shown below):

```json
{
  "eventId": "stable-event-id",
  "eventType": "OpenOrderRefundTaskEvent",
  "eventVersion": 1,
  "orderId": "order-id",
  "orderVersion": 3,
  "occurredAt": "2026-10-04T04:00:00Z",
  "actorId": "requester-user-id",
  "order": {
    "id": "order-id",
    "requesterId": "requester-user-id",
    "courierId": null,
    "itemDescription": "Collect a parcel",
    "pickupSupplierId": "supplier-id-1",
    "deliverySupplierId": "supplier-id-2",
    "offeredCredits": 25,
    "status": "CANCELLED",
    "createdAt": "2026-10-04T03:00:00Z",
    "expiresAt": "2026-10-04T05:00:00Z",
    "deliveryTimeLimitMinutes": 60,
    "version": 3,
    "originalOrderId": null,
    "repostedOrderId": null,
    "repostPlan": null
  }
}
```

`OrderCompletionTaskEvent` additionally contains:

| Field       | JSON type                       | Meaning                                                                |
| ----------- | ------------------------------- | ---------------------------------------------------------------------- |
| `overdue`   | boolean                         | Whether completion was overdue under the Order's completion-time rule. |
| `overdueAt` | ISO-8601 instant string or null | Deadline instant used to evaluate the overdue fact.                    |

Consumers must tolerate redelivery, deduplicate durably by `eventId`, and acknowledge only after their own action commits. Order delivery is at least once; subscriber retry, dead-letter, replay, and monitoring behavior must be agreed by the subscriber owner.

## Sprint 2-3 effective contract update (CHANGE-082, 2026-10-08)

Vincent approved the Order-side implementation and local-only stubs. This does not approve or implement a peer's service. Current local source inspection still finds only registration/read/reservation Credit routes, no courier-assignment or hold/reset routes, and no outcome subscribers in Credit/User. User's HTTP outcome routes exist but are not the approved Pub/Sub consumer and are not substituted silently.

### What Annablee / Credit Service must provide

1. **Acceptance:** `PUT /api/credits/orders/{orderId}/courier-assignment`, JSON `{"courierId":"authenticated-courier-uid"}`. Return bodyless **200** only after confirming that courier has an eligible Credit account and recording its ID on the active reservation. Do not change the amount or transfer funds. Same assignment is idempotent; a conflicting courier is rejected. Missing account/reservation must fail, not implicitly create an account. Order stays OPEN on failure.
2. **Every abort, including an expired abort:** `POST /api/credits/orders/{orderId}/hold-for-reopen`, **no body**, bodyless **200** after setting `courierId=null` and retaining reserved funds. This existing route name is retained even when Order subsequently becomes EXPIRED. The request uses the old business ID. Idempotent reset succeeds; refunded/settled/conflicting transactions fail. Order remains ACCEPTED on failure and does not save abort history or queue either outcome. A timeout/commit failure needs safe reconciliation: Credit might have reset while Order rolled back.
3. **Refund subscriber:** consume `OpenOrderRefundTaskEvent` for resulting `CANCELLED` or `EXPIRED`; release the reservation for `orderId` to `order.requesterId` once. Abort after expiry uses this same event, after the synchronous reset. A repost is a NEW reservation under a NEW ID: a late old-ID refund must never refund the repost's reservation.
4. **Settlement subscriber:** consume `OrderCompletionTaskEvent`, validate recorded courier against `order.courierId`, and atomically release the requester's held funds / transfer the offered amount under Credit's existing policy. Handle manual and 48-hour automatic completion identically. Credit owns ledger/balance math; Order publishes facts only.

Assignment/reset must authenticate and authorize trusted Order calls. The current adapter forwards a Firebase bearer token identifying the courier; requester-only reservation authorization is unsuitable for these two operations. Protect reset from a stale retry by validating the verified aborting courier against the CURRENT reservation assignment; never clear a different newly assigned courier. If the credential cannot safely prove that actor, the peer owners must agree trusted service identity plus actor binding before production use. Do not infer authorization from an order ID or an untrusted body alone.

Expected failures: 401 unauthenticated, 403 unauthorized, 404 missing reservation/account, 409 incompatible state or conflicting assignment; use a JSON error with `status`, `error`, `message`, `path`, `timestamp` (exact provider error codes to be agreed). Order requires exactly 200 success and fails closed on any other status/timeout. No body is needed for success. HTTP mode NEVER falls back to the in-memory mock.

### What User Service must provide

Subscribe to `AcceptedOrderCancellationTaskEvent` for **every** courier abort, whether its current Order snapshot is OPEN or EXPIRED. Apply the penalty once to **actorId**, the aborting courier; `order.courierId` is now null and MUST NOT identify the penalized user. This event is User-only for new aborts; do not refund Credit from it. Subscribe to completion facts for the existing overdue/on-time policy, identifying the courier from `order.courierId`. User owns penalty amounts/suspension policy.

### Event delivery / payload / replies

Keep the existing three configured topic IDs and typed event schemas. Payload is the common full Order snapshot already documented above: `eventId`, `eventType`, `eventVersion`, `orderId`, `orderVersion`, `occurredAt`, `actorId`, `order`; completion additionally carries `overdue` and `overdueAt`. Abort history/internal row UUID/attemptId are NOT event fields. Example expired-abort facts:

```json
{
  "eventType": "OpenOrderRefundTaskEvent",
  "eventVersion": 1,
  "orderId": "old-business-order-id",
  "actorId": "aborting-courier-uid",
  "order": { "id": "old-business-order-id", "requesterId": "requester-uid", "courierId": null, "status": "EXPIRED" }
}
```

This is a shortened illustration, not a replacement for the complete required schema. The separately queued User fact has `eventType: AcceptedOrderCancellationTaskEvent` and the same current Order snapshot/actor but a distinct stable eventId. Both intents commit atomically with Order/history/checkpoints/receipt; publication happens after commit. No synchronous business response is expected from a subscriber: acknowledge only after its own deduplicated transaction commits. For a push subscription this means the agreed successful HTTP acknowledgement; pull subscribers ACK the message. A publish acknowledgement does not prove a refund/penalty completed. Subscriber owners must provide durable eventId deduplication, retries, dead-letter/replay, monitoring and reconciliation.

**Legacy compatibility:** older queued AcceptedOrderCancellationTaskEvent payloads with ABORTED snapshots predate CHANGE-082 and originally requested a Credit refund as well. Preserve those durable payloads; coordinate legacy replay/refund reconciliation before enabling new consumers. Do not apply both a legacy refund and a new refund twice. Missing subscribers remain FEEDBACK-002 OPEN; reset remains Order-side AGREED/provider missing; assignment remains FEEDBACK-004 OPEN.

### FEEDBACK-005: Trusted automated repost credentials (OPEN)

Automatic repost uses existing Supplier validation and Credit reservation endpoints, but the configured lifecycle token is not a Firebase user ID token. Peers currently authorize user-bound calls. Before real scheduled integration, agree how trusted Order service identity proves requester identity and is allowed to validate suppliers/reserve NEW-order credits without a browser session. Existing request bodies stay as documented; no new peer endpoint, anonymous bypass or production mock is implemented. Verify this credential, audience, IAM/service authorization, timeout and retry behavior with the peer owners. Local mock testing alone cannot close this item.

## FEEDBACK-001: Historical synchronous outcome endpoints (superseded)

- **Status:** `SUPERSEDED` by FEEDBACK-002 for completion/cancellation/expiry outcomes.
- **History:** Earlier drafts proposed synchronous Credit settlement/release calls. The approved design now sends typed events for these consequences. Do not implement those old settlement/release HTTP proposals for these flows.
- **Still current:** the agreed Credit hold-before-reopen contract is FEEDBACK-003; its provider endpoint remains missing.

## FEEDBACK-002: Credit and User Service event subscriptions

- **Status:** `OPEN` — peer consumers were not found in the inspected repositories.
- **Responsible services:** Credit Service and User Service.
- **Scope:** typed outcome event subscriptions described here. This is future peer work; Order Service only owns the producer and event contract.
- **Delivery:** Google Cloud Pub/Sub; at least once, stable `eventId`, `eventVersion: 1`, resulting Order snapshot. Subscriber implementation must deduplicate and own its retry/acknowledgment/dead-letter/recovery behavior.

### Required subscriptions and actions

| Event type                           | Subscriber          | Action after consuming                                                                                                                                                                                                                   |
| ------------------------------------ | ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `OpenOrderRefundTaskEvent`           | Credit Service only | Refund/release the old-ID reservation. Resulting CANCELLED means requester cancellation; EXPIRED covers scheduler expiry OR expired courier abort after synchronous reset. No User refund subscription. |
| `AcceptedOrderCancellationTaskEvent` | Credit Service      | Legacy ABORTED payloads only: agree reconciliation as described in CHANGE-082 above. New abort refunds use OpenOrderRefundTaskEvent, never this event. |
| `AcceptedOrderCancellationTaskEvent` | User Service        | Apply the configured penalty once to actorId on EVERY abort; the current Order snapshot is OPEN or EXPIRED, while immutable ABORTED history stays internal. |
| `OrderCompletionTaskEvent`           | Credit Service      | Settle/transfer the reserved credits to `order.courierId`, verifying it matches the courier recorded on the reservation during acceptance. Process every completion event once at the business level.                                    |
| `OrderCompletionTaskEvent`           | User Service        | For every completion, apply the existing overdue penalty when `overdue` is true; otherwise apply the established on-time penalty-score reduction. The courier is `order.courierId`; `actorId` is the requester who confirmed completion. |

Unexpired accepted cancellation emits the User penalty event but NO Credit refund event. Every abort waits for Credit's synchronous reset confirmation. Expired abort additionally queues OpenOrderRefundTaskEvent. CHANGE-082 supersedes the historical no-event rule.

### Current peer inspection and next action

- No Pub/Sub outcome consumer was found in the inspected Credit source.
- Credit's `CreditReservation` and `ReservationResponse` include a nullable `courierId`, but `CreditController` only creates and reads reservations; no API/service operation assigns or updates the courier. The reservation operation initializes `courierId` to `null`.
- No event consumer was found in the inspected User source. Existing User HTTP routes are not the approved event contract and are not called by the Order event flow.
- **Next action:** Credit and User owners implement the subscriptions/actions in the table, agree topic/subscription configuration and trusted publisher validation, and define operational retry/dead-letter/replay/monitoring. For `OpenOrderRefundTaskEvent`, subscribe to the single shared OPEN-refund topic and use the resulting status to distinguish cancellation from expiry. Order Service must inspect the actual consumer code/tests before marking this feedback `VERIFIED`.

## FEEDBACK-003: Credit Service hold/reset on every accepted-order abort

- **Status:** `AGREED` — Order's synchronous hold-for-reopen behavior and minimal endpoint contract are finalized by the user; Credit has not implemented the endpoint.
- **Responsible service:** Credit Service.
- **Affected flow:** EVERY assigned-courier abort from ACCEPTED; overall Sequence 7; CHANGE-082 / ADR-025 supersedes CHANGE-064's before-expiry-only reset rule.

### Required behavior

On EVERY assigned-courier abort, Order synchronously waits for Credit to clear the previous courier and retain reserved funds. After success and a fresh expiry check, the current same-ID Order becomes OPEN or EXPIRED. Both outcomes queue the User penalty fact; EXPIRED also queues the Credit refund fact. If Credit rejects or is unavailable, Order remains ACCEPTED. CHANGE-082 supersedes the former expired-branch skip.

### Agreed endpoint contract

This is the approved Order-side contract for every accepted-order abort, whether the resulting current order is OPEN or EXPIRED. It is not a claim that Credit has agreed/implemented its provider or production authentication/reconciliation design.

- **Operation:** `POST /api/credits/orders/{orderId}/hold-for-reopen`
- **Request body:** none. Credit locates the transaction from `orderId` in the path.

- **Success:** bodyless synchronous `200 OK` after courierId=null and retained reservation. Order waits before saving OPEN/EXPIRED, history and outbox.
- **Required semantics:** locate the transaction by `orderId`; do not refund/release credits and do not transfer them. Retain the transaction and clear its current courier assignment. Make the operation idempotent by transaction state: a repeat when the same order is already held/reset with no courier assignment succeeds; missing transactions and conflicting states fail.
- **Minimal request contract:** the path Order ID is the only transaction input. Order validates its version and courier ownership before the call; Credit validates transaction state and identity from its own data. Event payloads continue to carry `orderVersion` for event identity and context.
- **Failure behavior:** any non-`200 OK` response means Order keeps the order `ACCEPTED` with its courier assignment. Credit should use an error response for an unauthenticated/forbidden caller, missing transaction, or conflicting transaction state.
- **Production authentication:** Order-to-Credit requires trusted service authentication. The current adapter forwards an `Authorization` value; that credential must be confirmed as suitable or replaced before live HTTP-peer use.
- **Recovery:** Credit may confirm the hold before Order's later database commit fails. Repeating the hold for the same already-held transaction must succeed so Order can retry/reconcile without refunding or stranding the transaction.
- **Related flow:** if the deadline passes in flight, Order rechecks expiresAt and follows EXPIRED: AcceptedOrderCancellationTaskEvent for User penalties and OpenOrderRefundTaskEvent for Credit refund.

### Implementation status

Order's `CreditServicePort`, mock adapter, bodyless HTTP request, and tests follow this agreed contract. The inspected Credit source has no matching route. Credit Service still needs to implement the endpoint, and Order must verify its actual implementation and tests before calling the integration verified.

## FEEDBACK-004: Credit Service courier assignment during Order acceptance

- **Status:** `OPEN` — required Credit operation is missing from the inspected controller, service, repository API, and contract tests.
- **Requesting service:** Order Service.
- **Responsible service:** Credit Service.
- **Affected flow:** courier accepts an unassigned `OPEN` Order.
- **User-approved behavior:** Order must synchronously ask Credit to associate the accepted courier ID with the existing reservation. Order waits for Credit's successful confirmation before persisting the Order as `ACCEPTED`; a rejection or unavailable Credit service leaves it `OPEN`.
- **Classification:** `MISSING`.

### Existing Credit implementation

`PUT /api/credits/orders/{orderId}/reservation` creates an idempotent reservation for a requester and amount. The request contains `requesterId` and `amount`; the controller requires the authenticated caller to match `requesterId`; a new `CreditReservation` is created with `courierId: null`. `GET /api/credits/orders/{orderId}/reservation` reads the reservation for its requester. Although the model and response contain a nullable `courierId`, no route or service/repository method sets it. This API cannot perform the requested acceptance-time update as implemented.

Order's `OrderAssignmentService.accept` verifies the courier, locks and validates the `OPEN` Order, synchronously calls `CreditServicePort.assignCourier` with Order ID and courier ID, rechecks the expiry boundary, and only then changes status and persists the checkpoint/Order/receipt. The local mock and proposed HTTP adapter implement this port on the Order side.

### To be discussed: proposed operation and format

The path and minimal request format below are proposals for Credit-owner agreement, not an existing Credit endpoint.

- **Proposed operation:** `PUT /api/credits/orders/{orderId}/courier-assignment`
- **Proposed request JSON:**

  ```json
  {
    "courierId": "authenticated-courier-user-id"
  }
  ```

- **Proposed success:** synchronous `200 OK` after the courier assignment is recorded. No response body is required. Order proceeds with `OPEN -> ACCEPTED` only after this successful response.
- **Required semantics:** locate the transaction by `orderId`; record the supplied courier without moving funds; reject refunded/paid or otherwise inactive transactions and a conflicting existing courier assignment. Repeating the same courier for the same order succeeds; a different courier conflicts. A reservation reset by FEEDBACK-003 is eligible for a new courier.
- **Minimal request contract:** `orderId` is in the path and `courierId` is the only body field. Order validates its version and acceptance rules locally; Credit validates transaction state from its own data. Published event payloads retain `orderVersion`.
- **Completion consistency:** when Credit consumes `OrderCompletionTaskEvent`, verify that `order.courierId` matches the courier associated with this reservation before transferring credits. Reject or quarantine mismatches for reconciliation; never pay a different courier silently.
- **Order failure behavior:** on timeout, rejection, or invalid response, Order must not persist `ACCEPTED`, its acceptance checkpoint, or command receipt. The Order status remains `OPEN`.
- **Authorization to agree:** the current Credit API authenticates a user and requires the authenticated identity to match the requester. For courier acceptance, the bearer identifies the courier, not the requester. Credit and Order owners must agree trusted Order-to-Credit service authentication or another safe authorization contract; do not assume the existing requester-only rule is suitable.
- **Consistency/idempotency to agree:** Credit may record the courier before the local Order transaction commits. Repeating the same courier assignment by Order ID must succeed; a different courier must conflict. The endpoint must define safe retry/reconciliation behavior if Credit succeeds but the Order database commit fails, without requiring a command ID in the request.
- **Related flow:** coordinate with FEEDBACK-003 so a successful hold/reset before reopening clears or releases the previous Credit-side courier assignment, allowing the next accepted courier to be recorded.

### Order-side contract-stub milestone

The user approved an Order-side mock/contract-stub milestone while the Credit endpoint is being completed. `MockPeerAdapters.assignCourier` models an active reservation, treats a repeat assignment to the same courier as idempotent, and rejects a conflicting courier. The hold operation locates the reservation by Order ID, clears the courier without refunding, and treats a repeat hold as idempotent. `HttpPeerAdapters` sends only `courierId` for assignment and sends no hold body; both require synchronous `200 OK`. Acceptance tests prove Credit confirmation precedes any Order mutation and that a Credit failure leaves the Order `OPEN` with no checkpoint, persistence, or receipt.

This is **not** a Credit endpoint implementation and does not verify a live peer integration. FEEDBACK-004 remains `OPEN`; Credit must agree/implement the route, trusted service authentication, state-based idempotency, and recovery semantics. The local base Compose configuration selects the mock adapter; `compose.http-peers.yaml` will fail on this operation until Credit provides the route.

## Feedback status rules

### 2026-10-08: Updated diagram/provider reconciliation

Local controller/service/DTO/security/repository source was re-inspected, not live peer APIs. User role-context/courier-eligibility and Supplier validation already exist. Credit PUT/GET reservation exists; assignment/reset routes and Credit/User PubSub outcome consumers were not found. FEEDBACK-002/003/004 remain open/provider missing. Mock mode alone models missing providers; HTTP mode never silently falls back.

| Local/staging topic (production suffix prod-v1) | Event / consumer | Required consequence |
| --- | --- | --- |
| open-order-refund-dev-v1 | OpenOrderRefundTaskEvent / Credit | Release old-ID reservation for CANCELLED or EXPIRED, including expired abort. |
| accepted-order-cancellation-dev-v1 | AcceptedOrderCancellationTaskEvent / User | Penalize aborting actorId once for EVERY abort; current snapshot OPEN or EXPIRED. |
| order-completion-dev-v1 | OrderCompletionTaskEvent / Credit and User | Credit settles to snapshot courier; User applies overdue/on-time consequences. |

All bodies: eventId, eventType, eventVersion:1, orderId, orderVersion, occurredAt, actorId, order (full current snapshot documented above). Completion additionally has overdue, overdueAt. No checkpoint/internal row/attempt IDs. All intents attempt publication immediately after commit; ADR-026 recovers pending/failed/expired leases every 15 minutes. Expected publish response is PubSub messageId; consumer must deduplicate, commit its business transaction, then ACK. No synchronous business response returns to Order; a publish ACK is not refund confirmation. EXPIRED refund is already present in Order code.

### FEEDBACK-006: Reservation confirmation and safe repost recovery (OPEN)

- **Owner:** Annablee / Credit; Order owns response handling and any approved retry persistence.
- **Existing request:** PUT /api/credits/orders/{newOrderId}/reservation, Authorization: Bearer <Firebase ID token>, JSON `{"requesterId":"requester-uid","amount":12}`. NewOrderId is NEW for a repost, not the expired original ID. Existing provider requires caller UID == requesterId.
- **Existing success:** 201 new / 200 replay; JSON `{"orderId":"new-order-id","requesterId":"requester-uid","courierId":null,"amount":12,"status":"RESERVED","balance":{"userId":"requester-uid","totalBalance":50,"reservedBalance":12,"usableBalance":38,"version":1,"asOf":"ISO-8601"},"createdAt":"ISO-8601","updatedAt":"ISO-8601","refundedAt":null,"paidAt":null}`. Timestamp strings here are format placeholders; use real ISO-8601 instants. Field names were checked against the actual ReservationResponse/BalanceResponse DTOs.
- **Existing recovery read:** GET /api/credits/orders/{newOrderId}/reservation, same bearer, no body; 200 returns reservation/status/timestamps (balance may be absent); 404 RESERVATION_NOT_FOUND indicates absent at that observation, not proof an in-flight write cannot later commit.
- **Required confirmation:** Order must validate 200/201, matching orderId/requesterId/amount and active RESERVED state. Empty/202/malformed/mismatched/REFUNDED/PAID responses must not create OPEN.
- **Provider gap:** current FirestoreCreditRepository.reserve replays an existing same-ID/requester/amount reservation without rejecting terminal status. Please agree terminal replay behavior (recommended 409 RESERVATION_CONFLICT), transactional idempotency and unknown-outcome reconciliation. Never overwrite refunded/paid reservations or allocate a different ID just to retry an unknown outcome.
- **Insufficient funds:** existing 409 envelope has `error:"INSUFFICIENT_CREDITS"`, status/message/path/timestamp/details. Only this semantic code should produce the small insufficient-credit card message, never every 409 or message-substring matching. It does not tell Order whether an old refund is pending.
- **Other errors:** 400 VALIDATION_ERROR; 401 UNAUTHENTICATED; 403 FORBIDDEN; 404 ACCOUNT_NOT_FOUND/RESERVATION_NOT_FOUND; 409 RESERVATION_CONFLICT; 503 SERVICE_UNAVAILABLE; 500 INTERNAL_ERROR. Agree transient timeout/connection-loss/429/5xx recovery versus permanent failures, and safe compensation for a reservation committed after expiry/local failure.
- **User-approved design, NOT implemented (CHANGE-084 / ADR-027):** durable retry request with one fixed candidate UUID saved before external reservation; commandId bound to requester/original/payload; retry temporary failures until success or configured new expiry, stopping invalid details, authorization rejection and other permanent failures. Isolate attempts and reconcile SAME candidate ID; preserve independent old-ID refund/outbox. Trusted credentials are documentation-only and need FEEDBACK-005 agreement; detailed task/schema/API design is still required.
- **Current limitation:** original remains EXPIRED/unlinked on reservation failure, but Order does not yet persist failures/retries, validate reservation bodies, classify Credit semantic errors or display persistent repost failure messages. This inspection is not live verification.

### FEEDBACK-005 follow-up: queued/automatic retry authentication

The internal repost endpoint passes a lifecycle secret as a bearer; it is not a Firebase user token. User-bound Supplier/Credit APIs cannot safely be retried indefinitely after logout/restart with that credential.

Approved by Vincent for owner discussion/documentation ONLY on 2026-10-09
(CHANGE-084 / ADR-027), NOT implemented or peer-approved: trusted Order service
identity with restricted audience/delegated requester authorization. Keep
documented business body fields; agree issuer/audience, permitted actions,
requester delegation, audit, refresh/rotation/storage, timeouts and denial
semantics. Do not add an anonymous/secret bypass, retain user bearers indefinitely,
or mock-fallback in HTTP mode. User explicitly says do not implement credentials
yet. No refund-confirmation event is introduced.

### FEEDBACK-005: Peer discussion handoff - 2026-10-09

- **Responsible owners:** User Service owner, Supplier Service owner, Annablee
  (Credit Service). Order owns saved user instructions, job identity and lifecycle.
- **Approval boundary:** user approves the proposal, not a chosen authentication
  protocol/endpoint/IAM change. Status remains OPEN. Do not implement in Order or
  peers until owners agree the detailed security/authorization contract and the
  user resumes implementation. Nothing below is a deployed endpoint.
- **Why:** automatic repost runs without a browser. Manual repost may start with
  a valid Firebase bearer, but later saved retries run after logout/token expiry.
  Neither a candidate UUID nor requesterId proves the caller's identity/rights.
- **Classification:** existing user-bound APIs are INCOMPLETE_OR_INCOMPATIBLE for
  unattended requester delegation; the required trusted delegation is MISSING.
  It may be an extension or dedicated internal route; exact route is undecided.

| Provider / existing operation | Existing business request and response | Required peer discussion/change |
| --- | --- | --- |
| User `GET /api/users/role-context` | No body; Firebase bearer determines UID. 200 JSON `{"userId":"requester-uid","roles":["requester"]}` (example role list). | A trusted Order caller needs role/eligibility verification for a specified delegated requester, NOT its service-account UID. Agree a restricted route/input for requesterId; return matching userId/current roles, deny disallowed requester/action. Existing foreground semantics must remain. |
| Supplier `POST /api/suppliers/validate` | JSON `{"pickupSupplierId":"pickup-id","deliverySupplierId":"delivery-id"}`; 200 `{"valid":true,"problems":[]}` or valid:false/problems with field/supplierId/reason. | Permit the agreed trusted Order identity for pair validation only; validate active/distinct IDs. No catalogue mutation permission. valid:false is a permanent details failure; preserve existing user-authenticated routes. |
| Credit `PUT /api/credits/orders/{candidateId}/reservation` | JSON `{"requesterId":"requester-uid","amount":12}`; 201 new or 200 same reservation, response fields in FEEDBACK-006. Current Firebase caller must equal requesterId. | Explicitly authorize restricted Order-service reservation on behalf of the saved requester; keep current requester-only foreground rule. Same candidate/requester/amount must not reserve twice; active RESERVED confirmation required. |
| Credit `GET /api/credits/orders/{candidateId}/reservation` | No body; 200 reservation/status or 404 RESERVATION_NOT_FOUND. Current access is requester-only. | Allow trusted Order reconciliation of its delegated candidate/requester, not unrestricted reading of everyone's ledger; identify how delegated requester is supplied/bound. |

The User example shows fields, not permission to hard-code a requester role.
User Service checks current Mongo profile/roles; a service credential must never
be passed to Firebase as though its UID were the human requester. Order's existing
automatic path does not currently perform this fresh User verification.

**Security contract owners need to settle before implementation:**

1. Service identity proof/verification, issuer/audience/signature and renewal;
   which identity may call which routes; local-vs-cloud trust setup and revocation.
   Cloud Run service identity is an option, not an approved deployment mechanism
   here. IAM ingress validation alone does not implement delegated application
   authorization in today's Firebase-only controllers.
2. How requester delegation is supplied and limited to authorized saved repost
   instructions, current requester rights, allowed amount/candidate and actions.
   Do not trust an unsigned verified=true/role flag. User consent is saved in
   Order; peers must agree how their authorization trusts that responsibility.
3. Positive 200/201 response bodies and deterministic errors: invalid credential
   -> 401; disallowed service/delegated requester/action -> 403; invalid fields
   -> 400; insufficient funds -> existing semantic 409 INSUFFICIENT_CREDITS;
   transaction conflict -> distinct code; transient unavailability -> 503.
   Proposed new error behavior requires owner agreement, not fabricated support.
4. Timeouts/rate limits/retry guidance, immutable payload binding/idempotency,
   terminal reservation replay and late-success/rollback compensation (006).
   Credentials can be renewed normally, but permanent authorization rejection
   must not be retried forever; agree authentication-vs-denial classification.
5. Audit correlation for task/command/candidate/original/requester/service caller;
   never log bearer tokens. Least privilege, no stored user refresh tokens or
   shared service-account key files; verify positive and negative contract tests.

**Order stopping point:** no credential provider, delegation bypass or background
worker implemented in this turn. HTTP does not fall back to mocks. Peer owners
should report the agreed route/header/input/response/error/security details, then
Order will inspect implementation/tests and verify actual integration before
changing OPEN to VERIFIED. Missing assignment/reset/subscribers (002-004) remain.

### FEEDBACK-006: Approved retry and EXPIRED-message follow-up - 2026-10-09

Vincent approved reuse of one saved NEW candidate ID for temporary retries until
the NEW expiry, stopping permanent details/authorization failures. Every attempt
uses the same PUT/GET reservation identity and unchanged requester/amount. A new
ID per timeout is prohibited; old-order refunds remain independent and intact.
Credit must agree terminal replay/reconciliation/late-success safety above;
approval of Order's retry principle does not verify Credit's idempotency contract.

Confirmed semantic INSUFFICIENT_CREDITS stops the task with original EXPIRED and
a small message. Do not claim old refund is pending: delayed refund and genuine
insufficiency are indistinguishable from the existing error. Other permanent
errors also get short appropriate messages. Temporary errors are retryable only
when classified as such; a 409 conflict/404 missing account/unknown response is
not automatically a temporary shortage or safe success. Future resubmission and
late remote reservation compensation still need explicit detailed design.

## Feedback status meanings

- `OPEN`: required peer capability is missing or agreement/implementation is pending.
- `AGREED`: Order-side behavior and contract are finalized, but the required peer implementation is missing.
- `IN_PROGRESS`: peer owner reports implementation has started.
- `READY_FOR_VERIFICATION`: peer owner reports implementation is ready; Order has not verified it.
- `VERIFIED`: Order re-read the actual peer code/tests and checked request, response, errors, authorization, semantics, and sequence behavior against the agreed contract.
- `SUPERSEDED`: a later approved design replaces the entry; retain the history and link the replacement.

A proposed contract, mock, HTTP adapter, design document, or peer report alone is not implementation verification. Do not change a status to `VERIFIED` without inspecting the actual peer implementation and tests.
