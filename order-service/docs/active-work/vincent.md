# Vincent - Active Work

## CHANGE-101/102: JWT learning and merged minute orchestration - locally verified

- Started: 2026-10-10. Vincent confirms current branch
  `sprint-2-3-credit-service-concurrency-update-event-payload` and the merge.
- Approved: JWT learning; one minute job runs eligible recovery -> OPEN expiry
  -> >=48-hour DELIVERED completion, with independent catches and existing guards.
- Scope: Order scheduler/tests/context/diagram/disclosure; no peer/frontend/
  schema/config/deployment changes or live HTTP recovery activation.
- Context: scoped effective loading under ADR-033; initially clean worktree.
  D1/Overall/Sprint-1 fingerprints match. Generic drift helper fails on existing
  manifest columns; not source drift. No nested Order instructions.
- Implemented source/tests commit `10fd76d`: removed independent command timer;
  recovery -> expiry -> completion, lifecycle time captured after recovery;
  per-phase catches, existing command HTTP gate and item locks/guards retained.
- TDD: ownership RED observed; intermediate focused failure was a test-fixture
  verification before its second tick, corrected. Final focused 28 pass (16
  real PostgreSQL recovery cases). Fresh-source Java 21 Maven offline verify
  passes 301 tests, zero failures/errors/skips; 95.60% lines / 81.95% branches.
  Source-copy hashes identical. New acceptance recovery/compensation/expiry
  guard tests use Credit stub; no live Credit crash/HTTP recovery claim.
- Logs: target/change102-red.log, change102-focused.log, change102-verify.log;
  reports in target/change102-verification/target. Cron disabled in test contexts
  only; timer ownership/cadences directly asserted. Existing workflow helper
  manifest/table/status/history format failures remain separately recorded.
- Learning: excluded JWT note and appended shared-minute lesson in existing
  cross-service recovery note. ADR-034/class/sequence/context/sprint/traceability
  and AI disclosure synchronized. No peer/frontend/schema/config/cloud changes.
- Next: human review/rebuild Order when ready; Annablee's real replay/result/
  fencing/compensation contract still required before enabling HTTP recovery.
  Sprint `[~]`; live recovery `[!]`. Personal local profile remains unstaged;
  learning remains excluded. Documentation/disclosure commit recorded below.

## Durable command recovery contract-stub slice — locally verified 2026-10-10

- Vincent explicitly confirms `sprint-2-3-credit-service-concurrency` and approves
  the updated Order/frontend plan: PENDING/COMPLETED outcomes, immutable same-key
  actions, candidate-ID reuse, claims/leases plus existing row locks, user-assisted
  authorization, pending UI and expiry-preserving reconciliation. Credit remains
  read-only; its existing business request bodies must not grow.
- Additional explicit approval: implement and test against a documented contract
  stub; keep live HTTP recovery disabled until Annablee provides and verifies
  historical key replay, result retrieval, fencing and scoped compensation.
- Started: clean worktree and branch inspected; local identity reconciled without
  deleting Yao Xiang's historical profile. Relevant actual Credit routes and
  transactional replay rechecked: base routes exist, recovery protocol is
  INCOMPLETE_OR_INCOMPATIBLE (existing FEEDBACK-003/006), not live verification.
- Implemented: ADR-033 / ARCH-EVO-038 / CHANGE-100; durable V5 intent/lease/
  generation/target guard, safe atomic finalizers, same-key stub historical replay
  and conditional reversal; owner-only API/discovery; pending/account-scoped
  IndexedDB UI and auth-only Continue. Requested Credit cooperation handoff is
  concurrency/concurrency-order-credit.md; FEEDBACK-010 extends 003/006.
- Backend verification: final Java 21 Maven verify **298 passed, 0 failures/errors/
  skips**, including real PostgreSQL command recovery/migrations and ten legacy
  race cases. Fresh coverage **95.76% lines / 81.82% branches**. No gate reduction.
  TDD red/green details and timer-isolation commands are in CHANGE-100.
- Frontend final: **73/73 tests**, lint **0 errors / 12 pre-existing unrelated
  warnings**, Next route generation/typecheck and production build passed.
  No native authenticated browser/IndexedDB QA claim.
- Local atomic commits: `2e2b263` approved design/contract-stub handoff and
  disclosure; `3dd1bf8` backend recovery/migration/tests; `5dea2ee` shared
  frontend pending-command recovery/tests. No push performed. Only the personal
  `docs/local/developer-profile.md` identity reconciliation is left uncommitted;
  learning remains excluded. Final verification handoff records these hashes.
- Source drift: approved PDF SHA256 values match. Generic format/drift helpers
  do not accept historical local table/text formats and FAIL separately; existing
  committed AI log merge markers preserved, no false all-workflow-passed claim.
- Blocking dependency: actual Credit historical key replay/result/fencing/safe
  compensation missing (INCOMPLETE_OR_INCOMPATIBLE). Credit and all sibling/
  infrastructure code unchanged; HTTP recovery hard disabled. Required action:
  Annablee agrees/implements handoff; re-inspect and run real contract/crash/restart
  tests before live enablement. Stub financial state is not durable across restart.
- Next: peer implementation, approved real HTTP adapter, native IndexedDB/
  authenticated desktop/mobile browser QA and operational/scale-to-zero review.
  Repost background retries and delegated credentials remain deferred. Learning
  updated but excluded; local profile is personal and not staged. Sprint [~].

## Automatic retry versus optional retry control clarification - 2026-10-10

- Started/finished: Vincent asks why a Retry/Check-status button is needed when
  recovery should be automatic. Clarification of the proposal only: retry button
  is not required for authorized automatic recovery; status polling is automatic.
  Fresh user authorization/sign-in may require an explicit Continue control.
- Current no-stored-token/deferred-service-identity proposal cannot promise
  financial background replay after a crash/logout. Same-key optional retry is
  not a substitute for a scheduler, and missing authorization does not become
  terminal failure. No UI decision, peer contract or implementation selected.
- Verification: workflow/profile/proposal summary and Git branch/state inspected;
  no runtime/source re-verification or tests required/run for this clarification.
  Existing plan, 003/005/006 and full pre-coding gate remain pending; Sprint [~].
  Only this active-work clarification changed; prior dirty records preserved.
  Next: approve/modify the existing plan and agree peer recovery/auth contracts.

## Three-Credit-command recovery implementation plan - 2026-10-10

- Started: Vincent requests a plan first for durable frontend keys, Order/Credit
  replay, lease-safe minute recovery and user-assisted authorization renewal for
  reservation, assignment and abort reset. This is NOT permission to implement,
  alter Credit, resume deferred background credentials or deploy infrastructure.
- Identity/allocation/branch match sprint-2-3-credit-service-concurrency; Sprint
  remains [~]. Preserve existing uncommitted advisory records; peers read-only.
- Rechecked actual frontend key builders/useApi, creation/assignment/transition
  services, CreditServicePort/HttpPeerAdapters, receipt and row-lock implementation,
  Credit controller/transactional reservation/assignment/reset and relevant test
  source. Credit's endpoints exist, but historical command-key result/recovery
  and attempt-fencing semantics remain INCOMPLETE_OR_INCOMPATIBLE (003/006).
- Working proposal: PENDING/COMPLETED plus outcome, ownership lease/generation,
  retry timing and separate safe reason metadata; immutable key/input binding,
  per-command and per-order claims, user-authenticated resume after crash rather
  than persisted tokens or auth bypass, existing synchronous/outbox boundaries.
- Gate: required broad historical rehydration is not complete; some large reads
  truncated. No full source-drift/workflow/implementation-ready claim. Complete
  that gate and resolve pending detailed design/provider approvals before coding.
- Finished: appended the candidate schema/classes/sequence/locking/auth/UI/API/
  test plan to PROPOSED ARCH-EVO-034, existing FEEDBACK-006 and excluded learning,
  plus AI disclosure. Two-state representation is proposed with safe reason
  metadata; no selected wire contract, migration number, storage or source added.
- Verification: git diff --check passed (existing CRLF normalization warning),
  learning local links/fences passed, learning remains Git-excluded. Manual D1
  and selected Overall SHA256 match recorded values. Git shows only the four
  pre-existing tracked advisory paths changed; no source/peer/frontend/test/schema
  changes. No tests/live calls/full workflow audit claimed; no commit or push.
- Blocked implementation: actual Credit's historical operation replay, authorized
  result retrieval, stale-attempt fencing and compensation require Annablee's
  agreement (003/006); unattended auth remains paused (005). User-assisted recovery
  may be a separately approved interim milestone, not falsely automatic finance.
  Current effective application files and peer routes remain unchanged.
- Next: Vincent approves/modifies the plan and UI/key/storage/claim/guard scope;
  peer agrees provider contract and missed-deadline recovery; finish full context/
  traceability/source-drift gate, synchronize approved ADR/change/diagrams/contracts,
  then TDD. Uncommitted docs/architecture-evolution.md, docs/peer-service-api-feedback.md,
  docs/active-work/vincent.md and ../ai/usage-log.md preserve earlier advisory edits;
  withheld commit because proposal/human review pending and prior edits overlap.

## UI-to-Credit durable command flow review - 2026-10-10

- Started: Vincent requests a step-by-step rationality review of PENDING/
  RETRYABLE/COMPLETED activities and frontend-generated keys shared with Credit.
  Design discussion only, no application, peer or migration approval. Profile,
  allocation and sprint-2-3-credit-service-concurrency match; Sprint [~].
- Rechecked frontend payload builders, CreateOrder/OrderActor DTOs, Order adapter,
  Credit controller and transactional replay/assignment/reset code, current
  ADR-018/027, proposal 034 and feedback 006. Existing frontend generates UUID
  commandId on each payload build; Order does not forward an activity key in the
  three active HTTP mutations. Current peer historical-key replay not established.
- Finished requested design review: extended the existing excluded learning,
  PROPOSED ARCH-EVO-034 follow-up and existing FEEDBACK-006 plus disclosure.
  Records explicit transaction boundaries and client key lifecycle, optional peer
  PENDING versus atomic effect/result, timeout handling, leases/concurrent UI retries
  and authoritative terminal outcomes. No implemented protocol or contract selected.
- Verification: git diff --check, learning local links and balanced fences pass;
  learning remains Git-excluded. No runtime calls, tests, cloud/DB mutations or
  application/peer/frontend/test/schema/infra edits. Full historical workflow audit
  not claimed; some broad record reads truncated. No source-drift completion claim.
- Next: user reviews proposed flow; Annablee agrees 003/006 scoped historical
  operation identity/result/retention and compensation; 005 auth remains paused.
  Detailed source/schema/API/UI/TDD approval still required before implementation.
  Current docs/architecture-evolution.md, docs/peer-service-api-feedback.md,
  docs/active-work/vincent.md and ../ai/usage-log.md stay uncommitted pending human
  proposal review, preserving prior advisory edits; learning excluded. No push.

## Two-state whole-handler replay clarification - 2026-10-10

- Started: Vincent challenges whether explicit recovery phases are necessary
  when peer deduplication permits whole-function replay. Explanation-only follow-up,
  not worker/schema/peer-contract approval. Vincent/profile/branch match; scope
  remains Order-owned, peers read-only, Sprint [~], background retries paused.
- Inspected current creation/receipt and Credit reservation/assignment/reset
  replay code plus existing ARCH-EVO-034 and ADR-018. Explained that PENDING/
  COMPLETED is viable if COMPLETED stores success/rejection outcome and the
  handler is idempotent end to end. Explicit phases are optional, not mandatory.
- Updated the existing excluded learning note and disclosure only. Required
  pre-I/O durable inputs/IDs, peer atomic effect/deduplication/equivalent result,
  local atomic effects/completion, concurrency/deadline/current-state checks and
  compensation. Current peer calls do not all have historical activity-ID replay.
- Finished: git diff --check and learning links/fences pass; learning still
  Git-excluded. No tests or runtime calls,
  full historical workflow audit not claimed (partial/truncated context reads).
  No code/design/contract implementation selected. Next: human review and explicit
  detailed design/peer agreement before any implementation. Existing dirty proposal
  records preserved, new advisory docs uncommitted; no commit/push.

## Durable pending-activity scheduler clarification - 2026-10-10

- Started: Vincent asks whether pending-before-HTTP plus minute-based recovery
  can make unsafe outbound operations consistent. Advisory evaluation only,
  not application/worker/contract approval. Profile/allocation/current branch
  match sprint-2-3-credit-service-concurrency; peers read-only and Sprint [~].
- Current context/ADR-027/018, proposal ARCH-EVO-034, outbound adapter/creation
  and Credit reserve/replay/assignment/reset rechecked. Broad historical reads
  partly truncated; full workflow audit not claimed. Initial guessed ADR-027
  filename failed; actual listed ADR file subsequently read successfully.
- Finished advisory clarification; updated the existing excluded crash-recovery
  learning and AI disclosure. Explain independent committed intent, atomic local
  finalization, terminal success versus rejection, ambiguity reconciliation, safe
  idempotency, attempt-aware stale replay prevention and worker claims.
- Verification: git diff --check and learning links/fences pass; learning remains
  Git-excluded. No runtime requests, application/peer/tests/schema edits or tests
  run. No full source-drift/completion audit claimed for this advisory follow-up.
- Next: review the proposal, then approve operation-specific designs and agree
  FEEDBACK-005/006/003 before implementation. Background retry pause unchanged;
  a minute scan is recommended, not selected/implemented as a new schedule.
  Current-turn active-work/AI/learning edits uncommitted; previous proposal edits
  in architecture-evolution/peer feedback preserved. Human review pending, no push.

## Creation/credit crash recovery learning and proposal - 2026-10-10

- Started: Vincent asks for outcome classification, a solution proposal and
  general learning documentation, not implementation. Profile/allocation/branch
  match sprint-2-3-credit-service-concurrency; Sprint 2-3 remains [~]. Peers read-only.
  Existing active-work/AI notes preserved. Cross-service recovery previously
  deferred; this turn authorizes investigation/documentation only, not a worker.
- Inspected current CREATE transaction, ignored reservation response, actual
  Credit PUT/GET/replay/transaction/error paths and existing tests/FEEDBACK-006.
  D1/selected Overall SHA-256 match. Current outbox covers committed outcomes,
  not creation intent. Broad historical context batches partly truncated; full
  workflow completion audit not claimed. No application/peer/runtime/cloud edits.
- Finished the requested proposal/learning task: added excluded learning/
  cross-service-state-change-crash-recovery.md (349 lines); registered PROPOSED
  ARCH-EVO-034; appended discussion to existing FEEDBACK-006; added AI disclosure.
  Covers outcome classification versus retryability, durable intent before remote
  write, stable candidate/command binding, active-state confirmation, crash matrix,
  forward recovery/compensation, late-write fencing, alternatives and future tests.
- Verification: git diff --check passes (existing CRLF normalization warning);
  learning has four balanced fence markers and all local Markdown links exist.
  git check-ignore confirms learning remains excluded. Exact change scope is
  documentation/disclosure only; no application/peer/test/schema/infra edits.
  No unit, distributed recovery, browser or live peer tests run for this proposal.
- Remaining: explicit detailed-design approval, agreed pre-creation compensation/
  late-write terminal protocol (FEEDBACK-006), delegated authorization (005),
  versioned migration/worker/UI contracts and actual fault-injection integration.
  Background retries remain paused. No effective ADR/contract/design superseded.
- Uncommitted paths: docs/architecture-evolution.md, docs/peer-service-api-feedback.md,
  docs/active-work/vincent.md and ../ai/usage-log.md; local learning file excluded.
  Proposal awaits human review; mixed pre-existing advisory notes in active-work/
  disclosure preserved and not staged. No commit/push. Next: review proposal and
  agree provider recovery/auth before any implementation; Sprint remains [~].

## Credit synchronous response-code clarification - 2026-10-10

- Follow-up: explained identical replay as same business orderId, requesterId
  and amount. Existing Credit replayReservation compares requester/amount and
  returns existing reservation/current account without reserving again; mismatch
  returns 409. Replay does not require RESERVED status, so 200 alone is not proof
  of an active hold. Existing terminal replay/recovery feedback remains; no fix,
  runtime request, tests or application/peer change. Advisory clarification done.
- Started/finished: same Vincent/branch/scope, advisory source inspection only.
  Re-read CreditController, ErrorCode, GlobalExceptionHandler, JsonSecurityHandlers,
  reservation/assignment/reset persistence branches and Order adapter/error advice.
- Provider success: reservation 201 new / 200 identical replay with reservation
  JSON; assignment and reset exactly 200 without body, including accepted replay.
  Provider errors: 400 validation, 401 authentication, 403 permission/self/assignment,
  404 missing account or reservation, 409 insufficient credits/reservation conflict,
  503 role lookup/persistence failure, 500 unexpected errors. Reset 404 is reservation;
  insufficient credits applies to reservation, not assignment/reset. Routing/media
  failures are separate from business outcomes; transport failures have no HTTP code.
- Order reservation translates peer errors; assignment/reset propagate RestClient
  errors with no equivalent advice mapping, so client-facing results can be generic
  500 rather than the original peer status. Report this existing gap, no fix selected.
  Timeout/lost acknowledgement may follow remote success; existing recovery deferral
  unchanged. Tests/runtime requests not run; no source/contract/schema/peer edits.
- Next: developer review. Static findings do not verify live integration or close
  Sprint gates. Existing uncommitted records retained; no commit/push.

## Outbound state-modifying interaction audit - 2026-10-10

- Started: Vincent requests an existing-source inventory, not implementation or
  a new contract. Profile/allocation/branch agree on
  sprint-2-3-credit-service-concurrency. Order-owned advisory scope; all peers
  read-only. Preserve prior records; no runtime API, database or cloud mutations.
- Inspection scope: HTTP adapters/ports and production callers, security role
  client, transition/lifecycle/repost services, outbox dispatch, event DTOs and
  topic settings; relevant actual User/Supplier/Credit providers and current
  contracts/ADR-025/026/feedback. No architecture change or approval proposed.
- Result: three active HTTP mutations to Credit: PUT reservation (creation and
  both repost paths), PUT courier-assignment (acceptance), POST hold-for-reopen
  (every ACCEPTED-only abort). Three event streams: OpenOrderRefundTaskEvent for
  CANCELLED/EXPIRED to Credit, OrderCompletionTaskEvent for requester/scheduled
  completion to Credit and intended User facts, AcceptedOrderCancellationTaskEvent
  for every abort to User only. Topics are environment-configured, not necessarily
  the default dev names. User subscriber/live processing remains unverified.
- Supplier POST validate performs repository reads only; User role-context and
  courier-eligibility GET implementations only read user data. Start/pickup/deliver
  have no additional peer business mutation. Legacy settle HTTP method exists but
  has no production caller; completion uses its event, not that missing route.
- Audit finished: response inventory ready. Non-safe means intended business
  mutation, not insecure or non-idempotent. Publication ACK is not financial
  completion. Next action: developer review; deferred cross-service consistency
  and peer consumer gates unchanged. No source/tests/contracts/schema changed.
- Verification limitations: static inspection only; no test run or live consumer
  verification. Larger historical workflow-context output was partly truncated;
  no full context/gate/source-drift audit or Sprint completion claimed.

## CHANGE-093: Courier acceptance versus requester cancellation - 2026-10-09

- Test slice locally verified. Vincent explicitly approves the fifth real PostgreSQL race on
  sprint-2-3-credit-service-concurrency. Profile/allocation match; preserve prior
  uncommitted advisory/disclosure notes. Scope: Order test only, both winner
  orderings, actual row contention, one outcome/version/checkpoint/receipt,
  no losing Credit assignment and refund only for winning cancellation.
- Effective references: F3/F13, F4.1.7, NFR3; existing ADR-017/018/019 and
  CHANGE-092 test harness. Existing source locks both paths; this characterizes
  already-implemented behavior, not a new production feature or API assumption.
  User approval covers test design described in the preceding status turn.
- D1 and selected Overall PDF SHA-256 match recorded fingerprints. Java 21 and
  Docker 27.5.1 available. Broader historical context batches partly truncated;
  no full workflow audit, new peer contract verification or Sprint completion
  claimed. Existing generic workflow-format and live integration gates remain.
- New parameterized test added before any production edit; existing behavior
  passed first execution unchanged, not an invented red/green claim. Focused pair
  2/2; full concurrency class 10/10 + 13 service regressions = 23/23; separate
  actual domain/transition regressions 36/36. Zero failures/errors/skips in each
  Java 21 offline Maven test run. No fresh full verify/JaCoCo/frontend/live test.
- Generic drift checker exits 2 (manifest headers incompatible), while manual
  PDF fingerprints match. verify_gate exits 1 with five format/history issues;
  no unrelated workflow repair or full Sprint completion. Patch check passes.
- Atomic test commit 827844b and separate verification-record commit 0f50886;
  no push. New traceability/change/context records
  and ignored learning updated. Prior uncommitted advisory/disclosure changes
  preserved; active-work/AI disclosure remain unstaged so unrelated prior notes
  are not mixed into the verified test commit. Next: developer review and existing
  live integration/cross-service recovery work; no additional lock needed.
- Final checks: learning links/fences, TOML parsing and staged diff checks pass.
  Only pre-existing/mixed advisory files docs/active-work/vincent.md and
  ../ai/usage-log.md remain tracked/uncommitted; learning stays Git-excluded.

## Acceptance/cancellation lock confirmation - 2026-10-09

- Follow-up test inventory inspection confirms exactly four parameterized race
  methods/eight cases; no dedicated accept-versus-cancel test has been added.
  Saved report: eight cases, zero failures/errors/skips, not a fresh execution.
  To verify this fifth pair, test both winner orderings and assert one state,
  no losing Credit assignment and refund intent only when cancellation wins.
  User asked status only; no tests/source added on their behalf.
- Lightweight source confirmation, Vincent on matching concurrency branch.
  accept, cancel and cancelAccepted are transactional and call findForUpdate
  before domain mutation; adapter delegates to PESSIMISTIC_WRITE JPA query.
  Acceptance holds the lock during Credit assignment; abort during Credit reset.
- Same-order accept versus requester cancel is serialized by this existing path;
  loser must revalidate state/version. That exact pair is not one of CHANGE-092's
  four dedicated race tests. No new runtime tests, implementation or design change.
  Existing notes/disclosure preserved; this active-work note is uncommitted for
  review. No further implementation is needed merely to add a redundant lock.

## Concurrent locking learning document - 2026-10-09

- User requested a separate learning Markdown explaining SQL locks and flow.
  Vincent/profile/allocation match sprint-2-3-credit-service-concurrency;
  documentation-only scope, no new architecture or implementation approval needed.
- Created ignored learning/concurrent-order-locking-sql-and-flow.md: Java/JPQL
  to observed Hibernate FOR NO KEY UPDATE SQL, commit/rollback/outer-transaction
  lifetime, four transition races, completion NOWAIT, batch scheduler locks,
  versions/receipts/outbox, safe SQL diagnostics, alternatives and remote Credit
  partial-success limitation. Prior learning and existing uncommitted notes preserved.
- Inspected actual assignment/transition/lifecycle/domain/repositories/tests,
  V1/V3 migrations, AFTER_COMMIT listener and saved SQL/test reports. Existing
  eight-case report remains zero failures/errors/skips; no new Maven/Docker/SQL
  execution or live peer verification. Parent/nested context batch was truncated;
  no complete historical workflow audit or full completion gate claimed.
- Documentation validation passed: 26 balanced fence markers, 12 local links
  resolve, git diff --check passes (existing CRLF notice only), and git
  check-ignore confirms the new learning file is excluded. Source/contracts/schema/diagrams,
  deployment and peer files unchanged. AI disclosure appended separately.
  Learning remains Git-excluded; active-work/disclosure left uncommitted for review,
  not mixed into prior user changes. Next: user reads guide; existing Sprint and
  deferred integration/recovery blockers remain unchanged.

## Staging Swagger URL clarification - 2026-10-09

- Lightweight read-only workflow; Vincent/profile/allocation/current branch match.
  application.yaml configures /api/orders/docs and /api/orders/v3/api-docs;
  gateway forwards /api/orders to Order. No source/configuration change needed.
- Live browser lookup could not access the staging docs URL; deployed page is
  not independently verified. User can open the configured route in their browser.
  No tests rerun; existing blockers unchanged. This active-work note remains
  uncommitted for review alongside prior advisory notes.

## Acceptance lock lifetime clarification - 2026-10-09

- Advisory-only task, Vincent on sprint-2-3-credit-service-concurrency. Inspected
  OrderAssignmentService.accept and controller call site: default @Transactional,
  getForUpdate before Credit assignment, repository save and method return.
- PostgreSQL releases the row lock at physical transaction commit/rollback,
  not repository save or Java method-body return. Spring normally commits after
  that return and before handing the result to the controller; an existing outer
  REQUIRED transaction keeps the lock until the outer transaction ends.
- No source, test, contract, schema or architecture change; no tests rerun.
  Existing sprint/integration blockers remain unchanged. Learning clarification
  recorded in the existing ignored learning document. Active-work update remains
  uncommitted for review; prior status notes preserved. No implementation next
  step is required for this terminology question.

## Staging ACCOUNT_NOT_FOUND diagnosis - 2026-10-09

- Follow-up clarification: missing migration/provisioning is a suspicion, not
  a proven live root cause. Firebase login and Credit account lookup are separate.
  User's pasted chat confirms previous discussion only, not database evidence.
  No new cloud/database tests or repair; existing documentation remains pending
  review and uncommitted. Next: Credit owner checks deployed database/UID/history.

- Read-only diagnostic task, Vincent on sprint-2-3-credit-service-concurrency;
  profile/allocation match, clean worktree at start. User asks why an existing
  login has no Credit account. No approval to provision cloud funds, reset data,
  change signup/login behavior or edit peer source.
- Source inspection completed: AuthProvider.signIn calls Firebase and email
  verification only, not Credit provisioning. signUp calls User creation then
  POST /api/credits/registration-facts with self bearer token and eventId/userId/
  occurredAt. Credit GET /me selects caller UID; getAccount throws the supplied
  ACCOUNT_NOT_FOUND when JpaCreditRepository cannot find credit_accounts.user_id.
- Local staging configuration selects credit_staging via Cloud SQL. Credit
  history includes 53db4ec replacing Firestore with PostgreSQL; current V1 creates
  schema, not old-user backfill. Old signup without provisioning, missing data
  migration or a wrong deployed datasource are plausible, not proven causes.
- Existing initialization endpoint inserts a missing account with 50 credits
  and an initial-allocation ledger entry; it does not restore previous balance/
  reservations/ledger. Credit owner must check existing historical funds and
  correct staging datasource before choosing migration or authorized provisioning.
- No cloud/SQL/API calls, tests, production/peer/frontend changes or financial
  writes. Local source diagnosis only; live cause remains unverified. Learning
  updated in ignored existing directory. Next: owner inspects deployed datasource
  and UID row/history; no delete/re-signup/reset recommendation. Sprint unchanged.

## Real PostgreSQL concurrency verification - 2026-10-09 / CHANGE-092

- Status follow-up: re-read saved Surefire/JaCoCo reports at HEAD 3d26ff8;
  all eight named races pass with zero failures/errors/skips; recorded Maven
  full-run result is 215 tests. The directory-wide XML sum is 219 because older
  reports remain, so it is not a fresh-run total. Coverage is 95.67% lines /
  84.70% branches. Production and race-test
  source unchanged since 1906afb. No new execution or live peer verification
  performed; this confirmation note left uncommitted for review.

- VERIFIED test unit: user confirms branch sprint-2-3-credit-service-concurrency for
  the four Order races. Clean worktree at start; profile/allocation reconciled.
- Test-only design: actual Spring services, independent threads/transactions,
  isolated PostgreSQL 15/Flyway. Gate winner after real lock; verify competitor
  blocked via pg_stat_activity/pg_blocking_pids. Both winner orderings; assert
  state, actor, version, checkpoint, receipt and outbox counts.
- User/Credit and external dispatch mocked. No application DB/cloud changes.
  Cross-service recovery explicitly deferred; Docker engine reachable.
- Relevant D1: F3/F13, F4.1.5/F5.1, F4.1.7, F4.1.8/F10; NFR3.
  ADR-018/020/025/026 retained. No requirements/design/migration change.
- D1/selected Overall hashes match. Historical context output partly truncated;
  no complete historical audit or Sprint completion claimed.
- Initial focused execution: 8 tests, 1 fixture-precision failure (nanoseconds
  versus PostgreSQL microseconds); persisted-deadline read-back correction.
  Focused rerun: 8 passed, zero failures/errors/skips. No production edit.
- Full Java 21 command: mvnw.cmd -o -B -ntp -Djacoco.append=false verify.
  BUILD SUCCESS: 215 tests, zero failures/errors/skips; includes the eight races
  again. Fresh coverage: 1392/1455 lines (95.67%), 465/549 branches (84.70%);
  both configured 80% gates pass. All required race tests ran against real PG15.
- Completion scheduler loser returns SQLSTATE 55P03 under existing NOWAIT policy;
  its next pass returns zero. Seven other cases prove waiting via PostgreSQL
  blocking PIDs, then correct state revalidation and single committed outcome.
- No full-stack/browser/live Credit ledger or real Pub/Sub delivery claim. No
  frontend, production source, migrations or peer files changed. Learning updated
  in existing ignored directory, not staged. Full Sprint remains [~].
- Follow-up races: manual/auto repost; abort/start; same-command duplicate replay;
  duplicate lifecycle replicas; dispatch/recovery lease overlap. Recommendations
  only, not implementation approval or verified coverage of those other races.
- Cross-service partial-success recovery remains user-deferred to next branch.
  Earlier UI abort-label request remains separate pending work.
- Test-only atomic commit: 1906afb, reviewed staged patch/diff check; no push.
- Supplied generic workflow scripts do NOT pass: drift exit 2 requires different
  manifest column names; record-gate exit 1 reports 3 existing-format/history
  issues (traceability columns/ID and historical active-work requirement ID).
  Manual SHA256 matches approved D1/Overall. This is not business/test failure,
  but full workflow completion remains blocked; no historical records rewritten.

## Simultaneous acceptance inspection - 2026-10-09

- Vincent asks whether concurrent courier acceptance is protected and requests
  adding protection if absent. Branch/profile match sprint-2-3-credit; worktree
  clean before inspection. Task resolved as existing-source verification, not
  a new application, peer, schema, deadline-policy or frontend change.
- OrderAssignmentService.accept is transactional. Its getForUpdate call reaches
  JpaOrderRepository.findByIdForUpdate with PESSIMISTIC_WRITE before validating
  acceptance and before credits.assignCourier. The lock spans the peer call and
  local persistence through transaction completion. Status, expected version,
  self-acceptance, existing assignment and deadline are checked. A competing
  distinct acceptance must acquire that same database row lock before Credit.
  No duplicate lock or process-local function lock added.
- Existing code also revalidates the deadline AFTER Credit confirmation. Earlier
  chat advice to determine eligibility only before Credit was a recommendation,
  not approval or the implemented rule. Preserve the current behavior. Remote
  assignment success followed by expiry rejection, timeout ambiguity or local
  rollback remains the existing FEEDBACK-006 recovery gap; row locking alone
  does not make the two services atomic. Receipt actor-binding gaps are separate.
- Executed with Java 21: mvnw.cmd -o -B -ntp
  -Dtest=OrderAssignmentServiceTest,OrderApplicationServicesTest,OrderDomainBehaviorTest,OrderAggregateTest,OrderPersistenceAdapterTest
  test. Result: 33 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS.
  Tests use mocked peers/repositories: no actual two-transaction PostgreSQL race,
  live Credit call, browser flow or coverage gate was exercised by this command.
- Full historical context reads were truncated; no full workflow/completion claim
  or behavior change is made. Tests compile existing source; no implementation
  tests added. Application databases, containers and cloud resources unchanged.
- Next verification: approved real PostgreSQL simultaneous-acceptance regression
  with two independent transactions, one winning assignment and loser making no
  Credit call; then peer-owner-approved uncertain-outcome recovery. Current
  concurrency implementation exists but that end-to-end race gate is unverified.
  Sprint remains [~]. This entry and AI disclosure await developer review.

## Order-to-Credit payload audit - 2026-10-09

- Vincent requests checking every Credit endpoint/event for sufficient, minimal
  information on sprint-2-3-credit. This replaces the interrupted automatic
  scheduler implementation preflight; no scheduler/source edits were made there.
  Clean worktree at start. Scope is read-only application/peer inspection plus
  mandatory Order feedback/handoff/learning/disclosure records, not schema changes.
- Inspected Order ports/adapters/call sites, event DTOs/factory/mapper/publishers,
  Credit controllers/DTOs/consumer/service/JPA ledger/idempotency, existing tests,
  browser Credit signup/balance calls and local/cloud routing configuration.
  ADR-011/018/025 retain the approved full snapshot and financial/User boundaries.
  D1/selected Overall hashes match. Full historical context rehydration remains
  incomplete due to truncated output; no full workflow/completion claim.
- Active backend HTTP payloads match Credit: PUT reservation has requesterId and
  amount; PUT assignment has courierId only; POST hold-for-reopen has no body.
  Business order ID is in each path and current Firebase bearer in the header.
  Creation/manual/auto repost share reservation; abort reset retains funds.
  Browser signup sends eventId/userId/occurredAt; /me is a bodyless authenticated
  read. Neither requires order descriptions, location details, balances or roles.
- Refund/completion events contain all fields used for validation, matching the
  reservation, ledger attribution and duplicate-payload hashing. Credit reads
  six of fifteen Order snapshot fields: id/version/requesterId/courierId/
  offeredCredits/status. It does not access the other nine listed in feedback
  section 7. Overdue facts remain consumed/hash-bound and shared with User; they
  do not change the transferred amount. Full snapshots are approved but are not
  minimal for Credit. Any reduction needs explicit approval and consumer/version/
  queued-message compatibility review, not silent field deletion.
- Runtime read-only outbox field-name queries confirm all three serialized event
  types have the full snapshot. At inspection: 11 refund published/1 pending,
  3 completion published and 5 penalty published. PUBLISHED means broker accepted
  publication, not Credit committed a refund/transfer. No ledger success claimed.
- Local-live setup intentionally creates only Credit refund/completion
  subscriptions; accepted-cancellation is User penalty only. Credit's legacy
  ABORTED refund consumer/provisioning remains incompatible (FEEDBACK-007).
  Dormant settle adapter points to a missing HTTP /settlement route, but no
  production caller uses it; completion uses Pub/Sub. Do not request that route
  as a new requirement or reactivate the obsolete path without approval.
- Verification: source/call-site/test-source inspection, static field-access
  comparison and runtime SELECTs only. No Java/FE suite, live financial mutation,
  Pub/Sub message, cloud change or browser action performed. No peer files edited.
- Next: user decides whether to retain the approved snapshot or coordinate a
  minimal event contract with Credit/User owners. FEEDBACK-003/005/006/007 remain
  open; background retry/delegated authorization remains paused. Section 7 of
  peer feedback owns the detailed audit; learning remains ignored; Sprint [~].

## Automatic repost runtime diagnosis - 2026-10-09

- Vincent requests a read-only check on sprint-2-3-credit: automatic repost
  should create a new OPEN order, hide its linked EXPIRED predecessor and become
  visible through polling. Worktree clean at start; no fix, financial action,
  database write, container restart or cloud mutation authorized/performed.
- Scope/profile/current sprint and ADR-025/026/027/028/030 records inspected.
  D1/selected Overall fingerprints match. Full historical mandatory context
  rehydration remains incomplete due to truncated outputs; no design change or
  complete workflow/Sprint gate is claimed. Frontend/peer application unchanged.
- Confirmed source AND running compiled scheduler references only expireDue and
  autoCompleteDue. No repostDue reference in that scheduler. Repository search
  finds its caller only in POST /api/orders/internal/lifecycle/repost, and no
  automatic caller configured in inspected Compose/infra/scripts. Minute expiry
  does not itself trigger automatic repost. This is an existing implementation
  gap, not a new decision to omit the approved NTH4 behavior.
- Running modes: prod, ORDER_PEERS_MODE=http, User roles=http. Internal repost
  forwards the lifecycle secret as a Bearer credential to Supplier/Credit; it is
  not a Firebase requester token. FEEDBACK-005 trusted delegated authorization
  is still proposal-only and all background retries remain explicitly paused.
  Do not bypass authentication, store permanent user tokens or add a retry worker.
- Read-only application SQL at 09:12 UTC: Test1.4 was EXPIRED at 08:45 UTC,
  automatic due 09:00 UTC, saved new expiry 09:15 UTC. It already has a linked
  OPEN successor titled Test1.4 (Manual Repost), created 08:52:13 UTC, with a
  MANUAL_REPOST receipt and a different business ID. Its plan is marked used.
  Therefore this particular order was manually reposted BEFORE its automatic
  due time; it must not create another automatic successor. Singapore times:
  old expiry 16:45, manual success 16:52, auto due 17:00, saved auto expiry 17:15.
- Running compiled requester query contains the EXPIRED/repostedOrderId filter;
  read-only SQL evaluates zero visible predecessor rows. Current My Requests
  replaces page.items through authenticated visible-page 15-second polling,
  rather than appending stale cards. No connected browser surface is available,
  so actual signed-in DOM/network refresh remains unverified.
- Fresh focused frontend run: 12 tests / 4 files pass (visible polling,
  authenticated lists, history pages, repost visibility). No backend suite or
  new live automatic/financial flow executed. Existing scheduler test explicitly
  verifies only expiry/completion; passing tests do not cover a scheduled repost.
- Next: user/peer agreement on FEEDBACK-005 and explicit resumption before an
  authorized automatic trigger can be implemented/tested end-to-end. For the
  current manual successor, inspect the user's authenticated My Requests network
  response if a stale card remains. Retain original history/refund references.
  Findings recorded here and in AI disclosure; learning stays ignored. Sprint [~].

## Field-specific creation errors and 30-minute repost expiry - 2026-10-09

- Vincent requests an Order/shared-frontend validation slice on sprint-2-3-credit:
  show only actual invalid fields and reasons; automatic expiry at least 30
  minutes after scheduled repost time, manual expiry at least 30 minutes after
  submission. No peer implementation, auth, broker or retry changes requested.
- Inspection: NewRequestPage replaces every HTTP 400 with an unrelated all-rule
  message. ApiError/OrderExceptionHandler already support details[field,message].
  Order.open bundles domain failures into one message; creation calls Supplier
  before local pair validation. Identical IDs, not similar display names, define
  the existing same-supplier rejection. Preserve that approved distinction.
- ADR-028/RepostPlan/V4 currently permit any expiry strictly after due time.
  createRepost accepts any future expiry; the manual UI alone checks 30 minutes.
  Existing automatic plans with a 15-minute window were valid under the old rule.
  Do not silently disable, extend or rewrite those saved instructions.
- Proposed slice: inline accessible field messages plus a summary of actual
  errors; populate the existing backend detail envelope without changing its
  shape; validate before Credit reservation; enforce new automatic plans at
  due+30 minutes and manual requests at submission+30 minutes. Retain quarter-hour
  UI slots, UTC APIs, identity/ownership/idempotency and approved late-auto behavior.
- APPROVED: Vincent explicitly chooses grandfathering already-saved automatic
  plans; implement the constraint for upcoming plans only. CHANGE-091 / ADR-030
  records the approved Order/frontend scope. No schema migration is needed.
- Local implementation verification: fresh Java 21 wrapper verify passes 207
  tests, zero failures/errors/skips; isolated PostgreSQL proves grandfathered plan
  hydration/execution. JaCoCo 95.33% line / 84.52% branch, unchanged >=80% gates.
  Tests-first failures and fixture corrections are recorded in CHANGE-091.
- Frontend: 51 tests /15 files pass, lint zero errors/12 pre-existing warnings,
  typecheck and production build pass. Creation shows actual inline field errors,
  including server details; manual expiry feedback is inline too. Saved plans,
  auth/peers/events/schedulers/schema/retries remain unchanged.
- D1/selected Overall hashes match. Large context outputs were truncated and not
  all historical mandatory records were rehydrated. This is a workflow gate
  limitation, not a claim of complete context compliance or full Sprint completion.
  Existing generic checker format/history failures remain separate, not passed.
- Next: human review, remaining context/gate audit and authenticated desktop/mobile
  browser verification after Order/frontend rebuild in the existing Compose
  profile. Live peers/cloud not retested. No application database/tunnel/container
  restart or reset; background credentials/retries remain paused; Sprint [~].
- CHANGE-091 / ADR-030 / ARCH-EVO-033, contexts/sprint/contracts/traceability,
  editable diagram and ignored learning synchronized; disclosure updated.
  Verified backend commit d8d090e and frontend commit 583729f; documentation and
  disclosure committed separately. No push. Generated evidence is ignored under
  target/change091-verification/.

## Approved one-time Credit security interference - 2026-10-09

- Vincent explicitly approved the narrowly scoped Credit push-security fix and
  regression tests on sprint-2-3-credit, plus a Markdown interference record.
  CHANGE-090 supersedes only the pending-permission stopping point below.
- Scope: isolate Pub/Sub authentication conversion from Firebase role lookup;
  preserve Google signature/expiry/issuer/audience/verified-service-account
  validation and ordinary user authorization. No financial logic, contracts,
  schema, other peers, cloud/IAM or application database changes authorized.
- Test first through real HTTP bearer/security filters using locally signed
  service JWTs; prove the existing failure, then positive/negative identity and
  user-role regressions. Run Credit verification in isolated Java 21 containers.
- D1 and Overall PDF fingerprints match; historical generic drift-manifest
  format failure remains separate. Frontend inspected, unchanged: this is an
  explicitly backend-security-only exception, not a new UI/architecture design.
- Complete locally: pre-fix 14 pass/1 expected converter error; post-fix clean
  source-only Java 21 `mvnw -o -B -ntp clean verify` passes all 80 Credit tests,
  no failures/errors/skips. Fifteen new real-bearer regressions pass. Fresh JaCoCo
  95.48% lines (844/884), 85.56% branches (231/270); unchanged 80% gates.
- Source/test commit 198cd7c. Interference MD lists the exact two Credit paths,
  preserved security, tests, rollback and narrow local-live rebuild command.
  Context/feedback/traceability/disclosure updated; learning remains ignored.
- FEEDBACK-008 READY_FOR_VERIFICATION, not live VERIFIED. Running Credit was not
  rebuilt/restarted, no cloud writes or application DB changes. Owner review and
  actual Google push/ledger/duplicate/backlog/DLQ checks remain; Sprint [~].
  User rebuilds only Credit, keeps tunnel, verifies original refunds/settlement.
  Do not republish with new event IDs or silently redrive money from the DLQ.

## Local push HTTP 500 diagnosis - 2026-10-09

- Read-only runtime diagnosis requested by Vincent on sprint-2-3-credit; Git
  clean at start. No peer/application changes, cloud writes or database resets.
- Diagnosis complete, fix blocked on explicit Credit-edit authorization:
  FEEDBACK-008 records a push chain inheriting the global Firebase-role converter.
  Correlated 06:48:12Z/26Z/43Z and 06:49:01Z errors: service OIDC token sent to
  User role-context, rejected 401, Credit throws AuthenticationServiceException
  and returns 500 before refund processing. All FoC services are now running;
  the earlier Mongo host-port startup conflict is no longer present.
- Read Credit security/controller/validator source and attachment; verified Spring
  Security 7.1.1 converter bean auto-selection against official source. Negative
  Setup/Check probes explain initial 401/404/405, not later authenticated success.
  No tests executed, financial success or specific ledger changes claimed.
- Requested narrow separate Credit security/test permission; Vincent asks why,
  which is NOT approval. Next: explain user-token versus service-token identity,
  obtain approval or hand off FEEDBACK-008 to Annablee. Then test isolated chain,
  live refund/transfer and duplicate/retry/DLQ recovery without resetting data.
- Order architecture/contracts/source/config unchanged; live gates remain [~].
- Verified diagnosis/handoff commit: 23b9108; whitespace/scope checks pass.
  Learning remains ignored; no push. This is not a Credit implementation fix.

## Verified script fix: Windows PowerShell native progress handling — 2026-10-09

- Vincent / sprint-2-3-credit, clean worktree at start. User's Setup now resolves
  gcloud but crashes in Invoke-Compose on Docker's normal stderr network progress.
- Narrow implementation-detail correction within CHANGE-088 / ADR-029: preserve
  private stdout/config capture, judge native success by exit code and restore
  caller preference. No peer/FE/app behavior, cloud/IAM/schema/topology change.
- Test first with a real native Windows fixture emitting stderr at exit 0,
  nonzero exits, stdout JSON isolation and error-preference restoration. Run
  existing local configuration/proxy regressions. No application DB resets or
  cloud Setup rerun as part of this diagnostic slice. Live gates remain [~].
- CHANGE-089: observed NativeCommandError red before fix. Green: 11 native
  output/exit/privacy/restoration assertions, 60 config/safety and 16 actual
  nginx/fixture assertions. Invoke-Compose now scopes Continue to native execution,
  restores preference in finally and still rejects nonzero exits. No rebuild.
- Verified fix/regression commit: 003fd25. Workflow/docs/disclosure separate;
  learning ignored, no push. Generic gate retains five historical findings.
- D1/Overall hashes match; generic checker format/history failures remain separate.
  No app/peer/FE/shared Compose/schema edits, no cloud Setup or application DB
  changes performed. Next: user reruns same Setup; then build/up/Check and local
  two-account ledger/UI verification. No sequence completion claimed.

## Local CLI installation follow-up — 2026-10-09

- Read-only setup diagnosis, Vincent / sprint-2-3-credit; worktree clean at start.
  User's existing CMD cannot resolve gcloud. Checked standard installation paths;
  found LocalAppData/Google/Cloud SDK/google-cloud-sdk/bin/gcloud.cmd.
- Full-path `gcloud.cmd --version` succeeds (exit 0): SDK 588.0.0. The fresh
  diagnostic process and user-level PATH contain that bin; the user's displayed
  terminal still cannot resolve it. Existing parent/terminal environment is the
  likely stale part; installation is verified, login/ADC/IAM are NOT verified.
- Advise a temporary CMD `set PATH` prefix using the verified bin, then `where
  gcloud`, version, personal CLI/ADC login and Setup from FoC-P28. Alternatively
  fully restart VS Code or use a fresh SDK shell. No persistent PATH change,
  credentials access, app/source edit, cloud call or connector startup performed.
- Prior CHANGE-088 missing-CLI observations remain historical, not current
  installed-file state. Live financial/browser gates and paused retries remain.
  Next action: user refreshes terminal environment and retries setup. Sprint [~].

## Approved local-live implementation — 2026-10-09 (CHANGE-088)

- User approves the previously proposed isolated real-PubSub authenticated HTTPS
  delivery to local Credit and requests build/up/down commands. Vincent on
  sprint-2-3-credit; worktree clean at start. Local-only override/helper/scripts
  in scope; sibling source, staging subscriptions, app databases untouched.
- ADR-029 records the connector detail: Docker cloudflared Quick Tunnel behind
  exact-path POST-only nginx; existing Google OIDC validation retained. Isolated
  names and managed-resource labels; no Credit subscription to User penalty topic.
  Existing Order prod security enabled locally with Firebase emulator/HTTP roles;
  NOT a switch to Cloud SQL or production Firebase.
- Tests first: namespace/URL/resource-ownership safety, composed real modes and
  scope preservation, nginx positive/negative/header/body routing. Cloud setup
  runs only with the user's authenticated gcloud, personal ADC and needed rights.
  Scripts never print tokens or embed keys. No mocked push success.
- Implemented: local-live override, non-root/read-only nginx, pinned cloudflared,
  scoped Setup/Check/Pause, ADR/runbook/traceability. Red first: missing setup script.
  60 configuration/safety assertions and 16 actual nginx/fixture checks pass.
  Read-only nginx temp-path startup issue caught and fixed; fixture cleanup scoped.
  Baseline preservation check initially used historical user-mongodb name; actual
  branch service is mongodb, corrected before rerun. This was a test fixture error.
- gcloud unavailable and standard ADC absent; no GCP writes, actual push auth,
  application stack start or ledger/browser verification claimed. Source hashes
  match; generic drift checker still rejects existing manifest columns (exit 2).
  Generic completion checker fails with five historical/fixed-format findings;
  no gate passed claim. Actual Setup safely stops before side effects without
  gcloud (exit 1); local config/routing checks remain separate evidence.
  No Java/frontend source/migration changes. Retries/delegation paused, User
  consumers and prior Credit semantic gaps remain separate. Sprint [~].
- Next action: user completes personal gcloud/ADC/IAM prerequisites, runs runbook
  Setup/build/up/Check, verifies each financial workflow with two local accounts
  and matching outbox/event/ledger/balance evidence. Pause before down (no -v).
- Metadata note: local developer-profile.md is actually tracked on this branch,
  despite workflow describing it as ignored. Its identity/branch still match;
  current explicit task approval is recorded here/ADR-029. No tracking change or
  personal-profile update committed as part of the connector concern.
- Atomic verified implementation: 9e77fa0 Order connector/scripts/tests;
  e78f887 shared Compose/env. Docs/workflow and AI disclosure committed separately.
  No push; ignored learning retained locally and never staged.

## Local live Docker/PubSub readiness review — 2026-10-09

- Follow-up clarification: proposed tunnel testing keeps Order, Credit and their
  databases local, but exercises the real cloud broker and Google push auth.
  Success requires local ledger/balance effects, not just a published message.
  Cloud Run deployment/scaling and missing User penalty processing remain separate
  gates. User asked whether it works; this is NOT setup/cloud-write approval.
- Vincent requests one local Docker run against real Credit for expiry, abort,
  cancellation and completion on sprint-2-3-credit. Clean worktree at start.
  Rehydrated Order workflow, ADR-021/026, current scope and integrated evidence.
- Existing HTTP override enables real User/Supplier/Credit Order adapters.
  Its comments restricting live tests to Sequences 1-3 are historical/stale:
  refund/completion handlers now exist, but local broker delivery is not wired.
  No Dockerfile/source changes are necessary merely to activate those handlers.
- Reproduced combined Compose config failure: missing
  GOOGLE_APPLICATION_CREDENTIALS_HOST. The standard local gcloud ADC path was
  absent; gcloud and tunnel commands were unavailable on the current PATH.
  No secret contents were read or logged; no substitute credentials created.
- Credit receives authenticated push at /api/credits/internal/order-events,
  checks Google signature/issuer, audience and configured verified SA email,
  and matches subscription paths. No local pull subscriber/relay exists.
  Compose's internal hostname and demo push identity do not create cloud ingress.
- configure-credit-pubsub.sh supports staging/production Cloud Run delivery,
  NOT local Docker delivery. Source inspection does not establish actual GCP
  subscription state. Running that script for local testing could target the
  cloud Credit database, not the local Credit database.
- Proposed (NOT approved/implemented): isolated developer test topics/subscriptions,
  temporary HTTPS ingress restricted to the Credit push endpoint, real push SA
  and matching audience/subscription environment overrides. Preserve Google OIDC
  validation, existing shared/staging subscriptions and peer-owned application
  code. Alternative local authenticated pull relay requires a separate design.
  Obtain approval for connector/configuration and cloud owner permission first.
- Existing one-minute OPEN expiry/48-hour DELIVERED completion scheduler and
  immediate after-commit dispatch with 15-minute outbox recovery remain unchanged.
  Abort before expiry retains hold; abort after expiry resets courier then queues
  refund; cancellation queues refund; completion queues transfer. Previous focused
  27 Credit tests are component evidence, not a live local broker test this turn.
- No stack/image build/start, database mutation, cloud provisioning, IAM or peer
  configuration edit performed. Accepted-cancellation remains User-only intended
  routing; existing incompatible Credit subscriber is left untouched per request.
  Trusted background repost delegation and all retry implementation remain paused.
- Next: user approve local authenticated ingress/isolation plan; then configure,
  validate Compose and run two-user UI tests with Order outbox, Credit ledger and
  balance assertions. Pending live gates keep Sprint [~].

## Focused Credit outcome verification — 2026-10-09

- Vincent asks whether refund CANCELLED/EXPIRED and completion transfer work
  through the two implemented streams; explicitly leave accepted-cancellation
  handler/subscription alone. No peer/application/infrastructure edits authorized.
- Current source: refund validates requester/amount and null stored courier,
  releases reserved funds and marks REFUNDED; completion verifies recorded
  courier, debits requester and credits courier, marks PAID; transaction/event
  deduplication and post-processing 204 push acknowledgment exist.
- Corrected 003 explanation: normal abort waits for exact Credit 200 while Order
  locked and ACCEPTED; commits OPEN/EXPIRED plus command receipt; only OPEN can
  be accepted. Same committed command replay avoids another Credit call, stale
  Order versions fail validation. No background reset/repost retry worker exists.
  Credit-only null replay authorization/delayed direct calls remain distinct.
- Ran existing CreditOrderEventConsumerTest, CreditOrderEventControllerTest,
  CreditServiceTest, JpaCreditRepositoryIntegrationTest in Java 21 Docker on a
  read-only source copy; Testcontainers creates isolated credit_test PostgreSQL.
  Build output is container-local; dependency cache only reused. No app DB touched.
  PASS: 27 tests, 0 failures/errors/skips (consumer 5, controller 3, service 7,
  persistence 12). PostgreSQL Flyway V1/V2 applied; refund and exactly-once-effect
  transfer assertions passed, including requester 50 -> 40, courier 50 -> 60,
  reserved 10 -> 0. Duplicate effects guarded. This is not live Pub/Sub, push
  auth, full Order-to-Credit, the full Credit suite or coverage gate verification.
  Source/config unchanged; accepted-cancellation intentionally left alone.
- Documentation checks pass: TOML and 27/local-only assertions, all 8 feedback
  JSON examples, whitespace and exact path scope. Learning remains ignored.
  Confirmed the two isolated PostgreSQL/Ryuk test containers were removed;
  no application-container/database removal or restart. Sprint remains [~].
- Verification documentation committed as 2a912e0; disclosure recorded separately.
  No push. Local learning remains excluded from commits.

## Credit feedback clarification — 2026-10-09

- Advisory follow-up to CHANGE-087 on sprint-2-3-credit; worktree initially clean.
  User reiterates the existing routing: accepted-cancellation is User penalty
  signaling only; Credit refunds CANCELLED/EXPIRED via open-order-refund.
- Explained 003 as authorized lost-response replay/stale-attempt protection,
  not another reset endpoint; null-state success currently does not move funds.
  Explained 006 as confirming an active hold and recovering partial success
  across Order/Credit databases, not a missing normal reservation route.
- Re-read JpaCreditRepository, CreditService, OrderTransitionService, provisioning
  and feedback. Findings unchanged; no application, peer or infrastructure edits.
  Existing FEEDBACK-007 already requests the correct subscription alignment.
  Penalty amount remains User-owned; aborting courier is event actorId, not the
  cleared snapshot courierId. Completion/assignment still need courier identity
  in Credit; the User-only rule concerns abort penalty signaling.
- Updated local refund/history learning with beginner examples (not staged).
  No runtime tests for this explanation. Next actions and Sprint [~] unchanged.

## Integrated Credit source review — 2026-10-09 (CHANGE-087)

- Vincent explicitly requests inspection/feedback rewrite on sprint-2-3-credit;
  HEAD 09e04a0, clean worktree initially. Local profile/allocation updated for
  this documentation-only review; no peer/app/frontend/schema/cloud edits.
- Assignment and core hold routes plus refund/completion push handlers EXIST.
  Compatible route/handler source is READY_FOR_VERIFICATION, not live VERIFIED.
  Removed obsolete missing-build requests; evidence kept in feedback section 6.
- Remaining 003: hold returns 200 for any courier when assignment is null before
  caller check; authorized replay/stale same-courier attempt protection unresolved.
- Remaining 006: reservation replay returns matching terminal REFUNDED/PAID;
  Order ignores success body; cross-database compensation/reconciliation pending.
- New 007: Credit expects ABORTED/refund on accepted-cancellation; current Order
  sends OPEN/EXPIRED User penalty facts. Script subscribes Credit to this stream.
  Need peer/platform-approved cutover; never refund OPEN or mutate Order to ABORTED.
- Refund/completion provisioning script exists separately from bootstrap; no
  live subscription verification. Production explicitly disabled; local Compose
  hostname/identity do not create a public cloud push path. gcloud unavailable.
- 005 trusted delegated auth remains proposal-only. Push OIDC is not outbound
  requester delegation. ALL background retry implementation stays paused.
- Source/tests/config inspection only; no Maven/browser/live financial tests run.
  Existing drift checker rejects manifest column format; actual selected D1 and
  Overall SHA-256 match. Documentation JSON/link/scope/whitespace checks below.
- Next: Annablee/platform resolve 003/006/007; User builds 002 subscribers;
  owners agree 005; run authenticated Order-to-Credit/push/ledger tests before
  VERIFIED or Sprint completion. Keep Sprint [~]. Feedback rewrite completed;
  validation evidence: git diff --check passed; context TOML parsed with the
  reviewed branch and paused-retry/live-unverified assertions; all 8 feedback
  JSON examples parsed; change-log links resolved; changed paths are Order docs
  only. Drift script exit 2 is an existing manifest-format incompatibility, not
  a passing drift check. Peer test files inspected, not executed.
- Documentation concern committed as 7e141e1; AI disclosure recorded separately.
  No push, learning-file change, peer/source/test/config change or live write.

## Explicit repost expiry and persistent failures — 2026-10-09 (CHANGE-086)

- Vincent, sprint-2-3; clean worktree at start; approved Order/backend frontend slice.
- User authorizes explicit automatic new expiry and latest failure persistence.
  Reuse quarter-hour picker; expiry > due >= original expiry; skip elapsed new expiry.
- Follow-up decision: disable legacy automatic plans lacking explicit expiry;
  do not invent/backfill deadlines. Add V4, preserve all business/history/outbox IDs.
- Save safe latest eligible attempt failure after rollback in independent locked
  transaction; overwrite newer failure, clear on successful linkage; no peer writes.
- Implemented/local verified: FINAL 196 backend tests, 0 failures/errors/skips;
  16 isolated PostgreSQL tests. Fresh coverage 95.23% lines / 83.49% branches.
  Frontend 49 tests; lint 0 errors/12 existing warnings; typecheck/build pass.
- V4 clean/upgrade/legacy-disable constraints and rollback/overwrite/auto exact
  expiry/success-clear verified. Refetch original version after failed manual POST.
  No application DB reset. Source/workflow/disclosure commits separate; learning ignored.
- Generic workflow gate retains 5 pre-existing format/history failures; drift
  checker manifest unsupported; actual selected source hashes match.
- Background retries/credentials remain paused. Stopping point: peer 002-006
  agreement and user resumption; real ledger/consumers/browser/cloud/hosted CI
  remain unverified. This does not upgrade overall Sprint [~] status.
- Verified commits: 8818f1a backend/V4/tests; b3cbf7b shared Order UI/tests.
  Workflow/disclosure separate. No push, no learning staged, no peer source edits.

## Current polling/failure-message implementation - 2026-10-09 (CHANGE-085)

- Vincent, `sprint-2-3`, clean worktree at task start; Order backend and approved
  shared frontend slice only. User/Supplier/Credit implementations read-only.
- Latest user decision supersedes the earlier retry implementation request:
  **pause ALL background retry implementation until peers agree**. No retry
  worker/table, same-candidate job persistence or trusted credentials this turn.
- Implement auth-ready visible-page polling (15-second default, focus refresh,
  cleanup/no overlap), retain immediate mutation invalidation, preserve existing
  cards during refresh and reject stale/aborted responses.
- Confirmed Credit semantic INSUFFICIENT_CREDITS -> short manual repost card
  message; other permanent failures get short appropriate messages. Original
  remains EXPIRED. No delayed-refund inference or automatic background retry.
- Rewrite peer feedback by service as explicitly requested, preserving stable
  FEEDBACK identifiers and historical contracts in Git rather than a stale body.
- Status: polling/manual-message slice implemented and locally verified;
  full Sprint remains [~], not production-complete.
- TDD observed missing hook / two RTL assertions / two adapter semantic failures
  before implementation. Final current-source backend: 170 tests, no failures/
  errors/skips; fresh coverage 93.47% lines / 83.19% branches. Isolated PostgreSQL
  tests ran. Regular verify passed; Windows clean deletion failed on generated
  metadata, so final run selected every current test class with append=false.
- Frontend: 45 tests, lint 0 errors/12 pre-existing warnings, typecheck/build pass.
  Authenticated/responsive live browser, peers/cloud/hosted CI not verified.
- Generic drift/gate scripts failed existing record formats/history (five gate
  blockers); actual D1/selected overall hashes match. No false gate pass.
- Source commits 4609abc, b60f74d, 826dea9. Learning ignored/not staged.
- Workflow/peer handoff commit 9d2cee9; AI disclosure recorded separately.
- Manual message remains client-local; auto next-expiry/persistent outcomes/
  same-candidate tasks remain incomplete. No background worker added in either
  HTTP or mock mode. Next: peers agree 002-006 and user resumes deferred scope;
  inspect/verify providers before implementation. No peer files changed.

## Current approval-documentation task - 2026-10-09 (CHANGE-084)

- Developer/branch: Vincent, `sprint-2-3`; clean worktree at task start.
- Record approved polling, bounded same-candidate-ID retries and short terminal
  failure messages. Document trusted peer authorization for discussion only;
  the user explicitly says NOT to implement that credential mechanism yet.
- This task is documentation-only. Application source, tests, migrations,
  frontend, peer services and cloud configuration must remain unchanged.
- Peer acceptance/security mechanism remains open in FEEDBACK-005; reservation
  confirmation/reconciliation remains open in FEEDBACK-006. User approval does
  not approve either contract on the provider owner's behalf.
- Status: approval records synchronized in ADR-027 / CHANGE-084 and the target
  diagram/feedback/context/sprint/traceability; no feature completion upgrade.
- Read-only provider inspection reconfirmed User role-context Firebase UID,
  Credit requester-bound PUT/GET reservation and Supplier pair response fields.
  D1/selected overall hashes match; historical source path lookup required
  `../../` from repo root, recorded without rewriting the source manifest.
- Next: Vincent discusses FEEDBACK-005 with User/Supplier/Credit owners; Annablee
  confirms FEEDBACK-006 semantics. Concrete retry persistence/API/security and
  polling implementation design remain follow-up; no source/tests/cloud writes.
- Verification: whitespace/TOML/approval flags/new record links/scope checks pass;
  learning ignored; Mermaid text-reviewed, not rendered. Runtime tests not run
  for this documentation-only task. Approval documentation commit: `c993591`.
  Authorized AI disclosure and this handoff follow as a separate atomic concern.

## Previous diagram reconciliation - 2026-10-08 (CHANGE-083)

- Same developer/branch/boundaries as below; PDFs and learning unchanged.
- Approved cadence implemented: one minute lifecycle job for OPEN expiry and
  >=48-hour DELIVERED completion; 15-minute outbox recovery for all event types;
  immediate dispatch retained. Single ORDER_LIFECYCLE_CRON replaces old settings.
  Atomic cadence commit: 392104b.
- Supplier valid:false/missing-confirmation repair follows the inspected existing
  provider contract; two new regressions failed before the adapter change.
- Approved explicit repost expiry/strict timing remains unimplemented; existing
  automatic expiry is still duration-derived. Frontend failure/status unchanged.
- Pending decisions: durable fixed-ID retries bounded by expiry/permanent errors;
  trusted background service authorization versus freshly authenticated user calls.
- Missing Credit assignment/reset and Credit/User consumers rechecked; feedback
  gives formats/responses and reservation reconciliation. HTTP stays fail-closed.
- Final verify after Supplier repair: 163 backend tests, no failures/errors/skips;
  isolated PostgreSQL tests pass; fresh coverage 93.37% lines / 82.82% branches.
  Unchanged frontend baseline: 34 Vitest/RTL tests pass. Base/HTTP Compose static
  checks and normalized timer settings pass; no full stack/browser/cloud test.
  Supplier implementation commit: d21c450. Detailed evidence in CHANGE-083.
- Next: decisions -> detailed retry/schema/API/security approval -> explicit expiry,
  durable auto/manual retries and Order UI/tests. Status [~]; peers/browser/cloud pending.

## Current Sprint 2-3 implementation - 2026-10-08

- Developer: Vincent, Developer 2; explicitly confirmed by the user.
- Branch: `sprint-2-3`.
- Scope: Order-owned parts only, using Project D1 and `Order Service Overall Doc.pdf`.
- Change: CHANGE-081/082, ADR-025; status `[~]`, Order-owned slice implemented, not production-complete.
- Approved: separate immutable courier-attempt history with one current Order;
  internal row UUID separate from business orderId; abort only from ACCEPTED;
  every abort signals User through the accepted-cancellation topic; expiry and
  requester cancellation signal Credit through the refund topic; preserve the
  aborting courier's history while requester sees OPEN/EXPIRED; new business ID
  for repost; minute-based completion after at least 48 hours since delivery.
- Local exception: mock the existing proposed courier-assignment route when the
  Credit implementation is missing. HTTP mode must not silently fall back to mocks.
- Follow-up approved/implemented: EVERY abort synchronously calls bodyless
  hold-for-reopen to clear Credit courierId, then rechecks expiry; current Order
  keeps its business ID as OPEN/EXPIRED, separate immutable ABORTED attempts.
  Peer stale-retry protection/reconciliation is required, not provider-verified.
- Repost retention/visibility approved: keep old EXPIRED row and new OPEN row
  with different business IDs; hide the successfully reposted old expired row
  from My Requests using repost linkage, not deletion; query/count and immediate
  frontend update are implemented. Manual replay verifies requester/original.
- Commits: `e7b6a09` requester query/count; `9e7aa3c` Order frontend; `e6063b0`
  abort/history/schema and latest-checkpoint/replay regressions.
- Verification: Java 21 Maven verify 162 tests, 0 failures/errors/skips; Docker
  PostgreSQL clean V3, V2 upgrade, repeat attempts, ownership/pagination and
  transactional rollback passed. Fresh JaCoCo 94.01% lines / 82.37% branches;
  no threshold weakening. Frontend 34 tests, typecheck/build pass; lint 0 errors,
  12 pre-existing peer login/profile warnings. Red tests observed before query,
  abort, requester replay and frontend visibility implementations. Initial V3
  constraint-drop and old migration-version assertions were repaired and rerun.
  Windows `clean` failed on a locked generated directory; regular verify and
  fresh non-appending coverage passed. Existing application DBs were not reset.
- Next action: Annablee implements assignment/reset/refund/settlement contracts;
  User owner implements penalty/completion subscribers. Resolve trusted automated
  repost credentials and stale reset reconciliation, then real HTTP/PubSub,
  authenticated responsive UI and Cloud Run scheduling checks. Full NTH3
  report/hold/resolution remains outside this approved lifecycle slice.
- Boundaries: do not edit sibling backend services; do not commit learning files;
  shared frontend/configuration changes require the approved Order-only slice.
- Historical Sprint 1 evidence below is not verification of Sprint 2-3.

## Historical Sprint 1 active work

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

## On-demand CI rehearsal result — 2026-09-30

The explicitly requested rehearsal ran on branch
`sprint-1/seq-1-to-seq-11`. The Order Service OpenAPI structural guard passed.
Maven verification was unavailable because the checked-in Windows Maven
wrapper failed before Maven startup. Frontend npm checks were unavailable
because Node/npm are not installed in this runner. Compose configuration
validation passed, with Docker configuration-access warnings. Docker image
build was unavailable because the Docker daemon/buildx pipe denied access even
with an isolated temporary Docker config. No Git history or learning file was
changed.

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

## Latest OpenAPI CI 404 fix — 2026-09-30

GitHub CI reached the Order Service tests after `mvnw` became executable, but
`OpenApiDocumentationTest` received 404 for `/api/orders/v3/api-docs`. CHANGE-
049 imports the core `SpringDocConfiguration` alongside the WebMVC and Swagger
configurations so the MVC slice can register the Springdoc resource. The
pasted CI log is the verification evidence for the diagnosis; local Maven is
unavailable, so CI must verify the fix.

## Latest OpenAPI CI context completion — 2026-09-30

The next GitHub run showed that Maven, Java compilation, and 42 other tests
were healthy. The sole failure was `OpenApiDocumentationTest`: its MVC slice
loaded UI-only `SwaggerConfig`, which required an absent
`SwaggerUiConfigProperties` bean. CHANGE-049 now imports Springdoc's API
property, core, and WebMVC configurations only; the unused Swagger UI test
property was removed.

Verification ran inside an Eclipse Temurin Java 21 Docker image. `mvn verify`
passed with 43 tests, zero failures/errors/skips, a successful generated
OpenAPI JSON request, and all JaCoCo coverage checks met. The test-only source
fix is commit `5f9335f`. No runtime endpoint, API contract, frontend,
peer-service source, database, or infrastructure configuration changed.
