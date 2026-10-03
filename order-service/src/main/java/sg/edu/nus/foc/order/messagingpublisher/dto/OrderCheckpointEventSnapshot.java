package sg.edu.nus.foc.order.messagingpublisher.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sg.edu.nus.foc.order.domain.OrderStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCheckpointEventSnapshot {
    private String id;
    private OrderStatus status;
    private Instant occurredAt;
    private String actorId;
    private String supplierId;
}
