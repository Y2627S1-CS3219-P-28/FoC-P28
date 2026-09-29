package sg.edu.nus.foc.order.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sg.edu.nus.foc.order.application.ErrandWorkflow;
import sg.edu.nus.foc.order.contracts.ErrandContracts.*;
import sg.edu.nus.foc.order.domain.Order;

@RestController
@RequestMapping("/api/orders")
@Profile("!prod & (local | test)")
@DocumentErrors
@RequiredArgsConstructor
public class ErrandController {
    private final ErrandWorkflow workflow;
    private final ErrandMapper mapper;
    private final ResultResponseMapper responses;
    private final HttpServletRequest httpRequest;

    @PostMapping("/errands")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Post an errand using prototype Credit and Supplier adapters")
    @ApiResponse(
            responseCode = "201",
            description = "OPEN errand created, or identical command replayed",
            content = @Content(schema = @Schema(implementation = ErrandResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid fields")
    @ApiResponse(responseCode = "409", description = "Command conflict")
    public ResponseEntity<?> create(@Valid @RequestBody CreateRequest request) {
        return responses.response(
                workflow.create(mapper.input(request)).map(mapper::response),
                201,
                httpRequest.getRequestURI());
    }

    @GetMapping("/errands")
    @Operation(summary = "Browse OPEN, unexpired errands")
    @ApiResponse(
            responseCode = "200",
            description = "Successful response",
            content = @Content(schema = @Schema(implementation = ErrandPageResponse.class)))
    public ResponseEntity<?> available(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return responses.response(
                workflow.available(page, size)
                        .map(
                                result ->
                                        new ErrandPageResponse(
                                                result.getContent().stream()
                                                        .map(mapper::response)
                                                        .toList(),
                                                page,
                                                size,
                                                result.getTotalElements(),
                                                result.getTotalPages())),
                200,
                httpRequest.getRequestURI());
    }

    @GetMapping("/errands/{id}")
    @Operation(summary = "Read the current errand after posting or acceptance")
    @ApiResponse(
            responseCode = "200",
            description = "Successful response",
            content = @Content(schema = @Schema(implementation = ErrandResponse.class)))
    public ResponseEntity<?> errand(@PathVariable String id) {
        return responses.response(
                workflow.getErrand(id).map(mapper::response), 200, httpRequest.getRequestURI());
    }

    @PostMapping("/errands/{id}/accept")
    @Operation(summary = "Accept an available errand and create a delivery")
    @ApiResponse(
            responseCode = "200",
            description = "Committed delivery",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @ApiResponse(responseCode = "403", description = "Own errand")
    @ApiResponse(
            responseCode = "409",
            description = "Unavailable errand, stale version or conflicting command")
    public ResponseEntity<?> accept(
            @PathVariable String id, @Valid @RequestBody ActionRequest request) {
        return responses.response(
                workflow.accept(id, mapper.input(request)).map(mapper::response),
                200,
                httpRequest.getRequestURI());
    }

    @GetMapping("/executions/{id}")
    @Operation(summary = "Read a delivery and its progress checkpoints")
    @ApiResponse(
            responseCode = "200",
            description = "Successful response",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    public ResponseEntity<?> order(@PathVariable String id) {
        return responses.response(
                workflow.getOrder(id).map(mapper::response), 200, httpRequest.getRequestURI());
    }

    @PostMapping("/executions/{id}/start")
    @Operation(summary = "Start an accepted delivery")
    @ApiResponse(
            responseCode = "200",
            description = "Successful response",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    public ResponseEntity<?> start(
            @PathVariable String id, @Valid @RequestBody ActionRequest request) {
        return progress(id, request, Order.Status.IN_PROGRESS);
    }

    @PostMapping("/executions/{id}/pickup")
    @Operation(summary = "Record pickup for an in-progress delivery")
    @ApiResponse(
            responseCode = "200",
            description = "Successful response",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    public ResponseEntity<?> pickup(
            @PathVariable String id, @Valid @RequestBody ActionRequest request) {
        return progress(id, request, Order.Status.PICKED_UP);
    }

    @PostMapping("/executions/{id}/deliver")
    @Operation(summary = "Record delivery after pickup")
    @ApiResponse(
            responseCode = "200",
            description = "Successful response",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    public ResponseEntity<?> deliver(
            @PathVariable String id, @Valid @RequestBody ActionRequest request) {
        return progress(id, request, Order.Status.DELIVERED);
    }

    @GetMapping("/prototype/suppliers")
    @Operation(summary = "List sample suppliers for the development prototype")
    @ApiResponse(
            responseCode = "200",
            description = "Successful response",
            content =
                    @Content(
                            array =
                                    @io.swagger.v3.oas.annotations.media.ArraySchema(
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    SupplierResponse.class))))
    public ResponseEntity<?> suppliers() {
        return responses.response(
                workflow.suppliers().map(values -> values.stream().map(mapper::response).toList()),
                200,
                httpRequest.getRequestURI());
    }

    private ResponseEntity<?> progress(String id, ActionRequest request, Order.Status target) {
        return responses.response(
                workflow.progress(id, mapper.input(request), target).map(mapper::response),
                200,
                httpRequest.getRequestURI());
    }
}
