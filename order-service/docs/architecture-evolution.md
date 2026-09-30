# Order Service Architecture Evolution

This is the persistent register for architecture and design discoveries made while implementing the Order Service. It complements, but does not replace, `docs/change-log.md`, `changes/`, ADRs, specifications, diagrams, contracts, traceability, or Git history.

Read this file at the beginning of every implementation-affecting turn. Follow the latest approved and effective design, not an original design that a later entry supersedes. Chat history is not authoritative.

## Baseline

The initial approved context is recorded in:

- `docs/ai-project-context.md`
- `docs/architecture-overall.md`
- `docs/architecture-order-service.md`
- `docs/decisions/`
- `docs/service-contracts.md`
- `docs/peer-service-api-feedback.md`
- the source and Sprint diagrams fingerprinted in `docs/project-d1-reference.md`

ARCH-EVO-001 records the user-supplied correction to the Order Service high-level diagram's application/domain/outbound-port control flow. Existing ADRs remain effective according to their own status and supersession rules.

## Change classification

Classify every implementation discovery before acting on it.

### Implementation detail

An internal mechanical choice that does not change approved requirements, externally visible behavior, service or data ownership, contracts, security, control flow, data flow, consistency, or architecture. A safe implementation detail may be implemented within the approved design and recorded through the normal change/TDD workflow.

### Design refinement

An internal class, method responsibility, validation step, persistence detail, internal interaction, or test-fixture adjustment that preserves intended external behavior and approved boundaries. Document every material refinement in this register and the related change record. Obtain approval when it changes an approved class/sequence responsibility or when its classification is uncertain.

### Architecture or specification change

A change to requirements, user-visible behavior, service boundaries, database ownership, public contracts, cross-service interactions, synchronous/asynchronous communication, event behavior, security/authorization, consistency/reliability, major component responsibilities, or sequence interactions. Explicit user approval is required before implementation.

The AI's recommendation is never approval. If classification is disputed or ambiguous, use the stricter class and stop for a decision.

## Required proposal before a material design change

Show:

- the original requirement or design;
- the implementation discovery and why the current design is insufficient;
- what may be added, changed, or removed;
- at least two viable solutions where appropriate;
- advantages, disadvantages, and trade-offs involving complexity, maintainability, performance, consistency, testing, and future change;
- the recommended solution and reason;
- all affected requirements, architecture, class/sequence diagrams, data model, contracts, tests, and source files;
- the proposed classification and whether approval is required.

Ask the user to approve, reject, or modify every architecture/specification change before implementing it. Do not change application code, implementation tests, contracts, or diagrams to embody an unapproved proposal.

## Evolution index

| Evolution ID | Date | Feature | Change type | Status | Current rule or outcome | Decision/change links | Supersedes |
|---|---|---|---|---|---|---|---|
| ARCH-EVO-001 | 2026-09-28 | High-level internal control flow | Design refinement | IMPLEMENTED | Application components invoke outbound ports; domain rules return decisions/data | CHANGE-012 | Previous diagram revision `875238E1...` |
| ARCH-EVO-002 | 2026-09-29 | Order Service persistence and deployment | Architecture or specification change | APPROVED | Order Service uses PostgreSQL on one Cloud SQL instance and deploys to Cloud Run with a public-IP Cloud SQL Java Connector | CHANGE-015 / ADR-008 | Unresolved PostgreSQL/Firestore and Kubernetes/Cloud Run conflict |
| ARCH-EVO-003 | 2026-09-30 | Unified Order dashboard and authenticated actor identity | Architecture or specification change | APPROVED | Shared dashboard exposes requester and courier functions without a client-side mode switch; Order uses User Service-confirmed actor IDs; supplier labels never flash opaque IDs | CHANGE-029 | Previous mode-switching frontend slice in CHANGE-022 |
| ARCH-EVO-004 | 2026-09-30 | Creation-time-only automatic repost choice | Architecture or specification change | IMPLEMENTED | Automatic repost is selected atomically during order creation; `OPEN` orders are read-only; manual repost is available only for un-reposted `EXPIRED` orders | CHANGE-031 | Post-creation configuration wording in CHANGE-022 |

Use stable `ARCH-EVO-NNN` identifiers. The detailed entry and its linked ADR/change record together preserve the decision history; do not copy full ADR contents into this table.

## Entry template

```markdown
## ARCH-EVO-NNN: <title>

- Change ID:
- Date:
- Developer:
- Feature:
- Original design or requirement:
- Problem discovered:
- Change type: Implementation detail / Design refinement / Architecture or specification change
- Status: PROPOSED
- Approved change:
- What was added:
- What was changed:
- What was removed:
- Why the change was necessary:
- Alternatives considered:
- Trade-offs:
- Affected architecture:
- Affected class diagram:
- Affected sequence diagram:
- Affected data model:
- Affected contracts:
- Affected tests:
- Affected source files:
- Approved by:
- Approval date:
- Effective from:
- Implementation status:
- Supersedes:
- Superseded by:
- Related ADR/override:
- Related traceability:
```

Allowed evolution statuses are `PROPOSED`, `APPROVED`, `IMPLEMENTED`, `REJECTED`, `SUPERSEDED`, `REVERTED`, and `ROLLED_BACK`.

- `APPROVED` permits the recorded change but does not claim that all artifacts or code are updated.
- `IMPLEMENTED` requires the approved design, affected documentation, traceability, tests, and implementation to be synchronized and verified.
- `SUPERSEDED` links to the newer effective entry while retaining the original history.
- `REVERTED` withdraws the decision; `ROLLED_BACK` means an applied implementation was undone and requires verification of the restored design.

## ARCH-EVO-001: Correct application/domain/outbound-port flow

- Change ID: CHANGE-012
- Date: 2026-09-28
- Developer: Vincent
- Feature: Order Service high-level internal architecture
- Original design or requirement: The prior diagram fingerprint and Markdown summary did not preserve the corrected direction between application components, Order-owned domain rules, and outbound ports.
- Problem discovered: The revised authoritative diagram explicitly states that domain rules return decisions while application components invoke outbound ports.
- Change type: Design refinement
- Status: IMPLEMENTED
- Approved change: Treat the user-corrected diagram as the current authoritative revision and persist its clarified dependency direction.
- What was added: Explicit application-to-domain, domain-to-application, application-to-port, and port-to-application control/data-flow rules.
- What was changed: Diagram fingerprint metadata and the architecture/context summaries.
- What was removed: The previous diagram fingerprint from the current-source row; it remains recorded here as superseded history.
- Why the change was necessary: Future implementation must not place repository or peer-service calls inside Order-owned domain rules.
- Alternatives considered: Leave the Markdown summaries implicit; rejected because future turns could reconstruct the old or ambiguous dependency direction.
- Trade-offs: Slightly more prescriptive internal layering in exchange for clearer separation, testability, and dependency inversion. No external behavior or contract changes.
- Affected architecture: `docs/architecture-order-service.md` and `docs/ai-project-context.md`.
- Affected class diagram: None; existing logical class responsibilities remain unchanged.
- Affected sequence diagram: None.
- Affected data model: None.
- Affected contracts: None.
- Affected tests: None; no implementation exists and no application behavior changed.
- Affected source files: None.
- Approved by: User-provided corrected authoritative diagram
- Approval date: 2026-09-28
- Effective from: Diagram SHA-256 `BB092EC12C895CC31ED1674F595FEFDAE36E20C2769FFFEF7BC1D20631ADB8B7`
- Implementation status: Source fingerprint and affected Markdown context synchronized; no application implementation was required.
- Supersedes: Diagram revision SHA-256 `875238E1A69A37245052DD6BE2E9167C39851B1EF566FF6A9F84B4DB7B6EA210`
- Superseded by: None
- Related ADR/override: None; this is a source-diagram design refinement, not a contract or product amendment.
- Related traceability: `docs/project-d1-reference.md`

## ARCH-EVO-002: Approve Order Service Cloud SQL and Cloud Run

- Change ID: CHANGE-015
- Date: 2026-09-29
- Developer: Vincent (Developer 2)
- Feature: Order Service persistence and deployment foundation
- Original design or requirement: Order Service design sources named PostgreSQL/Kubernetes while parent repository defaults named Firestore/Cloud Run.
- Problem discovered: The conflicting persistence/deployment authorities prevented a concrete Cloud SQL or Cloud Run setup.
- Change type: Architecture or specification change
- Status: APPROVED
- Approved change: Use PostgreSQL on one Cloud SQL instance with separate `order_staging` and `order_production` databases; deploy Order Service to Cloud Run using a public-IP Cloud SQL Java Connector.
- What was added: Cloud SQL instance/database/IAM/Secret Manager provisioning direction, Cloud Run connection direction, backup/PITR/deletion-protection requirements.
- What was changed: Order Service-specific effective persistence and deployment authority.
- What was removed: The unresolved conflict as a blocker for Order Service infrastructure.
- Why the change was necessary: The user selected the concrete cloud runtime and relational persistence needed for the service.
- Alternatives considered: Separate instances (more isolation/cost); private IP/VPC (more isolation/complexity); direct public connection (rejected).
- Trade-offs: Shared instance lowers cost but couples environments; public connector simplifies operations but is less isolated than private VPC networking.
- Affected architecture: `docs/architecture-order-service.md`, `docs/ai-project-context.md`, `docs/ai-project-context.toml`.
- Affected class diagram: None.
- Affected sequence diagram: Deployment/infrastructure configuration only; business sequence diagrams unchanged.
- Affected data model: Cloud database boundary approved; application schema and migration tool remain separately gated.
- Affected contracts: None.
- Affected tests: Infrastructure verification to be added; application tests unchanged.
- Affected source files: `infra/gcp/bootstrap.sh`, `scripts/ci/check-infra.sh`, environment/deployment configuration.
- Approved by: User
- Approval date: 2026-09-29
- Effective from: Approval of ADR-008
- Implementation status: Approved; repository synchronization and cloud provisioning are pending/underway.
- Supersedes: The unresolved PostgreSQL/Firestore and Kubernetes/Cloud Run conflict recorded by ADR-003.
- Superseded by: None
- Related ADR/override: `docs/decisions/ADR-008-order-service-cloud-sql-cloud-run.md`
- Related traceability: `docs/project-d1-reference.md`; no Sprint business requirement changed.

## ARCH-EVO-003: Unified Order dashboard and authenticated actor identity

- Change ID: CHANGE-029
- Date: 2026-09-30
- Developer: Vincent (Developer 2)
- Feature: Shared Order Service dashboard and acceptance authorization
- Original design or requirement: The earlier frontend slice included a client-side Requester/Courier mode switch, and Order application services accepted request-body actor IDs after a role/eligibility check.
- Problem discovered: The user requested one dashboard exposing both functions, and the actual User Service role endpoints derive identity from the bearer token rather than trusting the forwarded `X-User-Id`. Supplier lookup also caused a transient opaque-ID flash.
- Change type: Architecture or specification change
- Status: APPROVED
- Approved change: Remove the mode-switch control, expose both functions through shared navigation, use provider-confirmed identities for Order operations, filter self-orders from the browse presentation, and hold cards until Supplier lookup settles.
- What was added: Authenticated identity return values from the Order UserServicePort and adapters; stable supplier-label readiness handling.
- What was changed: Frontend route gating and mode persistence were removed; Order application callers now use the verified identity.
- What was removed: Client-side mode-switch UI and transient supplier-ID fallback.
- Why the change was necessary: It matches the user's chosen dashboard behavior and closes the actor-identity gap without modifying peer services or public Order contracts.
- Alternatives considered: Keep the switcher; add a new Order response containing supplier details; or keep rendering IDs until lookup completion. Those options were rejected for user experience, ownership, or security reasons.
- Trade-offs: The unified dashboard exposes more navigation at once, so backend authorization must remain authoritative. Waiting for lookup completion can briefly show skeletons, but avoids misleading identifiers.
- Affected architecture: Shared frontend role/function presentation and Order-side authorization boundary.
- Affected class diagram: UserServicePort now returns a verified identity; no public class ownership changed.
- Affected sequence diagram: Acceptance and actor-sensitive flows now bind the actor to the authenticated User Service identity.
- Affected data model: None.
- Affected contracts: No peer contract changed; existing User Service response fields are consumed by the adapter.
- Affected tests: Existing self-acceptance domain test retained; frontend Order-card coverage extended. Runtime and contract verification remain pending.
- Affected source files: `frontend/`, `order-service/src/main/java/sg/edu/nus/foc/order/application/`, `order-service/src/main/java/sg/edu/nus/foc/order/adapter/`, and `order-service/src/main/java/sg/edu/nus/foc/order/api/OrderController.java`.
- Approved by: Vincent
- Approval date: 2026-09-30
- Effective from: CHANGE-029 source update
- Implementation status: Source changes applied; full test/runtime verification pending.
- Supersedes: Mode-switch portion of CHANGE-022
- Superseded by: None
- Related ADR/override: None; this is a user-approved frontend/security refinement.
- Related traceability: `docs/requirements-traceability.md`, Sequence 3 and cross-cutting notes.

## ARCH-EVO-004: Creation-time-only automatic repost choice

- Change ID: CHANGE-031
- Date: 2026-09-30
- Developer: Vincent
- Feature: NTH4 automatic and manual reposting
- Original design or requirement: NTH4 permits an automatic repost plan for an unaccepted order and a requester-reviewed manual repost after expiry. The source context did not explicitly define post-creation mutability.
- Problem discovered: The earlier frontend and configure route allowed an `OPEN` order to be enabled, disabled, or edited after creation through a second request.
- Change type: Architecture or specification change
- Status: IMPLEMENTED
- Approved change: The developer clarified and approved that the automatic-repost checkbox and all plan details are selected during creation; an order cannot be changed later. Only an eligible `EXPIRED` order without a repost may show the manual repost action.
- What was added: Create-order repost fields, immutable read-only display, and tests for creation-time persistence and rejected later configuration.
- What was changed: Order creation stores the plan; the legacy configure route returns a conflict; the frontend no longer offers post-creation automatic-plan editing.
- What was removed: The `OPEN`-order configure interaction and its second-request mutation behavior.
- Why the change was necessary: It matches the clarified product behavior and prevents a user from changing the repost decision after an order is already published.
- Alternatives considered: Keep editable settings until expiry; add a separate disable-only command; or make the creation choice immutable. The approved immutable choice preserves predictable lifecycle semantics and requires no new command state.
- Trade-offs: A mistaken creation choice requires manual repost after expiry rather than editing the live order; the create request is slightly wider, but the lifecycle is deterministic and idempotent.
- Affected architecture: Order creation/repost application boundary and frontend request lifecycle.
- Affected class diagram: `Order` owns the creation-time `RepostPlan`; no new aggregate or peer ownership.
- Affected sequence diagram: Sequence 1 carries the optional plan; Sequence 10 reads it after expiry; Sequence 11 remains requester-reviewed manual repost.
- Affected data model: Existing embedded repost-plan columns are reused; no migration.
- Affected contracts: `POST /api/orders` gains optional repost fields. The legacy configure route remains but rejects mutation with conflict.
- Affected tests: Domain and repost-service tests updated; frontend payload/validation tests updated.
- Affected source files: Order API/application/domain classes and shared frontend create/repost controls.
- Approved by: Vincent
- Approval date: 2026-09-30
- Effective from: CHANGE-031 implementation
- Implementation status: Code and records synchronized; runtime/test gates pending.
- Supersedes: Post-creation configuration interpretation in CHANGE-022
- Superseded by: None
- Related ADR/override: None; this is a developer-approved clarification of NTH4 behavior.
- Related traceability: `docs/requirements-traceability.md`, Sequences 1, 10, and 11.

## ARCH-EVO-005: Temporary Credit outcome stub boundary

- Change ID: CHANGE-032
- Date: 2026-09-30
- Developer: Vincent
- Feature: Credit settlement/release for completion, cancellation, expiry, and automatic repost
- Discovery classification: Architecture or specification change, explicitly approved as a temporary Sprint 1 exception
- Status: APPROVED (temporary; provider implementation pending)
- Approved change: Order Service may invoke a validating no-op `CreditServicePort` for local verification while Credit Service implements the provider-owned settlement and release endpoints. The real boundary remains synchronous and Credit-owned.
- Proposed contracts: `POST /api/credits/orders/{orderId}/settlement` and `POST /api/credits/orders/{orderId}/release`, with command identity, order/requester identity, amount, outcome where applicable, and expected order version. Exact response/error/authentication fields require Credit owner agreement.
- Security decision: lifecycle calls must first pass the Order-owned lifecycle-token check; an explicit bearer-form internal credential is forwarded to automatic peer calls. Provider acceptance of that credential is not yet verified.
- Trade-offs: The mock allows Order state-machine and UI work to proceed, but cannot verify balances, ledger transfers, or peer error semantics. Sequences 7-9 remain `[~]`.
- Alternatives considered: block all outcome flows; invent Order-owned credit mutations; or introduce a broker. The approved temporary stub preserves Credit ownership and the synchronous boundary without inventing peer behavior.
- Affected architecture chain: Order application port/adapters, completion/cancellation/expiry/repost sequences, peer API feedback, traceability, tests, and active-work records. No peer source or schema changed.
- Exit criteria: Credit owner agrees and implements both endpoints; actual peer code and tests are re-read; contract/idempotency/authentication/error tests pass; mock is removed or disabled for the verified integration.
- Related records: `changes/CHANGE-032-credit-outcome-stub-exception.md`, `docs/peer-service-api-feedback.md` FEEDBACK-001.

## Supersession and synchronization

When a newer rule is approved:

1. Mark the old entry/rule `SUPERSEDED` and link the new evolution record.
2. State the new current rule and effective date.
3. Update every future-work navigation point that could otherwise select the old rule.
4. Preserve the original text and history.

After approval, inspect and update this chain:

```text
Requirement
-> Architecture diagram
-> Class diagram
-> Sequence diagram
-> Data model
-> Contract
-> Test
-> Implementation
```

Update each affected artifact or record an explicit blocker. A feature cannot be complete when implementation follows a changed design while any approved documentation still describes the superseded design.

## Required response after a design or implementation change

Use the per-turn response contract in `docs/completion-reporting.md`. Under `Design Decisions`, include the discovery classification, approval status, approver when known, current effective rule, alternatives, and trade-offs. Under `Affected Artifacts`, identify every applicable link in the synchronized design chain. Report stale artifacts, pending approval, blockers, and required human review under `Remaining Issues`.

The required report is an audit baseline, not the entire response. Add meaningful architecture-proposal, migration, compatibility, rollback, decision, or next-step sections when the current scenario benefits from them. Do not create a separate Markdown file only for the response; update the persistent records that actually changed.
