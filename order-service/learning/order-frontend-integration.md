# Order Service frontend integration lessons

## Simple explanation

The browser does not need to know database IDs by heart. It asks the authoritative Supplier
Service for active suppliers, shows human-readable names, and sends the selected IDs to Order
Service. The browser also cannot call the gateway from another local port unless the gateway
explicitly permits that origin with CORS. Signup is a small cross-service workflow: User Service
creates the profile, then Credit Service receives an authenticated registration fact.

## General engineering principle

- Keep reference data owned by its provider. A dropdown should display provider data and submit a
  stable identifier, rather than duplicating or inventing a local catalogue.
- Treat browser origin, gateway, and backend as separate network boundaries. CORS controls which
  browser origins may call the gateway; it is not authentication or authorization.
- For multi-service signup, use the provider's documented contract, forward the authenticated
  identity, and make the operation retry-safe. Partial failure needs an explicit recovery policy.

## How this project applies it

- Supplier Service `GET /api/suppliers` is read-only and authenticated. Order Service stores only
  supplier references. The shared frontend requests active suppliers through `useApi()` and sends
  the chosen IDs in the existing create-order payload.
- The local frontend is served at `http://localhost:3000` and the gateway at
  `http://localhost:8080`. The gateway now allows only those local origins and handles OPTIONS
  preflight requests. This is local development configuration, not a production wildcard.
- After Firebase signup and User Service registration, the frontend calls Credit Service
  `POST /api/credits/registration-facts` with the Firebase bearer token and
  `{eventId,userId,occurredAt}`. Credit Service owns allocation and idempotency.
- Order cards resolve their supplier references through one batched Supplier Service lookup,
  display names/buildings, and keep the opaque IDs only for internal API actions. Internal Order
  IDs are likewise not part of the user-facing card.

## Common failure modes

- A 404 from `localhost:3000/api/...` means the browser called the frontend server instead of the
  gateway; configure the runtime API base to port 8080 or use the gateway URL.
- A CORS preflight error means the gateway did not return the required allow-origin/method/header
  response. It is not proof that the backend endpoint is missing.
- A supplier dropdown can be empty when Supplier Service has no seeded catalogue or when the
  authenticated request is rejected. Keep a loading/error/empty state and do not accept arbitrary
  IDs as a hidden fallback.
- A list of cards should not issue one supplier request per card. Deduplicate all pickup/delivery
  IDs and use the provider's batch lookup; retain a safe ID fallback when a provider record is
  missing or temporarily unavailable.
- Signup can create a User Service record before Credit Service succeeds. The current approved
  frontend reports the failure; a later workflow change may add reconciliation or orchestration.

## Testing and verification

Test helper contracts with Vitest/RTL, run frontend typecheck/lint/build, and send an authenticated
browser request through the gateway. Verify an OPTIONS request from port 3000 returns 204 with the
allow-origin header. Verify supplier labels are display-only and submitted payloads contain IDs.
Verify Credit registration payload fields and bearer forwarding with a provider contract test.

## Related records

- `order-service/changes/CHANGE-025-supplier-selection-cors-credit-registration.md`
- `order-service/changes/CHANGE-027-order-card-supplier-display.md`
- `order-service/docs/frontend-integration-workflow.md`
- `order-service/docs/requirements-traceability.md`
