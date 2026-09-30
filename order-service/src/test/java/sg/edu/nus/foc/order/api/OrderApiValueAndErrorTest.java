package sg.edu.nus.foc.order.api;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import sg.edu.nus.foc.order.domain.OrderProblem;

class OrderApiValueAndErrorTest {
    @Test
    void allDtoRecordsExposeTheirValues() {
        Instant now = Instant.parse("2026-09-30T00:00:00Z");
        OrderDtos.Create create = new OrderDtos.Create("c", "u", "item", "p", "d", 2, 15, now, true, now, 3, 20);
        assertEquals("c", create.commandId());
        OrderDtos.Actor actor = new OrderDtos.Actor("a", "u", 1);
        assertEquals("u", actor.actorId());
        OrderDtos.RepostConfig config = new OrderDtos.RepostConfig("r", "u", 1, true, now, 2, 15);
        assertTrue(config.enabled());
        OrderDtos.ManualRepost manual = new OrderDtos.ManualRepost("m", "u", 1, "item", 2, 15, now);
        assertEquals("item", manual.itemDescription());
        OrderDtos.Draft draft = new OrderDtos.Draft("original", "item", "p", "d", 2, 15, now);
        assertEquals("original", draft.originalOrderId());
        OrderDtos.ErrorDetail detail = new OrderDtos.ErrorDetail("field", "bad");
        OrderDtos.ErrorResponse response = new OrderDtos.ErrorResponse(400, "VALIDATION_ERROR", "bad", "/path", now, List.of(detail));
        assertEquals(List.of(detail), response.details());
        assertEquals(400, response.status());
        assertEquals("VALIDATION_ERROR", response.error());
        assertEquals("bad", response.message());
        assertEquals("/path", response.path());
        assertEquals(now, response.timestamp());
        assertEquals("field", detail.field());
        assertEquals("bad", detail.message());
    }

    @Test
    void exceptionHandlerMapsCodesAndMalformedRequests() {
        OrderExceptionHandler handler = new OrderExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        assertEquals(404, handler.orderProblem(OrderProblem.notFound("missing"), request).getStatusCode().value());
        assertEquals(401, handler.orderProblem(new OrderProblem("UNAUTHENTICATED", "login"), request).getStatusCode().value());
        assertEquals(403, handler.orderProblem(OrderProblem.forbidden("denied"), request).getStatusCode().value());
        assertEquals(400, handler.orderProblem(new OrderProblem("VALIDATION_ERROR", "bad"), request).getStatusCode().value());
        assertEquals(503, handler.orderProblem(new OrderProblem("SERVICE_UNAVAILABLE", "down"), request).getStatusCode().value());
        assertEquals(409, handler.orderProblem(new OrderProblem("CONFLICT", "conflict"), request).getStatusCode().value());
        assertEquals(400, handler.invalid(new IllegalArgumentException("bad"), request).getStatusCode().value());
        assertEquals(400, handler.malformedRequest(new HttpMessageNotReadableException("bad",
            new MockHttpInputMessage(new ByteArrayInputStream(new byte[0]))), request).getStatusCode().value());
        assertEquals(400, handler.malformedRequest(new MethodArgumentTypeMismatchException("x", String.class, "id", null, null), request).getStatusCode().value());
    }

    @Test
    void validationErrorsIncludeFieldDetails() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "itemDescription", "must not be blank"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException((MethodParameter) null, binding);
        OrderDtos.ErrorResponse body = new OrderExceptionHandler()
            .invalidRequest(exception, new MockHttpServletRequest("POST", "/api/orders")).getBody();
        assertNotNull(body);
        assertEquals("itemDescription", body.details().getFirst().field());
        assertEquals("must not be blank", body.details().getFirst().message());
    }
}
