package sg.edu.nus.foc.order.api;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import sg.edu.nus.foc.order.contracts.ErrandContracts.*;
import sg.edu.nus.foc.order.domain.OrderProblem;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice(basePackageClasses = ErrandController.class)
public class OrderExceptionHandler {
    @ExceptionHandler(OrderProblem.class)
    ResponseEntity<ApiError> domain(OrderProblem error, HttpServletRequest request) {
        int status =
                switch (error.getCode()) {
                    case "FORBIDDEN" -> 403;
                    case "NOT_FOUND" -> 404;
                    case "CONFLICT" -> 409;
                    default -> 400;
                };
        return response(
                status,
                error.getCode(),
                error.getMessage(),
                error.getDetails().stream()
                        .map(d -> new ErrorDetail(d.field(), d.message()))
                        .toList(),
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(
            MethodArgumentNotValidException error, HttpServletRequest request) {
        return response(
                400,
                "VALIDATION_ERROR",
                "Check the highlighted fields.",
                error.getBindingResult().getFieldErrors().stream()
                        .map(e -> new ErrorDetail(e.getField(), e.getDefaultMessage()))
                        .toList(),
                request);
    }

    @ExceptionHandler({
        HandlerMethodValidationException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ApiError> malformed(Exception error, HttpServletRequest request) {
        return response(
                400,
                "VALIDATION_ERROR",
                "Invalid request fields, date, or pagination.",
                List.of(),
                request);
    }

    @ExceptionHandler({ConcurrencyFailureException.class, DataIntegrityViolationException.class})
    ResponseEntity<ApiError> conflict(Exception error, HttpServletRequest request) {
        return response(
                409,
                "CONFLICT",
                "The request conflicts with saved data. Refresh and retry.",
                List.of(),
                request);
    }

    private ResponseEntity<ApiError> response(
            int status,
            String code,
            String message,
            List<ErrorDetail> details,
            HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(
                        new ApiError(
                                status,
                                code,
                                message,
                                request.getRequestURI(),
                                Instant.now(),
                                details));
    }
}
