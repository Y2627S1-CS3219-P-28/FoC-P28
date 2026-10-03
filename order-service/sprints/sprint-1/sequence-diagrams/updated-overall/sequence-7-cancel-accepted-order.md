# Sequence 7: Courier aborts an ACCEPTED order

```mermaid
sequenceDiagram
    actor Courier
    participant Controller as OrderController
    participant Transition as OrderTransitionService
    participant Store as PostgreSQL transaction
    participant Outbox as Order event outbox
    participant Listener as After-commit listener
    participant Relay as OrderOutboxDispatcher
    participant Interface as IAcceptedOrderCancellationTaskPublisher
    participant Publisher as AcceptedOrderCancellationTaskPublisher
    participant Transport as Google Cloud Pub/Sub
    participant Credit as Credit Service consumer (future peer work)
    participant User as User Service consumer (future peer work)

    Courier->>Controller: Cancel assigned ACCEPTED order
    Controller->>Transition: POST /{id}/cancel-accepted(orderId, actor, expectedVersion)
    Transition->>Transition: Verify courier eligibility, authenticated courier owns assignment, ACCEPTED state, and expected version
    Transition->>Transition: Set ABORTED, clear courier, and create checkpoint
    Transition->>Outbox: Store full resulting Order event
    Transition->>Store: Save Order, checkpoint, receipt, and outbox row
    Store-->>Transition: Commit all rows atomically
    Store-->>Listener: Transaction committed
    Listener->>Relay: Dispatch cancellation event now
    Relay->>Outbox: Claim row with lease
    Relay->>Interface: publishAcceptedOrderCancellationTask(event)
    Interface->>Publisher: publish typed event
    Publisher->>Transport: Send using placeholder topic
    alt Transport confirms publish
        Transport-->>Publisher: Publish accepted
        Publisher-->>Relay: Success
        Relay->>Outbox: Mark PUBLISHED
        par Independent consumers; no reply awaited
            Transport-->>Credit: Deliver event asynchronously
        and
            Transport-->>User: Deliver event asynchronously
        end
    else Publish fails
        Transport-->>Publisher: Failure
        Publisher-->>Relay: Publication error
        Relay->>Outbox: Record retry time and error
        Note over Outbox,Relay: Cron recovery retries the durable pending event
    end
    Transition-->>Controller: Cancellation result
    Controller-->>Courier: OrderResponse
```

The authenticated courier whose user ID matches the order's assigned `courierId` is the only actor who may abort an accepted order. The requester and other couriers are forbidden. The resulting `ABORTED` state, cleared courier assignment, checkpoint, receipt, and event intent commit atomically. Immediate dispatch follows commit; cron retries due rows. Delivery is at least once, so consumers deduplicate by stable event ID. Consumers are assumed future peer work for this Order-only change; their replies are not awaited. Same-order reopening is excluded from Sprint 1 by ADR-001. The topic ID remains `TODO_TOPIC` until configured.
