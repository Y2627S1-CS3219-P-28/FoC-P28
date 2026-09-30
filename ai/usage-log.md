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
