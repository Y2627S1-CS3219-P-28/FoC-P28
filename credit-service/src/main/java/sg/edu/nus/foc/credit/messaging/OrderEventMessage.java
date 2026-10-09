/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-08
 * Mode: Code generation.
 * Scope: Generated the contract to handle the order service pub/sub events based on the provided requirements contract.
 * Author review: I reviewed for correctness and edited where needed.
 */

package sg.edu.nus.foc.credit.messaging;

import java.time.Instant;

public record OrderEventMessage(
        String eventId,
        String eventType,
        int eventVersion,
        String orderId,
        long orderVersion,
        Instant occurredAt,
        String actorId,
        OrderSnapshot order,
        Boolean overdue,
        Instant overdueAt) {

    public record OrderSnapshot(
            String id,
            String requesterId,
            String courierId,
            String itemDescription,
            String pickupSupplierId,
            String deliverySupplierId,
            long offeredCredits,
            String status,
            Instant createdAt,
            Instant expiresAt,
            int deliveryTimeLimitMinutes,
            long version,
            String originalOrderId,
            String repostedOrderId,
            RepostPlanSnapshot repostPlan) {
    }

    public record RepostPlanSnapshot(
            boolean enabled,
            Instant dueAt,
            long creditAmount,
            int deliveryDurationMinutes,
            boolean used) {
    }
}
