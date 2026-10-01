package sg.edu.nus.foc.order.domain.repository;

import sg.edu.nus.foc.order.domain.OrderCheckpoint;

public interface OrderCheckpointRepository {
    OrderCheckpoint save(OrderCheckpoint checkpoint);
}
