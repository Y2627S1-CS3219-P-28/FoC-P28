package sg.edu.nus.foc.order.domain;

import jakarta.persistence.Embeddable;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RepostPlan {
    private boolean enabled;
    private Instant dueAt;
    private long creditAmount;
    private int deliveryDurationMinutes;
    private boolean used;

    public RepostPlan(boolean enabled, Instant dueAt, long creditAmount, int deliveryDurationMinutes) {
        if (enabled && (dueAt == null || creditAmount <= 0 || deliveryDurationMinutes < 15)) {
            throw new OrderProblem("VALIDATION_ERROR", "Invalid repost plan.");
        }
        this.enabled = enabled;
        this.dueAt = dueAt;
        this.creditAmount = creditAmount;
        this.deliveryDurationMinutes = deliveryDurationMinutes;
    }

    public boolean isDueAt(Instant now) {
        return dueAt != null && !dueAt.isAfter(now);
    }

    public void markUsed() {
        used = true;
    }
}
