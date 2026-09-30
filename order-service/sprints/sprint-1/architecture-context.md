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
- `CreditServicePort`: reserve credits before a repost becomes `OPEN`.
- `OrderOutcomePublisher`: publish status/repost facts when included by the implemented contract.

Names represent approved logical responsibilities, not mandated Java packages or framework annotations. Concrete code structure must be proposed at the pre-coding gate after the project build is approved.
