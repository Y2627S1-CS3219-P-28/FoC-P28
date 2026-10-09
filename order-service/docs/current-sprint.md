# Active Sprint

## CHANGE-093 concurrency verification extension

Vincent approves the fifth real PostgreSQL race: courier acceptance versus
requester OPEN cancellation, both winner orderings, using existing unchanged
transactional locking. New pair 2/2 and all five races 10/10 pass, no skips;
49 related service/domain/transition regressions also pass, zero failures/errors/
skips (test commit 827844b). No new lifecycle/peer/schema/UI
design or overall Sprint completion; peer calls/dispatch remain mocked.

## Current validation slice - CHANGE-091 / ADR-030 (2026-10-09)

Vincent approves Order/frontend field-specific creation errors and a 30-minute
minimum for new automatic plans (scheduled due to expiry) and manual submissions
(submission to expiry). Existing saved explicit-expiry plans are grandfathered;
no migration, peer, retry or scheduler change. Source/test verification in
CHANGE-091; live browser/peer gates remain and Sprint stays [~].


## One-time Credit push security repair - CHANGE-090 (2026-10-09)

Vincent approved the narrow Credit converter/test exception. Signed push no longer
uses Firebase user-role lookup; ordinary user guards and all Google validators
remain. Fifteen new regressions and fresh clean Credit verify pass: 80 tests,
no skips, 95.48% line/85.56% branch coverage. Source/test commit 198cd7c.
See the interference MD and FEEDBACK-008 READY_FOR_VERIFICATION. Credit owner
review, local image rebuild and real Google refund/transfer/duplicate/DLQ checks
remain open. This does not close Sprint [~] or other peer/repost blockers.

## Effective local-live connector — CHANGE-088 (2026-10-09)

Vincent's approved local-only deployment refinement is implemented/tested in
ADR-029; [runbook](local-live-testing.md) supplies safe Setup/build/up/Check/Pause/down.
Real HTTP peers and real cloud financial push target local Credit/database.
60 config/safety and 16 restricted-ingress fixture assertions pass, not actual
cloud/financial verification. No real stack/cloud changes performed by this run.
User consumers, Credit semantic gaps and paused background retries stay open.
No sequence/Sprint `[x]` upgrade.

## Effective Credit review — CHANGE-087 (2026-10-09)

Vincent's documentation-only review runs on sprint-2-3-credit (09e04a0).
Credit assignment/core reset and refund/completion handlers now exist in source;
do not ask peers to recreate them. Current gaps are replay authorization (003),
terminal reservation/recovery (006), legacy accepted-cancellation refund routing
(007) and delegated background auth (005). Separate push provisioning exists but
live subscriptions/IAM/ledger are not verified. Read peer feedback section 6.
User consumers remain missing; retries paused. No source/cloud changes or [x].

## Effective CHANGE-086 follow-up (2026-10-09)

Explicit automatic new expiry and persisted latest failure outcomes are
implemented in the approved Order/frontend slice. V4 disables legacy plans
without an expiry; it does not delete rows or invent deadlines. My Requests
retains safe manual/automatic attempt messages after reload and clears them on
successful repost linkage. Strict rule: expiry > due >= original expiry.
Late execution only while saved expiry is future. Verification in CHANGE-086.
All background retries/trusted credentials remain paused; peers/browser/cloud
remain gates and Sprint status remains [~]. The dated paragraphs below are
historical context, not the effective expiry/persistence status.


Current sprint: Sprint 2 and Sprint 3 (Order-owned lifecycle slice; `[~]`)

CHANGE-085 implements visible authenticated 15-second polling and current manual
EXPIRED-card failure messages. Full reload does not retain that client message;
automatic persistent outcomes remain deferred. User now PAUSES ALL background
retry implementation until peer agreement, including mock-mode jobs.
Credentials remain proposal-only. Read the rewritten service-classified
peer-service-api-feedback.md (002-006) for provider work. Historical approval
and test evidence below are retained, not current completion claims.

Latest CHANGE-083 / ADR-026: one minute-based expiry/completion job and
15-minute outbox recovery; immediate AFTER_COMMIT publication retained.
Approved target: `repostExpiresAt > repostDueAt >= original.expiresAt`.
Explicit repost expiry, durable retries and insufficient-credit-only persistent
UI feedback are NOT implemented yet. CHANGE-084 / ADR-027 records Vincent's
approval of bounded same-ID temporary retries, short permanent-failure messages
and polling. Trusted credentials are approved for documentation/peer discussion
only: DO NOT IMPLEMENT them yet. Concrete retry/security design and peer agreement
remain open; FEEDBACK-005/006 and the diagram reconciliation record the gaps.
Do not claim 100% diagram compliance.

Current developer/workstream: Vincent, `sprint-2-3`, approved on 2026-10-08 in
CHANGE-081/082. Read `changes/CHANGE-082-sprint-2-3-lifecycle-implementation.md` and the
current section of `docs/active-work/vincent.md` before implementation. The user
selected the local `Order Service Overall Doc.pdf` alongside D1. The previously
fingerprinted Updated PDF is absent; do not claim the two PDFs are equivalent.

The approved lifecycle/history/reset/repost design is implemented under ADR-025.
V3 preserves business IDs and adds internal UUID/immutable attempts; every abort
resets Credit before OPEN/EXPIRED, queues User penalty and refunds only EXPIRED.
Reposts retain both IDs and hide linked expired originals before requester pagination.
Local verification passed 162 backend tests (no skips), 34 frontend tests,
lint/typecheck/build and fresh >=80% line/branch coverage. Peer assignment/reset
and event subscribers are missing/unverified; HTTP must fail closed, not mock-fallback.
Trusted auto-repost credentials, authenticated browser and Cloud Run scheduling
remain pending. This does not finish NTH3 Admin/report/hold/resolution features.
Read `sprints/sprint-2-3/README.md` for effective diagrams and boundaries.
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
