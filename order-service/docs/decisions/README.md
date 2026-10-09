# Architecture Decision Records

ADRs preserve approved business and architecture decisions that supersede or clarify other sources.

| ADR | Status | Summary |
|---|---|---|
| [ADR-027](ADR-027-repost-retry-polling-and-peer-auth-proposal.md) | User-approved design; not implemented | Polling, fixed-ID bounded retries and short terminal messages; trusted peer credentials documentation only, explicitly deferred |
| [ADR-026](ADR-026-shared-lifecycle-and-quarter-hour-outbox.md) | Accepted; locally verified | Shared minute expiry/completion, 15-minute all-event recovery, immediate dispatch retained |
| [ADR-023](ADR-023-hourly-outbox-recovery.md) | Cadence superseded by ADR-026 | Historical hourly recovery; immediate dispatch retained |
| [ADR-022](ADR-022-quarter-hour-ui-and-scheduler-cadence.md) | Accepted by user direction; implemented; outbox interval superseded by ADR-023 | Quarter-hour Requester timestamp selection, 15-minute expiry, and preserved minute auto-completion/immediate dispatch |
| [ADR-001](ADR-001-approved-project-amendments.md) | Accepted | OVERDUE, ABORTED reopening, service ownership, supplier authority, and reposting amendments supplied for persistent use |
| [ADR-002](ADR-002-courier-outcome-contract-split.md) | Accepted | Replaces the generic courier outcome flag/facts contract with explicit completed, overdue, and aborted operations |
| [ADR-009](ADR-009-updated-overall-event-architecture.md) | Partially superseded by ADR-011/013/021; live broker delivery pending | Records typed completion/cancellation events and same-order accepted reopening; current Pub/Sub topics/authentication are governed by ADR-021 |
| [ADR-010](ADR-010-order-transition-authorization.md) | Accepted | Requires requester ownership for completion/open cancellation and assigned-courier ownership for acceptance/progress/accepted cancellation |
| [ADR-014](ADR-014-accepted-cancellation-hybrid-flow.md) | Accepted | Unexpired accepted cancellation waits for synchronous Credit hold then reopens; expired cancellation emits the refund/penalty event |
| [ADR-011](ADR-011-unified-order-completion-event.md) | Accepted by user request; implementation verified | Publishes one completion event for every completed order with overdue facts consumed by both User and Credit |
| [ADR-012](ADR-012-admin-order-list-query.md) | Accepted | Adds an admin-authorized paginated Order-owned listing API with optional status filter to support NTH1 |
| [ADR-013](ADR-013-transactional-outbox.md) | Accepted by user approval; implemented and verified | Replaces publish-before-status with atomic Order/outbox persistence, after-commit dispatch, leased claims, bounded retry, and cron recovery |
| [ADR-015](ADR-015-scheduled-open-order-expiration-event.md) | Spring scheduling remains effective; separate event type superseded by ADR-019 | Uses a Spring scheduler to expire due OPEN orders; scheduled expiry now emits shared `OpenOrderRefundTaskEvent` |
| [ADR-016](ADR-016-exclude-checkpoint-history-from-events.md) | Accepted by user request; implementation in progress | Removes checkpoint history from outcome event payloads while retaining Order history and overdue calculation |
| [ADR-017](ADR-017-credit-courier-assignment-before-acceptance.md) | User-approved Order-side stub; real Credit API pending | Synchronously associate the courier with the reservation before persisting Order acceptance |
| [ADR-018](ADR-018-order-version-owned-by-order-service.md) | Accepted | Keep Order versions in Order checks and event payloads; omit them from synchronous Credit requests |
| [ADR-019](ADR-019-shared-open-order-refund-event.md) | Accepted by user direction; implementation in progress | Reuses one `OpenOrderRefundTaskEvent` and topic for requester cancellation and scheduled OPEN expiry; status distinguishes the result |
| [ADR-020](ADR-020-48-hour-delivered-order-auto-completion.md) | Accepted by user direction; implementation verification pending | Automatically completes `DELIVERED` orders after 48 hours through the existing completion checkpoint and event/outbox flow |
| [ADR-021](ADR-021-real-pubsub-dev-and-prod-topics.md) | Accepted by user approval; GCP setup pending | Uses real Pub/Sub in one GCP project with separate dev/prod topics, developer ADC locally, and Cloud Run service identity in production |
| [ADR-003](ADR-003-detailed-architecture-and-integration-gate.md) | Accepted | Requires peer implementation inspection and explicit detailed architecture/API/interaction approval before code or implementation tests |
| [ADR-004](ADR-004-shared-nextjs-frontend-role-model.md) | Accepted | Uses one shared responsive Next.js application with `ADMIN`/`USER` roles and Requester/Courier `USER` modes, subject to backend-authority reconciliation |
| [ADR-005](ADR-005-architecture-evolution-tracking.md) | Accepted | Classifies implementation discoveries, requires approval for architecture/specification changes, preserves supersession history, and synchronizes affected artifacts |
| [ADR-006](ADR-006-foreign-service-api-verification.md) | Accepted | Classifies expected-versus-actual peer APIs, records missing/unsuitable dependencies in one feedback file, and requires actual implementation verification before resumption |
| [ADR-007](ADR-007-per-turn-completion-reporting.md) | Accepted | Requires one auditable completion-report baseline for every Order Service turn while allowing meaningful scenario-specific sections |

Create a new ADR for a later decision; do not rewrite historical decisions without recording their supersession.

- [ADR-024: Central role annotations](ADR-024-central-role-annotations.md) — accepted; production User roles, per-request identity/role reuse, local split.
