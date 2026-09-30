<!--
AI Assistance Disclosure:
Tool: OpenAI Codex (GPT-5), date: 2026-09-26
Mode: Documentation generation.
Scope: Generated and updated the AI usage record from the available conversation history.
Author review: I reviewed for correctness and course compliance.
-->

# AI Usage Log

This log records the AI exchanges. The team remains responsible for validating all output. Requirements prioritization, architecture, component boundaries, schemas, interfaces, and performance/security trade-offs were finalized by the team, not by the AI tool.

## Credit Service implementation assistance

- **Tool:** OpenAI Codex (GPT-5)
- **Date:** 2026-09-25
- **Mode:** PLAN mode
- **Affected locations:** `credit-service/`, `compose.yaml`, `infra/gcp/bootstrap.sh`, and the
  related root documentation changes.
- **Prompt:** This is the architecture diagram designed for our application. I have already designed the database schema. write the jpa entity classes. for this application we are using sspring boot 4.1.1 ver and java 21

For sprint 1 this is the api contract for credit service for interactions with order service:
1. reserving credits - for order creations
PUT /api/credits/orders/{orderId}/reservation
2. for reservation lookup
GET /api/credits/orders/{orderId}/reservation
For user service we need to support user creation event and allocating initial credit to the new user's account:
1. account creation
POST /api/credits/registration-facts

Credit service can have 3 states RESERVED, PAID, REFUNDED
- RESERVED = user places an order, need to reserve the credit amount
- PAID = order is completed, credit transferred to courier
- REFUNDED = order is cancelled, credit transferred back to user
Use enum / types instead of hardcode strings

i need one collection to store each user's balances - including total balance, reserved balance, userId. Then another reservations - for each order it should store a reservaton entry, including amount, requester, courier, orderid. One more to store all the credit transactions with userids, transaction type, amount - transaction type should not be hardcoded string also

- **Key response:** Initial Credit Service implementation covering authenticated registration
  allocation, idempotent order-credit reservation, Firestore persistence and ledger records,
  standardized errors, deployment configuration, documentation, and automated tests. Exact file by
  file ai usage are documented in the file headers
- **Author verification:** Reviewed the plan devised, brainstormed and edited where necessary, clarified follow up questions, and modified the plan to ensure it follows our architecture, interfaces, and schemas before implementing

## Disclosure implementation

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Documentation and comment generation.
- **Exact prompt:**

  > implement the plan

- **Key response:** Added scoped attribution comments to substantive AI-influenced files, added
  the consolidated README disclosure, created this usage log, and ran structural and automated
  verification. Header coverage and `git diff --check` passed. All 14 Docker-independent unit
  tests passed.
- **Affected locations:** AI-influenced Credit Service source, tests, documentation, build and
  deployment configuration; `README.md`; `compose.yaml`; `infra/gcp/bootstrap.sh`; and this log.
- **Author verification:** The author selected the statement that the work was reviewed and tested.

## FR-based commit organization

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Requirements mapping and Git commit organization.
- **Exact follow-up prompts:**

  > dont need the commit message bodies - just do the headings

  > keep the Refs F1.2 etc

- **Exact implementation prompt:**

  ```text
  PLEASE IMPLEMENT THIS PLAN:
  # Credit Service Commit Grouping

  Create the commits in this order. Use only the heading and `Refs` line.

  1. Scaffold:
  git commit -m "chore(credit): scaffold Spring Boot credit service"

  2. Balance storage:
  git commit -m "feat(credit): track total and reserved balances" -m "Refs: F1.2, F1.2.2"

  3. Usable-balance calculation:
  git commit -m "feat(credit): derive usable balance from account state" -m "Refs: F1.2, F1.2.1"

  4. Initial allocation:
  git commit -m "feat(credit): allocate exactly 50 credits after registration" -m "Refs: F1, F1.1, F1.1.1"

  5. Successful reservation and balance updates:
  git commit -m "feat(credit): reserve order credits and update balances" -m "Refs: F1.2.2, F2, F2.1, F2.1.2, F2.1.3"

  6. Insufficient usable balance:
  git commit -m "feat(credit): reject reservations above usable balance" -m "Refs: F2.1.1"

  7. OpenAPI verification:
  git commit -m "test(credit): enforce OpenAPI documentation completeness"

  8. Documentation and AI disclosure:
  git commit -m "docs(credit): document service usage and AI assistance"

  Staging groups: scaffold; CreditAccount balance fields; usableBalance formula; registration
  allocation and tests; successful reservation and balance updates; insufficient-credit guard and
  tests; OpenAPI verification; and the READMEs plus AI usage log. Mixed F1/F2 files must be staged
  by hunk. Do not link F1.3, F1.3.1, F1.4, F1.4.1, or F3 requirements because their outcome
  behavior is not implemented. Run ./mvnw verify, docker compose config --quiet, and git status
  after all commits.
  ```

- **Key response:** Constructed index-only F1 and F2 snapshots for mixed controller, service,
  repository, error-handler, and test files; created the approved FR-linked commits in order; and
  verified the final combined implementation.
- **Affected locations:** Git history for the Credit Service implementation, both READMEs, and this
  usage log.


## User credit balance display in sidebar

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Implementation and testing assistance.
- **Exact prompt:** implement the plan
Add GET /api/credits/me, authenticated with the Firebase ID token.
user should only be allowed to view their own credit balances for now - will be adding role access later on for admins but this is out of scope for now

credit service should be able to lookup the user's credit account using creditAccounts/{userId}.
this api should return the user's total balance, reserved balance, and usable balance computed from total balance - reserved baalnce.
in the frontend add a sidebar to the left of the screen - persistent on desktop and replaces the existing desktop header navigation, for mobile navigation remain using drawer opened from the compact top bar

At the bottom of the sidebar, display a credit summary:
- Available: {usableBalance} as the primary value.
- {reservedBalance} reserved.
- {totalBalance} total.

when a user logs in, the system should refresh and get the latest credit balances by calling the api, and user can also manually click on refresh to refresh the balance when needed
for failures show credits unavailable
- **Key response:** Added token-subject account lookup through `creditAccounts/{userId}`,
  `GET /api/credits/me`, ownership/error tests, and a responsive desktop/mobile application
  shell that loads balances after sign-in and supports focus and manual refresh.
- **Affected locations:** `credit-service/`, the frontend application shell and balance hook,
  Credit Service documentation, and this usage log.
- **Author verification:** The code was reviewed and verified that it complies wiht the intended design

## Frontend sidebar placeholder navigation

- **Tool:** OpenAI Codex (GPT-5)
- **Date:** 2026-09-28
- **Mode:** Implementation and testing assistance.
- **Exact prompt:**

  ```text
  Implement the plan

  in a new branch feat/frontend-sidebar update the sidebar to have a few more placeholder navs - dashboard, browse errands, my errands, my requests and another button below all these pages -> post request

  Add sidebar-only navigation configuration in this order:
    1. Dashboard → `/`
    2. Browse Errands → `/errands`
    3. My Errands → `/my-errands`
    4. My Requests → `/my-requests`
    5. Profile
    6. Suppliers

  apart from existing profile and suppliers, the rest will have a coming soon placeholder. dont change the current implementation for profile and suppliers page
  ```

- **Key response:** Created `feat/frontend-sidebar`, added the ordered desktop and mobile sidebar
  links, added the distinct Post Request action, and generated authenticated coming-soon pages for
  Dashboard, Browse Errands, My Errands, My Requests, and Post Request while leaving Profile and
  Suppliers unchanged.
- **Affected locations:** Frontend navigation configuration, application shell, Dashboard and new
  placeholder routes, the shared coming-soon component, and this usage log.
- **Author verification:** I have reviewed the code generated and ensured that it complies with the plan I wanted 

## 2026-09-29 — Order Service Cloud SQL and Cloud Run architecture approval

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Architecture research, design comparison, and infrastructure setup assistance.
- **Exact prompt:**

  > Setup for me, the cloud sql for order microservice.

- **Key response:** Rehydrated the Order Service workflow and compared the unresolved PostgreSQL/
  Firestore and Kubernetes/Cloud Run authorities, Cloud SQL connection options, billing impact,
  and existing GCP scripts. The developer reviewed the trade-offs and selected PostgreSQL on one
  Cloud SQL instance, Cloud Run, separate staging/production databases, public IP with the Cloud
  SQL Java Connector, automated backups, point-in-time recovery, and deletion protection.
- **Affected locations:** Order Service architecture/context records, ADR-008, CHANGE-015,
  `infra/gcp/project.env`, `infra/gcp/bootstrap.sh`, `scripts/ci/check-infra.sh`, environment
  configuration, and Order Service Cloud Run deployment configuration.
- **Architecture options considered:** Firestore versus PostgreSQL/Cloud SQL; Kubernetes versus
  Cloud Run; one shared instance versus separate instances; public connector versus private IP/VPC.
- **Developer-selected architecture:** PostgreSQL on Cloud SQL and Cloud Run with one shared
  instance and separate `order_staging`/`order_production` databases, using public IP plus the
  Cloud SQL Java Connector.
- **Reason for selection:** Aligns the Order Service relational design with the project’s current
  Cloud Run deployment while reducing student-project operational and infrastructure cost.
- **Approved by:** User, 2026-09-29.
- **Tests run:** No application tests. Infrastructure script inspection and static verification
  were performed; actual GCP provisioning remains pending an authorized authenticated session.
- **Test results:** Application tests not applicable to this architecture/setup turn.
- **Specification or API deviation:** User-approved Order Service deviation from the parent
  Firestore default; sibling services were not changed.
- **Related decision or change record:** `order-service/docs/decisions/ADR-008-order-service-cloud-sql-cloud-run.md`,
  `order-service/changes/CHANGE-015-order-service-cloud-sql-cloud-run.md`.

## 2026-09-30 — Sprint 1 sequences 1-11 implementation

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Specification review, test-first implementation assistance, and verification assistance.
- **Exact prompt:**

  > Approve this detailed architecture for implementation of sequences 1–11 on the current branch

- **Key response:** Implemented the approved branch-only Sprint 1 scope using one `Order` aggregate,
  Flyway PostgreSQL persistence, synchronous credit reservation, User/Supplier/Credit outbound
  ports and adapters with bearer-token forwarding, lifecycle expiry/reposting services, REST
  commands/queries, local and production security profiles, and domain tests. Repost event
  publication remains deferred as approved.
- **Architecture options considered:** Separate sequence ownership versus combined branch-only
  implementation; a split errand/order model versus one Order aggregate; schema generation versus
  Flyway; direct peer calls versus ports/adapters; repost event versus no event in Sprint 1.
- **Developer-selected architecture:** One Order aggregate, Flyway migrations, adapter-based
  synchronous peer calls, forwarded Firebase bearer token, and deferred repost events.
- **Reason for selection:** Matches the approved Sprint 1 diagrams, preserves service ownership,
  supports independent local databases through a shared migration, and keeps credit reservation
  authoritative before an order or repost becomes `OPEN`.
- **Approved by:** Vincent, 2026-09-30.
- **Affected locations:** `order-service/src/main/java/`, `order-service/src/main/resources/`,
  `order-service/src/test/java/`, `order-service/pom.xml`, and Order Service traceability/active
  work records.
- **Tests run:** `git diff --check` passed. Maven compilation succeeded. `mvn test` passed using
  IntelliJ's Maven executable with a temporary local repository after the Windows Maven wrapper
  failed before startup (`Cannot index into a null array`).
- **Test results:** 5 tests run, 0 failures, 0 errors, 0 skipped. PostgreSQL-backed, peer-contract,
  acceptance, and Cloud Run integration checks remain pending.
- **Specification or API deviation:** No approved architecture deviation. Repost event publication
  is intentionally deferred for Sprint 1. Concrete Order Service endpoint shapes remain implementation
  details under the approved `/api/orders` resource.
- **Related decision or change record:** `order-service/changes/CHANGE-017-sprint-1-sequences-1-11-implementation.md`.

## 2026-09-30 — Local Order Service containers and frontend vertical-slice workflow

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Workflow refinement, containerization assistance, and local orchestration assistance.
- **Exact prompt:**

  > Continue from the remaining issue, setup the associated frontend readable from mobile web client and web client as well related to the backend code generated so far, setup Dockerfile for Order Service with local PostgreSQL, and provide a command to run the whole project in Docker.

- **Key response:** Added a multi-stage Order Service Dockerfile, a local PostgreSQL 15 Compose service with Flyway-backed startup configuration, Order-only Compose wiring and local environment placeholders, and strengthened the workflow so user-visible backend capabilities require a companion shared Next.js vertical slice or an explicitly recorded backend-only decision while preserving peer-owned shared configuration.
- **Affected locations:** `order-service/Dockerfile`, `compose.yaml`, `.env.example`, `order-service/README.md`, Order Service frontend workflow and agent instructions, and CHANGE-018.
- **Tests run:** Static configuration inspection completed. Docker Compose syntax/build/health verification remains pending.
- **Test results:** No Docker command was claimed as passed in this turn. Frontend source was not modified because the required application role/mode and detailed UI behavior remain unresolved.
- **Specification or API deviation:** No approved architecture deviation. Local PostgreSQL is development-only; Cloud SQL remains the approved deployment target. No frontend behavior was invented.
- **Related decision or change record:** `order-service/changes/CHANGE-018-local-compose-and-frontend-vertical-slice.md`.

## 2026-09-30 - Order Service frontend sequences 1-11 (Order Service / shared frontend)

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Test-driven implementation assistance and verification assistance.
- **Exact prompt:**

  > Implement the approved frontend from sequence 1 to sequence 11, add authenticated requester/courier list endpoints, enable automatic repost controls when creating and viewing orders, and use Vitest and React Testing Library.

- **Key response:** Added the approved responsive Next.js vertical slice for
  requester and courier modes, including request creation, available errands,
  acceptance, courier lifecycle actions, requester completion/cancellation,
  automatic repost configuration, and manual repost review. Added Vitest and
  React Testing Library setup and Order API contract helpers. Added Order
  Service requester/courier list queries and requester authorization for repost
  configuration.
- **Architecture options considered:** Existing coming-soon pages versus a
  shared responsive vertical slice; client-only lists versus authenticated
  Order Service list queries; unprotected repost configuration versus User
  Service requester verification. The approved responsive and authenticated
  choices were implemented.
- **Developer-selected architecture:** Existing AppShell, Geist, shadcn/Base
  UI, Lucide, Firebase token forwarding, and gateway-relative `useApi()` with
  one responsive desktop/mobile web implementation.
- **Reason for selection:** Preserves the shared frontend visual baseline and
  keeps business authorization and lifecycle state in Order/User Services.
- **Approved by:** Vincent, 2026-09-30.
- **Affected locations:** `frontend/src/app/`, `frontend/src/components/orders/`,
  `frontend/src/components/providers/order-mode-provider.tsx`, frontend test
  configuration and package manifests, plus Order Service query/repost source
  and tests.
- **Tests run:** Frontend typecheck, Vitest, ESLint, Next.js production build,
  and Docker Maven Order Service tests.
- **Test results:** Frontend typecheck passed; Vitest passed with 4 tests,
  including an RTL OrderCard render test;
  ESLint passed with 12 pre-existing warnings and no errors; Next.js build
  passed; Order Service tests passed after the API changes.
- **Specification or API deviation:** None approved. Live Firebase/browser,
  peer-contract, acceptance, and Cloud Run verification remain pending.
- **Related decision or change record:** `order-service/changes/CHANGE-022-order-frontend-sequences-1-11.md`.

## 2026-09-30 — Shared-file boundary and peer-preserving restoration

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Workflow refinement, shared-file conflict review, and restoration assistance.
- **Exact prompt:**

  > Do not modify files outside Order Service. In shared files, add only Order Service code; preserve peer code and comment out incompatible peer configuration rather than removing it. Restore the last shared-file changes accordingly.

- **Key response:** Added a persistent sibling-read-only/shared-file boundary, restored the peer-owned User Service MongoDB Compose configuration, removed the non-Order local MongoDB additions, and retained only the Order Service PostgreSQL/Docker additions.
- **Affected locations:** `compose.yaml`, `.env.example`, `order-service/README.md`, Order Service workflow records, and CHANGE-019. No sibling microservice folder was modified.
- **Tests run:** `docker compose config --quiet` passed after restoration; `git diff --check` passed.
- **Test results:** Docker image/container verification remains unavailable because Docker Desktop's Linux engine is not running.
- **Specification or API deviation:** No deviation. Shared-file changes are limited to Order Service additions; peer-owned configuration was restored unchanged.
- **Related decision or change record:** `order-service/changes/CHANGE-019-shared-file-boundary.md`.

## 2026-09-30 - Small atomic Git practice and local-stack recheck

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Workflow refinement and verification assistance.
- **Exact prompt:**

  > Set up small size atomic git practice upon every file changes. Preserve peer service sections in shared files and verify that the restored Docker setup can run the project locally.

- **Key response:** Persisted a small atomic-change protocol requiring explicit-path staging,
  one coherent concern per commit, staged diff checks, the smallest applicable deterministic
  check, and commit-hash handoff. Rechecked the restored root Compose configuration and retained
  the peer User Service/MongoDB section while keeping only Order Service PostgreSQL additions in
  shared files.
- **Affected locations:** Local Order Service workflow records, `compose.yaml`, `.env.example`,
  and this AI usage log; no sibling service directory was modified.
- **Tests run:** `docker compose config --quiet` passed and Compose services were enumerated.
  `docker version` could not connect to the Docker daemon, so image builds, container startup,
  health checks, and peer MongoDB connectivity were not verified.
- **Test results:** Static Compose validation passed; runtime verification remains unavailable.
- **Specification or API deviation:** None. The local stack continues to use Order Service's
  local PostgreSQL container; peer-owned configuration remains preserved.
- **Related decision or change record:** `order-service/changes/CHANGE-020-small-atomic-change-practice.md`.

## 2026-09-30 - Order Service Flyway runtime verification

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Verification assistance and implementation assistance.
- **Exact prompt:**

  > Settle all the remaining issue according to the workflow for the development from seq-1 to seq 11.

- **Key response:** Diagnosed the clean-container startup failure where Hibernate
  could not find `command_receipts`, identified the missing Spring Boot 4
  Flyway auto-configuration module, and added `spring-boot-flyway` to the Order
  Service build. No sibling service source or peer-owned Compose section was
  changed.
- **Architecture options considered:** Keep schema validation without Flyway;
  enable Flyway through the Boot 4 auto-configuration module; or replace the
  migration workflow. The existing approved Flyway workflow was retained.
- **Developer-selected architecture:** Existing Flyway migrations with the
  Spring Boot 4 auto-configuration module enabled.
- **Reason for selection:** It preserves the approved versioned migration
  workflow and makes a clean local PostgreSQL container reproducible for both
  developers.
- **Approved by:** Existing Sprint 1 implementation approval; runtime fix is
  limited to the Order Service build.
- **Affected locations:** `order-service/pom.xml` and local Order Service
  verification/change records.
- **Tests run:** Docker image rebuild; clean PostgreSQL/Firebase/Order startup;
  readiness and OpenAPI smoke checks; database schema inspection; Docker Maven
  test run.
- **Test results:** 5 tests run, 0 failures, 0 errors, 0 skipped. Flyway
  created `orders`, `order_checkpoints`, `command_receipts`, and
  `flyway_schema_history`; readiness returned `UP`.
- **Specification or API deviation:** None. Full peer-contract, acceptance,
  frontend, and Cloud Run verification remains pending.
- **Related decision or change record:** `order-service/changes/CHANGE-021-spring-boot-flyway-runtime-fix.md`.

## 2026-09-30 - Frontend sequences 1-11 approval and API-surface review

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Architecture clarification and integration analysis.
- **Exact prompt:**

  > Implement the Order Service frontend from sequence 1 to sequence 11 and use Vitest and React Testing Library.

- **Key response:** Recorded approval for the shared responsive Next.js
  frontend scope covering sequences 1-11, Requester/Courier mode switching,
  Firebase UID identity values, and Vitest plus React Testing Library. Re-read
  the existing frontend and Order API before implementation and identified an
  unresolved API-surface gap: the Order Service has no verified requester or
  courier "my orders" list query required by the approved pages.
- **Architecture options considered:** Use only existing `/available` and
  `/{id}` queries; add authenticated requester/courier list queries; or defer
  the My Requests/My Errands pages. No option was silently selected.
- **Developer-selected architecture:** Responsive shared frontend with a
  client-side mode switcher, while the backend remains authoritative. The
  missing list-query and backend role-enforcement decisions remain pending.
- **Reason for selection:** Preserves the approved shared-web model and avoids
  inventing an unsafe client-only authorization contract.
- **Approved by:** Vincent, 2026-09-30 (frontend scope, mode mapping, UID
  identity, and test stack).
- **Affected locations:** Order Service active-work and frontend vertical-slice
  records; no frontend source or peer service source changed.
- **Tests run:** Read-only frontend/API inspection only.
- **Test results:** No implementation tests run because the API-surface gap
  must be resolved before coding.
- **Specification or API deviation:** None approved. The missing list query
  and backend role enforcement are unresolved.
- **Related decision or change record:** `order-service/changes/CHANGE-018-local-compose-and-frontend-vertical-slice.md`.

## 2026-09-30 - Temporary local MongoDB Compose verification

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Local infrastructure implementation and verification assistance.
- **Exact prompt:**

  > Do not need the Admin Service Dockerfile. Do not touch the peer User
  > Service in Compose, but you may add temporary local MongoDB support so the
  > approved Order Service/frontend stack can be tested.

- **Key response:** Added a temporary `mongo:7` Compose dependency for User
  Service without changing any file under `user-service/`. The User Service
  defaults to the internal `user-mongodb:27017` URI, while the host mapping
  defaults to `27018` after the existing `27017` port was found occupied.
  Admin Service remains intentionally excluded from this local slice.
- **Architecture options considered:** Require a peer-managed MongoDB URI;
  expose the temporary container on the conflicting default port; or add an
  isolated local MongoDB dependency with an overridable host port. The isolated
  dependency was selected for local-only verification.
- **Developer-selected architecture:** Temporary root-Compose MongoDB for
  local development only; managed/staging User Service configuration remains
  overridable and unchanged.
- **Reason for selection:** It enables the approved frontend and gateway path
  to run locally without modifying peer-owned source or forcing a production
  database decision.
- **Approved by:** Vincent, 2026-09-30.
- **Affected locations:** `compose.yaml`, local Order Service change records,
  and this AI usage log. No sibling service directory was modified.
- **Tests run:** `docker compose config --quiet`; targeted Compose startup for
  MongoDB, User Service, and gateway; Compose service health inspection; User
  Service readiness request.
- **Test results:** Compose config passed. MongoDB, User Service, gateway,
  frontend, Order, PostgreSQL, Credit, Supplier, and Firebase containers were
  healthy/running. User Service readiness returned HTTP 200. The Admin Service
  was intentionally not included.
- **Specification or API deviation:** None. This is a local infrastructure
  implementation detail; no production User Service configuration or contract
  was changed.
- **Related decision or change record:**
  `order-service/changes/CHANGE-023-local-mongodb-compose-dependency.md`.

## 2026-09-30 - Local frontend gateway API-base correction

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Configuration diagnosis and implementation assistance.
- **Exact prompt:**

  > I approve it.

- **Key response:** After a signup request returned `404` from
  `http://localhost:3000/api/users`, traced the request path and identified
  that the direct frontend port was being used as the API origin. Updated the
  local Compose frontend configuration to use the gateway origin
  `http://localhost:8080`.
- **Architecture options considered:** Require users to open only the gateway;
  configure the direct frontend container with the gateway API origin; or add a
  Next.js API proxy. The explicit local runtime configuration was selected.
- **Developer-selected architecture:** Local frontend remains available at
  port 3000, while browser API calls target the gateway at port 8080.
- **Reason for selection:** It fixes direct local frontend testing without
  adding a second API proxy or changing deployed gateway behavior.
- **Approved by:** Vincent, 2026-09-30.
- **Affected locations:** `compose.yaml`, local Order Service change records,
  and this AI usage log. No sibling service source was modified.
- **Tests run:** Compose configuration validation, frontend container
  recreation, gateway health and `/api/users` route checks.
- **Test results:** `docker compose config --quiet` passed; gateway health and
  `/api/users` read-only checks returned HTTP 200.
- **Specification or API deviation:** None. This is a local configuration fix.
- **Related decision or change record:**
  `order-service/changes/CHANGE-024-local-frontend-gateway-api-base.md`.

## 2026-09-30 - Supplier selection, local CORS, and credit registration

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Test-first implementation assistance and integration verification planning.
- **Exact prompt:**

  > Approve the local gateway CORS allowlist, supplier-name dropdowns that submit supplier IDs,
  > and a frontend Credit Service registration-fact call when the peer endpoint exists. Always
  > invoke the current Order Service workflow and modify only the Order Service scope.

- **Key response:** Rehydrated the Order Service workflow and inspected the actual Supplier and
  Credit Service contracts. Added active Supplier Service-backed pickup and delivery selectors,
  explicit local gateway CORS preflight handling, and authenticated Credit Service registration
  facts after User Service signup. No sibling service directory was changed.
- **Architecture options considered:** Manual supplier ID entry versus provider-backed selection;
  gateway CORS allowlist versus gateway-only access; deferred credit provisioning versus the
  existing authenticated registration-fact contract. The developer approved provider-backed
  selectors, the local allowlist, and the existing Credit endpoint.
- **Developer-selected architecture:** The shared Next.js frontend uses `useApi()` and stable
  supplier IDs; the gateway allows only localhost development origins; Credit Service remains the
  owner of credit account creation and idempotency.
- **Reason for selection:** It removes manual identifier entry, fixes the approved direct local
  frontend path without wildcard CORS, and uses the already implemented peer contract without
  modifying peer code.
- **Approved by:** Vincent, 2026-09-30.
- **Affected locations:** `frontend/src/app/requests/new/page.tsx`,
  `frontend/src/components/providers/auth-provider.tsx`,
  `frontend/src/lib/suppliers.ts`, `frontend/src/lib/registration.ts` and tests,
  `gateway/templates/default.conf.template`, and Order Service change/learning records.
- **Tests run:** Focused Vitest execution was attempted before implementation, but `node`/`npm`
  is not available on this Windows session. `git diff --check` passed. Docker Compose and browser
  verification were not run because the Docker engine is inaccessible.
- **Test results:** Implementation tests, typecheck, lint, production build, CORS preflight, and
  authenticated peer-contract checks remain pending until Node/npm and Docker are available.
- **Specification or API deviation:** None. This is an approved frontend integration refinement;
  Supplier and Credit ownership/contracts remain unchanged and no sibling service source changed.
- **Related decision or change record:**
  `order-service/changes/CHANGE-025-supplier-selection-cors-credit-registration.md`.

## 2026-09-30 - Duplicate local CORS header fix

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Browser-error diagnosis and shared gateway fix.
- **Exact prompt:**

  > After the frontend changes, signup and login fail; the browser reports multiple
  > `Access-Control-Allow-Origin` values.

- **Key response:** Rehydrated the Order Service workflow, compared the browser error with the
  gateway and peer-service CORS configuration, and confirmed that both layers emitted the same
  origin header. Updated only the shared gateway proxy snippet so the gateway hides upstream CORS
  headers and emits one allowlisted response. No peer-service source was modified.
- **Architecture options considered:** Remove gateway CORS; alter each peer service; or retain a
  single gateway browser boundary and hide upstream duplicates. The last option was selected.
- **Developer-selected architecture:** Gateway-owned local CORS with explicit localhost origins;
  peer CORS configuration remains unchanged for direct service access.
- **Reason for selection:** It fixes browser preflight/response behavior without changing peer
  service code or the approved service boundaries.
- **Approved by:** Vincent's prior local CORS approval, 2026-09-30.
- **Affected locations:** `gateway/snippets/proxy.conf` and Order Service change/active-work
  records.
- **Tests run:** `git diff --check` and `docker compose config --quiet` passed. Docker/browser
  runtime verification remains pending because the Docker engine is inaccessible.
- **Test results:** Signup must be retried after rebuilding the gateway; no success is claimed yet.
- **Specification or API deviation:** None.
- **Related decision or change record:**
  `order-service/changes/CHANGE-026-gateway-cors-header-deduplication.md`.

## 2026-09-30 - Human-readable supplier display on Order cards

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Test-first frontend implementation assistance.
- **Exact prompt:**

  > Approve resolving supplier names for Order cards and do not show the internal Order ID in the
  > UI; keep IDs internally only.

- **Key response:** Rehydrated the Order Service workflow, verified the existing Supplier Service
  > `POST /api/suppliers/lookup` contract, wrote focused supplier/OrderCard tests first, then added
  a batched authenticated supplier-name hook for Browse Errands, My Errands, and My Requests.
  Order cards now display names/buildings, fall back safely to IDs when unresolved, and omit the
  user-facing Order ID. No peer-service source or backend contract changed.
- **Architecture options considered:** Change Order Service responses to embed supplier details;
  issue one Supplier request per card; or batch-resolve references in the shared frontend. The
  batched provider lookup was selected.
- **Developer-selected architecture:** Supplier Service remains the source of truth; Order Service
  stores references; the frontend resolves display labels through the gateway and keeps IDs for
  action payloads only.
- **Reason for selection:** Preserves service ownership and avoids N+1 calls or contract changes.
- **Approved by:** Vincent, 2026-09-30.
- **Affected locations:** `frontend/src/components/orders/`, `frontend/src/app/errands/`,
  `frontend/src/app/my-errands/`, `frontend/src/app/my-requests/`, `frontend/src/hooks/`,
  `frontend/src/lib/suppliers.ts`, and Order Service records.
- **Tests run:** Test-first files were added, but Vitest/typecheck/lint/build and browser checks
  are pending because Node/npm and the Docker runtime are unavailable in this session.
- **Test results:** `git diff --check` and staged checks passed; no runtime completion claim.
- **Specification or API deviation:** None.
- **Related decision or change record:**
  `order-service/changes/CHANGE-027-order-card-supplier-display.md`.

## 2026-09-30 - Requester/Courier mode-menu runtime fix

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Runtime-error diagnosis and frontend implementation assistance.
- **Exact prompt:**

  > When switching mode from requester to courier, the page shows “This page couldn't load” and
  > the console reports Base UI error #31.

- **Key response:** Rehydrated the Order Service frontend workflow, correlated the browser error
  with the installed Base UI source, and confirmed that the mode menu's `DropdownMenuLabel` was
  rendered without `DropdownMenuGroup` context. Wrapped the label and options in the required
  group. No backend, peer-service, or contract code changed.
- **Architecture options considered:** Replace the mode menu; remove its label; or preserve the
  existing menu and supply the required Base UI group context. The last option was selected.
- **Developer-selected architecture:** Keep the existing responsive Requester/Courier mode
  switcher and correct its primitive composition with the smallest frontend-only change.
- **Reason for selection:** It directly addresses the observed Base UI error without changing
  approved mode behavior or service boundaries.
- **Approved by:** Vincent's approved frontend scope and reported runtime defect, 2026-09-30.
- **Affected locations:** `frontend/src/components/app-shell.tsx` and
  `order-service/changes/CHANGE-028-mode-menu-group-context-fix.md`.
- **Tests run:** Static Base UI source inspection and `git diff --check`; Node/npm-based frontend
  tests and browser retest were unavailable in this session.
- **Test results:** The source-level fix is applied; runtime completion is not claimed until the
  frontend is rebuilt and the mode switch is retested.
- **Specification or API deviation:** None.
- **Related decision or change record:**
  `order-service/changes/CHANGE-028-mode-menu-group-context-fix.md`.

## 2026-09-30 - Unified dashboard and stable supplier locations

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Workflow rehydration, integration diagnosis, security-boundary refinement, and implementation assistance.
- **Exact prompt:**

  > Remove mode switching, prevent requesters from accepting their own orders,
  > and show supplier locations without a transient supplier-ID mapping.

- **Key response:** Re-read the Order Service workflow and actual User, Supplier,
  and Credit Service implementations. Confirmed that `GET /api/credits/me`
  exists and that `404 ACCOUNT_NOT_FOUND` indicates missing registration-fact
  provisioning. Removed the client-side mode switcher, exposed both dashboard
  functions, filtered self-orders from the browse view, bound Order-side actors
  to User Service-confirmed identities, and prevented supplier-ID flashes while
  preserving Supplier Service ownership.
- **Architecture options considered:** Keep the mode switcher; embed supplier
  names in Order responses; or resolve supplier references in the shared
  frontend. The user selected a unified dashboard and the existing provider-
  backed reference-resolution boundary.
- **Developer-selected architecture:** One shared dashboard, backend-authoritative
  identity, Supplier Service as catalogue source of truth, and stable location
  rendering while asynchronous lookup completes.
- **Reason for selection:** It matches the requested user experience without
  changing peer contracts, database ownership, or service boundaries.
- **Approved by:** Vincent, 2026-09-30.
- **Affected locations:** `frontend/src/components/app-shell.tsx`, frontend
  routes/layout/providers/hooks/components/tests, Order Service UserServicePort,
  adapters/application callers, and CHANGE-029/workflow records.
- **Tests run:** Static source inspection and patch review. `git diff --check`,
  Node/npm frontend checks, Java/Maven tests, Docker startup, and authenticated
  browser verification remain pending or unavailable in this session.
- **Test results:** No runtime completion claim.
- **Specification or API deviation:** User-approved frontend behavior refinement;
  no peer-service source or public API contract was changed.
- **Related decision or change record:**
  `order-service/changes/CHANGE-029-dashboard-without-mode-switch-and-location-stability.md`.

## 2026-09-30 - Credit outcome API gap inspection

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Peer-service API verification and dependency-gap analysis.
- **Order Service feature:** Sprint 1 Sequences 7-9 credit consequences.
- **Related FR/NFR/NTH:** Project D1 F7.1, F10.1.4, F11.1.2, F11.2.3; approved synchronous Credit boundary.
- **AI usage type:** Research / design comparison / verification assistance.
- **Architecture options considered:** Reuse reservation lookup; invent an Order-side balance update; or request explicit Credit-owned settlement/release operations. The first two were rejected because they do not satisfy Credit ownership or outcome semantics.
- **Developer-selected architecture:** Record the missing provider capability as `FEEDBACK-001` and do not invent or implement a peer API.
- **Reason for selection:** The actual Credit Service implements registration, self-balance lookup, reservation, and reservation lookup, but no settlement/release operations for completed, cancelled, or expired orders.
- **Approved by:** Vincent requested the compliance investigation; the missing API remains an open peer-owner decision.
- **Files changed:** `order-service/docs/peer-service-api-feedback.md` and local active-work records.
- **Tests run:** Read-only inspection of Credit Service controller, service, README, security configuration, and integration tests.
- **Test results:** Existing reservation/registration endpoints were confirmed; outcome operations were not found. No peer source was modified.
- **Specification or API deviation:** Sprint 1 deferment conflicts with the broader approved synchronous Credit boundary; unresolved.
- **Related decision or change record:** `FEEDBACK-001`; `CHANGE-029-dashboard-without-mode-switch-and-location-stability.md`.

## 2026-09-30 - Order expiry validation feedback

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Implementation assistance and verification assistance.
- **Order Service feature:** Post-request expiry validation for the Sequence 1 frontend flow.
- **Related FR/NFR/NTH:** Sprint 1 Sequence 1; Order domain creation rule requiring expiry at least 30 minutes after creation.
- **AI usage type:** Test assistance / implementation assistance / verification assistance.
- **Architecture options considered:** Leave rejection to the backend; change the backend minimum; or mirror the existing backend rule in the frontend while retaining backend authority.
- **Developer-selected architecture:** Mirror the existing 30-minute rule in the shared frontend with an HTML minimum, pre-submit validation, inline feedback, and a boundary unit test.
- **Reason for selection:** Prevents avoidable HTTP 400 responses without changing the approved domain rule or any service contract.
- **Approved by:** Vincent requested clear expiry feedback; no architecture or API deviation was introduced.
- **Files changed:** `frontend/src/app/requests/new/page.tsx`, `frontend/src/lib/orders.ts`, `frontend/src/lib/orders.test.ts`, and `order-service/changes/CHANGE-030-order-expiry-validation.md` plus synchronized workflow records.
- **Tests run:** Static inspection and staged diff checks; frontend Vitest/typecheck remain pending because Node/npm are unavailable in this environment.
- **Test results:** `git diff --check` passed after the change; no runtime completion claim.
- **Specification or API deviation:** None.
- **Related decision or change record:** `CHANGE-030-order-expiry-validation.md`.

## 2026-09-30 - Creation-time-only automatic repost choice

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Workflow rehydration, specification clarification, implementation assistance, and verification assistance.
- **Order Service feature:** NTH4 automatic reposting and Sequence 1/10/11 frontend/backend behavior.
- **Related FR/NFR/NTH:** NTH4; Sprint 1 Sequences 1, 10, and 11.
- **AI usage type:** Requirements comparison / architecture-evolution tracking / test and implementation assistance.
- **Finding:** The source context described an automatic plan for an unaccepted order and a requester-reviewed manual repost after expiry, but did not explicitly state whether an `OPEN` order could be edited later. The existing implementation interpreted “available when creating and viewing” as allowing post-creation configuration.
- **Developer clarification:** Vincent approved creation-time-only selection. If automatic repost is unticked at creation it cannot later be enabled; if enabled, all details are submitted at creation. Only an un-reposted expired order exposes manual repost.
- **Key response:** Extended the create contract and aggregate persistence, made the former configure route reject mutation, rendered `OPEN` settings read-only, retained the expired manual-repost controls, and added domain/application/frontend coverage.
- **Architecture options considered:** Keep editable settings; add a disable-only command; or make the creation choice immutable. The immutable creation choice was selected for deterministic lifecycle semantics and no new command state.
- **Approved by:** Vincent, 2026-09-30.
- **Affected locations:** Order Service API/application/domain/tests, shared frontend order payload/create page/repost controls/tests, and CHANGE-031 plus synchronized architecture/traceability/active-work records.
- **Peer boundary:** No peer-service source, supplier/credit contract, database schema, or event behavior was changed.
- **Tests run:** Static inspection and `git diff --check`; Java/Maven, frontend Vitest/typecheck, Docker, and browser verification remain pending or unavailable.
- **Test results:** No runtime completion claim.
- **Specification or API deviation:** CHANGE-031 records the approved clarification and supersedes the post-creation configuration interpretation in CHANGE-022.
- **Related decision or change record:** `CHANGE-031-creation-time-repost-choice.md`; `ARCH-EVO-004`.
