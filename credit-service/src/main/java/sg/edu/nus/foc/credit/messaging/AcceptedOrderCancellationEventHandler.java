package sg.edu.nus.foc.credit.messaging;

@FunctionalInterface
public interface AcceptedOrderCancellationEventHandler {

    void handle(OrderEventMessage message);
}
