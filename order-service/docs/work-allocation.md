# Order Service Work Allocation

## Current Sprint 2-3 workstream (2026-10-08)

Vincent (Developer 2) is assigned the Order-owned Sprint 2-3 work on `sprint-2-3`,
explicitly authorized by the user in CHANGE-081. Source: Project D1 and the local
`Order Service Overall Doc.pdf`. Scope includes the requested lifecycle/history,
repost and completion work; it does not authorize sibling-service implementation.
The accepted one-current-Order plus immutable courier-attempt history model is
approved in principle; Credit reset semantics and detailed migration remain pending.
Reposting retains old/new rows and hides the reposted expired original from the
requester list; implementation is pending. This allocation does not reassign Yao Xiang's historical work or
grant blanket ownership of shared frontend/configuration files.

## Historical Sprint 1 allocation

| Developer | Developer number | Assigned scope | Branch |
|---|---|---|---|
| Yao Xiang | Developer 1 | Order Service sequence diagrams/features 1-6 | `sprint-1/seq-1-to-6` |
| Vincent | Developer 2 | Order Service sequence diagrams/features 7-11 | `sprint-1/seq-7-to-11` |

## Collaboration rules

- Both developers follow the same Project D1, architecture, contracts, diagrams, specification-driven workflow, and TDD rules.
- Yao Xiang owns sequence diagrams/features 1-6.
- Vincent owns sequence diagrams/features 7-11.
- A developer must not modify the other developer's assigned implementation without agreement.
- Shared contract, architecture, requirement, domain-model, test-fixture, interface, or database changes must be discussed and recorded before implementation.
- Each developer must work on the correct Git branch.
- Each developer must update only their own active-work file.

## Shared frontend allocation

- `../frontend/` is a shared top-level Next.js application; no developer has blanket ownership of it.
- When an approved Order Service feature requires frontend work, feature ownership follows the sequence allocation above, but the developer must recheck frontend Git state and all relevant active-work records before editing.
- Shared API clients, authentication, permissions, layouts, navigation, runtime configuration, and design-system components require coordination and recorded agreement before modification.
- A developer must not alter another developer's frontend feature or overwrite unrelated frontend work without agreement.
- No frontend files are currently allocated or recorded as in progress by this Order Service workflow. Recheck this on every coding turn.

## Feature status values

- `[ ]` Not started.
- `[~]` In progress.
- `[!]` Blocked.
- `[x]` Completed and verified through the non-negotiable completion gate in `AGENTS.md`.

This file records shared team allocation only. The ignored `docs/local/developer-profile.md` identifies the current developer in a local workspace.
