# Reporting Service handoff — Order integration

Updated 2026-10-30 for Yao Xiang on `feat/reporting-service`. AI assistance: OpenAI Codex; human review pending. This replaces the former blocker-only handoff. Verification evidence is recorded in `docs/order-resolution-saga.md`.

## Reporting eligibility and frontend

Only the requester's own **ACCEPTED, IN_PROGRESS, PICKED_UP or DELIVERED** orders may receive a new ORDER/COURIER report. OPEN and terminal orders are excluded. This is the latest user-approved restriction; OPEN still counts as active for user/supplier deletion checks.

For ORDER/COURIER, the requester form reads `GET /api/orders/mine/active?page=1&size=100` through the authenticated gateway and follows pagination. Order filters ownership and eligible statuses in SQL before pagination. Browser filtering is defensive only; it cannot authorize a report.

Reporting generates a report ID, then forwards the requester's Firebase token to:

```http
PUT /api/orders/mine/active/{orderId}/report-holds/{reportId}
Content-Type: application/json

{"requireAssignedCourier":true}
```

Order locks the eligible requester-owned row and places that report's hold atomically. Its six-field response is `id, requesterId, courierId, status, reportId, isCompletionHold`. A retry reuses the hold and returns the **current** snapshot, which may have progressed. Missing/unowned/OPEN/terminal orders return indistinguishable 404s; binding, released-hold, missing-courier or lock conflicts return 409. Report validates the IDs, assigned courier, status and hold before saving.

A different active report on the same Order returns 409: "This order already has an active report. Please resolve it before submitting another report." Retrying the same report ID remains idempotent. The Order row lock serializes public hold placement.

Holds pause ordinary completion, refunds and abort penalty outcomes; START, PICKUP and DELIVER continue. Existing explicit admin cancellation remains separate. Report resolution does not cancel/complete an Order or choose its penalty.

## Report lifecycle

- For ORDER/COURIER reports, an assigned courier has **24 hours** from creation to submit one explanation and optional evidence.
- A timely response, or deadline expiry without a response, makes the report READY_FOR_REVIEW.
- Reads reconcile deadlines; a bounded database-selected minute job also persists overdue responses so status filters do not permanently hide them. Each case has an independent Firestore transaction and error handling.
- Admins may resolve READY_FOR_REVIEW or AWAITING_COURIER_RESPONSE. The latter displays an explicit warning and confirmation.
- The admin reply is required (1–2000 characters), stored with resolvedBy/resolvedAt, and returned only to the submitter and authorized admins. The reported courier still sees status/time.
- Categories remain ORDER, COURIER, CREDITS and GENERAL; no new violation taxonomy is introduced.

## APIs

| Operation | Access / purpose |
|---|---|
| POST `/api/reports` | Authenticated multipart `report` JSON plus up to five `photos`; creates a report after any required hold |
| GET `/api/reports/mine?view=submitted\|against-me&page=1&size=20` | Caller-scoped reports |
| GET `/api/reports/{id}` | Reporter, recorded courier or admin |
| POST `/api/reports/{id}/response` | Recorded courier; one timely multipart response |
| GET `/api/reports/mine/response-count` | Authenticated caller only; `{count}` of currently answerable courier reports; database aggregate excludes responded/resolved/expired cases |
| GET `/api/reports/{reportId}/attachments/{attachmentId}` | Authenticated private evidence download |
| GET `/api/reports?page=1&size=20&orderId=...&status=...` | Admin; optional exact Order/status filters and database pagination |
| POST `/api/reports/{reportId}/resolve` | Admin; `{commandId,reply}`. Forwards to the Order coordinator; preserves 200/202. GENERAL and all CREDITS reports resolve locally through the journal, including completed-order credit context |
| PUT `/api/reports/internal/order-resolutions/{commandId}` | Admin Firebase token; callback-free atomic Report decision using the exact Order contract below |
| POST `/api/reports/internal/order-resolution-compensations` | Dedicated Pub/Sub push identity; conditional restoration/fencing plus durable result |
| PATCH `/api/reports/{id}/status` | Legacy route returns 409 directing callers to command-based resolution; cannot release an Order hold |

The three resolution routes require `REPORT_RESOLUTION_ENABLED=true`. Swagger: `/api/reports/docs`, JSON: `/api/reports/v3/api-docs`. Internal does not mean unauthenticated.

Effective CHANGE-169: CREDITS and GENERAL are unlinked. Neither displays an Order picker or calls Order Service; a submitted nonblank orderId returns400. Both start READY_FOR_REVIEW with no completion hold, reported courier or response deadline. Only submitter/admin can view these reports. Separate caller-scoped completed and cancelled Order history APIs are retained for other uses. Historical linked credit reports remain readable and resolve locally.

Admin Reports now uses real Report APIs in every frontend mode. It loads an associated Order only for full case details and downloads evidence with the user's bearer token. It retains a command ID/reply across uncertain retries in per-admin/report browser session storage. A 202 is pending, never reported as success; the UI polls Order's command status. Existing Order demo backend fixtures are not used by this frontend.

## Order-owned resolution and compensation

The browser entry point forwards to Order's `POST /api/orders/admin/{orderId}/reports/{reportId}/resolve`. Order records intent before calling Report. **Report never calls the old Order hold-release endpoint.** Only Order releases its hold after confirmed Report success and a local commit.

See [docs/order-resolution-saga.md](docs/order-resolution-saga.md) for the exact request/result payloads, preimage/revision journal, compensation-before-original fencing, result outbox, identity settings, retry operations and topic names.

## Deployment and remaining operational checks

1. Deploy a contract-capable Order version containing V18 and both reporting APIs. Those sources are now present following the user's main merge.
2. The owner-created four topics were confirmed present on 2026-10-30. Their existing subscriptions are pull-only. Use the scoped `infra/gcp/configure-report-pubsub.sh <environment> prepare|wire` rollout below to configure identities/authenticated push; topic creation alone does not activate delivery.
3. Install the Report database composite indexes in `firestore.indexes.json`. Emulator success does not verify production indexes.
4. Configure both services' saga settings, then enable both feature flags. Defaults remain disabled; this change does not provision GCP or activate staging/production.
5. Verify real cloud OIDC, IAM, deadlines, retry/dead-letter routing and alerts before rollout. Local tests use explicitly gated Firebase-emulator tokens and actual Pub/Sub/Firestore emulators.

Preserve operation journals, tombstones and dedup/result records until both services agree a retention horizon. Never clear a hold to work around COMPENSATING or RECONCILIATION_REQUIRED. Existing report-creation failure after hold placement still requires operational reconciliation; resolution compensation does not implement a separate report-creation saga.

## Cloud rollout (CHANGE-167, approved 2026-10-30)

Run these Bash commands from the repository root in Cloud Shell, WSL or Git Bash with an authorized Google account. **The assistant has not run them against GCP.** Review `bootstrap.sh`: it is the existing project-wide prerequisite setup, not a Report-only script.

```bash
# Existing project setup: runtime identity, Firestore databases, private attachment buckets,
# Report database/storage permissions and deployer permissions (plus other existing project prerequisites).
bash infra/gcp/bootstrap.sh
# Scoped Report saga setup: safe before the Report Cloud Run service exists.
bash infra/gcp/configure-report-pubsub.sh staging prepare
```

Deploy the approved branch's Order, Report, gateway and frontend versions through the normal pipeline, retaining `ORDER_REPORT_RESOLUTION_ENABLED=false` and `REPORT_RESOLUTION_ENABLED=false`. Install the composite indexes from `reporting-service/firestore.indexes.json` in `reporting-staging` and wait for them to become ready. Then:

```bash
bash infra/gcp/configure-report-pubsub.sh staging wire
```

The scoped script reuses the existing `*-dev-v1-sub` subscriptions, verifies their topic bindings, configures Google OIDC push/retries and a retained dead-letter recovery channel. It does not delete subscriptions, deploy, enable flags or alter Order's existing Publisher grant. `wire` refuses to mutate infrastructure if either target Cloud Run service is missing. `prepare` is the default when the phase is omitted.

| Identity | Purpose / permissions |
|---|---|
| `foc-reporting-service@<project>.iam.gserviceaccount.com` | Report runtime; bootstrap grants own-database/storage access; scoped script grants result-topic Publisher and deployer actAs |
| `foc-report-push-staging@<project>.iam.gserviceaccount.com` | Authenticated request push into Report; Cloud Run Invoker on Report |
| `foc-report-result-staging@<project>.iam.gserviceaccount.com` | Authenticated result push into Order; Cloud Run Invoker on Order |
| Google Pub/Sub service agent | Token Creator on those push identities; Subscriber on source subscriptions and Publisher on the dead-letter topic for forwarding |

Report's runtime is **not** granted Subscriber: this is HTTP push, not a runtime pulling messages. Order already has Publisher; its grants are preserved. Production uses separate `...-production` push identities, `*-prod-v1-sub` subscriptions and prod topics.

The script also creates `report-resolution-dead-letter-dev-v1` plus its `-recovery` pull subscription (production: `report-resolution-dead-letter-prod-v1`). Both source subscriptions use a 60-second ACK deadline, 7-day retention, 10-600 second retry backoff and 10 maximum delivery attempts before best-effort dead-letter forwarding. Subscribe/alert/replay operationally; dead-lettering alone does not finish a saga or release its hold.

Cloud settings are now forwarded explicitly by both services' `deploy/env.yaml`, using `infra/environments/<environment>.env`. Report calls Order with a 20-second timeout; Order calls Report with a 5-second timeout. Push targets and audiences use the same direct backend URLs as deployment, never the public gateway. No emulator settings are supplied to cloud runtimes.

After reviewing wiring/indexes, set **both** saga flags to `true` in the staging environment config, redeploy both backends, and verify successful resolution plus duplicate/compensation handling using disposable staging cases. Do not inject rollback triggers into a shared staging database. Cloud Google OIDC delivery is still unverified until that check passes. Repeat the prepare/deploy/index/wire/activation sequence for production only after staging passes. Provisioning can be rerun; it never silently rebinds a subscription to a different topic.


## Bucket verification correction (2026-10-31)

AI assistance: OpenAI Codex; human review pending. The infrastructure check previously read only the Storage API's nested `iamConfiguration.publicAccessPrevention` field. Current `gcloud storage buckets describe --format=json` returns top-level `public_access_prevention`, causing false failures for both Reporting attachment buckets even when protection was enforced. The checker now accepts either representation while still requiring `enforced` and the Reporting runtime's bucket-scoped object access.

Four regression tests execute the actual bucket-check section with CLI-shaped/API-shaped metadata and negative protection/access cases; two reproduced the original defect before the fix. All four pass after the fix. Read-only verification against the actual staging and production buckets confirms both privacy and runtime access checks pass. Bash syntax and diff whitespace checks pass. No cloud resource, IAM, billing or feature-flag changes were made. Rerun the full infrastructure check and PR CI; this targeted verification does not establish complete deployment or Report saga delivery.

## Credit report context and courier reminders - CHANGE-168 (2026-10-30)

Owner: Yao Xiang on feat/reporting-service. The user requested optional Order context for credit issues, dates and a clearer order picker, an actionable duplicate-report message, and a red unanswered-report reminder. They explicitly approved completed-order context for either the requester or assigned courier, with no completion hold, no courier response request and direct admin review. Compact selectable rows reuse the existing UI theme; all original report categories and active ORDER/COURIER restrictions remain.

Contracts: GET /api/orders/mine/completed?page=1&size=20 lists only COMPLETED orders where the verified caller is requester/current courier. GET /api/orders/mine/completed/{orderId} performs the same ownership/status check for Report backend validation; missing, unrelated and non-completed records return 404. No userId is accepted. Any recognized Order role may use these caller-scoped reads. SQL applies status/ownership/count/pagination; no all-order scan. OrderResponse.startedAt is nullable and derived from the stored deadline minus its declared delivery duration; no SQL migration or speculative legacy backfill.

CREDITS may remain unlinked or reference that completed order. GENERAL remains unlinked. Credit linkage is context only: courierId/deadline remain null, status READY_FOR_REVIEW, no hold or funding/penalty action. Credit resolution stays in Report's durable local journal even with an orderId; it never calls Order's hold-release coordinator or polls an Order command. Only submitter/admin can access these credit reports. ORDER/COURIER still require own assigned ACCEPTED/IN_PROGRESS/PICKED_UP/DELIVERED order and their existing hold/response flow.

Duplicate protection: public placeActive locks the Order and rejects another active report while preserving same-report retry. Report saveNewLinked also serializes creation with Firestore active_report_orders slots, checking both the referenced report and a bounded legacy active-report query before writing report+slot atomically. Slot records can reference resolved history; a new report is permitted after resolution. Existing generic historical/admin multi-hold data is preserved. The response says: This order already has an active report. Please resolve it before submitting another report. Other conflicts are not relabeled as duplicates. This does not introduce a separate cross-service report-creation saga; the known hold-commit/report-save uncertainty remains a separate recovery requirement.

Reminder: GET /api/reports/mine/response-count returns {count}, scoped solely by the JWT subject, using Firestore aggregate predicates on courierId/status/responseDeadlineAt. Expired, responded and resolved reports do not count. One visible/authenticated five-second poller in the app shell supplies both desktop/mobile Report an Issue navigation badges; admin mode does not poll, account changes cannot inherit another user's badge and transient failures retain the same user's last confirmed count. No new event topic, secret or env var is required.

UI: optional completed-order rows for Credits and required active-order rows for Order/Courier; truncated description/ID, status and Created date, bounded scroll and existing photo validation retained. Started and Active report labels are omitted. Empty optional link remains supported; duplicate errors are shown on submission and preserve the entered form. No User/Credit/Supplier backend or cloud resources were modified.

Verification (final application source): root reactor 669 Order +12 common tests, zero failures/errors/skips, Maven SUCCESS and coverage gates (Order BRANCH81.03%, LINE94.80%). Report61 tests, zero failures/errors/skips, coverage BRANCH81.63%, LINE95.13%. Initial client test reproduced rejection of a response missing hold fields; fixed with a separate completed-context DTO, not by weakening hold validation. Real local Order/Report HTTP, PostgreSQL/Firestore and two-way Pub/Sub integration passes all seven scenario groups, including the new ownership/credit-context/duplicate/count cases plus existing compensation/replay/lost-response/fencing cases. Test containers/processes removed. Root Surefire emitted a post-success fork shutdown timeout warning; Maven still completed with exit0, zero failed tests. No production timeout or coverage gate changed. Logs: order-service/target/change168-order-verify.log, change168-report-final.log, change168-live.log; reporting-service/target/live-order-resolution-result.json.

Final frontend verification: all274 tests pass across52 files after the credit-case UI refinements; TypeScript, lint and production build pass. Credit case details show Submitter account, no courier-response section, and use actual Order participant IDs for Order names. The Order OpenAPI MVC slice now includes CompletedReportOrderController, with explicit path assertions; all4 focused documentation tests pass and the regenerated artifact includes both completed-context routes and startedAt. Report response-count OpenAPI and Firestore index/context validation pass. Live staging/production and Google IAM delivery remain unverified; this change does not deploy/rebuild the user's running stack. Install the two additional Firestore indexes in reporting-service/firestore.indexes.json before cloud rollout (courierId/status/responseDeadlineAt and orderId/status). No SQL migration is needed; Firestore slots are created lazily with a legacy-record check.


## Effective reporting simplification - CHANGE-169 (2026-10-30)

Owner: Yao Xiang on feat/reporting-service. The user revoked the optional CREDITS/Order association introduced in CHANGE-168 and requested the initial submission UI, while retaining completed/cancelled Order lookup and the red courier reply reminder. This supersedes only credit association and picker layout from CHANGE-168; active reporting/hold restrictions, duplicate feedback and the response-count behavior remain.

CREDITS and GENERAL submissions are unlinked: no Order selector or lookup, orderId null, READY_FOR_REVIEW, no reported courier, response deadline, completion hold or automatic credit adjustment. Sending a nonblank orderId for either category returns400. ORDER/COURIER keep the existing active-only associated-order dropdown. The card/row/date alternatives are removed from this form. A supplied initialOrderId is ignored for unlinked categories. Form contents/photos and the helpful active-report message remain on failure.

Retain GET /api/orders/mine/completed and /api/orders/mine/completed/{orderId}. Add the matching GET /api/orders/mine/cancelled and /api/orders/mine/cancelled/{orderId}. OrderCreditHistoryController replaces the internal CompletedReportOrderController class name without changing the completed URLs. List endpoints return the existing one-based Order page, with status/ownership/count/pagination in SQL; detail returns404 for wrong status, missing or unrelated orders. Requester/current assigned courier only, from verified principal, with any recognized Order role. Read-only history: no Credit ledger access, hold, funds or penalty changes. These APIs are independent of Report submission. Expiry also has refund behavior in the existing system; the requested retained history scope here is COMPLETED/CANCELLED, not a statement that other statuses have no Credit activity.

Remove the now-unused Report getCompletedOrder HTTP adapter/interface method and CompletedOrderContext DTO. Keep reading historical linked credit reports and resolving them locally through the existing Report journal; no database records are deleted or backfilled. The Report result/compensation contracts and topics remain unchanged. No SQL/Firestore migration or new index/env/role is required. Existing completed-query metadata, including nullable startedAt, is retained as response compatibility; the form does not display it.

Preserve GET /api/reports/mine/response-count and the red badge in normal desktop/mobile Report an Issue navigation, including five-second visible/authenticated polling, caller isolation and deadline filtering. No User/Credit/Supplier backend, CI workflow, cloud setup, running container or application database mutation is part of this change. Backend Java, Report form/tests and existing documentation are the affected slice. Tests and final evidence are recorded below after verification.

Final CHANGE-169 verification: root reactor670 Order +12 common tests, zero failures/errors/skips; coverage gates pass (Order BRANCH81.03%, LINE94.81%). Report60 tests, zero failures/errors/skips; gates pass (BRANCH81.49%, LINE95.01%). Frontend274 tests across52 files, lint and production build (including TypeScript) pass; focused form/reminder/navigation14 tests pass. All four completed/cancelled list/detail routes and retained response-count API are present in generated OpenAPI; context TOML validates. Updated actual Order/Report HTTP + PostgreSQL/Firestore + two-way Pub/Sub live integration passes all seven scenario groups, including unlinked credit creation/local resolution, rejection of supplied credit orderId, completed/cancelled ownership/status filtering and the preserved response count, plus existing rollback/duplicate/lost-response/fencing scenarios. Disposable fixtures removed. Logs: order-service/target/change169-order.log, change169-report.log, change169-frontend.log, change169-lint.log, change169-build.log, change169-live.log; reporting-service/target/live-order-resolution-result.json. No production deployment, GCP change, database cleanup or running frontend rebuild was performed. Working diff whitespace and merge checks pass.
