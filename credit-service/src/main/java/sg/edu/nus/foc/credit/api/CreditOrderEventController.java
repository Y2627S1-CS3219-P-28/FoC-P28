/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-08
 * Mode: Code generation.
 * Scope: Generated the controller to handle the order service pub/sub events based on the provided requirements contract.
 * Author review: I reviewed for correctness and edited where needed.
 */

package sg.edu.nus.foc.credit.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import sg.edu.nus.foc.credit.error.InvalidOrderEventException;
import sg.edu.nus.foc.credit.messaging.OrderEventPayloadConsumer;

@RestController
public class CreditOrderEventController {

    public static final String PUSH_PATH = "/api/credits/internal/order-events";

    private final OrderEventPayloadConsumer consumer;

    public CreditOrderEventController(OrderEventPayloadConsumer consumer) {
        this.consumer = consumer;
    }

    @PostMapping(PUSH_PATH)
    @Operation(summary = "Process an authenticated Order Service Pub/Sub event")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Event committed or replayed idempotently"),
            @ApiResponse(responseCode = "400", description = "Push envelope or Order event is invalid"),
            @ApiResponse(responseCode = "401", description = "Pub/Sub push identity is invalid"),
            @ApiResponse(responseCode = "409", description = "Event conflicts with persisted credit state"),
            @ApiResponse(responseCode = "503", description = "Credit persistence is unavailable")
    })
    public ResponseEntity<Void> receive(@Valid @RequestBody PubSubPushEnvelope envelope) {
        consumer.consume(envelope.subscription(), decode(envelope.message().data()));
        return ResponseEntity.noContent().build();
    }

    private static String decode(String encoded) {
        try {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new InvalidOrderEventException("Pub/Sub message data is not valid Base64.");
        }
    }
}
