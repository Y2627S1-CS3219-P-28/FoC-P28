package sg.edu.nus.foc.order.application.recovery;

/** Contract-fixture boundary. No live implementation until FEEDBACK-017 is verified. */
public interface CreditCompensationPublisher {
    void publish(CreditCompensationRequest request);
}
