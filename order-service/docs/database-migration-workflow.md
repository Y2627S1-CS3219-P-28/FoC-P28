# Order Service Database Migration Workflow

## Latest migration handoff — V5 / CHANGE-100

V5__durable_foreground_commands.sql adds only Order-owned `order_commands`,
immutable input/result JSON, owner/lease/generation/retry metadata and the
unique unresolved-target/due/owner indexes. Expected Flyway version: **5**.
Pull matching source+migration and rebuild/start Order normally; Flyway applies
it. Clean and V1/V3-to-latest PostgreSQL upgrade tests verify prior data and
V4 legacy-plan rules survive. Never edit applied V1–V4 or use ORM auto-update.
Recovery is disable workers, preserve unresolved records and reconcile; no
destructive rollback or deletion of pending financial evidence. Full handoff:
[concurrency-order-credit.md](../concurrency/concurrency-order-credit.md).
Historical V4 commands below are superseded only as the latest version pointer.

## Latest migration handoff — V4 / CHANGE-086

- Purpose: NTH4 explicit automatic expiry and latest safe failure code/message/time.
- File: `src/main/resources/db/migration/V4__explicit_repost_expiry_and_latest_failure.sql`.
- Expected schema version: 4, applied by Flyway on matching Order startup after
  rebuild. No manual schema command, volume reset or rewriting V1-V3.
- Objects: four nullable orders columns and enabled-plan timing constraint.
  Vincent explicitly chose disabling legacy enabled plans without expiry;
  old due/credit/duration/used fields, order status/IDs/history/outbox survive.
- Verification: isolated PostgreSQL clean/latest, V1-to-latest and V3-to-V4
  tests; legacy disabled with null expiry, valid equality due=original expiry,
  invalid absent/equal new expiry rejected. Persistence rollback/overwrite/
  success-clear/automatic saved-expiry tests; exact run results CHANGE-086.
- Consuming developer: pull migration+matching source, back up non-disposable
  DB first, rebuild Order image and confirm Flyway version 4. Legacy plans
  cannot be re-enabled halfway through creation-only settings.
- Recovery: no destructive down migration. Restore reviewed pre-upgrade backup
  or deploy a reviewed forward correction. Do not guess a deadline to undo the
  user's legacy-disable decision. Real application volumes were not touched.


## Purpose

This workflow keeps independent local Order Service databases at the same approved schema version. A developer's local database is disposable working state; the versioned migration history in Git is the shared source of truth.

This document applies to every schema-affecting change, including tables, columns, indexes, constraints, identifiers, timestamps, version fields, relationships, persistence mappings that require a schema change, and approved seed/reference data.

## Status and technology boundary

- This is a workflow rule; ADR-008 approves PostgreSQL on Cloud SQL for Order Service.
- The parent Firestore convention remains applicable to sibling services and does not replace the Order Service decision.
- Flyway was approved in CHANGE-017; the configured migration directory is `src/main/resources/db/migration/`. Do not introduce a second migration format.
- CHANGE-082 / ADR-025 adds V3: internal `orders.row_id` UUID PK, unique business `orders.id`, immutable `order_courier_attempts` snapshots and repeatable checkpoints. Existing business references/FKs/outbox payloads remain unchanged. Clean PostgreSQL and V2-to-V3 upgrade tests passed; existing application volumes were not reset.
- Back up before production upgrade and deploy matching application/schema together. V3 normalizes legacy current ABORTED rows to EXPIRED and backfills recoverable courier attempts; past unexpired aborts without checkpoints cannot be recreated. There is no destructive automatic down migration. Restore the pre-upgrade backup or create a reviewed forward correction; do not drop new history or rewrite V1/V2 to roll back.

## Non-negotiable rules

1. Every shared schema change must include a new, versioned migration file in the same change set as the code that requires it.
2. A migration must describe the purpose, affected requirement/feature, assumptions, and verification evidence in the related change record or handoff.
3. Never treat a manual SQL command, IDE database edit, `ddl-auto=update`, generated ORM schema, or an uncommitted local database as the shared schema source of truth.
4. Never edit or delete a migration that another developer or environment may already have applied. Add a new corrective migration instead.
5. Migration identifiers must be unique, ordered, deterministic, and coordinated when developers work in parallel.
6. Migrations must be safe to apply to a clean local database and to an existing database at the immediately preceding version. Destructive or data-changing operations require explicit review, backup/recovery analysis, and approval.
7. Migration files must not contain credentials, secrets, machine-specific paths, or private data.
8. A migration synchronizes schema. It does not imply that ordinary local application data is identical. Seed/reference data is shared only when explicitly represented by a reviewed migration or fixture.

## Developer workflow

For a schema-affecting feature:

1. Identify the requirement, feature, affected aggregate, and database ownership.
2. Confirm the change with the developer who owns the shared persistence foundation. For Sprint 1, coordinate with the owner of the sequence 1-6 foundation before changing shared Order tables or mappings.
3. Create the next migration using the approved tool and directory. Do not invent a second migration format.
4. Apply the migration to the developer's own local database and verify the resulting schema/version.
5. Add or update persistence tests, fixtures, and rollback/recovery notes required by the approved design.
6. Commit the migration with the application/persistence change, or provide the migration in the handoff before the peer consumes the code.
7. Tell the other developer the migration identifier, purpose, required command, expected schema version, and verification result.
8. The peer applies the same migration history to their independent local database and reports any mismatch before continuing.
9. Run the focused persistence/integration tests and record the result in active work.

## Parallel-development and conflict rules

- One developer owns the initial shared schema foundation unless the allocation is explicitly changed and recorded.
- Two developers must not create competing definitions for the same table, field, constraint, or migration identifier.
- If both branches need schema changes, agree on migration ordering before either migration is merged. If ordering cannot be agreed, stop and report the conflict.
- A peer's local database contents are not a dependency. The peer needs the committed migration history, configuration contract, and reproducible fixtures.
- Do not resolve a migration conflict by rewriting shared history or dropping another developer's local database without agreement.

## Verification and handoff minimum

Every migration handoff must state:

```text
Migration ID:
Purpose:
Related requirement/feature:
Affected schema objects:
Required tool/command:
Expected schema version:
Clean-database result:
Upgrade-from-previous-version result:
Persistence/integration tests:
Rollback or recovery considerations:
Known data differences:
```

A database-backed feature cannot be marked complete when its schema change exists only in a local database, is missing from Git, has not been applied by the consuming developer, or has not been verified against the approved contract.

## Cloud and environment boundary

Cloud infrastructure provisioning and application schema migration are separate concerns. `bootstrap.sh` may provision an approved database instance/database and IAM resources; it must not silently replace the application's versioned migration process. The same reviewed migration history must be applied and verified for staging and production through an approved deployment process.
