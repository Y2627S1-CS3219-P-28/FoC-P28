# CHANGE-032 — Temporary Credit outcome boundary for Sprint 1

- Date: 2026-09-30
- Status: Approved temporary exception; provider implementation pending
- Scope: Order Service Sequences 7, 8, 9, and automatic repost credit effects
- Approved by: Vincent

## Decision

Credit settlement and release remain Credit Service-owned and synchronous at
the Order boundary. Because the provider endpoints are not yet implemented,
Order Service may use a validating no-op `CreditServicePort` mock for local
Sequence 1–11 verification. The mock does not transfer balances and must not be
treated as a production integration.

## Proposed provider contract

Order Service's HTTP adapter targets:

```text
POST /api/credits/orders/{orderId}/settlement
{ commandId, requesterId, courierId, amount, expectedOrderVersion }

POST /api/credits/orders/{orderId}/release
{ commandId, requesterId, amount, outcome, expectedOrderVersion }
```

Both operations must be idempotent by `commandId`, authenticate the trusted
service caller, own reservation/ledger state, and document success and error
responses. `CANCELLED` and `EXPIRED` are valid release outcomes.

## Implementation

- `CreditServicePort` now exposes `settle` and `release`.
- `MockPeerAdapters` validates the command and returns success without changing
  external state.
- `HttpPeerAdapters` forwards the proposed request shapes and bearer token.
- Completion, cancellation, expiry, and automatic repost paths call the port
  before committing their Order-side result.
- Lifecycle-trigger authentication is checked by Order Service and an explicit
  bearer-form internal credential is forwarded for automatic peer calls.

## Trade-off and exit criteria

This keeps the Order state machine testable and preserves Credit ownership, but
cannot verify real balance or ledger behavior. Remove the mock only after the
Credit owner agrees to the contract, implements both endpoints, and the actual
peer code passes contract/idempotency/error/authentication verification.

## Affected artifacts

`docs/peer-service-api-feedback.md` (FEEDBACK-001),
`docs/requirements-traceability.md`, `docs/active-work/vincent.md`,
`docs/architecture-evolution.md`, and the Order application/adaptor classes.
No peer-service source or learning file was changed.
