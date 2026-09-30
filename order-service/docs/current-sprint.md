# Active Sprint

Current sprint: Sprint 1

Implementation authorization: the user approved combined sequences 1-11 on branch `sprint-1/seq-1-to-seq-11` on 2026-09-30. Every feature still requires peer-service inspection, a detailed feature-level architecture proposal, resolution of applicable technology/API conflicts, and recorded verification.

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

The Order Service persistence/deployment conflict is resolved by user-approved ADR-008: PostgreSQL on one Cloud SQL instance with separate staging/production databases, deployed through Cloud Run using a public-IP Cloud SQL Java Connector. This infrastructure decision does not authorize Sprint 1 business implementation or select a migration tool.

Open frontend contract blocker: the frontend application model is `ADMIN`/`USER` with Requester/Courier `USER` modes, while current backend conventions name requester/courier/admin authorities. The actual User Service contract and mapping must be inspected and approved before role-sensitive implementation.
