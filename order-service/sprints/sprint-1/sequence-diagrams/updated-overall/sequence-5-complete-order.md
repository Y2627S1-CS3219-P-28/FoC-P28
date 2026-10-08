# Sequence 5: Complete an order

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
    participant Credit as Credit Service consumer (future peer work)
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
    Transition->>Outbox: Store full resulting Order event with overdue facts
    Transition->>Store: Save Order, checkpoint, receipt, and outbox row
    Store-->>Transition: Commit all rows atomically
    Store-->>Listener: Transaction committed
    Listener->>Relay: Dispatch completion event now
    Relay->>Outbox: Claim row with lease
    Relay->>Interface: publishOrderCompletionTask(event)
    Interface->>Publisher: publish typed event
    Publisher->>Transport: Send using placeholder topic
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

The same `OrderCompletionTaskEvent` is published for every completion, whether requester-confirmed or automatically completed at least 48 hours after the `DELIVERED` checkpoint. It includes the complete resulting Order snapshot, `overdue`, and `overdueAt` (the delivery deadline). Credit Service transfers/settles the reserved credits to the courier. User Service applies the overdue penalty when `overdue` is true; otherwise it reduces the courier's penalty score under its existing policy. Overdue is true only when delivery occurred strictly after the deadline. The Order state and event intent commit atomically. Immediate delivery is attempted after commit, and cron recovers failed or interrupted attempts. A crash after Pub/Sub accepts the message but before the outbox marker commits can cause a duplicate; consumers deduplicate using stable `eventId`. Peer replies are not awaited. The auto-completion cron is configurable and defaults to once per minute; the event topic remains `TODO_TOPIC` until configured.
