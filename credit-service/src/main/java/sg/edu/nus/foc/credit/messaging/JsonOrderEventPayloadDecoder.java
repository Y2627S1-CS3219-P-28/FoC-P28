package sg.edu.nus.foc.credit.messaging;

import org.springframework.stereotype.Component;
import sg.edu.nus.foc.credit.error.InvalidOrderEventException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class JsonOrderEventPayloadDecoder implements OrderEventPayloadDecoder {

    private final JsonMapper mapper;

    public JsonOrderEventPayloadDecoder(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public OrderEventMessage decode(String subscription, String payload,
                                    String expectedSubscription, String expectedEventType) {
        OrderEventMessage message = mapper.readValue(payload, OrderEventMessage.class);
        if (!expectedSubscription.equals(subscription)) {
            throw new InvalidOrderEventException(
                    "Order event was delivered by the wrong Pub/Sub subscription.");
        }
        if (!expectedEventType.equals(message.eventType())) {
            throw new InvalidOrderEventException(
                    "Order event type does not match its push endpoint.");
        }
        if (message.order() == null || message.orderId() == null
                || !message.orderId().equals(message.order().id())
                || message.orderVersion() != message.order().version()) {
            throw new InvalidOrderEventException(
                    "Order event envelope does not match its snapshot.");
        }
        return message;
    }
}
