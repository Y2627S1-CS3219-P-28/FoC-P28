package sg.edu.nus.foc.order.infrastructure;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderStatus;

public interface JpaOrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByStatusAndExpiresAtAfterOrderByCreatedAtAsc(
            OrderStatus status,
            Instant now,
            Pageable pageable);

    Page<Order> findByRequesterIdOrderByCreatedAtDesc(String requesterId, Pageable pageable);

    Page<Order> findByCourierIdOrderByCreatedAtDesc(String courierId, Pageable pageable);

    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Order> findByStatusAndExpiresAtLessThanEqualAndCourierIdIsNull(
            OrderStatus status,
            Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("select o from Order o, OrderCheckpoint c "
            + "where c.orderId = o.id "
            + "and o.status = :status "
            + "and c.status = :status "
            + "and c.occurredAt <= :deliveredAtOrBefore")
    List<Order> findDueForAutoCompletion(
            @Param("status") OrderStatus status,
            @Param("deliveredAtOrBefore") Instant deliveredAtOrBefore);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") String id);
}
