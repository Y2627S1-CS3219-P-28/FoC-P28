# Pending Admin API requests

Updated2026-10-29, CHANGE-163. **Only Report integration remains a missing Admin HTTP API contract.** Completed User directory/search work is documented in [User Service handoff](../../user-service/user-servicehandoff.md); older requests remain in [history](admin-peer-api-request-history.md).

| Report operation | Required contract |
|---|---|
| Synchronous resolve without Order callback | Proposed PUT /api/reports/internal/order-resolutions/{commandId}; verified admin token; command/report/order/requester/courier/admin IDs, generation1, reply. Durable bound APPLIED or definite NO_EFFECT response; exact replay; Report journals revision/preimage internally. |
| Compensation consumer | ReportResolutionCompensationRequested generation2; atomically deduplicate, fence delayed resolve and restore only this operation's unchanged revision. |
| Durable result producer | ReportResolutionCompensationResult through Report outbox; bound COMPENSATED/RECONCILIATION_REQUIRED result; duplicate requests republish saved result; dedicated authenticated push to Order. |

The user approved this guarded Order contract stub; Report-owner implementation and real integration remain pending. This broker-compensation protocol supersedes the earlier proposed HTTP restore/result-query requirement. Do not call the handoff's callback-based status route from Order. Exact payloads/status/security/recovery: [Report handoff](report-servicehandoff.md). No live coordinator is enabled; Reports uses local sample data and cloud Coming soon.

Approved Report requirements (CHANGE-163): keep/expose the server-owned responseDeadlineAt; timely courier response or deadline expiry makes READY_FOR_REVIEW. Allow admin resolution while still awaiting after a UI warning/confirmation. Persist required reply as resolutionNote with resolvedBy/resolvedAt; reply visible only to submitter/admin. Resolution and its deduplication/preimage/revision journal must commit atomically; implement fenced conditional compensation/result outbox as above. Violation category remains undecided. Report/UI implementation is pending; no deadline duration is changed here.

Existing User profile/promotion/deactivation, new initial directory/name/ID search, Credit balance/history and Order/Supplier Admin operations use real APIs in both local Compose variants and cloud. Generic Admin mock endpoints remain removed. User penalty consumers and the Supplier deactivation/admission guard are separate outstanding integrations, retained in [peer feedback](peer-service-api-feedback.md), not completed by the directory endpoint.
