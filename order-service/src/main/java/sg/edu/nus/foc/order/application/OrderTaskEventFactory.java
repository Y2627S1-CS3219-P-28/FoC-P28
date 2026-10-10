package sg.edu.nus.foc.order.application;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.messagingpublisher.dto.AcceptedOrderCancellationTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderCompletionTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderEventSnapshot;
import sg.edu.nus.foc.order.messagingpublisher.dto.OpenOrderRefundTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.mapper.OrderTaskEventMapper;

@Component
@RequiredArgsConstructor
public class OrderTaskEventFactory {
    private final OrderTaskEventMapper mapper;

    public OrderCompletionTaskEvent completion(
            String commandId,
            Order order,
            String actorId,
            Instant occurredAt,
            boolean overdue,
            Instant overdueAt) {
        long resultingVersion = order.getVersion() + 1;
        return mapper.toCompletionEvent(
                eventId("OrderCompletionTaskEvent", commandId), order, resultingVersion, occurredAt);
    }

    public OpenOrderRefundTaskEvent openRefund(
            String commandId,
            Order order,
            String actorId,
            Instant occurredAt) {
        long resultingVersion = order.getVersion() + 1;
        return mapper.toOpenRefundEvent(
                eventId("OpenOrderRefundTaskEvent", commandId), order, resultingVersion, occurredAt);
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

    public OpenOrderRefundTaskEvent openRefund(
            String commandId,
            Order order,
            Instant occurredAt) {
        long resultingVersion = order.getVersion() + 1;
        return mapper.toOpenRefundEvent(
                eventId("OpenOrderRefundTaskEvent", commandId), order, resultingVersion, occurredAt);
    }

    private OrderEventSnapshot snapshot(Order order, long resultingVersion) {
        return mapper.toSnapshot(order, resultingVersion);
    }

    private String eventId(String eventType, String commandId) {
        byte[] identity = (eventType + ":" + commandId).getBytes(StandardCharsets.UTF_8);
        return UUID.nameUUIDFromBytes(identity).toString();
    }
}
