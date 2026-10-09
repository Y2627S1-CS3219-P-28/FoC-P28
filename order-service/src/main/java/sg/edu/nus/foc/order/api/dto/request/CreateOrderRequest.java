package sg.edu.nus.foc.order.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    @NotBlank
    private String commandId;

    @NotBlank
    private String requesterId;

    @NotBlank
    private String itemDescription;

    @NotBlank
    private String pickupSupplierId;

    @NotBlank
    private String deliverySupplierId;

    @Min(1)
    private long offeredCredits;

    @Min(15)
    private int deliveryTimeLimitMinutes;

    @NotNull
    private Instant expiresAt;

    private boolean automaticRepost;
    private Instant repostDueAt;
    private long repostCreditAmount;
    private int repostDeliveryDurationMinutes;
    private Instant repostExpiresAt;
}
