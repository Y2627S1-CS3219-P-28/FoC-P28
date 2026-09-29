package sg.edu.nus.foc.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckpointRepository extends JpaRepository<Checkpoint, String> {
    List<Checkpoint> findByOrderIdOrderByOccurredAtAscIdAsc(String orderId);
}
