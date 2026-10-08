# Order Service Requirements Traceability

## Current Sprint 2-3 effective slice

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
