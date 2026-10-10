# Order Service Permanent Project Context

CHANGE-093 extends the approved Order database concurrency verification with
courier acceptance versus requester OPEN cancellation, both winner orderings.
New pair 2/2 and full five-race class 10/10 pass; combined acceptance/service
run 23/23 passes; actual domain/transition follow-up 36/36 passes, no failures/
errors/skips. Test commit 827844b; results and workflow limitations in CHANGE-093.
Existing locking/source/contracts unchanged; peers/dispatch mocked, no fresh
full-suite coverage or live financial verification claim. Sprint remains [~].

Current verification branch: Vincent explicitly requests four real PostgreSQL
race tests on sprint-2-3-credit-service-concurrency (CHANGE-092, 2026-10-09).
Order test-only scope; no new lifecycle policy, peer or schema changes.
Cross-service recovery remains deferred. Eight real PG race cases and full
215-test verify pass with no failures/errors/skips; fresh JaCoCo 95.67% lines /
84.70% branches. Peers/delivery mocked; no production/schema/frontend change or
live financial verification. Historical branch references below are dated
context, not current allocation. Full Sprint remains [~].
## Mode-specific status options — CHANGE-095 (2026-10-09)

Requester dropdown excludes ABORTED. Courier dropdown excludes OPEN, EXPIRED and CANCELLED; it retains ABORTED immutable-attempt history. All statuses remains the default. OrderStatusFilter requires an explicit requester/courier mode, supplied by each existing page. This is a UI-only refinement of ADR-032: existing API enum/query, authentication, ownership, pagination and five-second polling remain unchanged.

## Order personal filters and five-second polling — CHANGE-094 / ADR-032 (2026-10-09)

Yao Xiang explicitly requests five-second Order UI polling, Abort errand wording, and status filters on My Errands/My Requests. The existing /api/orders/mine adds optional status; default all, invalid status 400, existing identity/mode checks and page envelope retained. Database filters before page/count, preserving ABORTED courier attempts and hidden successfully reposted requester originals. Existing Base UI filters/pagination reset page 1 and cancel stale reads. Order-only polling is5 seconds; Credit/generic default 15 seconds; auth/visibility/no-overlap/focus/mutation protections retained. No scheduler, event, peer, schema or background-retry change. Verification and limits: CHANGE-094. Historical Order interval descriptions are superseded only by this approved amendment.

## Lifecycle per-order failure isolation — CHANGE-093 (2026-10-09)

Yao Xiang explicitly requests failed scheduled tasks be skipped while later successes continue. Due selection returns IDs filtered in the DB (latest delivery cutoff for completion), without locking a whole batch. Nontransactional lifecycle coordinator calls fresh NOWAIT-locking per-order transactions (expiry worker / existing autoComplete with REQUIRES_NEW), catches each RuntimeException including commit failures, logs order ID, counts only successful transitions, then continues. Scheduler retains independent whole-pass catches. Failed orders remain eligible next normal lifecycle pass; no new repost retry mechanism or cron/contract/schema/peer change. Previous batch transaction description is superseded by this refinement; CHANGE-092 compact payloads and FEEDBACK-009 remain unchanged.


## Effective compact-event amendment — CHANGE-092 / ADR-031 (2026-10-09)

Yao Xiang approved the exact seven-field Order-only payload: eventId, eventType, orderId, orderStatus, creditAmount, occurredAt, courierId. This replaces full snapshots and overdue facts ONLY for OpenOrderRefundTaskEvent and OrderCompletionTaskEvent. AcceptedOrderCancellationTaskEvent retains its existing v1 envelope/snapshot. Internal outbox versions and Pub/Sub eventVersion attribute remain (compact schema v2); topic names and DB schema unchanged. Old pending snapshot rows normalize at dispatch from their saved facts, with stable IDs. Lifecycle/outbox scheduling, locks, synchronous Credit assignment/reset and ADR-025 abort behavior remain. Historical v1 descriptions below are superseded for these two bodies. Credit currently requires the old snapshot and overdue; FEEDBACK-009 is INCOMPLETE_OR_INCOMPATIBLE. User completion penalties need an agreed separate overdue source. User approved implementing Order-only and documenting peer work; live integration remains blocked, Sprint [~].


## Current validation amendment - CHANGE-091 / ADR-030

Vincent explicitly approved field-specific creation errors and 30-minute new
repost windows on sprint-2-3-credit, 2026-10-09. New automatic expiry >= scheduled
due + 30 minutes; due >= original expiry. Manual expiry >= submission + 30 minutes.
Existing saved explicit-expiry plans are grandfathered; no silent disable/update.
Late automatic execution still uses its saved future expiry, not execution +30.
Reuse existing field-error envelope; local creation checks precede Supplier and
Credit calls after requester verification. Backend remains authoritative. Existing
responsive Order controls only; no shared auth/client, peer, schema, retry or event
changes. Prior V4 disable-without-expiry decision remains. Verification tracked in
CHANGE-091; Sprint remains [~] pending live/browser/cloud and existing peer gates.


## One-time Credit push security exception - CHANGE-090 (2026-10-09)

Vincent explicitly approved isolating the existing Google push converter from
Firebase role lookup and regression tests. See the interference record under
changes/CHANGE-090-credit-push-security-interference.md. This task-specific
exception does not authorize Credit business/contract/schema changes or other
peer edits. Annablee owns Credit; FEEDBACK-008 is READY_FOR_VERIFICATION: fresh
Java 21 clean verify passes 80 tests/no skips, 95.48% line/85.56% branch coverage;
real Google/ledger/backlog checks and local application rebuild remain open.
Source/test commit 198cd7c. Existing event architecture and paused repost retries
are unchanged. The dated documentation-only scope below is historical.

## Local real broker connector — CHANGE-088 (2026-10-09)

Vincent approved isolated local-live testing, implemented by ADR-029. Use base +
HTTP + `compose.local-live.yaml`, personal ADC, unique test namespace and
`scripts/local-live.ps1` Setup/Check/Pause. Financial push goes through restricted
HTTPS into local Credit; current Google OIDC retained. Existing Order authenticated
profile selects HTTP roles, not cloud DB. Staging/base Compose/peer source unchanged.
60 config/safety and 16 actual nginx/fixture assertions pass; actual cloud/ledger/
browser tests await user's gcloud/ADC/permissions. [Runbook](local-live-testing.md).
No User penalty consumer added, no accepted-cancellation Credit subscription.
Background credentials/retries paused; previous feedback gaps/Sprint `[~]` retained.

## Integrated Credit inspection — CHANGE-087 (2026-10-09)

Current review branch: Vincent, `sprint-2-3-credit`, user explicitly requested
documentation-only review of integrated revision 09e04a0. Source now has Credit
courier assignment/core hold and refund/completion push consumers. Earlier
missing-route/consumer statements are historical, not the current source state.
Follow-up ran 27 existing focused Credit tests successfully (including 12 isolated
PostgreSQL tests) from read-only copied source. Refund and completion transfer/
duplicate behavior verified at Credit's layers; no deployed/end-to-end verification.
Normal Order abort is locked/status-version checked and waits for Credit 200;
completed command receipt replay prevents repeating Credit reset. The earlier
short stale-clear example did not describe that protected normal sequence.
Feedback section 6 records implemented capabilities without claiming live
verification. Remaining Credit gaps: 003 reset replay authorization; 006 terminal
reservation replay/cross-database recovery; 007 superseded ABORTED refund consumer
on the now User-only accepted-cancellation topic. Separate provisioning script
exists but deployed delivery/IAM/ledger effects remain unverified. Production
provisioning is disabled; local Compose is not a public cloud push subscriber.
005 delegated background auth is still missing/proposal-only; inbound Pub/Sub
OIDC does not provide outbound requester delegation. ALL retries remain paused.
No approved architecture, peer source, app code, schema or cloud setting changed.
Sprint remains [~]. Read peer feedback for remaining provider work, not old
dated missing-route paragraphs. D1/selected Overall hashes remain unchanged.

## Effective follow-up — CHANGE-086 (2026-10-09)

Vincent requested and approved explicit automatic `repostExpiresAt` and latest
manual/automatic failure persistence. Both are now implemented in Order's source;
the prior limitation paragraphs below are historical. UI uses 00/15/30/45 slots.
Domain/API require `repostExpiresAt > repostDueAt >= original.expiresAt`;
late execution uses the saved expiry and cannot run at/after it. V4 disables
legacy enabled plans without an explicit expiry, per Vincent's chosen migration,
rather than inventing deadlines. Existing orders/history/refund intents remain.

Eligible authorized failures save only safe code/message/time on the original
EXPIRED row after rollback, overwrite an older result, and clear on success.
UI reads those fields after reload/polling and refetches the original version
after a failed foreground POST. No new repost order is saved on peer rejection.
Background retries and trusted credentials remain PAUSED/not implemented in
every mode. Peer feedback 002-006 and live browser/cloud gates remain open.
CHANGE-086 owns this task's actual tests/migration evidence; Sprint stays [~].

## Historical implementation and approval context


CHANGE-085 (2026-10-09): authenticated visible-page 15-second Order-list and Credit
polling implemented with focus/mutation refresh, no overlap and stale/session
response guards. Manual repost shows short semantic insufficient/permanent errors
inside its EXPIRED card; message is client-local, not persisted across reload.
Credit reservation errors are mapped without guessing delayed refund status.
Latest explicit user instruction PAUSES ALL background retry implementation
(including mock mode) until peers agree; credentials remain documentation ONLY.
Peer feedback was replaced by service at user request; historical versions remain
in Git. Explicit auto next-expiry and durable automatic failure state remain
unimplemented. The approval-only paragraphs below describe the earlier turn,
not the current polling/manual-message implementation status.

Latest decision CHANGE-084 / ADR-027 (2026-10-09): Vincent approved polling and
same-candidate-ID temporary repost retries bounded by new expiry, stopping
confirmed insufficient credits, invalid details and authorization/permanent
rejection with short appropriate messages while the original stays EXPIRED.
The balance error cannot distinguish a delayed refund. Trusted peer credentials
are approved for documentation/discussion ONLY; DO NOT IMPLEMENT them yet.
Polling, durable retries and persistent failure UI remain unimplemented. Peer
acceptance, the concrete security/task design and FEEDBACK-005/006 remain open.

Latest implemented CHANGE-083 / ADR-026: one minute expiry/completion job, 15-minute outbox
recovery and retained immediate dispatch; retired timer settings do not apply.
Supplier valid:false handling repaired on Order side. Explicit repost expiry,
durable retries and insufficient-credit-only card feedback remain incomplete.
Read ADR-027, the diagram reconciliation gap table, Vincent active-work and
FEEDBACK-005/006 before continuing; do not confuse user design approval with
peer contract acceptance, implemented code or verified integration.

This is the canonical permanent context for `order-service`. Detailed requirements remain authoritative in their source documents; this file records stable boundaries, decisions, and navigation.

## Current workstream - CHANGE-081/082 (2026-10-08)

Vincent is working on `sprint-2-3`, scoped to Order-owned Sprint 2-3 behavior.
Read CHANGE-082 / ADR-025, the current-sprint pointer and Vincent's current active-work
section before using the historical Sprint 1 context below. The user selected
`../../../Order Service Overall Doc.pdf` for this work alongside D1; the previously
recorded Updated PDF is absent locally. Existing approved ADRs remain effective
except where current explicit approvals supersede them; do not equate the PDFs.

The user approved one current Order plus immutable courier-attempt history,
an internal UUID separate from business orderId, ACCEPTED-only abortion,
User penalty signaling for every abort and Credit refund signaling for expired
outcomes. The missing courier-assignment API may be mocked locally, not silently
in HTTP mode. Every abort synchronously calls the approved bodyless hold/reset
route to clear Credit courierId, then resolves current state OPEN/EXPIRED and
saves separate ABORTED history. Every abort queues User penalty; EXPIRED also
queues Credit refund. Repost retains the old EXPIRED row and new row/new business
ID; the implemented requester query hides linked expired originals before
pagination/counting. Refund/history references retain the old ID. V3 adds an
internal row UUID and immutable attempt IDs without changing business references.
The local source/migration/UI test gates passed; live provider/subscriber retry
safety and trusted auto-repost credentials remain pending in peer feedback.
Editable effective diagrams/traceability: `sprints/sprint-2-3/README.md`.
Do not interpret historical Sprint 1 flags as blocking this approved Sprint 2-3
slice, or passing local tests as production completion of every overall capability.

## Repository state at workflow setup

On 2026-09-23, the repository contained only empty `AGENTS.md`, `README.md`, and `Dockerfile` files. No Spring Boot source, React source, tests, PostgreSQL configuration, Docker implementation, Kubernetes manifests, CI/CD workflow, or service-local documentation existed. The approved stack is therefore a constraint for later setup, not an installed implementation.

No production code was created as part of the workflow setup.

## Source documents

The following source paths are relative to the repository root.

- Product backlog and platform NFRs: `../../../Project-D1.pdf`
- User-selected current workstream overall design: `../../../Order Service Overall Doc.pdf` plus approved amendments (CHANGE-081/082).
- Historical updated design (currently absent locally): `../../../Order Service Overall Doc - Updated.pdf`; do not assume equivalence.
- Overall platform architecture: `../../../High Level Architecture Diagram - FOC.png`
- Order Service architecture: `../../../High Level Architecture Diagram - Order Service.png`
- Overall Order Service class diagram: `../../../Class Diagram - Order Service.png`
- Sprint 1 implementation pack and sequences: `../../../Sprint 1/Order Service Sprint 1 Doc.pdf`
- Sprint 1 core class diagram: `../../../Sprint 1/S1 Class Diagram - Order Service.png`
- Sprint 1 core plus NTH4 class diagram: `../../../Sprint 1/S1 Class Diagram - Order Service + NTH 4.png`

See `docs/project-d1-reference.md` for version fingerprints and known document conflicts.

## Specification authority

Use the authority order in `AGENTS.md`. Project D1 is the default behavioral source unless an approved persistent amendment supersedes it. Sprint documents may narrow implementation scope but must not silently alter permanent requirements.

## Technology and deployment status

- Backend: Spring Boot.
- Frontend: the existing shared top-level `../frontend/` application, implemented with Next.js 16 App Router and React 19.
- The user-approved Order Service infrastructure uses PostgreSQL on Google Cloud SQL and Cloud Run. The parent repository's Firestore convention remains applicable to other services unless their owners approve a separate decision.
- Order Service uses one shared Cloud SQL instance in `asia-southeast1` with separate `order_staging` and `order_production` databases. Cloud Run connects through a public-IP Cloud SQL Java Connector. Automated backups, point-in-time recovery, deletion protection, and possible Cloud SQL charges are accepted decisions.
- The full decision and trade-offs are recorded in `docs/decisions/ADR-008-order-service-cloud-sql-cloud-run.md` and `changes/CHANGE-015-order-service-cloud-sql-cloud-run.md`.
- Client: one responsive web application used from desktop and mobile browsers. No separate native mobile application or `mobile-client/` is currently included.
- Frontend application roles are `ADMIN` and `USER`. Requester and Courier are modes/functions of the same `USER` account, not separate applications or accounts. Do not assume `ADMIN` can use either mode.
- The shared Order dashboard does not provide a client-side Requester/Courier mode switch. Both user functions are exposed through the shared navigation; backend authorization and authenticated User Service identity remain authoritative.
- The parent backend convention currently names requester/courier/admin authorities. The exact mapping between those authorities and the frontend `ADMIN`/`USER` plus mode model is unresolved and must be inspected and approved before role-sensitive implementation.
- Each microservice exclusively owns and writes its own database.
- Cross-service behavior uses explicit authenticated contracts and stable identifiers.
- The architecture uses inbound contracts, application services, Order-owned domain rules, outbound ports, Order-owned persistence, and typed task-event publishers for completion, accepted cancellation, and shared OPEN refunds. CHANGE-063/ADR-013 specifies an atomic transactional outbox with immediate after-commit dispatch and cron recovery; CHANGE-054 selects Google Cloud Pub/Sub; CHANGE-056 unifies completion into one event type with overdue facts; CHANGE-065/ADR-015 adds Spring-scheduled OPEN expiry and CHANGE-071/ADR-019 makes it share the refund event/topic with requester cancellation.
- The corrected Order Service high-level diagram establishes that application components orchestrate and invoke persistence/external-service/publication ports. Order-owned domain rules validate and return decisions/status/flag/checkpoint data; they do not invoke outbound ports directly.

## Detailed architecture approval workflow

- Rehydrate the applicable repository context before every Order Service workflow, architecture, design, implementation, test, verification, resumption, or completion turn, including later turns in the same conversation. Repository records, not chat history, are authoritative.
- Overall architecture is the long-term blueprint; active Sprint architecture is its approved implementation subset. Neither silently replaces the other.
- Before any application code or implementation tests, produce a feature-level component/class/sequence/contract/data/failure/security/observability/test design and obtain explicit user approval for every interaction.
- Before designing integrations, inspect the real User, Supplier, Credit, and Admin service repositories and recheck each relevant provider for the feature. The implemented API, authentication, errors, retries, idempotency, configuration, and tests take part in the comparison but do not silently override the specification.
- Any expected-versus-actual API mismatch requires an explicit adapter/change/reject decision. An unapproved mismatch blocks implementation; a rejected, missing, unsuitable, incomplete, or incompatible API is recorded in `docs/peer-service-api-feedback.md`.
- Every foreign API is classified as matching, different but potentially usable, similar but unsuitable, missing, or incomplete/incompatible after comparing the actual implementation's operation, request/response, authorization, errors/statuses, communication style, semantics, tests, and sequence fitness.
- `docs/peer-service-api-feedback.md` is the single shared append-only record for missing, unsuitable, incomplete, or incompatible dependencies. ADR-006 supersedes the unused planned `docs/integration-api-gaps.md` location; no prior gap entry required migration.
- Peer confirmation is not verification. Re-read the actual provider code/tests before setting feedback `VERIFIED` or resuming integration. Approved prototypes/stubs retain an explicit unimplemented-contract risk unless the user approves a contract-stub milestone.
- For event candidates, compare synchronous request-response, query/polling, in-process events, and durable broker events. Updated overall Sequence 6 includes requester-triggered OPEN cancellation and Spring-scheduled OPEN expiry using the shared `OpenOrderRefundTaskEvent`; CHANGE-063/ADR-013 approves transactional outbox dispatch and recovery, CHANGE-054 selects Pub/Sub, CHANGE-056 unifies completion event routing, and CHANGE-065/ADR-015 approves scheduled expiry publication.
- `docs/event-candidates.md` is the persistent decision registry for D1-supported candidates. Its `EV-1` through `EV-7` IDs are stable; architecture decisions use separate `EV-DEC-NNN` IDs. CHANGE-053/054/056/063/065/067/071 record the typed event flows, resulting Order snapshots without checkpoint history, Pub/Sub transport, at-least-once outbox delivery, and Spring-scheduled OPEN expiration using the shared refund event.
- `docs/architecture-review-playbook.md` preserves the peer-inspection, API-mismatch, detailed-proposal, deviation, AI-disclosure, required-response, and post-approval templates used at the feature gate.
- Label proposal content as approved architecture, existing peer implementation, proposed design, unresolved decision, or user-approved deviation. Record approvals before implementation.

## Architecture evolution during implementation

- `docs/architecture-evolution.md` is the persistent register for design discoveries, classifications, proposal/approval state, current rules, supersession, and artifact synchronization.
- Read it on every implementation-affecting turn and follow the latest approved effective design rather than superseded source or chat memory.
- Classify a discovery as an implementation detail, design refinement, or architecture/specification change before acting. Material refinements are documented; architecture/specification changes require explicit user approval.
- The proposal must preserve the original design, explain the discovered insufficiency, compare viable alternatives and trade-offs where appropriate, recommend without treating that recommendation as approval, and enumerate all affected artifacts/files.
- An approved evolution is not `IMPLEMENTED` until requirements, architecture/class/sequence diagrams, data model, contracts, tests, implementation, traceability, change/decision records, and supersession links are synchronized or explicitly blocked.

## Per-turn completion reporting

- `docs/completion-reporting.md` is the single response-format authority for every Order Service chat turn, including advisory-only, status, planning, partial, blocked, decision-request, workflow, design, implementation, test, and verification turns.
- Every final response retains the required Added/Updated/Removed/Design Decisions/Affected Artifacts/Verification/Remaining Issues audit sections and uses `None.` where empty.
- The audit sections are a baseline, not the entire response. Add meaningful task-specific headers and details before, between, or after them while preserving the required sections' relative order.
- Do not create a separate per-turn Markdown summary; update the durable record that owns the information.

## Shared frontend workflow

- `docs/frontend-integration-workflow.md` is the persistent authority for Order Service work affecting `../frontend/`.
- Reuse the existing Next.js App Router structure, gateway-based `useApi()`, Firebase authentication, runtime configuration, shadcn/Base UI components, and current routing conventions where suitable. Do not create a competing frontend or migrate the routing model without approval.
- Before every coding turn, re-read applicable frontend instructions/configuration, inspect routes/layouts/components/styles/API/auth/permissions/tests, check current Git state and overlapping developer work, and confirm scope.
- Frontend UI checks are never authoritative security. Backend services enforce every protected operation.
- Design affected backend and frontend work together. Identify `ADMIN`, `USER` Requester mode, or `USER` Courier mode; actual API contracts; desktop/mobile-web behavior; shared-file impact; and required frontend/contract tests before implementation.
- The approved role/client context is not a detailed UI specification. Do not invent layouts, navigation, a mode switcher, controls, messages, or feature behavior.

## Approved communication boundaries

- Credit reservation before `OPEN` and synchronous hold/reset before unexpired accepted-order reopening remain synchronous. Under CHANGE-063/ADR-013, event-producing transitions commit with their outbox rows; an after-commit listener tries immediate publication and cron retries due rows. A publish failure does not undo a committed Order transition; delivery is at least once and no event subscriber reply is awaited. CHANGE-064/ADR-014 defines the accepted-cancellation expiry split.
- Under CHANGE-068/ADR-017, acceptance synchronously asks Credit to associate the active reservation with the courier before Order persists `ACCEPTED`. An Order-side mock and proposed HTTP adapter exist; the provider route, trusted service authentication, and recovery agreement remain open in FEEDBACK-004.
- Order Service owns lifecycle/status. Credit Service owns balances, reservations, releases, transfers, settlement, deductions, and credit policy.
- Penalty policy remains in User Service. Under CHANGE-063/ADR-013, Order Service commits factual event intent with the corresponding outcome, then publishes through Google Cloud Pub/Sub; it does not decide points, scores, or suspension. Stable event IDs support consumer deduplication.
- Immediate validation, courier acceptance/concurrency, authoritative transitions, and operations needing immediate success/failure are synchronous.
- Dashboards, history, available-order queries, and ordinary internal queries do not require a broker. The updated design specifically assigns typed broker events to completion/cancellation consequences in Sequences 5-7.

## Service ownership

- User Service owns accounts, identity, roles, courier eligibility, penalties, penalty policy, suspension, and unsuspension.
- Supplier Service owns supplier records, activation, approved campus locations, and catalogue details.
- Order Service owns errand creation, lifecycle/status, assignment, transitions, checkpoints/history, supplier references, repost relationships, order flags, and publication of order facts/outcomes.
- Credit Service owns balances, reservations, release, transfers, settlement, deductions, and credit policy.
- Admin Service independently owns monitoring, reports/cases, administrative decisions, user administration, and supplier/catalogue administration.

Nice-to-have ownership: NTH1 and NTH3 belong to Admin Service; NTH2 belongs to User Service; NTH4 belongs to Order Service; NTH5 belongs to the platform/deployment process.

- CHANGE-057/ADR-012 explicitly approve an Order-owned, admin-authorized, paginated `GET /api/orders` query to support future NTH1 dashboard integration. The Admin Service still owns the dashboard; its implementation and frontend UI are not included.

## Persistent approved amendments

### 48-hour automatic completion (CHANGE-072 / ADR-020)

- A configurable Spring scheduler finds `DELIVERED` orders by database query once their delivered checkpoint is at least 48 hours old.
- Recheck the cutoff and status under a pessimistic no-wait row lock; record `COMPLETED`, lifecycle checkpoint, idempotent receipt, overdue facts, and the existing `OrderCompletionTaskEvent` outbox intent atomically.
- Requester-confirmed completion remains available earlier. No new event, peer integration, or schema is introduced. Cloud Run scale-to-zero can pause this in-process scheduler.

### OVERDUE

- `OVERDUE` is a flag, not a status.
- Evaluate it once when an order reaches completion by comparing actual delivery duration with the configured delivery time limit.
- Do not introduce continuous overdue monitoring or a scheduled overdue scanner.
- Order Service records/publishes the fact; User and Credit Services apply their own policies.
- This amendment supersedes the continuous monitoring language in Project D1 F12.1-F12.1.4.

### Accepted-order cancellation and reopening

- Under CHANGE-064/ADR-014, only the assigned courier may cancel an accepted order. Before `expiresAt`, Order synchronously waits for Credit to hold/reset the transaction without refund; only success permits direct `ACCEPTED -> OPEN` with the same order ID and cleared courier assignment.
- At/after expiry, Order changes `ACCEPTED -> ABORTED` and emits the accepted-cancellation event; Credit refunds and User applies the courier penalty. If expiry passes during the hold request, recheck and use this event path.
- This does not implement `ABORTED -> OPEN`; ADR-001 continues to prohibit that separate transition. NTH4 reposting creates a distinct linked order.
- Completion and open cancellation remain event-driven: Credit transfers completion credits/refunds open-cancellation reservation; User handles completion overdue penalty or on-time score reduction. User does not subscribe to open cancellation.

### Penalty ownership

- User Service alone calculates and applies penalties.
- Order Service publishes relevant facts; Admin Service investigates and issues decisions.
- Order Service and Admin Service must not calculate penalty points.

### Typed outcome event contracts

- The current Order design defines three typed Order events: `OpenOrderRefundTaskEvent` for requester cancellation or scheduled OPEN expiry, `AcceptedOrderCancellationTaskEvent`, and one `OrderCompletionTaskEvent` carrying `overdue` and `overdueAt` for every completion.
- CHANGE-053 directs each event to carry the Order snapshot and use a configured topic. CHANGE-054 selects Google Cloud Pub/Sub. CHANGE-067/ADR-016 clarifies the snapshot contains the current Order/repost fields but omits checkpoint history; completion overdue facts are computed from checkpoints internally. CHANGE-073/ADR-021 configures three dev topics in the existing GCP project, personal ADC locally, and production topics through Cloud Run environment configuration/service identity. The user authorizes Order Service-only changes and assumes peer consumers will be implemented later; the peer assumption is not a verified integration.
- Each payload carries `eventId`, `eventVersion`, `orderId`, `orderVersion`, event-specific facts, actor IDs, and `occurredAt`; consumers deduplicate at-least-once delivery.
- Credit refunds `OpenOrderRefundTaskEvent` for both OPEN outcomes, distinguishing requester cancellation (`order.status=CANCELLED`) from scheduler expiry (`order.status=EXPIRED`); it refunds `AcceptedOrderCancellationTaskEvent` only for expired accepted cancellation and transfers credits for every `OrderCompletionTaskEvent`. User applies a courier penalty for expired accepted cancellation and, for every completion, an overdue penalty or the existing on-time score reduction. User does not consume OPEN-refund events. Subscribers own independent acknowledgments and retries. This follows CHANGE-056/ADR-011, CHANGE-064/ADR-014, CHANGE-065/ADR-015, and CHANGE-071/ADR-019.
- Each event carries the resulting Order and repost fields, without checkpoint history, and publishes to the environment-configured topic. The outbox commits event intent with the lifecycle transition, then dispatches after commit and recovers due rows through cron. Delivery is at least once and consumers must deduplicate by stable event ID. No generic outcome discriminator or open-ended facts bag is permitted.

### Supplier ownership

- Supplier Service is the source of truth for supplier names, status, locations, and catalogue details.
- Order Service stores supplier IDs and resolves current details through Supplier Service contracts, including for historical orders.

### Reposting

- An unaccepted `OPEN` order becomes `EXPIRED`.
- Automatic or requester-reviewed manual reposting may create at most one new linked order.
- The repost has a distinct identity and lifecycle and is linked both ways with the original.
- Credits are reserved only when the repost is created and before it becomes `OPEN`; configuring a future repost does not reserve credits.
- Automatic and manual paths must be idempotent.

## Cross-cutting contract rules

- Carry authenticated actor context or trusted service identity.
- Carry command IDs for idempotent create/reserve/transition/expiry/repost operations and globally unique event IDs/versions for published event delivery.
- Include expected order version for status-changing commands; stale requests produce a conflict.
- Distinguish validation, unauthorized, not found, conflict, dependency unavailable, reservation rejection, and accepted-for-processing outcomes.
- Published facts contain event ID and order version for deduplication and ordering.
- Completion/cancellation event types are distinct contracts; payloads carry event/version/order correlation and event-specific facts, with no generic `facts` map or outcome discriminator.

## Quality rules

- TDD is mandatory.
- Project D1 NFR3 requires automated unit and acceptance tests, at least 80% line and branch coverage, deployment only after tests pass, positive and negative cases, equivalence partitions, and boundary values.
- Project D1 NFR4 requires machine-readable local service logs for cross-service calls, automated actions, and user/admin actions, including timestamp, action, and actor.
- Performance targets are deferred to their planned sprint unless active scope says otherwise, but design must not contradict them.

## Current work navigation

- Active sprint pointer: `docs/current-sprint.md`
- Shared Sprint 1 allocation: `docs/work-allocation.md`
- Local current-developer identity: ignored `docs/local/developer-profile.md`
- Per-developer unfinished work/handoff: `docs/active-work/`
- Approved decisions: `docs/decisions/`
- Detailed approved changes: `changes/`
- Human-readable workflow index: `skills.md`
- Project AI disclosure log: `../ai/usage-log.md`
- Detailed architecture review playbook: `docs/architecture-review-playbook.md`
- Architecture-evolution register: `docs/architecture-evolution.md`
- Per-turn completion reporting: `docs/completion-reporting.md`
- Peer-service API feedback: `docs/peer-service-api-feedback.md`
- D1-supported event proposal registry: `docs/event-candidates.md`
- Shared Next.js frontend integration workflow: `docs/frontend-integration-workflow.md`
- AI usage log formatting authority: `docs/ai-usage-format.md`

At every task boundary, validate the local developer profile, shared allocation, matching active-work file, and Git branch. The local identity is intentionally not stored in committed shared context.

CHANGE-069/ADR-018 defines the Order/Credit contract boundary: Order validates versions, ownership, and amount locally; assignment sends only `orderId` plus `courierId`, and hold/reset sends only `orderId` with no body. Published events retain `orderVersion` for consumers that need it. This leaves Order API concurrency checks and event schemas unchanged.

CHANGE-070 finalizes the unexpired accepted-cancellation hold contract: Order calls `POST /api/credits/orders/{orderId}/hold-for-reopen` without a request body and waits synchronously for `200 OK` before reopening. The contract is agreed, but Credit's endpoint is still missing; trusted service authentication and failure-window reconciliation remain production requirements.


## Current time-selection and scheduler defaults (CHANGE-077 / ADR-022)

USER Requester creation/repost clock minutes remain 00/15/30/45; suggestions
round up and existing 30-minute creation expiry validation remains. API/legacy
timestamps are not rounded. ADR-026 replaces ADR-022/023 cadence with one shared
minute expiry/completion job and 15-minute all-event recovery; immediate dispatch
retained. Application/Compose/env/deployment timer configuration is synchronized.
Browser/idle Cloud Run scheduling is unverified; execution-model limits unchanged.


## CHANGE-079 / ADR-024: Central role annotations (approved 2026-10-08)

Production Firebase validation resolves User Service roles once per request, verifies response identity against JWT subject, and enforces RequireRequesterRole/RequireCourierRole/RequireAdminRole. Order-specific production role mode defaults to HTTP; local anonymous/mock behavior remains. Adapters reuse verified roles/identity, retaining fresh courier eligibility and locked domain ownership/state guards before mutation. Shared reads accept any confirmed requester/courier/admin role; /mine uses its selected mode. Internal lifecycle/scheduler authorization, API bodies, event payloads and schema remain unchanged. See ADR-024 for endpoint policy, inspected peer contracts and positive/negative test obligations.

## Scheduler DB selection and independent outbox dispatch — CHANGE-096 (2026-10-10)

The active lifecycle paths retain CHANGE-093: DB-filtered IDs, separate REQUIRES_NEW expiry/completion workers, fresh NOWAIT locks and outside-proxy catch. Outbox recovery now selects bounded eligible event IDs in SQL (due PENDING or expired IN_PROGRESS lease), then individually claims/rechecks with SKIP LOCKED. Claim, markPublished and scheduleRetry use separate REQUIRES_NEW transactions; enqueue remains REQUIRED with Order/checkpoint/receipt. Dispatcher catches each event's claim/commit/retry-write errors, logs its ID and continues later events. A failed retry write leaves the committed lease recoverable after expiry. Neither scheduler nor batch coordinator is transactional. Pub/Sub publication stays outside DB transactions and is irreversible; stable-ID deduplication remains required. Legacy claimDue is retained for compatibility, unused by the active scheduler. No cadence, schema, event body, peer, frontend or paused repost change.

## Missing supplier references in Order cards — CHANGE-097 (2026-10-10)

CHANGE-097 repairs Order UI loading when Supplier lookup successfully reports missingIds: use the existing Location unavailable card label for terminal missing references. Orders still display; no contract/peer/auth/schema/polling change. Three hook regressions (two fail first); final63 frontend tests, lint/typecheck and Docker build pass. Rebuilt only local frontend; runtime pages/assets and corrected hook verified served. Saved supplier data remains missing; authenticated browser confirmation pending.

## Swagger current-origin server — CHANGE-098 (2026-10-10)

CHANGE-098 sets an explicit relative OpenAPI server / through OpenApiConfiguration. Swagger now resolves API calls against the origin serving the specification, retaining /api/orders paths and Firebase authorization. Live staging docs previously advertised an HTTP backend host despite HTTPS gateway UI. This is an Order-only documentation routing implementation detail; no gateway, peer, CORS allowlist, forwarded-header trust, schema, frontend or deployment-env change. Tests first: one expected server-URL failure and one existing documentation pass (target/change098-red.log). Fresh source-only wrapper-selected Maven3.9.16/Java21 offline verify:253 tests,0 failures/errors/skips, including28 PostgreSQL tests; coverage96.00% lines (1441/1501),83.72% branches (468/559), unchanged >=80% gates pass. Generated target/openapi.json servers=[{url:"/",description:"Current gateway or service origin"}];160 current POM/source/test/resource files equal the fresh tested copy. Logs/reports target/change098-verify.log and target/change098-source-check/target/, evidence target/change098-evidence.json. git diff --check passes. No local application container rebuild or real authenticated browser request; no commit/push/cloud deployment. Staging remains unchanged until Order Service redeployment.

## Backend expiry minimum verified — CHANGE-099 (2026-10-10)

The user requests backend protection when frontend expiry validation is bypassed.
Existing OrderCreationService/Order.open already enforce server now + 30 minutes,
with expiry-specific HTTP 400 before Supplier/Credit/persistence. Added direct API
regression coverage, preserving the inclusive domain boundary and all repost rules.
Focused 19 tests pass; full result and fixture diagnostic recorded in CHANGE-099.
No production/frontend/peer/schema/deployment change; live integration/Sprint gates
remain unchanged.

## Main typed Credit handlers merge - 2026-10-10

Merged main a641836 provides typed Credit routes and matching CI/provisioning paths. Peer feedback now references its decoder/handlers while retaining CHANGE-092 compact payloads and FEEDBACK-009. No consumer compatibility claim. Conflict resolved/staged, all other main index entries untouched. Standard Order verify passed258 tests and coverage; ShellCheck/actionlint pass. Existing restored staging URLs match three typed routes; live recheck result recorded in Yao Xiang active work. Former shared-path repair was based on stale branch state and is superseded. Previous reverted CI helper/-U edits remain reverted.

Final live staging/production infrastructure rerun passed after a transient IAM DNS failure; all typed subscription configurations match merged main. Prior two failures did not reproduce. Merge commit/push and hosted CI remain pending; peer compact consumers remain incompatible.
