package sg.edu.nus.foc.order.contracts;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
public final class ErrandContracts {
    private ErrandContracts() {}
    public record CreateRequest(
        @NotBlank @Size(max=128) String commandId,
        @NotBlank @Size(max=128) String requesterId,
        @NotBlank @Size(min=10,max=100) String description,
        @NotBlank @Size(max=128) String pickupSupplierId,
        @NotBlank @Size(max=128) String deliverySupplierId,
        @NotNull @Positive Long creditAmount,
        @NotNull @Min(15) @Max(1440) Integer deliveryDurationMinutes,
        @NotNull Instant expiresAt) {}
    public record ActionRequest(@NotBlank @Size(max=128) String commandId,
        @NotBlank @Size(max=128) String actorId, @NotNull @PositiveOrZero Long expectedVersion) {}
    public record SupplierResponse(String id,String name,String location) {}
    public record ErrandResponse(String id,String requesterId,String description,String pickupSupplierId,
        String deliverySupplierId,long creditAmount,int deliveryDurationMinutes,Instant expiresAt,Instant createdAt,
        String status,String orderId,long version,SupplierResponse pickup,SupplierResponse delivery) {}
    public record CheckpointResponse(String id,String orderId,String courierId,String status,Instant occurredAt,String supplierId) {}
    public record OrderResponse(String id,String errandId,String courierId,String status,int deliveryDurationMinutes,
        Instant acceptedAt,Instant startedAt,Instant pickedUpAt,Instant deliveredAt,Instant deliveryDeadline,
        long version,ErrandResponse errand,List<CheckpointResponse> checkpoints) {}
    public record PageResponse<T>(List<T> items,int page,int size,long totalItems,int totalPages) {}
    public record ErrorDetail(String field,String message) {}
    public record ApiError(int status,String error,String message,String path,Instant timestamp,List<ErrorDetail> details) {}
}
