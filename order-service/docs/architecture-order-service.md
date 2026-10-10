# Order Service Architecture

## Order personal filters and five-second polling — CHANGE-094 / ADR-032 (2026-10-09)

Yao Xiang explicitly requests five-second Order UI polling, Abort errand wording, and status filters on My Errands/My Requests. The existing /api/orders/mine adds optional status; default all, invalid status 400, existing identity/mode checks and page envelope retained. Database filters before page/count, preserving ABORTED courier attempts and hidden successfully reposted requester originals. Existing Base UI filters/pagination reset page 1 and cancel stale reads. Order-only polling is5 seconds; Credit/generic default 15 seconds; auth/visibility/no-overlap/focus/mutation protections retained. No scheduler, event, peer, schema or background-retry change. Verification and limits: CHANGE-094. Historical Order interval descriptions are superseded only by this approved amendment.

## Lifecycle per-order failure isolation — CHANGE-093 (2026-10-09)

Yao Xiang explicitly requests failed scheduled tasks be skipped while later successes continue. Due selection returns IDs filtered in the DB (latest delivery cutoff for completion), without locking a whole batch. Nontransactional lifecycle coordinator calls fresh NOWAIT-locking per-order transactions (expiry worker / existing autoComplete with REQUIRES_NEW), catches each RuntimeException including commit failures, logs order ID, counts only successful transitions, then continues. Scheduler retains independent whole-pass catches. Failed orders remain eligible next normal lifecycle pass; no new repost retry mechanism or cron/contract/schema/peer change. Previous batch transaction description is superseded by this refinement; CHANGE-092 compact payloads and FEEDBACK-009 remain unchanged.


## Effective compact-event amendment — CHANGE-092 / ADR-031 (2026-10-09)

Yao Xiang approved the exact seven-field Order-only payload: eventId, eventType, orderId, orderStatus, creditAmount, occurredAt, courierId. This replaces full snapshots and overdue facts ONLY for OpenOrderRefundTaskEvent and OrderCompletionTaskEvent. AcceptedOrderCancellationTaskEvent retains its existing v1 envelope/snapshot. Internal outbox versions and Pub/Sub eventVersion attribute remain (compact schema v2); topic names and DB schema unchanged. Old pending snapshot rows normalize at dispatch from their saved facts, with stable IDs. Lifecycle/outbox scheduling, locks, synchronous Credit assignment/reset and ADR-025 abort behavior remain. Historical v1 descriptions below are superseded for these two bodies. Credit currently requires the old snapshot and overdue; FEEDBACK-009 is INCOMPLETE_OR_INCOMPATIBLE. User completion penalties need an agreed separate overdue source. User approved implementing Order-only and documenting peer work; live integration remains blocked, Sprint [~].


## Effective validation refinement - CHANGE-091 / ADR-030

Order.open/createManualRepost own local field/minimum validation. New RepostPlan
constructor requires expiry >= scheduled due+30min; due>=original expiry remains.
JPA hydration and late automatic execution preserve saved explicit plans. Manual
submission requires expiry>=submission+30min, without applying that minimum to
automatic execution. Existing ErrorResponse details identify actual fields; UI
maps them inline, backend remains authoritative. Creation verifies requester,
validates local fields, then Supplier/Credit, before persistence. No responsibility,
peer/topology/event/schema/credential/retry change; dated timing below is history.


CHANGE-088 / ADR-029 adds an opt-in [local live test topology](local-live-testing.md)
only: real HTTP peers, existing authenticated Order profile, isolated real Pub/Sub,
restricted HTTPS push into local Credit. No class/domain/state/migration changes.
Actual cloud/ledger verification remains pending; paused retry/delegation retained.

## Effective CHANGE-086 refinement

Explicit automatic new expiry and latest safe repost failures are Order-owned
fields, not a retry task or an additional aggregate. RepostPlan validates
expiry > due >= original expiry; execution uses that saved future deadline.
OrderRepostService invokes Supplier/Credit synchronously and rolls back failed
business attempts. AFTER_ROLLBACK invokes RepostFailureRecorder through its
Spring proxy: a new locked transaction saves only code/message/time on the
still-EXPIRED unlinked original. Domain rules prevent older callbacks overwriting
newer outcomes; success clears the outcome. No domain-to-peer calls introduced.
V4 disables legacy plans with no explicit deadline, by Vincent's decision.
HTTP auth remains unchanged; background retries and credentials remain paused.
The earlier unimplemented descriptions below are historical; CHANGE-086 and
the effective diagram own current expiry/outcome status.


## Effective current workstream

CHANGE-084 / ADR-027: approved durable same-ID temporary retries until new expiry,
terminal failure EXPIRED-card messages and authenticated HTTP polling. These are
target responsibilities, not implemented worker/schema/API/UI. Trusted peer
authorization is proposal/documentation ONLY; implementation explicitly deferred.
No new source classes, persistence schema, endpoint or security mechanism added.

CHANGE-083 / ADR-026 supersedes historical timer cadence/settings: one shared
minute lifecycle job and 15-minute recovery with immediate dispatch retained.
Approved target and current gaps: docs/diagrams/order-lifecycle-reconciliation.md.
Explicit automatic expiry and durable repost retry/UI state are not implemented.

CHANGE-082 / ADR-025 implements the user-selected overall PDF with approved lifecycle overrides. Editable class/sequence/data responsibilities are in `sprints/sprint-2-3/README.md`; the historical Updated PDF named below is absent locally. Order now has an internal UUID primary key plus a unique business ID and immutable courier-attempt snapshots. No peer service writes Order storage, and domain classes still do not invoke external ports.

Authoritative sources: `../../../Order Service Overall Doc - Updated.pdf`, `../../../High Level Architecture Diagram - Order Service.png`, and `../../../Class Diagram - Order Service.png`. The updated overall design informed Sequences 5-7 and typed event contracts; CHANGE-056 supersedes its separate overdue completion flow. Standalone diagrams remain separately fingerprinted artifacts.

## Layers and ports

1. External callers: requester, courier, Admin Service, and trusted lifecycle triggers.
2. Inbound contracts: commands, queries, admin integration, expiry, auto-completion, and auto-repost triggers.
3. Application components: creation, assignment, transitions, queries/history, completion-time overdue evaluation, scheduled auto-completion, expiry, and reposting.
4. Order-owned domain rules: lifecycle/status, authorization, checkpoints/history, one-time overdue evaluation, and repost policy.
5. Outbound ports: Order persistence/outbox, User/Supplier/Credit and typed outcome publishers. Acceptance waits for synchronous Credit assignment before ACCEPTED (provider missing). The shared OrderLifecycleScheduler discovers OPEN expiry and >=48h delivery completion, using existing lifecycle/transition services to queue the respective refund/completion intents.
6. Messaging infrastructure: transactional outbox, immediate after-commit relay, 15-minute recovery and typed Google PubSub publishers. Same project/separate dev/prod topics; personal read-only ADC locally, attached Cloud Run identity in production, topic-scoped IAM. The shared lifecycle job triggers expiry/completion; peer subscriptions are future work and are not awaited by Order.

Order Service owns only Order data: status, assignment, checkpoints, flags, supplier references, and repost links.

## Approved persistence and deployment

By ADR-008, Order Service persists in PostgreSQL on one Cloud SQL instance in `asia-southeast1`,
with separate `order_staging` and `order_production` databases. Cloud Run is the deployment target;
the runtime connects through the public-IP Cloud SQL Java Connector. Database passwords remain in
Secret Manager. This is an Order Service-specific approved deviation from the parent repository's
Firestore convention; it does not change sibling-service persistence.

## Control and data flow

- Inbound contracts dispatch the selected command, query, or lifecycle use case to an application component.
- Application components ask Order-owned domain rules to validate and decide. Domain rules return decision, status, flag, and checkpoint data to the application layer.
- Application components invoke `OrderRepository`, User/Supplier/Credit service ports, and typed event publishers. Domain rules do not call repository, broker, or external-service ports directly.
- Completion, open cancellation, and expired accepted cancellation commit the resulting Order state, checkpoint, command receipt, and typed event in one PostgreSQL transaction. An `AFTER_COMMIT` listener immediately attempts Pub/Sub publication; a Spring cron poller retries due outbox entries. Publish failures do not roll back the committed transition. Delivery is at least once; stable event IDs let consumers deduplicate. Completion always publishes `OrderCompletionTaskEvent`, which carries `overdue` and `overdueAt` for User/Credit policy.
- Order Service does not wait for Credit or User subscriber responses. Consumer delivery, deduplication, retries, and dead-letter recovery belong to future peer work and are not verified by this Order-only change.

This direction keeps domain rules independent of persistence and peer-service integration while application components own orchestration.

## Overall class responsibilities

- `OrderCommandInterface` and `OrderCommandFacade` route creation, acceptance, transitions, cancellation, and repost commands.
- `OrderQueryInterface`/`OrderQueryService` provide available, current, account, and checkpoint/history views as their sprint scope permits.
- CHANGE-057 adds an admin-only all-orders query through `AdminOrderController` -> `OrderQueryService` -> `OrderRepository` -> persistence adapter; the controller never accesses JPA directly.
- `LifecycleTrigger`/`LifecycleProcessingService` handle expiry, auto-completion, and automatic repost processing when in scope.
- ADR-026 uses OrderLifecycleScheduler -> LifecycleProcessingService -> OrderRepository due-delivery selection -> OrderTransitionService.autoComplete; the transition shares normal completion/checkpoint/overdue/receipt/outbox behavior. The other pass expires due OPEN orders and queues refunds.
- Specialized application services handle creation, assignment, transitions, administration, and reposting.
- `OrderRepository` is the only Order persistence abstraction.
- `UserServicePort`, `SupplierServicePort`, and `CreditServicePort` isolate synchronous external calls. Credit reservation and unexpired accepted-cancellation `holdForReopen` remain synchronous; Order waits for hold confirmation before reopening. Completion, cancellation, and OPEN-refund consequences otherwise use typed events.
- Credit `assignCourier` is also synchronous: `OrderAssignmentService` validates before the call, waits for the proposed matching reservation confirmation, then rechecks expiry before acceptance. The Order mock and HTTP adapter do not verify that Credit provides the route.
- `OrderTransitionService` and `LifecycleProcessingService` create the shared `OpenOrderRefundTaskEvent` through `OrderTaskEventFactory` for requester-cancelled and scheduler-expired OPEN orders; `OrderOutboxDispatcher` routes it through one refund publisher (and retains a compatibility route for already-persisted legacy outbox rows).
- The shared minute lifecycle job also selects DELIVERED orders with delivery checkpoints at least 48 hours old using PostgreSQL locks. The transition rechecks latest-delivery eligibility under lock and reuses the requester completion event/outbox path.
- `OrderTransitionService` records the post-transition event through `OrderEventOutboxRepository` in the same transaction as the Order, checkpoint, and command receipt. `OrderOutboxAfterCommitListener` requests immediate delivery before the transactional service call returns; `OrderOutboxScheduler` invokes the recovery dispatcher on its configured cron. `OrderOutboxDispatcher` claims rows with leases, calls the typed publisher, and records success or bounded-backoff retry state.
- Matching publishers retain the existing three typed topics. Credit consumes shared refunds and completion transfers. User consumes accepted-cancellation penalty facts for EVERY abort and all completion facts. Legacy ABORTED cancellation-event refunds need coordinated replay/reconciliation; new expired aborts emit a separate shared refund. Neither subscriber writes Order data.
- Every ACCEPTED-only abort first resets Credit synchronously, then saves immutable ABORTED history and current OPEN/EXPIRED under the same business ID. It always queues User penalty, plus Credit refund only when expired. ADR-025 supersedes the prior ADR-001/014 behavior for this slice. Current Order, history, checkpoints, receipt and outbox commit atomically; the external Credit reset does not share that transaction.

The diagrams are logical architecture, not permission to implement future-sprint operations.

## Updated design reconciliation

The updated overall design supersedes the previous generic outcome/synchronous consequence description for Sequences 5-7. CHANGE-063/ADR-013 supersedes the publish-first ordering in CHANGE-053 with a transactional outbox, after-commit fast path, and cron recovery. CHANGE-067/ADR-016 defines event snapshots as the current Order/repost fields without checkpoint history; overdue facts remain derived internally. CHANGE-073/ADR-021 uses real Google Cloud Pub/Sub in all environments, one project with separate topics, personal local ADC, and the Cloud Run service identity. Delivery is at least once; Cloud Run scale-to-zero/request-based CPU limits cron recovery while idle.


## Quarter-hour UI and scheduler cadence (ADR-022)

The shared frontend QuarterHourDateTimePicker owns local date/hour/quarter-minute presentation; orders helpers own rounding and client validation; Post Request/RepostControls coordinate the existing authenticated API calls. Order application/domain layers retain API validation and authoritative deadlines. No backend class dependencies or data-model fields change.

Historical ADR-022/023 cadence is superseded by ADR-026: OrderLifecycleScheduler
checks expiry and completion every minute; OrderOutboxScheduler recovers all
pending/failed event kinds every 15 minutes. Immediate dispatch and repository
cutoff/locking are unchanged. Failed publication can wait for the next quarter-hour
recovery pass; Cloud Run idle/scale-to-zero can delay it further.


## CHANGE-079 / ADR-024: Central role annotations (approved 2026-10-08)

Production Firebase validation resolves User Service roles once per request, verifies response identity against JWT subject, and enforces RequireRequesterRole/RequireCourierRole/RequireAdminRole. Order-specific production role mode defaults to HTTP; local anonymous/mock behavior remains. Adapters reuse verified roles/identity, retaining fresh courier eligibility and locked domain ownership/state guards before mutation. Shared reads accept any confirmed requester/courier/admin role; /mine uses its selected mode. Internal lifecycle/scheduler authorization, API bodies, event payloads and schema remain unchanged. See ADR-024 for endpoint policy, inspected peer contracts and positive/negative test obligations.

## Scheduler DB selection and independent outbox dispatch — CHANGE-096 (2026-10-10)

The active lifecycle paths retain CHANGE-093: DB-filtered IDs, separate REQUIRES_NEW expiry/completion workers, fresh NOWAIT locks and outside-proxy catch. Outbox recovery now selects bounded eligible event IDs in SQL (due PENDING or expired IN_PROGRESS lease), then individually claims/rechecks with SKIP LOCKED. Claim, markPublished and scheduleRetry use separate REQUIRES_NEW transactions; enqueue remains REQUIRED with Order/checkpoint/receipt. Dispatcher catches each event's claim/commit/retry-write errors, logs its ID and continues later events. A failed retry write leaves the committed lease recoverable after expiry. Neither scheduler nor batch coordinator is transactional. Pub/Sub publication stays outside DB transactions and is irreversible; stable-ID deduplication remains required. Legacy claimDue is retained for compatibility, unused by the active scheduler. No cadence, schema, event body, peer, frontend or paused repost change.

## Swagger current-origin server — CHANGE-098 (2026-10-10)

CHANGE-098 sets an explicit relative OpenAPI server / through OpenApiConfiguration. Swagger now resolves API calls against the origin serving the specification, retaining /api/orders paths and Firebase authorization. Live staging docs previously advertised an HTTP backend host despite HTTPS gateway UI. This is an Order-only documentation routing implementation detail; no gateway, peer, CORS allowlist, forwarded-header trust, schema, frontend or deployment-env change. Tests first: one expected server-URL failure and one existing documentation pass (target/change098-red.log). Fresh source-only wrapper-selected Maven3.9.16/Java21 offline verify:253 tests,0 failures/errors/skips, including28 PostgreSQL tests; coverage96.00% lines (1441/1501),83.72% branches (468/559), unchanged >=80% gates pass. Generated target/openapi.json servers=[{url:"/",description:"Current gateway or service origin"}];160 current POM/source/test/resource files equal the fresh tested copy. Logs/reports target/change098-verify.log and target/change098-source-check/target/, evidence target/change098-evidence.json. git diff --check passes. No local application container rebuild or real authenticated browser request; no commit/push/cloud deployment. Staging remains unchanged until Order Service redeployment.
