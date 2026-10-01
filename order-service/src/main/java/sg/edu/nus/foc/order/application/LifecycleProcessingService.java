package sg.edu.nus.foc.order.application;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderCheckpoint;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.domain.repository.OrderCheckpointRepository;
import sg.edu.nus.foc.order.domain.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class LifecycleProcessingService {
    private final OrderRepository orders;
    private final OrderCheckpointRepository checkpoints;
    private final OrderRepostService reposts;
    private final CreditServicePort credits;
    private final OrderAuditLogger audit;

    @Transactional
    public int expireDue(Instant now, String lifecycleAuthorization) {
        List<Order> dueOrders = orders.findDueUnassigned(OrderStatus.OPEN, now);
        for (Order order : dueOrders) {
            long version = order.getVersion();
            order.expire(version, now);

            String commandId = "EXPIRE:" + order.getId();
            credits.release(
                    commandId,
                    order.getId(),
                    order.getRequesterId(),
                    order.getOfferedCredits(),
                    "EXPIRED",
                    version,
                    lifecycleAuthorization);
            audit.dependency("credit-service", "release", order.getId(), "accepted");

            orders.save(order);
            checkpoints.save(new OrderCheckpoint(
                    order.getId(),
                    order.getStatus(),
                    now,
                    "lifecycle",
                    null));
            audit.action("EXPIRE", order.getId(), "lifecycle", commandId, "accepted");
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
