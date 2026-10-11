# Sequence 7: Courier cancels an ACCEPTED order

## Effective compact-event amendment — CHANGE-092 / ADR-031 (2026-10-09)

Yao Xiang approved the exact seven-field Order-only payload: eventId, eventType, orderId, orderStatus, creditAmount, occurredAt, courierId. This replaces full snapshots and overdue facts ONLY for OpenOrderRefundTaskEvent and OrderCompletionTaskEvent. AcceptedOrderCancellationTaskEvent retains its existing v1 envelope/snapshot. Internal outbox versions and Pub/Sub eventVersion attribute remain (compact schema v2); topic names and DB schema unchanged. Old pending snapshot rows normalize at dispatch from their saved facts, with stable IDs. Lifecycle/outbox scheduling, locks, synchronous Credit assignment/reset and ADR-025 abort behavior remain. Historical v1 descriptions below are superseded for these two bodies. Credit currently requires the old snapshot and overdue; FEEDBACK-009 is INCOMPLETE_OR_INCOMPATIBLE. User completion penalties need an agreed separate overdue source. User approved implementing Order-only and documenting peer work; live integration remains blocked, Sprint [~].

Current lifecycle is ADR-025 / CHANGE-082, not the older ADR-014 expired-only penalty diagram. This amendment changes the refund body only; accepted-cancellation remains its existing version-1 schema.

```mermaid
sequenceDiagram
    actor Courier
    participant API as OrderController
    participant Flow as OrderTransitionService
    participant Credit as Credit synchronous reset
    participant DB as PostgreSQL Order/history/outbox
    participant Dispatch as After-commit dispatcher
    participant Penalty as AcceptedOrderCancellationTaskPublisher
    participant Refund as OpenOrderRefundTaskPublisher
    participant Broker as Pub/Sub
    participant User as User consumer
    participant Financial as Credit consumer
    Courier->>API: Cancel assigned ACCEPTED order
    API->>Flow: cancelAccepted(commandId, orderId, actor, version)
    Flow->>DB: Lock current order; validate role, assignment, state, version
    Flow->>Credit: POST hold-for-reopen(orderId), no body
    alt Credit fails
        Credit-->>Flow: Failure
        Note over Flow,DB: Leave ACCEPTED; no history or event committed
    else Credit confirms bodyless 200
        Credit-->>Flow: Reservation held; assignment cleared; no refund
        Flow->>DB: Save immutable ABORTED attempt/checkpoint
        alt Still before expiry after response
            Flow->>DB: Current OPEN; clear courier
        else At/after expiry
            Flow->>DB: Current EXPIRED; clear courier
            Flow->>DB: Queue seven-field OpenOrderRefundTaskEvent
        end
        Flow->>DB: Queue unchanged accepted-cancellation event for every abort
        Flow->>DB: Commit order, history, checkpoint, receipt and event intents atomically
        DB-->>Dispatch: AFTER_COMMIT signals
        Dispatch->>Penalty: publishAcceptedOrderCancellationTask(existing v1 event)
        Penalty->>Broker: Configured accepted-cancellation topic
        Broker-->>User: Every abort penalty (actorId identifies courier)
        opt Current outcome is EXPIRED
            Dispatch->>Refund: publishOpenOrderRefundTask(compact v2 body)
            Refund->>Broker: Configured open-refund topic
            Broker-->>Financial: Refund (compact consumer migration pending)
        end
        Note over Dispatch,DB: Mark each published after broker ACK; otherwise retry durable intent
    end
    Flow-->>API: OrderResponse
    API-->>Courier: OPEN or EXPIRED outcome
```

Assigned courier ownership and NOWAIT locking are unchanged. Reset occurs synchronously for every accepted cancellation before either current outcome. Every abort queues the accepted-cancellation User penalty event with actorId and the existing Order snapshot (courier cleared); only EXPIRED also queues compact refund. No Credit subscriber participates in the User penalty stream. Immediate dispatch and 15-minute outbox recovery remain ADR-026. FEEDBACK-009 blocks real financial integration until Credit accepts the seven-field refund body; accepted payload is unchanged and User consumer verification remains separate. Old full-snapshot refund rows normalize at dispatch, preserving event IDs. No peer reply is awaited after the atomic commit.
