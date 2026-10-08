# Sequence 7: Courier cancels an ACCEPTED order

```mermaid
sequenceDiagram
    actor Courier
    participant Controller as OrderController
    participant Transition as OrderTransitionService
    participant CreditPort as CreditServicePort
    participant Credit as Credit Service hold endpoint
    participant Store as PostgreSQL transaction
    participant Outbox as Order event outbox
    participant Listener as After-commit listener
    participant Relay as OrderOutboxDispatcher
    participant Interface as IAcceptedOrderCancellationTaskPublisher
    participant Publisher as AcceptedOrderCancellationTaskPublisher
    participant Transport as Google Cloud Pub/Sub
    participant CreditConsumer as Credit Service consumer (future peer work)
    participant User as User Service consumer (future peer work)

    Courier->>Controller: Cancel assigned ACCEPTED order
    Controller->>Transition: POST /{id}/cancel-accepted(orderId, actor, expectedVersion)
    Transition->>Transition: Verify courier identity, assignment, ACCEPTED state, and version
    alt Current time is before expiresAt
        Transition->>CreditPort: holdForReopen(orderId)
        CreditPort->>Credit: POST /api/credits/orders/{id}/hold-for-reopen
        alt Credit confirms hold
            Credit-->>CreditPort: 200 OK; transaction held, not refunded
            CreditPort-->>Transition: Success confirmed
            Transition->>Transition: Recheck expiresAt after Credit response
            alt Still before expiresAt
                Transition->>Transition: Set OPEN, clear courier, create OPEN checkpoint
                Transition->>Store: Save Order, checkpoint, and receipt (no event)
                Store-->>Transition: Commit
            else Expired during the Credit call
                Transition->>Transition: Set ABORTED, clear courier, create checkpoint
                Transition->>Outbox: Store full resulting cancellation event
                Transition->>Store: Save Order, checkpoint, receipt, and outbox row
                Store-->>Transition: Commit all rows atomically
                Store-->>Listener: Transaction committed
                Listener->>Relay: Dispatch expired cancellation event
                Relay->>Outbox: Claim row with lease
                Relay->>Interface: publishAcceptedOrderCancellationTask(event)
                Interface->>Publisher: publish typed event
                Publisher->>Transport: Send using placeholder topic
                Publisher-->>Relay: Publish result
                Relay->>Outbox: Mark published or schedule retry
                par Subscribers act independently; no replies awaited
                    Transport-->>CreditConsumer: Refund reservation
                and
                    Transport-->>User: Apply courier cancellation penalty
                end
            end
        else Credit rejects or is unavailable
            Credit-->>CreditPort: Error
            CreditPort-->>Transition: Failure
            Note over Transition,Store: Leave the Order ACCEPTED; do not write checkpoint, receipt, or cancellation event
        end
    else Already expired
        Transition->>Transition: Set ABORTED, clear courier, create checkpoint
        Transition->>Outbox: Store full resulting cancellation event
        Transition->>Store: Save Order, checkpoint, receipt, and outbox row
        Store-->>Transition: Commit all rows atomically
        Store-->>Listener: Transaction committed
        Listener->>Relay: Dispatch expired cancellation event
        Relay->>Outbox: Claim row with lease
        Relay->>Interface: publishAcceptedOrderCancellationTask(event)
        Interface->>Publisher: publish typed event
        Publisher->>Transport: Send using placeholder topic
        Publisher-->>Relay: Publish result
        Relay->>Outbox: Mark published or schedule retry
        par Subscribers act independently; no replies awaited
            Transport-->>CreditConsumer: Refund reservation
        and
            Transport-->>User: Apply courier cancellation penalty
        end
    end
    Transition-->>Controller: Cancellation result
    Controller-->>Courier: OrderResponse
```

Only the authenticated courier assigned to the accepted errand may cancel it. Before expiry, Order waits for Credit's synchronous hold confirmation and then changes `ACCEPTED` directly to `OPEN`, clearing the courier; this branch publishes no cancellation event. Credit failure leaves the order accepted. At or after expiry, Order changes it to `ABORTED` and commits an accepted-cancellation event in the outbox. Credit refunds the transaction and User applies the courier's penalty. If expiry passes during the hold request, Order rechecks the deadline and uses the expired branch. The direct `ACCEPTED -> OPEN` transition is distinct from the prohibited `ABORTED -> OPEN` transition. Consumers deduplicate at-least-once deliveries by stable event ID. The topic ID remains `TODO_TOPIC` until configured.
