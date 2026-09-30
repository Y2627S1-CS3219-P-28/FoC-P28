# Order Service Permanent Project Context

This is the canonical permanent context for `order-service`. Detailed requirements remain authoritative in their source documents; this file records stable boundaries, decisions, and navigation.

## Repository state at workflow setup

On 2026-09-23, the repository contained only empty `AGENTS.md`, `README.md`, and `Dockerfile` files. No Spring Boot source, React source, tests, PostgreSQL configuration, Docker implementation, Kubernetes manifests, CI/CD workflow, or service-local documentation existed. The approved stack is therefore a constraint for later setup, not an installed implementation.

No production code was created as part of the workflow setup.

## Source documents

The following source paths are relative to the repository root.

- Product backlog and platform NFRs: `../../../Project-D1.pdf`
- Overall Order Service FR/NTH design, diagrams, amendments, and logical contracts: `../../../Order Service Overall Doc.pdf`
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
- The architecture uses inbound contracts, application services, Order-owned domain rules, outbound ports, and Order-owned persistence. HTTPS/JSON and event transport are documented as technology options, not yet locked repository implementations.
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
- For event candidates, compare synchronous request-response, query/polling, in-process events, and durable broker events. No broker or concrete event is approved merely by appearing in an overall diagram or candidate list.
- `docs/event-candidates.md` is the persistent proposal registry for D1-supported candidates. Its `EV-1` through `EV-7` IDs are stable; architecture decisions use separate `EV-DEC-NNN` IDs. The registry does not approve an event or broker, and its Credit entry records a synchronous boundary rather than an event approval.
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

- Credit reservation and credit-related `COMPLETED`, `EXPIRED`, `CANCELLED`, and `ABORTED` processing are synchronous before the associated Order operation is finalized. The ordinary `ACCEPTED -> IN_PROGRESS`, `IN_PROGRESS -> PICKED_UP`, and `PICKED_UP -> DELIVERED` transitions do not require Credit Service unless a later approved requirement says otherwise.
- Order Service owns lifecycle/status. Credit Service owns balances, reservations, releases, transfers, settlement, deductions, and credit policy.
- Penalty policy remains in User Service. After committing a relevant Order outcome, Order Service may send approved completed, overdue, or aborted facts without deciding points, scores, or suspension. This non-blocking boundary is approved; the concrete event schema, transport, and any durable broker still require feature-level approval.
- Immediate validation, courier acceptance/concurrency, authoritative transitions, and operations needing immediate success/failure are synchronous.
- Dashboards, history, available-order queries, internal time-based lifecycle work, expiry, and reposting do not require a broker by default.

## Service ownership

- User Service owns accounts, identity, roles, courier eligibility, penalties, penalty policy, suspension, and unsuspension.
- Supplier Service owns supplier records, activation, approved campus locations, and catalogue details.
- Order Service owns errand creation, lifecycle/status, assignment, transitions, checkpoints/history, supplier references, repost relationships, order flags, and publication of order facts/outcomes.
- Credit Service owns balances, reservations, release, transfers, settlement, deductions, and credit policy.
- Admin Service independently owns monitoring, reports/cases, administrative decisions, user administration, and supplier/catalogue administration.

Nice-to-have ownership: NTH1 and NTH3 belong to Admin Service; NTH2 belongs to User Service; NTH4 belongs to Order Service; NTH5 belongs to the platform/deployment process.

## Persistent approved amendments

### OVERDUE

- `OVERDUE` is a flag, not a status.
- Evaluate it once when an order reaches completion by comparing actual delivery duration with the configured delivery time limit.
- Do not introduce continuous overdue monitoring or a scheduled overdue scanner.
- Order Service records/publishes the fact; User and Credit Services apply their own policies.
- This amendment supersedes the continuous monitoring language in Project D1 F12.1-F12.1.4.

### ABORTED reopening

- Do not implement `ABORTED` to `OPEN` reopening in Sprint 1 unless explicitly added by a later approved decision.
- Do not infer it from Project D1 F4.1.10 or F11.2.4.
- Reopening reuses an order ID; NTH4 reposting creates a distinct linked order.

### Penalty ownership

- User Service alone calculates and applies penalties.
- Order Service publishes relevant facts; Admin Service investigates and issues decisions.
- Order Service and Admin Service must not calculate penalty points.

### Courier outcome contracts

- Notify User Service through separate `acceptCourierCompleted`, `acceptCourierOverdue`, and `acceptCourierAborted` operations.
- Do not use a generic courier `outcomeType` discriminator or an open-ended `facts` property bag.
- Each operation carries `eventId`, `orderId`, `courierId`, `occurredAt`, and `orderVersion` for correlation, deduplication, and ordering.
- Completion and overdue are independent notifications because a completed order can also be overdue.
- This approved clarification supersedes the generic courier-outcome operations shown in the current overall class diagram until that source is regenerated.

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
- Carry command IDs for idempotent create/reserve/transition/outcome/expiry/repost operations.
- Include expected order version for status-changing commands; stale requests produce a conflict.
- Distinguish validation, unauthorized, not found, conflict, dependency unavailable, reservation rejection, and accepted-for-processing outcomes.
- Published facts contain event ID and order version for deduplication and ordering.
- Courier completed, overdue, and aborted notifications are distinct operations; the operation name supplies the outcome and no generic `facts` map is allowed.

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
