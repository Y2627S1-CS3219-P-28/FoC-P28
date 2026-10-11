# CHANGE-108: Pending command display after browser storage loss

- Status: PROPOSED, not an implementation or approved API extension.
- Date: 2026-10-11; requester: Vincent, current matching concurrency branch.
- Classification: material frontend design refinement plus additive owner-facing
  command response/access projection. Detailed approval required before code/tests.
- Related existing approvals: ADR-033/035 command recovery, ADR-037 manual repost;
  existing creation/acceptance/abort/F10 repost and NFR2 responsive/NFR3/NFR4 gates.
- Evolution: ARCH-EVO-044. No new broker interaction, peer API or financial rule.

## Existing design and inspected limitation

useOrderCommand restores only local IndexedDB intent. PendingOrderCommands polls
the owned command list, but renders separate labels/messages, not order cards.
CommandView has key/kind/orderId/status/reason but no pending display details.
CREATE/REPOST immutable descriptions/supplier references/amount/duration/expiry
already exist in saved CommandRequest. ACCEPT/ABORT target existing Orders.
Pending acceptance is not yet in courier /mine. Normal GET-by-ID can deny the
unassigned courier after expiry; do not loosen general read permissions to render
the pending command. Existing key/lease/guard/compensation behavior stays intact.

## Proposed owner-only display contract

Add nullable displayDetails to owner-facing CommandView, generated after existing
identity/command-owner checks. Fields: itemDescription, pickupSupplierId,
deliverySupplierId, offeredCredits, deliveryTimeLimitMinutes, expiresAt; for
ACCEPT/ABORT also current committed orderStatus and version. Existing orderId is
the actual target for ACCEPT/ABORT, original for REPOST and candidate for CREATE;
never treat a CREATE candidate as an already-created order.

CREATE/REPOST displayDetails come from frozen saved command input; ACCEPT/ABORT
details come from the existing target Order under command-owner authorization,
not a general read-access grant. Missing original target produces a safe pending
fallback, not a forged card. Never expose raw command payload, identity tokens,
lease/owner/hash, peer diagnostics or another user's command. Supplier references
are resolved using existing frontend supplier lookup; no new backend peer calls.
No persistence migration is expected: inputs and target Orders already exist.
Credit bodies/headers/result/compensation/event contracts remain unchanged.

## Proposed frontend design

- A shared account-scoped discovery/cache polls existing paginated list API every
  five seconds while visible, covers all relevant pages and clears on account
  change. Cards also retain local intent for a submission not yet discoverable.
- Match existing-order commands by kind + orderId and preserve the original key.
  Accept: OPEN card with Acceptance pending or Continue with authorization.
  Abort: ACCEPTED card with Abort pending or Continue with authorization.
  Disable competing card actions while unresolved. ABORTED attemptId history is
  read-only; never attach a live command to that historical snapshot.
- My Errands can show a pending acceptance's existing OPEN target from its owned
  display projection even before assignment. Deduplicate against normal cards by
  business orderId; label pending acceptance, never imply it is already accepted.
  Pending tasks remain discoverable outside the selected board page/status filter.
- CREATE/REPOST stay in Requests in progress as submitted-detail summaries, not
  fake Order cards. A pending repost's original remains a normal EXPIRED card;
  disable another repost until resolved. No successor card before confirmed save.
- Resume uses POST /commands/{sameKey}/resume with fresh user authentication,
  no new UUID/body. Polling remains read-only except existing local same-key
  resubmission when submission was not recorded. Remote-only discovery must not
  invent/resubmit frozen input on a missing-key response.
- A command disappearing from a pending page is NOT proof of success. For a
  previously known key, fetch owned terminal result, then clear pending and
  refresh normal lists. Network/discovery failure must retain known pending state.
- Preserve desktop/mobile UI primitives, account boundaries and role/mode guards.
  No global prohibition on independent new orders is introduced by this proposal;
  new-key duplicate creation after storage loss needs separate explicit policy.

## Options, trade-offs and approval

1. Keep the existing separate panel: no contract change, but poor action context
   and no submitted details after local loss.
2. Recommended: owner-only safe display summary plus shared discovery/card binding.
   More UI/cache/contract tests, but works without IndexedDB and does not grant
   unrelated order access or duplicate recovery handlers.
3. Fetch each target through normal GET: fewer DTO changes, extra requests and
   insufficient authorization after expiry; cannot supply uncreated order details.

Approve option2 response extension and rendering/recovery rules before writing
application code or tests. User's requested UI direction is recorded, not treated
as approval of the newly discovered access/response extension.

## Planned test-first verification and synchronization

Backend owner-only summary/source fields, no raw payload/secrets, list/status
contracts, read after courier eligibility/deadline changes, missing target;
frontend empty IndexedDB recovery, pending accept/abort card state/Continue/same
key, creation/repost details without fake cards, no duplicates/history binding,
pagination/filter coverage, stale response/account switch/failed refresh, terminal
transition and no terminal inference from list absence. Run focused then relevant
backend/frontend regressions, lint/typecheck, responsive/manual checks.

Expected areas: CommandView/OrderCommandService/OpenAPI/tests; shared command
discovery hook/context, useOrderCommand, pending panel, cards/actions and errands/
my-errands/my-requests/repost controls/tests; ADR/contract/context/traceability,
class/sequence/evolution/change/active-work and required disclosure after approval.
Peers, schema, outbox, event payloads, infrastructure and automatic repost unchanged.
Source inspection only this turn: no tests written/run; live verification not done.
Source authority PDF fingerprints unchanged; current Sprint/rollout gates remain.
