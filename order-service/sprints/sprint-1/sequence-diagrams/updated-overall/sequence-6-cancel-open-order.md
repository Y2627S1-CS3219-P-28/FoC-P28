# Sequence 6: Cancel an OPEN order

```mermaid
sequenceDiagram
    actor Requester
    participant Controller as OrderController
    participant Transition as OrderTransitionService
    participant Store as PostgreSQL transaction
    participant Outbox as Order event outbox
    participant Listener as After-commit listener
    participant Relay as OrderOutboxDispatcher
    participant Interface as IOpenOrderCancellationTaskPublisher
    participant Publisher as OpenOrderCancellationTaskPublisher
    participant Transport as Google Cloud Pub/Sub
    participant Credit as Credit Service consumer (future peer work)

    Requester->>Controller: Cancel own OPEN order
    Controller->>Transition: cancelOpen(orderId, actor, expectedVersion)
    Transition->>Transition: Verify requester and OPEN state
    Transition->>Transition: Set CANCELLED and create resulting checkpoint
    Transition->>Outbox: Store full resulting Order event
    Transition->>Store: Save Order, checkpoint, receipt, and outbox row
    Store-->>Transition: Commit all rows atomically
    Store-->>Listener: Transaction committed
    Listener->>Relay: Dispatch cancellation event now
    Relay->>Outbox: Claim row with lease
    Relay->>Interface: publishOpenOrderCancellationTask(event)
    Interface->>Publisher: publish typed event
    Publisher->>Transport: Send using placeholder topic
    alt Transport confirms publish
        Transport-->>Publisher: Publish accepted
        Publisher-->>Relay: Success
        Relay->>Outbox: Mark PUBLISHED
        Transport-->>Credit: Deliver event asynchronously
    else Publish fails
        Transport-->>Publisher: Failure
        Publisher-->>Relay: Publication error
        Relay->>Outbox: Record retry time and error
        Note over Outbox,Relay: Cron recovery retries the durable pending event
    end
    Transition-->>Controller: Cancellation result
    Controller-->>Requester: OrderResponse
```

The Order state, checkpoint, receipt, and cancellation event intent commit atomically. Credit's consumer reply is not awaited. Immediate publish follows commit; cron retries a failed or interrupted attempt. Delivery is at least once and Credit must deduplicate by stable event ID. OPEN expiry is a separate existing lifecycle path and is not covered by this cancellation publisher. The topic ID remains `TODO_TOPIC` until configured.
