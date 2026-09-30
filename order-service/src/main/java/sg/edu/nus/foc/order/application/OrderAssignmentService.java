package sg.edu.nus.foc.order.application;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderCheckpoint;
import sg.edu.nus.foc.order.infrastructure.CheckpointRepository;
import sg.edu.nus.foc.order.infrastructure.OrderRepository;
import sg.edu.nus.foc.order.infrastructure.CommandReceiptRepository;
import sg.edu.nus.foc.order.domain.CommandReceipt;

@Service
public class OrderAssignmentService {
    private final OrderRepository orders; private final CheckpointRepository checkpoints; private final CommandReceiptRepository receipts; private final UserServicePort users; private final OrderAuditLogger audit;
    public OrderAssignmentService(OrderRepository orders, CheckpointRepository checkpoints, CommandReceiptRepository receipts, UserServicePort users, OrderAuditLogger audit) {
        this.orders = orders; this.checkpoints = checkpoints; this.receipts=receipts; this.users = users; this.audit = audit;
    }
    @Transactional
    public Order accept(String commandId,String id, String courier, long version, String authorization) {
        var previous=receipts.findByOperationAndCommandId("ACCEPT",commandId); if(previous.isPresent()) return orders.findById(previous.get().getOrderId()).orElseThrow();
        String authenticatedCourier = users.verifyCourier(courier, authorization);
        Order order = lock(id);
        order.accept(authenticatedCourier, version, Instant.now());
        checkpoints.save(new OrderCheckpoint(id, order.getStatus(), Instant.now(), authenticatedCourier, null)); Order saved=orders.save(order); receipts.save(new CommandReceipt("ACCEPT",commandId,saved.getId(),Instant.now())); audit.action("ACCEPT", saved.getId(), authenticatedCourier, commandId, "accepted"); return saved;
    }
    private Order lock(String id) { return orders.lockById(id).orElseThrow(() -> sg.edu.nus.foc.order.domain.OrderProblem.notFound("Order not found.")); }
}
