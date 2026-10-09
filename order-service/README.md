# Order Service

For **local real HTTP peers + real Google Pub/Sub into local Credit**, use the
approved [local-live runbook](docs/local-live-testing.md) (CHANGE-088 / ADR-029).
It includes credentials/IAM prerequisites, Setup/build/up/Check/Pause/down and
per-workflow financial assertions. Do not run staging provisioning for local tests.

CHANGE-086 adds explicit automatic-repost expiry and saved latest failure messages.
Rebuild Order and frontend together. Flyway applies V4 at startup, disabling
legacy enabled plans without an explicit new expiry by Vincent's decision.
Do not reset volumes. See [migration handoff](docs/database-migration-workflow.md)
and [change/test evidence](changes/CHANGE-086-explicit-repost-expiry-and-persistent-failures.md).
Background retries and trusted peer credentials remain paused.

## Local container development

CHANGE-083 / ADR-026: ORDER_LIFECYCLE_CRON defaults to `0 * * * * *` for both
OPEN expiry and >=48-hour DELIVERED completion. It replaces ORDER_EXPIRY_CRON /
ORDER_AUTO_COMPLETION_CRON. Outbox recovery defaults to `0 */15 * * * *`;
immediate after-commit dispatch stays enabled. Remove old timer overrides and
update ORDER_OUTBOX_RECOVERY_CRON in ignored local .env if it still overrides
the hourly default. Rebuild Order after code changes; DB volumes are not reset.
See the [target diagram/gap table](docs/diagrams/order-lifecycle-reconciliation.md).

The root Compose stack runs this service against the local `order-postgres`
container. It does not connect to Cloud SQL. Flyway applies the versioned
migrations from `src/main/resources/db/migration` when the container starts.
The same stack starts the existing Firebase emulator used by the Firestore-backed
services. User Service keeps its existing MongoDB configuration untouched.

From the repository root:

```bash
docker compose up --build
```

Open the shared web application at <http://localhost:8080> (gateway) or
<http://localhost:3000> (frontend directly). The Order Service is available at
<http://localhost:8083> for direct local checks, and its PostgreSQL database is
published on `localhost:5433`.

The admin order listing is `GET /api/orders?status=COMPLETED&page=1&size=20`.
Omit `status` to include every status. It requires an authenticated Firebase
token and the `admin` role when running with the `prod` Spring profile. The
default local/non-production profile does not require authentication or enforce
the admin role. Page numbers start at 1 and sizes are capped at 100. This API
supports a future Admin Service dashboard; the current frontend and Admin
Service do not implement that dashboard.

Order outcome events use real Google Cloud Pub/Sub in local Compose and Cloud
Run. Local Compose uses the existing project `protean-vigil-509704-q4` and the
dev topics `order-completion-dev-v1`, `open-order-refund-dev-v1`, and
`accepted-order-cancellation-dev-v1`. Create these topics in GCP before starting
the Order Service; Compose does not create cloud topics.

Before starting the local stack, each developer must authenticate with their own
Google account and configure the ADC file path for Docker Compose. In PowerShell,
run:

```powershell
gcloud auth application-default login
gcloud auth application-default set-quota-project protean-vigil-509704-q4
```

The first command creates the local ADC file; the second records the project used
for client-library quota. If setting the quota project is denied, ask a project
administrator for `roles/serviceusage.serviceUsageConsumer` on that project.
The Order Service's Pub/Sub destination still comes from `PUBSUB_PROJECT_ID` and
the configured topic IDs.

Set `GOOGLE_APPLICATION_CREDENTIALS_HOST` in your own ignored root `.env` to
the ADC file path. For Windows, for example:

```dotenv
GOOGLE_APPLICATION_CREDENTIALS_HOST=C:/Users/<username>/AppData/Roaming/gcloud/application_default_credentials.json
```

Do not enter that `NAME=value` line as a PowerShell command; it belongs in
`.env`. For a one-session PowerShell run instead, set it with
`$env:GOOGLE_APPLICATION_CREDENTIALS_HOST = "C:/Users/$env:USERNAME/AppData/Roaming/gcloud/application_default_credentials.json"`
and run Compose in the same window. Compose mounts the file read-only into only
the Order Service container. Do not share or commit the credential file. The root
`.env.example` documents the required values.

Development and production use separate topics in the same GCP project.
Developers should have `roles/pubsub.publisher` only on the dev topics. Cloud
Run uses its attached service identity and should have that role only on the
production topics. Set `PUBSUB_PROJECT_ID` and all three `ORDER_*_TOPIC`
variables in the production deployment configuration; production topic IDs are
chosen when those topics are created. Do not grant publisher access at project
level. Local publishes are real cloud messages and can reach subscribers on the
dev topics.

Stop the stack with:

```bash
docker compose down
```

Add `-v` only when you intentionally want to delete the local PostgreSQL and
Firebase emulator volumes.

`admin-service/` is currently an empty placeholder with no runnable application
or Dockerfile. It is therefore not included as a built service until its owner
adds an implementation and image.


## Errand times and background schedules

Requester creation and repost date/time controls use local clock minutes 00, 15, 30, or 45. Defaults round up to a quarter-hour; expiry must still be at least 30 minutes ahead. The API continues receiving UTC ISO timestamps and supports existing valid deadlines.

| Setting | Default Spring cron | Purpose |
|---|---|---|
| ORDER_LIFECYCLE_CRON | 0 * * * * * | One job checks due unassigned OPEN expiry and latest-delivery >=48h completion |
| ORDER_OUTBOX_RECOVERY_CRON | 0 */15 * * * * | Recover all three event types every 15 minutes |

Publication is attempted immediately after commit; the quarter-hour outbox scan
recovers missed/failed attempts for all three types. Failed publishing can wait
until the next scan while running; Cloud Run scale-to-zero can delay it further.
Due OPEN orders are selected on each minute pass, while availability/acceptance
enforce the actual deadline immediately. Override the new settings in local .env
or deployment env if needed. Spring scheduling is not guaranteed while Cloud Run
is idle. The scheduler does not wait for consumer refund/settlement completion.

## Role authorization

With the prod Spring profile, every authenticated API request validates the Firebase token and resolves roles through User Service before reaching the controller. RequireRequesterRole protects create/complete/open cancellation/repost operations; RequireCourierRole protects accept/start/pickup/deliver/accepted cancellation; RequireAdminRole protects the all-orders query. Shared reads use RequireOrderRole for any recognized role. /mine additionally verifies its selected requester/courier mode and caller ID.

Production uses GET /api/users/role-context with the caller's verified token and requires its userId to match JWT subject. The confirmed role set is reused for that request, while courier eligibility is still checked separately. Orders retain ownership/state checks under their row locks. Missing tokens produce 401; missing roles or forged actor IDs produce 403; role/eligibility dependency failures produce 503.

The production Order-specific variable ORDER_USER_SERVICE_MODE defaults to http and takes precedence over the shared USER_SERVICE_MODE mock configuration. Explicit mock overrides are for controlled tests only. Production administrators need admin in their stored User Service roles; MOCK_ADMIN_EMAILS has no effect in HTTP mode. Ensure USER_SERVICE_URL points to the deployed User Service and the caller has a registered User profile.

The default local/non-production profile remains anonymous: role annotations are inactive, mock peers use the supplied per-user actor IDs, and local HTTP peers continue their existing token-based User Service checks. Swagger assets remain public; scheduler/system flows keep their existing separate authorization. See ADR-024 and the shared authorization diagrams for details.
