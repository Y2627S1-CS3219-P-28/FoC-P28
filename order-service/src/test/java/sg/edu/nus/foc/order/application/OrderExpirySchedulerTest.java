package sg.edu.nus.foc.order.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class OrderExpirySchedulerTest {
    @Test
    void runsConfiguredSpringScheduleAndCallsLifecycleExpiry() throws Exception {
        LifecycleProcessingService lifecycle = mock(LifecycleProcessingService.class);
        OrderExpiryScheduler scheduler = new OrderExpiryScheduler(lifecycle);
        when(lifecycle.expireDue(any(Instant.class))).thenReturn(3);

        scheduler.expireDueOrders();

        verify(lifecycle).expireDue(any(Instant.class));
        Scheduled scheduled = OrderExpiryScheduler.class
                .getMethod("expireDueOrders")
                .getAnnotation(Scheduled.class);
        org.junit.jupiter.api.Assertions.assertEquals(
                "${order.lifecycle.expiry-cron:0 * * * * *}", scheduled.cron());
    }

    @Test
    void logsAndSurvivesAnExpiryPassFailure() {
        LifecycleProcessingService lifecycle = mock(LifecycleProcessingService.class);
        OrderExpiryScheduler scheduler = new OrderExpiryScheduler(lifecycle);
        when(lifecycle.expireDue(any(Instant.class))).thenThrow(new IllegalStateException("database unavailable"));

        assertDoesNotThrow(scheduler::expireDueOrders);
    }
}
