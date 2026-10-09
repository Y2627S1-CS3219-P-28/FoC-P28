/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-08
 * Mode: Code generation.
 * Scope: Generated the consumer to handle the order service pub/sub events based on the provided requirements contract.
 * Author review: I reviewed for correctness and edited where needed.
 */

package sg.edu.nus.foc.credit.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import sg.edu.nus.foc.credit.config.CreditPushProperties;
import sg.edu.nus.foc.credit.credit.CreditOutcomeEvent;
import sg.edu.nus.foc.credit.credit.CreditOutcomeType;
import sg.edu.nus.foc.credit.credit.CreditOutcomeProcessor;
import sg.edu.nus.foc.credit.error.InvalidOrderEventException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class CreditOrderEventConsumer implements OrderEventPayloadConsumer {

    private static final Logger log = LoggerFactory.getLogger(CreditOrderEventConsumer.class);

    private final JsonMapper mapper;
    private final CreditOutcomeProcessor processor;
    private final CreditPushProperties properties;

    public CreditOrderEventConsumer(JsonMapper mapper, CreditOutcomeProcessor processor,
                                    CreditPushProperties properties) {
        this.mapper = mapper;
        this.processor = processor;
        this.properties = properties;
    }

    @Override
    public void consume(String subscription, String payload) {
        OrderEventMessage message = null;
        CreditOutcomeType type = null;
        try {
            message = mapper.readValue(payload, OrderEventMessage.class);
            logEvent("order_event_received", subscription, message, null, "received");
            if (message.order() == null || message.orderId() == null
                    || !message.orderId().equals(message.order().id())
                    || message.orderVersion() != message.order().version()) {
                throw new InvalidOrderEventException("Order event envelope does not match its snapshot.");
            }
            type = switch (message.eventType()) {
                case "OpenOrderRefundTaskEvent" -> CreditOutcomeType.OPEN_ORDER_REFUND;
                case "AcceptedOrderCancellationTaskEvent" -> CreditOutcomeType.ACCEPTED_ORDER_CANCELLATION;
                case "OrderCompletionTaskEvent" -> CreditOutcomeType.ORDER_COMPLETION;
                default -> throw new InvalidOrderEventException(
                        "Unsupported Order event type: " + message.eventType());
            };
            if (!expectedSubscription(type).equals(subscription)) {
                throw new InvalidOrderEventException(
                        "Order event type does not match its Pub/Sub subscription.");
            }
            if (type == CreditOutcomeType.ORDER_COMPLETION && message.overdue() == null) {
                throw new InvalidOrderEventException("Completion event is missing its overdue fact.");
            }
            processor.processOutcome(new CreditOutcomeEvent(
                    message.eventId(), type, message.eventVersion(), message.orderId(),
                    message.orderVersion(), message.occurredAt(), message.actorId(),
                    message.order().requesterId(), message.order().courierId(),
                    message.order().offeredCredits(), message.order().status(),
                    Boolean.TRUE.equals(message.overdue()), message.overdueAt()));
            logEvent("order_event_processed", subscription, message, type,
                    "acknowledgement_ready");
        } catch (RuntimeException exception) {
            log.atWarn()
                    .addKeyValue("service", "credit-service")
                    .addKeyValue("subscription", subscription)
                    .addKeyValue("eventId", message == null ? null : message.eventId())
                    .addKeyValue("eventType", message == null ? null : message.eventType())
                    .addKeyValue("creditOutcomeType", type)
                    .addKeyValue("orderId", message == null ? null : message.orderId())
                    .addKeyValue("orderVersion", message == null ? null : message.orderVersion())
                    .addKeyValue("outcome", "failed")
                    .addKeyValue("errorType", exception.getClass().getSimpleName())
                    .addKeyValue("errorMessage", exception.getMessage())
                    .log("order_event_processing_failed");
            throw exception;
        }
    }

    private static void logEvent(String logMessage, String subscription, OrderEventMessage message,
                                 CreditOutcomeType type, String outcome) {
        log.atInfo()
                .addKeyValue("service", "credit-service")
                .addKeyValue("subscription", subscription)
                .addKeyValue("eventId", message.eventId())
                .addKeyValue("eventType", message.eventType())
                .addKeyValue("creditOutcomeType", type)
                .addKeyValue("orderId", message.orderId())
                .addKeyValue("orderVersion", message.orderVersion())
                .addKeyValue("outcome", outcome)
                .log(logMessage);
    }

    private String expectedSubscription(CreditOutcomeType type) {
        return switch (type) {
            case OPEN_ORDER_REFUND -> properties.openRefundSubscriptionPath();
            case ACCEPTED_ORDER_CANCELLATION -> properties.acceptedCancellationSubscriptionPath();
            case ORDER_COMPLETION -> properties.completionSubscriptionPath();
        };
    }
}
