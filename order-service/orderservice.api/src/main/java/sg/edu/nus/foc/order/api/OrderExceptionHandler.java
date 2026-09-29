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

import java.time.Instant;
import java.util.List;

@RestControllerAdvice(basePackageClasses = ErrandController.class)
@lombok.extern.slf4j.Slf4j
public class OrderExceptionHandler {
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception error, HttpServletRequest request) {
        log.error("Unexpected Order Service failure at {}", request.getRequestURI(), error);
        return response(
                500, "INTERNAL_SERVER_ERROR", "An unexpected error occurred.", List.of(), request);
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
