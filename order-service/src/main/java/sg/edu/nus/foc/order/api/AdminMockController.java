package sg.edu.nus.foc.order.api;

import java.net.URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sg.edu.nus.foc.order.api.dto.request.AdminMockSupplierRequest;
import sg.edu.nus.foc.order.api.dto.request.AdminMockSuspensionRequest;
import sg.edu.nus.foc.order.api.dto.response.AdminMockOrderResponse;
import sg.edu.nus.foc.order.api.dto.response.AdminMockPageResponse;
import sg.edu.nus.foc.order.api.dto.response.AdminMockRoleResponse;
import sg.edu.nus.foc.order.api.dto.response.AdminMockSupplierResponse;
import sg.edu.nus.foc.order.api.dto.response.AdminMockUserResponse;
import sg.edu.nus.foc.order.api.dto.response.AdminMockTransactionResponse;
import sg.edu.nus.foc.order.application.adminmock.AdminMockService;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.config.AdminMockCondition;

/** User-approved demo surface, available only with explicit local emulator configuration. */
@RestController
@Conditional(AdminMockCondition.class)
@RequestMapping("/api/orders/admin-mock")
@RequiredArgsConstructor
@Tag(name = "Admin demo", description = "Local sample data only; mutations reset on restart")
public class AdminMockController {

    private final AdminMockService samples;

    @GetMapping("/role-context")
    @Operation(summary = "Mock the current user's admin role for the local prototype")
    public AdminMockRoleResponse roles(
            @Parameter(description = "Current Firebase UID; mock display identity only") @RequestParam String userId) {
        return samples.roles(userId);
    }

    @GetMapping("/orders")
    @Operation(summary = "Search local sample orders by ID and status, newest first")
    public AdminMockPageResponse<AdminMockOrderResponse> orders(
            @Parameter(description = "Optional Order status") @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Optional exact Order ID") @RequestParam(required = false) String orderId,
            @Parameter(description = "One-based page") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size, 1 through 100") @RequestParam(defaultValue = "10") int size) {
        return samples.orders(status, orderId, page, size);
    }

    @GetMapping("/users")
    @Operation(summary = "Search local sample users by name or exact user ID")
    public AdminMockPageResponse<AdminMockUserResponse> users(
            @Parameter(description = "name or id") @RequestParam(defaultValue = "name") String searchBy,
            @Parameter(description = "Search term") @RequestParam(defaultValue = "") String q,
            @Parameter(description = "One-based page") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size, 1 through 100") @RequestParam(defaultValue = "10") int size) {
        return samples.users(searchBy, q, page, size);
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "View a sample user profile and sample credit projection")
    public AdminMockUserResponse user(@Parameter(description = "Sample user ID") @PathVariable String id) {
        return samples.user(id);
    }

    @GetMapping("/users/{id}/transactions")
    @Operation(summary = "View the selected sample user's credit transactions, newest first")
    @ApiResponse(responseCode = "200", description = "Sample credit history page")
    @ApiResponse(responseCode = "400", description = "Invalid page or size")
    @ApiResponse(responseCode = "404", description = "Unknown sample user")
    public AdminMockPageResponse<AdminMockTransactionResponse> transactions(
            @Parameter(description = "Sample user ID") @PathVariable String id,
            @Parameter(description = "One-based page") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size, 1 through 100") @RequestParam(defaultValue = "20") int size) {
        return samples.transactions(id, page, size);
    }

    @PostMapping("/users/{id}/suspend")
    @Operation(summary = "Suspend a sample courier until a future date and time")
    public AdminMockUserResponse suspend(
            @Parameter(description = "Sample user ID") @PathVariable String id,
            @Valid @RequestBody AdminMockSuspensionRequest request) {
        return samples.suspend(id, request.getSuspensionEndDate());
    }

    @PostMapping("/users/{id}/promote")
    @Operation(summary = "Add admin to a sample user's roles")
    public AdminMockUserResponse promote(@Parameter(description = "Sample user ID") @PathVariable String id) {
        return samples.promote(id);
    }

    @PostMapping("/users/{id}/deactivate")
    @Operation(summary = "Soft-deactivate a sample user after UI confirmation")
    public AdminMockUserResponse deactivateUser(@Parameter(description = "Sample user ID") @PathVariable String id) {
        return samples.deactivateUser(id);
    }

    @GetMapping("/suppliers")
    @Operation(summary = "Search local sample suppliers by name or exact supplier ID")
    public AdminMockPageResponse<AdminMockSupplierResponse> suppliers(
            @Parameter(description = "name or id") @RequestParam(defaultValue = "name") String searchBy,
            @Parameter(description = "Search term") @RequestParam(defaultValue = "") String q,
            @Parameter(description = "One-based page") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size, 1 through 100") @RequestParam(defaultValue = "10") int size) {
        return samples.suppliers(searchBy, q, page, size);
    }

    @PostMapping("/suppliers")
    @Operation(summary = "Create a sample supplier using the Supplier Service form fields")
    @ApiResponse(responseCode = "201", description = "Sample supplier created")
    @ApiResponse(responseCode = "400", description = "Invalid supplier fields")
    public ResponseEntity<AdminMockSupplierResponse> createSupplier(
            @Valid @RequestBody AdminMockSupplierRequest request) {
        AdminMockSupplierResponse supplier = samples.createSupplier(request);
        return ResponseEntity.created(URI.create("/api/orders/admin-mock/suppliers/" + supplier.getId())).body(supplier);
    }

    @GetMapping("/suppliers/{id}")
    @Operation(summary = "View one sample supplier including inactive locations")
    public AdminMockSupplierResponse supplier(
            @Parameter(description = "Sample supplier ID") @PathVariable String id) {
        return samples.supplier(id);
    }

    @PostMapping("/suppliers/{id}/deactivate")
    @Operation(summary = "Soft-deactivate a sample supplier location after UI confirmation")
    public AdminMockSupplierResponse deactivateSupplier(
            @Parameter(description = "Sample supplier ID") @PathVariable String id) {
        return samples.deactivateSupplier(id);
    }
}
