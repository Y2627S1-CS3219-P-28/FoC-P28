# Order Service Requirements Traceability

## CHANGE-102 / ADR-034: merged minute orchestration

| ID | Requirement | Acceptance criteria | Design reference | Implementation reference | Test reference | Status |
| --- | --- | --- | --- | --- | --- | --- |
| SCH-REC | F3/F4.1.1/F13; approved orchestration amendment | Recover eligible pending commands before expiry; never bypass acceptance deadline or unresolved guard | ADR-033/034; effective minute sequence | OrderLifecycleScheduler -> OrderCommandService -> existing lifecycle workers | OrderCommandSchedulerTest; OrderCommandRecoveryIntegrationTest (valid, compensated, unresolved acceptance) | IN_PROGRESS |
| SCH-EXP | F4.1.8/F10; OPEN expiry | Still OPEN, unassigned, deadline reached: expiry follows recovery and queues one refund | ADR-026/034 | processDueOrders -> expireDue; unchanged locks/outbox | Recovery integration; existing expiry/concurrency regressions | IN_PROGRESS |
| SCH-COMP | F4.1.5/F5.1; automatic completion | Still DELIVERED, latest delivery >=48h: completion follows expiry | ADR-020/026/034 | processDueOrders -> autoCompleteDue; unchanged item transactions | OrderLifecycleSchedulerTest; existing completion/concurrency regressions | IN_PROGRESS |
| SCH-ISO | NFR3/NFR4; scheduling isolation | Single minute owner, lifecycle timestamp after recovery, error logging/isolation; outbox remains 15min | ADR-034 | OrderLifecycleScheduler; independent OrderOutboxScheduler | Scheduler ownership/order/time/failure tests; OrderSchedulerCadenceTest | IN_PROGRESS |

Local evidence: 28 focused and 301 full backend tests pass with no failures,
errors or skips; 95.60% line / 81.95% branch coverage. PostgreSQL is real; Credit
is stubbed. Status remains IN_PROGRESS until broader workflow/live gates pass.
The scheduler amendment changes orchestration only. No new financial/auth/API,
frontend, migration, topic or infrastructure contract; Sprint remains `[~]`.

## CHANGE-100 / ADR-033: foreground command recovery stub

| ID | Requirement | Acceptance criteria | Design reference | Implementation reference | Test reference | Status |
| --- | --- | --- | --- | --- | --- | --- |
| REC-CREATE | F1/F2 + ADR-033 | Commit immutable intent/candidate before reservation; one OPEN and saved terminal result after confirmed success | ADR-033; concurrency/concurrency-order-credit.md | OrderCommandService; OrderCommandStore; OrderCreationService.createConfirmed | OrderCommandRecoveryIntegrationTest: intent barrier, lost response, local rollback, insufficient funds | TESTED_STUB; live blocked |
| REC-ACCEPT | F3/F4.1.1/F13 + ADR-033 | Same/different-key competitors cannot produce multiple assignments; preserve deadline and reverse invalid remote success | ADR-033; existing Order acceptance rules | Command claims/guard; OrderAssignmentService.acceptConfirmed; LocalCreditCommandStub | Recovery integration: concurrent claim, expiry compensation, competing cancel/expiry | TESTED_STUB; live blocked |
| REC-ABORT | F11.2 + ADR-025/033 | Accepted-only reset confirmed before one immutable attempt + same-ID OPEN/EXPIRED + approved outbox | ADR-025; ADR-033 | OrderTransitionService.abortConfirmed; existing history/outbox | Recovery integration: successful three-action flow, unknown abort blocks start; CreditCommandStubTest old reset replay | TESTED_STUB; live blocked |
| REC-AUTH | ADR-010/033 | Owner/immutable input binding; unknown then 401 stays pending; no stored token; owned past read does not require new-task eligibility | ADR-033 / handoff authorization | OrderCommandService; OrderCommandController; existing JWT security | OrderCommandGateTest; OrderCommandSecurityTest; recovery authorization test | TESTED_STUB; real peer auth blocked |
| REC-LEASE | NFR3 + ADR-033/034 | Only due, unclaimed/expired-lease work is recovered; stale generation cannot finalize/compensate | ADR-033/034; V5 constraints | OrderCommandStore; first shared minute phase; protocol stub | Recovery PG claims/generation tests; stub fencing test; OrderCommandSchedulerTest | TESTED_STUB; provider fence blocked |
| REC-UI | ADR-033 + NFR3 | Persist frozen key before POST, retain on timeout/reload, disable pending action, Continue only for authorization, account isolation | ADR-033; frontend integration/style guide | useOrderCommand; IndexedDB storage; PendingOrderCommands; existing routes/actions | Hook 7 cases; pending panel 3 cases; full frontend regression | RTL/build tested; browser gate open |
| REC-DB | ADR-008/016/033 + NFR3 | Versioned V5 migration, clean/upgrade, preserved previous orders/history/outbox | Database migration workflow; V5 handoff | V5__durable_foreground_commands.sql | OrderOutboxMigrationTest; real PostgreSQL recovery suite | PostgreSQL tested; consuming DB handoff pending |

Full backend verification: 298 tests, 0 failures/errors/skips; JaCoCo 95.76%
lines and 81.82% branches. This is executed Order/contract-stub evidence, not
verified Credit historical persistence, live broker delivery or hosted-browser
completion. FEEDBACK-010 extends 003/006; Sprint remains `[~]`.
Earlier concurrency tests now prove caught NOWAIT 55P03 and isolate per-case
state, preserving current per-order lifecycle isolation rather than requiring
an obsolete blocking lifecycle batch. All ten legacy race cases pass.

## CHANGE-093: Courier acceptance versus requester OPEN cancellation

| D1 / approved invariant | Existing implementation | Simultaneous verification |
| --- | --- | --- |
| F3/F13 + F4.1.7: one valid OPEN transition | OrderAssignmentService.accept / OrderTransitionService.cancel / same getForUpdate row lock | acceptanceAndRequesterCancellationCommitOnlyOneOutcome, both winner orderings |
| NFR3: genuine contention and no duplicate consequences | Actual Spring services, PostgreSQL 15/Flyway; peers/dispatcher mocked | Blocking PID required; loser CONFLICT; one version/checkpoint/winning receipt; Credit assigned only for ACCEPTED; refund/dispatch only for CANCELLED |

Focused new pair: 2 passed; full five-race class: 10 passed; acceptance/service
regression selection: 13 passed (combined run 23, zero failures/errors/skips).
Separate actual domain/transition regression: 36 passed, zero failures/errors/
skips. Total regression evidence is 10 race cases plus 49 related tests, not a
fresh full-suite/coverage claim. CHANGE-093/test commit 827844b; no production/
peer/schema/frontend change. Generic workflow-format and live integration gates
remain open; overall Sprint [~].

## CHANGE-092: Real PostgreSQL concurrent transitions

| D1 / approved invariant | Existing implementation | New simultaneous test |
| --- | --- | --- |
| F3/F13: one courier can accept; row lock before Credit | OrderAssignmentService.accept / getForUpdate | Two couriers, each winner ordering; loser conflict and no Credit assignment |
| F4.1.7, F4.1.8/F10: cancellation or expiry, not both | OrderTransitionService.cancel / LifecycleProcessingService.expireDue | Each winner ordering; exactly one terminal checkpoint and refund intent |
| F3, F4.1.8/F10: acceptance excludes expiry | Assignment / due-unassigned pessimistic query | Each winner ordering; no expired assigned row or duplicate outcome |
| F4.1.5/F5.1, ADR-020: one completion at requester or 48-hour tick | Complete / autoCompleteDue | Each winner ordering; one completion receipt/checkpoint/event; safe next tick |
| NFR3: genuine persistence verification | PostgreSQL 15, Flyway V1-V4, independent transactions | OrderConcurrencyPostgresIntegrationTest; pg blocking evidence / explicit NOWAIT 55P03 |

Eight focused cases pass, zero skips; full verification reruns all eight and
passes 215 tests/no failures/errors/skips. Fresh coverage: 95.67% lines/84.70%
branches. Peers/delivery mocked, results in CHANGE-092; no live ledger/broker/
Sprint completion claim. Production/contract/schema/frontend behavior unchanged.
## Lifecycle per-order failure isolation — CHANGE-093 (2026-10-09)

Yao Xiang explicitly requests failed scheduled tasks be skipped while later successes continue. Due selection returns IDs filtered in the DB (latest delivery cutoff for completion), without locking a whole batch. Nontransactional lifecycle coordinator calls fresh NOWAIT-locking per-order transactions (expiry worker / existing autoComplete with REQUIRES_NEW), catches each RuntimeException including commit failures, logs order ID, counts only successful transitions, then continues. Scheduler retains independent whole-pass catches. Failed orders remain eligible next normal lifecycle pass; no new repost retry mechanism or cron/contract/schema/peer change. Previous batch transaction description is superseded by this refinement; CHANGE-092 compact payloads and FEEDBACK-009 remain unchanged.


## Effective compact-event amendment — CHANGE-092 / ADR-031 (2026-10-09)

Yao Xiang approved the exact seven-field Order-only payload: eventId, eventType, orderId, orderStatus, creditAmount, occurredAt, courierId. This replaces full snapshots and overdue facts ONLY for OpenOrderRefundTaskEvent and OrderCompletionTaskEvent. AcceptedOrderCancellationTaskEvent retains its existing v1 envelope/snapshot. Internal outbox versions and Pub/Sub eventVersion attribute remain (compact schema v2); topic names and DB schema unchanged. Old pending snapshot rows normalize at dispatch from their saved facts, with stable IDs. Lifecycle/outbox scheduling, locks, synchronous Credit assignment/reset and ADR-025 abort behavior remain. Historical v1 descriptions below are superseded for these two bodies. Credit currently requires the old snapshot and overdue; FEEDBACK-009 is INCOMPLETE_OR_INCOMPATIBLE. User completion penalties need an agreed separate overdue source. User approved implementing Order-only and documenting peer work; live integration remains blocked, Sprint [~].


## CHANGE-091: Actual field errors and new repost minimum

| Requirement / amendment | Classes / UI / contract | Tests / verification |
| --- | --- | --- |
| F1.1-F1.3, NFR2: explain only invalid creation fields | Order.open, CreateOrderRequest, existing OrderProblem.Detail/exception handler; Requester inline errors/summary | OrderFieldValidationTest, OrderCreationValidationTest; page RTL server/client details; no Supplier/Credit calls for invalid pair |
| NTH4 / ADR-030: new automatic expiry >= due+30min, due>=old expiry | RepostPlan constructor; creation picker/helper; old explicit plans unchanged | Exact boundary/domain/frontend tests; isolated PostgreSQL saved-short-plan reload/execution |
| NTH4 / ADR-030: manual expiry >= submission+30min | Order.createManualRepost, OrderRepostService; manual inline expiry error | Domain inclusive boundary, service rejection before reservation, RTL expiry feedback |
| ADR-028 late execution / NFR3 | Automatic path uses saved future expiry, not manual minimum | Legacy and late execution regressions; full Maven/coverage and frontend evidence in CHANGE-091 |

No new peer shape or schema, no retry/credential implementation; live/browser
integration remains a completion gate. Historical tables below remain dated.


## CHANGE-090: Credit push security exception

| Existing requirement / approved invariant | Repair | Verification / remaining gate |
| --- | --- | --- |
| F4.1.7 cancellation, F4.1.8/F10 expiry; F4.1.5/F5.1 completion; ADR-029 | Existing Credit push chain explicitly isolates conversion from Firebase roles | Valid signed local push reaches consumer; no User lookup; real Google and financial ledger checks pending |
| NFR3.1.1/3.1.3, existing Google push validation and user role/ownership guards | PubSubPushSecurityTest sends real bearer headers through production filter chains | 15 cases pass after observed red; fresh clean 80 Credit tests/no skips; 95.48% lines/85.56% branches |

One-time Vincent approval, exact Credit interference and test limitations are in
CHANGE-090. No Order lifecycle, financial contract, diagrams, database or frontend
behavior changed. FEEDBACK-008 is READY_FOR_VERIFICATION, not live VERIFIED;
Sprint remains [~].

## Effective CHANGE-088 local delivery traceability

CHANGE-089 verifies its Windows PS5.1 setup invariant under NFR3.1.3: harmless
native stderr is not failure; actual nonzero exits still fail; private stdout and
caller preference remain isolated. Test-LocalLiveNativeOutput has 11 positive/
negative assertions (observed red -> green). Existing 60 config + 16 ingress
regressions rerun successfully, not real Google/financial completion evidence.

| D1 / approved refinement | Implementation | Evidence / remaining gate |
| --- | --- | --- |
| F4.1.7/F4.1.8/F4.1.5/F5.1/F10/F11; ADR-029 | Local-live override, restricted ingress, owned Setup/Check/Pause | 60 config/safety + 16 actual nginx/fixture assertions; real refund/transfer/reset/browser gates pending |
| NFR3; ADR-013/021/026 | Google OIDC retained, isolated topics/scoped IAM, local DBs and cadence | Negative ingress, resource-ownership and unchanged baseline comparisons pass; actual GCP/IAM/consumer test unavailable |

No application contract/schema changed, no sequence marked `[x]`, no inference of
real financial success from fixture/metadata checks. See CHANGE-088/runbook.

## Effective CHANGE-086 traceability

| D1 / approved refinement | Implementation | Acceptance/negative evidence |
| --- | --- | --- |
| NTH4; F1 creation validation | RepostPlan/Order, CreateOrderRequest/OrderController, quarter-hour picker | RepostExpiryTest; controller plan propagation; RTL picker/payload and helper equality/invalid tests |
| NTH4; F8/F9 requester outcome UX | Order latest failure; RepostFailureRecorder; RepostControls; OrderMapper | RepostOutcomeJpaIntegrationTest rollback/overwrite/automatic late exact expiry/success-clear/unauthorized guard; RTL remount/version refresh |
| NFR2 / NFR3 | Existing auth/useApi/design system; domain/transaction separation | Full backend/RTL/lint/type/build and unchanged coverage gates; exact results CHANGE-086 |
| Shared migration discipline | V4 nullable fields/plan constraint; disable legacy no-expiry plans | Isolated PostgreSQL clean upgrade and V3-to-V4 test; invalid/equal expiry rejected, no invented deadline |
| ADR-027 pause | No background worker/task/credentials | Implementation scope inspection; peer 002-006 and live browser/cloud remain open |

Explicit expiry and persistent latest outcomes are implemented, not a production
completion claim. Background retries remain paused. Earlier descriptions of
missing expiry/client-local messages below are historical as of CHANGE-085.


## Current Sprint 2-3 effective slice

CHANGE-085 verification chain (no completion marker upgrade):

| D1 / approved refinement | Implementation | Test evidence / boundary |
| --- | --- | --- |
| F2/F8/F9; approved polling | useVisiblePolling/useOrderList; Browse/My Errands/My Requests | Fake timers: auth wait, 15 seconds, hidden/focus, no overlap, cleanup, account isolation, stale mutation guard |
| NTH4; short manual failure UX | RepostControls; HttpPeerAdapters.reserve | Credit exact insufficient vs other 409/malformed/authorization/transport cases; RTL keeps EXPIRED/no successful mutation, short appropriate message |
| NFR2 / NFR3; shared web/test discipline | Existing frontend primitives/useApi; no new UI transport | Frontend tests/lint/type/build; backend verify/coverage. Authenticated browser/responsive live evidence still pending |
| ADR-027 user scope follow-up | NO background worker/task/schema/credentials | All background retry implementation explicitly PAUSED pending peer agreement; automatic persistent failures/next-expiry not completed |

Current manual failure message is not retained across a full reload. Provider
source was read-only inspected, not live-tested. Feedback rewritten by service
with user approval; contracts 002-006 remain open. Generic workflow verifiers
do not fit existing record columns; their failures are recorded in CHANGE-085
rather than changing historical records merely to make a gate green.

CHANGE-084 / ADR-027 records approved NTH4 retry/failure refinements and polling:
one stable NEW candidate ID per saved request, temporary-only retries before its
expiry, EXPIRED original until success, short insufficient/permanent rejection
messages without guessing refund status. Trusted unattended credentials are a
documentation-only peer proposal, explicitly not implemented. Planned acceptance
cases are in ADR-027; no new tests executed or completion markers upgraded.

CHANGE-083 maps F10/F11 expiry and F6.4/F7 completion to one minute job, and
ADR-013 recovery to all-three-event 15-minute scans with immediate dispatch.
Supplier validation consumes its valid flag. NTH4 explicit expiry/strict timing/
durable failure UI/retries remains unimplemented, not verified by cadence tests.
Current target/gaps are in docs/diagrams/order-lifecycle-reconciliation.md.
No completion markers are upgraded.

See `sprints/sprint-2-3/README.md` for the approved class/sequence chain, requirement-by-test evidence and remaining gates. CHANGE-082 / ADR-025 supersedes the historical abort rules below: EVERY abort resets Credit, preserves immutable ABORTED courier history and emits User penalty; current OPEN/EXPIRED remains requester-visible and EXPIRED also emits Credit refund. Successfully reposted expired originals are hidden before requester pagination/counting; old IDs/refund intents remain stored.

Evidence as of 2026-10-08: 162 backend tests including real isolated PostgreSQL migration/upgrade/rollback/history/pagination, 34 frontend tests and lint/type/build passed; fresh line/branch coverage 94.01%/82.37%. Contract stubs are not live integration. NTH3 report/hold/resolution, missing assignment/reset/consumer implementations, trusted auto-repost credentials, browser and cloud verification are not complete. D1 PDFs are unchanged; approved overrides are recorded, not silently treated as original requirements.

## Historical Sprint 1 traceability (results/statuses at the referenced changes)

| Sequence | Requirement/design source | Planned behavior | Test evidence |
|---|---|---|---|
| 1 | F1.1-F1.3, Sprint 1 diagrams | Validate requester/suppliers/credits and create `OPEN` order | Implementation added; unit/integration verification pending |
| 2 | F2, Sprint 1 diagrams | List unexpired, unassigned `OPEN` orders | Backend and responsive Browse Errands UI added; live contract verification pending |
| 3 | F3, F13; CHANGE-068/069; ADR-017/018 | Verify courier eligibility, validate Order version locally, bind the authenticated courier identity, reject self-acceptance and pre-assigned orders, synchronously wait for Credit assignment confirmation using only Order ID and courier ID, then accept once | Acceptance and HTTP contract tests cover Credit-before-Order ordering, minimal request body, rejection with no Order writes, expiry recheck and 200 response; Maven compilation is blocked locally before tests; live Credit route is absent |
| 4 | F4.1.1-F4.1.2 | Assigned courier only: `ACCEPTED` to `IN_PROGRESS` | Aggregate ownership guard; suite verification pending |
| 5 | F4.1.3 | Assigned courier only: `IN_PROGRESS` to `PICKED_UP` with checkpoint | Aggregate ownership guard; suite verification pending |
| 6 | F4.1.4 | Assigned courier only: `PICKED_UP` to `DELIVERED` with checkpoint | Aggregate ownership guard; suite verification pending |
| 7 | F4.1.5, F5.1, updated overall Sequence 5, CHANGE-056/063/067/072, ADR-020 | Requester confirms `DELIVERED` to `COMPLETED`, or a Spring scheduler does so after 48 hours from the delivered checkpoint; both atomically commit the resulting Order/checkpoint/receipt and one completion event with overdue facts and no checkpoint history. | Boundary, database cutoff/lock, idempotency, shared event, dispatch, retry, lease, persistence and rollback tests; focused test run blocked by local Java compiler failure. |
| 8 | F4.1.7, F5.1, updated overall Sequences 6-7, CHANGE-055/061/063/064/069/070, ADR-010/013/014/018 | Requester may cancel own `OPEN` order; assigned courier may cancel own `ACCEPTED` order. Before expiry, the user-agreed synchronous Credit hold by Order ID precedes direct `ACCEPTED -> OPEN` with no event; Order validates its version locally. At/after expiry transition to `ABORTED` and commit the full-Order event for refund/penalty. Completion and cancellation event dispatch uses the outbox. | Application tests verify hold-before-open, hold-failure no writes, and expired outbox without hold. Mock/HTTP adapter tests cover bodyless hold and state-based idempotency. Frontend tests cover both returned states. The agreed Credit route is not implemented; production authentication remains unresolved. |
| 9 | F4.1.8, F10, NTH4; updated overall Sequence 6; CHANGE-065/ADR-015; CHANGE-071/ADR-019 | Spring scheduled scan expires due unassigned OPEN orders and atomically stores EXPIRED, checkpoint, and `OpenOrderRefundTaskEvent`; Credit consumes the shared event/topic to refund/release, distinguishing it from requester cancellation by `order.status` | Lifecycle state/checkpoint/outbox, shared event mapping, typed dispatch, and scheduler tests; full verification pending |
| 10 | NTH4 | Eligible expired order creates one linked `OPEN` repost after reservation | Implementation added; peer/idempotency verification pending |
| 11 | NTH4 | Requester receives draft and submits one linked repost | Backend and responsive manual-repost UI added; peer/idempotency verification pending |
| NTH1 | CHANGE-057 / ADR-012 (explicit Order-side supporting API) | Admin-only `GET /api/orders`; optional status (omitted returns every status); one-based `Pageable`; newest-first `OrderPageResponse` | Controller/security/persistence tests; `mvn verify` passed 86 tests and coverage gates |

Frontend vertical-slice note: CHANGE-022 covers the approved shared Next.js
implementation for sequences 1-11. Browser and live peer verification are not
claimed from static builds or unit tests.

## Cross-cutting

- CHANGE-057 / ADR-012: explicit Order-side query supporting the NTH1 Admin dashboard. `GET /api/orders` is admin-only, paginated, optionally status-filtered, and returns all statuses by default. Admin Service and frontend dashboard implementation remain outside scope.

- `Order` is the sole lifecycle aggregate and stores supplier IDs only.
- Automatic repost choice and plan details are submitted with order creation and
  are immutable afterward; an `OPEN` order is read-only for repost settings.
- Manual repost is exposed only for a requester-owned `EXPIRED` order that has
  not already been reposted.
- User identity/role/eligibility is obtained through approved User Service adapters.
- Credit reservation is synchronous before an order or repost becomes `OPEN`; Credit hold/reset is synchronous before an unexpired accepted order can return to `OPEN`.
- Commands and lifecycle triggers carry IDs; state changes carry expected versions.
- Flyway migrations are the schema source of truth; Hibernate only validates.
- Repost event publication remains deferred for Sprint 1; the updated overall design's typed completion, accepted-cancellation, and OPEN-refund events are a separate architecture update.
- The approved frontend vertical slice uses active Supplier Service names in pickup and delivery
  selectors while submitting supplier IDs; the authoritative catalogue remains in Supplier Service.
- Order cards resolve pickup and delivery references through the authenticated Supplier Service
  lookup contract, display names/buildings, retain IDs only for internal actions, and omit the
  internal Order ID from user-facing cards. Cards wait for lookup completion and never render
  opaque supplier IDs as a transient or error fallback.
- The shared dashboard exposes requester and courier functions without a client-side mode switch;
  backend User Service identity and authorization remain authoritative.
- Signup provisions the Credit Service account through its authenticated registration-fact contract;
  credit policy and account persistence remain owned by Credit Service.
- Local browser calls from `http://localhost:3000` to the gateway on `http://localhost:8080` use an
  explicit development-only CORS allowlist; deployed origins are unchanged.
- The post-request form mirrors the Order domain rule that `expiresAt` must be at least 30 minutes
  after creation and explains the constraint before submission; the backend remains authoritative.
- The updated overall design supersedes the synchronous outcome boundary for Sequences 5-7.
  CHANGE-063/ADR-013 records the approved transactional outbox: commit status/checkpoint/receipt
  and event intent together, then dispatch after commit with cron recovery. Delivery is at least
  once; consumer deduplication uses the stable event ID.
  Credit reservation before `OPEN` and hold/reset before unexpired accepted-order reopening remain
  synchronous. CHANGE-054 provides the Pub/Sub producer; CHANGE-063 implements transactional state/outbox transitions.
- CHANGE-064/ADR-014/070 and Sequence 7 document the deadline split: unexpired accepted cancellation waits on Credit and reopens, while expired cancellation publishes for Credit refund and User penalty. FEEDBACK-003 records the agreed hold contract; Credit's endpoint remains missing.
- The requested `*TaskPublisher` pairs, full resulting Order snapshot, topic placeholders, and
  transactional-outbox Sequence 5-7 diagrams are captured in CHANGE-053/054/056/063 and linked
  from Sprint indexes. No peer
  source is changed; Credit/User consumers are assumed future work per the user. Overdue facts are
  derived from checkpoint history without schema changes. Live Pub/Sub delivery needs configured
  topic IDs and an emulator or GCP project.
- The local mock models reservation balances, synchronous hold-for-reopen, and settlement support
  used by adapter tests. Refund/release on cancellation/expiry belongs to Credit's event consumers;
  Credit Service remains the production owner of balances and ledger state.
- Lifecycle expiry explicitly selects only `OPEN` orders whose `courierId` is `NULL`, preventing an
  already-accepted order from being expired by the Spring scheduler. CHANGE-065/071 records EXPIRED,
  its checkpoint, and shared `OpenOrderRefundTaskEvent` atomically; Credit refunds from that event.
- Collection responses use the shared `items/page/size/totalItems/totalPages` shape and errors use
  `status/error/message/path/timestamp/details`; OpenAPI operations declare the bearer requirement.
- Order lifecycle actions emit structured service-local audit events without logging credentials or
  credit secrets (NFR4 implementation detail; sink configuration remains deployment-owned).


## CHANGE-077 verification mapping

| Project reference | Approved refinement | Implementation | Verification |
|---|---|---|---|
| F1.1-F1.3; NTH4 | Requester creation/repost times use quarter-hour minute choices with the existing expiry minimum | frontend QuarterHourDateTimePicker, Post Request, RepostControls and orders helpers | 29 frontend tests pass, including real form-to-API mock payloads; TypeScript and lint pass; visual/authenticated browser pending |
| F4.1.8; F10; NFR3 | Expiry every 15 minutes; DB due selection remains authoritative | OrderExpiryScheduler and application/local/cloud cron settings | Cron boundary test passes; full suite has 125 tests, zero failures/errors, six Docker integration skips |
| F4.1.5; F5.1; ADR-013/020 | Recovery hourly per ADR-023; immediate dispatch and one-minute 48-hour completion preserved | OrderOutboxScheduler and OrderAutoCompletionScheduler | Existing scheduler cadence assertion updated for hourly recovery; not run in CHANGE-078. Immediate-dispatch path unchanged; deployed idle scheduling unverified |

See CHANGE-077 / ADR-022. API/event schemas, backend class responsibilities and stored deadlines are unchanged.


## CHANGE-079 / ADR-024: Central role annotations (approved 2026-10-08)

Production Firebase validation resolves User Service roles once per request, verifies response identity against JWT subject, and enforces RequireRequesterRole/RequireCourierRole/RequireAdminRole. Order-specific production role mode defaults to HTTP; local anonymous/mock behavior remains. Adapters reuse verified roles/identity, retaining fresh courier eligibility and locked domain ownership/state guards before mutation. Shared reads accept any confirmed requester/courier/admin role; /mine uses its selected mode. Internal lifecycle/scheduler authorization, API bodies, event payloads and schema remain unchanged. See ADR-024 for endpoint policy, inspected peer contracts and positive/negative test obligations.

CHANGE-092 executable evidence: exact seven-field refund/completion JSON, unchanged accepted schema, internal metadata and saved legacy normalization verified by CompactOrderEventContractTest/OrderTaskEventFactoryTest/OrderOutboxDispatcherTest/PubSub and transition regressions. 32 focused pass; fresh verify 195 passed/17 Docker skips, no failures/errors, coverage 91.33%/81.77%. F4.1.5/F4.1.7/F4.1.8/F5/F10/F11/F13 and approved F12 amendment traced in CHANGE-092. Credit/User integration BLOCKED FEEDBACK-009. No Sprint [x] upgrade.

## CHANGE-094 executable evidence

| D1 / amendment | Behavior and source responsibility | Verification |
| --- | --- | --- |
| F2/F8/F9; ADR-025/032 | OrderController.mine -> OrderQueryService -> domain repository -> JPA filtered current/history page/count; My Requests/My Errands Select and pagination | OrderMineStatusFilterTest, OrderQueryServiceTest, OrderPersistenceAdapterTest, OpenApiDocumentationTest and RTL page/helper tests pass; OrderPersonalStatusJpaIntegrationTest3 cases skipped without Docker |
| F4.1.9/F11; ADR-025 | OrderActions changes courier ACCEPTED label/dialog only, existing POST unchanged |4 existing updated action tests pass, positive/error and requester/incorrect-state visibility; domain regression suite unchanged |
| NFR2/NFR3; ADR-027/032 | Existing useOrderList/visible polling with explicit5-second Order interval, auth/hidden/cancel/stale safeguards; Credit15-second default retained |58 frontend tests, lint0 errors (12 unrelated existing warnings), typecheck pass;8 browser fixture width/mode scenarios pass; normal production build blocked by Google Fonts network |

Fresh backend verify:243total,219passed,24 Docker skips,0 failures/errors; lines92.36%, branches82.41%, existing >=80% gates pass. Browser fixtures mock auth/API/supplier names and omit AppShell/Geist font loading; they do not verify real Firebase/peers/production typography. No Sprint [x] claim.

## CHANGE-095 verification

| D1 / amendment | Behavior / source | Test / result |
| --- | --- | --- |
| F2/F8/F9; ADR-025/032 | Explicit OrderStatusFilter mode; My Requests excludes ABORTED; My Errands excludes OPEN/EXPIRED/CANCELLED | Exact rendered option tests fail first then pass; existing selection/pagination integration tests retained |
| NFR2/NFR3 | Existing Base UI style and correct mode choices across responsive widths; no auth/client/API changes | Full frontend 60 passed, lint/typecheck passed; eight actual-component mock-API browser scenarios at 320–1920 passed |

No backend tests rerun for this UI-only correction. Browser auth/API/font limitations and earlier PostgreSQL/Google Fonts build gaps remain; Sprint stays [~].

## CHANGE-096 verified scheduler isolation

| Project D1 reference | Requirement / approved invariant | Implementation | Test | Result |
| --- | --- | --- | --- | --- |
| F4.1.8 / F10; CHANGE-093 | DB selects due OPEN/unassigned IDs; independent expiry rollback and continuation | JpaOrderRepository.findDueUnassignedIds; OrderExpiryProcessingService.expire; LifecycleProcessingService.expireDue | LifecycleFailureIsolationTest; LifecycleTransactionIsolationTest; OrderLifecycleIsolationJpaIntegrationTest | Passed, including real PostgreSQL rollback/NOWAIT |
| F4.1.5 / F5 / F13; ADR-020/026 | DB selects latest-delivery >=48h; independent completion commit/rollback | JpaOrderRepository.findDueForAutoCompletionIds; OrderTransitionService.autoComplete | LifecycleTransactionIsolationTest; OrderLifecycleIsolationJpaIntegrationTest | Passed, including PostgreSQL latest checkpoint query |
| NFR2/NFR3; ADR-013 | DB-filtered due outbox IDs; per-event transactions and outside catch | JpaOrderEventOutboxRepository.findDueIds; OrderEventOutboxPersistenceAdapter; OrderOutboxDispatcher | OrderOutboxDispatcherTest; OrderEventOutboxPersistenceAdapterTest; OrderOutboxIsolationJpaIntegrationTest | Passed, including retry/marker mutation rollback and unrelated caller rollback |

Tests-first regression: 1 expected failure (retry database unavailable), target/change096-red.log. Focused 43 tests passed, including eight PostgreSQL lifecycle/outbox isolation tests; fresh source-only wrapper-selected Maven 3.9.16 / Java21 offline verify: 252 tests passed, 0 failures/errors/skips, including 28 PostgreSQL tests. JaCoCo 95.99% lines / 83.72% branches; unchanged >=80% gates passed. Logs: target/change096-focused.log and target/change096-verify.log; reports target/change096-source-check/target/. Docker28.4.0; isolated PostgreSQL15 Testcontainers; new isolation suites mock all cloud publishers. No application database, peer service, real topic or cloud setting changed.

This clears the previous local PostgreSQL skip limitation for CHANGE-092/093/094 (all 28 current DB tests executed). It does not verify real Credit/User consumers or real cloud publication and does not upgrade Sprint [~]. Frontend source unchanged and its tests/build not rerun this turn.

## CHANGE-097 personal Order rendering resilience

F2/F8/F9 personal Order views and NFR2/NFR3: useSupplierNames consumes both existing Supplier lookup items and missingIds. Definitive missing references use the existing Location unavailable label and stop blocking Order cards. New hook tests reproduce partial/all-missing failures first;63 full frontend tests, lint/typecheck and Docker production build pass. Local frontend rebuilt, both personal routes/assets return200 and serve corrected hook. No API/peer/data or authorization changes; authenticated browser and missing data restoration unverified. See changes/CHANGE-097-missing-supplier-order-cards.md.

## CHANGE-098 Swagger gateway routing

Parent AGENTS OpenAPI documentation requirement and NFR2/NFR3/NFR6: explicit current-origin server metadata prevents Swagger targeting a backend HTTP origin from an HTTPS gateway UI. New OpenApiConfiguration and updated OpenApiDocumentationTest retain documented API paths and authentication policy. Tests first: one expected server-URL failure and one existing documentation pass (target/change098-red.log). Fresh source-only wrapper-selected Maven3.9.16/Java21 offline verify:253 tests,0 failures/errors/skips, including28 PostgreSQL tests; coverage96.00% lines (1441/1501),83.72% branches (468/559), unchanged >=80% gates pass. Generated target/openapi.json servers=[{url:"/",description:"Current gateway or service origin"}];160 current POM/source/test/resource files equal the fresh tested copy. Logs/reports target/change098-verify.log and target/change098-source-check/target/, evidence target/change098-evidence.json. git diff --check passes. No local application container rebuild or real authenticated browser request; no commit/push/cloud deployment. Staging remains unchanged until Order Service redeployment.

## Backend expiry bypass regression — CHANGE-099 (2026-10-10)

| Requirement / invariant | Implementation | Test / evidence |
| --- | --- | --- |
| F1.1-F1.3 creation and approved 30-minute server expiry minimum | Existing OrderCreationService, Order.open, OrderExceptionHandler | New OrderCreationExpiryApiTest: four short/past HTTP requests rejected with expiry-only 400 and no financial/persistence writes; valid create remains successful. Existing domain test verifies inclusive exact 1800s. Focused 19 tests pass; full result in CHANGE-099. |

Business/API/architecture unchanged. Documentation and regression coverage only;
manual/automatic ADR-030 behavior and unresolved peer integration gates retained.
