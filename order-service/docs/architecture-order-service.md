# Order Service Architecture

## Effective current workstream

CHANGE-082 / ADR-025 implements the user-selected overall PDF with approved lifecycle overrides. Editable class/sequence/data responsibilities are in `sprints/sprint-2-3/README.md`; the historical Updated PDF named below is absent locally. Order now has an internal UUID primary key plus a unique business ID and immutable courier-attempt snapshots. No peer service writes Order storage, and domain classes still do not invoke external ports.

Authoritative sources: `../../../Order Service Overall Doc - Updated.pdf`, `../../../High Level Architecture Diagram - Order Service.png`, and `../../../Class Diagram - Order Service.png`. The updated overall design informed Sequences 5-7 and typed event contracts; CHANGE-056 supersedes its separate overdue completion flow. Standalone diagrams remain separately fingerprinted artifacts.

## Layers and ports

1. External callers: requester, courier, Admin Service, and trusted lifecycle triggers.
2. Inbound contracts: commands, queries, admin integration, expiry, auto-completion, and auto-repost triggers.
3. Application components: creation, assignment, transitions, queries/history, completion-time overdue evaluation, scheduled auto-completion, expiry, and reposting.
4. Order-owned domain rules: lifecycle/status, authorization, checkpoints/history, one-time overdue evaluation, and repost policy.
5. Outbound ports: Order persistence, event-outbox persistence, User/Supplier/Credit contracts, and typed completion/cancellation/refund event publishers. Acceptance synchronously asks Credit to assign the reservation to the courier before persisting `ACCEPTED` (CHANGE-068/ADR-017; provider endpoint pending). OPEN expiry is discovered by `OrderExpiryScheduler` and records the shared `OpenOrderRefundTaskEvent` in the transactional outbox.
6. Messaging infrastructure: a transactionally persisted outbox, immediate after-commit relay, Spring cron recovery relay, and typed completion/cancellation/refund publishers backed by Google Cloud Pub/Sub. Local Compose and Cloud Run use the same project with separate dev/prod topics. Local Compose uses personal ADC mounted read-only; Cloud Run uses its attached service identity. Topic-level IAM grants publisher access only to the matching environment's topics. OPEN expiry is triggered separately by `OrderExpiryScheduler`; peer subscriptions are future work and are not awaited by Order Service.

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
- CHANGE-072/ADR-020 adds `OrderAutoCompletionScheduler` -> `LifecycleProcessingService` -> `OrderRepository` due-delivery selection -> `OrderTransitionService.autoComplete`; the transition shares the normal completion checkpoint, overdue facts, command receipt and outbox event.
- Specialized application services handle creation, assignment, transitions, administration, and reposting.
- `OrderRepository` is the only Order persistence abstraction.
- `UserServicePort`, `SupplierServicePort`, and `CreditServicePort` isolate synchronous external calls. Credit reservation and unexpired accepted-cancellation `holdForReopen` remain synchronous; Order waits for hold confirmation before reopening. Completion, cancellation, and OPEN-refund consequences otherwise use typed events.
- Credit `assignCourier` is also synchronous: `OrderAssignmentService` validates before the call, waits for the proposed matching reservation confirmation, then rechecks expiry before acceptance. The Order mock and HTTP adapter do not verify that Credit provides the route.
- `OrderTransitionService` and `LifecycleProcessingService` create the shared `OpenOrderRefundTaskEvent` through `OrderTaskEventFactory` for requester-cancelled and scheduler-expired OPEN orders; `OrderOutboxDispatcher` routes it through one refund publisher (and retains a compatibility route for already-persisted legacy outbox rows).
- A second configurable lifecycle cron selects only `DELIVERED` orders with delivered checkpoints at least 48 hours old in PostgreSQL, using pessimistic no-wait row locks. It rechecks the eligibility under lock and uses the same `OrderCompletionTaskEvent` outbox path as requester completion.
- `OrderTransitionService` records the post-transition event through `OrderEventOutboxRepository` in the same transaction as the Order, checkpoint, and command receipt. `OrderOutboxAfterCommitListener` requests immediate delivery before the transactional service call returns; `OrderOutboxScheduler` invokes the recovery dispatcher on its configured cron. `OrderOutboxDispatcher` claims rows with leases, calls the typed publisher, and records success or bounded-backoff retry state.
- Matching publishers retain the existing three typed topics. Credit consumes shared refunds and completion transfers. User consumes accepted-cancellation penalty facts for EVERY abort and all completion facts. Legacy ABORTED cancellation-event refunds need coordinated replay/reconciliation; new expired aborts emit a separate shared refund. Neither subscriber writes Order data.
- Every ACCEPTED-only abort first resets Credit synchronously, then saves immutable ABORTED history and current OPEN/EXPIRED under the same business ID. It always queues User penalty, plus Credit refund only when expired. ADR-025 supersedes the prior ADR-001/014 behavior for this slice. Current Order, history, checkpoints, receipt and outbox commit atomically; the external Credit reset does not share that transaction.

The diagrams are logical architecture, not permission to implement future-sprint operations.

## Updated design reconciliation

The updated overall design supersedes the previous generic outcome/synchronous consequence description for Sequences 5-7. CHANGE-063/ADR-013 supersedes the publish-first ordering in CHANGE-053 with a transactional outbox, after-commit fast path, and cron recovery. CHANGE-067/ADR-016 defines event snapshots as the current Order/repost fields without checkpoint history; overdue facts remain derived internally. CHANGE-073/ADR-021 uses real Google Cloud Pub/Sub in all environments, one project with separate topics, personal local ADC, and the Cloud Run service identity. Delivery is at least once; Cloud Run scale-to-zero/request-based CPU limits cron recovery while idle.


## Quarter-hour UI and scheduler cadence (ADR-022)

The shared frontend QuarterHourDateTimePicker owns local date/hour/quarter-minute presentation; orders helpers own rounding and client validation; Post Request/RepostControls coordinate the existing authenticated API calls. Order application/domain layers retain API validation and authoritative deadlines. No backend class dependencies or data-model fields change.

OrderExpiryScheduler uses a 15-minute default, OrderOutboxScheduler recovers pending events hourly, and OrderAutoCompletionScheduler remains minute-based. Immediate outbox dispatch and repository DB cutoff/locking remain unchanged. Legacy/direct API deadlines can wait for the next expiry pass; a failed publish may wait nearly an hour for recovery while the service is running. See CHANGE-078 for the current cadence and Cloud Run scale-to-zero limitation.


## CHANGE-079 / ADR-024: Central role annotations (approved 2026-10-08)

Production Firebase validation resolves User Service roles once per request, verifies response identity against JWT subject, and enforces RequireRequesterRole/RequireCourierRole/RequireAdminRole. Order-specific production role mode defaults to HTTP; local anonymous/mock behavior remains. Adapters reuse verified roles/identity, retaining fresh courier eligibility and locked domain ownership/state guards before mutation. Shared reads accept any confirmed requester/courier/admin role; /mine uses its selected mode. Internal lifecycle/scheduler authorization, API bodies, event payloads and schema remain unchanged. See ADR-024 for endpoint policy, inspected peer contracts and positive/negative test obligations.
