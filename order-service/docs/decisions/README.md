# Architecture Decision Records

ADRs preserve approved business and architecture decisions that supersede or clarify other sources.

| ADR | Status | Summary |
|---|---|---|
| [ADR-001](ADR-001-approved-project-amendments.md) | Accepted | OVERDUE, ABORTED reopening, service ownership, supplier authority, and reposting amendments supplied for persistent use |
| [ADR-002](ADR-002-courier-outcome-contract-split.md) | Accepted | Replaces the generic courier outcome flag/facts contract with explicit completed, overdue, and aborted operations |
| [ADR-003](ADR-003-detailed-architecture-and-integration-gate.md) | Accepted | Requires peer implementation inspection and explicit detailed architecture/API/interaction approval before code or implementation tests |
| [ADR-004](ADR-004-shared-nextjs-frontend-role-model.md) | Accepted | Uses one shared responsive Next.js application with `ADMIN`/`USER` roles and Requester/Courier `USER` modes, subject to backend-authority reconciliation |
| [ADR-005](ADR-005-architecture-evolution-tracking.md) | Accepted | Classifies implementation discoveries, requires approval for architecture/specification changes, preserves supersession history, and synchronizes affected artifacts |
| [ADR-006](ADR-006-foreign-service-api-verification.md) | Accepted | Classifies expected-versus-actual peer APIs, records missing/unsuitable dependencies in one feedback file, and requires actual implementation verification before resumption |
| [ADR-007](ADR-007-per-turn-completion-reporting.md) | Accepted | Requires one auditable completion-report baseline for every Order Service turn while allowing meaningful scenario-specific sections |

Create a new ADR for a later decision; do not rewrite historical decisions without recording their supersession.
