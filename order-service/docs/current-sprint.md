# Active Sprint

Current sprint: Sprint 1

Implementation authorization: the user approved combined sequences 1-11 on the current handoff branch on 2026-09-30. CHANGE-053 approves the publisher/event payload direction; CHANGE-054 selects Google Cloud Pub/Sub; CHANGE-056 unifies completion events; CHANGE-063/ADR-013 approves transactional outbox delivery with immediate after-commit dispatch and cron recovery. Peer consumers are assumed future work and must not be edited in this scope.

Shared developer allocation: `docs/work-allocation.md`.

- Developer 1, Yao Xiang: sequences/features 1-6 on `sprint-1/seq-1-to-6`.
- Developer 2, Vincent: sequences/features 7-11 on `sprint-1/seq-7-to-11`.

Determine the current developer from the ignored `docs/local/developer-profile.md`, then validate that profile against the shared allocation, the developer's active-work file, and the current Git branch. This file does not identify a permanent current developer.

Authoritative scope:

- `sprints/sprint-1/scope.md`
- `sprints/sprint-1/requirements.md`
- `sprints/sprint-1/architecture-context.md`
- `sprints/sprint-1/contracts.md`
- `sprints/sprint-1/acceptance-tests.md`
- `sprints/sprint-1/class-diagrams/README.md`
- `sprints/sprint-1/sequence-diagrams/README.md`

Source implementation pack: `../../../Sprint 1/Order Service Sprint 1 Doc.pdf`.

Mandatory pre-implementation workflow references:

- `docs/architecture-review-playbook.md`
- `docs/architecture-evolution.md`
- `docs/peer-service-api-feedback.md`
- `docs/frontend-integration-workflow.md`
- `docs/event-candidates.md`

Shared frontend context: reuse `../frontend/`, the existing Next.js 16 App Router responsive web application. Sprint work with approved UI impact must identify `ADMIN`, `USER` Requester mode, or `USER` Courier mode and be coordinated against current frontend Git/active-work state. The role/client context does not authorize a detailed UI, shared-file change, or Sprint implementation.

For this branch-only approval, sequences 1-11 are in progress as one combined implementation. The normal two-developer allocation remains unchanged on other branches.

## Updated overall design reconciliation - 2026-10-02

CHANGE-057/ADR-012 adds an explicitly user-approved Order-side admin query supporting NTH1: `GET /api/orders` is admin-only, paginated, and optionally filtered by status. It does not implement the Admin Service or dashboard UI.

`../../../Order Service Overall Doc - Updated.pdf` informs Sequences 5-7. `CHANGE-053` records the full-Order event payload and topic placeholders; `CHANGE-054` selects Google Cloud Pub/Sub; `CHANGE-056` unifies completion events; `CHANGE-063/ADR-013` supersedes publish-before-status with atomic state/outbox commits, immediate after-commit dispatch, and cron recovery. Delivery is at least once and consumers deduplicate stable event IDs. Peer consumers remain future work and out of scope. Same-order `ABORTED` reopening remains outside this implementation.

`CHANGE-055` / `ADR-010` clarifies lifecycle authorization: accepted cancellation is performed by the assigned courier; acceptance requires a null courier assignment; courier progress remains assignment-bound; open cancellation and completion remain requester-owned. Role verification precedes idempotent receipt replay.

`CHANGE-064` / `ADR-014` adds a deadline split to accepted cancellation: before expiry, wait synchronously for Credit's hold/reset confirmation before changing `ACCEPTED` to `OPEN`; at/after expiry, transition to `ABORTED` and publish the cancellation event for Credit refund and User courier penalty. Open-cancellation, expired accepted-cancellation, and completion subscriber actions are detailed in `docs/peer-service-api-feedback.md`.

The Order Service persistence/deployment conflict is resolved by user-approved ADR-008: PostgreSQL on one Cloud SQL instance with separate staging/production databases, deployed through Cloud Run using a public-IP Cloud SQL Java Connector. This infrastructure decision does not authorize Sprint 1 business implementation or select a migration tool.

Open frontend contract blocker: the frontend application model is `ADMIN`/`USER` with Requester/Courier `USER` modes, while current backend conventions name requester/courier/admin authorities. The actual User Service contract and mapping must be inspected and approved before role-sensitive implementation.
