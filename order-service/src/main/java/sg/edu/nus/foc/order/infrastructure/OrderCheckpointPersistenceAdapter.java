package sg.edu.nus.foc.order.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import sg.edu.nus.foc.order.domain.OrderCheckpoint;
import sg.edu.nus.foc.order.domain.repository.OrderCheckpointRepository;

@Repository
@RequiredArgsConstructor
public class OrderCheckpointPersistenceAdapter implements OrderCheckpointRepository {
    private final JpaOrderCheckpointRepository repository;

    @Override
    public OrderCheckpoint save(OrderCheckpoint checkpoint) {
        return repository.save(checkpoint);
    }
}
