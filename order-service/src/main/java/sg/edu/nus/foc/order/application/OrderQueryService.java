package sg.edu.nus.foc.order.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import org.springframework.stereotype.Service;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderProblem;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.infrastructure.OrderRepository;

@Service
public class OrderQueryService {
    private final OrderRepository orders;
    public OrderQueryService(OrderRepository orders) { this.orders=orders; }
    public Order get(String id) { return orders.findById(id).orElseThrow(() -> OrderProblem.notFound("Order not found.")); }
    public Page<Order> available(int page,int size) { return orders.findByStatusAndExpiresAtAfterOrderByCreatedAtAsc(OrderStatus.OPEN, Instant.now(), PageRequest.of(Math.max(page,0), Math.min(Math.max(size,1),100))); }
}
