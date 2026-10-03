# Yao Xiang - Sprint 1 Active Work

- Developer: Developer 1
- Sprint: Sprint 1
- Scope: Approved combined Order Service sequences/features 1-11 handoff
- Branch: `order-service/sprint-1/yx-seq1-to-seq11`

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

- FEEDBACK-001 remains open for real Credit Service settlement and release operations.
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

## CHANGE-060 - Local Pub/Sub emulator

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
