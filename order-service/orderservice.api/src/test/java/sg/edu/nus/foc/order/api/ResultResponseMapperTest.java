package sg.edu.nus.foc.order.api;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import sg.edu.nus.foc.order.application.ResultWrapper;
import sg.edu.nus.foc.order.contracts.ErrandContracts.ApiError;
import sg.edu.nus.foc.order.domain.OrderProblem;

import java.util.List;

class ResultResponseMapperTest {
    private final ResultResponseMapper mapper = new ResultResponseMapper();

    @Test
    void unexpectedFailureIsSanitizedAndDoesNotExposeInternalMessage() {
        org.springframework.mock.web.MockHttpServletRequest request =
                new org.springframework.mock.web.MockHttpServletRequest(
                        "POST", "/api/orders/errands");
        ResponseEntity<ApiError> response =
                new OrderExceptionHandler()
                        .unexpected(new IllegalStateException("private database details"), request);
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred.");
        assertThat(response.getBody().path()).isEqualTo("/api/orders/errands");
    }

    @Test
    void preservesSuccessAndEveryBusinessHttpCode() {
        assertThat(
                        mapper.response(ResultWrapper.success("data"), 201, "/api/orders")
                                .getStatusCode()
                                .value())
                .isEqualTo(201);
        assertThat(mapper.response(ResultWrapper.success("data"), 200, "/api/orders").getBody())
                .isEqualTo("data");
        for (OrderProblem.Code code : OrderProblem.Code.values()) {
            int expected =
                    switch (code) {
                        case FORBIDDEN -> 403;
                        case NOT_FOUND -> 404;
                        case CONFLICT -> 409;
                        default -> 400;
                    };
            OrderProblem problem =
                    new OrderProblem(
                            code, "failure", List.of(new OrderProblem.Detail("field", "invalid")));
            ResponseEntity<?> response =
                    mapper.response(ResultWrapper.failure(problem), 200, "/api/orders/test");
            assertThat(response.getStatusCode().value()).isEqualTo(expected);
            ApiError body = (ApiError) response.getBody();
            assertThat(body.error()).isEqualTo(code.name());
            assertThat(body.path()).isEqualTo("/api/orders/test");
            assertThat(body.details()).hasSize(1);
        }
    }
}
