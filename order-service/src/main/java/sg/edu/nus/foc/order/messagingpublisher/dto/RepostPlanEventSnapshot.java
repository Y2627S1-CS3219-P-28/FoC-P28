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
public class RepostPlanEventSnapshot {
    private boolean enabled;
    private Instant dueAt;
    private long creditAmount;
    private int deliveryDurationMinutes;
    private boolean used;
}
