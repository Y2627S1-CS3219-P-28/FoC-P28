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

Stop the stack with:

```bash
docker compose down
```

Add `-v` only when you intentionally want to delete the local PostgreSQL and
Firebase emulator volumes.

`admin-service/` is currently an empty placeholder with no runnable application
or Dockerfile. It is therefore not included as a built service until its owner
adds an implementation and image.
