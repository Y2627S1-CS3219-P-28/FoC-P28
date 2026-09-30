# Sprint 1 Scope

Source (relative to the repository root): `../../../Sprint 1/Order Service Sprint 1 Doc.pdf`.

## Approved Sprint 1 pack

The approved pack covers Order Service F1, F2, selected Sprint 1 portions of F3-F6 and F13, plus NTH4 by an approved planning decision. It includes sequences 1-11 for creation, available-order viewing, acceptance, manual lifecycle transitions, authorization, checkpoints, expiry, and automatic/manual reposting.

## Initial development slice

No development is authorized yet. When separately approved, the initial requested slice is:

1. Sequence 7: requester confirms a `DELIVERED` order as `COMPLETED`.
2. Sequence 8: requester cancels an `OPEN` order as `CANCELLED`.
3. Sequence 9: trusted lifecycle processing expires an unaccepted due `OPEN` order.
4. Sequence 10: automatically create at most one eligible linked repost after expiry.
5. Sequence 11: return a manual repost draft and, after requester review, create at most one linked repost.

Sequences 1-6 are prerequisites represented by the Sprint 1 pack, but they are not authorized for coding by the current setup-only request.

## Explicitly out of scope

- Simultaneous acceptance edge case F3.2.1.
- 48-hour auto-completion.
- `ABORTED` to `OPEN` reopening.
- Account order history and shared checkpoint views.
- Completion checkpoint and terminal Credit outcome processing.
- `OVERDUE` evaluation.
- Admin Service integration, completion holds, reporting, and penalty processing.
- Cancellation Credit processing (deferred with F11 in the Sprint 1 pack).
- Detailed UI layouts/interactions, because no approved UI specification exists.
- Unapproved concrete API transport, endpoints, DTOs, persistence schema, frameworks, or infrastructure.
