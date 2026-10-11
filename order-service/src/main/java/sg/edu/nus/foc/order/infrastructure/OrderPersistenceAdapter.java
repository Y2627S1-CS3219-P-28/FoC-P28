package sg.edu.nus.foc.order.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderCourierAttempt;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.domain.repository.OrderPage;
import sg.edu.nus.foc.order.domain.repository.OrderRepository;

@Repository
@RequiredArgsConstructor
public class OrderPersistenceAdapter implements OrderRepository {
    private final JpaOrderRepository repository;
    private final JpaOrderCourierAttemptRepository attempts;
    private sg.edu.nus.foc.order.application.recovery.OrderCommandStore commands;

    @org.springframework.beans.factory.annotation.Autowired
    public void setCommandStore(sg.edu.nus.foc.order.application.recovery.OrderCommandStore commands) {
        this.commands = commands;
    }

    @Override
    public Optional<Order> get(String id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Order> getForUpdate(String id) {
        Optional<Order> locked = repository.findByIdForUpdate(id);
        if (commands != null) commands.guard(id);
        return locked;
    }

    @Override
    public Optional<Order> getForLifecycleUpdate(String id) {
        Optional<Order> locked = repository.findByIdForLifecycleUpdate(id);
        if (commands != null) commands.guard(id);
        return locked;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findDueUnassignedIds(OrderStatus status, Instant now) {
        return repository.findDueUnassignedIds(status, now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findDueForAutoCompletionIds(Instant deliveredAtOrBefore) {
        return repository.findDueForAutoCompletionIds(OrderStatus.DELIVERED, deliveredAtOrBefore);
    }

    @Override
    public Order save(Order order) {
        if (order.getAttemptId() != null) {
            throw new IllegalArgumentException("Courier history cannot be saved as a current Order.");
        }
        return repository.save(order);
    }

    @Override
    public void saveAbortedAttempt(OrderCourierAttempt attempt) {
        attempts.save(attempt);
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
    public OrderPage findRequestedBy(String requesterId, OrderStatus status, int page, int size) {
        if (status == null) {
            return findRequestedBy(requesterId, page, size);
        }
        return toOrderPage(repository.findRequesterOrdersByStatus(requesterId, status, pageRequest(page, size)));
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public OrderPage findCourierOrders(String courierId, int page, int size) {
        return findCourierOrders(courierId, null, page, size);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public OrderPage findCourierOrders(String courierId, OrderStatus status, int page, int size) {
        PageRequest pageable = pageRequest(page, size);
        Page<CourierOrderReference> result = status == null
                ? repository.findCourierTimeline(courierId, pageable)
                : repository.findCourierTimelineByStatus(courierId, status.name(), pageable);
        List<Order> items = result.getContent().stream()
                .map(reference -> reference.getAttemptId() == null
                        ? repository.findById(reference.getOrderId()).orElseThrow()
                        : Order.historicalAttempt(attempts.findById(reference.getAttemptId()).orElseThrow()))
                .toList();
        return new OrderPage(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
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
