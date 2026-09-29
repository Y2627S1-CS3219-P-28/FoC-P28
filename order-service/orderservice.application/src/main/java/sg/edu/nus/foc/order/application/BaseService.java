package sg.edu.nus.foc.order.application;

import org.springframework.transaction.interceptor.TransactionAspectSupport;

import sg.edu.nus.foc.order.domain.OrderProblem;

import java.util.List;

public abstract class BaseService {
    protected <T> ResultWrapper<T> ok(T data) {
        return ResultWrapper.success(data);
    }

    protected <T> ResultWrapper<T> error(OrderProblem problem) {
        return ResultWrapper.failure(problem);
    }

    protected <T> ResultWrapper<T> validationError(List<OrderProblem.Detail> details) {
        return error(
                new OrderProblem(
                        OrderProblem.Code.VALIDATION_ERROR,
                        "Check the highlighted fields.",
                        details));
    }

    /** Only call when returning failure after mutations in an active transaction. */
    protected <T> ResultWrapper<T> rollbackError(OrderProblem problem) {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return error(problem);
    }
}
