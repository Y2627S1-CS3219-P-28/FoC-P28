package sg.edu.nus.foc.order.application;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.repository.OrderCheckpointRepository;
import sg.edu.nus.foc.order.messagingpublisher.dto.AcceptedOrderCancellationTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderCompletionTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderEventSnapshot;
import sg.edu.nus.foc.order.messagingpublisher.dto.OpenOrderCancellationTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.mapper.OrderTaskEventMapper;

@Component
@RequiredArgsConstructor
public class OrderTaskEventFactory {
    private final OrderCheckpointRepository checkpoints;
    private final OrderTaskEventMapper mapper;

    public OrderCompletionTaskEvent completion(
            String commandId,
            Order order,
            String actorId,
            Instant occurredAt,
            boolean overdue,
            Instant overdueAt) {
        long resultingVersion = order.getVersion() + 1;
        OrderEventSnapshot snapshot = snapshot(order, resultingVersion);
        return mapper.toCompletionEvent(
                eventId("OrderCompletionTaskEvent", commandId),
                order,
                resultingVersion,
                actorId,
                occurredAt,
                overdue,
                overdueAt,
                snapshot);
    }

    public OpenOrderCancellationTaskEvent openCancellation(
            String commandId,
            Order order,
            String actorId,
            Instant occurredAt) {
        long resultingVersion = order.getVersion() + 1;
        OrderEventSnapshot snapshot = snapshot(order, resultingVersion);
        return mapper.toOpenCancellationEvent(
                eventId("OpenOrderCancellationTaskEvent", commandId),
                order,
                resultingVersion,
                actorId,
                occurredAt,
                snapshot);
    }

    public AcceptedOrderCancellationTaskEvent acceptedCancellation(
            String commandId,
            Order order,
            String actorId,
            Instant occurredAt) {
        long resultingVersion = order.getVersion() + 1;
        OrderEventSnapshot snapshot = snapshot(order, resultingVersion);
        return mapper.toAcceptedCancellationEvent(
                eventId("AcceptedOrderCancellationTaskEvent", commandId),
                order,
                resultingVersion,
                actorId,
                occurredAt,
                snapshot);
    }

    private OrderEventSnapshot snapshot(Order order, long resultingVersion) {
        return mapper.toSnapshot(order, checkpoints.findByOrderId(order.getId()), resultingVersion);
    }

    private String eventId(String eventType, String commandId) {
        byte[] identity = (eventType + ":" + commandId).getBytes(StandardCharsets.UTF_8);
        return UUID.nameUUIDFromBytes(identity).toString();
    }
}
