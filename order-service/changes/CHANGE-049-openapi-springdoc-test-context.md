# CHANGE-049 — Restore Springdoc route in OpenAPI test slice

**Date:** 2026-09-30
**Status:** Implemented; CI verification pending
**Scope:** Order Service OpenAPI documentation test

## Finding

After the Maven wrapper permission fix, CI reached the Order Service tests but
`OpenApiDocumentationTest` failed because `GET /api/orders/v3/api-docs`
returned 404. The MVC test slice imported the WebMVC and Swagger Springdoc
configurations without importing the required core `SpringDocConfiguration`.

## Implementation

Added `org.springdoc.core.configuration.SpringDocConfiguration` to the
test's Springdoc configuration imports. This supplies the core Springdoc bean
that the WebMVC OpenAPI resource configuration requires, allowing the test to
register the documentation endpoint before checking its paths, summaries, and
2xx responses.

## Boundary

Only the Order Service test configuration changed. No runtime endpoint,
OpenAPI contract, peer-service source, frontend, or deployment configuration
was modified.

## Verification

The pasted GitHub log confirms the previous test reached the OpenAPI test and
failed specifically with a 404 at line 73. `git diff --check` passes after the
fix. Local Maven execution is unavailable, so GitHub CI must verify the
updated test context.
