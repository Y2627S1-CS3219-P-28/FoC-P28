# Sequence 5: Complete an order

## Effective compact-event amendment — CHANGE-092 / ADR-031 (2026-10-09)

Yao Xiang approved the exact seven-field Order-only payload: eventId, eventType, orderId, orderStatus, creditAmount, occurredAt, courierId. This replaces full snapshots and overdue facts ONLY for OpenOrderRefundTaskEvent and OrderCompletionTaskEvent. AcceptedOrderCancellationTaskEvent retains its existing v1 envelope/snapshot. Internal outbox versions and Pub/Sub eventVersion attribute remain (compact schema v2); topic names and DB schema unchanged. Old pending snapshot rows normalize at dispatch from their saved facts, with stable IDs. Lifecycle/outbox scheduling, locks, synchronous Credit assignment/reset and ADR-025 abort behavior remain. Historical v1 descriptions below are superseded for these two bodies. Credit currently requires the old snapshot and overdue; FEEDBACK-009 is INCOMPLETE_OR_INCOMPATIBLE. User completion penalties need an agreed separate overdue source. User approved implementing Order-only and documenting peer work; live integration remains blocked, Sprint [~].


```mermaid
sequenceDiagram
    actor Requester
    actor Scheduler as Spring scheduler
    participant Controller as OrderController
    participant Lifecycle as LifecycleProcessingService
    participant Repository as OrderRepository
    participant Transition as OrderTransitionService
    participant Store as PostgreSQL transaction
    participant Outbox as Order event outbox
    participant Listener as After-commit listener
    participant Relay as OrderOutboxDispatcher
    participant Interface as IOrderCompletionTaskPublisher
    participant Publisher as OrderCompletionTaskPublisher
    participant Transport as Google Cloud Pub/Sub
    participant Credit as Credit consumer (compact migration pending)
    participant User as User Service consumer (future peer work)

    alt Requester confirms before automatic completion
        Requester->>Controller: Complete delivered order
        Controller->>Transition: complete(orderId, actor, expectedVersion)
        Transition->>Transition: Verify requester and DELIVERED state
    else At least 48 hours since delivery
        Scheduler->>Lifecycle: autoCompleteDue(now)
        Lifecycle->>Repository: Find DELIVERED orders with deliveredAt <= now - 48h; lock rows
        Repository-->>Lifecycle: Eligible orders
        loop Each eligible order
            Lifecycle->>Transition: autoComplete(orderId, now)
            Transition->>Repository: Lock and recheck status and deliveredAt cutoff
        end
        Note over Lifecycle,Transition: The lifecycle actor is recorded; repeated passes use a stable AUTO_COMPLETE command ID
    end
    Transition->>Transition: Derive overdue flag and deadline from ACCEPTED/DELIVERED checkpoints
    Transition->>Transition: Set COMPLETED and create resulting checkpoint
    Transition->>Outbox: Store seven-field COMPLETED event; versions in outbox columns
    Transition->>Store: Save Order, checkpoint, receipt, and outbox row
    Store-->>Transition: Commit all rows atomically
    Store-->>Listener: Transaction committed
    Listener->>Relay: Dispatch completion event now
    Relay->>Outbox: Claim row with lease
    Relay->>Interface: publishOrderCompletionTask(event)
    Interface->>Publisher: publish typed event
    Publisher->>Transport: Send compact JSON on configured completion topic; schema v2 attribute
    alt Transport confirms publish
        Transport-->>Publisher: Publish accepted
        Publisher-->>Relay: Success
        Relay->>Outbox: Mark PUBLISHED
        Transport-->>Credit: Deliver completion event asynchronously
        Transport-->>User: Deliver completion event asynchronously
    else Publish fails
        Transport-->>Publisher: Failure
        Publisher-->>Relay: Publication error
        Relay->>Outbox: Record retry time and error
        Note over Outbox,Relay: Cron recovery retries the durable pending event
    end
    opt Requester initiated completion
        Transition-->>Controller: Completion result
        Controller-->>Requester: OrderResponse
    end
```

The same `OrderCompletionTaskEvent` is published for every completion, whether requester-confirmed or automatically completed at least 48 hours after the `DELIVERED` checkpoint. It includes exactly the seven fields listed above. Credit Service transfers/settles the reserved credits to the courier. User Service still owns late penalties/on-time score reduction, but must agree a separate authoritative overdue source before integrating this reduced body (FEEDBACK-009). Overdue is true only when delivery occurred strictly after the deadline. The Order state and event intent commit atomically. Immediate delivery is attempted after commit, and cron recovers failed or interrupted attempts. A crash after Pub/Sub accepts the message but before the outbox marker commits can cause a duplicate; consumers deduplicate using stable `eventId`. Peer replies are not awaited. The auto-completion cron is configurable and defaults to once per minute; `ORDER_COMPLETION_TOPIC` selects `order-completion-dev-v1` locally/staging and `order-completion-prod-v1` in production. CHANGE-077/ADR-022 retains this one-minute completion scan and immediate after-commit publication; current lifecycle scan is shared once per minute and pending-event recovery runs every 15 minutes under CHANGE-083/ADR-026. Credit compact consumer migration remains FEEDBACK-009.
