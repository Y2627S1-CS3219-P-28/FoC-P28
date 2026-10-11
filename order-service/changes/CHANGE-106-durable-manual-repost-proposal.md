# CHANGE-106: Durable manual repost proposal

- Owner: Vincent, 2026-10-11.
- Branch: sprint-2-3-credit-service-concurrency-update-event-payload.
- Status: [~] design review. User approves the manual durable/pending concept;
  the specific schema/API extension below awaits approval before coding/tests.
- Requirements: NTH4 manual repost, F10 expiry and approved F10.1.3 visibility
  override; ADR-025/028/030 existing repost/history/window rules; ADR-033/035
  command consistency/auth/lease rules. No new broker/event/financial policy.
- D1/selected Overall source SHA256 still match their persistent references.

## Approved existing architecture and observed implementation

Successful repost creates one NEW linked OPEN order; retain original EXPIRED and
its old-ID refund/history, hide it from requester list only on successful link.
Manual input requires quarter-hour UI selection and expiry >= submission+30min.
Current OrderRepostService.manual performs reservation inside a local transaction,
but does not register durable intent first. RepostControls has transient busy state,
new key per click and no command recovery. Durable commands currently support only
CREATE/ACCEPT/ABORT. order_commands.order_id is both target guard and Credit target.
V5 kind CHECK permits only those three; no candidate column exists for a command
whose guarded original and reservation target differ. Never rewrite V5.

## Proposed design — approval required

1. Add REPOST to the existing coordinator and browser command union. One immutable
   key binds actor, original order/version and submitted description/credits/duration/
   expiry. Keep server-owned pickup/delivery references frozen from the original.
2. Add new versioned Order migration V8: permit REPOST and add nullable candidate
   order UUID. For REPOST, order_id remains ORIGINAL (existing partial unique pending
   target index and repository guards); candidate_order_id is the NEW reservation/
   successor ID, generated once before unsafe I/O. Preserve all existing rows/IDs.
3. Lock original while registering/checking requester, EXPIRED/unlinked/version.
   Commit intent before Credit I/O. Existing claim/lease/generation allows one owner;
   other same-key submissions receive saved status and other reposts conflict.
4. Proposed POST /api/orders/commands/{originalId}/repost takes existing
   ManualRepostRequest fields: commandId, actorId, expectedVersion, itemDescription,
   offeredCredits, deliveryTimeLimitMinutes, expiresAt. Return existing CommandView:
   202 PENDING or 200 COMPLETED with SUCCESS/REJECTED. Legacy POST
   /api/orders/{originalId}/repost delegates to coordinator; success retains normal
   Order JSON, unresolved202 saved command status; cannot bypass recovery.
5. Map internal REPOST to existing Credit RESERVE/CREATE gateway operation using
   saved candidate ID. Credit receives only requesterId/amount plus Authorization,
   Idempotency-Key and X-Command-Generation. Original ID/repost instructions stay in
   Order. No Credit implementation, DTO or topic change proposed.
6. Historical Credit lookup/same-key replay establishes remote result. Finalize
   under original lock and current claim, recheck ownership/state/link/version and
   candidate expiry, save new OPEN/checkpoint/receipt + original link + command
   SUCCESS atomically. Do not enforce a new sliding 30min window on every retry:
   validate it at initial submission, then require saved expiry still future.
7. Unknown timeout/local commit failure keeps PENDING. Minute recovery uses existing
   mechanism; no stored tokens/service credentials. AUTHORIZATION_REQUIRED offers
   owner Continue with fresh authorization. Expired candidate/invalidated original
   after possible remote effect needs existing conditional RESERVE compensation;
   only resolve rejection after confirmed reversal/fence, else reconciliation.
8. Confirmed terminal rejection keeps original EXPIRED/unlinked and saves latest
   safe repost failure message/code/time under original. Insufficient credits must
   not be guessed as refund delay. Unknown outcomes show pending, not failed.
9. UI retains account-scoped frozen key/input across reload/navigation; disables
   repost action/edited input while pending, shows processing until terminal
   success/rejection, Continue only for authorization. Success replaces linked old
   card with new order via existing requester-list helper and list refresh.
   Pending discovery gains REPOST label. Automatic repost remains deferred.

## Existing peer-service implementation / compatibility

Credit current reservation controller/service provides requester-owned RESERVE with
the paired headers, minimal body, new201/replay200 and durable outcome markers;
owner-authorized GET commands/{K} and POST commands/{K}/compensate. Source reviewed
at current Credit history (latest security change a2bab85). Classification:
MATCHES_APPROVED_CONTRACT for reused RESERVE wire operation; proposed REPOST adapter
mapping still needs new tests. Supplier validation checks existence/active/different
references, not opening hours; requester verification unchanged. No assumed new API.

## Alternatives and trade-offs

Recommended: extend existing coordinator with original/candidate identity. Reuses
security/recovery but needs migration, finalizer and UI/tests. Alternative: leave
repost legacy-only; smaller change but fails requested crash/reload guarantees.
Merely moving controllers cannot supply persistence/recovery. No broker proposal.

## Tests to write first after approval

- PostgreSQL clean/V7->V8 preservation; candidate stability, original guard and
  simultaneous same/different-key repost attempts; actor/input conflict rejection.
- Coordinator/API: initial/manual legacy route, timeout/lost response/local rollback,
  replay/new candidate saved once, terminal insufficiency + persistent message,
  deadline compensation, stale generation and owned fresh-token resume.
- Actual Credit/PostgreSQL integration: one reservation/one successor despite lost
  reply/restart; frozen expiry cannot be extended; no stranded late reservation.
- Frontend: pending disabled until terminal, same key/input after reload, only
  authorization Continue, success hides predecessor and shows successor; both
  modes/permissions and applicable responsive regressions.
- Backend/frontend regression, contracts, migration and coverage gates. No test
  results claimed here; no implementation tests/source/migrations written yet.

## Expected artifacts and scope

Order command payload/store/coordinator, manual finalizer/domain helper/controller,
V8 migration; frontend repost controls/command hook/types/pending label and tests;
requirements/contracts/architecture/class-sequence/data/context/ADR/traceability/
change/active-work/learning/disclosure records. Peer source and deployment remain
unchanged. Explicit schema/design approval is the stopping point.

## Current list polling (unchanged)

OrderController GET mine?mode=requester&userId=UID&page=1&size=20 (My Requests),
mine?mode=courier&userId=UID&page=1&size=20 (My Errands), and
available?page=1&size=20 (Browse Errands), all under /api/orders; optional status
filter/pagination retained. CommandController GET commands/{key}?userId=UID reads
one pending action; commands?userId=UID&page=1&size=20 discovers pending actions.
Visible Order polling is 5 seconds; focus/initial check may run sooner.
