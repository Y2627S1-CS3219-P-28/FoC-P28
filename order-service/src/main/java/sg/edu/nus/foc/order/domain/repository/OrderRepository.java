package sg.edu.nus.foc.order.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderStatus;

public interface OrderRepository {
    Optional<Order> get(String id);

    Optional<Order> getForUpdate(String id);

    Order save(Order order);

    OrderPage findAvailable(Instant now, int page, int size);

    OrderPage findRequestedBy(String requesterId, int page, int size);

    OrderPage findCourierOrders(String courierId, int page, int size);

    List<Order> findDueUnassigned(OrderStatus status, Instant now);
}
