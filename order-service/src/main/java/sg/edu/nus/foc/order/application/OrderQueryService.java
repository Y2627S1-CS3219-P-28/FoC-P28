package sg.edu.nus.foc.order.application;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderProblem;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.domain.repository.OrderPage;
import sg.edu.nus.foc.order.domain.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderQueryService {
    private final OrderRepository orders;

    public Order get(String id) {
        return orders.get(id)
                .orElseThrow(() -> OrderProblem.notFound("Order not found."));
    }

    public OrderPage available(int page, int size) {
        return orders.findAvailable(Instant.now(), page, size);
    }

    public OrderPage requestedBy(String requesterId, int page, int size) {
        return orders.findRequestedBy(requesterId, page, size);
    }

    public OrderPage courierFor(String courierId, int page, int size) {
        return orders.findCourierOrders(courierId, page, size);
    }

    public OrderPage allOrders(OrderStatus status, int page, int size) {
        return orders.findAllOrders(status, page, size);
    }
}
