package sg.edu.nus.foc.order.application.recovery;

import java.time.Instant;
import sg.edu.nus.foc.order.api.dto.response.OrderResponse;

/** Safe owner-facing status: no lease, hash, credentials or raw peer diagnostics. */
public record CommandView(String commandId, String kind, String status, String outcome, String reason,
                          String message, String orderId, int attemptCount,
                          Instant nextRetryAt, OrderResponse result) { }
