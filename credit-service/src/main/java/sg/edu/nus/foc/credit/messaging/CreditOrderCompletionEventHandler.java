package sg.edu.nus.foc.credit.messaging;

import org.springframework.stereotype.Component;
import sg.edu.nus.foc.credit.credit.CreditOutcomeProcessor;
import sg.edu.nus.foc.credit.credit.CreditOutcomeType;
import sg.edu.nus.foc.credit.error.InvalidOrderEventException;

@Component
public class CreditOrderCompletionEventHandler implements OrderCompletionEventHandler {

    private final CreditOutcomeProcessor processor;

    public CreditOrderCompletionEventHandler(CreditOutcomeProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void handle(OrderEventMessage message) {
        if (message.overdue() == null) {
            throw new InvalidOrderEventException("Completion event is missing its overdue fact.");
        }
        processor.processOutcome(CreditOutcomeEventMapper.map(
                message, CreditOutcomeType.ORDER_COMPLETION));
    }
}
