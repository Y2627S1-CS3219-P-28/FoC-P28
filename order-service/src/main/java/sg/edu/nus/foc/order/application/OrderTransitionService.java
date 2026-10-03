package sg.edu.nus.foc.order.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.foc.order.domain.CommandReceipt;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderCheckpoint;
import sg.edu.nus.foc.order.domain.OrderProblem;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.domain.repository.CommandReceiptRepository;
import sg.edu.nus.foc.order.domain.repository.OrderCheckpointRepository;
import sg.edu.nus.foc.order.domain.repository.OrderRepository;
import sg.edu.nus.foc.order.domain.repository.OrderEventOutboxRepository;
import sg.edu.nus.foc.order.messagingpublisher.dto.AcceptedOrderCancellationTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderCompletionTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OpenOrderCancellationTaskEvent;

@Service
@RequiredArgsConstructor
public class OrderTransitionService {
    private final OrderRepository orders;
    private final OrderCheckpointRepository checkpoints;
    private final CommandReceiptRepository receipts;
    private final UserServicePort users;
    private final OrderEventOutboxRepository outbox;
    private final ApplicationEventPublisher applicationEvents;
    private final OrderTaskEventFactory eventFactory;
    private final OrderAuditLogger audit;

    @Transactional
    public Order start(String commandId, String id, String actor, long version, String authorization) {
        return courier("START", commandId, id, actor, version, authorization, Order::start);
    }

    @Transactional
    public Order pickup(String commandId, String id, String actor, long version, String authorization) {
        return courier("PICKUP", commandId, id, actor, version, authorization, Order::markPickedUp);
    }

    @Transactional
    public Order deliver(String commandId, String id, String actor, long version, String authorization) {
        return courier("DELIVER", commandId, id, actor, version, authorization, Order::markDelivered);
    }

    @Transactional
    public Order complete(String commandId, String id, String actor, long version, String authorization) {
        String authenticatedActor = users.verifyRequester(actor, authorization);
        Optional<CommandReceipt> previous = receipts.findExisting("COMPLETE", commandId);
        if (previous.isPresent()) {
            return orders.get(previous.get().getOrderId()).orElseThrow();
        }

        Order order = findForUpdate(id);
        order.validateCompletion(authenticatedActor, version);
        List<OrderCheckpoint> history = checkpoints.findByOrderId(order.getId());
        Instant acceptedAt = checkpointTime(history, OrderStatus.ACCEPTED);
        Instant deliveredAt = checkpointTime(history, OrderStatus.DELIVERED);
        Instant overdueAt = acceptedAt.plusSeconds(order.getDeliveryTimeLimitMinutes() * 60L);
        Instant completedAt = Instant.now();
        OrderCheckpoint completedCheckpoint = new OrderCheckpoint(
                order.getId(),
                OrderStatus.COMPLETED,
                completedAt,
                authenticatedActor,
                null);

        boolean overdue = deliveredAt.isAfter(overdueAt);
        order.confirmCompletion(authenticatedActor, version);
        checkpoints.save(completedCheckpoint);
        Order saved = orders.save(order);
        receipts.save(new CommandReceipt("COMPLETE", commandId, saved.getId(), completedAt));

        OrderCompletionTaskEvent event = eventFactory.completion(
                commandId,
                saved,
                authenticatedActor,
                completedAt,
                overdue,
                overdueAt);
        outbox.enqueue(event);
        applicationEvents.publishEvent(new OrderOutboxDispatchRequested(event.getEventId()));
        audit.action("COMPLETE", saved.getId(), authenticatedActor, commandId, "accepted");
        return saved;
    }

    @Transactional
    public Order cancel(String commandId, String id, String actor, long version, String authorization) {
        String authenticatedActor = users.verifyRequester(actor, authorization);
        Optional<CommandReceipt> previous = receipts.findExisting("CANCEL", commandId);
        if (previous.isPresent()) {
            return orders.get(previous.get().getOrderId()).orElseThrow();
        }

        Order order = findForUpdate(id);
        order.validateOpenCancellation(authenticatedActor, version);
        Instant cancelledAt = Instant.now();
        order.cancelOpen(authenticatedActor, version);
        checkpoints.save(new OrderCheckpoint(
                order.getId(),
                OrderStatus.CANCELLED,
                cancelledAt,
                authenticatedActor,
                null));
        Order saved = orders.save(order);
        receipts.save(new CommandReceipt("CANCEL", commandId, saved.getId(), cancelledAt));
        OpenOrderCancellationTaskEvent event = eventFactory.openCancellation(
                commandId, saved, authenticatedActor, cancelledAt);
        outbox.enqueue(event);
        applicationEvents.publishEvent(new OrderOutboxDispatchRequested(event.getEventId()));
        audit.action("CANCEL", saved.getId(), authenticatedActor, commandId, "accepted");
        return saved;
    }

    @Transactional
    public Order cancelAccepted(String commandId, String id, String actor, long version, String authorization) {
        String authenticatedActor = users.verifyCourier(actor, authorization);
        Optional<CommandReceipt> previous = receipts.findExisting("CANCEL_ACCEPTED", commandId);
        if (previous.isPresent()) {
            return orders.get(previous.get().getOrderId()).orElseThrow();
        }

        Order order = findForUpdate(id);
        order.validateAcceptedCancellation(authenticatedActor, version);
        Instant cancelledAt = Instant.now();
        order.cancelAccepted(authenticatedActor, version);
        checkpoints.save(new OrderCheckpoint(
                order.getId(),
                OrderStatus.ABORTED,
                cancelledAt,
                authenticatedActor,
                null));
        Order saved = orders.save(order);
        receipts.save(new CommandReceipt("CANCEL_ACCEPTED", commandId, saved.getId(), cancelledAt));
        AcceptedOrderCancellationTaskEvent event = eventFactory.acceptedCancellation(
                commandId, saved, authenticatedActor, cancelledAt);
        outbox.enqueue(event);
        applicationEvents.publishEvent(new OrderOutboxDispatchRequested(event.getEventId()));
        audit.action("CANCEL_ACCEPTED", saved.getId(), authenticatedActor, commandId, "accepted");
        return saved;
    }

    private Order courier(
            String operation,
            String commandId,
            String id,
            String actor,
            long version,
            String authorization,
            OrderTransition action) {
        String authenticatedActor = users.verifyCourier(actor, authorization);
        return apply(operation, commandId, id, authenticatedActor, version, action, true);
    }

    private Order apply(
            String operation,
            String commandId,
            String id,
            String actor,
            long version,
            OrderTransition action,
            boolean checkpointRequired) {
        Optional<CommandReceipt> previous = receipts.findExisting(operation, commandId);
        if (previous.isPresent()) {
            return orders.get(previous.get().getOrderId()).orElseThrow();
        }

        Order order = findForUpdate(id);
        action.apply(order, actor, version);

        if (checkpointRequired) {
            checkpoints.save(new OrderCheckpoint(
                    order.getId(),
                    order.getStatus(),
                    Instant.now(),
                    actor,
                    null));
        }

        Order saved = orders.save(order);
        receipts.save(new CommandReceipt(operation, commandId, saved.getId(), Instant.now()));
        return saved;
    }

    private Instant checkpointTime(List<OrderCheckpoint> history, OrderStatus status) {
        return history.stream()
                .filter(checkpoint -> checkpoint.getStatus() == status)
                .map(OrderCheckpoint::getOccurredAt)
                .findFirst()
                .orElseThrow(() -> OrderProblem.conflict("Order checkpoint history is incomplete."));
    }

    private Order findForUpdate(String id) {
        return orders.getForUpdate(id)
                .orElseThrow(() -> OrderProblem.notFound("Order not found."));
    }

    @FunctionalInterface
    private interface OrderTransition {
        void apply(Order order, String actor, Long version);
    }
}
