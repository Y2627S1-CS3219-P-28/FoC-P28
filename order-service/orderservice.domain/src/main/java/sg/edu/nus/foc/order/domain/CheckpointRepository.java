package sg.edu.nus.foc.order.domain;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CheckpointRepository extends JpaRepository<Checkpoint,String> {
    List<Checkpoint> findByOrderIdOrderByOccurredAtAscIdAsc(String orderId);
}
