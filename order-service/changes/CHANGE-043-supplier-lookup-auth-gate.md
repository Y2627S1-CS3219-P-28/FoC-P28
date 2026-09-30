# CHANGE-043 — Gate browser API calls on Firebase authentication

**Date:** 2026-09-30
**Status:** Implemented; frontend runtime verification pending
**Scope:** Order Service frontend integration only

## Finding

After a hard refresh, Firebase restores the signed-in user and ID token
asynchronously. Page-level hooks could begin loading Order and Supplier data
before that state was settled, allowing a request without an `Authorization`
header to reach the gateway and return `401 UNAUTHENTICATED`.

## Implementation

- `useApi()` now refuses to call `fetch()` while authentication is loading, when
  there is no signed-in user, or when `getIdToken()` returns no token.
- Supplier lookup and permission effects wait for settled authentication.
- The Errands Order fetch and Supplier catalogue/detail/editor effects wait for
  settled authentication.
- A focused unit test verifies that an absent bearer token is rejected before
  a network request can be made.

## Boundary and compatibility

No Supplier Service or Order Service backend source, API contract, database,
gateway, or peer configuration was changed. The existing Firebase bearer
authentication contract remains unchanged.

## Verification

`git diff --check` passed. Frontend Vitest, typecheck, lint, Docker rebuild,
and hard-refresh browser verification require the local Node/Docker/browser
runtime and remain pending in this environment.
