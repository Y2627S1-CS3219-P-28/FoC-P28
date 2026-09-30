# Overall Architecture

Authoritative diagram: `../../../High Level Architecture Diagram - FOC.png`.

## Runtime topology

The existing shared top-level Next.js application is the single responsive web client for desktop and mobile browsers. It supports the `ADMIN` application role and the `USER` role with Requester/Courier modes. `USER` modes are functions of one account; do not assume `ADMIN` can use them. The source diagram visually separates web/mobile and administrator clients, but ADR-004 records the approved implementation interpretation that both experiences live in this shared application. Each backend service still owns and persists only its own data, and backend authorization remains authoritative. Observability consumes machine-readable logs, metrics, and audit events. NTH5 CI/CD and cloud deployment builds, tests, deploys, configures, and supports recovery for the platform.

## Cross-service responsibilities

- Order Service asks User Service for identity, roles, and courier eligibility and publishes order facts such as `COMPLETED`, `OVERDUE`, and `ABORTED`.
- Order Service asks Supplier Service to validate supplier pairs and resolve current supplier details.
- Order Service asks Credit Service to reserve/query/settle credits and publishes terminal order outcomes; Credit Service owns the resulting policy.
- Admin Service monitors orders and may place/release completion holds or apply supported case resolutions through explicit Order Service contracts.
- Admin Service calls User and Supplier administrative contracts; it does not write their databases.

Technology options shown by the diagram are HTTPS/JSON for request-response and event transport for factual events. These are not concrete repository choices until approved and configured.
