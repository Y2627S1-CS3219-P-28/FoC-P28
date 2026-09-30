package sg.edu.nus.foc.order.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import sg.edu.nus.foc.order.domain.Order;

public final class OrderDtos {
    private OrderDtos() {}
    public record Create(@NotBlank String commandId,@NotBlank String requesterId,@NotBlank String itemDescription,@NotBlank String pickupSupplierId,@NotBlank String deliverySupplierId,@Min(1) long offeredCredits,@Min(15) int deliveryTimeLimitMinutes,@NotNull Instant expiresAt) {}
    public record Actor(@NotBlank String commandId,@NotBlank String actorId,@Min(0) long expectedVersion) {}
    public record RepostConfig(@NotBlank String commandId,@NotBlank String actorId,@Min(0) long expectedVersion,boolean enabled,@NotNull Instant dueAt,@Min(1) long creditAmount,@Min(15) int deliveryDurationMinutes) {}
    public record ManualRepost(@NotBlank String commandId,@NotBlank String actorId,@Min(0) long expectedVersion,@NotBlank String itemDescription,@Min(1) long offeredCredits,@Min(15) int deliveryTimeLimitMinutes,@NotNull Instant expiresAt) {}
    public record Draft(String originalOrderId,String itemDescription,String pickupSupplierId,String deliverySupplierId,long offeredCredits,int deliveryTimeLimitMinutes,Instant expiresAt) {}
    public record View(String id,String requesterId,String courierId,String itemDescription,String pickupSupplierId,String deliverySupplierId,long offeredCredits,String status,Instant createdAt,Instant expiresAt,int deliveryTimeLimitMinutes,long version,String originalOrderId,String repostedOrderId) {
        public static View of(Order o){return new View(o.getId(),o.getRequesterId(),o.getCourierId(),o.getItemDescription(),o.getPickupSupplierId(),o.getDeliverySupplierId(),o.getOfferedCredits(),o.getStatus().name(),o.getCreatedAt(),o.getExpiresAt(),o.getDeliveryTimeLimitMinutes(),o.getVersion(),o.getOriginalOrderId(),o.getRepostedOrderId());}
    }
}
