# ADR-026: Shared minute lifecycle scheduler and 15-minute outbox recovery

- Date: 2026-10-08.
- Owner/approver: Vincent, explicit latest diagram amendments.
- Status: accepted for the cadence slice; implementation/verification in CHANGE-083.
- Supersedes only scheduler cadence/settings in ADR-022/023; preserves quarter-hour UI selectors, ADR-013 immediate dispatch, ADR-020 48-hour rule and ADR-025 abort/history.

Use one Spring scheduled method every minute with a single captured current time. It independently invokes OPEN expiry and DELIVERED auto-completion. Expiry requires OPEN, courierId IS NULL and expiresAt <= now; completion requires DELIVERED and the latest delivery at or before now minus 48 hours, with locked state/deadline revalidation. Both reuse existing atomic Order/checkpoint/outbox transactions. Failure in one pass does not skip the other.

Configure this job with ORDER_LIFECYCLE_CRON (default `0 * * * * *`), replacing ORDER_EXPIRY_CRON and ORDER_AUTO_COMPLETION_CRON. Remove old overrides from developer environments when rebuilding; those settings no longer control jobs. The new single setting can disable both checks with `-` in tests.

Outbox recovery uses ORDER_OUTBOX_RECOVERY_CRON=`0 */15 * * * *`. All three kinds (refund, completion, accepted cancellation) remain eligible; immediate AFTER_COMMIT publication is retained. This is publication recovery, not refund completion confirmation. Consumer processing can lag and must be idempotent.

Cloud Run idle/scale-to-zero timing is not guaranteed by Spring cron. This decision does not authorize a cloud billing/execution-model change, a peer API change, a broker-confirmed refund dependency, or an unapproved durable repost retry schema.
