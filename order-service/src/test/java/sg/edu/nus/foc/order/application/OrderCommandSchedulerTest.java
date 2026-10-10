package sg.edu.nus.foc.order.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import sg.edu.nus.foc.order.application.recovery.*;

class OrderCommandSchedulerTest {
    @Test void minuteScanDelegatesToTheHardGatedCoordinator() throws Exception {
        var service = mock(OrderCommandService.class);
        new OrderCommandRecoveryScheduler(service).recover();
        verify(service).recoverDue();
        assertEquals("${order.commands.cron:0 * * * * *}",
                OrderCommandRecoveryScheduler.class.getMethod("recover").getAnnotation(Scheduled.class).cron());
    }
}
