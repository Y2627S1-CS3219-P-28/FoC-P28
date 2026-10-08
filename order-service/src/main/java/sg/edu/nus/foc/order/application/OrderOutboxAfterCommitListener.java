package sg.edu.nus.foc.order.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderOutboxAfterCommitListener {
    private final OrderOutboxDispatcher dispatcher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatchAfterCommit(OrderOutboxDispatchRequested request) {
        try {
            dispatcher.dispatch(request.eventId());
        } catch (RuntimeException exception) {
            log.error("Order event {} could not be dispatched after commit; cron recovery will retry it.",
                    request.eventId(), exception);
        }
    }
}
