package sg.edu.nus.foc.order.api;

import java.time.Instant;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import sg.edu.nus.foc.order.domain.OrderProblem;

@RestControllerAdvice
public class OrderExceptionHandler {
    @ExceptionHandler(OrderProblem.class)
    public org.springframework.http.ResponseEntity<OrderDtos.ErrorResponse> orderProblem(OrderProblem e,HttpServletRequest request){int status=switch(e.code()){case "NOT_FOUND"->404;case "UNAUTHENTICATED"->401;case "FORBIDDEN"->403;case "VALIDATION_ERROR"->400;case "SERVICE_UNAVAILABLE","DEPENDENCY_UNAVAILABLE"->503;default->409;};return response(status,e.code(),e.getMessage(),request,e.details().stream().map(d->new OrderDtos.ErrorDetail(d.field(),d.message())).toList());}
    @ExceptionHandler(IllegalArgumentException.class)
    public org.springframework.http.ResponseEntity<OrderDtos.ErrorResponse> invalid(IllegalArgumentException e,HttpServletRequest request){return response(400,"VALIDATION_ERROR",e.getMessage(),request,List.of());}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public org.springframework.http.ResponseEntity<OrderDtos.ErrorResponse> invalidRequest(MethodArgumentNotValidException e,HttpServletRequest request){var details=e.getBindingResult().getFieldErrors().stream().map(error->new OrderDtos.ErrorDetail(error.getField(),error.getDefaultMessage()==null?"Invalid value":error.getDefaultMessage())).toList();return response(400,"VALIDATION_ERROR","Request validation failed.",request,details);}
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public org.springframework.http.ResponseEntity<OrderDtos.ErrorResponse> malformedRequest(Exception e,HttpServletRequest request){return response(400,"VALIDATION_ERROR","Request format is invalid.",request,List.of());}
    private org.springframework.http.ResponseEntity<OrderDtos.ErrorResponse> response(int status,String error,String message,HttpServletRequest request,List<OrderDtos.ErrorDetail> details){return org.springframework.http.ResponseEntity.status(status).body(new OrderDtos.ErrorResponse(status,error,message,request.getRequestURI(),Instant.now(),details));}
}
