# Sequence 6: Cancel or expire an OPEN order

```mermaid
sequenceDiagram
    actor Requester
    participant Controller as OrderController
    participant Transition as OrderTransitionService
    participant Scheduler as Spring OrderExpiryScheduler
    participant Lifecycle as LifecycleProcessingService
    participant Store as PostgreSQL transaction
    participant Outbox as Order event outbox
    participant Listener as After-commit listener
    participant Relay as OrderOutboxDispatcher
    participant RefundInterface as IOpenOrderRefundTaskPublisher
    participant RefundPublisher as OpenOrderRefundTaskPublisher
    participant Transport as Google Cloud Pub/Sub
    participant Credit as Credit Service consumer (future peer work)

    alt Requester-triggered cancellation
        Requester->>Controller: Cancel own OPEN order
        Controller->>Transition: cancelOpen(orderId, actor, expectedVersion)
        Transition->>Transition: Verify requester and OPEN state
        Transition->>Transition: Set CANCELLED and create checkpoint
        Transition->>Outbox: Store OpenOrderRefundTaskEvent (status=CANCELLED)
        Transition->>Store: Save Order, checkpoint, receipt, and outbox row
    else Scheduler-triggered expiration
        Scheduler->>Lifecycle: expireDue(now), configured Spring cron
        Lifecycle->>Store: Find due, unassigned OPEN orders with row locks
        Store-->>Lifecycle: Due orders
        loop Each due order
            Lifecycle->>Lifecycle: Set EXPIRED and create expiry checkpoint
            Lifecycle->>Outbox: Store OpenOrderRefundTaskEvent (status=EXPIRED)
            Lifecycle->>Store: Save Order, checkpoint, and outbox row
        end
    end
    Store-->>Listener: Transaction committed
    Listener->>Relay: Dispatch committed event now
    Relay->>Outbox: Claim row with lease
    Relay->>RefundInterface: publishOpenOrderRefundTask(event)
    RefundInterface->>RefundPublisher: Publish typed event
    RefundPublisher->>Transport: Send using shared OPEN-refund topic
    alt Transport confirms publish
        Transport-->>Relay: Publish accepted
        Relay->>Outbox: Mark PUBLISHED
        Transport-->>Credit: Deliver event asynchronously
        Credit->>Credit: Refund/release transaction using orderId
    else Publish fails
        Transport-->>Relay: Publication error
        Relay->>Outbox: Record retry time and error
        Note over Outbox,Relay: Existing Spring cron recovery retries the durable pending event
    end
    opt Requester-triggered command
        Transition-->>Controller: Cancellation result
        Controller-->>Requester: OrderResponse
    end
```

Requester cancellation is trigger-based and records `CANCELLED`; scheduled expiry records `EXPIRED`. Both persist one `OpenOrderRefundTaskEvent` with the full resulting Order/repost snapshot but no checkpoint history. Credit subscribes to one shared OPEN-refund topic and refunds/releases the transaction for either status; no User Service subscriber is involved. The `order.status` field lets Credit distinguish the outcome if it needs to. State, checkpoint, and event intent commit atomically; publication happens after commit through one typed publisher. Order Service does not await Credit's subscriber. Delivery is at least once and Credit must deduplicate by stable event ID. The production topic remains a placeholder for later configuration; local Compose initializes `open-order-refund-v1`. The dispatcher also translates pending legacy cancellation/expiration outbox records to the new event shape while preserving their event IDs. Cloud Run scale-to-zero with request-based CPU does not guarantee this in-process schedule runs while idle.
