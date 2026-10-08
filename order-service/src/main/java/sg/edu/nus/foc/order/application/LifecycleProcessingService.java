package sg.edu.nus.foc.order.application;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderCheckpoint;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.domain.repository.OrderCheckpointRepository;
import sg.edu.nus.foc.order.domain.repository.OrderEventOutboxRepository;
import sg.edu.nus.foc.order.domain.repository.OrderRepository;
import sg.edu.nus.foc.order.messagingpublisher.dto.OpenOrderRefundTaskEvent;

@Service
@RequiredArgsConstructor
public class LifecycleProcessingService {
    private final OrderRepository orders;
    private final OrderCheckpointRepository checkpoints;
    private final OrderRepostService reposts;
    private final OrderEventOutboxRepository outbox;
    private final ApplicationEventPublisher applicationEvents;
    private final OrderTaskEventFactory eventFactory;
    private final OrderAuditLogger audit;
    private final OrderTransitionService transitions;

    @Transactional
    public int autoCompleteDue(Instant now) {
        Instant deliveredAtOrBefore = now.minus(Order.AUTOMATIC_COMPLETION_DELAY);
        List<Order> dueOrders = orders.findDueForAutoCompletion(deliveredAtOrBefore);
        int completedCount = 0;
        for (Order order : dueOrders) {
            if (transitions.autoComplete(order.getId(), now)) {
                completedCount++;
            }
        }
        return completedCount;
    }

    @Transactional
    public int expireDue(Instant now) {
        List<Order> dueOrders = orders.findDueUnassigned(OrderStatus.OPEN, now);
        for (Order order : dueOrders) {
            long version = order.getVersion();
            order.expire(version, now);

            OrderCheckpoint expiredCheckpoint = new OrderCheckpoint(
                    order.getId(),
                    order.getStatus(),
                    now,
                    "lifecycle",
                    null);
            checkpoints.save(expiredCheckpoint);
            Order saved = orders.save(order);

            String commandId = "EXPIRE:" + saved.getId();
            OpenOrderRefundTaskEvent event = eventFactory.openRefund(commandId, saved, now);
            outbox.enqueue(event);
            applicationEvents.publishEvent(new OrderOutboxDispatchRequested(event.getEventId()));
            audit.action("EXPIRE", saved.getId(), "lifecycle", commandId, "accepted");
        }
        return dueOrders.size();
    }

    @Transactional
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
