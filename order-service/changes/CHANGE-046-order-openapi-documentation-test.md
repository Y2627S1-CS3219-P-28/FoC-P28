# CHANGE-046 — Add Order Service OpenAPI documentation test

**Date:** 2026-09-30
**Status:** Implemented; Maven CI verification pending
**Scope:** Order Service API test coverage

## Finding

CI's required structural check found that Order Service had the
`springdoc-openapi-starter-webmvc-ui` dependency but no
`OpenApiDocumentationTest` under `src/test`. The workflow therefore stopped
before Maven verification.

## Implementation

Added:

`order-service/src/test/java/sg/edu/nus/foc/order/api/OpenApiDocumentationTest.java`

The test uses a Spring MVC slice with mocked application collaborators. It
requests `/api/orders/v3/api-docs`, writes `target/openapi.json`, and checks
that every documented operation:

- is under `/api/orders`;
- has an OpenAPI summary; and
- declares a 2xx response.

The slice avoids requiring PostgreSQL, peer services, Docker, or Cloud
credentials during the documentation check.

## Boundary

No endpoint, API contract, gateway, peer-service source, or deployment
configuration was changed.

## Verification

The CI structural condition now finds `class OpenApiDocumentationTest` and
`git diff --check` passes. The Maven wrapper and system Maven were unavailable
in this execution environment, so the test itself must still be run by CI or
locally with Java/Maven available.
