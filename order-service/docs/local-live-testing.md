# Local Docker with real HTTP peers and real Pub/Sub

CHANGE-088 / [ADR-029](decisions/ADR-029-local-live-credit-push.md), Vincent,
2026-10-09, `sprint-2-3-credit`. Configuration and restricted-ingress tests pass;
live Google push authentication and financial effects still require the run below.
This is a development-only connector, not a production deployment.

## What runs where

- Browser, gateway, User, Supplier, Order, Credit, Firebase Auth emulator and
  application databases run locally in Docker. Existing named volumes are retained.
- Order uses real peer HTTP adapters and its existing authenticated security
  profile. `SPRING_PROFILES_ACTIVE=prod` selects existing JWT/method security;
  the override does **not** replace the local datasource or Auth emulator.
- Google Cloud Pub/Sub is real, in your explicitly selected existing project.
  Personal ADC publishes; there is no Pub/Sub emulator or shared service-account key.
- A temporary cloudflared HTTPS tunnel forwards only the three Credit event endpoints,
  through Order-owned nginx. Credit still validates Google's signed push identity.
  The whole Credit API, gateway and database are NOT exposed through the tunnel.
- Only refund/completion have Credit subscriptions. Accepted-cancellation is
  published for User penalties, but this setup does not implement User consumers.

## 1. Prerequisites and personal configuration

Run from the **FoC-P28 repository root**, not from `order-service`.
Docker Desktop must run Linux containers. Install Google Cloud CLI (`gcloud`)
and reopen the terminal if it is not on PATH. Use the **same personal Google
account** for CLI and ADC. Do not use a shared service-account JSON key.

```powershell
gcloud auth login
gcloud auth application-default login
gcloud auth application-default set-quota-project protean-vigil-509704-q4
```

Add these values to the root **ignored `.env`**. Preserve other values already
in that file; do not overwrite it wholesale. `LOCAL_TEST_ID` must be unique to
you: 2-12 lowercase letters/digits/hyphens, starting with a letter and ending
with a letter/digit. Shared names such as staging/main/dev are rejected.

```dotenv
LOCAL_PUBSUB_PROJECT_ID=protean-vigil-509704-q4
LOCAL_TEST_ID=vincent
GOOGLE_APPLICATION_CREDENTIALS_HOST=C:/Users/vince/AppData/Roaming/gcloud/application_default_credentials.json
```

Use **your actual ADC path and account**, not Vincent's path on another computer.
The standard Windows path is `%APPDATA%\gcloud\application_default_credentials.json`;
`CLOUDSDK_CONFIG` can change it. Never commit/read aloud/share that file or `.env`.

Your cloud owner must permit enabling the Pub/Sub API, reading the project number,
creating/updating **your isolated** topics/subscriptions and push service account,
and editing IAM on those resources. Setup grants your account publisher only on
the three test topics, Pub/Sub's service agent token creation only on the test
push identity, and scoped DLQ forwarding rights. It does not grant project-wide
publisher/admin rights. API enablement/quota needs existing owner permission.
Google/cloudflared access and outgoing HTTPS must be allowed by your network.

## 2. First setup, build and start

Setup starts just the two connector containers and provisions/reconciles owned
cloud resources. It does not start/reset application databases. Then start the
application stack and verify the live connector before using the UI.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File order-service\scripts\local-live.ps1 -Action Setup
docker compose -f compose.yaml -f compose.http-peers.yaml -f compose.local-live.yaml build
docker compose -f compose.yaml -f compose.http-peers.yaml -f compose.local-live.yaml up -d
powershell -NoProfile -ExecutionPolicy Bypass -File order-service\scripts\local-live.ps1 -Action Check
docker compose -f compose.yaml -f compose.http-peers.yaml -f compose.local-live.yaml ps
```

Stop if Setup/Check fails. Setup is idempotent for labeled resources belonging
to your namespace. A permissions failure can leave partial owned resources;
fix permissions and rerun. Existing unlabeled/foreign resources are refused,
not silently repurposed. Setup prints the public endpoint but never tokens.

Windows PS5.1 compatibility: CHANGE-089 fixes Docker's ordinary stderr progress
being mistaken for a terminating error. Host-script updates need no image rebuild.
After updating the checkout, rerun Setup; do not reset volumes or delete the
partially created network/helpers. Genuine nonzero Compose exits still fail.

Open **http://localhost:3000** and use local emulator test accounts. These commands
reuse the project's service names/volumes: they replace a previously running mock
or HTTP-only stack rather than starting a second independent stack alongside it.
The UI may have to sign in again following emulator/token changes.

### Rebuild and force-recreate later

```powershell
docker compose -f compose.yaml -f compose.http-peers.yaml -f compose.local-live.yaml up -d --build --force-recreate
powershell -NoProfile -ExecutionPolicy Bypass -File order-service\scripts\local-live.ps1 -Action Setup
powershell -NoProfile -ExecutionPolicy Bypass -File order-service\scripts\local-live.ps1 -Action Check
```

**Always rerun Setup after recreating/restarting the tunnel:** its hostname
changes, so subscriptions otherwise push to the old address. Ordinary `up -d`
without recreation preserves a running connector. If the tunnel exited, start it
with the same Compose `up -d` command before rerunning Setup. Quick Tunnels are
temporary, have no production SLA and require no account/domain of their own.

## 3. Stop safely (down)

Pause cloud delivery before stopping the local receiver:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File order-service\scripts\local-live.ps1 -Action Pause
docker compose -f compose.yaml -f compose.http-peers.yaml -f compose.local-live.yaml down
```

Pause changes only your two financial subscriptions to pull, retaining backlog.
Resume with Setup/Check. **Do not add `-v`:** that deletes database volumes.
Docker down neither clears your records nor deletes GCP resources. If Pause
fails, stopping Docker still stops containers, but cloud pushes continue failing
until you pause/repair the subscriptions. Backlog/DLQ retention is finite.
Review pending/DLQ messages before any cloud cleanup; this script deliberately
does not automatically delete topics, subscriptions, service accounts or data.

## 4. Cloud resources (example LOCAL_TEST_ID=vincent)

| Resource | Local isolated name / behavior |
| --- | --- |
| Refund topic | `open-order-refund-local-vincent-v1` |
| Completion topic | `order-completion-local-vincent-v1` |
| User penalty topic | `accepted-order-cancellation-local-vincent-v1`; no Credit subscription |
| Credit refund push subscription | `credit-open-order-refund-local-vincent-v1` |
| Credit completion push subscription | `credit-order-completion-local-vincent-v1` |
| DLQ topic | `order-credit-dlq-local-vincent-v1` |
| DLQ recovery pull subscription | `order-credit-dlq-recovery-local-vincent-v1` |
| Push identity | `foc-local-vincent-push@<project>.iam.gserviceaccount.com` |
| Stable custom audience | `https://foc-local-vincent.invalid/credit-push` (identity, NOT destination) |
| Destinations | Current tunnel URL plus `/api/credits/internal/order-events/open-refund` or `/completion` |

Managed topics/subscriptions are labeled `foc-local-test=vincent` and
`foc-component=order-credit`. Staging/dev/production resources are untouched.
Authenticated push uses the ordinary **wrapped** envelope: `message.data` is
base64 of the existing Order event JSON; `subscription` is the full Google
subscription path. Credit checks subscription/type, event snapshot, signature,
issuer, audience, verified push email and configured service account. Successful
processing returns **204**. Non-success must trigger broker retry, not a fake
proxy acknowledgement. Retry backoff is 10-600 seconds; DLQ attempt target 10 is
approximate, not an unlimited delivery guarantee. Retention is seven days and
subscription inactivity expiration is fourteen days. Inspect/recover failures.

The one-minute lifecycle job handles OPEN expiry and DELIVERED >=48-hour
completion. Outbox publication is attempted immediately after commit; its
15-minute recovery scan covers refund, completion and accepted-cancellation.
Published outbox status proves broker acceptance, not a completed ledger effect.

## 5. Test the actual financial workflows

Use **two different local users**, A requester and B courier; record baseline
balances. Avoid automatic repost for this initial financial test: background
delegated authorization/retries remain paused pending peer agreement.

| UI workflow | Expected Order / local Credit observations |
| --- | --- |
| A creates a request offering 10 credits | OPEN, new reservation RESERVED; A total unchanged, reserved +10, usable -10 |
| A cancels that OPEN request | CANCELLED; refund event then REFUNDED; A reserved returns to baseline, total unchanged |
| A creates another, leaves OPEN until selected expiry | Within a minute after deadline: EXPIRED and refund intent; eventual refund restores usable balance |
| B accepts another A request, then aborts before expiry | Synchronous reset first; same Order ID returns OPEN/courier null; reservation retained, Credit courier null; immutable ABORTED history visible only to B |
| B accepts another, waits beyond original expiry, then aborts | ACCEPTED until abort; reset then EXPIRED plus refund; immutable B history retained; eventual REFUNDED |
| B accepts, starts, picks up, delivers; A confirms completion | COMPLETED + completion intent; PAID reservation; A total -10 and reserved -10, B total +10, exactly once |
| A manually reposts a refunded expired request | New business Order ID and new reservation; linked original retained/hidden appropriately; latest failure persists if insufficient available balance |

Check refusal of self-acceptance, actions by a non-owner, and abort after start.
Do not skip authenticating B or assume a published User penalty was consumed.
Requester completion can be immediate after delivery; automatic completion
requires a **real latest delivery >=48 hours ago**. This setup does not shorten
that rule or backdate your DB. Existing isolated scheduler tests are separate
evidence; the real-clock cloud run is still a completion gate.

Use matching Order business ID/eventId across Order outbox and Credit reservation,
ledger and idempotency records. `GET /api/credits/me` returns `totalBalance`,
`reservedBalance`, `usableBalance`, `asOf`; the UI polls/refetches authoritative
Credit data. Refresh both accounts and allow push/poll latency. Inspect the local
Credit PostgreSQL (`credit_accounts`, `credit_reservations`, `credit_ledger`,
`credit_idempotency_records`) read-only if UI results are unclear. Do not merely
tick a case because Order changed status or Pub/Sub returned a message ID.

For duplicate/recovery testing, use only isolated subscriptions/test events;
record stable event IDs and verify a repeated delivery has no second balance or
ledger effect. Stop/restart just the local receiver, rerun Setup after tunnel
recreation, and confirm retained events eventually process. Never publish fake
events against staging/shared subscriptions or edit production records.

## 6. Logs, failures and scope of verification

```powershell
docker compose -f compose.yaml -f compose.http-peers.yaml -f compose.local-live.yaml logs -f --tail=100 order-service credit-service credit-push-ingress credit-push-tunnel
```

- Check fails with missing gcloud/ADC: complete section 1; do not invent credentials.
- ADC publish denial: CLI/ADC may use different accounts or topic IAM is missing.
- Push 401: check Google audience/service-account identity and exact config; do
  not disable verification. Check intentionally sends one **unauthenticated**
  public probe and expects 401; it is not a browser refresh request.
- Push 400: inspect wrapped payload/subscription/event validation, not CORS.
- Push 502 or stale endpoint: inspect Credit health and tunnel URL; rerun Setup.
- Insufficient credits during repost: old refund may not yet be processed, or new
  amount exceeds usable balance. Keep EXPIRED/message and wait/adjust; no refund
  completion can be inferred solely from successful publication.
- DLQ: inspect before acknowledging/deleting; repairing ingress alone does not
  automatically replay already dead-lettered messages.

`Check` validates cloud metadata and the public authentication/routing gate; it
does **not** publish a financial event or prove a real refund/transfer. Tests
`Test-LocalLive.ps1` (60 configuration/safety assertions) and
`Test-LocalLiveIngress.ps1` (20 actual nginx/fixture assertions) are reproducible
locally and do not provision GCP. The fixture is synthetic, NOT Credit OIDC or
financial integration. No new Maven/frontend source changes are part of this setup.
Generic workflow drift/completion scripts still reject existing manifest/record
formats; CHANGE-088 records those failures separately from connector tests.

**Outstanding:** real Google IAM/OIDC/push/ledger/browser verification; User
penalty/completion consumers; Credit replay/reconciliation gaps recorded in
[peer feedback](peer-service-api-feedback.md); background delegated credentials
and ALL retry implementation remain deferred. Sprint stays `[~]`.

## References

- [Google authenticated push](https://docs.cloud.google.com/pubsub/docs/authenticate-push-subscriptions)
- [Google dead-letter topics](https://docs.cloud.google.com/pubsub/docs/dead-letter-topics)
- [Cloudflare Quick Tunnels](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/)
- [ADR-029 topology and scope](decisions/ADR-029-local-live-credit-push.md)
- [CHANGE-088 verification record](../changes/CHANGE-088-local-live-credit-push.md)
