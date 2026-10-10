package sg.edu.nus.foc.order.application.recovery;

/** ADR-033 contract-stub boundary. No live HTTP implementation is authorized. */
public interface CreditCommandGateway {
    Result execute(String key, Mutation mutation, long generation, String authorization);
    boolean compensate(String key, Mutation mutation, long generation, String authorization);

    record Mutation(String kind, String orderId, String actorId, long amount) { }
    record Result(boolean applied, String code) {
        public static Result success() { return new Result(true, null); }
        public static Result rejection(String code) { return new Result(false, code); }
    }
}
