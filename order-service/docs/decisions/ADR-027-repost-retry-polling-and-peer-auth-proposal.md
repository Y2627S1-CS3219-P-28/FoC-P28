# ADR-027: Repost retry limits, polling and deferred peer authorization

- Date/approver: 2026-10-09 / Vincent, explicit decisions in the current request.
- Status: **USER-APPROVED DESIGN; NOT IMPLEMENTED**. Trusted authorization is
  **approved for peer discussion/documentation only; implementation deferred**.
- References: CHANGE-084, ARCH-EVO-029, NTH4, ADR-025/026, FEEDBACK-005/006.
- Supersedes CHANGE-083's pending user decisions, not its historical evidence.

## Approved existing architecture

Order owns lifecycle/repost instructions; User owns identity/roles/eligibility,
Supplier owns catalogue validity, and Credit owns reservations/balances/refunds.
Automatic and manual repost create a NEW business order ID. Abort-and-reopen
keeps the old business ID and separate immutable courier-attempt UUIDs.
Preserve the expired original, history and old-ID refund outbox. Hide the original
from My Requests only after a successful linked repost. Pub/Sub publication is
not refund confirmation. No new refund-confirmation event/gate is approved here.

Retain explicit timing target `repostExpiresAt > repostDueAt >= original.expiresAt`.
Late automatic execution is allowed only while the NEW expiry is still future.
This timing field/validation is still unimplemented. ADR-026's minute lifecycle
checks and 15-minute outbox recovery/immediate publication remain unchanged.

## User-approved retry and failure behavior

1. Persist a repost request and ONE new candidate business UUID before its first
   external reservation attempt. It differs from the expired original ID, but
   all retries of that request reuse it. Bind the request/command to the verified
   requester, original and saved details. Candidate IDs are idempotency keys,
   not credentials or evidence of permission.
2. Retry only temporary failures, until success or the configured new expiry.
   Before an attempt and before creating OPEN, recheck expiry, original state,
   linkage/ownership and whether another worker already completed the task.
   Use bounded backoff between attempts; avoid busy loops and duplicate workers.
   Exact backoff, leases, persistence/API names require the implementation design,
   not an invented claim of user approval or current code.
3. A timeout can mean Credit committed but the reply was lost. Reconcile the
   SAME candidate through its existing reservation read/idempotent write; confirm
   matching requester/amount/order ID and active RESERVED state before OPEN.
   A 404 observation does not prove an in-flight write cannot later complete.
   Late successful reservations/local commit failure need agreed recovery or
   compensation; stopping a worker must not silently strand reserved money.
4. Confirmed insufficient funds, invalid details, authorization rejection or
   expired new deadline stop automatic retries of that unchanged request. Do not
   interpret every conflict/404/5xx as transient or every 409 as insufficient.
5. While unresolved/failed, the original remains EXPIRED, visible and unlinked.
   Keep retry state separate from OrderStatus. On success link/hide the original
   and show the new OPEN order. At most one successful repost per original.

| Confirmed outcome | Approved behavior | Short UI text (proposed wording) |
| --- | --- | --- |
| Credit 409 with semantic `INSUFFICIENT_CREDITS` | Stop; original EXPIRED | Repost failed: insufficient available credits. |
| Invalid submitted details / invalid Supplier pair | Stop; original EXPIRED | Repost failed: check the request details. |
| Authorization rejected | Stop; original EXPIRED | Repost could not be authorized. |
| New expiry reached before successful OPEN | Stop; original EXPIRED | Repost expired before it could be posted. |
| Temporary transport/unavailable service failure | Save pending retry; original EXPIRED | No terminal failure message; optional neutral retry indicator |
| Conflict, missing account or unknown/mismatched reservation | Classify/reconcile; never assume retryable or OPEN | Short appropriate terminal message if non-retryable |

The user approved short appropriate permanent-failure messages, expanding the
earlier insufficient-credit-only failure-message proposal. Exact wording is not
user-supplied. Do not expose IDs, tokens, raw errors or technical retry details.
A delayed old refund and genuinely inadequate funds may yield the SAME error:
never label it "refund pending" without authoritative Credit information. A
stopped request may later be resubmitted with corrected details/available funds;
the safe command/candidate rules for that new attempt still need detailed design.

## User-approved polling direction

Use authenticated HTTP polling, not WebSocket/SSE or a frontend broker, to refresh
Order lists/status/repost outcomes and Credit balance in the shared Next.js app.
Poll only when auth is ready and a valid token is available; preserve authorization
and use the existing gateway/useApi boundary. Pause hidden tabs, refetch on focus,
avoid overlapping requests and refetch immediately after the user's own successful
mutation. Do not keep an expired token in a polling closure or treat pending auth
restoration as logout. Recommendation: visible-page 10-15 seconds; the precise
interval/backoff remains an implementation setting, not a fixed approved number.
Polling reads backend state; it does not run lifecycle jobs or trigger browser
state updates from a scheduler. Existing unified USER dashboard stays unchanged;
no role switcher or ADMIN permissions are introduced.

## Trusted peer authorization: proposal only, DO NOT IMPLEMENT YET

Both scheduled auto repost and saved manual background retries are unattended:
there may be no browser, logged-in user or renewable Firebase user bearer later.
Ordinary foreground calls still use Firebase identity. Current User role-context
derives its UID from that token; Credit reservation requires token UID=requesterId;
Supplier routes are user-authenticated. A lifecycle secret is not that credential.

Document for User/Supplier/Credit owner agreement a separately authenticated,
restricted Order service identity with explicit delegated requester operations.
Peers must verify the service caller AND authorize the requester/action. Saving
auto-repost instructions records user consent inside Order, not permission for
Order to bypass peer checks. Current requester roles/eligibility must remain
authoritative in User Service; sending requesterId/candidateId/verified=true is
not verification. Do not store long-lived user refresh tokens to implement this.

No issuer, audience, token/header format, endpoint, IAM grant or secret is selected
or implemented by this ADR. Cloud Run service identity is a candidate mechanism;
platform IAM authentication alone does not replace application requester
delegation. The provider owners must agree the precise contract, denial/revocation,
rotation/renewal, auditing and local test strategy in FEEDBACK-005 first. HTTP
mode remains fail-closed; no anonymous bypass or fallback to mocks is authorized.

## Trade-offs and implementation gate

- Polling is simpler across backend instances and reuses HTTP auth; updates have
  bounded delay and extra reads. WebSocket/SSE requires connection renewal and
  instance-to-instance distribution. No new real-time transport approved.
- Durable same-ID retries avoid lost-response duplicate reservations but require
  persistent tasks, concurrency control and reconciliation; a browser-only retry
  cannot resume after restart/logout. Fixed IDs alone do not guarantee atomic
  transactions across Credit and PostgreSQL.
- Stopping permanent failures prevents futile loops. Stopping insufficient funds
  can require a later user resubmission even if a delayed refund subsequently
  restores funds; the user explicitly accepts that limitation.
- Service delegation supports unattended work but expands the trust boundary.
  Its implementation is explicitly deferred pending discussion/agreement.

Before implementation: present concrete task schema/API/worker/polling design,
resolve missing/mismatched provider semantics, use a NEW Flyway migration if
needed, and preserve existing source authorities/history. No migration or source
edit is made by this documentation decision.

## Planned verification, not tests run

- Timing equality/boundary/new-expiry rejection; no changed automatic settings
  after creation; manual future-expiry validation.
- Lost reply/restart/concurrent workers reuse candidate ID and reserve once;
  committed reservation reconciles before OPEN; terminal reservation cannot OPEN.
- Insufficient funds vs other conflicts; invalid/forbidden stop; transient retry;
  expiry/local rollback/late remote success recovery without orphaned funds.
- EXPIRED card persists through failed/pending attempts; original hides only on
  success; short correct semantic message for permanent failure, no refund guess.
- Polling auth restoration/token renewal, hidden/focus, cleanup, no overlap, stale
  response protection, immediate mutation refresh and unauthorized-user isolation.
- Peer security tests: wrong caller/audience/expired credential, unpermitted
  delegated action/requester, revoked role, replay/idempotency, positive cases.
- Backend/RTL/contract/isolated PostgreSQL/live peer/UI and unchanged >=80% coverage
  gates. Existing CHANGE-083 results do not verify these unimplemented behaviors.
