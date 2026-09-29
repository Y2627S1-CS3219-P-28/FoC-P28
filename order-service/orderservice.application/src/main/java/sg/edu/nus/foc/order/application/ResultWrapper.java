package sg.edu.nus.foc.order.application;

import lombok.Getter;

import sg.edu.nus.foc.order.domain.OrderProblem;

import java.util.Objects;
import java.util.function.Function;

/** Exactly one outcome. Failure values never contain an exception or HTTP status. */
@Getter
public final class ResultWrapper<T> {
    private final T data;
    private final OrderProblem error;

    private ResultWrapper(T data, OrderProblem error) {
        this.data = data;
        this.error = error;
    }

    public static <T> ResultWrapper<T> success(T data) {
        return new ResultWrapper<>(Objects.requireNonNull(data), null);
    }

    public static <T> ResultWrapper<T> failure(OrderProblem error) {
        return new ResultWrapper<>(null, Objects.requireNonNull(error));
    }

    public boolean isError() {
        return error != null;
    }

    public <R> ResultWrapper<R> map(Function<T, R> mapper) {
        return isError() ? failure(error) : success(mapper.apply(data));
    }
}
