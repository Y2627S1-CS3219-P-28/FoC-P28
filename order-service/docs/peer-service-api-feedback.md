# Pending peer-service work

Updated 2026-10-29, CHANGE-162. Admin HTTP endpoint requests are Report-only; User directory is implemented and linked. Separate consumer/behavior follow-ups remain below. Completed/superseded requests are in [history](peer-service-api-feedback-history.md). Local provider inspection does not imply cloud rollout. Supplier/Credit backend code was not changed.

## User Service — penalty consumers (FEEDBACK-014)

Implement authenticated Pub/Sub consumers for both channels below; current User source has penalty HTTP helpers but no penalty subscriber. These are scoring consumers, not new human-admin HTTP mutation endpoints.

| Topic | Body eventType | Effect |
|---|---|---|
| courier-penalty-addition-{environment}-v1 | PenaltyScoreAdditionEvent | Add positive penaltyAmount |
| courier-penalty-deduction-{environment}-v1 | PenaltyScoreDeductionEvent | Deduct positive penaltyAmount |

Use local topics only with the emulator; staging uses dev, production prod. Body has exactly seven fields:

~~~json
{"eventId":"stable-event-id","eventType":"PenaltyScoreAdditionEvent","orderId":"order-id","courierId":"courier-uid","penaltyAmount":4,"occurredAt":"2026-10-28T08:00:00Z","appliedReason":"WRONG_LOCATION_DELIVERY"}
~~~

Pub/Sub attributes: eventId, eventType, eventVersion=1. Decode base64 message.data for push delivery. The JSON courierId is the score recipient, including when a requester/system completes the Order.

| appliedReason | Magnitude | Direction |
|---|---:|---|
| ACCEPTED_TASK_CANCELLATION |2|Addition|
| OVERDUE_DELIVERY |1|Addition|
| SUCCESSFUL_DELIVERY |1|Deduction|
| WRONG_LOCATION_DELIVERY |4|Addition|
| ITEM_MISHANDLING_OR_DAMAGE |4|Addition|
| DELIBERATE_ITEM_RETENTION_OR_THEFT |10|Addition|

- Order saves final score intent with the lifecycle transition and outbox. Apply Penalty alone stores a provisional policy; completion/cancellation emits one final outcome per courier attempt. Reopened Orders can have different couriers/attempts: do not deduplicate by orderId alone.
- Deduplicate by **body eventId**, not Google's messageId. Order retries reuse the persisted ID. Atomically save the processed ID and update the score; concurrent duplicates have one effect. ACK only after commit or a known duplicate; retry transient failures and monitor dead letters/permanent failures.
- Agree score floor/cap and suspension policy, including the theft permanent-suspension requirement. Two topics have no cross-topic order: clamping after each delivery makes +1/-1 order-dependent at zero. Define a ledger/reconciliation policy or explicitly agree arrival-order semantics before claiming order-independent scoring.
- Retired COURIER_UNRESPONSIVE(3) can still occur in saved historical events; preserve read compatibility. Do not create new choices for it. AcceptedOrderCancellationTaskEvent is retired; no new legacy subscription/scoring or penalty-replacement API is requested. Credit events remain separate.
- Full schemas/examples: [AsyncAPI](asyncapi/asyncapi.yaml). Provider consumer/security/duplicate tests and local integration are still required.

## Supplier Service — guard existing deactivation (FEEDBACK-015)

The existing admin DELETE /api/suppliers/{supplierId} soft-deactivates, but does not consult Order. Update that operation rather than creating a duplicate API.

- Before deactivation, forward the authenticated caller's Firebase token to GET /api/orders/suppliers/{supplierId}/active-involvement.
- **200:** {"supplierId":"supplier-id","hasActiveOrders":true}. Refuse deactivation with409 when true. Unknown/auth/network/5xx failure must not be treated as false. Owner still checks supplier existence and admin authority.
- Order SQL checks pickup/delivery involvement in OPEN, ACCEPTED, IN_PROGRESS, PICKED_UP and DELIVERED, including holds. The read alone does not reserve deletion permission: agree a deactivation/admission policy for the check/delete race. No Supplier backend changes have been made here.

## Report Service — resolution integration (FEEDBACK-018)

CHANGE-163 approved clarification2026-10-29: response deadline is required; preserve responseDeadlineAt and server-owned READY_FOR_REVIEW transition on timely response or deadline expiry. Admin may also resolve a still-awaiting report after a UI warning/confirmation. Persist required reply/resolutionNote, resolvedBy and resolvedAt; return reply only to submitter/admin. The provided Report model lacks these reply/audit fields and its resolved() guard currently blocks early resolution. Report owner must implement these changes plus the saga journal/conditional restore/fence/result protocol below. Category changes are not approved. Exact requirements and acceptance cases: [Report handoff](report-servicehandoff.md#approved-report-behavior--change-163-2026-10-29). This is pending provider/UI work, not a verified integration.

Report Service is absent. Order now has the user-approved guarded contract stub, default disabled. Actual peer integration remains pending; Admin Reports is local sample data/cloud Coming soon.

Required Report implementation (proposed contract, full examples in [handoff](report-servicehandoff.md)):

| Operation | Required input | Required result/guarantee |
|---|---|---|
| PUT /api/reports/internal/order-resolutions/{commandId} without Order callback | command/report/order/requester/courier/admin IDs, generation1, reply; verified admin token | Durable bound APPLIED/NO_EFFECT response; exact replay. Save prior state/reply and resulting revision atomically inside Report. |
| Pub/Sub compensation consumer | Eight fields: eventId, commandId, reportId, orderId, requesterId, courierId, adminId, generation2 | Deduplicate and fence delayed generation1; conditionally restore only this operation, preserving newer work. |
| Committed result producer | Same bound IDs plus compensationId, result eventId, outcome/code | Report outbox publishes COMPENSATED (REVERSED/FENCED_NO_EFFECT) or RECONCILIATION_REQUIRED; repeated requests republish the saved result. Dedicated OIDC push. |

This replaces the prior proposed HTTP restore/result-query requirement. Specify400/401/403/404/409/503 and conflicting-key behavior with the owner. Do not call a Report route that calls back into Order release. Keep the hold until local release commits; never compensate committed success due to a lost response. Request/result topics: report-resolution-compensation-{local|dev|prod}-v1 and report-resolution-compensation-result-{local|dev|prod}-v1. No GCP resources were created; agreed retention/reconciliation and real authenticated tests are activation gates.

Report creation must persist stable report/intent identity before remote hold placement and recover uncertain calls, preventing orphan holds. Confirm acceptance of Order's approved retry behavior: repeated placement reuses the hold but returns the **current** Order snapshot; delivery may have progressed. No snapshot migration is planned. Admin replies must be visible only to the submitter and authorized admins.
