# Order Service Architecture

Authoritative sources: `../../../High Level Architecture Diagram - Order Service.png` and `../../../Class Diagram - Order Service.png`.

## Layers and ports

1. External callers: requester, courier, Admin Service, and trusted lifecycle triggers.
2. Inbound contracts: commands, queries, admin integration, expiry, auto-completion, and auto-repost triggers.
3. Application components: creation, assignment, transitions, queries/history, completion-time overdue evaluation, expiry, and reposting.
4. Order-owned domain rules: lifecycle/status, authorization, checkpoints/history, one-time overdue evaluation, and repost policy.
5. Outbound ports: Order persistence, User/Supplier/Credit contracts, and factual outcome publication.

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
- Application components invoke `OrderRepository`, User/Supplier/Credit service ports, and factual outcome-publication ports. Domain rules do not call repository or external-service ports directly.
- Outbound ports return records, responses, or publication acknowledgements to the application layer, which returns the command/query result through the inbound contract.

This direction keeps domain rules independent of persistence and peer-service integration while application components own orchestration.

## Overall class responsibilities

- `OrderCommandInterface` and `OrderCommandFacade` route creation, acceptance, transitions, cancellation, and repost commands.
- `OrderQueryInterface`/`OrderQueryService` provide available, current, account, and checkpoint/history views as their sprint scope permits.
- `LifecycleTrigger`/`LifecycleProcessingService` handle expiry, auto-completion, and automatic repost processing when in scope.
- Specialized application services handle creation, assignment, transitions, administration, and reposting.
- `OrderRepository` is the only Order persistence abstraction.
- `UserServicePort`, `SupplierServicePort`, and `CreditServicePort` isolate external calls.
- `UserServicePort` sends separate courier completed, overdue, and aborted notifications; it does not send a generic outcome flag or property bag.
- `OrderOutcomePublisher` publishes status, hold, repost, and resolution facts without transferring ownership. Any courier-outcome publication exposed through it must also use separate completed, overdue, and aborted operations.

The diagrams are logical architecture, not permission to implement future-sprint operations.

## Diagram reconciliation

ADR-002 supersedes the generic `submitCourierOutcomeFlag()` and `publishCourierOutcomeFlag()` operation names in the current overall class diagram. When an editable source becomes available, regenerate the class diagram and overall design pack with explicit completed, overdue, and aborted operations. Until then, use ADR-002 and `docs/service-contracts.md` as the effective contract.
