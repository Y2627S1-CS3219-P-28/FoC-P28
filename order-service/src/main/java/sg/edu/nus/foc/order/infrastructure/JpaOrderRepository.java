package sg.edu.nus.foc.order.infrastructure;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderStatus;

public interface JpaOrderRepository extends JpaRepository<Order, UUID> {
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findById(@Param("id") String id);

    @Query(value = "select id as \"orderId\", cast(null as uuid) as \"attemptId\", created_at as \"visibleAt\" "
            + "from orders where courier_id = :courierId "
            + "union all select order_id as \"orderId\", id as \"attemptId\", aborted_at as \"visibleAt\" "
            + "from order_courier_attempts where courier_id = :courierId "
            + "order by \"visibleAt\" desc, \"orderId\", \"attemptId\"",
            countQuery = "select (select count(*) from orders where courier_id = :courierId) "
                    + "+ (select count(*) from order_courier_attempts where courier_id = :courierId)",
            nativeQuery = true)
    Page<CourierOrderReference> findCourierTimeline(@Param("courierId") String courierId, Pageable pageable);
    Page<Order> findByStatusAndExpiresAtAfterOrderByCreatedAtAsc(
            OrderStatus status,
            Instant now,
            Pageable pageable);

    @Query(value = "select o from Order o where o.requesterId = :requesterId "
            + "and (o.status <> sg.edu.nus.foc.order.domain.OrderStatus.EXPIRED "
            + "or o.repostedOrderId is null) order by o.createdAt desc",
            countQuery = "select count(o) from Order o where o.requesterId = :requesterId "
                    + "and (o.status <> sg.edu.nus.foc.order.domain.OrderStatus.EXPIRED "
                    + "or o.repostedOrderId is null)")
    Page<Order> findByRequesterIdOrderByCreatedAtDesc(
            @Param("requesterId") String requesterId,
            Pageable pageable);

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
