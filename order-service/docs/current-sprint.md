# Active Sprint

Current sprint: Sprint 2 and Sprint 3 (Order-owned work; planning gate)

Current developer/workstream: Vincent, `sprint-2-3`, approved on 2026-10-08 in
CHANGE-081. Read `changes/CHANGE-081-sprint-2-3-scope-and-decisions.md` and the
current section of `docs/active-work/vincent.md` before implementation. The user
selected the local `Order Service Overall Doc.pdf` alongside D1. The previously
fingerprinted Updated PDF is absent; do not claim the two PDFs are equivalent.

The missing Credit courier-assignment endpoint may be mocked for the local
Order-side milestone; it is not verified integration. Credit reset/reassignment
and expired-row replacement/history retention are awaiting clarification. No
Sprint 2-3 source changes, migrations, new tests or completion claims exist yet.
Prior Sprint 1 records remain historical and must not be deleted.

## Historical Sprint 1 context

CHANGE-080 repairs post-merge Compose validation by removing the retired user-mongodb helper whose volume was undeclared. Main's MongoDB/User Service configuration and Order's PostgreSQL/PubSub configuration are preserved. Base and HTTP-peer config validation pass locally; hosted CI rerun remains pending.

CHANGE-078/ADR-023 changes only the outbox recovery poll to hourly (top of each hour). The after-commit listener still attempts publication immediately; expiry remains every 15 minutes and auto-completion every minute. Failed events may wait nearly an hour for the next scan while the service runs; Cloud Run scale-to-zero can delay recovery further.

CHANGE-077/ADR-022 adds quarter-hour minute selectors to Requester creation/repost times and rounds default/minimum suggestions upward. Expiry defaults to every 15 minutes and outbox recovery hourly under ADR-023; auto-completion stays every minute and after-commit dispatch remains immediate. Frontend tests/lint/type checks and backend tests/coverage pass locally; six Docker integration tests and browser visual/deployed scheduling checks remain unverified.

CHANGE-076 sets the three Order Pub/Sub destination variables in production.env to the user-created prod-v1 topics and in staging.env to the existing dev-v1 topics. Both deployment templates render correctly. Main triggers staging; production remains manual promotion. Topic IAM and live publishing remain team verification tasks.

CHANGE-075 repairs the seven test failures/errors in the supplied CI attachment. Local verification now passes 117 tests; six PostgreSQL Testcontainers checks were skipped because this sandbox cannot access Docker. Fresh coverage is 90.82% lines and 81.19% branches. CHANGE-074 Compose config validation also passes with an empty temporary ADC path. Hosted CI, including PostgreSQL integration and image/infrastructure checks, must still run on the pushed fixes.

CHANGE-073/ADR-021 replaces the default Pub/Sub emulator with real Pub/Sub in the existing GCP project. Local Compose uses the three dev topic IDs, personal ADC, and topic-scoped publisher permissions; production topic IDs are environment-configured and Cloud Run uses its service identity. GCP topic creation/IAM and live publish verification remain pending. CHANGE-074 supplies a temporary empty ADC path to the CI-only Compose config check; the local stack still requires each developer’s personal ADC mount.

CHANGE-072/ADR-020 amends the earlier 48-hour auto-completion exclusion: a configurable Spring scheduler completes due `DELIVERED` orders using database cutoff selection, a row lock, and the existing completion event/outbox. The user authorized this Sequence 5 work on the profile's current branch; the shared allocation branch label remains inconsistent and was not changed.

CHANGE-069/ADR-018 keeps expected Order versions within Order's own commands and validation. Credit assignment sends only Order ID and courier ID; hold/reset sends only Order ID with no body. Event payloads retain `orderVersion`. CHANGE-070 finalizes FEEDBACK-003 as an agreed synchronous flow and request contract; the Credit endpoint remains unimplemented. FEEDBACK-004's assignment contract and endpoint also await Credit.

CHANGE-068/ADR-017 adds the user-approved Order-side Credit courier-assignment stub to Sequence 3. The service calls Credit synchronously after validating the locked order and persists `ACCEPTED` only after a matching successful response and a post-call expiry recheck. The mock is available in mock-peer mode; the proposed HTTP path has no matching Credit provider endpoint yet. See FEEDBACK-004. The earlier local compiler blocker was bypassed for verification in CHANGE-075 by launching the wrapper-selected Maven directly and using Java 21 source/target flags; the POM and hosted CI toolchain are unchanged.

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

`CHANGE-065` / `ADR-015` originally added a separate expiration event. `CHANGE-071` supersedes that event contract: the Spring `@Scheduled` job still discovers due unassigned OPEN orders and atomically persists `EXPIRED` plus its checkpoint, then records `OpenOrderRefundTaskEvent`. Requester-triggered OPEN cancellation records the same event with `CANCELLED`. Credit consumes the single event/topic and refunds/releases based on the resulting Order status. The synchronous Credit release call is removed. Cloud Run's current scale-to-zero/request-based CPU does not guarantee in-process scheduled execution while idle.

`CHANGE-067` / `ADR-016` removes checkpoint history from all published Order event snapshots to avoid growing message bodies. Order still stores and serves checkpoint history, and completion still derives `overdue`/`overdueAt` from it. The event contract stays at version 1 because peer consumers are future work and none were found in the inspected implementations.

The Order Service persistence/deployment conflict is resolved by user-approved ADR-008: PostgreSQL on one Cloud SQL instance with separate staging/production databases, deployed through Cloud Run using a public-IP Cloud SQL Java Connector. This infrastructure decision does not authorize Sprint 1 business implementation or select a migration tool.

Open frontend contract blocker: the frontend application model is `ADMIN`/`USER` with Requester/Courier `USER` modes, while current backend conventions name requester/courier/admin authorities. The actual User Service contract and mapping must be inspected and approved before role-sensitive implementation.


## CHANGE-079 / ADR-024: Central role annotations (approved 2026-10-08)

Production Firebase validation resolves User Service roles once per request, verifies response identity against JWT subject, and enforces RequireRequesterRole/RequireCourierRole/RequireAdminRole. Order-specific production role mode defaults to HTTP; local anonymous/mock behavior remains. Adapters reuse verified roles/identity, retaining fresh courier eligibility and locked domain ownership/state guards before mutation. Shared reads accept any confirmed requester/courier/admin role; /mine uses its selected mode. Internal lifecycle/scheduler authorization, API bodies, event payloads and schema remain unchanged. See ADR-024 for endpoint policy, inspected peer contracts and positive/negative test obligations.
