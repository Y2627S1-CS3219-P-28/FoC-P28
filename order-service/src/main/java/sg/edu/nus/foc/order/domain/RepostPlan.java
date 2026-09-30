package sg.edu.nus.foc.order.domain;

import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class RepostPlan {
    private boolean enabled;
    private Instant dueAt;
    private long creditAmount;
    private int deliveryDurationMinutes;
    private boolean used;

    protected RepostPlan() {}

    public RepostPlan(boolean enabled, Instant dueAt, long creditAmount, int deliveryDurationMinutes) {
        if (enabled && (dueAt == null || creditAmount <= 0 || deliveryDurationMinutes < 15)) {
            throw new OrderProblem("VALIDATION_ERROR", "Invalid repost plan.");
        }
        this.enabled = enabled;
        this.dueAt = dueAt;
        this.creditAmount = creditAmount;
        this.deliveryDurationMinutes = deliveryDurationMinutes;
    }

    public boolean enabled() { return enabled; }
    public Instant dueAt() { return dueAt; }
    public long creditAmount() { return creditAmount; }
    public int deliveryDurationMinutes() { return deliveryDurationMinutes; }
    public boolean used() { return used; }
    public boolean dueAt(Instant now) { return dueAt != null && !dueAt.isAfter(now); }
    public void markUsed() { used = true; }
}
