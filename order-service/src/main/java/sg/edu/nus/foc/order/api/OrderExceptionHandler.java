package sg.edu.nus.foc.order.api;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import sg.edu.nus.foc.order.domain.OrderProblem;

@RestControllerAdvice
public class OrderExceptionHandler {
    @ExceptionHandler(OrderProblem.class)
    public org.springframework.http.ResponseEntity<Map<String,Object>> orderProblem(OrderProblem e){int status=switch(e.code()){case "NOT_FOUND"->404;case "FORBIDDEN"->403;case "VALIDATION_ERROR"->400;default->409;};return org.springframework.http.ResponseEntity.status(status).body(Map.of("code",e.code(),"message",e.getMessage(),"details",e.details()));}
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> invalid(IllegalArgumentException e){return Map.of("code","VALIDATION_ERROR","message",e.getMessage());}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> invalidRequest(MethodArgumentNotValidException e){return Map.of("code","VALIDATION_ERROR","message","Request validation failed.");}
}
