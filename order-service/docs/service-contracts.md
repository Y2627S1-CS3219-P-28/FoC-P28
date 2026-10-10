# Service Contracts

## Effective foreground recovery exception — ADR-033 / CHANGE-100

CREATE/ACCEPT/CANCEL_ACCEPTED get a mock-only command API, committed intent,
immutable key/result and claim fencing. Actual Credit bodies are unchanged.
Historical operation-result and compensation are contract-stub assumptions in
`../concurrency/concurrency-order-credit.md`, NOT implemented/verified peer APIs.
HTTP recovery remains hard disabled.

## Effective compact-event amendment — CHANGE-092 / ADR-031 (2026-10-09)

Yao Xiang approved the exact seven-field Order-only payload: eventId, eventType, orderId, orderStatus, creditAmount, occurredAt, courierId. This replaces full snapshots and overdue facts ONLY for OpenOrderRefundTaskEvent and OrderCompletionTaskEvent. AcceptedOrderCancellationTaskEvent retains its existing v1 envelope/snapshot. Internal outbox versions and Pub/Sub eventVersion attribute remain (compact schema v2); topic names and DB schema unchanged. Old pending snapshot rows normalize at dispatch from their saved facts, with stable IDs. Lifecycle/outbox scheduling, locks, synchronous Credit assignment/reset and ADR-025 abort behavior remain. Historical v1 descriptions below are superseded for these two bodies. Credit currently requires the old snapshot and overdue; FEEDBACK-009 is INCOMPLETE_OR_INCOMPATIBLE. User completion penalties need an agreed separate overdue source. User approved implementing Order-only and documenting peer work; live integration remains blocked, Sprint [~].


## Effective validation amendment - CHANGE-091 / ADR-030

New automatic plans require repostDueAt >= original.expiresAt and
repostExpiresAt >= repostDueAt + 30 minutes. Manual submissions require expiresAt
>= submission + 30 minutes. Existing saved explicit-expiry automatic plans retain
their instructions, including shorter windows. Late automatic execution uses the
saved future expiry without demanding another 30 minutes from execution.
Creation/manual local validation failures use the existing details[field,message]
error envelope and identify only actual invalid fields. No peer API/event/schema
change. Latest user approval supersedes only the historical new-plan timing below.


## Credit Order-event push endpoints

Credit uses one authenticated Google Pub/Sub push endpoint per financial stream:
`/api/credits/internal/order-events/open-refund`,
`/api/credits/internal/order-events/accepted-cancellation`, and
`/api/credits/internal/order-events/completion`. Each route invokes its fixed
handler; the wrapped envelope's subscription and declared event type are checked
only as routing guardrails. Successful commits and idempotent replays return 204.

## Local delivery configuration — CHANGE-088 / ADR-029

The original shared endpoint has been replaced by the three typed paths above.
Wrapped Google push retains Google OIDC and subscription checks and returns 204
after processing. Isolated local financial subscriptions/topic names and a
stable custom audience now match Compose and Setup; HTTPS proxy preserves body
and bearer. See [runbook](local-live-testing.md). User penalty topic has no Credit
subscription here; User consumers/delegated authorization still require peers.
Synthetic routing tests do NOT verify actual push identities or ledger processing.

## Current provider evidence — CHANGE-087 (2026-10-09)

On sprint-2-3-credit, Credit's integrated 09e04a0 source implements the existing
PUT courier-assignment (courierId JSON, bodyless 200), POST hold-for-reopen
(no body, bodyless 200), and wrapped authenticated push refund/completion
(204 after durable processing) contracts. This is source evidence, not live
contract verification. No wire/schema change is approved by this review.
Remaining gaps: reset replay authorization (003), terminal replay/recovery (006),
delegated credentials (005), and incompatible ABORTED/refund handling of the
accepted-cancellation topic (007). ADR-025 remains authoritative: accepted
cancellation signals User penalties on OPEN/EXPIRED; only a separate refund event
releases an EXPIRED reservation. See peer feedback for requests/formats/evidence.
Prior dated missing-provider statements below are historical.

## Effective additive Order contract — CHANGE-086

`POST /api/orders` adds nullable `repostExpiresAt` (ISO UTC timestamp), REQUIRED
when automaticRepost=true. Require expiry > repostDueAt >= original expiresAt;
false ignores automatic-plan details. New expiry is never execution+duration.
Quarter-hour selection is a frontend rule, not a change to UTC wire format.
New automatic plans must provide the field; V4 disables legacy plans without it.

OrderResponse adds nullable repostExpiresAt, repostFailureCode,
repostFailureMessage and repostFailureAt to existing reads/commands/list items.
Only a safe latest eligible authorized attempt failure is persisted on the
original EXPIRED/unlinked order after rollback. A newer failure replaces it;
successful linkage clears it. Ownership/state/version/auth failures before an
eligible attempt cannot overwrite another user's message. Raw diagnostics and
tokens are never stored as outcomes. Failure persistence may increment the
original version: clients refetch it before another versioned manual attempt.

Manual repost request shape, all peer routes/topics/event JSON are unchanged.
No same-candidate durable job, retry worker, trusted credential or refund
confirmation gate is implemented. All background retry scope remains paused.
Earlier dated contract statements below remain historical evidence.


CHANGE-085: existing endpoint/DTO/topic shapes unchanged. Reservation 409 is
exposed as INSUFFICIENT_CREDITS ONLY when Credit confirms that semantic code;
other conflicts stay CONFLICT, 400/401/403/404 retain their classes and transport/
server failures become SERVICE_UNAVAILABLE. No raw peer diagnostic is exposed.
Frontend reads Order lists and /api/credits/me every 15 visible/auth-ready seconds
plus focus/mutation refresh. Manual repost failures do not produce a new OPEN
order; the short card message is client-local. Latest user instruction pauses
ALL background retry implementation pending peer agreement. No candidate task,
worker, security bypass, trusted credentials or new peer route implemented.
Current exact missing-provider handoff is peer-service-api-feedback.md.

CHANGE-084 / ADR-027 approves Order-side bounded same-ID retry and polling
principles, not new implemented endpoints/DTOs. Trusted service authorization
is a documentation-only proposal, explicitly deferred pending User/Supplier/
Credit owner agreement (FEEDBACK-005). Existing Firebase-user routes remain
unchanged. FEEDBACK-006 records reservation confirmation/reconciliation needs;
no new broker/refund-confirmation event is approved by this decision.

CHANGE-083 / ADR-026 replaces Order's two timer settings with ORDER_LIFECYCLE_CRON
(every minute) and sets outbox recovery to 15 minutes. Business endpoint/event
shapes remain unchanged in this repair. Explicit repost expiry/retry/status API
changes remain unimplemented; see the target diagram and FEEDBACK-005/006.
Supplier pair validation now requires explicit valid:true; false/missing rejects.

## Effective Sprint 2-3 amendments (CHANGE-082 / ADR-025)

This section supersedes historical abort/reopen descriptions below. Every ACCEPTED-only abort first calls bodyless `POST /api/credits/orders/{orderId}/hold-for-reopen`, requiring 200 after courierId reset and reservation retention. Current Order keeps its business ID and becomes OPEN/EXPIRED after a fresh deadline check; an immutable ABORTED courier attempt is stored separately. EVERY abort emits the accepted-cancellation event for User; only EXPIRED additionally emits the shared refund event for Credit. New refunds are not requested through the User penalty event. Peer feedback documents legacy reconciliation, missing APIs/subscribers, payload/status/authentication/retry requirements.

Courier `/mine` uses database-paginated current assignments plus ONLY that courier's immutable attempts. Existing response adds nullable `attemptId`; it is non-null for read-only ABORTED snapshots, null for current Orders. Both may share business `id`; clients key history by attemptId and never mutate a history snapshot. Requester `/mine` excludes EXPIRED originals only after successful repost linkage, before pagination/counting. Repost reserves a NEW ID, keeps old/new rows, and authenticated manual receipt replay validates requester/original linkage. No event schema/topic rename.

Acceptance still waits synchronously for bodyless 200 from `PUT /api/credits/orders/{orderId}/courier-assignment` with only courierId in JSON. Assignment/reset are Order-side approved stubs, not verified peer providers. No HTTP fallback to mocks. Automatic repost's current lifecycle credential is not a verified peer Firebase identity; FEEDBACK-005 remains open. Full NTH3 report/hold/admin contracts listed later are logical future scope, not implemented by CHANGE-082.

These are logical approved contracts. CHANGE-053/054 approve Order event snapshots and Google Cloud Pub/Sub; CHANGE-067/ADR-016 clarifies that snapshots omit checkpoint history. CHANGE-056 defines one `OrderCompletionTaskEvent` with `overdue` and `overdueAt` facts. CHANGE-063/ADR-013 approves atomic Order/outbox persistence, after-commit dispatch, and cron recovery with at-least-once delivery. CHANGE-064/ADR-014 adds a synchronous Credit hold before an unexpired accepted errand returns to `OPEN`; expired accepted cancellation remains event-driven. CHANGE-065/ADR-015 added Spring-scheduled OPEN expiry; CHANGE-071/ADR-019 supersedes only its separate event type so requester cancellation and scheduled expiry both emit `OpenOrderRefundTaskEvent` on one topic, distinguished by the resulting Order status.

CHANGE-073/ADR-021 configures real Pub/Sub with one GCP project and environment-specific topics. Local topic IDs are `order-completion-dev-v1`, `open-order-refund-dev-v1`, and `accepted-order-cancellation-dev-v1`; production topic IDs are deployment configuration. Grant publisher access at topic scope only. Local Compose uses each developer's ADC; Cloud Run uses its service identity.

CHANGE-068/ADR-017 adds the Order-side courier-assignment contract stub: Credit must confirm the reservation courier synchronously before Order persists acceptance. CHANGE-069/ADR-018 keeps Order versions local and makes assignment/hold requests minimal, while published events retain `orderVersion`. FEEDBACK-003's bodyless hold-by-Order-ID contract is agreed but not implemented by Credit; FEEDBACK-004's assignment contract and route remain open.

## Order Service consumes

### User Service

- `verifyIdentity(accessContext)`: authenticate protected order actions.
- Requester-only completion and `OPEN` cancellation use a User Service-confirmed requester identity that must match `Order.requesterId`.
- Courier-only acceptance, progress, and accepted-order cancellation use a User Service-confirmed courier identity. Acceptance requires no assigned courier; progress and accepted cancellation require a match with `Order.courierId`.
- `getRoleContext(userId)`: return requester/courier/admin role context.
- `getCourierEligibility(userId)`: return whether a courier may accept new errands.
- `getUserSummary(userId)`: minimal display identity for authorized views.
- User consumes `AcceptedOrderCancellationTaskEvent` for EVERY courier abort (current snapshot OPEN/EXPIRED): apply the configured penalty to actorId, not cleared courierId. User does not subscribe to requester OPEN-cancellation refunds.
- User consumes `OrderCompletionTaskEvent` for every completion: use `overdue` and `overdueAt` to apply the configured penalty when late or decrease the score when on time. Penalty/score policy remains User-owned.
- These are event subscriptions, not blocking Order-to-User requests. Payloads carry `eventId`, `eventVersion`, top-level `orderId`/`orderVersion`, the current Order/repost snapshot without checkpoint history, `actorId`, and `occurredAt`; consumers deduplicate at-least-once delivery. Completion carries overdue facts derived internally from checkpoints.

### Supplier Service

- `validateSupplierPair(pickupId, deliveryId)`: both IDs exist, both are active for creation, and they are distinct.
- `resolveSupplierDetails(supplierIds)`: current names, categories, and campus locations, including inactive suppliers for historical orders.

### Credit Service

- `reserveCredits(orderId, requesterId, amount)`: reserve before an original or repost becomes `OPEN`.
- `assignCourier(orderId, courierId)`: proposed synchronous operation during courier acceptance. `orderId` is in the path and `courierId` is the only request-body field. Order waits for `200 OK` before persisting `ACCEPTED`; Credit validates transaction details from its own record. See FEEDBACK-004.
- `holdForReopen(orderId)`: synchronously clear courierId while retaining funds on EVERY abort, including expired outcomes; no request body. Order waits for `200 OK` before saving OPEN/EXPIRED/history/events; failure leaves ACCEPTED. See FEEDBACK-003/CHANGE-082.
- Order checks its version locally before these calls; it does not send that version to Credit. Published Order events retain `orderVersion`.
- Credit consumes `OpenOrderRefundTaskEvent`: refund/release the reserved amount for either requester cancellation of an `OPEN` order (`order.status=CANCELLED`) or scheduled expiry of an unassigned `OPEN` order (`order.status=EXPIRED`). User is not involved. This event replaces Order's former synchronous Credit release call and the formerly separate expiration event.
- Credit consumes `OpenOrderRefundTaskEvent` also for expired courier aborts. Legacy ABORTED accepted-cancellation payloads require coordinated refund reconciliation; NEW accepted-cancellation events are User penalty facts, not Credit refund instructions.
- Credit consumes every `OrderCompletionTaskEvent`: transfer/settle the reserved credits to the courier.
- Same-order current ACCEPTED resolves directly OPEN/EXPIRED while ABORTED survives in immutable courier history (ADR-025 supersedes the prior prohibition). The hold endpoint is missing from the inspected Credit API; see FEEDBACK-003.
- `getReservationStatus(orderId)`: recovery/idempotency query after uncertain reservation responses.

## Order Service provides

- Admin all-orders query (CHANGE-057): `GET /api/orders`, optional `status`, one-based `page`/`size` pagination, `OrderPageResponse`; admin role required. Omitted status returns every status. This supports NTH1/Admin Service but does not implement its dashboard.

- Commands: `createOrder`, `acceptOrder`, `startTask`, `markPickedUp`, `markDelivered`, `confirmCompletion`, `cancelOpen`, `cancelAccepted`.
- Queries: `getOrder`, `listOrders`, `listAvailableOrders`, `getCheckpointHistory`, `getOrderFacts`.
- Reposting: creation-time automatic plan, `getManualRepostDraft`, and
  `requestManualRepost`. The legacy `configureReposting` route is retained for
  compatibility but rejects post-creation mutation.
- Lifecycle: `processExpiry`, `processAutoRepost`, and `autoCompleteDue`. Spring scheduler selects `DELIVERED` orders with a delivered checkpoint at or before `now - 48 hours`; the transition rechecks under lock and commits the ordinary completion checkpoint/receipt and `OrderCompletionTaskEvent` outbox row atomically.
- Admin integration: `placeCompletionHold`, `releaseCompletionHold`, `applyAdministrativeResolution`.
- Facts: `subscribeToOrderOutcomes`, including typed completion, shared OPEN-refund, and accepted cancellation event contracts.

Only approved active-workstream contracts are implementable. See `sprints/sprint-2-3/README.md` for the current slice and `sprints/sprint-1/contracts.md` for historical scope.

## Common guarantees

- Authenticated actor context or trusted service identity.
- Command ID for idempotent create, reserve, transition, expiry, and repost operations; globally unique event ID and event version for each published event.
- Expected order version for state changes and explicit conflict on stale commands.
- Stable order, user, and supplier identifiers; never exchange database entities.
- Distinct validation, unauthorized, not found, conflict, dependency unavailable, reservation rejection, and accepted-for-processing failures.
- Event payloads contain event ID/version, order ID/version, actor IDs, event-specific facts, and occurred-at time without unnecessary private data.
- Completion/refund/abort commit state/checkpoints/history where applicable, receipt and event intent together; dispatch follows commit and recovery retries due outbox rows. Every abort waits synchronously for Credit reset, then emits User penalty, plus Credit refund only for EXPIRED. Pub/Sub subscribers are asynchronous; Order does not wait for their replies. Delivery is at least once and requires durable consumer deduplication. Provider/consumer guarantees are not verified by this Order-only milestone.


## UI time selection and background cadence

CHANGE-077/ADR-022 keeps Requester UI clock minutes 00/15/30/45 and upward
rounding without a backend quarter-hour constraint. ADR-026 supersedes timer
cadence: one minute job for due unassigned OPEN expiry and >=48h latest-delivery
completion; all-event outbox recovery every 15 minutes; immediate publication
retained. These are defaults, not runtime SLAs, especially with idle Cloud Run CPU.


## CHANGE-079 / ADR-024: Central role annotations (approved 2026-10-08)

Production Firebase validation resolves User Service roles once per request, verifies response identity against JWT subject, and enforces RequireRequesterRole/RequireCourierRole/RequireAdminRole. Order-specific production role mode defaults to HTTP; local anonymous/mock behavior remains. Adapters reuse verified roles/identity, retaining fresh courier eligibility and locked domain ownership/state guards before mutation. Shared reads accept any confirmed requester/courier/admin role; /mine uses its selected mode. Internal lifecycle/scheduler authorization, API bodies, event payloads and schema remain unchanged. See ADR-024 for endpoint policy, inspected peer contracts and positive/negative test obligations.

CHANGE-093 changes only internal lifecycle processing boundaries: due-ID query and per-order transaction/lock/recheck, caught failures and successful-transition counts. Existing internal expiry-trigger API path/body/authentication remains unchanged; its expired count now excludes skipped/failed candidates. No foreign API or event body change.

## Personal Order status filter — CHANGE-094 / ADR-032

`GET /api/orders/mine?mode=requester|courier&userId=<verified-uid>&page=1&size=20&status=COMPLETED`

- status is an optional uppercase OrderStatus query parameter: OPEN, ACCEPTED, IN_PROGRESS, PICKED_UP, DELIVERED, COMPLETED, CANCELLED, ABORTED, EXPIRED. Omitted/empty means all; invalid enum or mode returns 400 VALIDATION_ERROR. No request body or new endpoint.
- Same production Firebase/role and selected-mode/identity verification; status cannot broaden ownership. Existing local behavior retained.
- Response remains `{items,page,size,totalItems,totalPages}`; one-based page, default 20/max 100. Filtered counts and content share the same DB constraints.
- Requester status applies to current owned Orders and retains hiding linked EXPIRED originals. Courier status applies to current assignments plus that courier's immutable ABORTED attempts only when status=ABORTED; all includes both. Business id and nullable attemptId/history behavior unchanged.
- POST /{id}/cancel-accepted unchanged. Abort errand is presentation wording; state/history/credit/outbox behavior unchanged.
- Frontend Order refresh5 seconds while visible/auth-ready; same selected status/page on periodic/manual/focus/mutation reads. Credit polling remains15 seconds. No financial mutation is triggered by polling.

CHANGE-095 UI refinement: personal dropdown choices are mode-specific (requester excludes ABORTED; courier excludes OPEN/EXPIRED/CANCELLED). This does not alter /api/orders/mine accepted enum, page shape, ownership/auth or server query contract.

## Scheduler DB selection and independent outbox dispatch — CHANGE-096 (2026-10-10)

The active lifecycle paths retain CHANGE-093: DB-filtered IDs, separate REQUIRES_NEW expiry/completion workers, fresh NOWAIT locks and outside-proxy catch. Outbox recovery now selects bounded eligible event IDs in SQL (due PENDING or expired IN_PROGRESS lease), then individually claims/rechecks with SKIP LOCKED. Claim, markPublished and scheduleRetry use separate REQUIRES_NEW transactions; enqueue remains REQUIRED with Order/checkpoint/receipt. Dispatcher catches each event's claim/commit/retry-write errors, logs its ID and continues later events. A failed retry write leaves the committed lease recoverable after expiry. Neither scheduler nor batch coordinator is transactional. Pub/Sub publication stays outside DB transactions and is irreversible; stable-ID deduplication remains required. Legacy claimDue is retained for compatibility, unused by the active scheduler. No cadence, schema, event body, peer, frontend or paused repost change.

Internal repository addition only: findDueIds(now, limit) returns eligible event IDs in deterministic created_at/event_id order, bounded in SQL; each individual claim revalidates eligibility under lock. No HTTP or subscriber payload change. FEEDBACK-009 remains unresolved.

## Swagger current-origin server — CHANGE-098 (2026-10-10)

CHANGE-098 sets an explicit relative OpenAPI server / through OpenApiConfiguration. Swagger now resolves API calls against the origin serving the specification, retaining /api/orders paths and Firebase authorization. Live staging docs previously advertised an HTTP backend host despite HTTPS gateway UI. This is an Order-only documentation routing implementation detail; no gateway, peer, CORS allowlist, forwarded-header trust, schema, frontend or deployment-env change. Tests first: one expected server-URL failure and one existing documentation pass (target/change098-red.log). Fresh source-only wrapper-selected Maven3.9.16/Java21 offline verify:253 tests,0 failures/errors/skips, including28 PostgreSQL tests; coverage96.00% lines (1441/1501),83.72% branches (468/559), unchanged >=80% gates pass. Generated target/openapi.json servers=[{url:"/",description:"Current gateway or service origin"}];160 current POM/source/test/resource files equal the fresh tested copy. Logs/reports target/change098-verify.log and target/change098-source-check/target/, evidence target/change098-evidence.json. git diff --check passes. No local application container rebuild or real authenticated browser request; no commit/push/cloud deployment. Staging remains unchanged until Order Service redeployment.

## Backend creation expiry guard — CHANGE-099 (2026-10-10)

Existing contract, newly verified at the HTTP boundary: POST /api/orders requires
expiresAt >= server validation time + 30 minutes. OrderCreationService supplies
Instant.now(); Order.open enforces the minimum before Supplier validation/Credit
reservation/persistence. Earlier values produce HTTP 400 VALIDATION_ERROR with
an expiresAt detail. Client-supplied timestamps cannot replace the server clock.
Wire schema and role checks remain unchanged; exact domain boundary is inclusive.
Direct API regression uses mocked providers; it is not a live integration claim.
