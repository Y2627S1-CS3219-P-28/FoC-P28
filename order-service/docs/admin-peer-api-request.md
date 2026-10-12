# Admin Peer API Requests

Date: 2026-10-11. Owner: Yao Xiang. Scope: CHANGE-111 Admin UI prototype.
Evidence: current repository controllers, request/response models and services;
source inspection only, not live integration verification.

The UI currently uses ONLY Order-owned sample endpoints under
/api/orders/admin-mock. Role lookup is mocked too. No real User, Supplier,
Credit, Firebase account or Order data is mutated. Replacing the adapter with
real providers is a separate milestone. Do not interpret sample fields or
successful demo actions as completed peer implementation.

## Already implemented: reuse these, do not create duplicates

| Service | Existing endpoint | Contract / use |
|---|---|---|
| User | GET /api/users/role-context | Caller bearer token; returns userId and roles. Compare returned UID with signed-in UID and require admin. |
| User | GET /api/users/{userId} | Existing User profile, currently no explicit admin guard in this controller. Reuse the path; privacy/authorization needs hardening before admin integration. |
| User | GET /api/users/summary/{userId} | Existing name/identity summary for resolving requester/courier labels. |
| Order | GET /api/orders?status=COMPLETED&page=1&size=10 | Admin-authorized paginated all-orders query. Omit status for all current statuses. Newest created first. |
| Order | GET /api/orders/{id} | Existing detail lookup; adapter can use exact ID search without another endpoint. Apply selected status to this single result when presenting the search page. Current read authorization remains in force. |
| Supplier | GET /api/suppliers?q=Deck&status=all&page=1&size=10 | Existing paginated name search, admin-only inactive/all views. Name search also matches building/location; adapter must document this broader match. |
| Supplier | GET /api/suppliers/{id} | Existing exact ID lookup including inactive suppliers; adapt the result to a one-item search page. |
| Supplier | POST /api/suppliers/lookup | Existing bulk name resolution: body ids array; returns items and missingIds. |
| Supplier | POST /api/suppliers | Admin-only creation; 201 SupplierResponse. Reuse existing CreateSupplierRequest. |
| Supplier | DELETE /api/suppliers/{id} | Admin-only soft deactivation; 200 SupplierResponse with active false. |
| Supplier | PATCH /api/suppliers/{id} | Existing admin field/active update; not a missing API. |

Supplier creation already accepts name, type, building, floor,
locationDescription, latitude, longitude, openingTime, closingTime, imageUrl.
SupplierResponse additionally has id, active, openNow, createdAt, updatedAt
and optional distanceMeters. The demo reuses these wire fields and existing form.
No Supplier API work is requested for the current prototype.

## ADMIN-API-001: User search, paging and trusted profile reads

- Responsible service: User Service.
- Status: adjustment required; existing endpoints must be reused.
- Actual: GET /api/users returns List<User> without search or pagination.
  GET /api/users/{userId} returns one User. Neither controller operation verifies
  an admin; the current listing implementation calls repository.findAll().
- Required: server-side name or exact UID search, bounded pagination and
  authoritative admin verification for cross-user profiles/listing.
- Proposed extension: GET /api/users?searchBy=name&q=Alex&page=1&size=10.
  searchBy is name or id, q trimmed; empty q lists all. Page is one-based,
  size 1..100; filter/count before paginating, stable username/userId ordering.
  GET /api/users/{userId} remains the exact profile path.

Expected paged response:

~~~json
{
  "items": [{
    "id": "mongodb-profile-id",
    "userId": "firebase-uid",
    "username": "Alex Tan",
    "email": "alex@u.nus.edu",
    "roles": ["requester", "courier"],
    "penalty": 1,
    "isCourierSuspended": false,
    "suspensionEndDate": null
  }],
  "page": 1,
  "size": 10,
  "totalItems": 1,
  "totalPages": 1
}
~~~

This envelope changes the existing unpaged array contract. Coordinate the
existing callers' migration (or an explicitly versioned/opt-in paged response);
do not silently break them. No new duplicate list/detail API is required.
Errors: 400 invalid query, 401 invalid token, 403 non-admin, 404 unknown UID for
detail; standard repository error envelope. Never trust a body-supplied admin role.

### Profile fields requested by the UI

The current User model stores id, userId, username, email, roles, penalty,
isCourierSuspended and a java.sql.Date suspensionEndDate. It does NOT store
phoneNumber, address or active, and a SQL date cannot represent an exact
suspension ending time. Demo phone/address/active values are sample extensions.

If these fields are available in a later User-owned model, return them from the
existing profile endpoint. Otherwise return null/Not provided until the owner
agrees the model, privacy and migration. Credit balances must come from Credit,
not be invented or independently maintained by User Service.

## ADMIN-API-002: Explicit suspension end date/time

- Responsible service: User Service.
- Status: missing administrative mutation; current penalty operations are not
  a substitute and must not be invoked to implement manual suspension.
- Proposed operation: PUT /api/users/{userId}/suspension.
- Purpose: administrator chooses a future UTC ending instant; User Service owns
  eligibility, enforcement and eventual suspension restoration.

Expected request:

~~~json
{ "suspensionEndDate": "2026-11-01T04:00:00Z" }
~~~

Expected response: 200 updated User profile, including
isCourierSuspended=true and the precise suspensionEndDate. Reapplying the same
ending instant is safe. Invalid/past instant -> 400, missing user -> 404,
missing/invalid token -> 401, non-admin -> 403.

The demo models the repository's courier suspension fields. Account-wide versus
courier-only suspension needs the User owner's explicit policy agreement; do not
assume requester capabilities are disabled merely because a courier is suspended.
Also agree how already assigned orders and pending financial commands can finish
or recover. Storage must preserve timezone/instant precision, not date alone.

## ADMIN-API-003: Promote an existing user to administrator

- Responsible service: User Service.
- Status: not present in this checkout. Reconcile any newer main implementation
  before adding an API; historical FEEDBACK-011 discussed a subsequently reverted
  user-promotion route, and is not evidence of a working route here.
- Current PUT /api/users/{userId} accepts username/email only; it cannot promote.
- Proposed operation: PUT /api/users/{userId}/user-promotion.
- Expected request: no body; this operation adds only the admin role.
- Expected response: 200 existing User profile with admin added to roles,
  retaining all existing roles. Repeating the request must not duplicate roles.
- Caller: authenticated existing administrator, verified by User Service;
  401 invalid token, 403 not admin, 404 unknown user, 409 inactive user.

The returned role must become visible through existing role-context; frontend
must never supply a trusted role directly. Coordinate last-admin and recovery
policies with the owner. No real promotion happens in the prototype.

## ADMIN-API-004: Safe user deactivation

- Responsible service: User Service.
- Status: existing DELETE /api/users/{id} performs a hard Mongo deletion and
  returns the string User removed, without an explicit controller admin check.
  That is unsuitable for the requested safe deactivate operation.
- Proposed adjustment: reuse DELETE /api/users/{userId} as an admin-authorized
  soft deactivation (or agree a separate activation resource if existing hard
  deletion must remain compatible). This is a proposal, not a completed change.
- Expected request: no body; confirmation occurs in UI.
- Expected response: 200 updated profile with active=false, preserved identity,
  roles and history. Repeat deactivation should be safe.
- Errors: 401 invalid token, 403 non-admin, 404 unknown user, 409 when deactivation
  would strand active orders/unresolved commands unless a safe recovery policy
  has been agreed.

User owns login/token revocation and eligibility decisions. Agree how inactive
users can resolve existing Order/Credit obligations; do not delete/revoke first
and assume remote effects roll back. The mock flips only its sample active flag.

## ADMIN-API-005: Administrator reads another user's credit balance

- Responsible service: Credit Service.
- Status: missing. Existing GET /api/credits/me is caller-only and must remain so.
- Proposed operation: GET /api/credits/users/{userId}/balance.
- Request: userId path variable, caller's bearer token; no body.
- Purpose: profile displays authoritative Credit-owned balance without
  impersonating another Firebase user.

Expected response, reusing existing BalanceResponse:

~~~json
{
  "userId": "firebase-uid",
  "usableBalance": 100,
  "reservedBalance": 5,
  "totalBalance": 105,
  "asOf": "2026-10-11T12:00:00Z"
}
~~~

Verify an authoritative admin role; 401 invalid token, 403 non-admin,
404 no credit account, 503 role provider unavailable. A unavailable credit
projection should not hide the user profile; show Credits unavailable and retry.
No balance mutation is requested.

## ADMIN-API-006: Administrator reads another user's credit transactions

- Responsible service: Credit Service. Status: MISSING in the inspected checkout.
- Existing implementation: GET /api/credits/me/transactions uses the verified
  caller's UID. It cannot load an arbitrary selected admin profile. Keep its
  ownership rules; do not impersonate the selected user or pass their token.
- Required operation: GET /api/credits/users/{userId}/transactions?page=1&size=20.
- Request: selected Firebase UID in the path, one-based page and size 1..100
  in query parameters, administrator's bearer token; no request body.
- Expected response: existing TransactionPageResponse / TransactionResponse
  wire format, filtered by selected user before paging, newest occurredAt first,
  stable transactionId tie-breaker. Example:

~~~json
{
  "items": [{
    "transactionId": "transaction-id",
    "type": "RECEIVED",
    "amount": 10,
    "direction": "CREDIT",
    "occurredAt": "2026-10-11T12:00:00Z",
    "orderId": "order-id"
  }],
  "page": 1,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
~~~

`amount` is a nonnegative credit magnitude; direction is CREDIT or DEBIT.
Existing type values: INITIAL_ALLOCATION, RESERVATION, PAID, RECEIVED,
REFUNDED_CANCELLATION, REFUNDED_EXPIRY, REFUNDED. orderId may be null for
initial allocation; occurredAt is an ISO-8601 UTC instant. Empty existing
accounts return an empty items page. Invalid paging=400, invalid/missing
token=401, non-admin=403, missing account=404, role lookup unavailable=503.
Verify admin authorization and selected-user isolation in provider tests.

The prototype uses only Order-owned sample history, with the same wire fields.
Its fixture allocations and transactions are illustrative, not actual Credit
records or policy. Profile and history failures are independent: show history
retry while retaining the selected profile and admin actions.

## Prototype contracts and replacement checklist

### CHANGE-114 mapping boundary: no new service endpoint required

Existing Supplier create/detail fields `latitude`, `longitude` and
`locationDescription` cover map placement. The create JSON contract is unchanged;
the frontend sends numeric coordinates, not a Google/Photon result or token.
An address selection shows its full label in the form and can prefill an empty
locationDescription within its current200-character limit. Existing manually
entered description is preserved. The address label is a reference, not a
reverse-geocoded guarantee after dragging; coordinates are the exact placement.
No structured/persisted `address` field or additional backend API is requested.

Administrative operations still use the gateway and Order-owned mock adapter.
Public map rendering/search are separate, isolated in frontend/src/lib/map-provider.ts:

- Tiles: HTTPS OpenStreetMap tiles via Leaflet1.9.4, with visible attribution,
  normal browser cache/referrer behavior and no background/bulk downloads.
- Suggestions: GET https://photon.komoot.io/api/?q={address}&limit=5&lang=en&lat=1.2966&lon=103.7764.
  Requests are debounced650ms, require3 characters, timeout8s, and omit cookies/
  Authorization/Firebase UID. NUS coordinates bias the search; they do not
  prohibit results outside Singapore. Query changes abort and ignore old replies.
- Photon returns GeoJSON features with geometry.type=Point and coordinates
  ordered [longitude, latitude], plus properties such as name/street/housenumber/
  district/city/postcode/country. The adapter validates types/ranges, deduplicates
  and bounds suggestions; it never silently fabricates locations on failure.
- Selected pins/manual coordinate fields stay synchronized. Public-search errors
  retain map/manual entry; tile errors retain coordinates and Google Maps links.
- CHANGE-115 removes the list overview map. Detail map loads the selected supplier
  by ID; creation picker remains. No provider/backend contract change.

Replace the public provider adapter when production traffic needs a contracted
or self-hosted geocoder/tile service. Public Photon allows reasonable use but
offers no availability guarantee. Primary documentation:
[Photon](https://github.com/komoot/photon),
[OSM tile policy](https://operations.osmfoundation.org/policies/tiles/),
[Leaflet stable release](https://leafletjs.com/download.html).

Supplier deactivation now exists only in detailed view, using its same confirmed
sample operation; no contract change. Order detail user/supplier names navigate
to Admin routes; separate ID copy controls use existing IDs. Grouped Order cards
preserve all returned fields, including a separate repost card and unknown fields.

CHANGE-113's dedicated Order/Supplier pages require no additional endpoint:
Order details reuse GET /orders?orderId={id}&page=1&size=1 in this sample surface;
Supplier details reuse GET /suppliers/{id}. Existing real Order/Supplier detail
reads remain listed above. Actions only moved in the UI; their contracts stay.

All prototype routes live inside Order Service, behind an explicit local-only
condition: ORDER_ADMIN_MOCK_ENABLED=true, FOC_ENVIRONMENT=local and configured
Firebase Auth emulator. Local Compose uses prod authentication; this condition
does not disable JWT verification or replace real production role guards.
Cloud configs omit the flag and emulator, so sample controllers are not registered.

| Mock path (prefix /api/orders/admin-mock) | Method | Sample contract |
|---|---|---|
| /role-context?userId={uid} | GET | Existing User role-context shape, roles=["admin"]; compare UID in browser. |
| /orders?status=OPEN&orderId={id}&page=1&size=10 | GET | Existing OrderResponse fields plus resolved names, paged. |
| /users?searchBy=name&q=Alex&page=1&size=10 | GET | Paged sample User fields plus clearly sample-only profile/credit projections. |
| /users/{id} | GET | One sample profile. |
| /users/{id}/transactions?page=1&size=20 | GET | Selected sample user's TransactionPageResponse wire format, newest first. |
| /users/{id}/suspend | POST | Future suspensionEndDate JSON -> sample profile. |
| /users/{id}/promote | POST | No body -> sample profile with admin. |
| /users/{id}/deactivate | POST | No body -> sample active=false profile. |
| /suppliers?searchBy=name&q=Deck&page=1&size=10 | GET | Paged SupplierResponse fields. |
| /suppliers | POST | Existing SupplierInput JSON -> 201 sample SupplierResponse. |
| /suppliers/{id} | GET | One sample SupplierResponse, including inactive records. |
| /suppliers/{id}/deactivate | POST | No body -> sample inactive SupplierResponse. |

Mock pagination is in-memory because this is a small isolated fixture, not the
real Order database or scheduler. All real lifecycle DB queries remain unchanged.
The immutable copy mapper prevents HTTP readers from altering stored fixtures.

Replace frontend/src/lib/admin-api.ts transport and any necessary normalizers,
not the page controls. Use real role-context, validate UID/admin membership, then
real provider calls with their actual HTTP methods and bodies. The frontend role
check only controls UI: every actual backend operation must enforce admin too.
Do not fall back to sample data when a real dependency fails. Verify actual
contracts, errors, authorization, mutation semantics and integration tests before
calling real Admin UI complete. No sibling backend implementation was changed.
