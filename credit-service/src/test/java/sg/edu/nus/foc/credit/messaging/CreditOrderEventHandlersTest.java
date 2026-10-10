package sg.edu.nus.foc.credit.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import sg.edu.nus.foc.credit.credit.CreditOutcomeEvent;
import sg.edu.nus.foc.credit.credit.CreditOutcomeType;
import sg.edu.nus.foc.credit.error.InvalidOrderEventException;

class CreditOrderEventHandlersTest {

    private final AtomicReference<CreditOutcomeEvent> processed = new AtomicReference<>();

    @Test
    void eachHandlerMapsToItsFixedOutcomeType() {
        new CreditOpenOrderRefundEventHandler(processed::set).handle(
                message("OpenOrderRefundTaskEvent", "CANCELLED", null, null));
        assertThat(processed.get().type()).isEqualTo(CreditOutcomeType.OPEN_ORDER_REFUND);

        new CreditAcceptedOrderCancellationEventHandler(processed::set).handle(
                message("AcceptedOrderCancellationTaskEvent", "ABORTED", null, null));
        assertThat(processed.get().type())
                .isEqualTo(CreditOutcomeType.ACCEPTED_ORDER_CANCELLATION);

        new CreditOrderCompletionEventHandler(processed::set).handle(
                message("OrderCompletionTaskEvent", "COMPLETED", "courier-1", false));
        assertThat(processed.get().type()).isEqualTo(CreditOutcomeType.ORDER_COMPLETION);
        assertThat(processed.get().courierId()).isEqualTo("courier-1");
    }

    @Test
    void completionHandlerRejectsMissingCompletionFacts() {
        CreditOrderCompletionEventHandler handler = new CreditOrderCompletionEventHandler(
                ignored -> { throw new AssertionError("processor must not be called"); });

        assertThatThrownBy(() -> handler.handle(
                message("OrderCompletionTaskEvent", "COMPLETED", "courier-1", null)))
                .isInstanceOf(InvalidOrderEventException.class)
                .hasMessageContaining("overdue");
    }

    private static OrderEventMessage message(String eventType, String status,
                                             String courierId, Boolean overdue) {
        Instant now = Instant.parse("2026-10-08T08:00:00Z");
        return new OrderEventMessage(
                "event-1", eventType, 1, "order-1", 4, now, "requester-1",
                new OrderEventMessage.OrderSnapshot(
                        "order-1", "requester-1", courierId, "Parcel", "s1", "s2",
                        10, status, now, now.plusSeconds(3600), 60, 4,
                        null, null, null),
                overdue, null);
    }
}
