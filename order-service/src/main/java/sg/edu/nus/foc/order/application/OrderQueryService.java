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

    public OrderPage requestedBy(String requesterId, OrderStatus status, int page, int size) {
        return orders.findRequestedBy(requesterId, status, page, size);
    }

    public OrderPage courierFor(String courierId, OrderStatus status, int page, int size) {
        return orders.findCourierOrders(courierId, status, page, size);
    }

    public OrderPage allOrders(OrderStatus status, int page, int size) {
        return orders.findAllOrders(status, page, size);
    }

    public OrderPage allOrders(OrderStatus status, String orderId, Instant createdFrom, Instant createdBefore, int page, int size) {
        validateCreatedRange(createdFrom, createdBefore);
        if (orderId != null && orderId.length() > 128) {
            throw new OrderProblem("VALIDATION_ERROR", "Order ID must not exceed 128 characters.");
        }
        return orders.findAllOrders(status, orderId == null || orderId.isBlank() ? null : orderId.strip(),
                createdFrom, createdBefore, page, size);
    }

    public static void validateCreatedRange(Instant from, Instant before) {
        if (from != null && before != null && !from.isBefore(before)) {
            throw new OrderProblem("VALIDATION_ERROR", "createdFrom must be before createdBefore.");
        }
    }
}
