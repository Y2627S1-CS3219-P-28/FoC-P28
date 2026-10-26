

## Effective retirement of accepted-cancellation publishing - CHANGE-155 / ADR-053 (2026-10-19)

User approved removing AcceptedOrderCancellationTaskEvent publishing AND legacy replay after inspecting staging: eight legacy rows were PUBLISHED, with no pending/failed rows shown. Preserve published history; this task does not modify any database or GCP resource. Production was not inspected: before rollout, verify no PENDING, IN_PROGRESS or FAILED legacy rows in each target database. Resolve any such rows explicitly before deployment; do not convert them into new score events or mark them PUBLISHED without broker confirmation.

Removed the legacy DTO, publisher/interface, factory/mapper methods, now-unused snapshot DTOs and dispatcher branch. The four current channels remain OpenOrderRefundTaskEvent, OrderCompletionTaskEvent, PenaltyScoreAdditionEvent and PenaltyScoreDeductionEvent. Courier abort still uses synchronous Credit hold/reopen and the existing final addition (plus refund when expired); no lifecycle, hold, penalty policy, idempotency or financial rules change. Historical database rows and immutable migrations remain intact. Other older refund/expiration normalization remains supported.

The retired topic setting is removed from Order config, deployment templates, local Compose/bootstrap, environment examples/staging/production settings, topic provisioning and CI topic checks. These scripts no longer require or create the old topic; they do not delete existing cloud topics or subscriptions. User Service consumes only the new scoring channels. Existing admin-provider authorization requests remain open. AsyncAPI1.3.0 describes four outgoing operations; earlier statements requiring accepted-cancellation replay/configuration are superseded by this amendment.

An unexpected non-published legacy row after deployment is unsupported and follows the existing bounded failure/recovery path, never automatic financial conversion. This is why the pre-deployment backlog check is required. Live User penalty processing, production backlog and cloud deletion are not verified here.


## Effective delivered-report review amendment — CHANGE-141 / ADR-048 (2026-10-18)

Approved Yao Xiang: new requester reports require own DELIVERED order; one damage/wrong-location category. Reports have AWAITING_COURIER_RESPONSE, READY_FOR_REVIEW and one RESOLVED final status. Courier response window is eight hours; server owns deadlines. Report Service is MISSING: the Report UI is a local-only isolated demo; HTTP-peer/staging/production show Coming soon and never fetch sample reports. Existing real Order financial actions remain separate from resolving a report.

Order stores deliveryOverdue at actual delivery, comparing deliveredAt against stored START plus duration deadline. isOverdue remains null before delivery or if historical timing is unknown; no runtime status refresh or new overdue scheduler. Admin waiver only before completion on DELIVERED late orders preserves the true lateness and saves actor/time/reason. Normal waived-late completion funds the courier but emits neither overdue addition nor successful-delivery reward. Explicit admin-selected penalties remain separate.

AdminOrderController adds PUT /api/orders/admin/{orderId}/completion-holds/{reportId}/release (no body) and POST /api/orders/admin/{orderId}/overdue-waiver ({reason}). Both verify admin role, use the verified principal, NOWAIT locked Order and the pending-command guard; all local writes are transactional. Release retains its report binding/audit and any other active holds. It may follow admin cancellation/completion and emits no funding event. Report Service must durably retry its release intent using forwarded admin Firebase authorization; no browser-only live orchestration exists. See FEEDBACK-015. V12 backfills only known delivery facts and adds waiver/release audit fields and an active-hold index; V1–V11 remain immutable.

Frontend uses existing theme/components, compact toolbar, darker selected rows and no redundant selection count. Normal/admin Reports poll every five seconds while visible; sample ownership, deadline and evidence checks are server-side in the gated prototype, which has no production identity authority or financial effects.

# Order Service Requirements Traceability

## CHANGE-156 / ADR-054 - Compensation fixture traceability [~]

CHANGE-157 verification follow-up: the PostgreSQL fixture now uses supported
messaging configuration and an explicit broker transport fixture. All11 database
cases and2 configuration regressions pass with no skips, including clean V15 and
V14 upgrade. Actual Credit/broker cooperation is still unverified; full-regression
and coverage evidence is recorded in CHANGE-157, not inferred from these13 checks.
Final CHANGE-157 root verify: common12 + Order562 pass,0 failures/errors/skips;
fresh Order coverage94.73% LINE/81.02% BRANCH passes unchanged gates. Existing
live Credit recovery16 cases pass; the new compensation protocol remains a fixture.

| Requirement / amendment | Implementation | Verification |
| --- | --- | --- |
| F1, F3, F11, F13; ADR-033/035; approved compensation amendment | Immutable persisted policy/deadline; one initial keyed Credit mutation; fenced compensation rather than forward replay | OrderCompensationWorkflowTest, OrderCompensationJournalTest, OrderCommandGateTest |
| NTH4; ADR-037 | REPOST reserves the candidate under the same policy; legacy automatic path refuses bypass | PostgreSQL fixture test and existing repost regression; delegated automatic behavior still deferred |
| Durability/consistency/testing NFRs; ADR-036/054 | V15 step/final markers, command-owned outbox, atomic result inbox and final local transaction | OrderCompensationPostgresIntegrationTest: required real PostgreSQL; unavailable runtime is not a pass |
| Existing USER Requester/Courier pending UI; ADR-038 | Existing polling/key retention; pending COMPENSATING -> terminal rejection | use-order-command.test.tsx compensation case; runtime evidence in CHANGE-156 |
| Cross-service safe reversal/fencing | Exact peer contract, safe CLEAR_COURIER pre-image restoration, missing-K tombstone and result outbox | FEEDBACK-017 blocks live; Order fixtures never verify actual Credit |

No new Order lifecycle state, penalty policy, supplier ownership or event-contract
replacement is inferred. Existing live contracts remain until coordinated rollout.

## CHANGE-140: Admin table density

D1 NTH1 oversight; user-selected20-row pages are a presentation/request-size detail within existing contracts, not a new business rule. Existing mock/live adapter assertions cover20-row Orders, mock Users, exact-search metadata and retained supplier-card behavior; admin page/action regressions cover existing pagination/navigation. No new backend or peer implementation.


## CHANGE-139: Compact oversight date filters [~]

D1 NTH1 admin oversight; user-selected reference toolbar/calendar presentation, no new domain or penalty behavior. AdminOrderDateRange owns native date UX, adminOrderDateBounds converts local days, API adapters send explicit instants, AdminOrderController binds/authenticates, OrderQueryService validates and OrderPersistenceAdapter/JPA Criteria filter/count/page in SQL. Existing samples remain isolated. UI/date/adapter regressions cover refresh preservation, range validation and navigation; MVC guards/date binding and PostgreSQL boundary/count/pagination tests verify the real path. Diagram: diagrams/admin-order-query-filters.mmd. Additional per-column ID/name filters/history/export/report actions remain outside this increment; no fabricated unavailable controls.


## CHANGE-138: Admin resolution actions UI [~]

D1 NTH1 admin oversight, NTH2 selected courier penalty and NTH3 explicit report resolution reuse approved ADR-044/046 transitions. No new financial/scoring requirement. Row/radio/keyboard selection, state-aware toolbar, retained links, penalty/no-penalty, maximum preview and required preset/custom reason are exercised by admin-order-actions.test.tsx. API adapters verify real routes and mock isolation. Domain/MVC/PostgreSQL regressions cover saved explanation, optional legacy clients, binding, rollback, role lookup and policy discovery; migration test covers clean/V10 upgrade. AdminOrderController maps the request, transaction services own atomic writes, Order owns state/participant/decision invariants and existing outbox owns publication. Diagram: diagrams/admin-order-resolution-ui.mmd. Increment verified locally; peer scoring/cloud integration remain pending, no whole-Sprint completion claim.


## CHANGE-137: Admin completion / held funding resolution [~]

D1 NTH1 oversight, NTH2 explicit penalties and NTH3 report resolution; approved
ADR-046 extends ADR-044 cancellation/ADR-045 timing/decisions. Four assigned active
states -> COMPLETED, held override only through admin action; ordinary guards kept.
Domain tests cover states/participants/holds/timing/decision; service tests cover
funding-only waiver, max/tie/no automatic score, replay and write failure; MVC
tests verify401/403/identity/400/409 and existing local bypass. PostgreSQL tests
verify actual rollback/receipt/outbox/hold preservation, NOWAIT retry and competing
admin cancel/complete. Generated OpenAPI includes the new route.
Order increment verification belongs to CHANGE-137; Report/User/deployed scoring
and financial confirmation remain pending, not a complete NTH/Sprint claim.


## CHANGE-136: Existing admin oversight organization

D1 NTH1 / ADR-044 behavior retained. Admin cancellation/list now share the
AdminOrderController; existing authorization, wire/validation/OpenAPI and lock
conflict regressions verify the move. No new requirement or peer interaction.


## CHANGE-135: Start-based overdue consistency and decision audit [~]

D1 F4 courier progress/delivery/completion, NTH2 penalties and NTH3 holds; approved
START-deadline and admin resolution amendment. Domain Order owns timestamps and
nullable observation; transition service persists timestamp/checkpoint atomically.
Tests cover delayed START, actual delivery boundary, missing legacy evidence, held
completion/abort, waiver/highest decision, SQL due selection and independent rollback.
V10 clean/V9 upgrade preserves statuses/versions/checkpoints/outbox; Report/User
integration and deployed confirmation remain pending. See CHANGE-135/ADR-045.


## CHANGE-134: Admin cancellation and overdue observation [~]

D1 NTH1 administrator oversight, NTH2 highest selected incident and NTH3 report
resolution, user-confirmed active/held cancellation. User adds unresponsive3
beyond original six cases. No global per-Order scoring/suspension claim.
Tests cover roles, participants, highest/null/reward, terminal states, exact
replay, real rollback/locking and grouped overdue timing. Report/User subscriber
implementation and deployed verification remain pending. See ADR-044/CHANGE-134.


## CHANGE-133: Admin account oversight integration [~]

Existing D1 administrative oversight/ADR-039 provider boundary: selected-user
history uses completed Credit contract; mock Compose retained. Regressions cover
selected UID/paging/abort/error propagation, profile independence and retry.
OpenAPI merge preserves AdminMock, holds and involvement coverage. Balance/User
management and live deployment confirmation remain pending; no new lifecycle rule.


## CHANGE-132 policy query API

Approved maintainability/API refinement of ADR-043 and NTH2 mapping (CHANGE-131).
PenaltySystemPolicy name, reward lookup, immutable report subset/full list.
Same D1 six reasons/values. Common contract tests assert exact membership,
order/immutability; existing Order tests verify unchanged automatic events.
No new functional workflow or User/Report implementation/readiness claim.


## CHANGE-131: NTH2 full penalty case mapping [~]

D1 PDF37-39/NTH2 explicitly defines +2 accepted cancellation, +1 deadline
failure, +4 wrong delivery location, +4 mishandling/damage, +10 deliberate
retention/theft and -1 successful completion without confirmed complaint.
Shared CourierPenaltyCase represents all six. Common CSV tests protect exact
names/amounts; existing Order event tests protect the three implemented
automatic reasons and seven-field wire contract. D1 PDF38-41 requires report
investigation/hold and admin confirmation; the new mappings alone do not apply
scores, choose the highest incident, suspend users or implement Report Service.
Permanent suspension and one-month threshold suspension remain integration gaps.

## CHANGE-130 / ADR-043 penalty policy extraction

Technical maintainability refinement of the user-approved D1 courier penalty
amendment ADR-041 (+2 accepted abort/+1 overdue/-1 success), not a new FR/NTH.
One common enum definition; Order factory/DTO imports change only. Shared
reason/amount tests and existing seven-field/idempotency event tests preserve
the contract. No new endpoint/schema/UI or completed User integration claim.

## CHANGE-129 / ADR-042 active relationship reads [~]

| Requirement | Implementation | Verification/status |
| --- | --- | --- |
| User-requested deletion/deactivation precheck; existing Supplier F6.3 | SQL EXISTS over five active states and both User/location relationships | Real PG every status, hold/past expiry, unknown and aborted-history tests |
| Approved shared-role read/NFR4 | RequireOrderRole, minimal ID/boolean DTO | Production401/403 and all3 roles; JSON/OpenAPI |
| NFR3 separation/efficiency | Read-only service -> domain port -> infrastructure query; explicit MapStruct |420 backend0skips; coverage gates pass |
| Existing Order visibility/NFR2 | Dynamic supplier missing/null fallback, no names stored or new rows |129 frontend tests, typecheck/lint/build pass; browser not verified |
| Deletion consistency | FEEDBACK-015 provider callbacks and race/obligation policy | Missing integration; no full deletion-safety claim |

Prior completion-hold and explicit penalty slices preserved. Schema/config/peer
implementations unchanged for129. Full provider integration/Sprint remain partial.
## CHANGE-128 / ADR-041 explicit penalties [~]

| Requirement | Implementation | Evidence/status |
| --- | --- | --- |
| D1 courier outcome/penalty requirements, approved -1 override | CourierPenaltyCase maps accepted cancellation2, overdue1, successful1; direction in event type | Enum/JSON/manual/system/deadline recipient tests |
| Event-driven integration/NFR4 consistency | Credit and one penalty intent atomically committed with status/receipt; after-commit replay | Real PG injected penalty-write rollback; duplicate receipt no-event |
| NFR3 reliable at-least-once delivery | Stored payload/ID, independent retry/redrive, retained legacy dispatcher | Publisher topic routing; failed-event retry and later success |
| Contract/environment isolation | Seven fields, hidden operational versions; local/dev/prod topic settings | AsyncAPI YAML/references, Compose and bootstrap checks |
| User consumer dependency | FEEDBACK-014: authenticate, atomic dedup/score, transmitted amount, clamp ordering/cutover | Actual consumer absent; no end-to-end score claim |

Full penalty subscriber integration/Sprint remain partial. No Credit behavior or
frontend change for this penalty milestone. Detailed class/sequence design: ADR-041.

## CHANGE-127 / ADR-040 requester completion holds [~]

| Requirement | Design -> implementation | Verification/status |
| --- | --- | --- |
| D1 NTH2 PDF38/NTH3 PDF41 | Per-report immutable placement and Order boolean, active own Order only | Domain/API/PG rollback/binding races pass; actual Report/release pending |
| NTH2 manual/48-hour completion and final funding freeze | SQL due filters + locked recheck + aggregate guards before Credit/outbox | Held completion/cancel/expiry/abort no effects; independent candidates proceed |
| NFR4 caller authority | Existing Firebase JWT/Requester role and UID/owner binding | Role/UID/owner denial and forwarding tests |
| NFR2 requester feedback | OrderResponse isCompletionHold, existing action message/disabled button | Held and stale-browser regression tests; visual/browser confirmation pending |
| NFR3 persistence/regression | V9 additive migration and insert-only global report ID |394 backend0skips/126 frontend pass; coverage/typecheck/lint/build pass; local9 preserves seven Orders |

Report submission itself, investigation, courier reporting policy, release and
settlement remain deferred; full NTH2/NTH3/Sprint completion is not claimed.
## CHANGE-125 Admin typography/theme consistency [~]

| Requirement | Implementation | Verification |
|---|---|---|
| User: same overall font/size/colour; NFR2.1 and shared style baseline | Central Admin layout policy, regular identifiers, semantic shared tokens; hierarchy and numeric metrics retained | Existing203 frontend regressions, typecheck/lint/build pass; authenticated visual/theme review pending |

Normal-profile shared presenter source and data/contracts unchanged. Style guide
records the approved Admin scope; no new CSS implementation-detail tests.

## CHANGE-124 consistent Order table typography [~]

| Requirement | Implementation | Verification |
|---|---|---|
| User: consistent sizes and regular IDs; NFR2.1/UI baseline | Regular14px Order table body, scoped link weight and matching badge/date size | Existing34 Admin regressions, TypeScript/scoped lint/build pass; authenticated visual review pending |

No API/data/interaction change or new CSS implementation-detail tests.

## CHANGE-123 compact table Order IDs [~]

| Requirement | Implementation | Verification |
|---|---|---|
| User: truncate table IDs without losing full copy/detail; NFR2.1 | Optional CopyId compact mode only in Admin Order table; bounded width/ellipsis | Existing Admin copy/navigation/detail tests and typecheck/lint/build in123; authenticated visual check pending |

Identifier values/search/API contracts unchanged. No CSS-class-only tests added.

## CHANGE-122 Compose-selectable Admin providers [~]

| Approved requirement / invariant | Implementation | Verification |
|---|---|---|
| User: base Compose mock UI, HTTP override actual local endpoints with staging-equivalent capabilities | FOC_ADMIN_API_MODE -> runtime public config -> existing Admin adapter; base/override mode flags | Config/mode/auth regression matrix and resolved Compose projection assertions |
| NFR4 / authoritative role checks | Mock allowed only local+emulator+mock flag; cloud/invalid flags live; HTTP demo routes disabled | Non-admin local-live denial; cloud mock-flag rejection; no fallback/peer impersonation |
| NFR3.1.3 / runtime contract | New variable in .env.example and README; request-time rather than built-in mode | RED then GREEN tests, frontend/typecheck/lint/build and Compose checks in122 |

Existing119 provider matching/missing APIs and121 layouts retained; local real
data differs from staging. Live authenticated rollout is not proven by these tests.

## CHANGE-121 consistent Admin responsive searches [~]

| Requirement | Implementation | Verification |
|---|---|---|
| User-requested consistent mobile controls; NFR2.1 | Shared AdminFilterForm/AdminFilterActions used by all Admin searches | Existing34 Admin tests, typecheck and scoped lint pass; production build recorded in121; actual mobile visual check pending |

Existing search/reset/query handlers and provider contracts remain unchanged.

## CHANGE-120 mobile Order filter alignment [~]

| Requirement | Implementation | Verification |
|---|---|---|
| User-requested aligned fields; NFR2.1 responsive web | Single responsive AdminOrders form/grid, equal first-row fields and mobile action row | Existing34 Admin interaction tests and TypeScript/scoped lint pass; production build in CHANGE-120; browser visual check pending |

Presentation-only implementation detail; existing API handlers and119 provider
behavior unchanged. No CSS-class-only tests added for this reversible fix.

## CHANGE-119 environment-aware Admin integration [~]

| Approved requirement / invariant | Implementation | Evidence |
|---|---|---|
| Explicit user amendment: mocks locally, completed APIs outside local, unfinished sections/placeholders | Runtime useAdminApi; live/mock adapter capabilities; Coming soon cards and disabled User actions | Mode matrix, real route/method/search/404/error tests and unavailable-operation no-call assertions |
| NFR4 / existing provider authorization | Real role-context with current UID/admin comparison; no mock fallback or credit impersonation | AdminGate role/identity/failure regressions and live non-admin denial |
| NFR2.1 / existing Admin layouts/navigation | Reused wrapping controls/cards; UID summary lookup and linked details; labels independent of Order loading | UI no-call/placeholder/navigation/pending-label regressions; authenticated visual check pending |
| NFR3.1.3 regression | Existing local prototype tests retained; additional live capability tests | Full frontend verification recorded in CHANGE-119 |

Matching API contracts reused; ADMIN-API-001..006 remain incomplete. No claims of
full NTH1/Admin integration or peer implementation completion; local demo and
live capability verification are separate.

CHANGE-116 is the effective clarification of CHANGE-115's Order UI amendment:
original card widths/two columns, Version/Item Description swapped and requester
above courier. Existing regression now checks the ordinary grid and no wide span.
Supplier placement/map assertions remain; no requirement/API scope expansion.

## CHANGE-115 Admin presentation clarification [~]

User's explicit follow-up under the same Admin amendment/NFR2.1/NFR3: metadata
then credits/wide description; requester above courier; no list overview map;
status in Location details. Regressions check reordered response keys, field
order/span and status/map placement. Detail/creation maps and confirmations stay.

## CHANGE-114 Admin map/layout amendment [~]

| User-approved behavior / existing requirements | Implementation | Evidence |
| --- | --- | --- |
| Maps/pins/suggestions/drag/manual coordinates; NTH1-supporting mock Admin scope | LocationMap/LeafletMap/AddressSearch/map-provider; optional SupplierForm mode | Real Leaflet synthetic drag/lifecycle, GeoJSON/anonymous request/race/error/form sync tests |
| Detail-only confirmed Supplier deactivation; NFR4 retained | AdminSupplierDetails existing action; removed card control | Confirmation/error/inactive regressions; cards still navigate |
| Grouped complete Order details, name navigation and separate IDs | Logical cards including Repost/Other, RecordLink + CopyId | Repost/unknown-field completeness and exact User/Supplier route assertions |
| Separate User columns; NFR2.1/NFR3 | Horizontally scrollable single-field columns | Header/cell regressions; typecheck/lint/build; visual review pending |

No actual backend integration completion is claimed. Maps depend on external
public providers; service data/actions retain the approved local mock milestone.

## CHANGE-113 dedicated Admin pages [~]

User-approved continuation of NTH1-supporting Admin prototype. Dedicated Order/
Supplier routes show the selected complete record; existing User/Supplier actions
appear before details/history. Existing page/ID/status search remains. Regression
tests check actual route links, exact-ID requests, full detail rendering, back/
retry controls, action ordering and confirmed deactivation. NFR2.1/NFR3/NFR4
remain applicable; browser verification/real provider integration remain pending.

## CHANGE-112 Admin profile/navigation refinement [~]

| Effective approved amendment / existing requirement | Implementation | Evidence |
| --- | --- | --- |
| User's Admin UI refinements; existing NTH1 prototype | Selected Admin routes, full profile, copy UID and existing actions | Deep-link/off-page/late-response tests; provider integration remains pending |
| Existing Profile credit history, admin-selected sample user | Shared TransactionHistoryView; separate mock history loader/DTO | Paging/identity/failure-isolation UI tests and mock wire-contract tests |
| NFR2.1 responsive web, NFR3 regression | Equal-height wrapping controls, separate scrollable Order columns, SupplierBadges | Typecheck/lint/build/tests; authenticated visual check still required |
| NFR4 security and approved mock gate | Existing AdminGate and local condition retained; ADMIN-API-006 | Existing role/gate regressions; no impersonation or sibling service writes |

## CHANGE-111 Admin UI prototype [~]

| Effective requirement | Implementation | Verification |
| --- | --- | --- |
| User-approved admin mock amendment; NTH1 support | AppShell switch, guarded admin routes/three pages, dedicated adapter | Permission/identity/navigation tests; real NTH1 integration not claimed |
| All statuses/latest/paging/ID search; existing ADR-012 | AdminOrders, sample query, all response fields/details, names/copy controls | Backend search/page/latest contract and UI status/page/copy tests |
| Sample user profile/suspend/promote/confirmed deactivate | AdminUsers, future UTC end instant, sample store | Profile/mutation/validation/confirmation tests; real peer APIs pending |
| Sample suppliers; existing Supplier F6 support | AdminSuppliers, reused SupplierForm and DTO wire fields | Search/create/deactivation tests; real Supplier APIs already exist |
| NFR2.1 responsive web, NFR3 regression, NFR4 security | Shared sidebar/mobile sheet/primitives; local-only backend gate | Tests/lint/typecheck/build/condition checks; browser evidence in CHANGE-111 |

## CHANGE-110 command submission polling [~]

| Effective requirement | Existing design -> implementation | Verification |
| --- | --- | --- |
| F1/F3/F11.2, NTH4 + ADR-033/035/037 | Browser intent -> active POST -> outcome; useOrderCommand pauses individual GET during submission/local resume | Four delayed-operation tests, terminal success/rejection and Continue tests with actual visible polling |
| ADR-033/038 unknown outcome and stable key; NFR4 identity | Timeout retains frozen key/input; missing-key recovery never invents another ID | Actual-poll timeout/404 same-key test; existing reload/account/discovery regressions |
| NFR3 test adequacy/regression | New real-poll tests supplement earlier mocked polling |8 observed RED then GREEN; final124 frontend tests/28 files, zero skips; lint0errors, typecheck/build pass |

Source/test386f58f; no backend/peer/API/schema change. Automated-only timing
verification; authenticated browser and deployment remain pending. Full Sprint
completion is not claimed; CHANGE-110 contains scope, approval and rollback.

## CHANGE-109 profile merge compatibility [~]

| Requirement/convention | Contract -> frontend implementation | Verification |
| --- | --- | --- |
| User-approved repair; authenticated gateway convention | Current AuthProvider -> local account-keyed profile GET via useApi | Load/auth restoration/signed-out/failure/account-switch tests |
| Existing own-profile edit; no new Order FR/role/sequence | Username-only PUT; Firebase verified email + refreshed token then email-only PUT | Minimal payload, rejected saves, unverified no-write, sync retry, stale-account tests |
| Existing Firebase password flow; NFR3.1.3 | Reauthenticate before updatePassword; no password to User Service | Success/rejected reauthentication; no unintended peer calls |
| NFR2.1 responsive UI; NFR3.1.3 | Existing primitives/cards with mobile padding; Edit control |116 tests/27 files, typecheck/build pass, lint0errors; actual320-1920px browser/email checks pending |

Source/testd7f72a8; CHANGE-109. User implementation inspected, not real-user
integration verified. Order financial command recovery unchanged.

## CHANGE-108 / ADR-038 pending display recovery [~]

| Effective requirement | Design -> implementation | Verification | Status |
| --- | --- | --- | --- |
| F1/F3/F4.1.1/F4.1.9/F11.2 + ADR-033/035 | ADR-038 safe display -> CommandView/OrderCommandService | Owner projection/source/expired-target/null tests; saved PostgreSQL discovery | Automated passed |
| F2/F8/F9 + NFR2/NFR4 | Shared page/account discovery -> pending existing cards, no history binding | Storage loss, same-key resume, account isolation, dedup and real page filter/pagination tests | Automated passed; browser pending |
| NTH4 + F10/F10.1.3 + ADR-037 | Frozen REPOST details separate; original EXPIRED form blocked | Submitted summaries, actual abort/repost controls, terminal confirmation and conditional key cleanup | Automated passed |
| NFR3/4 | No new Credit payload, schema, general read permission or workflow handler |373 backend0skips/103 frontend, lint/typecheck/build/OpenAPI pass;95.37%line/80.27%branch | Browser/human/rollout/strict gates remain |

CHANGE-107 / ARCH-EVO-043: F4.1.8/F10 expiry and NFR3/NFR4 -> retire only unused
HTTP adapter -> OpenApiDocumentationTest proves no expiry operation/routing and
retains all repost paths; OrderLifecycleSchedulerTest and expiry regressions
preserve direct expiry/refund behavior. No new product requirement or peer API.

CHANGE-106 / ADR-037 [~]: NTH4 + F10/F10.1.3 override -> immutable REPOST original/
candidate -> reserve -> atomic OPEN/link/result -> same-key pending UI. ADR-030
initial30min minimum, ADR-033/035 authorization/lease/compensation; NFR3 test/coverage
and NFR4 owner binding. Automated backend/real Credit/PG/frontend verification
passes; local consuming rollout/interactive and strict-record gates remain open.

| Effective requirement | Implementation / approved design | Test and evidence | Status |
| --- | --- | --- | --- |
| NTH4 + F10/F10.1.3 visibility override | ADR-037; REPOST original guard/candidate; atomic manualConfirmed/link/result | PG stable candidate/concurrent keys; actual Credit lost reply/rollback; UI successor replacement | Automated passed, rollout pending |
| ADR-030 manual initial30min window | frozen created_at + explicit expiry, no sliding retry deadline | RepostExpiryTest; actual Credit late compensation and valid late retry | Passed |
| ADR-033/035 consistency + NFR4 ownership | claim/lease/generation, immutable actor/input; minimal existing RESERVE/history/compensation | owner forgery/API wrong-role; pending blocker; real reservation/replay/reversal | Passed in actual Credit harness; User/Supplier fixtures |
| NFR3 line/branch/test gate | unchanged Maven coverage and shared frontend regression | 367 backend0skips,94.76%line/80.23%branch;90 frontend; lint/typecheck | Passed; generic workflow helper format gate not passed |

## CHANGE-105 / ADR-036 effective verification

| Requirement/amendment | Implementation and test responsibility | Gate |
| --- | --- | --- |
| F1/F2/F3/F4/F11 + ADR-033/035/036 | Legacy CREATE/ACCEPT/ABORT coordinator; intent before remote effects, stable keys/candidate, atomic finalization | Controller + real Credit recovery regressions |
| F4.1.7/F4.1.8/F10/F5.1 + ADR-013/031/036 | Local emulator cancellation/expiry refunds, completion transfer; abort after expiry | Actual Credit/PG/emulator tests + real Compose HTTP smoke |
| NFR4 + ADR-036 | Relationship reads, receipt binding/key-reuse conflict, scoped event IDs, cloud lifecycle fail-closed | Unit/API/PG/cloud/local security tests |
| NFR consistency + ADR-036 | Bounded async publication, 15-minute scan, retained FAILED(10), ADMIN redrive | Domain/listener/PG/controller security |
| NFR3.1.1/3.1.3 | >=80% line/branch; real integration vs fixtures distinguished | Exact counts/coverage recorded in CHANGE-105 |

No automatic-repost/delegation or User penalty completion claim. Sprint [~].

## CHANGE-104 / ADR-035: actual financial command recovery

| D1 / amendment | Requirement / implementation | Test evidence | Status |
| --- | --- | --- | --- |
| F1/F2 + ADR-033/035 | Reserve before OPEN; durable intent/candidate; atomic local outcome | Actual Credit replay/lost response/local rollback/restart | Real eight-scenario suite passes; full-stack gates separate |
| F3/F4.1.1/F13 + ADR-033/035 | One worker; pending guard; reverse specific assignment after deadline | Simultaneous same-key claim; cancellation blocked; compensation then expiry intent | Actual Credit/PostgreSQL passes |
| F11.2 + ADR-025/033/035 | Confirm clear before accepted abort history/reopen | Lost clear, history once, replay cannot clear newer assignment | Actual Credit/PostgreSQL passes |
| NFR3.1.1/NFR3.1.3 | >=80% line/branch gate; positive/negative contracts | 25 HTTP fixture tests; eight real-provider tests; full verify evidence in CHANGE-104 | Recorded at handoff |
| ADR-033 security/UI + NFR4 | No saved tokens; pending UI; same-key Continue | 83 frontend tests, including eight recovery hook tests; lint/typecheck | Passing; no live browser/User/Supplier/IAM claim |

Legacy routes remain compatible; enabled browser recovery uses /api/orders/commands.
Repost recovery remains outside this slice. Sprint remains [~].

## CHANGE-103: AsyncAPI documentation representation

| Requirement / amendment | Documented contract | Verification | Status |
| --- | --- | --- | --- |
| F4.1.7, F4.1.8/F10, F11 + ADR-013/025/031 | CANCELLED/EXPIRED refund with old order ID; compact v2 | AsyncAPI parser, DTO/topic parity and refund examples | Documentation verified; live gates unchanged |
| F4.1.5/F5.1, F11 + ADR-031 | Manual/automatic completion compact v2; no inferred overdue facts | Parser, exact field/version checks and completion example | Documentation verified; financial/User gates open |
| F11.2 + ADR-025 | Every accepted abort: User-only actor facts, resulting OPEN/EXPIRED snapshot v1 | Parser, snapshot/nested field sets and both outcome examples | Documentation verified; User subscriber unverified |
| NFR consistency/verification + ADR-013/021/026/034 | Three send-only channels; immediate publication + separate 15-minute recovery; stable IDs | Attribute/topic/cadence parity and negative schema checks | Documentation-only; not live delivery proof |

CHANGE-103 changes no requirements, lifecycle behavior or Sprint completion state.

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


CHANGE-142 (2026-10-18), user-directed presentation refinement: Report detail and submission use one continuous panel, with Order summary before report metadata and aligned definition-list rows. Admin clicks the Order summary to navigate to existing /admin/orders/{id}; no separate View full order details button and no financial actions on Report pages. Only Mark report resolved remains. Existing Order page actions, API/mocks/auth/statuses/polling and DB stay unchanged. GOV.UK summary-list structure informed layout; FoC theme retained.


CHANGE-143 (2026-10-18): Report Order summary has labelled Description and requester/courier display names. Status label sits above one equal-height select/refresh row. Admin Orders live list/details resolve distinct User summary and Supplier detail IDs independently via existing gateway/token transport; never block Orders on name failure or substitute sample data. Loading labels, Deleted labels for404/empty, unavailable for transient errors; copy/navigation IDs preserved. No global name cache, DB-owned names or API/config changes. Live Reports remains Coming soon until FEEDBACK-015 provider exists.


CHANGE-144 / ADR-049: D1 nice-to-have report/penalty system refined by user approval: one current courier-attempt contribution, synchronous admin replacement, local Report open/closed/reply prototype. Live replacement blocked on FEEDBACK-016; Report Service still missing. Delivery-stored overdue and completion-hold guards retained. Acceptance evidence in CHANGE-144; no claim of real User score mutation.


CHANGE-146: D1 NTH report/penalty investigation refined by explicit user request. Held active Order may receive non-waivable highest report policy without funding/score side effects; only requester/scheduler complete; final scoring highest vs overdue; admin cancel explicit/stored only. Negative/rollback/event tests in OrderDeferredPenaltyTest, AdminApplyPenaltyServiceTest, AdminApplyPenaltyControllerTest, DeferredPenaltyJpaIntegrationTest and DeferredPenaltyMigrationTest. Report provider still missing; no new User dependency.


## Effective CHANGE-147 — Report reply privacy and active policy choices (2026-10-19)

Approved Yao Xiang: admin resolution reply is visible only to the submitter and reviewing admins; courier participants still see resolved status/time and other existing case information. Local prototype list/detail responses redact resolutionNote on copied snapshots, and UI hides the reply in courier views. Production Report Service is still missing and must enforce this with verified caller identity, never client-supplied roles. Live Reports remain Coming soon.

COURIER_UNRESPONSIVE is retired from active policy lists and new admin apply/cancel choices. Its enum/amount remain only for historical persisted rows and existing outbox events; immutable migrations and saved penalties are not rewritten or waived. Six active policies; three selectable report penalties (wrong location4, damage4, theft10). Cancellation UI selects one radio case and sends the existing one-element list; stored maximum semantics and existing penalty topics remain unchanged.

Report cards omit Order descriptions; closed cards are muted but remain navigable. Requester/courier names link to admin profiles, IDs have separate copy rows. Admin table names, email, roles and IDs truncate visually while full values remain in titles/copy/details. No sibling backend or shared runtime/CI/cloud configuration changes. See CHANGE-147.


## Report list filters — CHANGE-149

The local Report list GET accepts optional orderId (exact related Order ID, trimmed, max128) and reportStatus (AWAITING_COURIER_RESPONSE, READY_FOR_REVIEW, RESOLVED). Existing status=OPEN or RESOLVED remains the grouping selector; reportStatus intersects it. Empty filters mean all within the group. Scope/filters apply before pagination and totals; filters reset both page counters. The future Report Service must support equivalent filtering on its existing list contract and derive participants/admin from verified identity; this prototype's sample identity flags are never production authority. UI uses the same shared AdminFilterForm/Actions, Select and Input as Order search; normal user and Admin Reports retain their respective scopes. No refresh icon; existing visible five-second polling and error retry remain. HTTP-peer/staging/production Reports still show Coming soon, with no mock fallback.


## Effective CHANGE-153 — Hold eligibility and paused funding (2026-10-19)

Yao Xiang approved placing a requester-owned completion hold on any Order status except COMPLETED. This supersedes delivered-only placement and the old blanket terminal-state rejection for new holds. Preserve status, ownership/role checks, bound report IDs, locked transaction and release history. A hold placed after cancellation/expiry records a hold only; it does not restore lifecycle state or reverse settled funding.

The user explicitly clarified that ordinary cancellation, expiry and courier abort (and their refunds/penalties) must remain paused, in addition to requester/automatic completion. START, PICKUP and DELIVER continue normally; acceptance keeps its existing behavior. Admin cancellation remains the previously approved explicit override. Auto-completion and expiry retain held-row SQL exclusion. Report mock submission fixtures remain delivered-item samples; this change broadens the real Order hold endpoint, not a production Report Service implementation.

No schema, event/topic, frontend, shared configuration or sibling backend change. This is eligibility validation only. See CHANGE-153 and ADR-052.
