package sg.edu.nus.foc.order.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import sg.edu.nus.foc.order.domain.Order;

public final class OrderDtos {
    private OrderDtos() {}
    public record Create(@NotBlank String commandId,@NotBlank String requesterId,@NotBlank String itemDescription,@NotBlank String pickupSupplierId,@NotBlank String deliverySupplierId,@Min(1) long offeredCredits,@Min(15) int deliveryTimeLimitMinutes,@NotNull Instant expiresAt,boolean automaticRepost,Instant repostDueAt,long repostCreditAmount,int repostDeliveryDurationMinutes) {}
    public record Actor(@NotBlank String commandId,@NotBlank String actorId,@Min(0) long expectedVersion) {}
    public record RepostConfig(@NotBlank String commandId,@NotBlank String actorId,@Min(0) long expectedVersion,boolean enabled,@NotNull Instant dueAt,@Min(1) long creditAmount,@Min(15) int deliveryDurationMinutes) {}
    public record ManualRepost(@NotBlank String commandId,@NotBlank String actorId,@Min(0) long expectedVersion,@NotBlank String itemDescription,@Min(1) long offeredCredits,@Min(15) int deliveryTimeLimitMinutes,@NotNull Instant expiresAt) {}
    public record Draft(String originalOrderId,String itemDescription,String pickupSupplierId,String deliverySupplierId,long offeredCredits,int deliveryTimeLimitMinutes,Instant expiresAt) {}
    public record View(String id,String requesterId,String courierId,String itemDescription,String pickupSupplierId,String deliverySupplierId,long offeredCredits,String status,Instant createdAt,Instant expiresAt,int deliveryTimeLimitMinutes,long version,String originalOrderId,String repostedOrderId,boolean automaticRepostEnabled,Instant repostDueAt,long repostCreditAmount,int repostDeliveryDurationMinutes) {
        public static View of(Order o){var plan=o.getRepostPlan();return new View(o.getId(),o.getRequesterId(),o.getCourierId(),o.getItemDescription(),o.getPickupSupplierId(),o.getDeliverySupplierId(),o.getOfferedCredits(),o.getStatus().name(),o.getCreatedAt(),o.getExpiresAt(),o.getDeliveryTimeLimitMinutes(),o.getVersion(),o.getOriginalOrderId(),o.getRepostedOrderId(),plan!=null&&plan.enabled(),plan==null?null:plan.dueAt(),plan==null?0:plan.creditAmount(),plan==null?0:plan.deliveryDurationMinutes());}
    }
    public record PageView<T>(List<T> items,int page,int size,long totalItems,int totalPages) {
        public static <T> PageView<T> of(Page<T> page){return new PageView<>(page.getContent(),page.getNumber()+1,page.getSize(),page.getTotalElements(),page.getTotalPages());}
    }
    public record ErrorDetail(String field,String message) {}
    public record ErrorResponse(int status,String error,String message,String path,Instant timestamp,List<ErrorDetail> details) {}
}
