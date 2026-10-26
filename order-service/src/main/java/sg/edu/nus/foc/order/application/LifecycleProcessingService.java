package sg.edu.nus.foc.order.application;

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.domain.repository.OrderRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class LifecycleProcessingService {

    private final OrderRepository orders;
    private final OrderExpiryProcessingService expiry;
    private final OrderRepostService reposts;
    private final OrderAuditLogger audit;
    private final OrderTransitionService transitions;

    public int autoCompleteDue(Instant now) {
        Instant deliveredAtOrBefore = now.minus(Order.AUTOMATIC_COMPLETION_DELAY);
        List<String> dueOrderIds = orders.findDueForAutoCompletionIds(deliveredAtOrBefore);
        int completedCount = 0;
        for (String orderId : dueOrderIds) {
            try {
                if (transitions.autoComplete(orderId, now)) {
                    completedCount++;
                }
            } catch (RuntimeException exception) {
                log.error("Automatic completion failed for order {}; continuing with remaining orders.",
                        orderId, exception);
            }
        }
        return completedCount;
    }

    public int expireDue(Instant now) {
        List<String> dueOrderIds = orders.findDueUnassignedIds(OrderStatus.OPEN, now);
        int expiredCount = 0;
        for (String orderId : dueOrderIds) {
            try {
                if (expiry.expire(orderId, now)) {
                    expiredCount++;
                }
            } catch (RuntimeException exception) {
                log.error("Expiry failed for order {}; continuing with remaining orders.", orderId, exception);
            }
        }
        return expiredCount;
    }

    public int repostDue(Instant now, String lifecycleAuthorization) {
        int repostedCount = 0;
        List<Order> expiredOrders = orders.findDueUnassigned(OrderStatus.EXPIRED, now);
        for (Order order : expiredOrders) {
            if (order.eligibleForAutomaticRepost(now)) {
                String commandId = "AUTO_REPOST:" + order.getId();
                reposts.automatic(commandId, order.getId(), now, lifecycleAuthorization);
                audit.action(
                        "AUTO_REPOST",
                        order.getId(),
                        order.getRequesterId(),
                        commandId,
                        "accepted");
                repostedCount++;
            }
        }
        return repostedCount;
    }
}
