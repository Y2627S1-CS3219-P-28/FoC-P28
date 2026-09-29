package sg.edu.nus.foc.order.api;
import sg.edu.nus.foc.order.application.ErrandWorkflow;
import sg.edu.nus.foc.order.contracts.ErrandContracts.*;
import sg.edu.nus.foc.order.domain.Order;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController @RequestMapping("/api/orders") @Profile("!prod & (local | test)")
@DocumentErrors
public class ErrandController {
    private final ErrandWorkflow workflow;
    private final ErrandMapper mapper;
    public ErrandController(ErrandWorkflow workflow,ErrandMapper mapper) { this.workflow=workflow; this.mapper=mapper; }
    @PostMapping("/errands") @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary="Post an errand using prototype Credit and Supplier adapters")
    @ApiResponse(responseCode="201",description="OPEN errand created, or identical command replayed")
    @ApiResponse(responseCode="400",description="Invalid fields") @ApiResponse(responseCode="409",description="Command conflict")
    public ErrandResponse create(@Valid @RequestBody CreateRequest request) { return mapper.response(workflow.create(mapper.input(request))); }
    @GetMapping("/errands") @Operation(summary="Browse OPEN, unexpired errands")
    @ApiResponse(responseCode="200",description="Successful response")
    public PageResponse<ErrandResponse> available(@RequestParam(defaultValue="1") @Min(1) int page,
        @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        var result=workflow.available(page,size);
        return new PageResponse<>(result.getContent().stream().map(mapper::response).toList(),page,size,result.getTotalElements(),result.getTotalPages());
    }
    @GetMapping("/errands/{id}") @Operation(summary="Read the current errand after posting or acceptance")
    @ApiResponse(responseCode="200",description="Successful response")
    public ErrandResponse errand(@PathVariable String id) { return mapper.response(workflow.getErrand(id)); }
    @PostMapping("/errands/{id}/accept") @Operation(summary="Accept an available errand and create a delivery")
    @ApiResponse(responseCode="200",description="Committed delivery") @ApiResponse(responseCode="403",description="Own errand")
    @ApiResponse(responseCode="409",description="Unavailable errand, stale version or conflicting command")
    public OrderResponse accept(@PathVariable String id,@Valid @RequestBody ActionRequest request) { return mapper.response(workflow.accept(id,mapper.input(request))); }
    @GetMapping("/executions/{id}") @Operation(summary="Read a delivery and its progress checkpoints")
    @ApiResponse(responseCode="200",description="Successful response")
    public OrderResponse order(@PathVariable String id) { return mapper.response(workflow.getOrder(id)); }
    @PostMapping("/executions/{id}/start") @Operation(summary="Start an accepted delivery")
    @ApiResponse(responseCode="200",description="Successful response")
    public OrderResponse start(@PathVariable String id,@Valid @RequestBody ActionRequest request) { return progress(id,request,Order.Status.IN_PROGRESS); }
    @PostMapping("/executions/{id}/pickup") @Operation(summary="Record pickup for an in-progress delivery")
    @ApiResponse(responseCode="200",description="Successful response")
    public OrderResponse pickup(@PathVariable String id,@Valid @RequestBody ActionRequest request) { return progress(id,request,Order.Status.PICKED_UP); }
    @PostMapping("/executions/{id}/deliver") @Operation(summary="Record delivery after pickup")
    @ApiResponse(responseCode="200",description="Successful response")
    public OrderResponse deliver(@PathVariable String id,@Valid @RequestBody ActionRequest request) { return progress(id,request,Order.Status.DELIVERED); }
    @GetMapping("/prototype/suppliers") @Operation(summary="List sample suppliers for the development prototype")
    @ApiResponse(responseCode="200",description="Successful response")
    public List<SupplierResponse> suppliers() { return workflow.suppliers().stream().map(mapper::response).toList(); }
    private OrderResponse progress(String id,ActionRequest request,Order.Status target) { return mapper.response(workflow.progress(id,mapper.input(request),target)); }
}

