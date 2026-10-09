<!--
AI Assistance Disclosure:
Tool: OpenAI Codex (GPT-5), date: 2026-09-26
Mode: Documentation generation.
Scope: Generated and updated the AI usage record from the available conversation history.
Author review: I reviewed for correctness and course compliance.
-->

# AI Usage Log

## Peer-service API feedback contract rewrite assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-04
- **Mode:** Documentation and verification
- **Affected locations:** `Foc-P28/order-service/docs/peer-service-api-feedback.md`, `docs/decisions/ADR-014-accepted-cancellation-hybrid-flow.md`, `hands-off/README.md`, `changes/CHANGE-066-peer-service-contract-feedback-rewrite.md`, `docs/change-log.md`, `docs/active-work/yao-xiang.md`, and this usage log
- **Prompt:**

  > I want u to rewrite the peer-service-api-feedback. So for every api we need other microservice to implement. U provide not only what we intend to have. Provide them the contract and the format. For event driven flow, Juts provide therm what inside the order event so they know what field they have. For cancel accepted unexpired order required endpoint, create a section called to be discussed and put the endpoint format and what we intend to have under that section

- **Key response:** Rewrote peer feedback with current implemented User/Supplier/Credit HTTP contracts, typed event fields and subscriber actions, and a proposed Credit hold-for-reopen contract in “To be discussed.” Corrected linked current handoff/ADR references. No peer source or application code changed.
- **Author verification:** Inspected the actual peer controllers/DTOs and Order event DTOs/mapper/adapter; reviewed the changed documentation and ran `git diff --check`. Peer consumer and hold endpoint implementation remain unverified.

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

## 2026-09-30 — Verification and temporary Credit outcome model

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Test-specification implementation and verification assistance.
- **Order Service feature:** Sprint 1 Sequences 7–11 local Credit outcome
  behavior and cross-service verification.
- **Developer decision:** Vincent's previously approved temporary Credit
  exception remains in force. Real settlement/release APIs remain Credit
  Service-owned and pending; the local mock is permitted only for deterministic
  Order Service testing.
- **Key response:** Replaced the validation-only local mock with an in-memory
  50-credit account/reservation model covering reservation, settlement
  transfer, cancellation/expiry release, insufficient balance, and command-ID
  idempotency. Fixed two shared-frontend React effect lint errors without
  changing peer-service source.
- **Verification:** Order Java 21/Maven tests passed 12/12; frontend Vitest/RTL
  passed 11/11, typecheck passed, and ESLint passed with 12 warnings and no
  errors; Credit pure tests passed 15/15; User Service tests passed 2/2; the
  Order Docker image rebuilt successfully.
- **Peer-test limitation:** Credit's full suite (26 tests, 11 errors) and
  Supplier's suite (92 tests, 21 errors) could not start their
  Firestore/Testcontainers integration paths because the isolated runner had
  no Docker socket. This is reported as unavailable verification, not as a
  peer implementation defect. No peer source or learning file was modified.
- **Author verification:** Vincent remains responsible for running the
  Docker-backed peer integration and authenticated browser gates and for
  replacing the mock after the Credit owner verifies FEEDBACK-001.

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

## 2026-09-30 - Credit outcome stub and completion-gate hardening

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Workflow rehydration, approved exception implementation, contract documentation, and verification assistance.
- **Order Service feature:** Sprint 1 Sequences 7-11 credit outcomes, lifecycle expiry/repost, API response consistency, OpenAPI metadata, and NFR4 audit logging.
- **Related FR/NFR/NTH:** D1 credit outcome requirements; NFR4 structured logging; Sprint 1 Sequences 7-11.
- **AI usage type:** Peer-API feedback / implementation assistance / architecture-evolution tracking / test and verification assistance.
- **Developer decision:** Vincent approved a temporary validating no-op Credit mock while the Credit Service owner implements settlement and release. Credit ownership, synchronous ordering, and the no-broker decision remain unchanged.
- **Key response:** Added `CreditServicePort.settle/release`, mock and proposed HTTP adapter calls, synchronous completion/cancellation/expiry/repost invocation, explicit unassigned-order expiry selection, authenticated manual repost-draft verification, standard page/error envelopes, OpenAPI operation/security metadata, and structured Order audit events.
- **Approved exception:** The mock does not move balances or write a ledger. FEEDBACK-001 remains `OPEN`; Sequences 7-9 remain `[~]` until the actual peer endpoints, credentials, responses, errors, idempotency, and cross-service tests are verified.
- **Affected locations:** Order Service application/adapters/API/configuration/infrastructure, shared frontend pagination consumers/tests, `docs/peer-service-api-feedback.md`, `CHANGE-032`, architecture evolution, traceability, active work, and this log.
- **Peer boundary:** No peer-service source, peer contract implementation, schema, or learning file was modified.
- **Tests run:** Static inspection and patch validation; `git diff --check` was run. Java/Maven (Java 21 required), Node/npm frontend tests, Docker/browser/E2E, and JaCoCo could not be completed in the current environment.
- **Test results:** No runtime completion claim; the branch remains under the completion gate.
- **Related decision or change record:** `CHANGE-032-credit-outcome-stub-exception.md`; `ARCH-EVO-005`; `FEEDBACK-001`.

## 2026-09-30 - Lifecycle repository-method compile correction

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Build-failure diagnosis, implementation assistance, and Docker verification.
- **Finding:** `LifecycleProcessingService.repostDue` still referenced the removed short expiry-query method after the explicit unassigned-order repository refinement.
- **Key response:** Updated automatic repost selection to call `findByStatusAndExpiresAtLessThanEqualAndCourierIdIsNull` and recorded CHANGE-033.
- **Verification:** `docker compose build order-service` completed successfully through Maven compilation and Spring Boot layered-jar extraction.
- **Peer boundary:** No peer-service or learning file changed.

## 2026-09-30 - Order Service JaCoCo coverage gate

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Build verification and test-coverage configuration assistance.
- **Order Service feature:** Maven verification for Sprint 1 Order Service.
- **Key response:** Added JaCoCo 0.8.15 to `order-service/pom.xml`, configured
  HTML/XML reporting, and enforced the project-required 80% line and branch
  thresholds without excluding production classes beyond the application
  bootstrap class.
- **Verification:** All 12 Order tests passed. The JaCoCo check then failed at
  40.25% line coverage (128/318) and 28.71% branch coverage (60/209).
- **Author verification:** Vincent remains responsible for selecting and
  reviewing additional tests needed to satisfy the approved coverage gate.

## 2026-09-30 - Order Service coverage test completion

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Test-spec-driven implementation and verification assistance.
- **Exact task:** Add the Order Service unit, controller, integration, and
  contract tests required to raise JaCoCo line and branch coverage to at least
  80%.
- **Key response:** Added focused domain, application, controller, DTO/error,
  HTTP peer-adapter contract, local peer-credit integration, and MockPeerAdapters
  edge-case tests. Added only a package-private RestClient constructor needed
  to bind deterministic adapter contract-test servers; production construction
  remains unchanged.
- **Affected locations:** `order-service/src/test/java/` coverage tests,
  `order-service/src/main/java/sg/edu/nus/foc/order/adapter/HttpPeerAdapters.java`,
  `order-service/changes/CHANGE-036-order-coverage-tests.md`, active work,
  change log, and this usage log.
- **Peer boundary:** No peer-service source or contract implementation was
  modified. The two untracked `order-service/learning/` files were left
  untouched.
- **Verification:** In a disposable Java 21 container, `./mvnw -B -ntp clean
  verify` passed with 42 tests, 0 failures, 0 errors, 0 skipped. JaCoCo passed
  both required thresholds: 89.29% lines (300/336) and 83.25% branches
  (174/209).
- **Remaining verification:** Docker-backed runtime, authenticated browser,
  live peer-service contract, and Cloud Run checks remain environment-dependent
  and are not claimed by this test-only change.
- **Related change record:** `CHANGE-036-order-coverage-tests.md`.

## 2026-09-30 - Order Service HTTP-peer smoke profile

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Workflow rehydration, approved integration-profile implementation, and traceability documentation.
- **Exact task:** Add the approved separate Compose profile for real User, Supplier, and Credit HTTP-peer smoke testing while preserving the default mock stack.
- **Key response:** Added `compose.http-peers.yaml`, selecting `ORDER_PEERS_MODE=http`, container-network peer URLs, and peer health-check dependencies. The default `compose.yaml` remains mock-only.
- **Scope:** The profile supports authenticated Sequences 1–3 smoke testing. Credit settlement/release remains unavailable under open `FEEDBACK-001`, so no live completion-gate claim was made for Sequences 7–9.
- **Affected locations:** `compose.http-peers.yaml`, `order-service/changes/CHANGE-037-http-peer-smoke-profile.md`, Order Service change log and active-work record, and this usage log.
- **Peer boundary:** No sibling service source, peer contract, database schema, or learning file was modified.
- **Verification:** The override was statically reviewed. Live Docker/browser verification remains pending because it requires the developer's local Docker engine, Firebase-emulator account, and browser session.
- **Related change record:** `CHANGE-037-http-peer-smoke-profile.md`; `FEEDBACK-001` remains open.

## 2026-09-30 - HTTP-peer adapter startup fix

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Runtime-log diagnosis and Order Service bug-fix assistance.
- **Finding:** The HTTP-peer Compose run failed during Spring context creation because `HttpPeerAdapters` exposed both a production URL constructor and a test constructor without an explicit injection marker; Spring searched for a missing no-argument constructor.
- **Key response:** Annotated the URL-based production constructor with `@Autowired`; test construction and peer contracts remain unchanged.
- **Affected locations:** `order-service/src/main/java/sg/edu/nus/foc/order/adapter/HttpPeerAdapters.java`, CHANGE-038, the Order active-work/change-log records, and this usage log.
- **Peer boundary:** No sibling service source, contract, database schema, or learning file was modified.
- **Verification:** `git diff --check` passed. Maven and Docker verification were unavailable in this execution environment, so no runtime success was claimed.
- **Related change record:** `CHANGE-038-http-peer-constructor-injection-fix.md`.

## 2026-09-30 - Credit reservation UI refresh diagnosis

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Runtime-path diagnosis and beginner learning documentation.
- **Finding:** Default Compose reservations are held in Order Service's in-memory mock adapter, while the sidebar reads the separate Credit Service account. In HTTP-peer mode the Credit account updates, but the frontend `useCreditBalance` hook has no post-order cache invalidation and refreshes only on mount, focus, or manual Refresh.
- **Key response:** Documented the two Compose modes, the two independent read paths, the stale-client snapshot behavior, and the safe HTTP-peer test procedure in `order-service/learning/gcp-resource-setup-learning.md`.
- **Affected locations:** CHANGE-039, the Order active-work/change-log records, the requested learning file, and this usage log.
- **Peer boundary:** No peer-service source, API contract, schema, or application source was modified.
- **Verification:** Static inspection of the Order adapter, Credit endpoint, frontend hook, and Compose configuration. No live browser or Docker state was claimed.
- **Related change record:** `CHANGE-039-credit-ui-refresh-diagnosis.md`.

## 2026-09-30 - Credit UI invalidation guidance

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Architecture guidance and beginner learning documentation.
- **Decision guidance:** A durable broker is not required merely to refresh the browser credit summary. Order creation already waits for synchronous Credit reservation; the missing behavior is frontend cache invalidation.
- **Key response:** Documented a shared credit context or lightweight browser-event pattern that refetches `GET /api/credits/me` after successful credit-affecting mutations, and explained why a broker/WebSocket/SSE/polling loop would add unnecessary complexity.
- **Affected locations:** CHANGE-040, the Order active-work/change-log records, the requested learning file, and this usage log.
- **Peer boundary:** No peer-service source, API contract, schema, infrastructure, or application source was modified.
- **Verification:** Static reasoning from the synchronous Order/Credit call path and frontend `useCreditBalance` behavior; no runtime claim.
- **Related change record:** `CHANGE-040-credit-ui-invalidation-learning.md`.

## 2026-09-30 - Credit UI invalidation implementation

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Approved frontend vertical-slice implementation and test addition.
- **Key response:** Added a shared browser invalidation event, wired `useCreditBalance` to refetch the authoritative Credit balance, and emitted the event after successful order creation, manual repost, cancellation, and completion.
- **Affected locations:** `frontend/src/lib/credit-balance-events.ts`, its Vitest test, the credit hook, Order creation/actions/repost components, CHANGE-041, active-work/change-log records, the learning file, and this usage log.
- **Peer boundary:** No peer-service source, API contract, schema, broker, or infrastructure was modified.
- **Verification:** Static inspection passed. Frontend Vitest/typecheck could not run because `npm` was unavailable in this execution environment; no runtime success was claimed.
- **Related change record:** `CHANGE-041-credit-ui-invalidation-implementation.md`.

## 2026-09-30 - Supplier lookup authentication-race diagnosis

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Browser-network diagnosis and beginner learning documentation.
- **Finding:** A hard refresh can trigger the frontend Supplier lookup before Firebase restores the signed-in user/token. The gateway returns `401 UNAUTHENTICATED`; CORS is present and is not the cause.
- **Key response:** Documented the React hook/`RequireAuth` timing boundary and the recommended auth-settled guard without changing the Supplier API or peer source.
- **Affected locations:** CHANGE-042, the Order active-work/change-log records, the requested learning file, and this usage log.
- **Peer boundary:** No peer-service source, API contract, schema, or infrastructure was modified.
- **Verification:** Static inspection of `useSupplierNames`, `RequireAuth`, `useApi`, Firebase auth state, and the supplied network response. No runtime claim.
- **Related change record:** `CHANGE-042-supplier-lookup-auth-race-diagnosis.md`.

## 2026-09-30 - Supplier lookup authentication gate

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Approved frontend bug-fix implementation and verification planning.
- **Exact task:** Prevent `401 UNAUTHENTICATED` responses when refreshing the
  Order Service `/errands` route by ensuring browser requests carry a Firebase
  bearer token before reaching the gateway.
- **Key response:** Added a centralized `useApi()` authentication gate,
  auth-settled guards for Order and Supplier data-loading effects, and a unit
  test for rejecting absent bearer tokens before `fetch()`. The Profile page
  was also routed through `useApi()` so protected browser calls share the same
  fail-closed token gate.
- **Affected locations:** `frontend/src/hooks/use-api.ts`,
  `frontend/src/hooks/use-supplier-names.ts`,
  `frontend/src/hooks/use-supplier-permissions.ts`, the Errands and request
  pages, Profile, Supplier browser/detail/editor components,
  `frontend/src/lib/api.ts`, the focused test, CHANGE-043, active work, and the
  change log.
- **Peer boundary:** No sibling service source, API contract, database,
  gateway, or infrastructure file was changed. Learning files remain
  untracked by project convention.
- **Verification:** `git diff --check` passed. Vitest, typecheck, lint,
  Docker rebuild, and authenticated browser verification remain pending because
  Node/Docker/browser runtime access is unavailable in this execution
  environment.
- **Author verification:** The author should rebuild the frontend and confirm
  a hard refresh of `/errands` sends a bearer-authenticated request without a
  401 response.
- **Related change record:** `CHANGE-043-supplier-lookup-auth-gate.md`.

## 2026-09-30 - Frontend test typecheck correction

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Docker-build failure diagnosis and atomic test correction.
- **Finding:** The frontend Docker build compiled application code but failed
  TypeScript checking because the new auth-guard test relied on Vitest globals
  without importing them.
- **Key response:** Added explicit `describe`, `it`, and `expect` imports from
  `vitest`; no production behavior or peer-service code changed.
- **Affected locations:** `frontend/src/lib/api-auth.test.ts`, active work,
  CHANGE-043, and this usage log.
- **Verification:** `git diff --check` passed. A local Docker rebuild could not
  be rerun in this execution environment because Docker Buildx configuration
  access was denied; the developer should rerun the frontend build locally.
- **Related commit:** `c16fac0 test(frontend): type auth guard test with vitest`.

## 2026-09-30 - Sprint 1 sequences 1–11 developer handoff

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Workflow-guided handoff and documentation generation.
- **Exact task:** Create an Order Service handoff for Yao Xiang describing the
  combined Sprint 1 branch, sequence flows, UI polishing direction, current
  real/mock integration status, Credit Service feedback dependency, testing
  plan, and production-level completion gates.
- **Key response:** Added a detailed handoff README under
  `order-service/hands-off/` and recorded CHANGE-044, the active-work update,
  and the change-log entry.
- **Affected locations:**
  `order-service/hands-off/README.md`,
  `order-service/changes/CHANGE-044-yao-xiang-sprint-1-handoff.md`,
  `order-service/docs/change-log.md`, and
  `order-service/docs/active-work/vincent.md`.
- **Peer boundary:** No sibling-service source or peer-owned contract was
  modified. The handoff points to `FEEDBACK-001` for Credit settlement/release
  agreement. Existing learning files remain untracked.
- **Verification:** Reconciled the handoff against the Order Service workflow,
  Sprint 1 requirements/contracts/diagrams, frontend workflow/style guide,
  Compose profiles, peer feedback, and current active-work records. `git diff
  --check` and repository status checks remain to be run before commit.
- **Author verification:** The author should review the receiving-developer
  instructions and confirm whether any exact local/developer file should be
  handled in a separate approved workflow change.

## 2026-09-30 - Sequence 1 gateway and request-flow learning

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Repository-grounded beginner learning documentation.
- **Exact task:** Explain the gateway forwarding path, browser CORS/auth flow,
  Order Service class/layer invocation for Sequence 1, peer calls, response
  propagation, and Compose/Spring environment-variable behavior.
- **Key response:** Added a detailed untracked learning note under
  `order-service/learning/` and recorded CHANGE-045.
- **Affected locations:**
  `order-service/learning/seq1-order-creation-and-gateway-flow.md`,
  `order-service/changes/CHANGE-045-sequence-1-gateway-learning.md`,
  `order-service/docs/change-log.md`, and
  `order-service/docs/active-work/vincent.md`.
- **Peer boundary:** No application source, gateway configuration, Compose
  configuration, peer-service source, or contract was modified.
- **Verification:** Static inspection matched the gateway template/proxy,
  Compose files, frontend auth/API code, Order Controller, creation service,
  domain aggregate, peer adapters, and Spring configuration. No live runtime
  verification was claimed.

## 2026-09-30 - Gateway URL substitution clarification

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** Beginner learning-document clarification.
- **Exact task:** Clarify which code forwards `/api/orders`, where
  `ORDER_SERVICE_URL` comes from, and whether the Dockerfile environment is
  substituted into the nginx template.
- **Key response:** Expanded the Sequence 1 learning note with the Dockerfile
  `ENV` → container environment → nginx `envsubst` → rendered `set
  $upstream` → `proxy_pass` chain, plus the Cloud Run deployment override.
- **Affected locations:** The existing learning note, CHANGE-045, and the
  Order active-work record.
- **Peer boundary:** No gateway behavior, Compose configuration, application
  source, peer service, or API contract was modified.
- **Verification:** Re-inspected `gateway/Dockerfile`, nginx template/proxy,
  `gateway/deploy/env.yaml`, and Compose configuration. No live runtime claim.

## 2026-09-30 - Order OpenAPI CI documentation-test fix

- **Tool:** OpenAI Codex (GPT-5)
- **Mode:** CI failure diagnosis and test implementation.
- **Finding:** CI's structural check found the Order Service OpenAPI dependency
  but no `OpenApiDocumentationTest` under `src/test`.
- **Key response:** Added an MVC-slice OpenAPI test that generates
  `target/openapi.json` and checks service path prefixes, operation summaries,
  and 2xx responses without requiring PostgreSQL or peer containers.
- **Affected locations:** The new Order Service test,
  `CHANGE-046`, the change log, and active-work state.
- **Peer boundary:** No endpoint behavior, API contract, gateway, peer-service
  source, or deployment configuration was modified.
- **Verification:** The structural class check and `git diff --check` passed.
  Maven wrapper and system Maven were unavailable, so test execution remains
  pending CI or local Java/Maven verification.
- **Follow-up correction:** Updated the OpenAPI test's excluded security
  auto-configuration imports to the Spring Boot 4.1 package names used by this
  service (`4a40c5b`).

## 2026-09-30 — Frontend supplier-permissions CI lint fix (Order Service)

- **Tool:** OpenAI Codex (GPT-5)
- **Date:** 2026-09-30
- **Mode:** Implementation and verification
- **Prompt:**

  > one more fail test in ci

- **Key response:** Diagnosed the CI lint error in
  `use-supplier-permissions.ts` and changed the hook to associate permission
  results with the Firebase UID, avoiding a synchronous state update inside an
  effect while retaining fail-closed behavior during auth transitions. Change
  record: `CHANGE-047`.
- **Affected locations:** `frontend/src/hooks/use-supplier-permissions.ts`,
  `order-service/changes/CHANGE-047-frontend-permissions-lint-fix.md`,
  `order-service/docs/change-log.md`, and
  `order-service/docs/active-work/vincent.md`.
- **Author verification:** The developer must confirm `npm run lint` in CI or
  a local Node/npm environment. `git diff --check` passed; no peer-service
  source or learning file was changed.

## 2026-09-30 — On-demand Order Service CI rehearsal workflow

- **Tool:** OpenAI Codex (GPT-5)
- **Date:** 2026-09-30
- **Mode:** Workflow design and documentation
- **Prompt:**

  > Can we run one time ci on our order service and if it is ok then onlt we push to gh, make this part of our current project workflow skill and is trigger based on demand, just pick those related to order service ci component and run one time ourself if i tirgger this skill

- **Key response:** Added CHANGE-048 and documented an explicit-trigger-only
  local CI rehearsal covering Order Service Maven/OpenAPI/JaCoCo checks, the
  approved shared frontend npm checks, and relevant container/configuration
  checks. The rehearsal does not push or mutate Git history and excludes peer,
  Cloud IAM, and browser checks by default.
- **Affected locations:** The local Order Service skill, `skills.md`,
  `docs/ai-project-context.toml`, `CHANGE-048`, the change log, and active-work
  records.
- **Author verification:** The developer selected the pre-push rehearsal
  workflow and may explicitly trigger it in a later turn. No application or
  peer-service source was changed.

## 2026-09-30 — OpenAPI test Springdoc context fix (Order Service)

- **Tool:** OpenAI Codex (GPT-5)
- **Date:** 2026-09-30
- **Mode:** CI failure diagnosis and test implementation
- **Prompt:**

  > The maven error still occurs when running ci

- **Key response:** Inspected the pasted GitHub log and identified a new
  failure after the executable-bit fix: `OpenApiDocumentationTest` returned
  404 for `/api/orders/v3/api-docs`. Added the missing core
  `SpringDocConfiguration` import and recorded the fix as `CHANGE-049`.
- **Affected locations:**
  `order-service/src/test/java/sg/edu/nus/foc/order/api/OpenApiDocumentationTest.java`,
  `order-service/changes/CHANGE-049-openapi-springdoc-test-context.md`, the
  Order Service change log/active-work records, and this usage log.
- **Author verification:** The developer supplied the CI evidence. The fix is
  pending GitHub Maven verification; no peer-service or runtime API source was
  modified.

## 2026-09-30 — On-demand CI rehearsal execution (Order Service)

- **Tool:** OpenAI Codex (GPT-5)
- **Date:** 2026-09-30
- **Mode:** Verification
- **Prompt:**

  > Run the Order Service pre-push CI rehearsal now.

- **Key response:** Ran the scoped rehearsal on
  `sprint-1/seq-1-to-seq-11`. The OpenAPI structural guard and Compose config
  validation passed. Maven, npm, and Docker image checks were unavailable due
  to the local wrapper/runtime and Docker daemon permission limitations.
- **Affected locations:** Order Service verification only; no application,
  peer-service, or learning file was changed.
- **Author verification:** Results are recorded as unavailable rather than
  passed where the local runner could not execute the command. The developer
  must rerun the unavailable checks locally or in CI before pushing.

## 2026-09-30 — OpenAPI Springdoc API-only test correction (Order Service)

- **Tool:** OpenAI Codex (GPT-5)
- **Date:** 2026-09-30
- **Mode:** CI failure diagnosis, implementation, and verification
- **Prompt:**

  > One last chance to solve this CI issue, maven side is still not working, please inspect the file and determine why and update

- **Key response:** Inspected the supplied GitHub Maven output and determined
  that Maven and compilation were working; the only error was the OpenAPI MVC
  test importing UI-only `SwaggerConfig` without its UI properties bean.
  Replaced that UI configuration with Springdoc's API properties
  auto-configuration and retained only the core/WebMVC JSON API
  configurations. Recorded the completed correction in `CHANGE-049`.
- **Affected locations:**
  `order-service/src/test/java/sg/edu/nus/foc/order/api/OpenApiDocumentationTest.java`,
  `order-service/changes/CHANGE-049-openapi-springdoc-test-context.md`, the
  Order Service change log/active-work record, and this usage log.
- **Author verification:** The developer supplied the failing CI evidence.
  The corrected test was verified with Java 21 in Docker: all 43 Maven tests
  passed with zero failures/errors/skips and the JaCoCo coverage check passed.
  No runtime API, peer-service source, frontend, persistence, or deployment
  behavior was changed.

## 2026-10-01 — Java layering and traditional DTO refactor (Order Service)

- **Tool:** OpenAI Codex (GPT-5)
- **Date:** 2026-10-01
- **Mode:** Architecture refinement and implementation
- **Exact implementation prompt:**

  > All your code stick tgt. For some like declared variables, it's fine to stick tgt but for import statements and class, declared methods, all these u should put spaces. And why u use var ? Alywas do typing. And why u dont have the repository class file for the domain. And the response dto and request dto for api. Dont omit them. Use the traditional dtos. U are calling findby straight in the application layer. But u should put those interacts with db in another file cuz we need separation of concern. And always use thespring annotation getter setter and constrructur instead of doing manual declaration. Using spring annotation will guarantee the future support but using manual function no

- **Key response:** The developer selected explicit Java types, conventional
  formatting, named request/response DTO classes, Lombok-generated boilerplate,
  domain repository interfaces, and infrastructure JPA adapters. AI assistance
  implemented the selected structure while preserving domain behavior, HTTP
  fields, persistence schema, and peer boundaries. Static inspection and Java
  21 production/test-source compilation passed; the test suite was not run.
- **Affected locations:** `order-service/AGENTS.md`, CHANGE-050, architecture
  evolution/change log/active-work records, Order Service API/application/domain/
  repository/infrastructure Java source, directly affected tests, and this log.
- **Author verification:** Yao Xiang supplied and selected the structure and style
  direction. Maven compilation passed; developer review and test execution remain
  pending.

## 2026-10-01 — MapStruct response mapping refinement (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-01
- **Mode:** Design refinement, implementation, and compile verification
- **Exact implementation prompt:**

  > Can u use mapper taht uses @mapping instead of using the from of the response

- **Key response:** The developer selected annotation-driven response mapping.
  AI assistance added a Spring-managed MapStruct `OrderMapper` with explicit
  `@Mapping` declarations, removed mapping factories from response DTOs, and
  injected the mapper into `OrderController`. Generated mapping source was
  inspected and Java 21 production/test-source compilation passed.
- **Affected locations:** Order Service `pom.xml`, API mapper/controller/response
  DTOs, directly affected tests, CHANGE-050, architecture evolution/change log/
  active-work records, `order-service/AGENTS.md`, and this usage log.
- **Author verification:** Yao Xiang explicitly requested MapStruct-style
  `@Mapping`. Public JSON, domain behavior, persistence schema, peer contracts,
  and frontend behavior remain unchanged. Test execution remains pending.

## 2026-10-02 - Updated overall design comparison (Order Service)

- **Tool:** OpenAI Codex (GPT-5)
- **Date:** 2026-10-02
- **Mode:** Architecture research, comparison, and documentation synchronization
- **Exact prompt:**

  > Please check the newly added order service overall design-updated. I have updated new seq. Do comparison with the order-service overall design to find the difference and update the required md file and all that. Check the workflow when we have archiutecture update what do we do

- **Key response:** Compared the newly supplied `Order Service Overall Doc - Updated.pdf`
  with the fingerprinted overall design. Recorded the material changes to Sequences 5-8:
  typed completion/cancellation events, transactional outbox, shared broker fan-out,
  independent Credit/User subscribers, at-least-once delivery and recovery rules, and
  same-order accepted cancellation reopening. Synchronized the source fingerprint,
  architecture, contracts, event registry, Sprint context, traceability, peer feedback,
  active work, ADR-009, CHANGE-051, and change log. No application or peer source changed.
- **Affected locations:** Order Service `docs/`, `changes/CHANGE-051-overall-design-update-comparison.md`,
  `docs/decisions/ADR-009-updated-overall-event-architecture.md`, and this log.
- **Author verification:** Yao Xiang supplied the updated design and requested the comparison.
  PDF text extraction and source/code searches were completed. Poppler visual review was
  unavailable, and event/outbox/broker implementation remains blocked pending feature-level
  approval and Sprint 1 reconciliation.

## 2026-10-02 - Messaging publisher layout and sequence diagrams (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-02
- **Mode:** Architecture documentation and workflow update
- **Exact prompt:**

  > ALways invoke the order service work flow before coding for order service  I want new folder like the api folder we have right now and called messagingpublisher. In the messagingPublisher folder we have sub folder like publisher and interfaces. In the interface we will have the IeventPublisher interface. In the publisher,we will have the actual publisher. Please update the agent.md and skill.md to ask ai to store publisher file in this publisher folder. Check the order service overall doc -updated. I want new seq diagrams with these updated info and the class diagrams for those publisher and their interface. Every event gonna have their Ipublisher and publisher. We have publishAcceptedOrderCancellationTask() in AcceptedOrderCancellationPublisher. And the interface we also have publishAcceptedOrderCancellationTask(). And this is in seq 7. User service and Credit Service subscribe to it. We have publishOpenOrderCancellationTask(). And this is in seq 6. Credit Service subscribe to it. We will publish publishOrderCompletionTask() when the task completed is not overdue. Credit Service subscribe to it. And this is in seq 5. We will publish publishOverdueOrderCompletionTask() when the task completed is overdued. And this is new sequence. Credit service subscribe to it. User Service subscribe to it.

- **Key response:** Invoked the Order Service workflow and added publisher package conventions to `AGENTS.md`, the executable skill, and `skills.md`. Added updated sequence diagrams for Sequences 5-8 and a class diagram with four event-specific interface/publisher pairs and requested subscriber routing. Inspected actual Credit and User source; neither has the requested event consumers. Recorded FEEDBACK-002 and CHANGE-052. No Java or test source was changed because event schema, `IEventPublisher` role, broker, outbox migration, and peer consumer contracts remain unresolved.
- **Affected locations:** Order Service workflow files, Sprint sequence/class diagram indexes and new Mermaid diagrams, `changes/CHANGE-052-messaging-publisher-layout-and-diagrams.md`, `docs/architecture-evolution.md`, `docs/peer-service-api-feedback.md`, Sprint 1 contracts, active-work handoff, and this usage log.
- **Author verification:** Yao Xiang explicitly requested the package layout, method names, subscriber routing, and diagrams. The package/docs scope is recorded as requested; the open transport and peer contracts are not treated as approved or implemented. Mermaid was not rendered in this environment.

## 2026-10-02 - Publish-before-status publisher design (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-02
- **Mode:** Architecture documentation and implementation workflow
- **Exact prompt:**

  > Invoke the order service work flow before coding. Please put placeholder for the topic. i willadd inside the respective publisher later. And u only need to do in order service. Dont edit the other service. The other service will make the corresponding consumer later. Now u just assume it works. And u put the order info all in the published event. Now let's start doing the changed sequence. Add the publisher and its interface. We have OrderCompletionTaskPublisher and its interface. OverdueOrderCompletionTaskPublisher and its interface. We have OpenOrderCancellationTaskPublisher and its interface. We have AcceptedOrderCancellationTaskPublisher and its interface. Link them its respective sequence flow. Since we have publisher, as long as we successfully published the event then we can assume it is success then proceed to change the status. We dont have to wait for the other microservice reply. We only revert the change of status like the whole process if we didn't manage to publish

- **Key response:** Re-invoked the Order Service workflow and synchronized CHANGE-053, architecture/contract/traceability records, publisher class and sequence diagrams, active-work handoff, and package-placement instructions with publish-before-status, full Order snapshots, topic placeholders, and assumed future consumers. No Java or peer-service source changed because the service has no selected messaging client and the complete snapshot needs checkpoint/overdue persistence support.
- **Affected locations:** `Foc-P28/order-service/` architecture, workflow, contracts, diagrams, and change records; `Foc-P28/ai/usage-log.md`.
- **Author verification:** The developer's explicit prompt authorizes the Order-side ordering, payload scope, topic placeholders, and future-consumer assumption. Concrete transport selection and persistence changes remain unresolved; peer integrations are not verified.

## 2026-10-02 — Google Cloud Pub/Sub producer implementation (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-02
- **Mode:** Implementation, testing, and verification
- **Exact prompt:**

  > google pubsub. Please start coding

- **Key response:** Re-invoked the Order Service workflow, used the developer-selected Google Cloud Pub/Sub transport, and implemented the four typed publisher pairs, complete Order/repost/checkpoint event snapshots, placeholder topic configuration, emulator channel setup, publish-confirmation handling, and publish-before-status transitions. Added accepted-order cancellation as an Order Service endpoint and updated architecture/traceability records. AI assisted implementation and verification; the developer selected the transport and authorized the Order-only scope.
- **Affected locations:** `Foc-P28/order-service/` Pub/Sub dependency/configuration, application/domain/API/infrastructure source, tests, sequence/class diagrams, CHANGE-054, ADR-009, traceability, active-work and architecture records; this usage log.
- **Author verification:** `mvn verify` passed all 57 tests with no skipped tests and passed configured JaCoCo line and branch coverage gates. Publisher construction and acknowledgment paths were covered with mocks/local channel setup. The developer should configure real topic IDs before use; live broker delivery and Credit/User consumer integration have not been verified. No sibling service or frontend source was changed.

## 2026-10-02 - Order transition requester/courier authorization (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-02
- **Mode:** Implementation, testing, and documentation
- **Exact prompt:**

  > Cancel Accepted which will mark accepted order as aborted, mark order as delivered, mark order as pick up, mark order as in_progress, can only done by the courier and this action can only be done to the order has courier id the same id as their user id which means this action can only be done by courier and to their order. Accept order that mark order as accepted can only be done by courier and they can only accept the order when the courierId is still null and the requester id of the order is not the user id so no one accept yet. The cancel can only be done by requester and we need verify this order has the same requester as their userid. Complete the order can only be done by the requester to their own order as well.

- **Key response:** Re-invoked the Order Service workflow and implemented the approved identity/ownership rules. `Order` now rejects acceptance when a courier is already assigned and limits accepted cancellation to the assigned courier; transition services verify the required requester/courier role before command-receipt replay. Corrected updated overall Sequence 7 and recorded CHANGE-055/ADR-010. Added focused authorization tests.
- **Affected locations:** Order Service aggregate/application source and tests; updated overall Sequence 7; Sprint requirements/acceptance tests; service contracts; traceability; CHANGE-055; ADR-010; architecture evolution/change log; active-work handoff; this usage log.
- **Author verification:** The user explicitly approved the behavior in the task prompt. The initial focused suite passed (15 tests); two further ownership/replay tests passed in the final full suite. `mvn verify` passed all 62 tests with no failures/errors/skips and passed JaCoCo line/branch gates. C: had 0 GB free, so tests ran with temporary files redirected to `D:\tmp` (D: had 53 GB free). No peer-service or frontend source changed.

## 2026-10-02 - Unified order completion event (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-02
- **Mode:** Architecture update, implementation, tests, and documentation
- **Exact prompt:**

  > I want to remove the publish the overdueOrderCompletion flow because when the order is completed. We need add penalty score if it is overdued. And minus score when it is not overdued. So user service anyhow need consume the event whenever there's completed order regardless of the overdue status so it doesn't make sense to separate out a overdue order completion event and the logic. I want u to remove that seq diagram if we have the extra seq diagram for overdueordercompletion and remove its publisher and update the business logic as well. So now we just publish only one type of event which is ordercompletiontaskevent whenever we complete the order. Please update all the affected document

- **Key response:** Re-invoked the Order Service workflow. Recorded CHANGE-056/ADR-011; unified completion publication so every completion sends `OrderCompletionTaskEvent` with the full Order snapshot, `overdue`, and `overdueAt`; removed the overdue-only DTO/publisher/topic configuration and Sequence 8 diagram; updated current contracts, diagrams, architecture instructions, and traceability. Only Order Service is in scope; Credit/User consumers remain assumed future work.
- **Affected locations:** `Foc-P28/order-service/` transition/event source, tests, sequence/class diagrams, architecture/contract/traceability docs, ADR-009 supersession note, ADR-011, CHANGE-056, active-work and change log; this usage log.
- **Author verification:** Full `mvn verify` passed all 62 tests with no failures/errors/skips and passed JaCoCo line/branch gates. `git diff --check` passed and active-file scans found no overdue-only publisher or Sequence 8 diagram references. The first sandboxed compile attempt failed closing compiler resources while reading a local dependency JAR; retrying outside the sandbox succeeded. No peer-service or frontend source changed. Live Pub/Sub and peer consumer behavior remain unverified.

## 2026-10-02 - Admin paginated Order query (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-02
- **Mode:** Implementation, testing, and documentation
- **Exact prompt:**

  > Can u check the supplier create endpoint and learn how they do admin verification. Then create me the admin controller with the api that return all the orders and support filtering by status. If the status is null, then it will return all order with all types of status in default. Else it will just filter the order by status and return the results page so they need receive pageable parameter from frontend as well

- **Key response:** Inspected Supplier's `@PreAuthorize("hasRole('ADMIN')")` and User Service role-context integration, then added `GET /api/orders` with optional exact status filtering, one-based `Pageable`, newest-first results, and the existing `OrderPageResponse`. User Service role-context is called with the verified caller token in HTTP mode; local mock mode follows Supplier's configured admin email behavior. Recorded CHANGE-057/ADR-012 and approved the synchronous query for EV-3. No Admin Service or frontend source was changed.
- **Affected locations:** Order Service admin API, query/repository/persistence layers, Firebase/User Service role security and configuration, tests, OpenAPI guard, README, requirements/contracts/traceability, sequence/class diagrams, CHANGE-057, ADR-012, ARCH-EVO-013, EV-DEC-003, active-work, change log; this usage log.
- **Author verification:** The initial test-first compile failed because the new controller was not yet implemented; focused tests then passed. Full `mvn verify` passed all 86 tests with zero failures/errors/skips and passed JaCoCo line/branch gates. Tests cover 401/403, role-context bearer forwarding, mock admin email mapping, page numbering/size cap, filtering, and persistence branching. The User Service implementation was inspected, but live cross-service authentication was not run. Frontend and sibling service source were unchanged.

## 2026-10-02 - REST client startup bean fix (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-02
- **Mode:** Diagnosis, implementation, testing, and verification
- **Exact prompt:**

  > APPLICATION FAILED TO START: Parameter 4 of method roleProvider in SecurityConfiguration required a bean of type `RestClient.Builder` that could not be found.

- **Key response:** Traced the failure to the missing Spring Boot 4.1.1 `spring-boot-starter-restclient` dependency. Added the starter and a focused auto-configuration test; the existing security context test also started successfully.
- **Affected locations:** `Foc-P28/order-service/pom.xml`, `src/test/java/sg/edu/nus/foc/order/config/RestClientBuilderAutoConfigurationTest.java`, CHANGE-058, active-work and change-log; this usage log.
- **Author verification:** Focused test passed. Full `mvn verify` passed all 87 tests with no failures/errors/skips and passed JaCoCo line/branch gates. Docker rebuild/runtime startup was not exercised; the developer can rebuild the Order Service image to apply the dependency. No frontend or peer-service source changed.

## 2026-10-02 - Public Swagger webjar assets (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-02
- **Mode:** Diagnosis, implementation, testing, and verification
- **Exact prompt:**

  > I got this when I try to login the swagger-ui of order service: HTTP 401 for `/webjars/swagger-ui/index.html`.

- **Key response:** Confirmed `/webjars/**` was absent from the Order Service security allowlist. Added it so Swagger's static assets load without Firebase authentication; protected API and admin authorization rules remain unchanged.
- **Affected locations:** `Foc-P28/order-service/src/main/java/sg/edu/nus/foc/order/config/SecurityConfiguration.java`, the security test, CHANGE-059, active-work and change-log; this usage log.
- **Author verification:** The new MockMvc check reproduced HTTP 401 before the fix and passed after it. All five security tests passed, including anonymous rejection for `/api/orders`. Full `mvn verify` passed all 88 tests and JaCoCo line/branch gates. Live Docker/browser verification remains pending. No frontend or peer-service source changed.

## 2026-10-02 - Local Pub/Sub emulator (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-02
- **Mode:** Implementation, runtime verification, and documentation
- **Exact prompt:**

  > Forgot about the real pub sub flow. Assist me to create the pubsub emulator to mock it. Cuz I have created the actual topic and assign the value in the application.yaml file so once we deploy then it will work. Now just use the pubsub emulator only

- **Key response:** Added the Google Cloud Pub/Sub emulator to the default local Compose stack, configured Order Service to use a local project and emulator endpoint, and initialized its three event topics before startup. Preserved the real Cloud project/topic defaults for deployed runs and kept mock peer-adapter mode independent.
- **Affected locations:** Root `compose.yaml`; Order Service `application.yaml`, README, CHANGE-054 follow-up, CHANGE-060, change log, active-work; this usage log.
- **Author verification:** Compose config passed. The emulator and Order Service started, all topics were created and listed, repeat initialization handled already-existing topics, Order Service was healthy with the emulator endpoint and local project, and a local publish was acknowledged with message ID `1`. Docker package build and Spring startup passed. Maven tests could not run: the wrapper failed in PowerShell, and system Maven could not create its configured local repository even with an override. No peer service or consumer source changed; real-cloud publish was not attempted.

## 2026-10-03 - Courier accepted-errand cancellation UI (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-03
- **Mode:** Implementation, testing, and verification
- **Exact prompt:**

  > Check the frontend of cancelling an accepted Task. Do we have frontend for that. If we dont have. Please add it. The courier should be able to cancel the accepted errand if the errand still in accepted status

- **Key response:** Added a confirmation-protected cancel action for assigned couriers on accepted errand cards in My Errands and linked it to the existing versioned Order cancellation endpoint. Successful `ABORTED` responses remove the errand from the courier's active list; failures retain the card and show the API error.
- **Affected locations:** `frontend/src/components/orders/order-actions.tsx`, its Vitest component test, `frontend/src/app/my-errands/page.tsx`, `frontend/src/lib/orders.ts` and its tests; Order Service acceptance tests, traceability, CHANGE-061, active-work; this usage log. No Order Service backend, API contract, or peer-service source changed.
- **Author verification:** The new tests failed first because the accepted courier action was missing, then all 18 frontend tests passed. Typecheck and production build passed; lint passed with 12 pre-existing warnings in login/profile files. Authenticated browser and live Pub/Sub checks remain pending.

## 2026-10-03 - Restore Order security profile split

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-03
- **Mode:** Implementation, testing, and documentation
- **Affected locations:** `Foc-P28/order-service/src/main/java/sg/edu/nus/foc/order/config/`, Order security tests, README, ADR-012, architecture evolution, CHANGE-057/059 follow-up notes, CHANGE-062, change log, active-work record; this usage log.
- **Exact prompt:**

  > Please revert to the initial production and local split. We only ccheck the admin role for admin endpoint when in production. In local, we dont need to do so we dont need authentication in local so please revert back. And only mandate authentication when in production

- **Key response:** Restored anonymous access for non-production profiles and Firebase bearer authentication for production. Isolated method-security activation in the `prod` profile so admin role checks apply only in production.
- **Author verification:** Ten focused Order security/configuration tests and full `mvn verify` passed (89 tests, zero failures/errors/skips; JaCoCo line/branch gates passed), including local anonymous access and production 401/403/admin-success cases. Docker profile startup remains pending; see CHANGE-062.

## 2026-10-03 - Transactional outbox for Order outcome events (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-03
- **Mode:** Implementation, testing, architecture documentation, and verification
- **Exact follow-up prompts:**

  > Can we just use a cron job cuz it is easier to do exception handling and spring has mature cron job as well

  > why need schedules(fixed delay). I thot we can straightaway run after we commit to the outbox

  > Okie I approved. Start coding

- **Key response:** Implemented the user-approved transactional outbox in Order Service. Completion and both cancellation transitions persist the resulting Order, checkpoint, command receipt, and full event snapshot atomically. An `AFTER_COMMIT` listener immediately dispatches; Spring cron recovers pending/expired-lease rows with bounded retry. Pub/Sub failure does not reverse a committed transition. Delivery remains at least once with stable event IDs for consumer deduplication.
- **Affected locations:** `Foc-P28/order-service/src/main/java/sg/edu/nus/foc/order/` transition, outbox, persistence, dispatch, and scheduling code; `src/main/resources/db/migration/V2__create_order_event_outbox.sql`; `application.yaml`; outbox/transition/PostgreSQL Testcontainers tests; CHANGE-063, ADR-013, architecture and sequence/class diagrams, requirements traceability, acceptance tests, active-work and workflow records; this usage log. No peer-service or frontend implementation changed.
- **Author verification:** The developer approved the transactional-outbox approach, immediate post-commit dispatch, and cron recovery. Full `mvn -B -ntp '-Dmaven.repo.local=target/m2-repository' verify` passed 102 tests with zero failures/errors/skips; JaCoCo line and branch gates passed. Testcontainers exercised clean install and V1-to-V2 migration, Hibernate schema validation, lease-aware Postgres claims, persisted retry/publish state, and rollback of both Order and outbox intent. `git diff --check` passed. Live Pub/Sub consumer behavior and Cloud Run idle-time cron execution were not verified; see CHANGE-063.

## 2026-10-03 - Transactional outbox handoff for Vincent (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-03
- **Mode:** Documentation and handoff
- **Exact prompt:**

  > write hand-ff document for Vincent mentioning what have we done. We done testing, implement event driven flow and all that

- **Key response:** Wrote a dedicated Yao-to-Vincent handoff for CHANGE-063 covering atomic Order/outbox persistence, the three completion/cancellation event flows, implementation paths, 102-test verification, and remaining Pub/Sub consumer and Cloud Run cron limitations. Linked it from the existing hands-off README.
- **Affected locations:** `Foc-P28/order-service/hands-off/CHANGE-063-transactional-outbox-to-vincent.md`, `hands-off/README.md`, Yao Xiang active work; this usage log.
- **Author verification:** Cross-checked the handoff against CHANGE-063, ADR-013, source, V2 migration, sequence/class diagrams, and the recorded Maven result. No Vincent active-work entry or peer-service source was edited.

## 2026-10-03 - Accepted-cancellation expiry split and peer contracts (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-03
- **Mode:** Order Service workflow, implementation, tests, and documentation
- **Exact prompt:**

  > Please update the Cancel Accepted Errand flow and related documentation based on the following logic. Only the assigned courier may cancel. Before expiry, synchronously ask Credit Service to hold/reset the transaction without refund and reopen only after Credit confirms. After expiry, publish the accepted-cancellation event for Credit to refund and User Service to penalize the courier. Verify the open-cancel and completion subscribers and document the missing synchronous Credit endpoint in the peer API document.

- **Key response:** Implemented the Order-side deadline split, direct `ACCEPTED -> OPEN` after synchronous hold confirmation, and expired `ACCEPTED -> ABORTED` outbox flow. Updated the peer dependency contract, subscriber action details, UI response feedback, diagrams, architecture/decision records, and acceptance criteria. No Credit or User source was edited.
- **Affected locations:** `Foc-P28/order-service` transition/domain/peer adapter code, tests, CHANGE-064, ADR-014, sequence 5-7 and publisher class diagrams, architecture/service contracts, FEEDBACK-002/003, sprint acceptance/traceability, Yao active work and handoff indexes; `Foc-P28/frontend` accepted-cancellation action and list behavior; this usage log.
- **Author verification:** Order Service `mvn verify` passed 107 tests with no failures/errors/skips and both JaCoCo gates. Focused backend tests passed 39 tests before the final HTTP-202 rejection assertion; the final full verification includes it. Frontend Vitest passed 11 tests; `npm run typecheck` and ESLint on changed frontend files passed. Live Credit hold API, Credit/User event consumers, Pub/Sub delivery, and browser verification remain pending. Credit/User repositories were not modified.

## 2026-10-03 - Make Pub/Sub publisher factory unit test credential-independent (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-03
- **Mode:** Test maintenance and verification
- **Exact prompt:**

  > yes fix the test

- **Key response:** Changed the publisher factory cache/lifecycle test to use the emulator transport instead of constructing a production publisher that requests Application Default Credentials. The test no longer requires Google credentials or a live Pub/Sub service.
- **Affected locations:** `Foc-P28/order-service/src/test/java/sg/edu/nus/foc/order/messagingpublisher/publisher/GoogleCloudPubSubPublisherFactoryTest.java`, `Foc-P28/order-service/docs/active-work/yao-xiang.md`; this usage log.
- **Author verification:** The diff was inspected and `git diff --check` passed. The focused Maven test did not execute: local Maven compilation failed with `Cannot close compiler resources` while compiling production sources. Direct drive inspection showed 5.18 GB free on C: and 27.79 GB on D:, so disk space is not the cause. CI verification after this change remains pending.

## Spring-scheduled Order expiration event assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-03
- **Mode:** Design, implementation, documentation, and verification
- **Exact prompt:**

  > Check the Order Service Overall Doc - Updated. We have cancel and expire open order the same sequence diagram which is the sequence 6. The flow is the same. It's just the event published is different. And cancel open order is trigger based and the expire open order is cron job based. 1. the evnt should replace that call. So the credit service will consume the published event and do the refund. 2. use the scheduler

  > use spring scheduler

- **Key response:** Added Spring-scheduled expiry discovery and `OrderExpirationTaskEvent` using the existing transactional outbox/after-commit dispatcher. Removed the synchronous Credit release boundary from expiry. Updated Sequence 6/9, contracts, class diagram, evolution/decision/traceability and Vincent's handoff. Credit subscriber and Pub/Sub topic initialization remain future work/out of scope. Local Maven verification remains blocked by the Windows compiler reporting `Fatal Error: Cannot close compiler resources`; `git diff --check` passed.
- **Affected locations:** `Foc-P28/order-service` lifecycle scheduler/service, event DTO/publisher/mapper/dispatcher, persistence query, tests, application configuration, sequence/class diagrams, requirements/contracts, peer feedback, ADR-015, CHANGE-065, architecture evolution, handoff, active work, and this usage log.
- **Author verification:** The developer/team selected Spring scheduling and confirmed the event replaces synchronous Credit release. Maven tests could not be verified in this local toolchain; run full `mvn verify`/CI in a working Java 21 environment and confirm Credit's consumer contract separately. Shared Compose and Credit/User source were not modified.
## Remove checkpoint history from Order events

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-06
- **Mode:** Contract update, implementation, documentation, and verification
- **Exact prompt:**

  > For order event, i dont think we should put the checkpoint in cuz we gonna send all the history there. I think it will make it massive. we dont reallyy need there. Please update the service api feedback as qwqell

- **Key response:** Removed checkpoint history from the common event snapshot, mapper, and publisher factory; removed the event-only checkpoint DTO; retained checkpoint persistence/history and internal overdue calculation. Updated the peer event contract and synchronized ADR/change/architecture/sprint/handoff artifacts. Kept event version 1 because no consumers were found in inspected peer implementations.
- **Affected locations:** `Foc-P28/order-service` event DTO/mapper/factory and application tests; peer-service API feedback, service contracts, diagrams, requirements, architecture context/evolution, ADR-016, CHANGE-067, change log, current sprint, Vincent handoff, active work; this usage log.
- **Author verification:** `git diff --check` passed. Focused Maven tests did not reach test execution: Java 21 failed with `Fatal Error: Cannot close compiler resources`; forked compilation also failed generically. Rerun focused tests and full `mvn verify` in CI or a working Java 21 environment. Peer source, database schema, and frontend were not changed.

## Credit courier assignment before Order acceptance

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-06
- **Mode:** Contract review, test-driven implementation, documentation, and verification
- **Exact prompt:**

  > Then u should create the mock endpoint for credit service and start linking the accepted flow. Later on we will replace the real endpoint when it is done.

- **Key response:** Added an Order-owned Credit assignment port, local mock behavior, and a proposed typed HTTP adapter contract. Acceptance now waits for Credit confirmation before persisting `ACCEPTED`, and leaves the Order unchanged on Credit failure. FEEDBACK-004 remains open because Credit has no matching route and authentication/recovery semantics need agreement.
- **Affected locations:** `Foc-P28/order-service` assignment application/domain, peer adapters and DTOs, tests, FEEDBACK-004, service/sprint contracts, Sequence 3 diagram, ADR-017, CHANGE-068, architecture evolution, traceability, current sprint, active work, and this log.
- **Author verification:** Tests were added before production changes. Focused Maven compilation failed before test execution with `Fatal Error: Cannot close compiler resources`; rerun focused and full verification in a working Java 21 environment. The user authorized an Order-side stub only; no Credit or frontend source changed, and the live provider integration remains unverified.

## Order version boundary for synchronous Credit calls assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-06
- **Mode:** Contract refinement, implementation, documentation, and verification
- **Affected locations:** `Foc-P28/order-service` Credit port/adapters/DTOs, tests, peer and service contracts, Sequence 3 and class diagrams, CHANGE-069, ADR-018, architecture evolution, traceability, active work, and AI project context; this usage log.
- **Exact prompt:**

  > Then I think dont need to send the version. Please update the doc as well
  >
  > I think if event it can still make sense cuz we might need them to identify the event. But if just towards the other service then dont need
  >
  > I feel like for the endpoint we call to credit service, hold-for-transaction, do we need to provide them the other info. They just need a order id to find the transactrion. Then for the assign the courier they just need the courier id and order id. The rest are redundant

- **Key response:** Removed redundant command, requester, amount, and Order-version fields from synchronous assignment and hold/reset requests. Assignment sends only `courierId` in the body with `orderId` in the path; hold/reset sends only `orderId` in the path with no body. Mock retries are idempotent by transaction state. Order-side version validation and event `orderVersion` remain. No Credit Service or frontend source was changed. Maven compilation failed locally before test execution; verification remains pending.
- **Author verification:** Review the diff and run the focused Order Service Maven tests in a working Java 21 environment; the real Credit assignment/hold endpoints remain unimplemented and unverified.

## Accepted-order cancellation hold contract confirmation assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-06
- **Mode:** Contract documentation and architecture-context update
- **Affected locations:** `Foc-P28/order-service/docs/peer-service-api-feedback.md`, ADR-014, service contracts, architecture/current-sprint/traceability/context, CHANGE-070, and this log.
- **Exact prompt:**

  > we confirm the synchronous hold for reopen flow. so not need tom say to be discussed. This will be the final one.

- **Key response:** Marked FEEDBACK-003 as `AGREED` and removed its “To be discussed” label. Recorded the final bodyless Order-ID endpoint contract and synchronous 200-before-reopen behavior; preserved the distinction that Credit has not implemented the endpoint and production authentication/reconciliation remain requirements. No source or tests changed.
- **Author verification:** Confirmed the current Credit source has no hold-for-reopen route. Reviewed FEEDBACK-003 and linked decision, change, traceability, and current-sprint references for consistent status.

## Unify OPEN-order cancellation and expiry refund event (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-06
- **Mode:** Architecture-approved implementation, documentation, and verification
- **Exact prompt:**

  > I feel like we dont need the expirationevent. We just have use cuz opentaskcancellation and the orderexpirationevent highly similar so we can reuse them so just rename the openordercancellation to openorderrefund then use it when expire and the open ordercancellation. remove the expiration event and update the topic

- **Key response:** Replaced separate requester-cancellation and scheduled-expiry event types with one `OpenOrderRefundTaskEvent`, one publisher interface/implementation, and one topic. Kept `CANCELLED` versus `EXPIRED` Order status and the Spring expiry/outbox flow. The dispatcher converts pending legacy outbox rows for either prior event into the shared event while preserving `eventId`. Updated Sequence 6, peer/service contracts, publisher diagram, ADR/change/evolution, Order instructions, active-work, and current indexes. No peer-service code or schema migration changed.
- **Affected locations:** `Foc-P28/order-service` event DTO/mapper/factory, cancellation and lifecycle processing, dispatcher/publisher/topic config and tests; root `compose.yaml` Order-topic initialization; Sequence 6 and publisher class diagram; service/peer/event/architecture/sprint contracts; CHANGE-071, ADR-019, architecture evolution, handoff/instructions/active-work; `Foc-P28/ai/usage-log.md`.
- **Author verification:** `git diff --check` passed and source/current-doc searches show the legacy event names only in dispatcher compatibility handling/tests and historical records. Focused Maven tests did not execute: compilation failed with Java 21 `Fatal Error: Cannot close compiler resources`, including a forked compile. Compose validation was unavailable because the installed Docker command rejected Compose `-f` and access to its Docker config was denied. Rerun focused/full tests and Compose validation in CI or a working environment. Credit's subscriber implementation remains unverified and out of scope.

## 48-hour delivered-order auto-completion assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-07
- **Mode:** Scope amendment, test-driven implementation, documentation, and verification
- **Prompt:**

  > I wanna do the auto complete scheduler that pick up the delivered task and compared the delivered time with current time. if it is >= 48 hours then we just do our complete flow
  >
  > continue doing. We run out of time so we need complete it early

- **Key response:** Added a configurable Spring scheduler that selects currently `DELIVERED` orders in PostgreSQL by their delivered-checkpoint cutoff (at least 48 hours) and uses pessimistic no-wait row locking. It rechecks under lock, then completes through the same checkpoint, overdue calculation, receipt, and `OrderCompletionTaskEvent` outbox flow as requester completion, with a lifecycle actor and stable auto-completion command ID. Scope/Sequence 5 records were amended. No peer service, frontend, event schema, or database migration changed.
- **Affected locations:** `Foc-P28/order-service` lifecycle and transition services, Order/domain repository/JPA adapter, scheduler/configuration, tests, Sprint scope/requirements/acceptance/Sequence 5/class diagram, service/architecture/current-sprint/traceability/event registry/context, CHANGE-072, ADR-020, architecture evolution, handoff, active-work; root `.env.example` and `compose.yaml`; this log.
- **Author verification:** Tests were added before implementation. The focused Maven command reached compilation of 86 production files but Java 21 failed before test execution with `Fatal Error: Cannot close compiler resources`; forked compilation also failed before tests. `git diff --check` passed. No database schema migration was needed. Full Maven verification, PostgreSQL query/locking integration test, and runtime scheduler behavior require a working CI/local test environment.

## Real Pub/Sub development-topic setup assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-07
- **Mode:** Architecture approval, implementation, documentation, and verification
- **Affected locations:** `Foc-P28/compose.yaml`, `.env.example`, `order-service` publisher factory/test, application and Cloud Run configuration, README, AGENTS/skills, peer/service contracts, architecture context/evolution, ADR-021, CHANGE-073, active-work/change indexes, and this log
- **Exact prompts:**

  > Can we actually dont use the pubsub emulator. Instead, we publish to the actual pubsub which I can provide u the secret key or what. Just that when we go on prod we save all these info in the google secret. Locally will be in env that share among us. We can just create the different topics in the gcp like dev topic and prod topic.
  >
  > I will do so. accepted-order-cancellation-dev-v1, open-order-refund-dev-v1, order-completion-dev-v1. Do the coding, Outside of the repo, just tell me what to do on gcp

- **Key response:** Replaced the local emulator stack and emulator publisher transport with authenticated real Pub/Sub. Configured the existing project and three approved dev topics; mounted per-developer ADC read-only in the Order container; configured production topic overrides and Cloud Run identity guidance; retained mock peer adapters. Added CHANGE-073/ADR-021 and synchronized configuration, architecture, instructions, and event-topic documentation. No GCP/IAM or peer-service resources were modified.
- **Author verification:** The focused Maven wrapper command could not start (`Cannot start maven from wrapper`) before test execution. Docker Compose validation was unavailable because the local Docker CLI could not read its config file (access denied); image runtime and live GCP publication were not verified. GCP topic/IAM setup remains pending; no credentials were requested or exposed.

This log records the AI exchanges. The team remains responsible for validating all output. Requirements prioritization, architecture, component boundaries, schemas, interfaces, and performance/security trade-offs were finalized by the team, not by the AI tool.


## CI Compose validation fix assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-08
- **Mode:** Diagnosis, implementation, and verification
- **Affected locations:** .github/workflows/ci.yml, order-service/changes/CHANGE-074-ci-compose-adc-config.md, order-service/docs/change-log.md, order-service/docs/current-sprint.md, order-service/docs/active-work/yao-xiang.md, this log
- **Prompt:** “Fix the ci cd error”
- **Key response:** Diagnosed the Compose CI validation step as omitting the required local-only GOOGLE_APPLICATION_CREDENTIALS_HOST. Configured that static validation step to use a temporary empty runner file path; it does not start services or expose credentials.
- **Author verification:** Reviewed workflow scope and changed files. git diff --check passed. The GitHub run log could not be retrieved because gh is not installed; local Compose config validation was unavailable because Docker Desktop’s config file is inaccessible in this environment. A hosted CI rerun remains necessary.


## Attached CI failure repair assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-08
- **Mode:** Workflow review, diagnosis, test repair, and verification
- **Prompt:** User supplied “required variable GOOGLE_APPLICATION_CREDENTIALS_HOST is missing a value” from Compose validation, “Job results: success failure success failure,” and the attached Maven log, requesting a fix for CI when pushing.
- **Key response:** Confirmed CHANGE-074 addresses the actual Compose error without personal ADC in CI. Repaired seven test failures/errors across fixed-date acceptance tests, Jackson 2 event serialization, and incomplete legacy event fixtures. Preserved business/API behavior and the 80% coverage gate. Added CHANGE-075 and synchronized tracking records.
- **Affected locations:** .github/workflows/ci.yml; order-service/src/test/java/sg/edu/nus/foc/order/application/{OrderAssignmentServiceTest,OrderTaskEventFactoryTest,OrderOutboxDispatcherTest}.java; order-service/changes/CHANGE-074-ci-compose-adc-config.md and CHANGE-075-deterministic-ci-tests.md; change-log/current-sprint/active-work records; this log.
- **Author verification:** Focused 13 tests passed. Fresh full Maven verification reported 123 tests, zero failures/errors and six skipped PostgreSQL Testcontainers tests because the sandbox cannot access the Docker named pipe. Fresh coverage measures 90.82% lines and 81.19% branches. Used cached wrapper-selected Maven 3.9.16 and local Java 21 source/target flags to bypass Windows wrapper and JDK symbol-archive permissions; tracked build settings are unchanged. Moved obsolete generated classes aside before fresh coverage verification. Compose config --quiet returned exit code 0 with an empty ADC placeholder; workflow YAML parsing and git diff --check passed. Hosted CI, Docker integration/image build, actionlint, and cloud infrastructure checks remain pending. No cloud publication or credential access was performed.


## Production Pub/Sub deployment guidance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-08
- **Mode:** Repository inspection and advisory guidance
- **Prompt:** User created the three prod-v1 topics and asks how Pub/Sub will work after pushing to main and whether database configuration is needed.
- **Key response:** Verified the existing deployment topic template, missing environment assignments, shared Cloud Run runtime identity, main-to-staging automatic deployment and manual production promotion. Explained topic-level publisher IAM, Cloud Run ADC without credential files, environment configuration rather than database settings, and independent subscriber responsibilities. No runtime/configuration or GCP changes made.
- **Affected locations:** order-service/docs/active-work/yao-xiang.md and this log only.
- **Author verification:** Read the actual deploy workflows/script, environment template/files, publisher factory and project settings; checked official Google Cloud identity/IAM documentation. No live IAM/topic validation or publish test performed; user's topic creation remains reported evidence.


## Deployment Pub/Sub topic values implementation (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-08
- **Mode:** Approved configuration update and static verification
- **Prompt:** “add in the .env for me.” This follows the production/staging topic setup guidance.
- **Key response:** Appended the three prod-v1 topic settings to infra/environments/production.env and existing dev-v1 settings to staging.env. Preserved existing values and the deployed project/identity. Added CHANGE-076 and updated sprint/change/work records. No application source, peer services, cloud resources or credentials changed.
- **Affected locations:** infra/environments/{production,staging}.env; order-service/changes/CHANGE-076-deployment-pubsub-topic-values.md; Order change-log/current-sprint/active-work records; this log.
- **Author verification:** Local substitution and YAML parsing of both deployment templates confirmed exact topic/project values, one assignment per variable and preservation of existing settings. git diff --check passed. No Maven tests required for this configuration-only change; no deployment, IAM validation or live Pub/Sub publication performed.


## Quarter-hour Requester time selection and scheduler cadence

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-08
- **Mode:** User-approved UI/configuration refinement, test-first implementation and verification
- **Prompt:** User requested minute choices 00/15/30/45 when creating errands, expiry scans every 15 minutes, event scheduling every five minutes, and auto-completion every minute because courier delivery times are uncontrolled.
- **Key response:** Invoked Order spec workflow and frontend baseline; implemented a shared date/hour/quarter-minute picker on creation and repost forms, rounded suggestions and client validation with existing UTC payloads. Changed expiry/recovery defaults and local/cloud overrides to 15/five minutes, preserving immediate dispatch and minute auto-completion. Added CHANGE-077/ADR-022/ARCH-EVO-024 and synchronized requirements, sequence notes, runtime context, traceability and handoff. No peer-service source, API/event schema, stored deadlines or migration changed.
- **Affected locations:** frontend/src/app/requests/new, components/orders picker/repost controls and tests, lib/orders helpers/tests; Order expiry/outbox schedulers and tests, application/deploy env settings; root Compose/.env.example and infra environment files; Order change/decision/architecture/sprint/context/handoff records; this log.
- **Author verification:** Red helper/picker and Spring next-run boundary tests observed before implementation. Final frontend 29 tests pass; type generation/TypeScript pass; lint has only 12 pre-existing warnings outside changed files and changed-file lint is clean. Focused backend 12 tests pass; full Maven 125 tests with zero failures/errors and six skipped Docker integration checks due to denied named-pipe access. Fresh JaCoCo lines 90.82%, branches 81.19%; configured gate passes. Compose static config and rendered staging/production cron YAML pass; git diff --check passes. Browser skill was used for discovery but no browser was connected, so visual/responsive/authenticated runtime verification was unavailable. No real Pub/Sub publication or deployment performed.


## Hourly Order outbox recovery cadence assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-08
- **Mode:** Implementation and documentation
- **Affected locations:** OrderOutboxScheduler and Order Service cron configuration; CHANGE-078, ADR-023, architecture evolution, sequence/requirements/traceability/handoff records, and existing scheduler cadence test
- **Prompt:** “Then the listener run periodically or just right after the method call. In this case, I dont think we actually fail that many times. Maybe make the publish failed event scheduler run per hour”
- **Key response:** Confirmed the after-commit listener is event-triggered and the cron scheduler is recovery-only. Changed the recovery interval to hourly while preserving immediate publication, 15-minute expiry, and one-minute auto-completion. Documented the longer retry wait and Cloud Run scale-to-zero limitation.
- **Author verification:** Static source/configuration/reference inspection and git diff --check; automated tests and deployed/cloud scheduling were not run.


## Role authorization annotation architecture review assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-08
- **Mode:** Architecture review and advisory documentation
- **Affected locations:** order-service/docs/active-work/yao-xiang.md and ai/usage-log.md only; no application source, tests, contracts, configuration, or accepted decisions changed
- **Prompt:** User asked how current authentication works and whether @RequireCourierRole(id), @RequireRequesterRole(id), and @RequireAdminRole(id) could centralize token extraction, User Service lookup, and repeated checks.
- **Key response:** Inspected current Firebase validation, role-provider/mock-versus-HTTP configuration, User Service role/eligibility endpoints, and locked Order ownership guards. Recommended Spring Security meta-annotations for role checks, centralized verified actor context, request-scoped role reuse, separate courier eligibility, and transactional ownership checks. Explained the proposed annotations do authorization while the filter/decoder performs authentication; no role ID attribute is needed. Implementation remains unapproved.
- **Author verification:** Human review of this proposal is pending. AI performed source/documentation inspection and whitespace checks only; no tests or live cross-service verification were performed.


## Supplied role-aspect reference assessment assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-08
- **Mode:** Architecture review and advisory documentation
- **Affected locations:** order-service/docs/active-work/yao-xiang.md and ai/usage-log.md only
- **Prompt:** User supplied a RoleAspect/SecurityHelper/BaseSecurityHelper reference and requested a recommendation for clean API methods with centralized role annotations for courier, requester, and admin.
- **Key response:** Read the entire supplied reference and current Order/User security implementation. Confirmed the reference checks existing SecurityContext roles and its User cache serves display names. Recommended three Spring Security meta-annotations with central verified actor/role context, token-based User role lookup, complete role-set membership, separate courier eligibility, and locked Order ownership guards. Called out current mock Spring-role versus HTTP application-verification mode so checks are not accidentally removed. Proposed design is unapproved/unimplemented.
- **Author verification:** Human review is pending. AI inspected repository/reference/official Spring documentation and ran a review-record whitespace check; no tests, live authentication calls, source edits, or deployment occurred.


## Contextual role annotations implementation assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-08
- **Mode:** Architecture refinement, implementation, testing, verification and documentation
- **Affected locations:** Order Service API/security/adapters, application-prod.yaml, authorization tests, ADR-024/CHANGE-079/ARCH-EVO-026, context/Sprint/contracts/diagrams/README/handoff/active-work/indexes; Order-specific .env.example setting. No sibling service or frontend source changes.
- **Prompt:** Yeah We need do based on our context. The file provided previously is just for reference. Cuz in every endpoint eventually we need call the suer service to verify the role regardless it is the admin endpoint anot so we can wrap in the annotation
- **Key response:** Implemented requester/courier/admin Spring role meta-annotations and shared-read policy; production defaults to real User Service roles once per request and validates response identity against verified JWT subject. Reuses confirmed roles in adapters, preserving local mock identities, fresh courier eligibility and locked ownership guards. Added traditional role DTO and fail-closed JSON 403/503 handling. Initial tests exposed three authorization failures; refined focused 37 tests pass, full suite has 134 passes and six skipped Docker PostgreSQL checks. Fresh coverage 91.21% lines/82.19% branches and configured 80% gate pass. Git diff check passes; live peer/cloud verification was not performed.
- **Author verification:** The developer selected the contextual annotation architecture and production User Service verification; AI assisted source comparison and implementation. Original PDFs remain absent; approved Markdown and actual User endpoints inspected. Human code review, hosted Docker checks and live deployment verification remain pending; no commit/push/deployment performed.


## Courier eligibility and locked ownership explanation assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-08
- **Mode:** Source inspection and explanation
- **Affected locations:** order-service/docs/active-work/yao-xiang.md and ai/usage-log.md; application source inspected only
- **Prompt:** Courier eligibility and locked order ownership checks remain separate. (what this means
- **Key response:** Distinguished confirmed courier role, current User Service eligibility and per-order ownership/state guards under the transaction row lock. Explained that another courier cannot act on an assigned order and a suspended courier can retain the role while failing eligibility. Acceptance retains its separate unassigned OPEN-order checks. No application behavior changed; no tests or live calls run.
- **Author verification:** Explanation checked against current HttpPeerAdapters, OrderAssignmentService, OrderTransitionService and Order aggregate. Existing approved architecture retained; human review of the explanation is pending.


## Authorization change scope explanation assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-08
- **Mode:** Source review and explanation
- **Affected locations:** Order Service source/tests/design records and active-work review; no new source modifications this turn
- **Prompt:** Tell me what have u done for error checking and why need change so many files. When u just create a wrapper
- **Key response:** Explained the meta-annotations/controller placements, production switch from mock to actual User Service roles, identity re-use, retained courier eligibility/locked ownership, 401/403/503 mapping, related tests and required architecture records. Clarified the implementation exceeds a wrapper-only code change because the user requested real User Service role verification on every production API request.
- **Author verification:** Compared the current working-tree diff and source behavior. Developer review of whether to reduce this scope remains open; no files were reverted or behavior changed during the explanation.


## Previous production authentication behavior explanation (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-08
- **Mode:** Source review and explanation
- **Affected locations:** Order Service security configuration, role providers, controllers, peer adapters, environment configuration and active-work notes; no application code changed
- **Prompt:** Previously before your changes how the endpoint do authentication in prod
- **Key response:** Explained that the committed baseline required Firebase JWT authentication for non-public production routes. Authorities in the security filter came from the mock role provider configured in the deployment env (requester/courier defaults and ADMIN email allowlist). Specific application operations separately called User Service via HTTP for requester/courier identity and eligibility; AdminOrderController used an ADMIN role preauthorization. Local profile permitted requests.
- **Author verification:** Compared committed source and staging/production environment configuration. No tests or live service calls were run.


## Staging/production real-peer configuration review (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-08
- **Mode:** Configuration and peer-source inspection; advisory answer
- **Affected locations:** Deployment workflows/script/env files, Order configuration/adapters, Supplier role configuration and Credit/User routes reviewed; Order active-work and this disclosure updated
- **Prompt:** When we put the project on stage or deploy on prod, will it automatically use all the actual endpoint and set up all the thing when we stage. We dont have to do extra stuff. Cuz those mock should only be used locally for testing
- **Key response:** Explained automatic deployment wiring (prod profile, HTTP peers and current Order HTTP role default, environment URLs, Cloud SQL/secret injection, attached runtime identity and dev/prod topics). Identified shared USER_SERVICE_MODE=mock still enabling Supplier mock roles, missing Credit assignment/hold endpoints and Credit/User event consumers, and the recorded scheduling limitation. HTTP mode has no automatic mock fallback. Deploy health checks cannot establish full end-to-end readiness.
- **Author verification:** Read current repository configurations and peer source. No live deployment, cloud permissions/subscriptions, or end-to-end tests verified. No application or infrastructure configuration modified; user review of deployment readiness remains pending.


## Sprint 2-3 lifecycle clarification assistance (Order Service)

- **Tool:** OpenAI Codex
- **Date:** 2026-10-08
- **Mode:** Local source/specification review and approval documentation
- **Affected locations:** Order workflow profile, allocation, current-sprint, Vincent active work, CHANGE-081, change log and architecture evolution; this disclosure
- **Prompt:** Use the existing Credit assignment endpoint as a mock if missing; ask whether reopened acceptance may overwrite its courier and whether the old expired order ID can be overwritten; work only on Order-owned parts and record under Vincent.
- **Key response:** Recorded the developer's confirmed scope and local mock exception, distinguished abort-to-EXPIRED from repost-to-new-OPEN, and identified the proposed Credit contract's conflicting-assignment restriction and stale-retry risk. Credit reset/reassignment and repost replacement/retention remain pending; no application source, tests, peer service or infrastructure changed.
- **Author verification:** Vincent explicitly selected the scope, identity, mock exception and previously the separate courier-attempt history model. Further design choices and runtime tests are pending; AI advice is not developer approval.

## Post-merge Compose CI cleanup (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-10-08
- **Mode:** Configuration repair and static validation
- **Affected locations:** compose.yaml; order-service/changes/CHANGE-080-retired-mongo-compose-cleanup.md; Order current-sprint/change-log/active-work; this disclosure
- **Prompt:** service "user-mongodb" refers to undefined volume user-mongodb-data: invalid compose project (fix the ci, now it got problem after I resolve ot)
- **Key response:** Removed the retired temporary user-mongodb helper instead of adding back its obsolete volume. Preserved main's MongoDB/User Service configuration and Order's PostgreSQL/PubSub configuration. The existing CI placeholder ADC step was not changed.
- **Author verification:** Reproduced the reported error before the fix; base and HTTP-peer Compose config checks exit 0 after the fix. Normalized JSON has no dangling named-volume references. Exact peer/Order block preservation checks pass. No live containers/cloud publishing, application tests or hosted CI run performed. Developer review and hosted CI rerun remain pending.

## Integrated Credit dependency review assistance (Order Service)

- **Tool:** OpenAI Codex
- **Date:** 2026-10-09
- **Mode:** Peer source/contract inspection and Order documentation update
- **Affected locations:** Order peer-service-api-feedback, project context, profile/allocation, active work, current sprint, service contracts, change log and CHANGE-087; this disclosure. Peer source/config/tests inspected read-only.
- **Prompt:** Check the current sprint-2-3-credit branch after integrating Credit changes; identify implemented endpoints and topic subscriptions and update peer feedback to leave outstanding provider work only.
- **Key response:** Removed obsolete missing-assignment/reset/refund/completion requests and recorded actual implementations and push provisioning evidence. Retained reset replay authorization (003), reservation terminal replay/cross-service reconciliation (006), delegated background authorization (005), and documented the incompatible Credit accepted-cancellation refund subscriber (007). Current Order abort emits User penalty facts with OPEN/EXPIRED, not the legacy ABORTED Credit refund semantics. Provisioning script exists separately from bootstrap; cloud deployment is unverified. No peer, application, test or infrastructure fixes were made.
- **Author verification:** User authorized branch inspection and feedback rewrite, not a peer implementation or architecture change. Source/test/config comparison performed; eight feedback JSON examples, context TOML/assertions, change-log links, exact scope and Git whitespace checks passed. Recorded D1/Overall hashes match; generic drift checker failed on pre-existing manifest column format (exit 2). No Maven, live APIs, browser, GCP subscriptions/IAM, or ledger delivery tests executed. Background retry implementation remains paused and Sprint [~]; peer/runtime verification is pending.

## Focused Credit outcome verification assistance (Order Service)

- **Tool:** OpenAI Codex
- **Date:** 2026-10-09
- **Mode:** Existing peer-test execution, source review and clarification
- **Affected locations:** Order feedback/context/active work and CHANGE-087 follow-up; local ignored learning; this disclosure. Credit source and tests read-only, build output in disposable Docker source copy.
- **Prompt:** Confirm whether implemented refund/completion subscriptions can refund expired/cancelled orders and transfer completed credits; leave accepted-cancellation alone. Challenge the stale-reset example because acceptance follows only after confirmed reset and reopening.
- **Key response:** Confirmed implemented Credit financial handlers and compatible Order event fields. Ran ./mvnw -B -ntp clean test -Dtest=CreditOrderEventConsumerTest,CreditOrderEventControllerTest,CreditServiceTest,JpaCreditRepositoryIntegrationTest in Java 21 Docker with isolated Testcontainers PostgreSQL V1/V2. All 27 tests passed (5 consumer, 3 controller, 7 service, 12 persistence), zero failures/errors/skips. Duplicate refund/settlement effects guarded; completion assertions move requester 50 -> 40, courier 50 -> 60 and reserved 10 -> 0. Corrected 003: current Order locks and checks status/version, waits for reset 200, commits OPEN/EXPIRED plus command receipt; committed replay avoids another Credit call. Simplified stale-clear story omitted these guards. Residual Credit-only replay protection remains distinct; no retry worker implemented.
- **Author verification:** Vincent reaffirmed the sequential reset/open/accept behavior and requested no accepted-cancellation changes. Re-read actual guards and financial handlers; existing tests executed unchanged against temporary database, not application data. No peer/application/config/infrastructure source edited or cloud provisioning performed. Legacy accepted-cancellation tests passing do not establish current 007 compliance. Full Credit suite, coverage verify, actual Pub/Sub/OIDC/IAM and full Order-to-Credit integration remain unverified; no architecture decision made by AI. Human review of these results remains pending.

## Local live Docker and Pub/Sub readiness assistance (Order Service)

- **Tool:** OpenAI Codex
- **Date:** 2026-10-09
- **Mode:** Read-only configuration/source review and readiness documentation
- **Affected locations:** Order Vincent active work; ignored local refund/history learning; this disclosure. Compose, Dockerfiles, Credit push security/configuration and cloud provisioning script inspected read-only.
- **Prompt:** Review Docker and Order setup so a local run can exercise real Credit endpoints and Pub/Sub for expired, aborted, cancelled and completed workflows.
- **Key response:** HTTP-peer override exists and Credit financial handlers are implemented, but no local cloud push delivery path is configured. Combined Compose config fails on missing GOOGLE_APPLICATION_CREDENTIALS_HOST. Credit requires Google-signed push identity/audience; the internal Docker URL/demo identity is not public cloud ingress. Existing provisioning script targets staging/production Cloud Run, not local Credit. Proposed isolated authenticated HTTPS ingress for local push testing; not implemented or approved. Noted historical Sequences 1-3-only override comments. Existing one-minute lifecycle and immediate/15-minute recovery publication remain unchanged.
- **Author verification:** Source/configuration inspection and actual Compose config failure recorded; official Google push requirements checked. No images rebuilt, stack launched, app databases touched, cloud resources/IAM changed or peer source edited. Prior 27 focused Credit tests remain component-only evidence. User must approve new local delivery setup and cloud owner must authorize resources; developer review and live broker/browser/ledger verification pending. Learning excluded from commits; Sprint remains [~].

## Approved local-live integration connector assistance (Order Service)

- **Tool:** OpenAI Codex
- **Date:** 2026-10-09
- **Mode:** Approved local configuration, test-first helper implementation and handoff
- **Affected locations:** compose.local-live.yaml, .env.example, HTTP-override comments; Order-owned deploy/local-live, scripts/fixtures/tests, CHANGE-088/ADR-029/runbook/context/traceability/active-work and ignored learning; this disclosure. All peer application source remains unchanged.
- **Prompt:** Proceed with local real HTTP peer endpoints and real Pub/Sub delivery; supply Docker build/up/down commands for financial workflow integration testing.
- **Key response:** Implemented developer-isolated topics/subscriptions, Google-authenticated wrapped financial push through temporary cloudflared HTTPS and exact-path POST-only non-root/read-only nginx. Scoped Setup/Check/Pause verifies resource ownership, config, audience, identity, DLQ and namespace before retargeting; no shared/staging subscription edits or key files. Enabled existing Order authenticated profile with local DB/Auth and real User roles. Credit accepted-cancellation subscription intentionally not created; User penalty consumers and all background retry/delegation implementation remain deferred. Runbook includes personal login/ADC prerequisites, commands, tunnel reconfiguration, safe stop and per-flow balance assertions.
- **Author verification:** Vincent explicitly approved local ingress/configuration implementation; cloud owner permission still required. Observed red test before implementation. Final 60 actual Compose/safety assertions (gcloud mocked) and 16 real nginx/fixture assertions passed; read-only nginx temp-directory defect caught/fixed, baseline test's obsolete Mongo name corrected then rerun. Actual Setup fails safely before side effects because gcloud is unavailable; standard ADC absent. D1/Overall hashes match; generic drift checker retains pre-existing manifest-format failure (exit 2); completion checker fails five historical/fixed-format findings (exit 1). No application stack build/start, cloud provisioning, live Google auth/financial/browser tests, database reset or peer source edit claimed. Helper images pulled and temporary fixture containers/network cleaned. Maven/Vitest not rerun for config-only changes. Scope/whitespace/config metadata validated; human cloud/ledger verification remains pending, Sprint [~], learning excluded, no push.

## Windows PowerShell Compose compatibility assistance (Order Service)

- **Tool:** OpenAI Codex
- **Date:** 2026-10-09
- **Mode:** Reproduction, test-first script fix, regression verification and documentation
- **Affected locations:** Order scripts/local-live.ps1, native output regression/fixture, CHANGE-089, active work/context/runbook/traceability/change log and ignored learning; this disclosure. No shared configuration, peer/application/FE source, database or cloud changes.
- **Prompt:** User supplied Setup NativeCommandError at Invoke-Compose on `Network foc_default Creating`, continuing the approved local-live implementation.
- **Key response:** Reproduced Windows PS5.1 treating harmless native stderr as terminating before exit-code inspection. Scoped Continue around Invoke-Compose, immediate exit capture and finally preference restoration preserve private stdout and genuine failure handling. Native Windows fixture red before fix, 11 assertions green afterward; 60 existing config/safety and 16 actual nginx/fixture regressions rerun successfully. No rebuild/database reset required; user reruns the same Setup command.
- **Author verification:** Existing ADR-029/CHANGE-088 authorizes the connector; this is an implementation detail, not an AI architecture decision or peer contract amendment. Source fingerprints match, context/whitespace/scope checked; generic drift/completion checker format/history failures remain separate. No full cloud Setup, Google push/auth/ledger or browser verification claimed, no Java/FE suites rerun for this host-script-only fix. Fixture containers/network cleaned; learned stderr is not equivalent to nonzero status. Human review and live gates pending, Sprint [~], learning excluded, atomic commits and no push.

## Live Credit push authentication diagnosis assistance (Order Service)

- **Tool:** OpenAI Codex
- **Date:** 2026-10-09
- **Mode:** Read-only runtime/source diagnosis and peer blocker documentation
- **Affected locations:** Order FEEDBACK-008 and Vincent active work; ignored learning; this disclosure. Credit source and local logs inspected read-only.
- **Prompt:** Ingress returns repeated 500 when cancelling an OPEN order; UI becomes cancelled but credits are not refunded. Diagnose and fix; then explain why separate Credit security authorization is needed.
- **Key response:** Matching Credit logs show the Pub/Sub service identity entering FirebaseRoleAuthoritiesConverter and User role-context, which rejects the non-Firebase token with 401; Credit throws and returns 500 before its refund handler. Push chain has a dedicated decoder but inherits the global Firebase authentication converter, consistent with official Spring Security 7.1.1 source. Proposed explicit independent push converter preserving all Google identity checks and ordinary user authorization. Initial unauthenticated 401 and proxy 404/405 are expected Check probes, not financial verification.
- **Author verification:** Vincent has not approved separate peer-code edits; his follow-up asks why, not approval. No Credit/Order application source, tests, database, cloud/IAM or container configuration changed; no refund success claimed. Tests and live ledger/retry recovery remain pending. Peer feedback preserves prior entries and Sprint [~]; developer/Annablee review required.

## One-time Credit push security repair assistance (Order Service / Credit Service)

- **Tool:** OpenAI Codex
- **Date:** 2026-10-09
- **Mode:** Approved narrow peer exception, test-first security repair, regression verification and handoff
- **Affected locations:** Credit SecurityConfig push converter and new PubSubPushSecurityTest only; Order CHANGE-090 interference Markdown, Vincent active work/profile/allocation, context, current sprint, traceability, feedback/change log and ignored learning; this disclosure. No other peer/source, frontend, consumer/financial logic, contract/schema, gateway/Compose/cloud/IAM or application database edits.
- **Prompt:** "I approve the fix, for this time but also pls create at md file to document the interference what u change at credit service"
- **Key response:** Isolated push authentication conversion from the global Firebase role converter while retaining existing Google decoder/signature/lifetime/issuer/audience/verified-service-account checks. User API role/identity checks remain intact. After correcting a test-fixture stub, pre-fix signed valid push alone failed through FirebaseRoleAuthoritiesConverter (14 other cases passed). Minimal converter selection then passes 15 signed/header-based security regressions. Full Credit verify rerun from clean source-only Java 21 Docker with isolated PostgreSQL: 80 tests, zero failures/errors/skips; fresh JaCoCo 95.48% lines (844/884), 85.56% branches (231/270), unchanged 80% gates. Package/OpenAPI export also pass. Source/test commit 198cd7c; requested interference record documents exact files, scope, security invariants, test limits, rebuild/rollback and owner handoff.
- **Author verification:** Vincent explicitly approved this one-time exception, not blanket Credit ownership or financial design changes. Actual filter chains and signed JWT validation tested with local JWKS; role/financial collaborators mocked in the security slice, not a live Google integration. Fresh XML evidence extracted; whitespace/scope/context syntax checked; D1/Overall fingerprints match, historical generic drift checker manifest-format failure remains separate. No running application rebuild/restart, cloud writes, ledger/refund success or DLQ redrive performed. FEEDBACK-008 is READY_FOR_VERIFICATION; Annablee review and real push/refund/transfer/duplicate/backlog checks pending. Sprint [~], delegated/background retries paused, learning excluded, atomic source/docs/disclosure commits and no push.

## Field-specific errors and new repost minimum assistance (Order Service)

- **Tool:** OpenAI Codex
- **Date:** 2026-10-09
- **Mode:** Approved implementation, tests, verification and documentation
- **Affected locations:** Order domain/application/CreateOrderRequest and related tests; shared Order request form/picker/manual controls/helpers/tests; CHANGE-091/ADR-030/context/evolution/sprint/contracts/diagram/traceability/active work; ignored learning and this disclosure. Peer source, shared auth/client, DB schema, timers and deployment unchanged.
- **Prompt:** Show only actual field errors; automatic and manual repost expiry need a 30-minute window. "Ucan safely ifnore them but upcoming repost plan u shud imeplemnt the constaint" for already-saved automatic plans.
- **Key response:** Preserved saved explicit-expiry plans; new automatic expiry >= scheduled due+30min and manual expiry >= submission+30min. Local creation checks before Supplier/Credit, existing field-detail envelope mapped inline. Late automatic execution still uses its saved future expiry. Tests-first missing-rule failures observed; fresh copied-source Java 21 wrapper verify passes 207 tests/no skips, 95.33% lines and 84.52% branches; 51 frontend tests pass, lint/typecheck/build pass with pre-existing warnings. Isolated PostgreSQL tests confirm legacy hydration/execution; no application DB reset.
- **Author verification:** Vincent explicitly approved the slice and grandfathering rather than AI-recommended disabling. Final patch and scope checked; human review and authenticated/responsive browser/live peer verification pending. Mandatory historical context rehydration was incomplete because large outputs were truncated; do not claim the full workflow completion gate passed. D1/selected Overall hashes match; historical generic checker issues not waived. All background retries/trusted credentials remain paused, Sprint [~]; learning/evidence ignored, atomic scoped commits, no push or application restart/cloud writes.

## Automatic repost trigger and requester visibility diagnosis assistance (Order Service)

- **Tool:** OpenAI Codex
- **Date:** 2026-10-09
- **Mode:** Read-only source/runtime diagnosis, focused existing test verification and documentation
- **Affected locations:** Vincent active work and ignored repost learning; this disclosure. Order/frontend source, compiled running scheduler/query, targeted application SELECTs and logs inspected without mutation.
- **Prompt:** Check whether automatic repost creates a new OPEN successor, hides the old EXPIRED request and updates the UI through polling.
- **Key response:** Source and running compiled scheduler invoke expiry/completion, not repostDue; inspected internal repost route has no scheduled caller. Real HTTP background delegated authorization remains FEEDBACK-005, paused/proposal-only. Target Test1.4 already has a different-ID OPEN manual successor and MANUAL_REPOST receipt at 16:52 Singapore, before auto due 17:00; used/link guards correctly prevent a second repost. Running requester filter and targeted SQL exclude its linked expired predecessor. Current frontend uses visible/authenticated 15-second replacement polling; 12 focused tests across four files passed.
- **Author verification:** No application/peer code, database, financial event, container, cloud or credential modification; no full backend suite or live automatic repost triggered. Browser inventory contains no connected surface, so actual authenticated DOM/network remains unverified. D1/Overall hashes match; complete historical context audit remains incomplete due to truncated records. Diagnosis does not resume paused background credentials/retries or choose their design. Human review and peer agreement required; Sprint [~], learning ignored, no push.

## Order-to-Credit data sufficiency and minimization audit assistance (Order Service)

- **Tool:** OpenAI Codex
- **Date:** 2026-10-09
- **Mode:** Read-only application/contract/runtime audit and documentation
- **Affected locations:** Order peer feedback section 7 and Vincent active work; ignored refund learning; this disclosure. Producer/consumer/HTTP/test/configuration source and runtime event field names inspected read-only.
- **Prompt:** "Check for every endpoint and event posting .... are we posting necessary information only to credit service ... not too much not to less just right for the credit service to be able to do their task"
- **Key response:** Active reservation/assignment/bodyless reset and browser provisioning/balance requests match required small shapes. Refund/completion v1 events contain all required financial/consistency/idempotency facts, but nine of fifteen nested Order fields are unused by Credit. This larger shape is previously approved, not silently changed. Completion overdue facts remain required/hash-bound/shared with User; Credit does not calculate penalties from them. Accepted-cancellation is User-only under ADR-025; local-live excludes Credit, older Credit consumer/provisioning remains FEEDBACK-007. Dormant HTTP settlement adapter has no provider route or production caller; actual completion uses Pub/Sub. Field-name-only runtime outbox SELECTs confirm all shapes; publication states do not prove financial completion.
- **Author verification:** No application/test/peer/schema/deployment/cloud changes, financial requests or message replays. Source/test-source/static field comparisons and read-only SQL only; no test suite or live ledger/subscription verification claimed. D1/Overall hashes match; complete historical context audit remains incomplete due to truncated output. Any payload reduction requires explicit user and peer contract/old-message compatibility agreement. Existing gaps/retry/auth pause and Sprint [~] retained; human review pending, learning excluded, no push.

## Simultaneous order acceptance inspection assistance (Order Service)

- **Tool:** OpenAI Codex
- **Date:** 2026-10-09
- **Mode:** Existing-source inspection, focused regression verification and handoff
- **Affected locations:** Order Vincent active-work record and this disclosure only; existing acceptance/domain/persistence source and tests inspected and executed unchanged.
- **Prompt:** "Have u handled that when multipl users accept at the same time ? If not pls add, is it just adding of this lock will do"
- **Key response:** Existing transactional acceptance obtains a PostgreSQL pessimistic row lock before validation and synchronous Credit assignment; no redundant lock added. Current code checks deadline both before and after Credit confirmation, not the earlier chat-only recommendation to check before only. Documented existing FEEDBACK-006 cross-service partial-success risk and absence of a real two-transaction race test. Java 21 offline focused run passed 33 tests with zero failures/errors/skips. No application source, implementation tests, peer contracts, deadline policy, schema or infrastructure changed.
- **Author verification:** Automated evidence is unit/mock regression only, not real PostgreSQL simultaneous acceptance, live Credit, browser, coverage or Sprint completion. Full historical context reads were truncated; no completed workflow gate claimed. Developer review pending; Sprint remains [~]. No application database, cloud or Git history changes; documentation left for review.

## Real PostgreSQL concurrent-transition tests assistance (Order Service)

- **Tool:** OpenAI Codex
- **Date:** 2026-10-09
- **Mode:** Test implementation, database verification and documentation
- **Affected locations:** OrderConcurrencyPostgresIntegrationTest; CHANGE-092; Order Vincent active-work, allocation/profile, context, traceability/change index; existing excluded learning document; this disclosure. No production, frontend, peer, migration or infrastructure edits.
- **Prompt:** "For this current branch make sure u implelent enoguh test to test the concurrency of this 4 scenarios .........But they are not yet fully verified by real simultaneous PostgreSQL tests. are there any other concurrency event that i forget to consider ?" Cross-service recovery explicitly deferred by Vincent to another branch.
- **Key response:** Eight cases invoke actual Spring services/PG15/Flyway in separate transactions, both winner orderings for accept/accept, cancel/expiry, accept/expiry and requester/scheduled completion. Gates plus pg_blocking_pids prove waiting; existing completion NOWAIT loser is checked via SQLSTATE 55P03 and safe next tick. Assert one final state/version, checkpoint/receipt/intent and at-most-one Credit assignment. Initial 7/8 run exposed fixture timestamp precision; persisted-deadline read-back corrected it without business changes. Focused 8/8 and full 215-test verify pass, zero failures/errors/skips. Fresh JaCoCo append=false: 95.67% lines, 84.70% branches, both 80% gates met. Other races identified but not claimed tested.
- **Author verification:** Vincent approved current-branch test scope, not new business/peer behavior. Peers and after-commit dispatcher mocked; persisted Order/outbox and locks real. No application database/cloud changes or live ledger/broker/browser claims; full historical context reads partly truncated. Generic workflow checks remain failing (drift manifest columns, record gate 3 format/history issues); manual document hashes match. Sprint remains [~], human patch review pending; learning not staged, no push. Reviewed passing test commit 1906afb and separate workflow commit 15869ee; disclosure committed independently.
