package sg.edu.nus.foc.order.messagingpublisher.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sg.edu.nus.foc.order.domain.OrderStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventSnapshot {
    private String id;
    private String requesterId;
    private String courierId;
    private String itemDescription;
    private String pickupSupplierId;
    private String deliverySupplierId;
    private long offeredCredits;
    private OrderStatus status;
    private Instant createdAt;
    private Instant expiresAt;
    private int deliveryTimeLimitMinutes;
    private long version;
    private String originalOrderId;
    private String repostedOrderId;
    private RepostPlanEventSnapshot repostPlan;
    private List<OrderCheckpointEventSnapshot> checkpoints;
}
