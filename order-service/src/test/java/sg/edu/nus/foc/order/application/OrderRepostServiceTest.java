package sg.edu.nus.foc.order.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import sg.edu.nus.foc.order.domain.OrderProblem;
import sg.edu.nus.foc.order.domain.RepostPlan;
import sg.edu.nus.foc.order.domain.repository.CommandReceiptRepository;
import sg.edu.nus.foc.order.domain.repository.OrderCheckpointRepository;
import sg.edu.nus.foc.order.domain.repository.OrderRepository;

class OrderRepostServiceTest {
    @Test
    void configureRejectsPostCreationChanges() {
        OrderRepository orders = mock(OrderRepository.class);
        OrderCheckpointRepository checkpoints = mock(OrderCheckpointRepository.class);
        CommandReceiptRepository receipts = mock(CommandReceiptRepository.class);
        SupplierServicePort suppliers = mock(SupplierServicePort.class);
        CreditServicePort credits = mock(CreditServicePort.class);
        UserServicePort users = mock(UserServicePort.class);
        OrderAuditLogger audit = mock(OrderAuditLogger.class);
        Instant now = Instant.parse("2026-10-01T00:00:00Z");
        assertThrows(OrderProblem.class, () -> new OrderRepostService(orders, checkpoints, receipts, suppliers, credits, users, audit)
            .configure("cmd", "order-id", "requester", 0,
                new RepostPlan(true, now.plusSeconds(7200), 6, 30), "Bearer token"));
    }
}
