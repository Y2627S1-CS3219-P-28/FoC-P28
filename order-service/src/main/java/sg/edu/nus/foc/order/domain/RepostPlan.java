package sg.edu.nus.foc.order.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
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
    @Column(name = "repost_expires_at")
    private Instant expiresAt;
    private long creditAmount;
    private int deliveryDurationMinutes;
    private boolean used;

    public RepostPlan(boolean enabled, Instant dueAt, long creditAmount, int deliveryDurationMinutes, Instant expiresAt) {
        if (enabled && (dueAt == null || expiresAt == null || !expiresAt.isAfter(dueAt)
                || creditAmount <= 0 || deliveryDurationMinutes < 15)) {
            throw new OrderProblem("VALIDATION_ERROR", "Invalid repost plan.");
        }
        this.enabled = enabled;
        this.dueAt = dueAt;
        this.expiresAt = expiresAt;
        this.creditAmount = creditAmount;
        this.deliveryDurationMinutes = deliveryDurationMinutes;
    }

    public boolean isDueAt(Instant now) {
        return dueAt != null && !dueAt.isAfter(now);
    }

    public void validateAgainst(Instant originalExpiry) {
        if (enabled && (dueAt == null || dueAt.isBefore(originalExpiry) || expiresAt == null
                || !expiresAt.isAfter(dueAt))) {
            throw new OrderProblem("VALIDATION_ERROR",
                    "Repost expiry must be later than repost time, and repost time must be at or after the original expiry.");
        }
    }

    public boolean hasFutureExpiry(Instant now) {
        return expiresAt != null && expiresAt.isAfter(now);
    }

    public void markUsed() {
        used = true;
    }
}
