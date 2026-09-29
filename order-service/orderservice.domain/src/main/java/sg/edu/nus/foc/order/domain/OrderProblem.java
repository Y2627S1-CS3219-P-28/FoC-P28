package sg.edu.nus.foc.order.domain;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

@Getter
public final class OrderProblem {
    public enum Code {
        VALIDATION_ERROR,
        RESERVATION_REJECTED,
        FORBIDDEN,
        NOT_FOUND,
        CONFLICT
    }

    public record Detail(String field, String message) {}

    private final Code code;
    private final String message;
    private final List<Detail> details;

    public OrderProblem(Code code, String message) {
        this(code, message, List.of());
    }

    public OrderProblem(Code code, String message, List<Detail> details) {
        this.message = Objects.requireNonNull(message);
        this.code = Objects.requireNonNull(code);
        this.details = List.copyOf(details);
    }

    public static OrderProblem conflict(String message) {
        return new OrderProblem(Code.CONFLICT, message);
    }
}
