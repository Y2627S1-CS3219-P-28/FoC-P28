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

## Admin Bootstrap Creation

### Local
When running locally, follow the steps below to create the first admin account:
1. Start up the app with `docker compose up --build`
2. Access the Firebase Auth Emulator at `localhost:4000`
3. Go to the `Authentication` tab and click `Add user`
4. Enter the email of your desired bootstrap admin user (e.g. admin@u.nus.edu), set email to verified and enter a password that is at least 8 characters long, with a number and special character
5. Save the user
6. Duplicate the .env.example file and rename it to .env
7. Edit the `ADMIN_BOOTSTRAP_EMAIL=admin@u.nus.edu`and change the email after the `=` character to the email you set in the Firebase Emulator
8. Restart the user service with `docker compose up -d --force-recreate user-service`
9. The admin account is created and you can log in with it on `localhost:8080`

### Cloud
When running locally, follow the steps below to create the first admin account:
1. Access the online Firebase Console for the project
2. Go to the `Authentication` tab and click `Add user`
3. Enter the email of your desired bootstrap admin user (e.g. admin@u.nus.edu) and enter a password that is at least 8 characters long, with a number and special character
5. Save the user
6. Duplicate the .env.example file and rename it to .env
7. Edit the `ADMIN_BOOTSTRAP_EMAIL=admin@u.nus.edu`and change the email after the `=` character to the email you set in the Firebase Emulator
8. Restart the user service
9. The admin account is created and you can log in with it

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
| GET    | `/api/users/courier-eligibility`| authenticated / services     | Retrieve boolean if user can be courier by other services     |
| PUT    | `/api/users/me`          | authenticated user                  | Update own information after registration              |
| PUT    | `/api/users/{:userId}`   | authenticated admin user            | Update user information after registration             |


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


