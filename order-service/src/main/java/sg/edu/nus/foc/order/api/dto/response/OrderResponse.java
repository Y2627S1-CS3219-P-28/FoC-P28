package sg.edu.nus.foc.order.api.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String id;
    private String requesterId;
    private String courierId;
    private String itemDescription;
    private String pickupSupplierId;
    private String deliverySupplierId;
    private long offeredCredits;
    private String status;
    private Instant createdAt;
    private Instant expiresAt;
    private int deliveryTimeLimitMinutes;
    private long version;
    private String originalOrderId;
    private String repostedOrderId;
    private boolean automaticRepostEnabled;
    private Instant repostDueAt;
    private long repostCreditAmount;
    private int repostDeliveryDurationMinutes;
}
