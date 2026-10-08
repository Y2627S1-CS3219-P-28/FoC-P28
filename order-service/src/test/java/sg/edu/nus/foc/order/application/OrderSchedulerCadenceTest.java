package sg.edu.nus.foc.order.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

class OrderSchedulerCadenceTest {

    @Test
    void expiresOnQuarterHoursAndRecoversEventsHourly() throws Exception {
        LocalDateTime betweenPasses = LocalDateTime.of(2026, 10, 8, 10, 1, 0);
        CronExpression expiry = defaultCron(OrderExpiryScheduler.class, "expireDueOrders");
        CronExpression recovery = defaultCron(OrderOutboxScheduler.class, "dispatchDueEvents");

        assertEquals(LocalDateTime.of(2026, 10, 8, 10, 15, 0), expiry.next(betweenPasses));
        assertEquals(LocalDateTime.of(2026, 10, 8, 11, 0, 0),
                expiry.next(LocalDateTime.of(2026, 10, 8, 10, 59, 0)));
        assertEquals(LocalDateTime.of(2026, 10, 8, 11, 0, 0), recovery.next(betweenPasses));
        assertEquals(LocalDateTime.of(2026, 10, 8, 11, 0, 0),
                recovery.next(LocalDateTime.of(2026, 10, 8, 10, 5, 1)));
    }

    @Test
    void keepsAutoCompletionAtEveryMinute() throws Exception {
        CronExpression completion = defaultCron(
                OrderAutoCompletionScheduler.class, "autoCompleteDueOrders");

        assertEquals(LocalDateTime.of(2026, 10, 8, 10, 2, 0),
                completion.next(LocalDateTime.of(2026, 10, 8, 10, 1, 20)));
    }

    private CronExpression defaultCron(Class<?> schedulerType, String methodName) throws Exception {
        Scheduled scheduled = schedulerType.getMethod(methodName).getAnnotation(Scheduled.class);
        String placeholder = scheduled.cron();
        String expression = placeholder.substring(placeholder.indexOf(':') + 1, placeholder.length() - 1);
        return CronExpression.parse(expression);
    }
}
