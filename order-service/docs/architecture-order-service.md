# Order Service Architecture

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
- Matching publishers emit one event type to a shared broker. Credit consumes shared OPEN-refund events, expired accepted-cancellation refunds, and completion transfers. User consumes expired accepted-cancellation events to apply courier penalties and every completion event to apply overdue penalties or on-time score reduction. Neither subscriber writes Order data.
- An unexpired accepted cancellation transitions directly `ACCEPTED -> OPEN` only after Credit synchronously confirms the transaction hold. An expired accepted cancellation transitions `ACCEPTED -> ABORTED` and publishes the event. `ABORTED -> OPEN` remains prohibited by ADR-001.

The diagrams are logical architecture, not permission to implement future-sprint operations.

## Updated design reconciliation

The updated overall design supersedes the previous generic outcome/synchronous consequence description for Sequences 5-7. CHANGE-063/ADR-013 supersedes the publish-first ordering in CHANGE-053 with a transactional outbox, after-commit fast path, and cron recovery. CHANGE-067/ADR-016 defines event snapshots as the current Order/repost fields without checkpoint history; overdue facts remain derived internally. CHANGE-073/ADR-021 uses real Google Cloud Pub/Sub in all environments, one project with separate topics, personal local ADC, and the Cloud Run service identity. Delivery is at least once; Cloud Run scale-to-zero/request-based CPU limits cron recovery while idle.
