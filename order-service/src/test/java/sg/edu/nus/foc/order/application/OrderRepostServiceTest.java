package sg.edu.nus.foc.order.application;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.RepostPlan;
import sg.edu.nus.foc.order.infrastructure.CheckpointRepository;
import sg.edu.nus.foc.order.infrastructure.CommandReceiptRepository;
import sg.edu.nus.foc.order.infrastructure.OrderRepository;

class OrderRepostServiceTest {
    @Test
    void configureVerifiesRequesterWithForwardedAuthorization() {
        OrderRepository orders = mock(OrderRepository.class);
        CheckpointRepository checkpoints = mock(CheckpointRepository.class);
        CommandReceiptRepository receipts = mock(CommandReceiptRepository.class);
        SupplierServicePort suppliers = mock(SupplierServicePort.class);
        CreditServicePort credits = mock(CreditServicePort.class);
        UserServicePort users = mock(UserServicePort.class);
        Instant now = Instant.parse("2026-10-01T00:00:00Z");
        Order order = Order.open("requester", "parcel", "pickup", "delivery", 5, 30, now, now.plusSeconds(3600));
        when(receipts.findByOperationAndCommandId("CONFIGURE_REPOST", "cmd")).thenReturn(Optional.empty());
        when(orders.lockById(order.getId())).thenReturn(Optional.of(order));
        when(orders.save(order)).thenReturn(order);

        new OrderRepostService(orders, checkpoints, receipts, suppliers, credits, users)
            .configure("cmd", order.getId(), "requester", 0,
                new RepostPlan(true, now.plusSeconds(7200), 6, 30), "Bearer token");

        verify(users).verifyRequester("requester", "Bearer token");
        verify(receipts).save(any());
    }
}
