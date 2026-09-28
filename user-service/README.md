# user-service

Owner: Oh Yi Xian

The user account and role service for FoC. It stores application-level user
profiles and roles in MongoDB, while Firebase Authentication remains responsible
for authentication and email/password credentials.
Spring Boot 4.1.1 on Java 21 with MongoDB.
It follows the repo conventions in [AGENTS.md](../AGENTS.md).

- **API:** `/api/users`
- **Health:** `/actuator/health` (with readiness and liveness groups)

## Run

```bash
# Whole system through the gateway at http://localhost:8080
docker compose up --build

# Service from source against the emulators
docker compose up -d firebase-emulator
MONGODB_URI="mongodb+srv://USERNAME:PASSWORD@cluster.mongodb.net/UserServiceDB?retryWrites=true&w=majority" \
PORT=8081 ./mvnw spring-boot:run
```

- Swagger UI: `/api/users/docs`
- OpenAPI JSON: `/api/users/v3/api-docs`
- Health: `/actuator/health`

## Sprint 1 API

Authenticated endpoints use:
`Authorization: Bearer <Firebase ID token>`
The Firebase ID token identifies the caller. The User Service uses the Firebase
UID from the verified token rather than trusting a UID supplied by the client
when retrieving the current user's profile.

| Method | Path                     | Who                                 | Purpose                                                |
| ------ | ------------------------ | ----------------------------------- | ------------------------------------------------------ |
| POST   | `/api/users`             | unauthenticated during registration | Create an application user after Firebase registration |
| GET    | `/api/users/me`          | authenticated user                  | Retrieve the currently authenticated user's profile    |
| GET    | `/api/users/role-context`| authenticated / services            | Retrieve roles for authorization by other services     |
| PUT    | `/api/users/{:userId}`   | authenticated user                  | Update user information after registration             |


## Data model

The `users` collection contains one document per application user in MongoDB.

| Field                | Type             | Notes                                                        |
| -------------------- | ---------------- | ------------------------------------------------------------ |
| `id`                 | string           | MongoDB document ID                                          |
| `username`           | string           | User's application username                                  |
| `userId`             | string           | Application/Firebase user identifier                         |
| `email`              | string           | User email; unique                                           |
| `roles`              | array of strings | Application roles such as `requester`, `courier`, `admin`    |
| `penalty`            | integer          | Current user penalty count; defaults to `0`                  |
| `isCourierSuspended` | boolean          | Whether the user is currently suspended from courier actions |
| `suspensionEndDate`  | date / null      | End date of the courier suspension                           |

## Test

Locally:
```bash
$env:MONGODB_URI="mongodb+srv://..."
./mvnw verify
```

Tests are not implemented yet.


