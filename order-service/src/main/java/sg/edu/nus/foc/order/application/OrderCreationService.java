package sg.edu.nus.foc.order.application;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.foc.order.domain.CommandReceipt;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderCheckpoint;
import sg.edu.nus.foc.order.domain.RepostPlan;
import sg.edu.nus.foc.order.domain.repository.CommandReceiptRepository;
import sg.edu.nus.foc.order.domain.repository.OrderCheckpointRepository;
import sg.edu.nus.foc.order.domain.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderCreationService {
    private final OrderRepository orders;
    private final CommandReceiptRepository receipts;
    private final OrderCheckpointRepository checkpoints;
    private final UserServicePort users;
    private final SupplierServicePort suppliers;
    private final CreditServicePort credits;
    private final OrderAuditLogger audit;

    @Transactional
    public Order create(
            String commandId,
            String requesterId,
            String description,
            String pickup,
            String delivery,
            long amount,
            int duration,
            Instant expiresAt,
            RepostPlan repostPlan,
            String authorization) {
        Optional<CommandReceipt> previous = receipts.findExisting(
                "CREATE",
                commandId);
        if (previous.isPresent()) {
            return orders.get(previous.get().getOrderId()).orElseThrow();
        }

        String authenticatedRequester = users.verifyRequester(requesterId, authorization);
        suppliers.validatePair(pickup, delivery, authorization);

        Order order = Order.open(
                authenticatedRequester,
                description,
                pickup,
                delivery,
                amount,
                duration,
                Instant.now(),
                expiresAt,
                repostPlan);

        credits.reserve(order.getId(), authenticatedRequester, amount, authorization);
        audit.dependency("credit-service", "reserve", order.getId(), "accepted");

        Order saved = orders.save(order);
        OrderCheckpoint checkpoint = new OrderCheckpoint(
                saved.getId(),
                saved.getStatus(),
                saved.getCreatedAt(),
                authenticatedRequester,
                null);
        checkpoints.save(checkpoint);
        receipts.save(new CommandReceipt("CREATE", commandId, saved.getId(), Instant.now()));
        audit.action("CREATE", saved.getId(), authenticatedRequester, commandId, "accepted");
        return saved;
    }
}
