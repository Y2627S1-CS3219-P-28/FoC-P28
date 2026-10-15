package sg.edu.nus.foc.order.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sg.edu.nus.foc.order.api.dto.request.PlaceCompletionHoldRequest;
import sg.edu.nus.foc.order.api.dto.response.CompletionHoldResponse;
import sg.edu.nus.foc.order.api.mapper.OrderMapper;
import sg.edu.nus.foc.order.application.OrderCompletionHoldService;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.security.annotation.RequireRequesterRole;

@RestController
@RequestMapping("/api/orders/internal")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Order report holds", description = "Requester-authorized report placement; release is deferred")
public class OrderCompletionHoldController {

    private final OrderCompletionHoldService holds;
    private final OrderMapper mapper;

    @RequireRequesterRole
    @PutMapping("/{orderId}/completion-holds/{reportId}")
    @Operation(summary = "Place an idempotent report completion hold on the requester's active order",
            description = "Report Service forwards the requester's Firebase token. Order verifies role, UID and ownership. The hold prevents completion and funding outcomes without changing Order status.")
    @ApiResponse(responseCode = "200", description = "Hold placement committed, or the same report already placed its hold")
    @ApiResponse(responseCode = "400", description = "Invalid requester or report ID")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "403", description = "Requester role, token identity or Order ownership does not match")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @ApiResponse(responseCode = "409", description = "Order is terminal, has unresolved work, or report binding conflicts")
    @ApiResponse(responseCode = "503", description = "User role verification is unavailable")
    public CompletionHoldResponse place(
            @Parameter(description = "Order business ID") @PathVariable String orderId,
            @Parameter(description = "Stable Report Service report ID; reused for retries") @PathVariable String reportId,
            @Valid @RequestBody PlaceCompletionHoldRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Order order = holds.place(orderId, reportId, request.getRequesterId(), authorization);
        return mapper.toHoldResponse(order, reportId);
    }
}
