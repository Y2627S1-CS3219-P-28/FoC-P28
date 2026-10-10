package sg.edu.nus.foc.credit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import sg.edu.nus.foc.credit.config.CreditPushProperties;
import sg.edu.nus.foc.credit.error.InvalidOrderEventException;
import sg.edu.nus.foc.credit.messaging.OrderEventMessage;

class CreditOrderEventControllerTest {

    private static final CreditPushProperties PROPERTIES = new CreditPushProperties(
            "project", "audience", "push@example.com", "completion", "refund", "cancellation");

    @Test
    void eachEndpointInvokesOnlyItsDesignatedHandler() {
        AtomicReference<String> expectedSubscription = new AtomicReference<>();
        AtomicReference<String> expectedEventType = new AtomicReference<>();
        AtomicInteger openRefundCalls = new AtomicInteger();
        AtomicInteger acceptedCancellationCalls = new AtomicInteger();
        AtomicInteger completionCalls = new AtomicInteger();
        CreditOrderEventController controller = new CreditOrderEventController(
                (subscription, payload, expectedSource, expectedType) -> {
                    expectedSubscription.set(expectedSource);
                    expectedEventType.set(expectedType);
                    return message(expectedType);
                },
                PROPERTIES,
                ignored -> openRefundCalls.incrementAndGet(),
                ignored -> acceptedCancellationCalls.incrementAndGet(),
                ignored -> completionCalls.incrementAndGet());

        assertThat(controller.receiveOpenRefund(envelope(PROPERTIES.openRefundSubscriptionPath()))
                .getStatusCode().value()).isEqualTo(204);
        assertThat(expectedSubscription.get()).isEqualTo(PROPERTIES.openRefundSubscriptionPath());
        assertThat(expectedEventType.get()).isEqualTo("OpenOrderRefundTaskEvent");

        assertThat(controller.receiveAcceptedCancellation(
                envelope(PROPERTIES.acceptedCancellationSubscriptionPath()))
                .getStatusCode().value()).isEqualTo(204);
        assertThat(expectedSubscription.get()).isEqualTo(
                PROPERTIES.acceptedCancellationSubscriptionPath());
        assertThat(expectedEventType.get()).isEqualTo("AcceptedOrderCancellationTaskEvent");

        assertThat(controller.receiveCompletion(envelope(PROPERTIES.completionSubscriptionPath()))
                .getStatusCode().value()).isEqualTo(204);
        assertThat(expectedSubscription.get()).isEqualTo(PROPERTIES.completionSubscriptionPath());
        assertThat(expectedEventType.get()).isEqualTo("OrderCompletionTaskEvent");
        assertThat(openRefundCalls).hasValue(1);
        assertThat(acceptedCancellationCalls).hasValue(1);
        assertThat(completionCalls).hasValue(1);
    }

    @Test
    void rejectsInvalidBase64WithoutCallingTheDecoderOrHandler() {
        CreditOrderEventController controller = new CreditOrderEventController(
                (subscription, payload, expectedSource, expectedType) -> {
                    throw new AssertionError("decoder must not be called");
                },
                PROPERTIES,
                ignored -> { throw new AssertionError("handler must not be called"); },
                ignored -> { throw new AssertionError("handler must not be called"); },
                ignored -> { throw new AssertionError("handler must not be called"); });
        PubSubPushEnvelope envelope = new PubSubPushEnvelope(
                new PubSubPushEnvelope.Message("%%%", "message-1"),
                PROPERTIES.openRefundSubscriptionPath());

        assertThatThrownBy(() -> controller.receiveOpenRefund(envelope))
                .isInstanceOf(InvalidOrderEventException.class);
    }

    @Test
    void propagatesHandlerFailureSoPubSubRetries() {
        CreditOrderEventController controller = new CreditOrderEventController(
                (subscription, payload, expectedSource, expectedType) -> message(expectedType),
                PROPERTIES,
                ignored -> { },
                ignored -> { },
                ignored -> { throw new IllegalStateException("database unavailable"); });

        assertThatThrownBy(() -> controller.receiveCompletion(
                envelope(PROPERTIES.completionSubscriptionPath())))
                .isInstanceOf(IllegalStateException.class);
    }

    private static PubSubPushEnvelope envelope(String subscription) {
        String encoded = Base64.getEncoder().encodeToString(
                "event-json".getBytes(StandardCharsets.UTF_8));
        return new PubSubPushEnvelope(
                new PubSubPushEnvelope.Message(encoded, "message-1"), subscription);
    }

    private static OrderEventMessage message(String eventType) {
        Instant now = Instant.parse("2026-10-08T08:00:00Z");
        return new OrderEventMessage(
                "event-1", eventType, 1, "order-1", 4, now, "actor-1",
                new OrderEventMessage.OrderSnapshot(
                        "order-1", "requester-1", "courier-1", "Parcel", "s1", "s2",
                        10, "COMPLETED", now, now.plusSeconds(3600), 60, 4,
                        null, null, null),
                false, null);
    }
}
