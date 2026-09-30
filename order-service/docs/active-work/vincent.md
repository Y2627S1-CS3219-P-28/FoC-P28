# Vincent - Sprint 1 Active Work

- Developer: Developer 2
- Sprint: Sprint 1
- Scope: Order Service sequence diagrams/features 1-11 (explicit branch-only approval)
- Branch: `sprint-1/seq-1-to-seq-11`

## Feature status

- [~] Sequence diagram/feature 1 - Create order
- [~] Sequence diagram/feature 2 - View available orders
- [~] Sequence diagram/feature 3 - Accept order
- [~] Sequence diagram/feature 4 - Start task
- [~] Sequence diagram/feature 5 - Mark item picked up
- [~] Sequence diagram/feature 6 - Mark delivered
- [~] Sequence diagram/feature 7 - Confirm completion
- [~] Sequence diagram/feature 8 - Cancel an `OPEN` order
- [~] Sequence diagram/feature 9 - Expire an unaccepted `OPEN` order
- [~] Sequence diagram/feature 10 - Automatic reposting
- [~] Sequence diagram/feature 11 - Manual reposting

## Current task

Continue the approved combined Sprint 1 Sequences 1-11 implementation with CHANGE-032's temporary Credit outcome exception. CHANGE-018 covers local container packaging and shared-frontend vertical-slice coordination; CHANGE-019 governs shared-file boundaries and peer-preserving restoration; CHANGE-020 governs small atomic Git changes and staged verification.

The prior setup-only gate is superseded for this branch by the user's explicit
approval on 2026-09-30. CHANGE-017 authorizes the combined sequences 1-11
implementation using one `Order` aggregate, Flyway, bearer-forwarding peer
adapters, and no repost event in Sprint 1.

Implementation is active under CHANGE-017. Earlier setup-only notes below are historical context and do not gate this explicitly approved branch. The branch uses one `Order` aggregate, Flyway migrations, peer adapters with bearer forwarding, and no repost event publication in Sprint 1. Cloud SQL infrastructure was provisioned separately; application deployment and full integration verification remain pending.

## Last completed action

The local Order Service container path is verified after adding the Spring
Boot 4 Flyway auto-configuration module. A clean PostgreSQL volume applied
`V1__create_orders.sql`; readiness and OpenAPI smoke checks passed; the
database contains `orders`, `order_checkpoints`, `command_receipts`, and
`flyway_schema_history`. The Docker Maven test run passed: 5 tests run, 0
failures, 0 errors, 0 skipped. CHANGE-018 local container packaging is
committed in `ed6aab2`; the lifecycle implementation is committed in
`43c39c5`; the Flyway runtime fix is recorded under CHANGE-021.

CHANGE-022 is implemented: the shared frontend now covers sequences 1-11 with
Post Request, Browse Errands, My Errands, My Requests, lifecycle actions, and
automatic/manual repost controls. Its earlier mode-switch and post-creation
repost-editing behavior were superseded by CHANGE-029 and CHANGE-031. The Order
Service now exposes requester/courier list queries and authorizes manual repost
through the User Service port. Frontend Vitest, typecheck, lint, and
production-build gates passed historically; current changes require rerunning
those gates, while live browser and peer-contract verification remain pending.

Recorded CHANGE-017 and the approved branch-only scope, added the single-aggregate persistence/domain foundation, Flyway migration, peer-service ports/adapters, lifecycle services for sequences 1–11, REST endpoints, local/prod security profiles, and domain tests. Cloud SQL infrastructure remains provisioned; application deployment and integration verification remain pending.

## Next action

CHANGE-025 is implemented on this branch. The approved frontend now loads active Supplier
Service entries into pickup and delivery selectors, signup forwards the Firebase bearer token to
User Service and then Credit Service registration facts, and the local gateway has an explicit
localhost CORS allowlist. No sibling service directory was changed. Run the frontend checks and
rebuild the gateway/frontend when Node and Docker are available. `git diff --check` and
`docker compose config --quiet` pass; the Docker engine is unavailable for runtime checks.

Run authenticated browser verification against the local stack, then run
peer-contract and acceptance checks when User, Supplier, Credit, and Gateway
services are available. The local stack excluding the intentionally deferred
Admin Service is now runnable through the temporary MongoDB dependency in
CHANGE-023.

Frontend inspection on 2026-09-30 found the shared Next.js App Router, existing
responsive AppShell/navigation, Firebase token provider, gateway-relative
`useApi()`, and coming-soon pages for Browse Errands, My Errands, My Requests,
and Post Request. These routes were replaced by the approved CHANGE-022
implementation.

The current Order API accepts requester/actor IDs in request bodies while
production security currently checks authentication but not the
`ROLE_REQUESTER`/`ROLE_COURIER` distinction. This is recorded as an integration
decision to resolve before relying on frontend guards as anything more than UX.

On 2026-09-30 the developer approved the Requester/Courier mapping, a
client-side mode switcher, Firebase UID identity values, and a responsive
frontend scope covering sequences 1-11. Vitest and React Testing Library were
selected for frontend tests. The approved list-query and repost-authorization
changes are implemented; their live peer and browser verification remains
pending.

The earlier generic persistence/deployment decision prompt is superseded by ADR-008. The current approved branch-only task is implementation and verification; no further architecture approval is required for the recorded decisions.

The following historical decision-request paragraph is retained for audit context only and must not be treated as an active blocker after CHANGE-017 approval.

Obtain user decisions on PostgreSQL versus Firestore, Kubernetes versus Cloud Run, and—when role-sensitive frontend work is proposed—the mapping from frontend `USER` modes to actual backend authorities. Then re-inspect the live frontend and relevant peer services, prepare the detailed sequence 7 vertical-slice proposal, and stop for explicit decisions before writing implementation tests.

## Files being modified

Implementation records and source: CHANGE-017, CHANGE-021, CHANGE-022, `docs/current-sprint.md`, `docs/requirements-traceability.md`, this handoff, `pom.xml`, domain/application/adapter/API/configuration source, Flyway migration, frontend routes/components, and tests. Existing infrastructure and workflow records remain unchanged except for implementation-gate status updates.

## Verification state

`git diff --check` passed. The configured Compose images built successfully. A
clean local PostgreSQL container, Firebase emulator, and Order Service started
successfully; readiness and OpenAPI smoke checks passed, and Flyway created the
expected schema. The Docker Maven test run passed: 5 tests run, 0 failures, 0
errors, 0 skipped. The Windows Maven wrapper still fails before startup in this
environment. Full local startup excluding Admin Service is now verified: the
temporary MongoDB container, User Service, gateway, frontend, Order,
PostgreSQL, Credit, Supplier, and Firebase services are healthy. Admin Service
remains intentionally deferred.
Frontend typecheck, Vitest, lint, and production build passed. Authenticated
browser, peer-contract, acceptance, and Cloud Run checks remain pending.

## Latest frontend CI lint fix — 2026-09-30

CHANGE-047 updates `frontend/src/hooks/use-supplier-permissions.ts` to avoid
the synchronous `setPermissions(null)` effect reset rejected by
`react-hooks/set-state-in-effect`. The hook now associates fetched permissions
with the Firebase UID and exposes them only for the matching current user.
`git diff --check` passed. Node/npm are unavailable in this environment, so
frontend lint, Vitest, typecheck, and build remain pending CI or local
verification.

## Latest on-demand CI rehearsal workflow — 2026-09-30

CHANGE-048 adds an explicit-trigger-only pre-push CI rehearsal to the local
Order Service skill. It scopes verification to the Order Service backend, the
approved shared frontend vertical slice, and relevant container/configuration
checks. It does not push or mutate Git history and does not run sibling,
Cloud-IAM, or browser checks by default. No rehearsal was triggered in this
turn.

## Dependencies on Yao Xiang

Combined implementation is explicitly authorized on this branch only; do not modify Yao Xiang's assigned branch. Reconcile the shared `Order` aggregate, persistence abstractions, status-transition interfaces, test fixtures, and service ports in this branch. Both developers must follow ADR-002 for any courier-outcome port or adapter.

## Blockers

- Combined Sprint 1 scope is authorized only on `sprint-1/seq-1-to-seq-11`.
- Full implementation verification remains incomplete because peer-contract, acceptance, authenticated browser, and Cloud Run checks have not run.
- The approved frontend role/mode mapping, responsive UI, and sequence 1-11 pages are implemented under CHANGE-022; live authorization and browser verification remain pending.
- Shared-file restoration preserved all sibling service folders. CHANGE-023 now
  adds only the approved temporary root-Compose MongoDB dependency; no
  `user-service/` file was modified.
- CHANGE-020 atomic-change practice is recorded; the configured Compose images
  and the local Order/PostgreSQL/Firebase/User Service slice are verified.
- The frontend `ADMIN`/`USER` plus Requester/Courier mode model is recorded and implemented; live User Service role verification remains pending.
- CHANGE-025 supplier selection, local CORS preflight handling, and Credit registration-fact
  integration are implemented; focused tests, typecheck, lint, build, Docker, and authenticated
  browser verification are pending because this session cannot access Node/npm or the Docker engine.

CHANGE-026 fixes the observed signup failure: peer services and the gateway were both returning
`Access-Control-Allow-Origin`, so the browser rejected the duplicate value. The gateway now hides
upstream CORS headers and remains the single local browser CORS boundary. `git diff --check` and
`docker compose config --quiet` pass; rebuild and authenticated signup verification remain pending.

Pending frontend discovery: `frontend/src/components/orders/order-card.tsx` currently renders
`pickupSupplierId` and `deliverySupplierId` directly, which explains the opaque supplier codes in
the UI. The verified Supplier Service `POST /api/suppliers/lookup` contract can resolve both IDs in
one authenticated request, including inactive suppliers. No code change has been made; proposed
Display-name resolution was subsequently approved and implemented under CHANGE-027.

CHANGE-027 is now approved and implemented. Order cards batch-resolve supplier references through
Supplier Service, display supplier names/buildings, retain IDs for internal actions, and omit the
internal Order ID. No Supplier Service or Order Service backend source changed. Frontend automated
and authenticated browser verification remains pending.
- Shared implementation artifacts are being created in this combined branch rather than modifying Yao Xiang's assigned branch.
- User approved PostgreSQL/Cloud SQL, Cloud Run, one shared instance with separate `order_staging` and `order_production` databases, public IP plus the Cloud SQL Java Connector, automated backups, point-in-time recovery, deletion protection, and billing acknowledgement. Repository synchronization and live Cloud SQL infrastructure provisioning/verification are complete for the approved direction; application deployment remains pending.

## Decisions awaiting approval

- Approved infrastructure decision: PostgreSQL/Cloud SQL, Cloud Run, one shared instance, public IP plus Cloud SQL Java Connector, automated backups, point-in-time recovery, deletion protection, and billing acknowledgement. Application migration-tool and Sprint business decisions remain open.
- Live verification of the approved frontend application-role mapping against User Service authority responses.
- PostgreSQL/peer integration results and deployment readiness remain pending; the local PostgreSQL/Flyway and unit gates are complete.
- The Order Service JaCoCo 80% line/branch gate is satisfied by CHANGE-036; peer-service, authenticated-browser, full Docker-stack, and Cloud Run verification remain pending.

## Latest local-stack verification — 2026-09-30

The temporary `user-mongodb` dependency from CHANGE-023 initially conflicted
with an existing host process on port 27017. The host mapping now defaults to
27018 while User Service continues to use `user-mongodb:27017` internally.
`docker compose config --quiet` and the targeted Compose startup passed. All
non-Admin services required for the approved local Order Service/frontend slice
reported healthy; authenticated browser and live peer-contract checks remain
pending.

## Latest local frontend routing fix — 2026-09-30

The developer approved fixing direct local frontend usage after signup posted
to `localhost:3000/api/users` and received `404`. CHANGE-024 sets the local
Compose `FOC_API_BASE_URL` to `http://localhost:8080`, so the browser reaches
the gateway while the frontend remains available on port 3000. Deployed
configuration is unchanged.

## Sequence 1-11 compliance audit — 2026-09-30

Static audit of branch `sprint-1/seq-1-to-seq-11` found backend endpoints/services and shared
frontend routes for all eleven Sprint 1 sequences. The implementation remains `[~]`, not `[x]`:
peer-contract, acceptance, authenticated-browser, coverage, and deployment verification are not
complete. Findings requiring follow-up are: the manual repost draft and submit paths do not both
bind the actor to the authenticated User Service identity; automatic repost calls Credit Service
without a forwarded trusted lifecycle authorization; Order Service endpoints lack the required
OpenAPI operation metadata and standard error envelope; Spring `Page` responses differ from the
repository pagination convention; lifecycle expiry does not explicitly query unassigned orders;
and no current automated test proves the required concurrency, idempotency, API, or frontend route
behavior. The current developer profile/work-allocation branch still names `sprint-1/seq-7-to-11`,
while this branch is the explicitly approved combined-scope branch; that source-of-truth mismatch
is retained as a workflow blocker rather than silently rewritten.

## Latest mode-switch runtime fix — 2026-09-30

The developer reported a Base UI error #31 when opening the Requester/Courier mode menu on
`/my-errands`. Static inspection of the installed Base UI package confirmed that
`DropdownMenuLabel` requires a surrounding menu group. CHANGE-028 wraps the mode label and its
options in `DropdownMenuGroup` in `frontend/src/components/app-shell.tsx`. No sibling service
source or backend contract changed. Node/npm-based frontend checks and an authenticated browser
retest remain pending.

## Latest unified-dashboard and location-stability change â€” 2026-09-30

CHANGE-029 is implemented in source. The client-side Requester/Courier mode
switcher and its persistence provider were removed; requester and courier
navigation/actions are now available from the shared dashboard. Browse Errands
filters the authenticated user's own orders, while the Order aggregate continues
to reject self-acceptance. User Service adapters now return the provider-confirmed
identity and Order application services use it instead of trusting request-body
actor IDs. Order cards wait for Supplier Service lookup completion and never show
opaque supplier IDs as a transient or error fallback.

The Credit Service `GET /api/credits/me` endpoint was rechecked read-only. A
`404 ACCOUNT_NOT_FOUND` means the current user has not been provisioned by the
registration-facts flow; no peer Credit Service source was changed.

FEEDBACK-001 is open for missing Credit Service settlement/release operations
for completed, cancelled, and expired orders. This is a D1-level dependency
for Sequences 7-9, while Sprint 1 documents currently defer those operations;
the authority conflict must be resolved before those sequences can be marked
fully compliant.

## Next action after CHANGE-029

Run `git diff --check`, frontend typecheck/Vitest/lint/build, Order Service
tests with Java 21/Maven, and an authenticated local-browser check when the
required runtimes and Docker engine are available. Rebuild the local stack so
the running images include the current frontend and Order Service changes.

## Latest coverage verification — 2026-09-30

CHANGE-036 expanded the Order Service tests across domain rules, application
services, controller routes, API/error contracts, HTTP peer adapters, and the
local peer-credit integration path. A clean Java 21 Maven verification passed:
42 tests, 0 failures, 0 errors, 0 skipped; JaCoCo line coverage is 89.29%
(300/336) and branch coverage is 83.25% (174/209). The 80% line/branch gate
introduced by CHANGE-035 now passes. No sibling service or learning file was
modified. Runtime peer, browser, Docker-stack, and Cloud Run gates remain
separate and are not claimed by this test-only verification.

## Latest expiry-validation feedback change — 2026-09-30

The post-request frontend now prevents an expiry shorter than the Order domain's
30-minute minimum and reports the constraint inline and in a toast. The backend
rule remains authoritative. CHANGE-030 records the change; frontend Vitest,
typecheck, and browser verification remain pending because Node/npm are not
available in the current execution environment.

## Latest creation-time repost clarification — 2026-09-30

The developer clarified and approved CHANGE-031: automatic repost must be
selected, with all plan details, in the create-order request. An `OPEN` order
now displays that choice read-only and cannot be configured afterward. Manual
repost remains available only for an un-reposted `EXPIRED` order. The Order
create DTO, aggregate, view, frontend payload, and tests were synchronized;
the legacy configure route returns a conflict for stale clients. Runtime,
Java/Maven, and frontend Node/npm verification remain pending.

## Latest Credit outcome boundary exception — 2026-09-30

Vincent approved a temporary local Credit mock so the Order Service can exercise
completion, cancellation, expiry, and automatic-repost paths while the Credit
Service owner implements the real settlement/release APIs. `CreditServicePort`
now exposes `settle` and `release`; the HTTP adapter targets the proposed
provider paths and the mock performs validation only. The lifecycle trigger
checks its internal token and forwards an explicit bearer-form credential to
automatic peer calls.

FEEDBACK-001 remains `OPEN`: the provider implementation, exact response/error
contract, trusted service credential, idempotency behavior, and cross-service
tests still require peer-owner agreement and verification. Sequences 7-9 and
the overall Sprint 1 completion gate therefore remain `[~]`; the mock is not a
production completion claim. No peer-service source or learning file changed.

The same turn added explicit `courierId IS NULL` expiry selection, standard
pagination/error envelopes, OpenAPI operation/security metadata, authenticated
manual repost drafts, and structured Order audit logging. Java/Maven, Node/npm,
Docker, browser, and coverage verification remain pending or unavailable in
this environment.

## Latest lifecycle query compile fix — 2026-09-30

Docker compilation exposed one stale call to the pre-refinement repository
method in automatic repost processing. CHANGE-033 aligned it with the explicit
`courierId IS NULL` query. `docker compose build order-service` now passes;
full stack startup and authenticated browser verification remain pending.

## Latest verification and local Credit model — 2026-09-30

CHANGE-034 replaces the validation-only local Credit mock with a deterministic
in-memory model for the approved temporary exception. It tracks 50-credit
accounts, reservations, settlement transfer, cancellation/expiry release,
insufficient-balance rejection, and command-id idempotency. The model is not a
production Credit implementation and does not write the peer ledger.

Verification completed:

- Order Service Java 21/Maven tests: 12 passed.
- Frontend Vitest/RTL: 11 passed; typecheck passed; ESLint passed with 12
  warnings and no errors.
- Credit Service pure tests: 15 passed.
- User Service tests: 2 passed.
- Order Service Docker image rebuild: passed.

Peer integration limits:

- Credit full suite: 26 tests, 11 errors from Firestore/Testcontainers because
  the isolated test runner had no Docker socket; pure tests passed.
- Supplier suite: 92 tests, 21 errors from the same Testcontainers limitation;
  one seed-file test also lacked its repository-relative CSV fixture.
- No peer source was changed. The blocked integration runs are not treated as
  verified peer contracts. Vincent will run the Docker-backed integration and
  authenticated browser checks locally.

The shared frontend production build compiled and completed TypeScript, but
the Next.js static-page generation did not complete in the isolated runner and
was interrupted after it stopped making progress. This is recorded as
unavailable verification, not a source failure.

JaCoCo is the Java code-coverage tool configured by peer Maven builds. It
measures executed lines/branches/methods; it does not prove business
correctness. Before CHANGE-035, Order Service had no JaCoCo plugin and its
coverage gate was outstanding.

## Latest Order JaCoCo setup — 2026-09-30

CHANGE-035 added JaCoCo 0.8.15 to `order-service/pom.xml`, generates reports
during `verify`, and enforces the existing 80% line-and-branch requirement.
The 12 Order tests pass, but the gate currently fails at 128/318 lines
(40.25%) and 60/209 branches (28.71%). The threshold was not weakened.
Additional Order unit/controller/integration/contract tests are required.

## Latest HTTP-peer smoke profile — 2026-09-30

CHANGE-037 implements Vincent's approved Option 2. The default
`compose.yaml` remains deterministic with `ORDER_PEERS_MODE=mock`. The new
root `compose.http-peers.yaml` override selects the real HTTP adapters and
container-network URLs for User, Supplier, and Credit Service, and waits for
their health checks before starting Order Service.

Use the override only for an authenticated Sequences 1–3 smoke run:

```bash
docker compose -f compose.yaml -f compose.http-peers.yaml up -d --build --force-recreate
```

This profile does not make settlement, release, expiry, cancellation, or
automatic-repost outcomes live because FEEDBACK-001 remains open for the
Credit Service outcome APIs. A valid Firebase-emulator bearer token and a
provisioned Credit account are required. No peer-service source or learning
file was changed, and no live Docker/browser verification is claimed yet.

## Latest HTTP-peer startup fix — 2026-09-30

The HTTP-peer Compose run failed before serving requests because Spring saw two
`HttpPeerAdapters` constructors and attempted to find a no-argument constructor.
CHANGE-038 marks the URL-based production constructor with `@Autowired`; the
package-private constructor used by adapter tests remains available.

`git diff --check` passed. Maven could not run here because the Maven
wrapper/JDK was unavailable, and Docker image verification was blocked by
inaccessible local Buildx configuration. Rebuild the HTTP-peer profile locally
before treating the smoke run as verified.

## Latest credit UI refresh diagnosis — 2026-09-30

CHANGE-039 records why a newly reserved credit may not appear immediately in
the sidebar. In default mock mode, Order Service keeps reservation state in its
in-memory `MockPeerAdapters`, while the frontend reads the separate Credit
Service account. In HTTP mode, the real Credit account is updated, but
`useCreditBalance` only refreshes on mount, browser focus, or manual Refresh;
order creation does not invalidate the shared AppShell balance.

This turn changed only the requested learning document and traceability
records. No frontend fix was implemented yet. Use the HTTP-peer override and
manual Refresh to verify the real reservation path.

## Latest credit UI invalidation guidance — 2026-09-30

CHANGE-040 documents that a broker is not needed to refresh the credit
sidebar. HTTP Order creation already waits for synchronous Credit reservation;
the stale value is a frontend cache snapshot. The approved implementation
direction is a shared credit context or lightweight browser invalidation event
that refetches `GET /api/credits/me` after successful credit-affecting
mutations. No application code was changed in this advisory turn.

## Latest credit UI invalidation implementation — 2026-09-30

CHANGE-041 implements the approved frontend-only fix. A shared browser event is
dispatched after successful order creation, manual repost, cancellation, and
completion. `useCreditBalance` listens for the event and immediately refetches
`GET /api/credits/me`; no broker or peer-service change was introduced.

Automatic repost is backend-triggered, so focus/manual refresh remains the
fallback for lifecycle changes that occur without a browser mutation. Frontend
Vitest and typecheck verification remain pending because `npm` is unavailable
in this execution environment.

## Latest Supplier lookup 401 diagnosis — 2026-09-30

CHANGE-042 records a hard-refresh authentication race. The Orders page invokes
`useSupplierNames` before the `RequireAuth` JSX guard can display its loading
state. Firebase may not yet have restored `currentUser`/the ID token, so the
lookup reaches the gateway without a valid bearer and receives 401. The CORS
headers are present; this is not a CORS failure. The frontend hook and initial
Order fetch should be gated on settled authentication before a future fix is
implemented.

## Latest Supplier lookup authentication gate — 2026-09-30

CHANGE-043 implements the approved frontend-only fix. `useApi()` now refuses
to invoke `fetch()` while Firebase auth is loading, without a current user, or
without a non-null ID token. The Errands Order fetch, Supplier lookup,
permissions, catalogue, detail, and editor effects also wait for settled
authentication, preventing hard-refresh requests from reaching the gateway
without `Authorization: Bearer ...`.

The Profile page also uses `useApi()` instead of constructing a bearer header
directly, so protected browser calls share the same fail-closed token gate.

A focused bearer-token unit test was added. `git diff --check` passed;
frontend Vitest/typecheck/lint, Docker rebuild, and authenticated hard-refresh
browser verification remain pending because this environment cannot run the
local Node/Docker/browser stack.

## Next action after CHANGE-043

Rebuild the frontend locally, hard-refresh `/errands`, and confirm the first
Order and Supplier requests contain an Authorization header and no 401 occurs.

## Latest frontend build correction — 2026-09-30

The developer's Docker build reached Next.js compilation but failed during
TypeScript checking because `api-auth.test.ts` relied on Vitest globals that
were not included in the production TypeScript type environment. CHANGE-043's
test now imports `describe`, `it`, and `expect` explicitly. `git diff --check`
passed. A local Docker rebuild and browser retest remain required; this
execution environment could not access Docker Buildx.

## Sprint 1 combined-branch handoff — 2026-09-30

Added `order-service/hands-off/README.md` and `CHANGE-044` for Yao Xiang. The
handoff records the combined `sprint-1/seq-1-to-seq-11` context, the approved
architecture and UI rules, the high-level class/service flow for each
sequence, the real HTTP-peer versus deterministic mock Compose commands, and
the open `FEEDBACK-001` Credit settlement/release dependency.

Current honest status remains `[~]`: Vincent reports sequences 1–6 exercised
against real local HTTP peers; sequences 7–11 currently rely on the local
mock for Credit outcomes. Docker/browser/peer-provider evidence still needs to
be reproduced and recorded. No sibling-service source was changed and the two
learning files remain untracked.

Next action: Yao Xiang should read the handoff, reproduce the reported smoke
tests, run the backend/frontend suites, coordinate the Credit contract with
Annablee through `FEEDBACK-001`, and update traceability before proposing any
sequence completion marker.

## Sequence 1 gateway/request-flow learning — 2026-09-30

Added the untracked learning note
`order-service/learning/seq1-order-creation-and-gateway-flow.md` and
`CHANGE-045`. It explains the actual browser CORS preflight, gateway nginx
`/api/orders` routing, Docker service-name resolution, Spring controller and
security layers, application orchestration, domain `Order.open(...)`, User/
Supplier/Credit ports, PostgreSQL persistence, response propagation, and how
Compose `ORDER_PEERS_MODE` becomes the Spring `order.peers.mode` property.

No application or peer-service behavior changed. Runtime Docker/browser
walkthrough remains for the developer to reproduce locally.

The learning note was expanded to explain that the gateway Dockerfile provides
the local `ORDER_SERVICE_URL`, the official nginx entrypoint uses `envsubst` to
render `default.conf.template` at container startup, `set $upstream` selects
the rendered destination, and `proxy_pass $upstream` performs the forwarding.
Cloud Run injects the corresponding HTTPS URL through `gateway/deploy/env.yaml`.

## OpenAPI CI structural fix — 2026-09-30

CI reported that Order Service had no `OpenApiDocumentationTest`. Added the
MVC-slice test under `order-service/src/test/java/sg/edu/nus/foc/order/api/`.
It requests `/api/orders/v3/api-docs`, writes `target/openapi.json`, and checks
path prefix, operation summaries, and 2xx responses while mocking application
collaborators so PostgreSQL and peer containers are not required.

The structural class check and `git diff --check` pass. The Maven wrapper and
system Maven were unavailable in this environment, so Maven test execution is
pending CI or a local Java/Maven setup.

The OpenAPI test was corrected to use the Spring Boot 4.1 security
auto-configuration package names; commit `4a40c5b` records that test-only
compatibility correction.
