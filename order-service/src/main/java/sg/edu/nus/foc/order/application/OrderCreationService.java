package sg.edu.nus.foc.order.application;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.infrastructure.CheckpointRepository;
import sg.edu.nus.foc.order.infrastructure.OrderRepository;
import sg.edu.nus.foc.order.infrastructure.CommandReceiptRepository;
import sg.edu.nus.foc.order.domain.CommandReceipt;

@Service
public class OrderCreationService {
    private final OrderRepository orders; private final CommandReceiptRepository receipts;
    private final CheckpointRepository checkpoints;
    private final UserServicePort users;
    private final SupplierServicePort suppliers;
    private final CreditServicePort credits;

    public OrderCreationService(OrderRepository orders, CommandReceiptRepository receipts, CheckpointRepository checkpoints,
                                 UserServicePort users, SupplierServicePort suppliers, CreditServicePort credits) {
        this.orders = orders; this.receipts=receipts; this.checkpoints = checkpoints; this.users = users;
        this.suppliers = suppliers; this.credits = credits;
    }

    @Transactional
    public Order create(String commandId, String requesterId, String description, String pickup, String delivery,
                        long amount, int duration, Instant expiresAt, String authorization) {
        var previous=receipts.findByOperationAndCommandId("CREATE",commandId);
        if(previous.isPresent()) return orders.findById(previous.get().getOrderId()).orElseThrow();
        String authenticatedRequester = users.verifyRequester(requesterId, authorization);
        suppliers.validatePair(pickup, delivery, authorization);
        Order order = Order.open(authenticatedRequester, description, pickup, delivery, amount, duration, Instant.now(), expiresAt);
        credits.reserve(order.getId(), authenticatedRequester, amount, authorization);
        Order saved = orders.save(order);
        checkpoints.save(new sg.edu.nus.foc.order.domain.OrderCheckpoint(saved.getId(), saved.getStatus(), saved.getCreatedAt(), authenticatedRequester, null));
        receipts.save(new CommandReceipt("CREATE",commandId,saved.getId(),Instant.now()));
        return saved;
    }
}
