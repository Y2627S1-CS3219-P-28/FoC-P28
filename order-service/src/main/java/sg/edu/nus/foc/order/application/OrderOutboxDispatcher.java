package sg.edu.nus.foc.order.application;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import sg.edu.nus.foc.order.domain.OrderEventOutbox;
import sg.edu.nus.foc.order.domain.repository.OrderEventOutboxRepository;
import sg.edu.nus.foc.order.messagingpublisher.dto.AcceptedOrderCancellationTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderCompletionTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OpenOrderCancellationTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.interfaces.IAcceptedOrderCancellationTaskPublisher;
import sg.edu.nus.foc.order.messagingpublisher.interfaces.IOrderCompletionTaskPublisher;
import sg.edu.nus.foc.order.messagingpublisher.interfaces.IOpenOrderCancellationTaskPublisher;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderOutboxDispatcher {
    private static final Duration CLAIM_LEASE = Duration.ofMinutes(2);
    private static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(5);

    private final OrderEventOutboxRepository outbox;
    private final IOrderCompletionTaskPublisher completionPublisher;
    private final IOpenOrderCancellationTaskPublisher openCancellationPublisher;
    private final IAcceptedOrderCancellationTaskPublisher acceptedCancellationPublisher;
    private final JsonMapper objectMapper;

    @Value("${order.messaging.outbox.batch-size:50}")
    private int batchSize;

    public void dispatch(String eventId) {
        Instant now = Instant.now();
        Optional<OrderEventOutbox> claimed = outbox.claim(eventId, now, now.plus(CLAIM_LEASE));
        claimed.ifPresent(this::publishClaimed);
    }

    public void dispatchDueBatch() {
        Instant now = Instant.now();
        List<OrderEventOutbox> claimed = outbox.claimDue(now, now.plus(CLAIM_LEASE), batchSize);
        claimed.forEach(this::publishClaimed);
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
                    objectMapper.readValue(message.getPayload(), OrderCompletionTaskEvent.class));
            case "OpenOrderCancellationTaskEvent" -> openCancellationPublisher.publishOpenOrderCancellationTask(
                    objectMapper.readValue(message.getPayload(), OpenOrderCancellationTaskEvent.class));
            case "AcceptedOrderCancellationTaskEvent" -> acceptedCancellationPublisher.publishAcceptedOrderCancellationTask(
                    objectMapper.readValue(message.getPayload(), AcceptedOrderCancellationTaskEvent.class));
            default -> throw new IllegalArgumentException("Unsupported Order event type: " + message.getEventType());
        }
    }

    private Duration retryDelay(int attemptCount) {
        int exponent = Math.min(Math.max(attemptCount, 1), 8);
        long delaySeconds = Math.min(1L << exponent, MAX_RETRY_DELAY.toSeconds());
        return Duration.ofSeconds(delaySeconds);
    }
}
