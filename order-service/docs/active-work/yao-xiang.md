# Yao Xiang - Sprint 1 Active Work

## Current request - explain authorization change scope (2026-10-08)

- Request: explain error handling and why the central role-annotation change touched many files. Advisory review only; no code change requested.
- Workflow: reloaded mandatory context, developer profile/branch, and inspected current role annotations, role provider, caller helper, adapters, security filters, handlers, tests and the 33 tracked diff paths.
- Findings: the annotations/controller placements are the wrapper itself. The broader Order-only change makes production roles come from User Service HTTP (the previous config used mock roles), validates role-context identity, reuses the same verified request principal in adapters, retains fresh courier eligibility and local mock access, and maps 401/403/503 errors. Tests and architecture/workflow documentation were updated under the Order Service process.
- Assessment: the broad scope is explained by the requested production role source and project workflow, but it is more than a wrapper-only implementation. No source changes made during this explanation.
- Verification: source/diff inspection only; no tests run this turn. Existing uncommitted implementation remains intact.


## Current request - explain separate eligibility and ownership guards (2026-10-08)

- Request: explain what preserving separate courier eligibility and locked Order ownership checks means after the annotation refactor. Explanation only; no behavior change.
- Workflow: reloaded 58 mandatory instruction/context/Sprint/decision records, developer profile/combined branch and live adapter/application/domain guards. Existing pending implementation preserved; original PDFs remain unavailable as previously recorded.
- Findings: annotations check confirmed role membership. HttpPeerAdapters still calls User Service courier-eligibility independently. Application methods lock the Order before aggregate guards compare the actor to its assigned courier or original requester and validate status/version. A valid courier role cannot authorize another courier's assigned order, and suspension can make a courier ineligible even when the role remains. Acceptance has separate OPEN/unassigned/non-self acceptance rules.
- Verification/status: inspected current source; explanation complete. No source, API, architecture, diagram, test or configuration changes; no tests/live peer calls executed this turn. Only this work record and AI disclosure updated.


## Current request - contextual role annotations (2026-10-08)

- User approved implementation of centralized User Service role verification and role annotations in our context. ADR-024 / CHANGE-079 record the detailed design before source changes.
- Scope: Order Service only plus authorized AI disclosure; local anonymous/mock behavior, HTTP courier eligibility, locked ownership guards, internal schedulers and API DTOs remain. Production Order-specific role lookup uses HTTP; shared sibling role configuration is untouched.
- Workflow: mandatory context and frontend baseline inspected; developer/profile/combined branch match. Original source PDFs remain absent; effective approved Markdown contracts apply. User role/eligibility source matches the existing contract; live peer verification remains pending.
- Implementation: three command role meta-annotations plus a shared-read role annotation; production HTTP roles once per request, response identity validation, central typed JWT caller guard, fresh courier eligibility, generic 403 and dependency 503. Traditional top-level role DTO; application/domain locked ownership guards, API/event schema and local/system behavior preserved.
- Verification: initial 7-test run had three expected failures; refined focused 37 tests pass. Full suite: 140 tests, zero failures/errors, six Docker-dependent PostgreSQL checks skipped (named-pipe access denied), 134 passed. Fresh coverage: 91.21% lines / 82.19% branches; configured 80% check passes after aligning the local check with fresh execution data. OpenAPI and diff checks pass.
- Persistent updates: ADR-024 / CHANGE-079 / ARCH-EVO-026, context/TOML, requirements/contracts/acceptance/traceability, architecture, class/sequence diagrams, README/handoff/indexes and AI usage. Root .env.example only receives an Order-specific setting/note. Frontend and sibling source/config remain unchanged.
- Status: source implementation and local non-Docker verification complete, uncommitted for review; no push/deploy requested. Live User role/eligibility calls, production admin provisioning and the six Docker checks remain unverified.


## Current request - supplied RoleAspect reference review (2026-10-08)

- Request: assess the supplied RoleAspect/SecurityHelper/BaseSecurityHelper example and recommend centralized role annotations so API methods contain application delegation instead of authentication code. Advisory-only follow-up; implementation is not requested.
- Workflow: reloaded parent/service instructions, local spec workflow, profile/allocation/combined Sprint 1 branch, mandatory context/sprint/ADR/contract records and live Order/User security source. Original PDFs remain unavailable at their referenced paths. Preserve unrelated pending work; no source/contract/profile changes are approved by this review.
- Reference finding: RoleAspect reads role metadata and checks SecurityContext through SecurityHelper; the shown role check does not call User Service. The ten-minute userInfoCache is used for display names. Reference role ordering, first-authority selection, custom-principal toString(), and anonymous local identity need adaptation to our multiple-role JWT/ownership model.
- Recommendation (PROPOSED): keep the annotation + shared security-context/helper design. Implement RequireCourierRole/RequireRequesterRole/RequireAdminRole using Spring Security @PreAuthorize meta-annotations; a handwritten RoleAspect is feasible but unnecessary because method security is already configured. Authentication validates the Firebase token first, the existing RoleProvider resolves confirmed roles, annotations check required-role membership, and API methods only bind/map/delegate. SecurityHelper/CurrentActor supplies typed JWT subject and delegated credential where needed.
- Trust boundary: User Service already exposes token-based GET /api/users/role-context; use the verified caller token and validate returned userId against JWT subject rather than trusting a supplied user ID. Reuse confirmed roles per request, retaining separate courier eligibility verification and in-transaction locked Order ownership/state guards. Courier/requester capabilities can coexist; no role ordering or automatic admin override of Order ownership is proposed.
- Refactor caveat: staging/production currently have USER_SERVICE_MODE=mock and ORDER_PEERS_MODE=http. A bare role annotation cannot replace the current HTTP action verification until authoritative role/identity and eligibility responsibilities have been aligned; do not silently switch sources or remove checks. Preserve existing local anonymous access with a mock caller identity, and keep scheduler/system authorization separate from human role annotations.
- Reference adaptation: the ten-minute cache shown is for display names; it is not evidence of safe authorization caching. Our SecurityHelper should inspect the full authority set and use Jwt.getSubject()/configured authentication name instead of first-authority selection or principal.toString().
- Verification/status: reference and live Order/User source inspected, mandatory context refreshed, official Spring Security method/JWT documentation checked, and review-record git diff --check passed. No tests/live role calls were run. Advisory review complete; only active-work and AI usage records changed. No accepted ADR, API contract, configuration, application source, peer source, or test changed.

## Current request - authorization annotations design review (2026-10-08)

- Request: explain current authentication and evaluate @RequireCourierRole/@RequireRequesterRole/@RequireAdminRole for centralizing repeated checks. Advisory/design review only; no implementation requested or approved.
- Workflow: reloaded parent/service instructions and spec workflow, local profile, allocation/current branch, current Sprint/contracts/ADRs and tracking records. Current developer is Yao Xiang on the approved combined sequences 1-11 branch; historical shared allocation remains split. Existing unrelated work is preserved. Original design PDFs remain unavailable.
- Findings: prod validates Firebase JWT and resolves authorities through RoleProvider; !prod permits anonymous requests and does not enable method security. Admin uses @PreAuthorize. HttpPeerAdapters verifies requester/courier identity against role-context, and courier verification also calls courier-eligibility. OrderAssignmentService/OrderTransitionService lock the Order before domain ownership/state guards. User Service implements both GET endpoints and grants requester/courier roles at registration. Staging/production currently set USER_SERVICE_MODE=mock but ORDER_PEERS_MODE=http, so Spring admin roles still come from the mock email list while action verification uses HTTP.
- Recommendation (PROPOSED, not approved/implemented): use Spring @PreAuthorize meta-annotations for three fixed role requirements; token validation remains in the security filter/decoder, roles in the converter/provider, and a CurrentActor helper supplies the verified JWT subject in prod. Reuse verified roles once per request rather than parse tokens/call User anew inside every annotation. Keep fresh courier eligibility distinct from role checks, and keep ownership/state validation on the locked Order inside each mutation transaction. Annotate public user-command entry points that Spring invokes through a proxy; retain a separate scheduler/system path and the local mock actor provider. Role annotations need no ID attribute.
- Refinements for a future implementation: validate the HTTP role response identity against the JWT subject; reject/remove conflicting client actor IDs in prod; review duplicated role-context calls; generalize the admin-specific denied message and map role/eligibility failures consistently. Preserve current eligibility policy until an explicit change is agreed.
- Verification: inspected actual Order/User source, existing security tests, effective profile/environment settings, and official Spring method-security/JWT/proxy documentation. No tests or live authentication calls were run.
- Status: advisory review complete; implementation remains a proposal awaiting a later user instruction. Only active-work and AI-disclosure records changed for this review.
- Boundary: retain local anonymous and production-authenticated behavior as currently approved; do not implement annotations or change role sources/peer contracts during this advisory turn.

## Current request - hourly outbox recovery poll (2026-10-08)

- Request/approval: user asked whether the outbox retry scheduler could run hourly because publish failures are expected to be rare.
- Scope: Order Service outbox scheduler cadence/configuration and its existing cadence assertion; synchronize current architecture and operating docs. No frontend, API, event schema, peer service, database, or deployment behavior change.
- Workflow: reloaded parent/service instructions, spec workflow, profile, allocation, active work, current Sprint/ADR/contracts, frontend baseline, and relevant scheduler implementation. Combined sequences 1-11 branch remains authorized by prior user direction; historical shared sequence allocation is split 1-6/7-11. Source PDFs are absent; approved Markdown records are present. Existing uncommitted frontend, CI, and Pub/Sub work preserved.
- Implementation: immediate AFTER_COMMIT dispatch remains; only recovery scan is hourly (0 0 * * * *). Expiry remains every 15 minutes; auto-completion remains every minute. Retry backoff, lease, and due-row logic unchanged.
- Design records: CHANGE-078, ADR-023, ARCH-EVO-025, current context, requirements/sequence/traceability, README/handoff, and AI usage disclosure updated.
- Trade-off: a failed publish can wait nearly an hour while the service runs; Cloud Run scale-to-zero/request-based CPU can delay recovery further while idle.
- Verification: static cadence/reference review and git diff --check; tests not run. No Docker/cloud deployment or Pub/Sub publish performed.
- Status: implementation and documentation complete, uncommitted.

## Current request - quarter-hour time inputs and scheduler cadence (2026-10-08)

- Request/approval: restrict errand creation minute choices to 00/15/30/45, run expiry every 15 minutes, event recovery every five minutes, and auto-completion every minute. User explicitly authorizes the existing shared Requester UI and Order scheduler configuration changes.
- Workflow: reloaded Order/frontend instructions and local spec skill, profile, combined Sprint 1 branch/allocation, requirements/contracts/ADRs, developer active work and live frontend. No frontend worktree overlap found; preserve existing CI/deployment changes. Referenced design PDFs remain absent.
- Implementation plan: reusable date/hour/quarter-minute control for create expiry and repost times; rounded defaults and client-side validation; existing API timestamp shape and authentication unchanged. Update expiry/outbox cron defaults and local/cloud overrides; retain immediate after-commit dispatch and DB-filtered due selection. Existing/legacy deadlines may be processed on the next 15-minute pass; auto-completion remains minute-based.
- Implementation: CHANGE-077/ADR-022/ARCH-EVO-024 deliver the shared date/hour/quarter-minute picker for creation/automatic/manual repost, rounded defaults and pre-submit guards, and synchronized expiry/outbox/auto-completion cron settings in local/cloud config. API shapes, auth, event payloads, DB operations and immediate after-commit dispatch remain unchanged.
- TDD: new frontend helpers/control failed before implementation; Spring next-run test failed at 10:02 versus required 10:15 before cron changes. Green tests cover payload conversion and manual repost invalid/future times.
- Verification: 29 frontend tests pass; route generation and TypeScript pass; full lint has only 12 existing warnings outside changed files, changed-file lint clean. Focused scheduler/outbox: 12 tests pass. Full Maven: 125 tests, zero failures/errors, six skipped Docker integration checks. Fresh coverage 90.82% lines/81.19% branches and configured gate pass. Compose config and both rendered cloud cron templates pass; git diff --check passes. Local Maven permission workaround is unchanged from CHANGE-075.
- Remaining/status: implementation complete and uncommitted; browser runtime reported no connected browser, so 320-1920px visual/authenticated walkthrough remains unverified. Cloud Run idle scheduling limits remain; no deployment or cloud publication performed. Original PDF sources and learning/ directory remain absent; no parallel learning folder was created.

## Current request - configure deployment Pub/Sub topics (2026-10-08)

- Request/approval: user asked to add the environment settings described in the preceding guidance. Scope is the three Order topic settings in production and staging deployment .env files; no cloud changes or deployment.
- Workflow: re-invoked Order Service spec-driven workflow, checked Yao Xiang's profile/combined Sprint 1 branch, existing uncommitted CI repairs, approved ADR-021, deployment rendering and frontend baseline. Original design PDFs remain absent as already recorded. No peer-owned values or frontend source will change.
- Implementation: CHANGE-076 appends the three production prod-v1 topic values and staging dev-v1 values. Existing deployment template, project, credentials, peer/database settings and production promotion workflow are unchanged.
- Verification: substituted the complete deployment YAML for both environments and parsed it; all topics match exactly with no duplicates or missing variables. Original environment settings remain intact; git diff --check passes. No Maven tests, cloud IAM validation, publish or deployment performed.
- Status: requested configuration complete, uncommitted. GCP topic-scoped publisher permission and live smoke verification remain pending; main deploys staging and production is manually promoted.

## Current request - production Pub/Sub setup guidance (2026-10-08)

- Request: user created accepted-order-cancellation-prod-v1, open-order-refund-prod-v1, and order-completion-prod-v1 and asks how deployment will publish after merging to main; no deployment or GCP mutation requested.
- Workflow: reloaded Order instructions/spec-driven skill, profile/allocation/current branch, sprint/architecture/contracts/ADR-021 and tracking records. Yao Xiang's current combined Sprint 1 branch matches the profile and prior authorization; unrelated CI repair changes remain uncommitted. Referenced original design PDFs remain absent as previously recorded.
- Findings: deploy/env.yaml already injects the three topic variables and PUBSUB_PROJECT_ID. infra/environments/production.env and staging.env omit all three topic assignments, so envsubst currently renders empty topic values. Topic configuration belongs in these environment files, not the database. The runtime identity is foc-order-service@protean-vigil-509704-q4.iam.gserviceaccount.com for both staging and production. Main triggers staging; production is manual promotion using configuration from the promoted commit.
- Guidance: add prod topic values to production.env and dev values to staging.env; grant topic-scoped Pub/Sub Publisher to the runtime identity for each intended destination. Cloud Run provides ADC without a key file. Independent Credit/User subscriptions are required for their side effects. The existing shared runtime identity means IAM alone cannot distinguish staging from production; environment topic values select the destination.
- Verification: inspected deployment workflows, env template/files, deploy script, publisher factory and project settings; checked official Google Pub/Sub IAM and Cloud Run identity docs. No runtime publish, GCP policy check, tests or deployment performed.
- Status: advisory complete. No application/configuration changes; only this work record and AI disclosure updated. Topic/IAM existence is user-reported and not independently verified.

## Current request - fix CI/CD failure (2026-10-08)

- Request: fix the failing CI checks; the user provided the Compose interpolation error and attached Maven output. Preserve unrelated work.
- Workflow: Order Service spec-driven workflow invoked before test changes; parent/service instructions, skill, developer profile, allocation, branch, sprint/contracts/decisions, active-work, and frontend baseline reviewed. Profile/current branch match Yao Xiang's approved combined Sprint 1 handoff. No peer-service or frontend changes.
- Confirmed causes: missing GOOGLE_APPLICATION_CREDENTIALS_HOST during static Compose validation; seven test failures/errors from date-dependent acceptance fixtures, Jackson 2 Instant serialization, and incomplete legacy Order event snapshots. The summary CI-passed rejection is downstream of these failures.
- Fixes: CHANGE-074 supplies an empty temporary runner path only during Compose config validation. CHANGE-075 fixes acceptance time deterministically (including the exact expiry boundary without a sleep), uses runtime-compatible Jackson 3 in event tests, and adds mandatory primitive snapshot fields in legacy routing tests. Production source/contracts/behavior and the 80% coverage threshold are unchanged.
- Verification: focused 13 tests passed; fresh full build has 123 tests, zero failures/errors, six skipped PostgreSQL Testcontainers tests due to denied Docker named-pipe access. Fresh coverage is 90.82% lines and 81.19% branches. Compose config validation passes with the placeholder path; workflow YAML parses and git diff --check passes.
- Local environment: launched cached wrapper-selected Maven 3.9.16 directly because Windows mvnw.cmd cannot start; Java 21 source/target CLI flags bypass the sandbox's denied JDK ct.sym path. Initial coverage saw obsolete compiled event classes; moved generated class output aside and reran with fresh coverage data. Neither workaround changes CI or tracked build settings.
- Status: implementation and local verification complete; changes are uncommitted. Push workflow, tests, and records to rerun hosted CI. Docker integration/image build, actionlint, and cloud infrastructure checks remain unverified here.

## Current request - use real Pub/Sub dev topics locally (2026-10-07)
- Follow-up request (2026-10-08): document the exact local `gcloud auth application-default login`, quota-project, and `GOOGLE_APPLICATION_CREDENTIALS_HOST` setup. Complete: README now provides both the persistent root `.env` entry and a one-session PowerShell form.

- Request: stop using the Pub/Sub emulator; publish locally to the actual Google Pub/Sub service using `order-completion-dev-v1`, `open-order-refund-dev-v1`, and `accepted-order-cancellation-dev-v1`. Reuse the existing GCP project; keep production topics separate; user will handle GCP resource/IAM setup and asked for instructions, not direct cloud changes.
- Workflow: reloaded parent/Order instructions, profile, shared allocation, active-work, current sprint, architecture, peer/event contracts, decision/evolution records, project context, current app/Compose config, and relevant skill. Current developer is Yao Xiang / Developer 1 / Sprint 1. Current branch is `order-service/sprint-1/yx-seq1-to-seq11`, matching the profile and this active-work record; shared allocation still lists the older `sprint-1/seq-1-to-6` branch. The earlier user-approved combined-branch handoff remains applicable. Many pre-existing uncommitted Order Service changes exist; they were preserved.
- Approval: User approved real Pub/Sub, one existing GCP project, topic-level IAM separation, the three dev topic IDs, and implementation. No GCP resources, IAM, peer service, or frontend changes are authorized or performed.
- Implementation: Replaced the root Compose emulator with real Pub/Sub and a read-only per-developer ADC mount; configured the specified dev topics/project; removed emulator channel support from the Google publisher factory; added environment-backed production topic values; documented the setup in CHANGE-073/ADR-021 and the affected workflow/context files. `ORDER_PEERS_MODE=mock` is unchanged.
- Verification: The test was changed before the factory implementation. The Maven wrapper could not start (`Cannot start maven from wrapper`), so the focused test did not execute. Docker Compose validation was unavailable because the local Docker CLI could not read `C:\Users\thamy\.docker\config.json` (access denied). Real publish verification remains pending until the GCP topics and IAM are available.
- Next: Configure Pub/Sub API/topics/topic-scoped IAM in GCP, set the local ADC path in each developer's ignored `.env`, validate Compose, run the focused/full Maven tests, then test a real dev-topic publish.

## Current request - 48-hour delivered-order auto-completion (2026-10-07)

- Request: add a Spring scheduler that finds `DELIVERED` orders whose delivered checkpoint is at least 48 hours old and completes them through the established completion/outbox flow.
- Workflow finding: `sprints/sprint-1/scope.md` explicitly listed 48-hour auto-completion as out of scope. The user's “continue doing” authorizes this feature and the proposed implementation on the current branch.
- Approved design: configurable Spring `@Scheduled` trigger; database-side selection of due `DELIVERED` orders from their delivered checkpoint with row locking; exact eligibility is `deliveredAt <= now - 48 hours`; complete each through a trusted lifecycle path sharing the existing completion checkpoint, overdue calculation, command receipt/idempotency, and `OrderCompletionTaskEvent` transactional outbox behavior. Normal requester completion remains available before the threshold. Scheduler actor is `lifecycle`; no peer-service or schema change expected.
- Branch discrepancy: profile/current branch are `order-service/sprint-1/yx-seq1-to-seq11`; shared `docs/work-allocation.md` lists `sprint-1/seq-1-to-6`. The user authorized proceeding on the current branch; Sequence 5 remains within Yao's assigned feature range. Shared allocation is left unchanged.
- Implementation: added the lifecycle cron, DB-filtered/locked due-order query, 48-hour recheck, automatic completion transition, and shared completion event/outbox path; updated scope, acceptance/traceability, sequence/class diagrams, ADR/change/evolution, current context, handoff, and usage disclosure. Tests were written before source changes. No peer/frontend changes or schema migration.
- Status: source and documentation updates complete. `git diff --check` passed. Focused Maven tests did not reach test execution: Java 21 failed during production compilation (`Cannot close compiler resources`); forked compilation also stopped before tests. PostgreSQL query/lock integration test is added but unverified. Run focused tests and `mvn verify` in CI or a working local Java environment.

## Current request - unify OPEN-order refund event (2026-10-06)

- Request: replace separate requester-cancellation and scheduled-expiration events with `OpenOrderRefundTaskEvent`, remove the expiration publisher/topic, and share the new refund topic for both outcomes.
- Workflow: Order Service spec-driven workflow rehydrated. Developer profile and active branch are Yao Xiang / Developer 1 / Sprint 1 / `order-service/sprint-1/yx-seq1-to-seq11`; this Sequence 6 change is within the shared allocation's sequences 1-6. Existing worktree contains prior uncommitted approved Order Service work, including CHANGE-065 expiry/outbox implementation; preserve it.
- Approval/classification: the user's direct instruction approves the Sequence 6 event-contract change. This supersedes the separate OPEN-expiration event in CHANGE-065/ADR-015; status transitions remain `CANCELLED` for requester cancellation and `EXPIRED` for scheduler expiry.
- Implementation: both requester cancellation and scheduled expiry create the shared full-Order refund event; one typed publisher and local topic are configured. Dispatcher tests cover the new route and conversion of pending legacy cancellation/expiry records while preserving stable event IDs. No schema migration or peer-service source change.
- Status: source and docs complete. `git diff --check` and current-contract/source consistency inspection passed. Focused Maven tests reached production compilation but Java 21 failed with `Fatal Error: Cannot close compiler resources`; forked compilation also stopped before tests. Compose validation is blocked by local Docker CLI/config access. Rerun focused/full tests and Compose validation in CI or a working local environment. Credit subscriber implementation remains future work.

## Current request - finalize accepted-cancellation hold contract (2026-10-06)

- Request: remove the `To be discussed` label for the synchronous hold-for-reopen flow; the user confirmed it as final.
- Workflow: Order Service spec workflow re-invoked. Profile is Yao Xiang, Sprint 1, branch `order-service/sprint-1/yx-seq1-to-seq11`; branch matches profile and active-work record. Shared allocation still lists initial split sequences 1-6/7-11, so this turn is limited to the user-directed shared contract documentation. Reviewed applicable approval, peer-feedback, ADR, current-sprint, traceability, and completion-report records. No application source or tests are in scope.
- Decision: FEEDBACK-003 is now `AGREED` for `POST /api/credits/orders/{orderId}/hold-for-reopen`, no body, synchronous `200 OK`, and transaction-state idempotency. Credit implementation remains missing; production authentication and reconciliation are still required.
- Records: CHANGE-070, FEEDBACK-003, ADR-014, service/current-sprint/traceability/architecture context, structured project context, and AI usage log.
- Status: Documentation update complete; no runtime/test behavior changed. Credit peer implementation remains unverified.

## Current request - keep Order version local to synchronous Credit calls (2026-10-06)

- Request: retain Order versions in events where they help consumers identify/order event facts, but do not send versions in synchronous requests from Order Service to Credit Service.
- Workflow: Order Service workflow invoked. Developer profile is Yao Xiang (Developer 1), Sprint 1, branch `order-service/sprint-1/yx-seq1-to-seq11`; the active-work handoff records combined sequences 1-11 on this branch, while the shared allocation table retains the earlier 1-6/7-11 split. User explicitly directed this shared Credit-contract update. Root/Order instructions, current sprint, context/ADRs, peer feedback, existing code/tests, and frontend baseline were rechecked. No frontend change is in scope.
- Approval: User clarified the rule: keep versions in events, omit versions from synchronous peer-service calls, and send Credit only the Order ID for hold/reset or Order ID plus courier ID for assignment. This supersedes the wider request fields previously proposed in FEEDBACK-003/004.
- Scope: Keep expected versions at Order API/domain boundaries and event versions in published payloads. Send only `orderId` and `courierId` for assignment, and only `orderId` (no request body) for hold/reset; remove redundant command/requester/amount fields from these Credit calls. Keep settlement versionless. Do not modify Credit Service or frontend.
- Provider finding: Credit's courier-assignment and hold/reset routes remain missing. Event consumers may use event `orderVersion`; no event contract change is approved.
- Test-first plan: update HTTP adapter and mock adapter expectations first; assert assignment serializes only courier ID, hold has no request body, and repeated identical transaction-state operations are safe; update acceptance/cancellation call assertions. Then update code and docs.
- Status: implemented in Order port, HTTP DTOs, mock behavior, and application calls; contract/docs updated. The focused Maven attempt before this final request-shape refinement failed before tests with javac's `Cannot close compiler resources`; the reduced contract still needs verification in a working Java 21 environment. Actual Credit API routes remain open.

## Current request - record Credit courier assignment on acceptance (2026-10-06)

- Request: before accepting an Order, synchronously ask Credit Service to associate the reservation/transaction with the acting courier; only persist `ACCEPTED` after Credit confirms.
- Workflow: rechecked Order Service instructions and spec-driven workflow, developer profile, work allocation, active work, branch, acceptance application/domain code, Credit controller/service/repository/model/API tests, contracts, and peer-feedback workflow. Yao Xiang's Developer 1 profile and approved combined sequences 1-11 branch match `order-service/sprint-1/yx-seq1-to-seq11`.
- Approval: user directly specified the synchronous interaction and then explicitly authorized an Order-side mock/contract-stub milestone to link acceptance, with the real Credit endpoint to replace it later. Do not represent the stub as a verified Credit integration.
- Finding/classification: `MISSING`. Credit's reservation record contains nullable `courierId`, but its controller/service exposes no operation to set it. Order now has an approved contract stub to call while provider work remains pending.
- Scope: Order Service port, mock/HTTP adapters, acceptance service/domain validation, tests, and synchronized stub/change docs. Do not modify Credit Service or frontend.
- Source drift/blocker: referenced Project D1 and updated overall-design PDFs remain absent. Credit assignment route is missing; trusted service authentication/idempotency/failure-recovery semantics remain open for the real provider.
- Status: Order-side port/mock/HTTP contract stub and acceptance sequencing implemented. FEEDBACK-004 remains `OPEN`; actual Credit integration is unverified. Focused Maven tests were blocked at compilation by `Fatal Error: Cannot close compiler resources`.
- Design: `OrderAssignmentService` holds/validates the Order, awaits `assignCourier`, then rechecks expiry and persists acceptance. Mock behavior enforces active reservation, exact command idempotency, assignment conflicts, settlement-to-assigned-courier, and assignment clearing on reopen hold. HTTP DTOs validate the proposed `200 OK` confirmation.
- Records: CHANGE-068, ADR-017, Sequence 3 contract diagram, service contracts, sprint acceptance criteria, architecture context, traceability, and AI usage log were updated. No Credit Service or frontend source changed.
- Remaining: rerun Maven in a functioning local compiler environment; Credit must implement/agree trusted service authentication, route/response, idempotency, and failure recovery before HTTP-peer use or FEEDBACK-004 verification.

## Current request - remove completed endpoints from peer feedback (2026-10-06)

- Request: keep the peer API feedback focused on endpoints that still need an adjustment or peer implementation; remove the completed Credit reservation endpoint.
- Workflow: rechecked Order Service instructions, profile, allocation, active work, current branch, peer-feedback document, and the implementation references. Yao Xiang's Developer 1 profile and combined sequences 1-11 authorization match `order-service/sprint-1/yx-seq1-to-seq11`.
- Scope: documentation-only cleanup in `docs/peer-service-api-feedback.md`; retain the Supplier response-handling gap and missing event consumers/Credit hold endpoint. No peer source or endpoint implementation changes.
- Source drift: referenced Project D1 and updated overall-design PDFs remain absent from this checkout; this cleanup changes no business behavior or approved contract.
- Status: complete. Removed completed User role/eligibility and Credit reservation endpoint descriptions; retained Supplier validation because Order must handle its `valid: false` response, plus missing event consumers and the proposed Credit hold endpoint. `git diff --check` passed; no peer source changed.

## Current request - remove checkpoint history from Order events (2026-10-06)

- Request: omit checkpoint history from published Order event payloads because it duplicates the order's history; update peer API feedback accordingly.
- Workflow: checked parent/Order instructions, profile, work allocation, active-work record, current sprint, relevant contracts/ADRs, and branch. Yao Xiang's Developer 1 profile and combined sequences 1-11 handoff match branch `order-service/sprint-1/yx-seq1-to-seq11`.
- Approval: user's direct request approves the contract change. Current peer consumer implementation is absent, so the new payload field set is the contract future subscribers should implement.
- Scope: Order event DTO/mapping and tests plus synchronized Order Service contracts, diagrams, evolution/change records, handoff, and AI disclosure. Preserve internal checkpoints and completion overdue calculation; no peer source or database schema change.
- Source drift: Project D1 and updated overall-design PDFs referenced by `docs/project-d1-reference.md` are absent from this checkout. Use approved Markdown decisions and the inspected current event implementation.
- Status: source and documentation updates complete; focused Maven verification blocked by the local Java compiler (`Fatal Error: Cannot close compiler resources`, also generic failure with forked compilation). `git diff --check` passed; CI verification remains pending.

## Current request - rewrite peer service API feedback (2026-10-04)

- Request: rewrite `docs/peer-service-api-feedback.md` to give peer teams concrete contracts and payload field formats, and add a clearly labeled “To be discussed” section for Credit's unexpired accepted-cancellation hold endpoint.
- Workflow: reloaded Order Service instructions, local developer profile, allocation, active work, current sprint, relevant contracts/decisions/diagrams, peer-feedback rules, and completion format. Current branch is `order-service/sprint-1/yx-seq1-to-seq11`, matching Yao Xiang's approved combined sequences 1-11 handoff.
- Source drift: Project D1 and updated overall-design PDF files referenced by `docs/project-d1-reference.md` are not present in this checkout, so their fingerprints could not be rechecked. This documentation rewrite follows the approved current Markdown amendments and the actual peer/Order DTO implementations.
- Scope: documentation only in Order Service plus the permitted AI usage disclosure. Preserve other in-progress scheduler/outbox work in the shared working tree; do not modify peer source.
- Peer inspection: confirmed Credit implements reservation and lookup HTTP routes but has no hold-for-reopen route or requested event consumers; User implements role-context/courier-eligibility HTTP routes but lacks the requested outcome event consumers; Supplier implements the validation route used by Order. Contract wording will distinguish current peer implementations from future requirements.
- Result: rewrote FEEDBACK-001 as superseded history, expanded FEEDBACK-002 with subscriber contracts and complete event fields, and added FEEDBACK-003 “To be discussed” with the hold-for-reopen request/success format and required behavior. Corrected ADR-014 and Vincent handoff references, and recorded CHANGE-066.
- Verification: inspected the current peer controllers/DTOs and Order event models/adapter; `git diff --check` passed for changed Markdown. No tests were run for this documentation-only change. Peer consumers and the Credit hold endpoint remain unverified.

- Developer: Developer 1
- Sprint: Sprint 1
- Scope: Approved combined Order Service sequences/features 1-11 handoff
- Branch: `order-service/sprint-1/yx-seq1-to-seq11`

## Current request - scheduled OrderExpirationTaskEvent (2026-10-03)

- Request and approval: the user confirmed that updated overall Sequence 6 combines requester-triggered OPEN cancellation and scheduler-triggered OPEN expiry, with different event types; Credit consumes the expiry event and refunds. The user explicitly selected Spring scheduler.
- Workflow findings: use the existing transactional outbox after-commit path; add a separate Spring `@Scheduled` scan because the existing outbox cron only recovers pending events and does not find expired orders.
- Source review: the updated overall PDF was located, but local PDF text/render tools and Python were unavailable. The user explicitly supplied the Sequence 6 relationship and event behavior; that direction was used for the synchronized Markdown sequence.
- Implementation: added `OrderExpirationTaskEvent`, mapper, publisher interface/implementation, dispatcher route, and a configurable `OrderExpiryScheduler`; expiry persists status, checkpoint, and outbox intent atomically. Added row-level pessimistic locking to the due-order query and removed the synchronous Credit `release` port and adapters.
- Documentation: synchronized updated overall Sequence 6, Sequence 9 requirements/acceptance, publisher class diagram, service/peer contracts, event decisions, ADR-015, CHANGE-065, architecture evolution, traceability, current sprint, and Vincent handoff. No Credit/User source or root Compose changes.
- Peer status: Credit expiration-event consumer remains future work and was not modified. Local emulator topic initialization does not include the new topic; set `ORDER_EXPIRATION_TOPIC` / initialize it before a local publish test.
- Operational limit: Cloud Run scale-to-zero with request-based CPU does not guarantee Spring scheduling while idle; the user selected Spring scheduling, but production runtime reliability remains unresolved and no deployment/billing changes were made.
- Verification: focused Maven test invocation is currently blocked during Java compilation by the local Windows toolchain's `Fatal Error: Cannot close compiler resources` / generic compilation failure. An attempted `mvn clean` could not resolve the uncached clean plugin because network access is denied. `git diff --check` passed; rerun Maven/CI in a working toolchain before marking verified.

## Current task - Pub/Sub publisher factory CI test (2026-10-03)

- Request: fix CI failure in `GoogleCloudPubSubPublisherFactoryTest.reusesPublisherForAProductionTopicAndClosesIt`.
- Scope: User explicitly authorized this focused test-only change on the current branch despite the shared allocation/profile mismatch; no wider sequence ownership was inferred.
- Change: the cache/lifecycle test now uses the emulator transport instead of building a production publisher that requires Google Application Default Credentials. Renamed the test to describe the behavior it actually verifies.
- Verification: `git diff --check` passed. Focused Maven test did not reach test execution: javac failed with `Cannot close compiler resources` while compiling production sources on the local Windows environment. Direct drive inspection showed 5.18 GB free on C: and 27.79 GB on D:, so disk space is not the cause. CI result after the change remains pending.

## Current task - accepted-cancellation hybrid flow (2026-10-03)

- Request: assigned courier cancellation reopens an unexpired accepted errand only after synchronous Credit hold confirmation; expired cancellation remains event-driven. Clarify Credit/User subscriber actions and document the missing Credit API.
- Workflow: Order Service hands-off/spec workflow, profile/work allocation/branch checks, peer feedback, architecture docs, and actual Credit/User APIs were inspected before coding. Yao Xiang is assigned Developer 1 on the combined sequences 1-11 branch.
- Approval: user supplied and approved the behavior. No Credit or User source is changed. Shared frontend cancellation feedback/list behavior is included in this vertical slice.
- Peer findings: Credit has reservation APIs but no hold/reset endpoint; neither peer has the requested event consumers. `FEEDBACK-003` records the proposed `POST /api/credits/orders/{orderId}/hold-for-reopen` contract. `FEEDBACK-002` now specifies each consumer action.
- Implementation: Order adds direct `ACCEPTED -> OPEN` after successful synchronous mock/HTTP hold when still unexpired; failed hold writes no transition/checkpoint/receipt/event. Expired cancellation skips hold and atomically writes `ABORTED` plus accepted-cancellation outbox event. Completion/open/expired-accepted subscriber responsibilities are documented. No schema change is planned.
- Verification: full Maven `verify` passed 107 tests with no failures/errors/skips and both JaCoCo gates passed. The focused cancellation/adapter suite passed 39 tests before a final HTTP-202 assertion; full verification includes the assertion. Frontend Vitest passed (11), typecheck passed, and ESLint passed for modified frontend files. Live Credit HTTP endpoint, Pub/Sub consumers, and browser verification remain pending external implementation/runtime.

## Feature status

- [x] CHANGE-056 - Unify completion publication; 62 tests and coverage gates passed
- [x] CHANGE-057 - Admin all-orders paginated query; implementation and completion checks passed

## Current task - CHANGE-057 Admin order listing

- Request: add an Order Service admin controller that lists all orders and optionally filters by status.
- Scope authority: User request is a specific exception to Sprint 1's explicit exclusion of Admin Service integration; no frontend or Admin Service code is requested.
- Workflow status: User explicitly approved this feature and Supplier-style admin verification; test-first implementation and completion checks passed.
- Existing API finding: User Service `GET /api/users/role-context` authenticates the Firebase bearer token and returns the token owner's `userId` and roles. The Admin Service repository currently contains only empty scaffolding.
- Approved shape: separate `AdminOrderController` at `GET /api/orders`; optional status plus one-based `Pageable`; return `OrderPageResponse`; authorize with `@PreAuthorize("hasRole('ADMIN')")` using User Service role-context; apply status/pagination through `OrderQueryService` -> `OrderRepository` -> `OrderPersistenceAdapter`/JPA.
- Authorization implementation will mirror the Supplier role provider, including the local mock provider/configured admin email behavior and verified Firebase identity; no caller-supplied roles are trusted.
- EV-3 uses synchronous query/read for the explicitly requested endpoint; no event, projection, schema migration, Admin Service, or frontend change is included.
- Peer API classification: User Service `GET /api/users/role-context` is `MATCHES_APPROVED_CONTRACT`; endpoint, bearer authentication, DTO, and provider pattern were inspected. Admin Service remains empty scaffolding and is outside scope.
- Frontend baseline: inspected routes/config/API clients and Git state; no admin UI exists and no frontend edits are requested.
- Implementation: complete in Order Service only. `AdminOrderController` uses `@PreAuthorize("hasRole('ADMIN')")`; authentication maps User Service roles into `ROLE_*`, with Supplier-style mock mode configured by `MOCK_ADMIN_EMAILS`.
- Pagination/filter: omitted status returns all statuses; supplied enum status filters exactly; one-based `Pageable`, default page 1/size 20, max size 100; newest-created first; existing `OrderPageResponse`.
- TDD evidence: the first escalated compile failed as expected because the controller did not exist yet. Focused tests then passed. Full `mvn verify` passed 86 tests, 0 failures/errors/skips, and line/branch coverage gates.
- OpenAPI structural test now includes the admin endpoint. User Service `GET /api/users/role-context` was inspected and component-contract tested with a mock HTTP server; live service-to-service runtime authentication is not verified.
- Frontend and sibling service source remained unchanged. NTH1 Admin dashboard implementation remains a separate follow-up.
- Final `git diff --check` passed. Scope review confirmed CHANGE-057 source/tests/config are confined to Order Service; the only permitted parent edit is `../ai/usage-log.md`. Existing branch changes for prior CHANGE-051-056 work remain present and untouched by this feature.

- [~] Sequence diagram/feature 1 - Create order
- [~] Sequence diagram/feature 2 - View available orders
- [~] Sequence diagram/feature 3 - Accept order
- [~] Sequence diagram/feature 4 - Start task
- [~] Sequence diagram/feature 5 - Mark item picked up
- [~] Sequence diagram/feature 6 - Mark delivered
- [~] Sequence diagram/feature 7 - Confirm completion
- [~] Sequence diagram/feature 8 - Cancel order
- [~] Sequence diagram/feature 9 - Expire order
- [~] Sequence diagram/feature 10 - Automatic repost
- [~] Sequence diagram/feature 11 - Manual repost

## Current task

Completed task: CHANGE-055 / ADR-010 applies the user's requester/courier authorization rules. Accepted cancellation is assigned-courier-only; acceptance requires an unassigned order; courier progress is assignment-bound; open cancellation and completion remain requester-owned. Full verification passed. The preceding Pub/Sub implementation (CHANGE-054) remains intact.

## Last completed action

Implemented the CHANGE-054 Google Cloud Pub/Sub producer, typed event publishers and snapshots, publish-first completion/cancellation orchestration, and accepted-cancellation endpoint. Added mapping, publisher, transition, controller, and API context tests. Full `mvn verify` passes all 57 tests and the configured 80% JaCoCo line/branch coverage checks.

## Latest task - CHANGE-055

Implemented the approved requester/courier ownership rules in the aggregate and application services. Added domain tests for pre-assigned acceptance and accepted-cancellation ownership; added an application test that proves requester-initiated accepted cancellation is rejected before publishing. Corrected updated overall Sequence 7 and synchronized ADR-010, requirements, acceptance tests, service contracts, traceability, architecture evolution, change log, and AI usage disclosure.

Verification: the initial focused authorization/event suite passed (15 tests); two further cases passed in the final full suite. Full `mvn verify` passed all 62 tests with no failures/errors/skips; JaCoCo line and branch gates passed. C: has no free space, so Maven/Surefire temp files were redirected to `D:\tmp` on D: (53 GB free). Docker container prune did not restore space; no broader Docker cleanup was performed.

## Next action

Provision/configure topic IDs and run a live emulator or GCP delivery smoke test when available. Keep peer consumer source untouched; FEEDBACK-002 remains unverified future work.

## Files modified for CHANGE-053

- `AGENTS.md`, `.codex/skills/spec-driven-development/SKILL.md`, `skills.md`
- `changes/CHANGE-053-publish-before-order-status.md`
- `changes/CHANGE-054-google-cloud-pubsub-producer.md`
- `docs/architecture-evolution.md`, `docs/change-log.md`, this active-work file
- architecture, service contracts, event decision records, traceability, sprint contracts and diagram indexes
- `sprints/sprint-1/sequence-diagrams/updated-overall/` and `sprints/sprint-1/class-diagrams/updated-overall/`
- `../ai/usage-log.md`

## Verification state

- Workflow/profile/branch/sprint context: checked; current branch matches local profile and combined handoff.
- Credit and User source inspection: completed; no matching event consumers found.
- Frontend: inspected for workflow compliance; no frontend paths are part of the change.
- Mermaid diagrams: written but not rendered.
- `git diff --check`: passed; targeted `rg` review found only explicitly historical outbox references.
- Google Pub/Sub documentation: official Java client/BOM, publisher acknowledgment, and emulator configuration inspected.
- Maven: full `verify` passed (57 tests, 0 failures/errors/skips; JaCoCo line and branch thresholds passed).
- No sequence is marked complete because implementation and completion gates remain open.
- CHANGE-055 initial focused suite passed (15 tests); final full `mvn verify` passed (62 tests, 0 failures/errors/skips; line/branch coverage gates passed) with Maven temporary files directed to `D:\tmp` because C: had no free space.

## Dependencies on Vincent

The combined-branch handoff authorizes Yao Xiang to preserve and continue sequences 7-11. Existing Vincent implementation history remains intact.

## Latest overall design update comparison - 2026-10-02

`Order Service Overall Doc - Updated.pdf` (SHA-256 `EFBA1503C7D50B5A6C1214F09ECBCD1FF453004E0A6CA3F3F9F717C78831EA40`) informs Sequences 5-8. CHANGE-051 records that comparison. CHANGE-053 records the user's publish-before-status amendment, full snapshot, topic placeholders, and assumption of future Credit/User consumers. CHANGE-056/ADR-011 supersede the separate completion-event split with one completion event carrying overdue facts.

## CHANGE-056 - Unified completion event

- User approval: Explicit request on 2026-10-02.
- Current rule: Publish one `OrderCompletionTaskEvent` for every completion. Include full Order data, `overdue`, and `overdueAt`; User and Credit are assumed to consume every completion and select their policy from those facts.
- Source: Order Service only. No peer-service or frontend changes.
- Verification: `mvn verify` passed all 62 tests and coverage gates; `git diff --check` passed and active-file scans found no overdue-only publisher or Sequence 8 diagram references. Peer consumers and live Pub/Sub delivery remain unverified.

The implementation uses Pub/Sub for completion, open cancellation, and expired accepted-cancellation outcomes and derives overdue facts from checkpoint history. The accepted-cancellation rule in this earlier status note is superseded by CHANGE-064/ADR-014: before expiry, synchronous Credit hold precedes direct `ACCEPTED -> OPEN`; after expiry, the order becomes `ABORTED` and publishes. `ABORTED -> OPEN` remains excluded under ADR-001. No schema migration is needed. Pub/Sub topic IDs remain placeholders and runtime publishing still requires valid topics/project credentials or a configured emulator.

## Blockers

- FEEDBACK-001 is superseded for outcome processing by FEEDBACK-002; current peer work is the missing Credit/User event consumers (FEEDBACK-002) and Credit hold-for-reopen endpoint (FEEDBACK-003). The Supplier pair-validation response also requires an Order-side adapter fix because HTTP 200 with `valid: false` is currently ignored.
- CHANGE-050's full-suite test execution is now verified by CHANGE-054; its API/runtime peer contracts retain their separate integration risks.
- CHANGE-054 selected Pub/Sub. Topic IDs remain placeholders and the event path fails closed until a real topic is supplied. Full snapshots include persisted Order fields and checkpoint history; overdue facts derive from accepted/delivered checkpoints, so no new schema field is needed. Pub/Sub emulator runtime availability is unconfirmed.
- FEEDBACK-002 remains OPEN because actual peer consumers are absent/unverified, but their future implementation is assumed and does not block this Order-only milestone.

## CHANGE-058 - REST client startup bean

- User reported Order Service startup failure because `SecurityConfiguration.roleProvider` could not inject `RestClient.Builder`.
- Root cause: Spring Boot 4.1.1's REST client auto-configuration was not on the application's classpath; `pom.xml` included MVC and used `RestClient` without `spring-boot-starter-restclient`.
- Added that starter and `RestClientBuilderAutoConfigurationTest`, which asserts Boot's auto-configuration supplies the builder. Existing `AdminOrderControllerSecurityTest` also starts the Spring test context successfully with the security configuration active.
- Verification: focused regression test passed; `mvn verify` passed all 87 tests with no failures/errors/skips and JaCoCo line/branch gates met. Live Docker image rebuild/startup remains for the developer to run.
- No application behavior, API contract, persistence, frontend, or peer-service source changed. Existing unrelated working-tree edits were preserved.

## CHANGE-059 - Public Swagger webjar assets

- User reported HTTP 401 for `/webjars/swagger-ui/index.html` after opening the Swagger UI workaround URL.
- Root cause confirmed in `SecurityConfiguration`: the public path allowlist exposed Order docs and swagger-ui routes, but omitted the `/webjars/**` static resource path used by Swagger UI.
- Added `/webjars/**` to the public security allowlist and an unauthenticated MockMvc regression test. This only exposes Swagger's static resources; admin/order APIs remain authenticated and role-protected.
- TDD evidence: the new test first failed with the same 401 response; after the allowlist change, all five security tests passed.
- Verification: full `mvn verify` passed all 88 tests with no failures/errors/skips and JaCoCo line/branch gates passed. `git diff --check` passed. Docker image rebuild/live container validation remains pending.

## Request creation runtime diagnosis - 2026-10-02

- Browser console showed `/api/credits/me` 404 plus `/api/orders` 400/500 responses. Recent Docker logs confirmed the Order Service's 500 was caused by Credit Service returning `ACCOUNT_NOT_FOUND` for the authenticated Firebase UID during credit reservation.
- Credit Service exposes authenticated `POST /api/credits/registration-facts`; its controller requires `request.userId` to match the bearer token, and its repository creates the initial account only when it is absent. The frontend signup flow provisions both User and Credit records, but signing in or manually creating a Firebase Auth emulator user does not run that signup flow.
- Running services are healthy. No source or peer-service files changed. The four 400 response bodies were not present in the pasted console output and remain unidentified; inspect Network response JSON if they continue after provisioning.
- Follow-up comparison confirmed the default `compose.yaml` selects `ORDER_PEERS_MODE=mock`, while `compose.http-peers.yaml` explicitly overrides it to `http`. The user confirmed the HTTP-peer profile worked previously, so that profile alone is not established as the regression cause. HTTP mode routed credit reservation to the real Credit Service, whose `ACCOUNT_NOT_FOUND` response explains the observed create failure; unfinished settlement/release operations are not on the create path. No relevant source or Compose-profile edits caused the account to be absent.

## Mock peer mode restored - 2026-10-02

- User requested that local Order Service use its mock peer adapter again.
- No adapter implementation or `compose.yaml` source change was needed: the base Compose file already sets `ORDER_PEERS_MODE=mock`; the optional `compose.http-peers.yaml` overlay selects HTTP peers.
- Recreated only the Order Service from the base Compose file, without the HTTP override: `docker compose -f compose.yaml up -d --build --force-recreate --no-deps order-service`.
- Verification: the running container reports `ORDER_PEERS_MODE=mock` and Docker reports it `healthy` on port 8083. No other service container was recreated.

## Local Pub/Sub authentication diagnosis - 2026-10-02

- The local Compose stack does not define a Pub/Sub emulator or set `PUBSUB_EMULATOR_HOST`. `GoogleCloudPubSubPublisherFactory` uses the emulator only when that property is nonblank; otherwise it creates a normal Google Cloud Pub/Sub publisher using ADC.
- Therefore `ORDER_PEERS_MODE=mock` does not stub event publication. The completion event still targets the configured Google Cloud project/topic.
- `gcloud auth application-default login` on the host can provide local user ADC, but the Order Service is running in Docker; the container cannot read host ADC unless explicitly mounted and `GOOGLE_APPLICATION_CREDENTIALS` points to it. This is suitable only for a deliberate real-cloud smoke test with a development project/topic and least-privilege publisher access.
- Recommended local development direction: run the Pub/Sub emulator and point the Order container to it. The existing Java publisher factory already configures plaintext transport and no credentials for a nonblank emulator host. Topics must be created in the emulator before publishing; the emulator does not provide production IAM validation.
- No implementation/configuration change was made in this advisory turn. Emulator integration and real-cloud smoke-test workflow remain design options, not approved implementation changes.

## CHANGE-060 - Local Pub/Sub emulator (superseded by CHANGE-073/ADR-021)

- User approved using only the emulator for local publishing while keeping the configured Google Cloud project/topic defaults for deployment.
- Root `compose.yaml` now starts Google's Cloud SDK emulator image, waits for its port, initializes all three Order event topics, and starts Order Service only after topic setup succeeds. Local Order Service gets `PUBSUB_EMULATOR_HOST=pubsub-emulator:8085`, `PUBSUB_PROJECT_ID=demo-foc`, and the three local topic IDs. `ORDER_PEERS_MODE=mock` remains unchanged.
- `application.yaml` now allows project/topic environment overrides while preserving the configured cloud project and topic IDs as defaults when local variables are absent. No Firebase, peer service, consumer, or business flow changed.
- Updated Order Service README, CHANGE-054 follow-up, CHANGE-060, change log, and AI usage disclosure.
- Verification: Compose config passed; emulator and Order Service started; topic initializer created all three topics and idempotently recognized them on a second run; emulator topic listing confirmed all topics; Order Service reports healthy and its environment points to the emulator/local project; a local test publish returned message ID `1`. Docker package build and Spring startup passed. Maven tests were unavailable because the wrapper fails in this PowerShell environment and system Maven could not create its configured local repository, including with a temporary repository override. `git diff --check` passed.
- Emulator messages are ephemeral and emulator success does not verify Google Cloud IAM or production delivery. The real-cloud defaults remain configured for deployment.

## Dual local/emulator and real-cloud test recommendation - 2026-10-02

- Recommendation only; no implementation approval or configuration change: make the Pub/Sub emulator the normal local Compose path, and provide a separate explicit cloud-smoke override. Keep the mock peer adapter choice independent from Pub/Sub transport selection.
- Emulator path: start a local Pub/Sub emulator container, set `PUBSUB_EMULATOR_HOST` inside Order Service to the emulator's Compose DNS name and port, use a non-production project ID, and initialize all configured topics in the emulator before publishing. No ADC mount is needed; this exercises serialization and publish success without sending events to cloud subscribers.
- Cloud-smoke path: explicitly unset the emulator host, set the intended development project ID, and expose host ADC to the Order container read-only through a local-only override. Use synthetic orders and topic-level `roles/pubsub.publisher`; never use a production topic for local lifecycle tests. Verify the intended transport/project at startup to prevent accidental cloud publication.
- Emulator publication success does not verify Google Cloud IAM, project/topic configuration, or real subscriptions; keep one deliberate real-cloud smoke test for those checks. Real-cloud publication can trigger any subscriptions attached to the topic.

## Accepted-task cancellation frontend review - 2026-10-03

- User request: check for a courier-facing action to cancel an accepted errand; add it if missing, available only while the order is `ACCEPTED`.
- Finding: `frontend/src/components/orders/order-actions.tsx` offers the courier “Start errand” for `ACCEPTED` but no cancellation action. The existing Order API is `POST /api/orders/{id}/cancel-accepted` with the standard `commandId`, `actorId`, and `expectedVersion` payload. Backend/domain checks restrict it to the assigned authenticated courier and current `ACCEPTED` state, then set `ABORTED` and clear `courierId` after Pub/Sub confirms publication.
- Frontend ownership: shared Next.js app; this is the existing `USER` Courier mode on `/my-errands`. Initial Git inspection found no frontend paths modified or untracked; Order Service and shared project changes already exist and were left untouched.
- Approved and implemented UI (user confirmed on 2026-10-03): add a destructive `Cancel errand` action only beside `Start errand` for `ACCEPTED` courier cards; require confirmation; call the existing cancel-accepted endpoint with the current version; on success remove the errand from the courier's list (the backend clears its assignment) and show a success toast; report API/status conflicts through the existing error toast. Keep authorization in the backend.
- Workflow status: user confirmed the missing frontend and existing API, and explicitly directed implementation with consistent UI. Approved scope is the existing accepted-cancellation behavior and its frontend link; no backend contract or peer-service change. Implementation and verification are complete as recorded in CHANGE-061. The required local Next.js guide was read after installing the locked frontend dependencies.

## Accepted-task cancellation frontend implementation - 2026-10-03

- Completed CHANGE-061 after the user confirmed the frontend was missing and approved linking the existing API with consistent UI.
- Added a destructive confirmation action beside `Start errand` only for `ACCEPTED` courier errands. The action uses the existing authenticated `useApi()` client and versioned `POST /api/orders/{id}/cancel-accepted` contract. On success, the returned `ABORTED` task is removed from the courier's list; failures retain it and use existing error feedback.
- Updated the frontend status union for `ABORTED`, Sequence 7 acceptance criteria, traceability, change log, and AI usage disclosure. No backend, API contract, sibling-service, or shared auth/permission files changed.
- TDD: new component tests failed first because the action was absent. Final verification passed `npm test` (18 tests), `npm run typecheck`, `npm run lint` (12 existing warnings outside changed files, zero errors), `npm run build` (network-enabled retry for existing Google Fonts), and `git diff --check` for changed files.
- Authenticated browser and live Pub/Sub verification remain pending. CHANGE-061 contains detailed implementation and verification evidence.

## Current task - restore local/production authentication split (2026-10-03)

- Request: restore the original profile split: local APIs require no authentication; production requires authentication, and the admin role is enforced for admin endpoints.
- Scope: Order Service security configuration and focused tests; preserve unrelated working-tree changes.
- Workflow status: hands-off, service/repository instructions, developer profile, work allocation, branch, this active-work record, CHANGE-057/059, and existing security tests inspected. The user's explicit request resolves local-versus-production behavior.
- Baseline: the committed security configuration had `@Profile("!prod")` with `permitAll()` and `@Profile("prod")` with OAuth2 bearer authentication. Current uncommitted configuration removed this split and added global authentication plus admin method security.
- Implementation: restored a `!prod` `permitAll()` security chain, a `prod` Firebase bearer chain, and production-only method security so `@PreAuthorize("hasRole('ADMIN')")` does not block local calls.
- Verification: focused Maven tests passed (10 tests; zero failures/errors/skips) for anonymous local admin-list access, production anonymous rejection, production non-admin rejection, admin pagination/filter access, Swagger webjar access, role-provider configuration, and JSON security handlers. Full `mvn verify` passed 89 tests with no failures/errors/skips and passed both JaCoCo gates. `git diff --check` passed. The initial pre-implementation test run was unavailable because Maven's default local repository was unwritable and dependency downloads were blocked in the sandbox; the escalated focused and full runs succeeded using `order-service/target/m2-repository`.
- No API contract, business behavior, diagrams/data model, persistence, Compose, frontend, or peer-service source changed. Docker/runtime profile checks have not been run.

## Current task - transactional outbox for Order outcome events (2026-10-03)

- Request: replace publish-before-status behavior with a transactional outbox. Persist each outcome event atomically with its Order transition; dispatch immediately after commit; retain a Spring cron recovery poller for pending deliveries.
- Scope: Order Service only. Do not change peer consumers, shared Compose, or deployment billing settings.
- Approval: user explicitly approved the transactional outbox and immediate-after-commit dispatch with a scheduled recovery mechanism in the preceding conversation turn.
- Workflow: invoked the Order Service hands-off/spec-driven workflow; Flyway is already selected and configured. The existing worktree contains extensive unrelated pending changes, which must be preserved.
- Implementation status: complete. V2 Flyway migration, persistent outbox model/repository, after-commit dispatcher/listener, lease-based cron recovery, transition event snapshots, and architecture records are implemented.
- Verification: `mvn -B -ntp '-Dmaven.repo.local=target/m2-repository' verify` passed 102 tests with zero failures/errors/skips; JaCoCo line and branch gates passed. PostgreSQL Testcontainers checks exercise Flyway from clean and V1 schemas, Hibernate validation, `FOR UPDATE SKIP LOCKED`, persisted publish/retry state, and atomic rollback of Order plus outbox intent. `git diff --check` passed.
- Operational note: Cloud Run currently scales to zero with request-based CPU; a Spring scheduler cannot provide continuous recovery under that setting. No cost-affecting deployment change is authorized; this will be reported as a remaining deployment decision.
- Handoff: prepared `hands-off/CHANGE-063-transactional-outbox-to-vincent.md` for Vincent and linked it from `hands-off/README.md`. It summarizes the three event flows, implementation locations, 102-test verification, consumer deduplication requirement, Cloud Run cron limitation, retained-row growth, and the uncommitted-worktree caution. Vincent's active-work record was not edited.


## Previous production authentication behavior — 2026-10-08

Reviewed the committed pre-refactor security configuration and current production environment settings to explain how authentication and role checks previously worked. Production required a valid Firebase JWT globally; Spring authorities came from the configured mock role provider in the staging/production env files, with ADMIN granted by the configured email allowlist. Separately, selected requester/courier application operations used the HTTP User Service adapter for identity, role, and courier eligibility checks. Local profile permitted requests. No source behavior changed.


## Staging/production real-peer configuration review — 2026-10-08

- Request: confirm whether staging/production automatically uses actual endpoints and reserves mocks for local testing.
- Context: parent/service instructions, Order workflow skill, profile/allocation, active Sprint 1 documents, architecture/contracts/traceability/decisions and AI disclosure context loaded. Yao Xiang, combined sequences 1–11 on the user-approved branch order-service/sprint-1/yx-seq1-to-seq11; the historical allocation mismatch is already covered by the branch-specific approval. Referenced PDF/image sources remain absent, so their fingerprints cannot be rechecked.
- Review started: tracing environment rendering, service URLs/identity and conditional adapter selection; preserving pending CHANGE-079 source/configuration work.

- Outcome: deployment automatically renders real peer URLs, sets SPRING_PROFILES_ACTIVE=prod and ORDER_PEERS_MODE=http, injects Cloud SQL configuration/password secret, and attaches foc-order-service@protean-vigil-509704-q4.iam.gserviceaccount.com. Pending CHANGE-079 application-prod.yaml defaults Order role lookup to HTTP despite the shared USER_SERVICE_MODE=mock. Staging uses dev topics and production prod topics; local ADC is not used by Cloud Run.
- Gaps: shared staging/production role mode remains mock (confirmed Supplier consumes it); mock adapters are selected by flags rather than strictly excluded by prod. Credit courier-assignment and hold-for-reopen routes and Credit/User Pub/Sub consumers remain missing from inspected source. HTTP mode does not fall back to the mock. Successful health checks do not verify these peer flows. The existing idle Cloud Run scheduling limitation remains recorded.
- Verification: inspected deploy workflows/script/templates, Order configurations/adapter conditions, Supplier role configuration, actual Credit/User routes and source searches for consumers. No live deployment, tests, cloud IAM or subscription validation was performed. Source artifacts remain unavailable for fingerprint checks. No application/infrastructure behavior changed; only this record and the AI usage disclosure were updated.


## CHANGE-080: Remove retired local Mongo helper after merge — 2026-10-08

- Request: fix CI Compose validation after accepting main's User Service MongoDB configuration. The user authorized this shared Compose repair and requires preserving peer-owned configuration.
- Context: parent/service instructions, skill, profile/allocation, active sprint/decisions/contracts and current source loaded. Yao Xiang on order-service/sprint-1/yx-seq1-to-seq11 under the recorded combined-sequence approval. Original reference PDFs remain absent; no new business/schema/contract/architecture decision is required.
- Cause: main's mongodb service and UserServiceDB connection are retained, but the obsolete user-mongodb service still mounts undeclared user-mongodb-data. CI fails during Compose validation, after creating its empty ADC placeholder.
- Status: in progress; remove the obsolete helper only, preserve main's peer blocks and Order's PostgreSQL/PubSub configuration, then validate base and HTTP-peer Compose configurations.

- Completed: removed only the retired user-mongodb helper from compose.yaml. Main's mongodb and user-service blocks exactly match origin/main; Order Service/PostgreSQL blocks and all remaining volume declarations are preserved.
- Verification: original missing-volume failure reproduced (exit 1); base and HTTP-peer Compose config checks pass (exit 0) with an empty temporary ADC fixture. Normalized config has nine services, three declared named volumes, and no dangling mounts. No containers or cloud calls started; no source/schema/frontend change. Local Docker configuration read-warning did not prevent validation; full hosted CI remains pending.
- Persistent records: CHANGE-080, current-sprint, change-log and AI disclosure updated. Original reference PDFs remain unavailable; this cleanup changes no approved business/architecture behavior.


## CHANGE-092: Compact Order event payloads — 2026-10-09

Yao Xiang confirmed ownership on order-service/sprint-1/yx-sprint-2-and-3 and explicitly approved the seven-field Order-only contract despite Credit incompatibility. ADR-031/CHANGE-092 define exact fields, unchanged accepted event, internal metadata, legacy pending-row conversion and test plan. Source fingerprints match (including Updated PDF now available under ../). FEEDBACK-009 blocks live Credit/User integration. Implementation/testing in progress; no sibling/cloud/schema/frontend writes authorized.

- Completed Order implementation: exact seven-key compact refund/completion, JsonIgnore operational metadata, explicit MapStruct fields, pending saved-snapshot conversion and unchanged accepted envelope. Final focused 32/32 pass; fresh wrapper-selected offline verify 212 total, 195 passed, 17 Docker-dependent skips, 0 failures/errors. Coverage 91.33% lines / 81.77% branches; original  >=80% gates passed. Exact source evidence and requirement/test table in CHANGE-092. No schema, scheduler, peer/frontend/config/cloud change.
- Remaining blocker: FEEDBACK-009, Credit needs compact parsing and Credit-owned transaction lookup; User needs an agreed authoritative overdue source before completion penalties. Do not report integrated flows complete. Next: re-read changed provider schemas/tests and verify isolated refund/transfer/penalty/dedup behavior; run 17 PostgreSQL tests once Docker is available. Existing Credit/User files and all cloud state unchanged. Sprint [~], no commit/push/deploy performed.

## Accepted-cancellation event count review — 2026-10-09

- Request: explain the number of events created by cancelAccepted; advisory/source review only. Current Yao Xiang profile and branch match. Order context rehydrated; no code or contract change requested.
- Verified source: successful reset followed by OPEN queues one AcceptedOrderCancellationTaskEvent (User penalty); EXPIRED queues that event plus OpenOrderRefundTaskEvent (Credit refund). Expiry is checked after synchronous Credit reset. Failed validation/reset/transaction creates no committed new events; command-receipt replay returns before enqueue. AFTER_COMMIT listener dispatches each durable intent; broker delivery remains at least once.
- Inspected current OrderTransitionService, OrderOutboxAfterCommitListener and OrderAbortTransitionTest source. No tests executed this turn, no runtime broker confirmation. Count explanation complete; existing FEEDBACK-009 integration blocker remains. Only this active-work entry and AI disclosure updated; pending implementation untouched.

## CHANGE-093: Scheduler per-order failure isolation — 2026-10-09

User explicitly requests skip failed scheduled orders and continue successes. Branch/profile match; mandatory context and frontend baseline reloaded, D1/Overall/Updated hashes match. Existing whole-pass catch is insufficient due to shared batch transactions. CHANGE-093 records ID-only DB selection, separate per-item transaction/lock/rechecks and outside-transaction catch. Implementation/testing in progress; preserve CHANGE-092 and paused repost work; no peer/frontend/schema/config change.

- Order-side implementation and deterministic verification completed: per-item REQUIRES_NEW worker/proxy, NOWAIT lock/recheck, DB-only eligible ID selection; outside catch skips failures and counts only committed transitions. Tests-first 2 expected failures; final focused 44 pass; fresh verify 232 total / 211 passed / 21 Docker skips / 0 failures/errors, BUILD SUCCESS; line91.44% / branch82.10%, unchanged coverage gates pass. Exact test/traceability evidence in CHANGE-093.
- Remaining: 4 new PostgreSQL isolation/locking/query tests (within 21 total skipped) require Docker; mock transaction-manager proxy evidence is not live DB verification. FEEDBACK-009 unchanged; Sprint [~], no integrated-completion claim. No peer/frontend/schema/config/cron/cloud edits. Changes remain uncommitted because existing CHANGE-092 overlaps source/doc paths; no mixed staging, push or deployment. Next: start Docker and run the isolated PostgreSQL integration suite, then review separate change concerns before staging.

## Scheduler versus expiry worker clarification — 2026-10-09

- Request: why a new scheduler was added. Verified Git diff shows OrderLifecycleScheduler unchanged; the only new application class for CHANGE-093 is OrderExpiryProcessingService, a Spring @Service with REQUIRES_NEW and no @Scheduled. Existing scheduler -> coordinator -> per-order transactional worker isolates expiry rollback/commit; separate bean enables Spring proxy interception. Same-class self-invocation would bypass the transactional proxy. No new timer or cadence change. Source-only inspection; no tests rerun. Only active-work/disclosure updated; pending implementation preserved.

## CHANGE-094: Order UI polling, abort wording and status filters — 2026-10-09

- Started on confirmed current branch; user explicitly authorizes this Order/shared-frontend scope. Mandatory context reloaded, three PDF hashes match, frontend clean/no recorded overlap. ADR-032/CHANGE-094 record optional status on existing mine API, DB-before-pagination/history-aware filtering, five-second Order-only polling and Abort errand labels. Existing layout/auth/peer/source work preserved. Tests-first verification in progress.

- Implementation/deterministic checks finished: optional mine status, DB-before-pagination content/count/history and identity guards retained;5-second Order reads, Abort errand labels, All/select/empty/pagination UI.58 frontend and22 focused backend pass; full backend 219passed/24 Docker skips,0 failures/errors, original coverage gates92.36%/82.41% pass; lint0 errors / 12 unrelated existing warnings, typecheck passes.8 actual-component isolated browser width/mode scenarios pass at 320-1920, fallback fonts; no real auth/peer guarantee.
- Remaining gates:3 new DBfilter regressions within 24skips require Docker; normal production build blocked by Google Fonts connection; FEEDBACK-009 unchanged. Exact changed uncommitted paths/test evidence in CHANGE-094. No mixed staging with existing CHANGE-092/093, no push/deployment; Sprint [~]. Next: run isolated PostgreSQL filters and normal network-enabled frontend build, then real authenticated personal-list/browser checks.

## CHANGE-095: Correct personal status options — 2026-10-09

- Started: explicit user correction of requester/courier options under CHANGE-094/ADR-032. Current Yao Xiang branch matches; existing frontend modifications are this session's pending changes. Shared frontend/current workflow and source hashes inspected; preserve existing source/peer work. No API/backend change required. Tests first in progress.

- Implemented CHANGE-095: OrderStatusFilter receives explicit mode; My Requests excludes ABORTED; My Errands excludes OPEN/EXPIRED/CANCELLED. Existing default/reset/page/poll/API semantics preserved. Two exact-option regressions failed first (6 other page cases passed); final full frontend 60 passed / zero failures/skips, lint passes with 12 unrelated existing warnings, typecheck passes. Eight isolated actual-component Edge checks verify exact options, selection/paging and no overflow at 320/768/1440/1920 for both modes.
- No backend/peer/auth/schema/event/scheduler/deployment changes or backend retesting this turn. Existing live PostgreSQL/peer/Google Fonts build gates remain; browser fixtures are not real auth/financial integration. Exact paths/results in CHANGE-095. Changes remain uncommitted with prior overlapping frontend work; no staging/push/deployment, Sprint [~].

## CHANGE-096: Scheduler database selection and per-item transaction audit — 2026-10-10

User requests DB-filtered scheduled candidates, independent transactions, rollback/skip failed items and continue successes. Current Yao Xiang branch/profile match, workflow/ADRs/frontend baseline and source hashes rechecked. CHANGE-093 already meets lifecycle requirements. Outbox retry-record failure can abort later events; approved user invariant authorizes the narrow Order-only refinement: due-ID SQL scan, individual REQUIRES_NEW claim/mark/retry, outer per-event catch. Enqueue stays in the Order transaction; Pub/Sub is irreversible and stays outside it. Docker now available; isolated PostgreSQL verification planned. Preserve pending changes and paused repost scope.

- Implemented Order-only CHANGE-096: due-ID SQL selection and per-event REQUIRES_NEW claim/mark/retry; catch includes claim/commit/retry-record failure. Lifecycle implementation already met request and remains unchanged. Tests-first regression: 1 expected failure (retry database unavailable), target/change096-red.log. Focused 43 tests passed, including eight PostgreSQL lifecycle/outbox isolation tests; fresh source-only wrapper-selected Maven 3.9.16 / Java21 offline verify: 252 tests passed, 0 failures/errors/skips, including 28 PostgreSQL tests. JaCoCo 95.99% lines / 83.72% branches; unchanged >=80% gates passed. Logs: target/change096-focused.log and target/change096-verify.log; reports target/change096-source-check/target/. Docker28.4.0; isolated PostgreSQL15 Testcontainers; new isolation suites mock all cloud publishers. No application database, peer service, real topic or cloud setting changed.
- Prior Docker skips are now cleared for the current source. FEEDBACK-009, real authenticated peer/cloud/browser verification and earlier frontend Google Fonts build limitation remain; no Sprint completion claim. No frontend retesting, CI rehearsal, production/cloud action, staging/commit/push/deploy. Four production paths, three test paths and records enumerated in CHANGE-096 remain uncommitted because dispatcher/tests/docs overlap pending CHANGE-092–095; preserve separate concerns before staging.

- Final consistency check: 159 POM/source/test/resource files exactly match the successful fresh build. Corrected three trailing blank lines in documentation; no application change after verify.

## Concurrency audit — 2026-10-10

User requests review of whether row locks prevent concurrent API overwrites. Current Yao Xiang branch/profile match; workflow/context and source hashes rechecked. Review-only, no application/contract/schema/peer edits. Existing mutations use transactional PESSIMISTIC_WRITE plus domain expectedVersion, ordinary locks wait and lifecycle locks NOWAIT. Suspected gaps: concurrent duplicate CREATE before receipt uniqueness, outbox marker/retry writes lacking locking/version/lease-owner checks, database exceptions not mapped to standard conflicts, and remote Credit changes surviving local rollback. Temporary ignored audit tests will exercise current behavior with isolated PostgreSQL and mocked peers; proposals stay unapproved.

- Audit outcome: same-row Order mutations protected by transaction/lock/version/state guards; actual two-courier race preserves one winner and rejects loser. Reproduced two concrete gaps: concurrent same-command creates invoke Credit twice with distinct IDs before local receipt uniqueness rollback; concurrent outbox retry snapshot overwrites committed PUBLISHED to PENDING/null marker. Final16 temporary/selected checks pass with no skips, but two probes assert current defects, not fixes. Initial outbox hook timeout corrected in ignored harness only. Source-only findings: ordinary API locks wait (lifecycle NOWAIT only), DAO lock/uniqueness exceptions lack explicit conflict handlers, synchronous peer success survives local rollback (existing FEEDBACK-006), receipt replay checked before lock. Detailed evidence/recommendations in CHANGE-096 follow-up.
- Next: obtain a detailed approved command-idempotency/remote-recovery and outbox claim-generation conditional-update design before source/schema/contract changes; decide API lock wait policy and error mapping. No application/committed-test/frontend/peer/config/schema edits, no full verify/CI rehearsal or production/cloud action this review; no source fix claimed. Preserve uncommitted CHANGE-092–096; no staging/commit/push/deploy.

## Command ID versus Order ID clarification — 2026-10-10

User asks why same-command creates conflict despite different order IDs. Verified CreateOrderRequest, frontend buildCreateOrderPayload/crypto.randomUUID, Order.open generated UUID, OrderCreationService receipt precheck and Credit-before-receipt ordering, CommandReceipt mapping and V1 unique(operation, command_id). Command ID identifies an action/retry; Order ID identifies the errand. Sequential same-command replay returns original Order. Concurrent same-command requests can both miss the precheck, reserve distinct generated IDs and compete on the same CREATE/command key; loser local transaction rolls back. New intentions need new command IDs; retries of the same intention should reuse one. Current frontend rebuilds payload with a fresh UUID; do not imply it automatically reuses IDs across user retries. Explanation only, no source/test/schema/contract/peer changes or tests rerun; previous race audit remains evidence. Existing concurrency fixes unimplemented.

## Publication timing verification — 2026-10-10

User asks whether event publication is at the end of transactional methods to reduce rollback after publish. Review/verification only on current Yao Xiang branch. Source distinguishes outbox enqueue/internal Spring signal from actual Pub/Sub. All outcome paths enqueue and signal within transaction; audit code follows. Listener is AFTER_COMMIT; dispatcher/network publish run after successful commit. Prepare isolated PostgreSQL probes for error after signal, no publish before commit, and failed publish preserving committed state; no application changes, real cloud/peer calls or schema/config changes.

- Publication timing outcome: Verified existing AFTER_COMMIT flow: business methods write Order/checkpoint/receipt/outbox and send only an internal Spring signal; audit code follows that signal. Actual typed publisher is invoked after successful Order commit; dispatcher then marks the outbox PUBLISHED in a separate transaction. Eight focused checks passed, 0 failures/errors/skips: three temporary PostgreSQL timing probes, four existing PostgreSQL lifecycle-isolation checks and one listener test. Probes verify error after signal rolls back without publisher invocation, publisher sees committed state in an independent transaction, and publish failure preserves CANCELLED plus durable retry intent. All peer/cloud publishers mocked; no real Pub/Sub verification. Temporary setup initially had three context errors from a multi-interface mock, corrected by mocking all peer ports; next run had one boundary-fixture failure, corrected by using expiry sixty seconds in the past rather than a PostgreSQL timestamp-rounding boundary. Final log: target/publication-timing-verified.log; ignored copied-source harness: target/publication-timing-source/. Current159 POM/source/test/resource files still equal the previously verified source; git diff --check passes. No application/committed-test/schema/config/peer/frontend change, no full verify/coverage rerun or staging/commit/push/deploy. Publication is irreversible; a failed post-publish marker can cause duplicate delivery. Previously reproduced outbox stale-worker overwrite and CREATE idempotency gaps remain unfixed.

## Post-rebuild login/preload diagnostic — 2026-10-10

User reports repeated unused-preload warning and incomplete post-login loading symptom. Current Yao Xiang profile/branch confirmed; review only, preserve overlapping changes, no shared auth/layout or peer edits. Asked for exact visible symptom and additional red console/network errors. Inspect live Docker services, login HTML/static assets, frontend auth/navigation/font/polling source and gateway error sample.

- Outcome: Docker frontend/gateway/backend services healthy at inspection. GET http://127.0.0.1:8080/login returns200; all17 distinct linked Next static assets returned200. Observed document preload is a script with as=script. No >=400 access requests found in the sampled gateway logs (last12m, up to500lines); this does not prove the browser login/API flow succeeds. Source also defines Geist/Geist Mono via next/font/google, whose default preload is documented by Next.js. Warning means early-fetched resource not used promptly, not proof it failed to download; exact warning URL unknown. Browser stale assets after rebuild are a possibility, not a confirmed root cause. Actual authenticated browser behavior not reproduced; await blank/spinner/missing-data description plus red error/failed request. No source/configuration/data changes or tests/build rerun; existing statuses unchanged. Ignored HTTP/log diagnostic target/login-loading-audit.cjs only.

## CHANGE-097: Missing supplier names block Order cards — 2026-10-10

User confirms /mine200 and supplies requester UID. Read-only PostgreSQL: five owned Orders (2OPEN,2COMPLETED,1DELIVERED); seven unique referenced supplier IDs, five return404 and two200 from the configured Firestore emulator. Supplier lookup returns terminal missingIds; useSupplierNames ignores those and keeps ready=false, hiding cards. Narrow Order UI bug fix within approved existing Location unavailable fallback: treat missingIds as resolved unavailable labels, preserve retrieved names, no peer/contract/auth/schema/configuration changes. TDD targeted hook regressions before modification; existing shared frontend edits preserved.

- CHANGE-097 outcome: CHANGE-097 repairs Order UI loading when Supplier lookup successfully reports missingIds: use the existing Location unavailable card label for terminal missing references. Orders still display; no contract/peer/auth/schema/polling change. Three hook regressions (two fail first); final63 frontend tests, lint/typecheck and Docker build pass. Rebuilt only local frontend; runtime pages/assets and corrected hook verified served. Saved supplier data remains missing; authenticated browser confirmation pending. Exact artifacts/results and limitations in changes/CHANGE-097-missing-supplier-order-cards.md. No backend/peer/config/data changes, staging/commit/push/cloud action. Earlier unauthenticated probe503 is not the reported authenticated200; optional Firebase diagnostic405 unavailable, temporary JSON parser corrected. No account identifiers logged in shared records.

## CHANGE-098: Staging Swagger advertises wrong server origin — 2026-10-10

User reports network/CORS failure and provides gateway-staging Swagger URL. Public live HTML/docs/config all200. OpenAPI servers contains http://order-service-staging-374055363871.asia-southeast1.run.app while UI is HTTPS gateway. Repository gateway rewrites Host to backend and forwards internal scheme; OpenAPI has no explicit server. Narrow Order-only documentation/runtime implementation detail: explicit relative server /, preserve API paths/security/auth and gateway configuration. TDD existing MVC OpenAPI test for current-origin server; new Order config bean, fresh full backend verification planned. No cloud mutation/deploy/peer/frontend edits; preserve pending092–097.

- CHANGE-098 outcome: CHANGE-098 sets an explicit relative OpenAPI server / through OpenApiConfiguration. Swagger now resolves API calls against the origin serving the specification, retaining /api/orders paths and Firebase authorization. Live staging docs previously advertised an HTTP backend host despite HTTPS gateway UI. This is an Order-only documentation routing implementation detail; no gateway, peer, CORS allowlist, forwarded-header trust, schema, frontend or deployment-env change. Tests first: one expected server-URL failure and one existing documentation pass (target/change098-red.log). Fresh source-only wrapper-selected Maven3.9.16/Java21 offline verify:253 tests,0 failures/errors/skips, including28 PostgreSQL tests; coverage96.00% lines (1441/1501),83.72% branches (468/559), unchanged >=80% gates pass. Generated target/openapi.json servers=[{url:"/",description:"Current gateway or service origin"}];160 current POM/source/test/resource files equal the fresh tested copy. Logs/reports target/change098-verify.log and target/change098-source-check/target/, evidence target/change098-evidence.json. git diff --check passes. No local application container rebuild or real authenticated browser request; no commit/push/cloud deployment. Staging remains unchanged until Order Service redeployment. Exact artifact manifest and staging verification steps in changes/CHANGE-098-swagger-current-origin.md. One new config and existing MVC test only; frontend/peer/gateway/security code preserved. No Sprint completion or live fix claim.

## CHANGE-099: Direct API expiry minimum regression — 2026-10-10

User requests backend enforcement of expiry >= current time + 30 minutes when frontend validation is bypassed. Current Yao Xiang profile/branch agree; scope is Order-owned creation validation verification. CHANGE-091 already enforces this in Order.open using server Instant.now from OrderCreationService, before Supplier/Credit/persistence. Existing domain tests cover 1799-second rejection and inclusive 1800-second acceptance. Add direct HTTP regression using real OrderController/OrderCreationService/domain/error handler and mocked providers: reject past/current/short windows with field-specific 400 and no financial/persistence writes; retain successful valid creation. No duplicate production check or clock/contract/schema/auth/peer/frontend change. Rehydrated applicable workflow/context/decision/sprint records and frontend baseline; three PDF hashes match. Large historical command outputs were truncated; do not claim the entire historical context-completion gate. Existing FEEDBACK-009/integrated/browser/cloud gates and concurrent-create/outbox lease audit gaps remain outside this task; Sprint [~]. No artificial red test expected because production behavior already exists. Fresh copied-source verification planned; preserve all pending work.

- Finished: new OrderCreationExpiryApiTest has five direct API cases; focused19 and full258 tests pass, zero failures/errors/skips. Initial fixture failure (missing disabled-repost primitive values) diagnosed and corrected; not a production RED. Coverage96.00% lines/83.72% branches, existing gates unchanged;161 files equal tested fresh copy; git diff --check passes. Existing backend rule retained, no application logic/DTO/frontend/peer/schema/auth/deployment edits. CHANGE-099, README/contract/traceability/context/sprint/index and AI disclosure record this finding. No image rebuild, live authenticated API, commit/push/deployment or application DB reset; existing integrated gates/concurrency audit gaps unchanged. Next: review tests/docs and verify the deployed Order version if live behavior differs.

## Supplier reference mismatch clarification — 2026-10-10

User asks why five saved location references were missing despite no Supplier changes. Read-only Order diagnosis/configuration/provider inspection on confirmed Yao Xiang branch; preserve all pending work. Current Supplier log at 2026-10-10T02:49:59Z reports 21 created, 0 updated, 0 unchanged, 0 deactivated, 0 skipped. Seeder reuses existing IDs by natural key; creation uses Firestore auto-generated document IDs. Therefore this startup created a fresh catalogue instead of reusing matching supplier records. Order PostgreSQL and Firebase data are separate named volumes, so prior Order references can survive a catalogue reset/namespace/import mismatch. Emulator logs report importing an export, which does NOT establish that supplier-local's earlier documents were restored. Exact reason matching old records were unavailable remains unverified; do not claim a particular reset, user deletion, failed shutdown or named-database export bug. Normal preserved/reused catalogue seeding does not regenerate existing IDs. Default Order mock validation also checks only nonempty/distinct supplier IDs, not existence, so invalid mock-created references are a separate possible source. No Supplier source/data/configuration mutation; the earlier frontend-only rebuild did not recreate emulator/backend volumes. No tests or rebuild this advisory turn. Next: inspect export/namespace history before proposing a persistent restoration fix.

## Main merge feedback conflict and CI verification - 2026-10-10

User reverted the prior CI repair and merged main a641836. Yao Xiang identity/branch still match. Rehydrated workflow/context/ADRs/sprint and frontend baseline; PDF fingerprints match. One conflict in peer-service-api-feedback.md: removed legacy CreditOrderEventConsumer versus new typed decoder/handlers. Resolve to actual main class references, label v1 snapshot semantics historical, preserve CHANGE-092 seven-field contracts/FEEDBACK-009. Main now has event-specific push URLs in both checker and provisioner, matching deployed Credit image a641836; prior shared-URL repair was based on stale checkout and has been undone. This corrects the earlier diagnosis; Order commit86708cb did not modify the POM, CI workflow, provider routes or subscription scripts. Existing default-CLI auth invalid_grant is bypassed with existing ADC for read-only checks. No application/peer/schema/payload edits or reintroduction of reverted CI helper/diagnostic files. Verify current standard Maven command and live infrastructure, preserve other staged main changes, stage only the resolved document; do not finish the merge or push.

- Merge conflict resolved: Main merge a641836 resolves the prior stale-branch infrastructure expectation: current Credit has three typed endpoints, and both scripts/ci/check-infra.sh and infra/gcp/configure-credit-pubsub.sh use their corresponding paths. Those incoming-main scripts were preserved byte-for-byte. Resolved the sole peer-service-api-feedback.md conflict to JsonOrderEventPayloadDecoder/typed handlers with former v1 semantics labeled; retained exactly seven compact keys and OPEN FEEDBACK-009. Corrected its removed CreditOrderEventConsumer reference. Only that conflict was staged; every other existing index entry was preserved. Earlier shared-URL cloud repair used stale branch code and was reversed: all three original event-specific staging paths are restored, including completion confirmed after the interrupted turn. This supersedes the earlier claimed shared-path root cause; Order commit86708cb changed neither these scripts/routes nor the POM/workflow.
- Verification so far: Standard online wrapper-selected Maven3.9.16/Java21 -B -ntp verify, without -U/offline mode, passed 258 tests, zero failures/errors/skips, including isolated PostgreSQL Testcontainers; coverage gates pass. Current161 POM/src/resource files equal the fresh tested copy. ShellCheck0.11.0 on all CI/infra shell scripts, actionlint1.7.12 on workflows, staged/unstaged diff checks and conflict/index/source/contract checks pass. Logs/reports target/main-merge-ci-verify.log and target/main-merge-ci-source-check/target/. First live cloud run reported two IAM failures while showing a concrete Google IAM DNS NameResolutionError; no deleted account/removed IAM inference. Repeat live infrastructure result pending; all three typed subscription predicates passed during first run. Current Maven artifact resolution passes but arbitrary hosted runner/network failures cannot be guaranteed absent. Reverted CI/helper changes were not reintroduced.

- Finished merge/CI verification: Final live scripts/ci/check-infra.sh staging production rerun PASSED with all identities, databases, access, all three event-specific push subscriptions/DLQ/OIDC and buckets; production push stays intentionally disabled. The first-run two false-negative IAM checks were transient DNS failures and cleared on rerun. The prior subscription mismatch and Maven model failure did not reproduce on the merged tree. Hosted/WIF CI must still run after the user completes/pushes the merge; no arbitrary remote-network guarantee. Merge conflict remains resolved/staged, MERGE_HEAD intentionally present; no merge commit/push or application-container rebuild. FEEDBACK-009 and other peer business/semantic gaps remain open.
