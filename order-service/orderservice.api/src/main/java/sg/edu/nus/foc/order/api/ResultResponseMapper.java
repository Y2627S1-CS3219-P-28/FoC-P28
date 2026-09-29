package sg.edu.nus.foc.order.api;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import sg.edu.nus.foc.order.application.ResultWrapper;
import sg.edu.nus.foc.order.contracts.ErrandContracts.*;
import sg.edu.nus.foc.order.domain.OrderProblem;

import java.time.Instant;

/** Converts application outcomes to the existing public response format. */
@Component
public class ResultResponseMapper {
    public ResponseEntity<?> response(ResultWrapper<?> result, int successStatus, String path) {
        if (!result.isError()) {
            return ResponseEntity.status(successStatus).body(result.getData());
        }
        OrderProblem problem = result.getError();
        int status =
                switch (problem.getCode()) {
                    case FORBIDDEN -> 403;
                    case NOT_FOUND -> 404;
                    case CONFLICT -> 409;
                    case VALIDATION_ERROR, RESERVATION_REJECTED -> 400;
                };
        return ResponseEntity.status(status)
                .body(
                        new ApiError(
                                status,
                                problem.getCode().name(),
                                problem.getMessage(),
                                path,
                                Instant.now(),
                                problem.getDetails().stream()
                                        .map(
                                                detail ->
                                                        new ErrorDetail(
                                                                detail.field(), detail.message()))
                                        .toList()));
    }
}
