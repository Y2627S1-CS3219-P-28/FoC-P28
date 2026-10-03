# Architecture Decision Records

ADRs preserve approved business and architecture decisions that supersede or clarify other sources.

| ADR | Status | Summary |
|---|---|---|
| [ADR-001](ADR-001-approved-project-amendments.md) | Accepted | OVERDUE, ABORTED reopening, service ownership, supplier authority, and reposting amendments supplied for persistent use |
| [ADR-002](ADR-002-courier-outcome-contract-split.md) | Accepted | Replaces the generic courier outcome flag/facts contract with explicit completed, overdue, and aborted operations |
| [ADR-009](ADR-009-updated-overall-event-architecture.md) | Partially superseded by ADR-011/013; live broker delivery pending | Records typed completion/cancellation events, Pub/Sub transport, full Order snapshots, placeholder topics, assumed future subscribers, and same-order accepted reopening |
| [ADR-010](ADR-010-order-transition-authorization.md) | Accepted | Requires requester ownership for completion/open cancellation and assigned-courier ownership for acceptance/progress/accepted cancellation |
| [ADR-014](ADR-014-accepted-cancellation-hybrid-flow.md) | Accepted | Unexpired accepted cancellation waits for synchronous Credit hold then reopens; expired cancellation emits the refund/penalty event |
| [ADR-011](ADR-011-unified-order-completion-event.md) | Accepted by user request; implementation verified | Publishes one completion event for every completed order with overdue facts consumed by both User and Credit |
| [ADR-012](ADR-012-admin-order-list-query.md) | Accepted | Adds an admin-authorized paginated Order-owned listing API with optional status filter to support NTH1 |
| [ADR-013](ADR-013-transactional-outbox.md) | Accepted by user approval; implemented and verified | Replaces publish-before-status with atomic Order/outbox persistence, after-commit dispatch, leased claims, bounded retry, and cron recovery |
| [ADR-003](ADR-003-detailed-architecture-and-integration-gate.md) | Accepted | Requires peer implementation inspection and explicit detailed architecture/API/interaction approval before code or implementation tests |
| [ADR-004](ADR-004-shared-nextjs-frontend-role-model.md) | Accepted | Uses one shared responsive Next.js application with `ADMIN`/`USER` roles and Requester/Courier `USER` modes, subject to backend-authority reconciliation |
| [ADR-005](ADR-005-architecture-evolution-tracking.md) | Accepted | Classifies implementation discoveries, requires approval for architecture/specification changes, preserves supersession history, and synchronizes affected artifacts |
| [ADR-006](ADR-006-foreign-service-api-verification.md) | Accepted | Classifies expected-versus-actual peer APIs, records missing/unsuitable dependencies in one feedback file, and requires actual implementation verification before resumption |
| [ADR-007](ADR-007-per-turn-completion-reporting.md) | Accepted | Requires one auditable completion-report baseline for every Order Service turn while allowing meaningful scenario-specific sections |

Create a new ADR for a later decision; do not rewrite historical decisions without recording their supersession.
