package sg.edu.nus.foc.order.application.recovery;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Separate from existing lifecycle and outcome-outbox schedulers. */
@Component
@RequiredArgsConstructor
public class OrderCommandRecoveryScheduler {
    private final OrderCommandService commands;
    @Scheduled(cron = "${order.commands.cron:0 * * * * *}")
    public void recover() { commands.recoverDue(); }
}
