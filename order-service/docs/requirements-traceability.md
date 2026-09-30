# Order Service Sprint 1 Traceability

| Sequence | Requirement/design source | Planned behavior | Test evidence |
|---|---|---|---|
| 1 | F1.1-F1.3, Sprint 1 diagrams | Validate requester/suppliers/credits and create `OPEN` order | Implementation added; unit/integration verification pending |
| 2 | F2, Sprint 1 diagrams | List unexpired, unassigned `OPEN` orders | Backend and responsive Browse Errands UI added; live contract verification pending |
| 3 | F3, F13 | Verify courier eligibility, bind the authenticated courier identity, reject self-acceptance, and atomically accept once | Implementation added; concurrency/contract verification pending |
| 4 | F4.1.1-F4.1.2 | `ACCEPTED` to `IN_PROGRESS` | Implementation added; verification pending |
| 5 | F4.1.3 | `IN_PROGRESS` to `PICKED_UP` with checkpoint | Implementation added; verification pending |
| 6 | F4.1.4 | `PICKED_UP` to `DELIVERED` with checkpoint | Implementation added; verification pending |
| 7 | F4.1.5, F5.1 | Requester confirms `DELIVERED` to `COMPLETED` | Implementation added; domain test coverage; integration verification pending |
| 8 | F4.1.7, F5.1 | Requester cancels `OPEN` to `CANCELLED` | Implementation added; domain test coverage; integration verification pending |
| 9 | F4.1.8, F10, NTH4 | Trusted trigger expires due unaccepted `OPEN` order | Implementation added; lifecycle verification pending |
| 10 | NTH4 | Eligible expired order creates one linked `OPEN` repost after reservation | Implementation added; peer/idempotency verification pending |
| 11 | NTH4 | Requester receives draft and submits one linked repost | Backend and responsive manual-repost UI added; peer/idempotency verification pending |

Frontend vertical-slice note: CHANGE-022 covers the approved shared Next.js
implementation for sequences 1-11. Browser and live peer verification are not
claimed from static builds or unit tests.

## Cross-cutting

- `Order` is the sole lifecycle aggregate and stores supplier IDs only.
- User identity/role/eligibility is obtained through approved User Service adapters.
- Credit reservation is synchronous before an order or repost becomes `OPEN`.
- Commands and lifecycle triggers carry IDs; state changes carry expected versions.
- Flyway migrations are the schema source of truth; Hibernate only validates.
- Repost event publication is deferred for Sprint 1.
- The approved frontend vertical slice uses active Supplier Service names in pickup and delivery
  selectors while submitting supplier IDs; the authoritative catalogue remains in Supplier Service.
- Order cards resolve pickup and delivery references through the authenticated Supplier Service
  lookup contract, display names/buildings, retain IDs only for internal actions, and omit the
  internal Order ID from user-facing cards. Cards wait for lookup completion and never render
  opaque supplier IDs as a transient or error fallback.
- The shared dashboard exposes requester and courier functions without a client-side mode switch;
  backend User Service identity and authorization remain authoritative.
- Signup provisions the Credit Service account through its authenticated registration-fact contract;
  credit policy and account persistence remain owned by Credit Service.
- Local browser calls from `http://localhost:3000` to the gateway on `http://localhost:8080` use an
  explicit development-only CORS allowlist; deployed origins are unchanged.
- The post-request form mirrors the Order domain rule that `expiresAt` must be at least 30 minutes
  after creation and explains the constraint before submission; the backend remains authoritative.
