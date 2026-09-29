package sg.edu.nus.foc.order.domain;
/** Monetary operations are owned by Credit Service; the prototype adapter has no monetary effect. */
public interface CreditGateway {
    boolean reserve(String errandId, String requesterId, long amount);
    boolean release(String errandId, String requesterId);
}
