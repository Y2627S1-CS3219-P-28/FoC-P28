package sg.edu.nus.foc.order.application;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryScheduler {
    private final LifecycleProcessingService lifecycle;

    @Scheduled(cron = "${order.lifecycle.expiry-cron:0 * * * * *}")
    public void expireDueOrders() {
        try {
            int expired = lifecycle.expireDue(Instant.now());
            if (expired > 0) {
                log.info("Expired {} due OPEN orders and recorded their refund events.", expired);
            }
        } catch (RuntimeException exception) {
            log.error("Order expiry pass failed; the next scheduler pass will retry.", exception);
        }
    }
}
