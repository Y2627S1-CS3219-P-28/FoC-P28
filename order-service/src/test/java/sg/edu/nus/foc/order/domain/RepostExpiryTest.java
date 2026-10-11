package sg.edu.nus.foc.order.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RepostExpiryTest {
    private static final Instant START = Instant.parse("2026-10-09T10:00:00Z");
    private static final Instant ORIGINAL_EXPIRY = START.plusSeconds(3600);

    private RepostPlan plan(Instant due, Instant expiry) {
        return new RepostPlan(true, due, 2, 15, expiry);
    }

    @Test
    void autoExpiryIsExclusiveAndLateExecutionWithinWindowIsAllowed() {
        Instant expiry = ORIGINAL_EXPIRY.plusSeconds(3600);
        Order order = Order.open("owner", "item", "pickup", "delivery", 1, 15,
                START, ORIGINAL_EXPIRY, plan(ORIGINAL_EXPIRY, expiry));
        order.expire(0, ORIGINAL_EXPIRY);
        assertTrue(order.eligibleForAutomaticRepost(expiry.minusSeconds(1)));
        assertFalse(order.eligibleForAutomaticRepost(expiry));
        assertFalse(order.eligibleForAutomaticRepost(expiry.plusSeconds(1)));
    }

    @Test
    void dueCannotPrecedeOriginalExpiry() {
        assertThrows(OrderProblem.class, () -> Order.open("owner", "item", "pickup", "delivery", 1, 15,
                START, ORIGINAL_EXPIRY, plan(ORIGINAL_EXPIRY.minusSeconds(1), ORIGINAL_EXPIRY.plusSeconds(3600))));
    }

    @Test
    void planExpiryMustBePresentAndStrictlyAfterDue() {
        assertThrows(OrderProblem.class, () -> Order.open("owner", "item", "pickup", "delivery", 1, 15,
                START, ORIGINAL_EXPIRY, plan(ORIGINAL_EXPIRY, null)));
        assertThrows(OrderProblem.class, () -> Order.open("owner", "item", "pickup", "delivery", 1, 15,
                START, ORIGINAL_EXPIRY, plan(ORIGINAL_EXPIRY, ORIGINAL_EXPIRY)));
    }
}
