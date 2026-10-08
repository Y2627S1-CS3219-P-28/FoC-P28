package sg.edu.nus.foc.order.application;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs both lifecycle checks without coupling their database transactions or failures. */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderLifecycleScheduler {
    private final LifecycleProcessingService lifecycle;

    @Scheduled(cron = "${order.lifecycle.cron:0 * * * * *}")
    public void processDueOrders() {
        Instant now = Instant.now();
        try {
            int expired = lifecycle.expireDue(now);
            if (expired > 0) {
                log.info("Expired {} due OPEN orders and recorded their refund events.", expired);
            }
        } catch (RuntimeException exception) {
            log.error("Order expiry pass failed; the next lifecycle pass will retry.", exception);
        }
        try {
            int completed = lifecycle.autoCompleteDue(now);
            if (completed > 0) {
                log.info("Automatically completed {} DELIVERED orders after the 48-hour grace period.", completed);
            }
        } catch (RuntimeException exception) {
            log.error("Automatic order-completion pass failed; the next lifecycle pass will retry.", exception);
        }
    }
}
