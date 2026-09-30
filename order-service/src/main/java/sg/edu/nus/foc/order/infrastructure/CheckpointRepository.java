package sg.edu.nus.foc.order.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import sg.edu.nus.foc.order.domain.OrderCheckpoint;

public interface CheckpointRepository extends JpaRepository<OrderCheckpoint, String> { }
