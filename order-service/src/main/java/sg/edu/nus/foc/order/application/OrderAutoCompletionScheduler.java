package sg.edu.nus.foc.order.application;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAutoCompletionScheduler {
    private final LifecycleProcessingService lifecycle;

    @Scheduled(cron = "${order.lifecycle.auto-completion-cron:0 * * * * *}")
    public void autoCompleteDueOrders() {
        try {
            int completed = lifecycle.autoCompleteDue(Instant.now());
            if (completed > 0) {
                log.info("Automatically completed {} DELIVERED orders after the 48-hour grace period.", completed);
            }
        } catch (RuntimeException exception) {
            log.error("Automatic order-completion pass failed; the next scheduler pass will retry.", exception);
        }
    }
}
