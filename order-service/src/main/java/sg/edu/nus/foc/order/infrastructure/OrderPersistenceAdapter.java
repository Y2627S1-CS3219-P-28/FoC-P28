package sg.edu.nus.foc.order.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.domain.repository.OrderPage;
import sg.edu.nus.foc.order.domain.repository.OrderRepository;

@Repository
@RequiredArgsConstructor
public class OrderPersistenceAdapter implements OrderRepository {
    private final JpaOrderRepository repository;

    @Override
    public Optional<Order> get(String id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Order> getForUpdate(String id) {
        return repository.findByIdForUpdate(id);
    }

    @Override
    public Order save(Order order) {
        return repository.save(order);
    }

    @Override
    public OrderPage findAvailable(Instant now, int page, int size) {
        Page<Order> result = repository.findByStatusAndExpiresAtAfterOrderByCreatedAtAsc(
                OrderStatus.OPEN,
                now,
                pageRequest(page, size));
        return toOrderPage(result);
    }

    @Override
    public OrderPage findRequestedBy(String requesterId, int page, int size) {
        Page<Order> result = repository.findByRequesterIdOrderByCreatedAtDesc(
                requesterId,
                pageRequest(page, size));
        return toOrderPage(result);
    }

    @Override
    public OrderPage findCourierOrders(String courierId, int page, int size) {
        Page<Order> result = repository.findByCourierIdOrderByCreatedAtDesc(
                courierId,
                pageRequest(page, size));
        return toOrderPage(result);
    }

    @Override
    public OrderPage findAllOrders(OrderStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> result = status == null
                ? repository.findAll(pageable)
                : repository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return toOrderPage(result);
    }

    @Override
    public List<Order> findDueUnassigned(OrderStatus status, Instant now) {
        return repository.findByStatusAndExpiresAtLessThanEqualAndCourierIdIsNull(status, now);
    }

    @Override
    public List<Order> findDueForAutoCompletion(Instant deliveredAtOrBefore) {
        return repository.findDueForAutoCompletion(OrderStatus.DELIVERED, deliveredAtOrBefore);
    }

    private PageRequest pageRequest(int page, int size) {
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(pageNumber, pageSize);
    }

    private OrderPage toOrderPage(Page<Order> result) {
        return new OrderPage(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
