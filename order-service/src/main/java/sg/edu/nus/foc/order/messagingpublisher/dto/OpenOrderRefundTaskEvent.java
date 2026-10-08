package sg.edu.nus.foc.order.messagingpublisher.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenOrderRefundTaskEvent implements OrderTaskEvent {
    private String eventId;
    private String eventType;
    private int eventVersion;
    private String orderId;
    private long orderVersion;
    private Instant occurredAt;
    private String actorId;
    private OrderEventSnapshot order;
}
