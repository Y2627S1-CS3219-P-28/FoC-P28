# CHANGE-081: Vincent's Sprint 2-3 scope and pending lifecycle design

- Date: 2026-10-08 (Asia/Singapore)
- Developer/approver: Vincent; identity and Order-owned scope explicitly confirmed.
- Branch: `sprint-2-3`.
- Status: Partial approval recorded; implementation pending unresolved choices.
- Classification: Scope transition and approved architecture/specification amendments.

## Sources and inspection

The user selected `Order Service Overall Doc.pdf` alongside Project D1, both at
the same directory level as FOC. The overall document contains sequences 1-12,
including abort/reopen/expiry, administration/holds and automatic/manual reposting.
Relevant sequence/class diagram images were visually inspected. This source is
not the missing previously recorded `Order Service Overall Doc - Updated.pdf`;
old approved ADRs remain relevant unless explicitly superseded by current approval.

- Overall PDF SHA256: `F0E925278FCCC0FCBB48F6C7BE1C23D908E15BEF75BB7C013EEC28C3F05DA602`.
- D1 SHA256: `C7381CB03CE40E64FAA1B1E9856CC99D13BF9295BA3159481C63A4AF42BFB5AC`.

Related requirements: Order F3/F5 acceptance authorization; F4.1.6/F7 completion;
F4.1.7-F4.1.11, F10/F11 cancellation/expiry/abort/reopening; F8/F9 current/history;
NTH2 User-owned penalties; NTH4 reposting. Exact scope/test mapping remains to be
completed before implementation; no requirement is marked verified by this record.

## Confirmed decisions (approved, not applied)

1. Vincent owns this branch's Order-side work. Peers remain read-only; their
   implementation/integration will be added and independently verified later.
2. Acceptance must wait for synchronous Credit courier-assignment confirmation.
   The existing proposed `PUT /api/credits/orders/{orderId}/courier-assignment`
   body `{"courierId":"..."}` may be mocked locally because the peer route is
   absent. This is a renewed contract-stub exception, not proof of a live account.
3. One current Order plus immutable courier-attempt history was explicitly
   selected. The user additionally requested a database UUID primary key distinct
   from business orderId. Detailed migration/key/reference design remains pending.
4. Only the assigned courier may abort an ACCEPTED task before IN_PROGRESS.
   Preserve the aborted attempt; requester sees OPEN before original expiry or
   EXPIRED at/after expiry, not an active ABORTED order.
5. Every abort publishes the accepted-cancellation fact for User penalties.
   An expired outcome additionally publishes the refund fact for Credit;
   before-expiry reopening does not publish a Credit refund/abort event.
6. Requester cancellation and expiry use the existing refund topic/outbox.
   Normal/automatic completion uses the existing completion event/outbox.
7. Reposting must create a new business orderId and reserve new credits before
   OPEN. The user requested removing/replacing the expired original, but has
   subsequently asked about overwriting the row/ID; final retention/replacement
   mechanics are unresolved, not silently selected by AI.
8. The minute-based scheduler completes DELIVERED tasks only after at least
   48 hours, using the normal completion workflow. It exists already; correctness
   and production scheduling availability still require verification.

Items 3-5 supersede conflicting historical Sprint 1 policy in principle;
application, contracts, diagram/data design and tests are not synchronized yet.

## Unresolved decisions and risks

- ADR-014/FEEDBACK-003 currently reset Credit's assigned courier synchronously
  before OPEN. The user asks whether reassignment can overwrite instead.
  FEEDBACK-004 currently rejects a conflicting existing courier. Blind overwrite
  would change that peer contract and permit stale retries to replace a newer
  courier. Do not remove reset or assert overwrite support without a decision and
  a provider-compatible contract; the provider is currently missing.
- Clarify whether overwriting means abort-to-EXPIRED with the SAME business ID,
  or replacing an EXPIRED order with a new OPEN business ID during repost.
  Refund snapshots must retain the OLD ID, history must not be rewritten, and
  failed reservation/repost must leave the expired original recoverable.
- No allocation or user approval authorizes edits to Credit/User/Supplier/Admin.
- Actual consumers, trusted lifecycle authentication and Cloud Run cron behavior
  remain separate production gates. No automatic HTTP-to-mock fallback is approved.

## Changes and verification

Only workflow/approval records and the local profile are updated. The local
profile is currently tracked in Git despite historical instructions describing
it as ignored; no ignore/untrack operation is performed by this change. No
application source, tests, migrations, event schemas, API implementations,
frontend, Compose, infrastructure or learning files are changed. TOML parsing,
explicit pending-decision assertions, unique evolution ID, change-record existence
and `git diff --check` passed. No runtime tests are claimed.

## Next implementation gate

Resolve the two choices above; complete the detailed feature proposal and affected
contract/diagram/data/test chain; use a new Flyway migration (never edit V1/V2);
observe failing tests before code; verify clean and upgrade database paths,
authorization, repeated courier attempts, race boundaries, idempotency, outbox
delivery/retries, UI isolation and the 48-hour threshold. Keep Sprint 2-3 incomplete
until its applicable completion gates pass.
