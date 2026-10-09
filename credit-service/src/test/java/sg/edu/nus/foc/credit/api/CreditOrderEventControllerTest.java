/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-08
 * Mode: Test generation.
 * Scope: Generate tests based on the provided scope.
 * Author review: I reviewed for correctness and edited where needed.
 */

package sg.edu.nus.foc.credit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import sg.edu.nus.foc.credit.error.InvalidOrderEventException;

class CreditOrderEventControllerTest {

    @Test
    void decodesAndProcessesThePushEnvelopeBeforeAcknowledging() {
        AtomicReference<String> subscription = new AtomicReference<>();
        AtomicReference<String> payload = new AtomicReference<>();
        CreditOrderEventController controller = new CreditOrderEventController((source, body) -> {
            subscription.set(source);
            payload.set(body);
        });
        String encoded = Base64.getEncoder().encodeToString("event-json".getBytes(StandardCharsets.UTF_8));

        assertThat(controller.receive(new PubSubPushEnvelope(
                new PubSubPushEnvelope.Message(encoded, "message-1"), "subscription")).getStatusCode().value())
                .isEqualTo(204);
        assertThat(subscription.get()).isEqualTo("subscription");
        assertThat(payload.get()).isEqualTo("event-json");
    }

    @Test
    void rejectsInvalidBase64WithoutCallingTheConsumer() {
        CreditOrderEventController controller = new CreditOrderEventController(
                (subscription, payload) -> { throw new AssertionError("must not be called"); });

        assertThatThrownBy(() -> controller.receive(new PubSubPushEnvelope(
                new PubSubPushEnvelope.Message("%%%", "message-1"), "subscription")))
                .isInstanceOf(InvalidOrderEventException.class);
    }

    @Test
    void propagatesProcessingFailureSoPubSubRetries() {
        CreditOrderEventController controller = new CreditOrderEventController(
                (subscription, payload) -> { throw new IllegalStateException("database unavailable"); });
        String encoded = Base64.getEncoder().encodeToString("event-json".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> controller.receive(new PubSubPushEnvelope(
                new PubSubPushEnvelope.Message(encoded, "message-1"), "subscription")))
                .isInstanceOf(IllegalStateException.class);
    }
}
