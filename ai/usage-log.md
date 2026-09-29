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

## Maven module conversion assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-09-29
- **Mode:** Architecture comparison, implementation, verification, documentation.
- **Affected locations:** Order Service POMs, module directories, Dockerfiles, README and persistent context; this disclosure.
- **Prompt:** "Then can do it here"
- **Key response:** Implemented the developer-approved eight-module Maven structure, preserving all relocated source/configuration/migration contents. Maven verification passed five tests without failures or skips; both Docker images built and passed isolated PostgreSQL readiness/profile checks. Shared CI path adjustment is prepared and awaits scope approval.
- **Author verification:** AI supported research/comparison of package-only and Maven-module organization. Yao Xiang selected the module architecture and explicitly authorized implementation. Human review of the final changes is pending.

## Maven module CI path correction assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-09-29
- **Mode:** Implementation, verification, documentation.
- **Affected locations:** `.github/workflows/ci.yml`; Order Service CHANGE-015, architecture-evolution/change-log and active-work records; this disclosure.
- **Prompt:** "yeah" (approval of the proposed three Order Service-specific CI path adjustments).
- **Key response:** Updated only Order Service's OpenAPI test search and OpenAPI/coverage upload paths. Executed the exact OpenAPI shell gate successfully, verified existing report paths, and passed git diff --check. Other service paths and CI triggers remain unchanged. Remote CI is pending commit/push.
- **Author verification:** AI supported research/comparison of the module layout and CI assumptions; Yao Xiang selected the architecture and explicitly approved this shared-workflow scope exception. Human review of the final diff is pending.
## Sequences 1-6 architecture review assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-09-29
- **Mode:** Document comparison and static source review.
- **Prompt:** "check the seq1 to 6 that assigned to me" and "Only bother those are in the seq 1 to 6 that assigned to me. Dont bother the seq 7 to 12 stuff."
- **Key response:** Compared the six assigned primary sequence diagrams with the supplied Extended Architecture PDF, D1 and approved architecture; identified model/identity, acceptance-time and authentication choices, plus conditional peer gaps in role claim issuance and creation compensation. Preserved checkpoint and validation requirements; no feature implementation or architecture approval inferred.
- **Affected locations:** Order Service active-work and peer-service feedback records; this disclosure. Application source and other services unchanged.
- **Author verification:** AI supported research/comparison only. Yao Xiang explicitly limited scope to sequences 1-6. Design choices and human review remain pending. Static peer inspection is not runtime integration verification.

## Sequences 1-6 prototype decisions assistance (Order Service)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-09-29
- **Mode:** Design clarification and documentation.
- **Affected locations:** Order Service ADR-009, CHANGE-016, architecture-evolution, context, in-scope sprint and peer-feedback records; this disclosure.
- **Prompt:** "1. split"; accept while open/unexpired, omit scheduled start time; "dont need to autehnticate Just foxus on making the apis"; "Store in domain layer"; "put dummy function that always return the intended thing".
- **Key response:** Recorded the user's choices and retained actual startedAt for duration measurement. Clarified that dummy creation-release success is not a real Credit effect. Asked whether authenticated Credit reservation and Supplier APIs should also be stubbed. No application implementation or tests changed; all eight source hashes matched.
- **Author verification:** Yao Xiang selected the model, timing, JPA placement and prototype exceptions. AI documented these choices and identified the remaining dependency-boundary question; broader stubs and runtime integration have not been approved or verified.

## Sequences 1-6 backend and frontend prototype assistance (Order Service / Frontend)

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-09-29
- **Mode:** Implementation, test generation, verification and documentation.
- **Affected locations:** Order Service domain/application/API/contracts/gateway modules, Maven dependencies, Flyway V2, tests and README; frontend Order pages/components/types, local proxy and Playwright tests; persistent ADR-009/CHANGE-016 context; this disclosure.
- **Prompt:** "Okie start doing now. When u do backend please do frontend as well"
- **Key response:** Implemented the core create/browse/accept/start/pickup/deliver prototype using the selected Errand/Order split, domain JPA, MapStruct, no scheduled availability time, deferred authentication and local dummy Credit/Supplier adapters. Maven verify passed 15 tests with zero failures/errors/skips and retained coverage gates. Frontend lint/typecheck passed (12 existing unrelated warnings); four desktop/mobile browser tests passed. Existing V1 database upgraded to V2 without reset. Optional repost-term storage and actual integrations remain unimplemented; full Sprint completion is not claimed.
- **Author verification:** AI supported research/comparison and implementation of the architecture selected by Yao Xiang. The developer approved the prototype and frontend scope. Final human review is pending; dummy integrations are not verified peer contracts. MapStruct setup checked against official documentation. Browser screenshots inspected; runtime behavior tested against local PostgreSQL. No sibling service changes or messages.

## Order Service Lombok and formatting assistance

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-09-29
- **Mode:** Refactoring, verification and documentation.
- **Affected locations:** Order Service prototype Java source/tests, domain/application/API POMs, README and CHANGE-017 context; this disclosure.
- **Prompt:** User requested annotation-generated accessors/constructors and spacing between imports, classes and methods instead of cramped handwritten boilerplate.
- **Key response:** Added Lombok getters, protected JPA no-argument constructors and equivalent plain constructors; retained business-state initialisation and computed getters. Avoided blanket lifecycle setters. Formatted prototype Java code. Maven verify passed all 15 tests and coverage gates; git diff --check passed. API, schema and business behavior unchanged; frontend unchanged.
- **Author verification:** AI checked official Lombok getter/setter and constructor documentation and implemented the requested refactor. Existing tests verify behavior; no new tests were needed for formatting and equivalent boilerplate generation. Final human review pending. Prior prototype limitations remain.

## Order Service typed statuses and explicit local types

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-09-29
- **Mode:** Refactoring, verification and documentation.
- **Affected locations:** Order Service domain, application, API contracts/controller and tests; README and CHANGE-018 context; this disclosure.
- **Prompt:** User requested status enums for safety and explicit Java types instead of var throughout Order Service.
- **Key response:** Converted Errand string status and repository query parameters to an enum; retained existing Order/Checkpoint enum. Added contract-owned response enums with unchanged JSON values. Replaced var declarations in Java source/tests and documented the convention. Kept initialisation and business transition guards. Maven verify passed all 15 tests and coverage gates; generated OpenAPI enum values and absence of var verified; git diff --check passed.
- **Author verification:** Yao Xiang requested the enum/type convention. AI implemented and verified this refinement using existing persistence/API/domain regression tests. Human diff review pending. No real integration or broader prototype completion claimed.

## Order Service result/error pattern review

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-09-29
- **Mode:** Advisory document and source review.
- **Prompt:** User supplied API Response and Error Handling Flow and raised resource/debugging concerns about service exception rethrows.
- **Affected locations:** Order Service active-work record; this disclosure. Application code unchanged.
- **Key response:** Compared current OrderProblem/global-handler flow with explicit results. Recommended plain error values for expected failures, retained unexpected exceptions and rollback guarantees, and identified public response-envelope incompatibility requiring a user decision. Checked official Java Throwable and Spring rollback documentation. No runtime tests performed.
- **Author verification:** Proposed design only; user choice on public JSON envelope pending. Attached examples are reference material, not independent authority to replace the existing API contract.

## Order Service explicit result flow implementation

- **Tool:** OpenAI Codex (GPT-6)
- **Date:** 2026-09-29
- **Mode:** Implementation, test generation, verification and documentation.
- **Affected locations:** Order Service domain, application, API/contracts, prototype Supplier adapter and tests; README and CHANGE-019/ADR-009 context; this disclosure.
- **Prompt:** "okie can" approving BaseService/result wrappers internally while preserving public JSON.
- **Key response:** Added exclusive success/failure ResultWrapper and BaseService helpers. Converted OrderProblem from exception to immutable enum-coded error data; domain operations return rejection values before mutation. Services return expected failures without throwing. API mapping preserves existing JSON/statuses. Explicit rollbackError marks late failure transactions; unexpected exceptions propagate, get logged and produce a generic error. Credit compensation remains registered on transaction rollback.
- **Verification:** ResultWrapper test initially failed compilation before implementation. Final Maven verify passed 25 tests with zero failures/errors/skips and unchanged coverage gates. PostgreSQL tests cover early error without rollback, explicit rollback after managed mutation, unexpected-exception rollback, successful commit and failed-insert rollback with dummy credit release. Existing six-flow API/OpenAPI tests pass; no var or thrown OrderProblem remains; git diff --check passes. Frontend unchanged and not retested.
- **Author verification:** Yao Xiang approved the internal-result design and keeping the current wire format. AI implemented and tested it, including correcting a test failure-injection setup. Human review pending. Existing authentication, real peer integration and optional repost limitations remain; no broader feature completion claimed.
