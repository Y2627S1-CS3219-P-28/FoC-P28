# Sprint 1 Sequence Diagrams

Authoritative source (relative to the repository root): pages 3-13 of `../../../Sprint 1/Order Service Sprint 1 Doc.pdf`.

## Initial development slice

- Sequence 7 (page 9): requester -> Order Service -> User Service/Order Data; verify requester and `DELIVERED`, then set `COMPLETED`.
- Sequence 8 (page 10): requester -> Order Service -> User Service/Order Data; verify requester and `OPEN`, then set `CANCELLED`.
- Sequence 9 (page 11): trusted lifecycle trigger -> Order Service -> Order Data; find due unaccepted `OPEN` orders, set `EXPIRED`, record expiry checkpoint.
- Sequence 10 (page 12): lifecycle trigger -> Order Service -> Order Data/Supplier/Credit; check eligibility/no existing repost, validate suppliers, reserve credits, create and link one `OPEN` repost.
- Sequence 11 (page 13): requester obtains a draft without side effects, submits reviewed details, then Order Service validates suppliers, reserves credits, and creates/links one `OPEN` repost.

The diagrams explicitly defer completion checkpoint/settlement/overdue/auto-completion, cancellation checkpoint/Credit processing, and expiry Credit processing. Do not infer those behaviors into this slice.
