package sg.edu.nus.foc.credit.messaging;

@FunctionalInterface
public interface OrderEventPayloadDecoder {

    OrderEventMessage decode(String subscription, String payload,
                             String expectedSubscription, String expectedEventType);
}
