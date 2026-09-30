package sg.edu.nus.foc.order.application;

public interface CreditServicePort {
    void reserve(String orderId, String requesterId, long amount, String authorization);
}
