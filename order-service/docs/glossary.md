# Glossary

- **Application role**: frontend-level `ADMIN` or `USER` classification. It does not replace backend authorization authorities.
- **Actor context**: authenticated identity, roles, and trusted-service information attached to a protected call.
- **Checkpoint**: immutable lifecycle record associated with a status transition.
- **Command ID**: stable identifier used to deduplicate retried commands.
- **Completion hold**: condition preventing completion while an Admin-owned report is investigated; not an order status.
- **Delivery deadline**: task-start time plus delivery time limit.
- **Lifecycle trigger**: trusted initiator for time-based expiry, auto-completion, or automatic repost processing.
- **Order version**: optimistic-concurrency value supplied with state-changing commands and facts.
- **Outcome notification**: an immutable, deduplicable statement that a named outcome occurred. Courier outcomes use separate completed, overdue, and aborted operations; the operation name carries the outcome and no generic `facts` map is accepted.
- **OVERDUE**: completion-time boolean fact/flag; never an order status.
- **Reopening**: future behavior that changes the same `ABORTED` order back to `OPEN` using the same ID.
- **Repost**: one new order created after an original becomes `EXPIRED`, linked to the original and owning a separate lifecycle.
- **Repost plan**: optional configuration stored on the original; it does not reserve future credits.
- **Supplier reference**: Supplier Service-owned identifier stored by Order Service instead of copied catalogue truth.
- **Terminal outcome**: `COMPLETED`, `CANCELLED`, `EXPIRED`, or `ABORTED` fact supplied to other services for their own policies.
- **USER mode**: Requester or Courier function selected or performed by the same `USER` account; not a separate account or application.
- **Responsive web client**: the single shared Next.js application used from desktop and mobile browsers; not a native mobile application.
