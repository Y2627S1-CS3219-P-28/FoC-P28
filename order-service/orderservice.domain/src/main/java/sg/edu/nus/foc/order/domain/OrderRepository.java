package sg.edu.nus.foc.order.domain;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
public interface OrderRepository extends JpaRepository<Order,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from Order o where o.id = :id")
    Optional<Order> lockById(String id);
}
