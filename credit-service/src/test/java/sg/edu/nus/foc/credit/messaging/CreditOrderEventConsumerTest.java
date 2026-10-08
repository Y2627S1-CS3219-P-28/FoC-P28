/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-08
 * Mode: Test generation.
 * Scope: Generate tests based on the provided scope.
 * Author review: I reviewed for correctness and edited where needed.
 */

package sg.edu.nus.foc.credit.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import sg.edu.nus.foc.credit.config.CreditPushProperties;
import sg.edu.nus.foc.credit.credit.CreditOutcomeType;
import sg.edu.nus.foc.credit.credit.CreditOutcomeEvent;
import sg.edu.nus.foc.credit.error.InvalidOrderEventException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class CreditOrderEventConsumerTest {

    private final AtomicReference<CreditOutcomeEvent> processed = new AtomicReference<>();
    private final CreditPushProperties properties = new CreditPushProperties(
            "project", "audience", "push@example.com", "completion", "refund", "cancellation");
    private final CreditOrderEventConsumer consumer = new CreditOrderEventConsumer(
            JsonMapper.builder().findAndAddModules().build(), processed::set, properties);

    @Test
    void mapsTheFinalOrderCompletionContract(CapturedOutput output) {
        consumer.consume(properties.completionSubscriptionPath(),
                eventJson("OrderCompletionTaskEvent", "COMPLETED", "courier-1", 4));

        assertThat(processed.get()).satisfies(event -> {
            assertThat(event.type()).isEqualTo(CreditOutcomeType.ORDER_COMPLETION);
            assertThat(event.orderId()).isEqualTo("order-1");
            assertThat(event.orderVersion()).isEqualTo(4);
            assertThat(event.requesterId()).isEqualTo("requester-1");
            assertThat(event.courierId()).isEqualTo("courier-1");
            assertThat(event.offeredCredits()).isEqualTo(10);
        });
        assertThat(output)
                .contains("order_event_received")
                .contains("order_event_processed");
    }

    @Test
    void mapsBothRefundEventContracts() {
        consumer.consume(properties.openRefundSubscriptionPath(),
                eventJson("OpenOrderRefundTaskEvent", "EXPIRED", null, 4));
        assertThat(processed.get().type()).isEqualTo(CreditOutcomeType.OPEN_ORDER_REFUND);

        consumer.consume(properties.acceptedCancellationSubscriptionPath(),
                eventJson("AcceptedOrderCancellationTaskEvent", "ABORTED", null, 4));
        assertThat(processed.get().type()).isEqualTo(CreditOutcomeType.ACCEPTED_ORDER_CANCELLATION);
    }

    @Test
    void rejectsUnknownTypesAndMismatchedSnapshots() {
        assertThatThrownBy(() -> consumer.consume(properties.completionSubscriptionPath(),
                eventJson("UnknownEvent", "COMPLETED", "courier-1", 4)))
                .isInstanceOf(InvalidOrderEventException.class);
        assertThatThrownBy(() -> consumer.consume(properties.openRefundSubscriptionPath(),
                eventJson("OpenOrderRefundTaskEvent", "CANCELLED", null, 5)))
                .isInstanceOf(InvalidOrderEventException.class);
    }

    @Test
    void requiresCompletionOnlyFactsOnlyForCompletionEvents() {
        String completionWithoutOverdue = eventJson(
                "OrderCompletionTaskEvent", "COMPLETED", "courier-1", 4)
                .replace("\"overdue\":false,", "");

        assertThatThrownBy(() -> consumer.consume(
                properties.completionSubscriptionPath(), completionWithoutOverdue))
                .isInstanceOf(InvalidOrderEventException.class)
                .hasMessageContaining("overdue");

        String refundWithoutCompletionFacts = eventJson(
                "OpenOrderRefundTaskEvent", "CANCELLED", null, 4)
                .replace("\"overdue\":false,", "");
        consumer.consume(properties.openRefundSubscriptionPath(), refundWithoutCompletionFacts);
        assertThat(processed.get().type()).isEqualTo(CreditOutcomeType.OPEN_ORDER_REFUND);
    }

    @Test
    void rejectsAnEventDeliveredByTheWrongSubscription(CapturedOutput output) {
        assertThatThrownBy(() -> consumer.consume(properties.completionSubscriptionPath(),
                eventJson("OpenOrderRefundTaskEvent", "CANCELLED", null, 4)))
                .isInstanceOf(InvalidOrderEventException.class)
                .hasMessageContaining("does not match");
        assertThat(output).contains("order_event_processing_failed");
    }

    private String eventJson(String eventType, String status, String courierId, long snapshotVersion) {
        String courier = courierId == null ? "null" : "\"" + courierId + "\"";
        return """
                {
                  "eventId":"event-1",
                  "eventType":"%s",
                  "eventVersion":1,
                  "orderId":"order-1",
                  "orderVersion":4,
                  "occurredAt":"2026-10-08T08:00:00Z",
                  "actorId":"requester-1",
                  "order":{
                    "id":"order-1",
                    "requesterId":"requester-1",
                    "courierId":%s,
                    "itemDescription":"Parcel",
                    "pickupSupplierId":"s1",
                    "deliverySupplierId":"s2",
                    "offeredCredits":10,
                    "status":"%s",
                    "createdAt":"2026-10-08T07:00:00Z",
                    "expiresAt":"2026-10-08T09:00:00Z",
                    "deliveryTimeLimitMinutes":60,
                    "version":%d,
                    "originalOrderId":null,
                    "repostedOrderId":null,
                    "repostPlan":null
                  },
                  "overdue":false,
                  "overdueAt":null
                }
                """.formatted(eventType, courier, status, snapshotVersion);
    }
}
