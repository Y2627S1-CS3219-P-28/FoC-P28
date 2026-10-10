package sg.edu.nus.foc.order.application;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;
import sg.edu.nus.foc.order.domain.OrderEventOutbox;
import sg.edu.nus.foc.order.domain.repository.OrderEventOutboxRepository;
import sg.edu.nus.foc.order.messagingpublisher.dto.AcceptedOrderCancellationTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderCompletionTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OpenOrderRefundTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.interfaces.IAcceptedOrderCancellationTaskPublisher;
import sg.edu.nus.foc.order.messagingpublisher.interfaces.IOrderCompletionTaskPublisher;
import sg.edu.nus.foc.order.messagingpublisher.interfaces.IOpenOrderRefundTaskPublisher;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderOutboxDispatcher {
    private static final Duration CLAIM_LEASE = Duration.ofMinutes(2);
    private static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(5);

    private final OrderEventOutboxRepository outbox;
    private final IOrderCompletionTaskPublisher completionPublisher;
    private final IOpenOrderRefundTaskPublisher openRefundPublisher;
    private final IAcceptedOrderCancellationTaskPublisher acceptedCancellationPublisher;
    private final JsonMapper objectMapper;

    @Value("${order.messaging.outbox.batch-size:50}")
    private int batchSize;

    public void dispatch(String eventId) {
        try {
            Instant now = Instant.now();
            Optional<OrderEventOutbox> claimed = outbox.claim(eventId, now, now.plus(CLAIM_LEASE));
            claimed.ifPresent(this::publishClaimed);
        } catch (RuntimeException exception) {
            // Catch outside persistence proxies, including claim/commit/retry-write failures.
            // A committed claim remains recoverable when its lease expires.
            log.error("Order event {} dispatch failed; later events will continue and recovery will retry it.",
                    eventId, exception);
        }
    }

    public void dispatchDueBatch() {
        Instant now = Instant.now();
        List<String> dueEventIds = outbox.findDueIds(now, batchSize);
        for (String eventId : dueEventIds) {
            dispatch(eventId);
        }
    }

    private void publishClaimed(OrderEventOutbox message) {
        try {
            publish(message);
            outbox.markPublished(message.getEventId(), Instant.now());
        } catch (RuntimeException exception) {
            Instant retryAt = Instant.now().plus(retryDelay(message.getAttemptCount()));
            outbox.scheduleRetry(message.getEventId(), retryAt, exception.getMessage());
            log.warn("Order event {} publication failed; retry scheduled for {}", message.getEventId(), retryAt, exception);
        }
    }

    private void publish(OrderEventOutbox message) {
        switch (message.getEventType()) {
            case "OrderCompletionTaskEvent" -> completionPublisher.publishOrderCompletionTask(
                    readCompletion(message));
            case "OpenOrderRefundTaskEvent" -> openRefundPublisher.publishOpenOrderRefundTask(
                    readOpenRefund(message));
            case "AcceptedOrderCancellationTaskEvent" -> acceptedCancellationPublisher.publishAcceptedOrderCancellationTask(
                    objectMapper.readValue(message.getPayload(), AcceptedOrderCancellationTaskEvent.class));
            case "OpenOrderCancellationTaskEvent", "OrderExpirationTaskEvent" -> publishLegacyOpenRefund(message);
            default -> throw new IllegalArgumentException("Unsupported Order event type: " + message.getEventType());
        }
    }

    private void publishLegacyOpenRefund(OrderEventOutbox message) {
        openRefundPublisher.publishOpenOrderRefundTask(readOpenRefund(message));
    }

    private OrderCompletionTaskEvent readCompletion(OrderEventOutbox message) {
        OrderCompletionTaskEvent event = objectMapper.treeToValue(
                compactPayload(message), OrderCompletionTaskEvent.class);
        event.setEventVersion(2);
        event.setOrderVersion(message.getOrderVersion());
        return event;
    }

    private OpenOrderRefundTaskEvent readOpenRefund(OrderEventOutbox message) {
        OpenOrderRefundTaskEvent event = objectMapper.treeToValue(
                compactPayload(message), OpenOrderRefundTaskEvent.class);
        event.setEventType("OpenOrderRefundTaskEvent");
        event.setEventVersion(2);
        event.setOrderVersion(message.getOrderVersion());
        return event;
    }

    private ObjectNode compactPayload(OrderEventOutbox message) {
        ObjectNode payload = (ObjectNode) objectMapper.readTree(message.getPayload());
        JsonNode snapshot = payload.get("order");
        if (snapshot != null && !snapshot.isNull()) {
            if (!snapshot.hasNonNull("status") || !snapshot.hasNonNull("offeredCredits")) {
                throw new IllegalArgumentException("Legacy Order event is missing refund/completion facts.");
            }
            payload.set("orderStatus", snapshot.get("status"));
            payload.set("creditAmount", snapshot.get("offeredCredits"));
            payload.set("courierId", snapshot.get("courierId"));
        }
        return payload;
    }

    private Duration retryDelay(int attemptCount) {
        int exponent = Math.min(Math.max(attemptCount, 1), 8);
        long delaySeconds = Math.min(1L << exponent, MAX_RETRY_DELAY.toSeconds());
        return Duration.ofSeconds(delaySeconds);
    }
}
