package sg.edu.nus.foc.order.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sg.edu.nus.foc.order.api.dto.response.OrderPageResponse;
import sg.edu.nus.foc.order.api.mapper.OrderMapper;
import sg.edu.nus.foc.order.application.OrderQueryService;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.security.annotation.RequireAdminRole;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderQueryService queries;
    private final OrderMapper orderMapper;

    @Operation(summary = "List all orders for administrators, optionally filtered by status")
    @SecurityRequirement(name = "bearerAuth")
    @RequireAdminRole
    @GetMapping
    public OrderPageResponse listOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return orderMapper.toResponse(queries.allOrders(
                status,
                pageable.getPageNumber(),
                pageable.getPageSize()));
    }
}
