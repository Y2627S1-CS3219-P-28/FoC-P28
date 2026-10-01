package sg.edu.nus.foc.order.application;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.foc.order.domain.CommandReceipt;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderCheckpoint;
import sg.edu.nus.foc.order.domain.OrderProblem;
import sg.edu.nus.foc.order.domain.repository.CommandReceiptRepository;
import sg.edu.nus.foc.order.domain.repository.OrderCheckpointRepository;
import sg.edu.nus.foc.order.domain.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderTransitionService {
    private final OrderRepository orders;
    private final OrderCheckpointRepository checkpoints;
    private final CommandReceiptRepository receipts;
    private final UserServicePort users;
    private final CreditServicePort credits;
    private final OrderAuditLogger audit;

    @Transactional
    public Order start(String commandId, String id, String actor, long version, String authorization) {
        return courier(
                "START",
                commandId,
                id,
                actor,
                version,
                authorization,
                Order::start);
    }

    @Transactional
    public Order pickup(String commandId, String id, String actor, long version, String authorization) {
        return courier(
                "PICKUP",
                commandId,
                id,
                actor,
                version,
                authorization,
                Order::markPickedUp);
    }

    @Transactional
    public Order deliver(String commandId, String id, String actor, long version, String authorization) {
        return courier(
                "DELIVER",
                commandId,
                id,
                actor,
                version,
                authorization,
                Order::markDelivered);
    }

    @Transactional
    public Order complete(String commandId, String id, String actor, long version, String authorization) {
        Optional<CommandReceipt> previous = receipts.findExisting(
                "COMPLETE",
                commandId);
        if (previous.isPresent()) {
            return orders.get(previous.get().getOrderId()).orElseThrow();
        }

        String authenticatedActor = users.verifyRequester(actor, authorization);
        Order order = findForUpdate(id);
        order.confirmCompletion(authenticatedActor, version);

        credits.settle(
                commandId,
                order.getId(),
                order.getRequesterId(),
                order.getCourierId(),
                order.getOfferedCredits(),
                version,
                authorization);
        audit.dependency("credit-service", "settle", order.getId(), "accepted");

        Order saved = orders.save(order);
        receipts.save(new CommandReceipt("COMPLETE", commandId, saved.getId(), Instant.now()));
        audit.action("COMPLETE", saved.getId(), authenticatedActor, commandId, "accepted");
        return saved;
    }

    @Transactional
    public Order cancel(String commandId, String id, String actor, long version, String authorization) {
        Optional<CommandReceipt> previous = receipts.findExisting(
                "CANCEL",
                commandId);
        if (previous.isPresent()) {
            return orders.get(previous.get().getOrderId()).orElseThrow();
        }

        String authenticatedActor = users.verifyRequester(actor, authorization);
        Order order = findForUpdate(id);
        order.cancelOpen(authenticatedActor, version);

        credits.release(
                commandId,
                order.getId(),
                order.getRequesterId(),
                order.getOfferedCredits(),
                "CANCELLED",
                version,
                authorization);
        audit.dependency("credit-service", "release", order.getId(), "accepted");

        Order saved = orders.save(order);
        receipts.save(new CommandReceipt("CANCEL", commandId, saved.getId(), Instant.now()));
        audit.action("CANCEL", saved.getId(), authenticatedActor, commandId, "accepted");
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
        Optional<CommandReceipt> previous = receipts.findExisting(
                operation,
                commandId);
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

    private Order findForUpdate(String id) {
        return orders.getForUpdate(id)
                .orElseThrow(() -> OrderProblem.notFound("Order not found."));
    }

    @FunctionalInterface
    private interface OrderTransition {
        void apply(Order order, String actor, Long version);
    }
}
