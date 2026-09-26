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

## 2026-09-26 — Disclosure implementation

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

## 2026-09-26 — FR-based commit organization

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
