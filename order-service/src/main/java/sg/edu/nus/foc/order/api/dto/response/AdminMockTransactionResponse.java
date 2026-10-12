package sg.edu.nus.foc.order.api.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sample projection matching the existing Credit transaction history wire fields. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminMockTransactionResponse {

    private String transactionId;
    private String type;
    private long amount;
    private String direction;
    private Instant occurredAt;
    private String orderId;
}
