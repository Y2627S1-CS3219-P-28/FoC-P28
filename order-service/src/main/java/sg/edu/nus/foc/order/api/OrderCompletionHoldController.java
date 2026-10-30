package sg.edu.nus.foc.order.api;

import java.security.Principal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sg.edu.nus.foc.order.api.dto.request.PlaceCompletionHoldRequest;
import sg.edu.nus.foc.order.api.dto.response.ActiveReportHoldResponse;
import sg.edu.nus.foc.order.api.dto.response.OrderPageResponse;
import sg.edu.nus.foc.order.api.mapper.OrderMapper;
import sg.edu.nus.foc.order.application.OrderCompletionHoldService;
import sg.edu.nus.foc.order.application.OrderQueryService;
import sg.edu.nus.foc.order.domain.OrderProblem;
import sg.edu.nus.foc.order.security.annotation.RequireRequesterRole;

@RestController
@RequestMapping("/api/orders/mine/active")
@RequiredArgsConstructor
@RequireRequesterRole
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Order report holds", description = "Caller-scoped active Orders and atomic report hold placement")
public class OrderCompletionHoldController {
    private final OrderCompletionHoldService holds;
    private final OrderQueryService queries;
    private final OrderMapper mapper;

    @GetMapping
    @Operation(summary = "List the authenticated requester's reportable orders", description = "SQL filtering before pagination; ACCEPTED, IN_PROGRESS, PICKED_UP and DELIVERED only. OPEN and terminal orders are excluded. Newest first with stable ID tie-break. Caller comes only from the validated token.")
    @ApiResponse(responseCode = "200", description = "One-based active Order page")
    @ApiResponse(responseCode = "400", description = "Invalid page or size; page >=1 and size 1..100")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "403", description = "Requester role required")
    @ApiResponse(responseCode = "503", description = "Role dependency unavailable")
    public OrderPageResponse active(Principal principal,
            @Parameter(description = "One-based page") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size, 1..100") @RequestParam(defaultValue = "20") int size) {
        return mapper.toResponse(queries.requestedActiveBy(caller(principal), page, size));
    }

    @PutMapping("/{orderId}/report-holds/{reportId}")
    @Operation(summary = "Atomically validate a reportable requester-owned order and place its report hold", description = "ACCEPTED, IN_PROGRESS, PICKED_UP or DELIVERED only; OPEN and terminal orders return 404. Locks the scoped Order and saves the report-specific hold in one transaction. No requester ID is accepted from the body. Retries reuse the hold and return the current authoritative snapshot; status can progress after the initial placement. Released report IDs cannot reactivate holds.")
    @ApiResponse(responseCode = "200", description = "Hold committed or existing active hold reused; current snapshot returned")
    @ApiResponse(responseCode = "400", description = "Invalid report ID or missing requireAssignedCourier flag")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "403", description = "Requester role required")
    @ApiResponse(responseCode = "404", description = "Missing, unowned or inactive Order; indistinguishable response")
    @ApiResponse(responseCode = "409", description = "Required courier missing, report binding conflict, released hold, unresolved command or row contention")
    @ApiResponse(responseCode = "503", description = "Role dependency unavailable")
    public ActiveReportHoldResponse place(@PathVariable String orderId, @PathVariable String reportId,
            @Valid @RequestBody PlaceCompletionHoldRequest request, Principal principal,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return mapper.toActiveHoldResponse(holds.placeActive(orderId, reportId, caller(principal),
                request.getRequireAssignedCourier(), authorization), reportId);
    }

    private String caller(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new OrderProblem("UNAUTHENTICATED", "Authentication is required.");
        }
        return principal.getName();
    }
}
