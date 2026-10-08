# Order Service

## Local container development

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
