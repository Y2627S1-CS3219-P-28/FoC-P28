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

Order outcome events use Google Cloud Pub/Sub. The default local Compose stack
starts the Pub/Sub emulator, initializes the three topics, and points Order
Service to the emulator using the local `demo-foc` project ID. Local publishing
does not require Google credentials and does not send events to Google Cloud.
The emulator and its messages are local development state; recreated emulator
containers start fresh, and Compose initializes the topics again.

Outside the local Compose stack, the application keeps the configured Google
Cloud project and topic IDs in `application.yaml` and uses Application Default
Credentials when `PUBSUB_EMULATOR_HOST` is unset. The deployed runtime must have
Pub/Sub publisher access to those topics.

Stop the stack with:

```bash
docker compose down
```

Add `-v` only when you intentionally want to delete the local PostgreSQL and
Firebase emulator volumes.

`admin-service/` is currently an empty placeholder with no runnable application
or Dockerfile. It is therefore not included as a built service until its owner
adds an implementation and image.
