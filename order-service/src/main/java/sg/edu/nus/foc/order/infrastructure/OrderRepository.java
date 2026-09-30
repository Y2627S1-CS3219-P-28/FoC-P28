package sg.edu.nus.foc.order.infrastructure;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByStatusOrderByCreatedAtAsc(OrderStatus status, Pageable pageable);
    Page<Order> findByStatusAndExpiresAtAfterOrderByCreatedAtAsc(OrderStatus status, Instant now, Pageable pageable);

    Page<Order> findByRequesterIdOrderByCreatedAtDesc(String requesterId, Pageable pageable);

    Page<Order> findByCourierIdOrderByCreatedAtDesc(String courierId, Pageable pageable);

    List<Order> findByStatusAndExpiresAtLessThanEqualAndCourierIdIsNull(OrderStatus status, Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> lockById(@Param("id") String id);
}
