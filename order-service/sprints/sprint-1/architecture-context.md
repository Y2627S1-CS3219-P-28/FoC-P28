# Sprint 1 Architecture Context

Authoritative diagram (relative to the repository root): `../../../Sprint 1/S1 Class Diagram - Order Service + NTH 4.png`.

## Components relevant to sequences 7-11

- `OrderCommandInterface` exposes `confirmCompletion`, `cancelOpen`, `configureReposting`, and `requestManualRepost`.
- `OrderQueryInterface` exposes `getManualRepostDraft`.
- `LifecycleTrigger` exposes `processExpiry` and `processAutoRepost`.
- `OrderCommandFacade` delegates status and repost commands.
- `OrderTransitionService` performs completion, cancellation, and expiry transitions.
- `OrderRepostService` configures plans, checks eligibility, and creates one repost.
- `OrderQueryService` produces the manual repost draft.
- `LifecycleProcessingService` finds due orders and runs expiry/auto-repost processing.
- `Order` is the aggregate root and owns status and two-way repost links.
- `RepostPlan` is owned by the original order and contains repost credit/timing configuration plus reposted state.

## Outbound ports

- `OrderRepository`: load/save orders and find due originals.
- `UserServicePort`: verify requester/actor identity and role context.
- `SupplierServicePort`: validate supplier pairs and resolve current details.
- `CreditServicePort`: reserve credits before an order/repost becomes `OPEN`; synchronously hold/reset the existing transaction before an unexpired accepted order transitions directly back to `OPEN`. Completion and expired-cancellation consequences belong to typed event subscriptions in the updated overall design.
- Three typed event publisher ports: order completion (with `overdue` and `overdueAt` facts), open-order cancellation, and accepted-order cancellation. CHANGE-052/053/054 establish layout, full snapshots, and Google Cloud Pub/Sub; CHANGE-056 unifies completion delivery to both User and Credit; CHANGE-063/ADR-013 establishes an atomic transactional outbox with immediate after-commit dispatch and cron recovery. Peer consumers remain future work and are not verified in this Order-only milestone.

The updated overall design source changes Sequences 5-8. `CHANGE-051` records the comparison. CHANGE-064/ADR-014 further specifies the accepted-cancellation branches: unexpired cancellation waits for synchronous Credit hold then transitions `ACCEPTED -> OPEN`; expired cancellation transitions to `ABORTED` and publishes for Credit refund and User penalty. ADR-001 continues to prohibit `ABORTED -> OPEN`. Sequence 5 publishes one completion event with overdue facts for User and Credit; the overdue-only Sequence 8 event flow is superseded by CHANGE-056. CHANGE-055/ADR-010 require the assigned courier to cancel; requester-only ownership remains for open cancellation and completion.

Names represent approved logical responsibilities, not mandated Java packages or framework annotations. Concrete code structure must be proposed at the pre-coding gate after the project build is approved.
