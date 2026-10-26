package sg.edu.nus.foc.order.api;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import sg.edu.nus.foc.order.api.dto.response.ErrorResponse;

/** Translate contention for admin resolution commands without changing other API contracts. */
@RestControllerAdvice(assignableTypes = AdminOrderController.class)
public class AdminOrderExceptionHandler {

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> busyOrder(HttpServletRequest request) {
        String action = request.getRequestURI().endsWith("/complete") ? "completion"
                : request.getRequestURI().endsWith("/cancel") ? "cancellation" : "review action";
        ErrorResponse error = new ErrorResponse(409, "CONFLICT",
                "This order is being updated. Please retry the " + action + ".",
                request.getRequestURI(), Instant.now(), List.of());
        return ResponseEntity.status(409).body(error);
    }
}
