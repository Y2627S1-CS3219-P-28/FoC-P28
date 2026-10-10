package sg.edu.nus.foc.credit.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import sg.edu.nus.foc.credit.error.InvalidOrderEventException;
import tools.jackson.databind.json.JsonMapper;

class JsonOrderEventPayloadDecoderTest {

    private static final String SUBSCRIPTION = "projects/project/subscriptions/completion";

    private final JsonOrderEventPayloadDecoder decoder = new JsonOrderEventPayloadDecoder(
            JsonMapper.builder().findAndAddModules().build());

    @Test
    void decodesTheExpectedEventWithoutChoosingItsHandler() {
        OrderEventMessage message = decoder.decode(
                SUBSCRIPTION,
                eventJson("OrderCompletionTaskEvent", "COMPLETED", "courier-1", 4),
                SUBSCRIPTION,
                "OrderCompletionTaskEvent");

        assertThat(message.eventId()).isEqualTo("event-1");
        assertThat(message.orderId()).isEqualTo("order-1");
        assertThat(message.order().requesterId()).isEqualTo("requester-1");
        assertThat(message.order().courierId()).isEqualTo("courier-1");
    }

    @Test
    void rejectsWrongSubscriptionTypeAndSnapshot() {
        String completion = eventJson(
                "OrderCompletionTaskEvent", "COMPLETED", "courier-1", 4);

        assertThatThrownBy(() -> decoder.decode(
                "projects/project/subscriptions/refund", completion,
                SUBSCRIPTION, "OrderCompletionTaskEvent"))
                .isInstanceOf(InvalidOrderEventException.class)
                .hasMessageContaining("subscription");
        assertThatThrownBy(() -> decoder.decode(
                SUBSCRIPTION, completion, SUBSCRIPTION, "OpenOrderRefundTaskEvent"))
                .isInstanceOf(InvalidOrderEventException.class)
                .hasMessageContaining("endpoint");
        assertThatThrownBy(() -> decoder.decode(
                SUBSCRIPTION,
                eventJson("OrderCompletionTaskEvent", "COMPLETED", "courier-1", 5),
                SUBSCRIPTION,
                "OrderCompletionTaskEvent"))
                .isInstanceOf(InvalidOrderEventException.class)
                .hasMessageContaining("snapshot");
    }

    static String eventJson(String eventType, String status, String courierId,
                            long snapshotVersion) {
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
