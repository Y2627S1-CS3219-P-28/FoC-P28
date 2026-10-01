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
public class OrderAssignmentService {
    private final OrderRepository orders;
    private final OrderCheckpointRepository checkpoints;
    private final CommandReceiptRepository receipts;
    private final UserServicePort users;
    private final OrderAuditLogger audit;

    @Transactional
    public Order accept(
            String commandId,
            String id,
            String courier,
            long version,
            String authorization) {
        Optional<CommandReceipt> previous = receipts.findExisting(
                "ACCEPT",
                commandId);
        if (previous.isPresent()) {
            return orders.get(previous.get().getOrderId()).orElseThrow();
        }

        String authenticatedCourier = users.verifyCourier(courier, authorization);
        Order order = findForUpdate(id);
        Instant acceptedAt = Instant.now();
        order.accept(authenticatedCourier, version, acceptedAt);

        checkpoints.save(new OrderCheckpoint(
                id,
                order.getStatus(),
                acceptedAt,
                authenticatedCourier,
                null));

        Order saved = orders.save(order);
        receipts.save(new CommandReceipt("ACCEPT", commandId, saved.getId(), Instant.now()));
        audit.action("ACCEPT", saved.getId(), authenticatedCourier, commandId, "accepted");
        return saved;
    }

    private Order findForUpdate(String id) {
        return orders.getForUpdate(id)
                .orElseThrow(() -> OrderProblem.notFound("Order not found."));
    }
}
