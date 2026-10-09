package sg.edu.nus.foc.credit.credit;

import java.time.Instant;

public record CreditOutcomeEvent(
        String eventId,
        CreditOutcomeType type,
        int eventVersion,
        String orderId,
        long orderVersion,
        Instant occurredAt,
        String actorId,
        String requesterId,
        String courierId,
        long offeredCredits,
        String orderStatus,
        boolean overdue,
        Instant overdueAt) {
}
