package sg.edu.nus.foc.credit.messaging;

import sg.edu.nus.foc.credit.credit.CreditOutcomeEvent;
import sg.edu.nus.foc.credit.credit.CreditOutcomeType;

final class CreditOutcomeEventMapper {

    private CreditOutcomeEventMapper() {
    }

    static CreditOutcomeEvent map(OrderEventMessage message, CreditOutcomeType type) {
        return new CreditOutcomeEvent(
                message.eventId(), type, message.eventVersion(), message.orderId(),
                message.orderVersion(), message.occurredAt(), message.actorId(),
                message.order().requesterId(), message.order().courierId(),
                message.order().offeredCredits(), message.order().status(),
                Boolean.TRUE.equals(message.overdue()), message.overdueAt());
    }
}
