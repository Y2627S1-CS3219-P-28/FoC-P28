package sg.edu.nus.foc.order.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class OrderAutoCompletionSchedulerTest {
    @Test
    void runsConfiguredSpringScheduleAndProcessesDueOrders() throws Exception {
        LifecycleProcessingService lifecycle = mock(LifecycleProcessingService.class);
        OrderAutoCompletionScheduler scheduler = new OrderAutoCompletionScheduler(lifecycle);
        when(lifecycle.autoCompleteDue(any(Instant.class))).thenReturn(2);

        scheduler.autoCompleteDueOrders();

        verify(lifecycle).autoCompleteDue(any(Instant.class));
        Scheduled scheduled = OrderAutoCompletionScheduler.class
                .getMethod("autoCompleteDueOrders")
                .getAnnotation(Scheduled.class);
        org.junit.jupiter.api.Assertions.assertEquals(
                "${order.lifecycle.auto-completion-cron:0 * * * * *}",
                scheduled.cron());
    }

    @Test
    void logsAndSurvivesAnAutoCompletionPassFailure() {
        LifecycleProcessingService lifecycle = mock(LifecycleProcessingService.class);
        OrderAutoCompletionScheduler scheduler = new OrderAutoCompletionScheduler(lifecycle);
        when(lifecycle.autoCompleteDue(any(Instant.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertDoesNotThrow(scheduler::autoCompleteDueOrders);
    }
}
