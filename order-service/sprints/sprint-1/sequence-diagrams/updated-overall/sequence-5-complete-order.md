# Sequence 5: Complete an order

```mermaid
sequenceDiagram
    actor Requester
    participant Controller as OrderController
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

    Requester->>Controller: Complete delivered order
    Controller->>Transition: complete(orderId, actor, expectedVersion)
    Transition->>Transition: Verify requester and DELIVERED state
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
    Transition-->>Controller: Completion result
    Controller-->>Requester: OrderResponse
```

The same `OrderCompletionTaskEvent` is published for every completion. It includes the complete resulting Order snapshot, `overdue`, and `overdueAt` (the delivery deadline). Credit Service transfers/settles the reserved credits to the courier. User Service applies the overdue penalty when `overdue` is true; otherwise it reduces the courier's penalty score under its existing policy. Overdue is true only when delivery occurred strictly after the deadline. The Order state and event intent commit atomically. Immediate delivery is attempted after commit, and cron recovers failed or interrupted attempts. A crash after Pub/Sub accepts the message but before the outbox marker commits can cause a duplicate; consumers deduplicate using stable `eventId`. Peer replies are not awaited. The topic ID remains `TODO_TOPIC` until configured.
