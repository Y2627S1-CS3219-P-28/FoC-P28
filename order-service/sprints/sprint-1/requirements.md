# Sprint 1 Requirements

This file narrows implementation planning; Project D1 and ADR-001 remain authoritative.

## Sequence 7 - Confirm completion

- Project D1: F4.1.5; F5.1/F5.1.1; F13/F13.1.1.
- Preconditions: authenticated actor is the original requester; order status is `DELIVERED`; expected order version is current.
- Result: replace `DELIVERED` with `COMPLETED` and return the committed state.
- Deferred: completion checkpoint (F6.4 clarification), Credit settlement/outcome, `OVERDUE`, and auto-completion.

## Sequence 8 - Cancel an OPEN order

- Project D1: F4.1.7; F5.1/F5.1.1; F13/F13.1.1.
- Preconditions: authenticated actor is the original requester; order is still `OPEN`; expected version is current.
- Result: replace `OPEN` with `CANCELLED` and return the committed state.
- Deferred: cancellation checkpoint and Credit Service cancellation processing under F11.

## Sequence 9 - Expire an unaccepted OPEN order

- Project D1 baseline: F4.1.8; F10/F10.1/F10.1.1-F10.1.4; NTH4 prerequisite.
- Sprint narrowing: process only due, unaccepted `OPEN` orders; do not process `ABORTED` reopening/expiry in this slice.
- Preconditions: trusted lifecycle trigger; status `OPEN`; expiry timestamp reached; no courier assigned.
- Result: atomically replace `OPEN` with `EXPIRED`, record one expiry checkpoint, and make repeated processing idempotent.
- Deferred: Credit outcome/release processing.

## Sequence 10 - Automatic repost

- Project D1: NTH4; creation validation references F1.1-F1.3.
- Preconditions: original is `EXPIRED`; automatic repost plan enabled and due; no repost exists; current supplier pair is valid/active/distinct; Credit Service confirms a reservation for the new order.
- Result: create exactly one distinct `OPEN` order, link original and repost both ways, mark the plan/original as reposted, and publish a deduplicable repost-created fact when that publication is in the implemented contract.
- Failure: if supplier validation or reservation fails, create no repost and keep the original `EXPIRED`.
- Constraint: initial creation/configuration does not reserve future repost credits.

## Sequence 11 - Manual repost

- Project D1: NTH4; creation validation references F1.1-F1.3.
- Draft: only the original requester may obtain copied item and supplier references from an `EXPIRED` original with no repost. Draft retrieval creates no order and reserves no credits.
- Submit: requester reviews/updates copied details, credit amount, posting/expiry timing, and other approved fields; validate them and current supplier references; reserve credits.
- Result: create exactly one distinct linked `OPEN` order and mark the original as reposted.
- Failure: invalid input, authorization failure, existing repost, invalid supplier pair, or rejected reservation creates no order.

## Cross-cutting requirements

- One valid status at a time (F13).
- Identity/authorization through User Service; no copied authority.
- Order Service stores supplier IDs, not authoritative catalogue details.
- Credit Service owns reservations and all credit policy.
- Idempotent commands/triggers use command IDs; state changes use expected order version.
- Apply Project D1 NFR3 test/coverage rules and NFR4 logging rules from the first implementation.
