# Sprint 1 Class Diagrams

- `updated-overall/admin-order-query-class-diagram.md` (CHANGE-057): admin controller, query service, repository port, persistence adapter, JPA repository, DTO mapper, and role-authority components.

Authoritative sources (relative to the repository root):

- Core FR subset: `../../../Sprint 1/S1 Class Diagram - Order Service.png`.
- Core FRs plus NTH4 extension: `../../../Sprint 1/S1 Class Diagram - Order Service + NTH 4.png`.

Use the NTH4 extension for the current initial slice. It adds `LifecycleTrigger`, `LifecycleProcessingService`, `OrderRepostService`, `RepostPlan`, manual repost query/commands, due-order repository lookup, and repost/status fact publication while preserving the core services and ports.

The source images remain outside this service repository and are fingerprinted in `docs/project-d1-reference.md`. If their fingerprints change, reread and compare them before coding.

## Updated overall-design publisher classes

See [updated-overall/publisher-class-diagram.md](updated-overall/publisher-class-diagram.md) for the event-specific publisher interfaces, matching implementations, outbox repository/relay, and recovery scheduler. Sequence 5 uses one completion event with overdue facts for both consumers. CHANGE-053/054/056/063 define full resulting snapshots, placeholder topics, Google Cloud Pub/Sub, atomic outbox persistence, after-commit dispatch, and at-least-once retry.
