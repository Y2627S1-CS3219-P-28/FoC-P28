# Instruction Map

| Path | Scope | Notes |
|---|---|---|
| `../AGENTS.md` | Parent multi-service repository, including Order Service | Repository-wide stack, API, security, testing, deployment, Git, and service conventions; applies together with the service instructions |
| `AGENTS.md` | Entire Order Service repository | Service-specific authority, allocation, architecture-approval, peer-integration, TDD, completion, and change-control rules |
| `docs/architecture-review-playbook.md` | Order Service feature-level architecture reviews | Operational checklists and templates required before and after architecture approval; it does not approve a design |
| `docs/architecture-evolution.md` | Discoveries made during Order Service design or implementation | Change classification, material proposal/approval, current/superseded design history, artifact synchronization, and evolution-specific completion-report details |
| `docs/database-migration-workflow.md` | Every Order Service schema-affecting change | Versioned migration files, independent local database synchronization, peer handoff, migration verification, and schema-conflict rules |
| `docs/completion-reporting.md` | Final response for every Order Service chat turn | Required audit sections plus meaningful scenario-specific headers/details for advisory, status, planning, partial, blocked, decision, workflow, design, implementation, test, and verification turns |
| `learning/` | Beginner-friendly, project-specific and transferable engineering learning | Reuse existing learning documents; explain general principles and current-project implementation, trade-offs, failure modes, verification, and traceability links |
| `docs/peer-service-api-feedback.md` | Missing, unsuitable, incomplete, or incompatible peer-service APIs | Single append-only feedback lifecycle, blocked stopping point, responsible peer action, and actual-implementation verification before resumption |
| `docs/frontend-integration-workflow.md` | Order Service work with shared frontend impact | Approved Next.js/client/role context, live inspection, shared-file safety, responsive vertical-slice gate, tests, completion, and response format |
| `docs/frontend-ui-style-guide.md` | All future Order Service UI proposals and implementation | Screenshot-informed Friend on Campus visual baseline reconciled with live Geist/shadcn/Lucide tokens; consistency gate without inventing product behavior |
| `docs/ai-usage-format.md` | Required Order Service entries in `../ai/usage-log.md` | Established task heading, bold field labels, prompt/key-response/affected-location/author-verification format; no competing schema |
| `docs/event-candidates.md` | Order Service event/interaction proposals | Stable D1 candidate IDs, separate decision IDs, comparison requirements, and proposal-only status |
| `../frontend/AGENTS.md` | Shared top-level frontend | Version-specific Next.js and frontend conventions; read before every coding turn and frontend edit together with root/Order instructions |

No nested `AGENTS.md` files exist inside `order-service/`. The shared sibling `../frontend/AGENTS.md` applies whenever an approved Order Service vertical slice affects frontend source. Add an Order Service nested file only after a local source/test/deployment subtree exists and needs rules not already covered here; register it in this map.

The parent project `../AGENTS.md` requires Java 21/Spring Boot 4.1.1, Firestore defaults, Docker/Cloud Run conventions, OpenAPI, Firebase authentication, and repository-wide tests. ADR-008 records the user-approved Order Service exception: PostgreSQL on Cloud SQL and Cloud Run. The parent file was not modified; sibling-service Firestore conventions remain unchanged.
