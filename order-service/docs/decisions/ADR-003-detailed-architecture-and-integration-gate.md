# ADR-003: Detailed Architecture and Integration Approval Gate

Date: 2026-09-26

Status: Accepted; infrastructure conflict superseded by ADR-008

Approved by: User-provided Order Service architecture-review prompt

## Context

The overall and Sprint 1 diagrams define logical structure but do not settle every feature-level component, concrete peer API, failure mode, communication style, event transport, or operational trade-off. Peer services may also implement contracts that differ from the Order Service's expected logical contracts. Implementing directly from a high-level diagram would permit silent architecture and API drift.

## Decision

1. Every feature or sequence requires a detailed component, class, service-sequence, contract, data, concurrency, consistency, idempotency, failure, security, observability, and test proposal before application code or implementation tests.
2. The proposal must distinguish approved architecture, actual peer implementation, proposed design, unresolved decisions, and user-approved deviations. The user explicitly decides every interaction before implementation.
3. User, Supplier, Credit, and Admin service implementations are inspected for the initial Order Service review. Each later feature rechecks the relevant provider's instructions, APIs/DTOs, adapters, event schemas, tests, authentication, configuration, errors, retries, idempotency, and API documentation.
4. An actual API that differs from the expected contract cannot be used without explicit approval. The review must compare both contracts, risks, adapter feasibility, and provider-change feasibility. A rejected API remains unimplemented and is recorded as an open integration API gap.
5. Every event candidate is compared with synchronous request-response, query/polling, and an in-process domain event. A durable broker requires a specific approval plus delivery, ordering, idempotency, retry/dead-letter recovery, versioning, monitoring, security, and contract-test decisions.
6. Credit reservation and credit-related `COMPLETED`, `EXPIRED`, `CANCELLED`, and `ABORTED` processing are synchronous before the related Order operation is finalized. This does not add Credit calls to ordinary `ACCEPTED -> IN_PROGRESS`, `IN_PROGRESS -> PICKED_UP`, or `PICKED_UP -> DELIVERED` transitions.
7. Penalty evaluation remains in User Service and does not block an already committed Order outcome. Order Service can send only approved factual completed, overdue, and aborted signals. No concrete event schema, transport, or durable broker is approved by this ADR alone.
8. `OVERDUE` remains an Order-owned flag and timestamp evaluated during completion, with no continuous overdue scheduler.
9. A dashboard, history/list query, internal timer, expiry, or reposting process does not require a broker merely because it is non-real-time or time-based.
10. Record approved architecture decisions and AI assistance before implementation, then follow TDD and the existing completion gate.

## Superseded infrastructure conflict

The Order Service design sources named PostgreSQL and Kubernetes, while the current parent repository instructions require Firestore and Cloud Run. ADR-008 records the user's approved Order Service choice of PostgreSQL on Cloud SQL and Cloud Run. This conflict is no longer a blocker for Order Service infrastructure; the parent Firestore default remains applicable to sibling services.

## Consequences

- Overall architecture is context, not blanket implementation approval.
- No application source, implementation test, broker, contract change, or mismatched API use may precede the relevant explicit approval.
- `docs/integration-api-gaps.md` is created only when a rejected or unavailable provider capability must be tracked; no speculative gap file is required.
- A feature cannot be complete while an architecture decision, API mismatch, or required peer contract test is unresolved.
- This ADR approves the review process and communication boundaries, not a concrete endpoint, DTO, persistence schema, event, or broker.
