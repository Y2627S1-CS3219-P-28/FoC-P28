package sg.edu.nus.foc.credit.messaging;

@FunctionalInterface
public interface OrderEventPayloadConsumer {
    void consume(String subscription, String payload);
}
