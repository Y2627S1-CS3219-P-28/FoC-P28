# Sprint 1 Sequence Diagrams

- `updated-overall/admin-list-orders.md` (CHANGE-057): Admin caller obtains all orders or filters by status; the Order Service authenticates the Firebase token, resolves the User Service role, then queries through the application and persistence boundaries with `Pageable`.

Authoritative source (relative to the repository root): pages 3-13 of `../../../Sprint 1/Order Service Sprint 1 Doc.pdf`.

## Initial development slice

- Sequence 7 (page 9): requester -> Order Service -> User Service/Order Data; verify requester and `DELIVERED`, then set `COMPLETED`.
- Sequence 8 (page 10): requester -> Order Service -> User Service/Order Data; verify requester and `OPEN`, then set `CANCELLED`.
- Sequence 9 (page 11): trusted lifecycle trigger -> Order Service -> Order Data; find due unaccepted `OPEN` orders, set `EXPIRED`, record expiry checkpoint.
- Sequence 10 (page 12): lifecycle trigger -> Order Service -> Order Data/Supplier/Credit; check eligibility/no existing repost, validate suppliers, reserve credits, create and link one `OPEN` repost.
- Sequence 11 (page 13): requester obtains a draft without side effects, submits reviewed details, then Order Service validates suppliers, reserves credits, and creates/links one `OPEN` repost.

The Sprint 1 diagrams above are the currently implemented subset. The updated overall source `../../../Order Service Overall Doc - Updated.pdf` supersedes their Sequence 5-8 architecture with typed completion/cancellation events. CHANGE-063/ADR-013 supersedes CHANGE-053's publish-before-status implementation ordering: Order state/checkpoint/receipt and event intent commit atomically; a listener dispatches after commit and cron recovers due rows. Delivery is at least once. Under CHANGE-064/ADR-014, accepted cancellation before expiry waits for a synchronous Credit hold then reopens directly from `ACCEPTED` to `OPEN`; cancellation at/after expiry transitions to `ABORTED` and emits an event. ADR-001 still prohibits `ABORTED -> OPEN`. Under CHANGE-055/ADR-010, the assigned courier performs accepted cancellation.

## Updated overall-design sequence diagrams

The following proposed diagrams expand the updated overall design's event flows. They are kept separate from the original Sprint 1 source diagrams so the older implementation baseline remains reviewable:

- [Sequence 5: complete an order](updated-overall/sequence-5-complete-order.md)
- [Sequence 6: cancel an OPEN order](updated-overall/sequence-6-cancel-open-order.md)
- [Sequence 7: cancel an ACCEPTED order](updated-overall/sequence-7-cancel-accepted-order.md)

These diagrams reflect three event-specific publisher pairs, full resulting Order snapshots, topic placeholders, assumed future consumers, and Google Cloud Pub/Sub. Sequence 5 publishes one completion event for both overdue and non-overdue orders, with overdue facts in its payload. The after-commit callback runs before the transactional service call returns, so the request waits for the publish attempt but no peer consumer reply. Outbox delivery is at least once; peer consumers must deduplicate and remain unverified. See CHANGE-053/054/056/063 and ADR-013.
