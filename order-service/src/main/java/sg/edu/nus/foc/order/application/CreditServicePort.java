package sg.edu.nus.foc.order.application;

public interface CreditServicePort {
    void reserve(String orderId, String requesterId, long amount, String authorization);
    void holdForReopen(String commandId, String orderId, String requesterId, String courierId, long amount,
                       long expectedOrderVersion, String authorization);
    void settle(String commandId, String orderId, String requesterId, String courierId, long amount,
                long expectedOrderVersion, String authorization);
    void release(String commandId, String orderId, String requesterId, long amount, String outcome,
                 long expectedOrderVersion, String authorization);
}
