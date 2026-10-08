package sg.edu.nus.foc.order.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;
import sg.edu.nus.foc.order.domain.OrderEventOutbox;
import sg.edu.nus.foc.order.domain.OutboxState;
import sg.edu.nus.foc.order.domain.repository.OrderEventOutboxRepository;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderCompletionTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OpenOrderRefundTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.interfaces.IAcceptedOrderCancellationTaskPublisher;
import sg.edu.nus.foc.order.messagingpublisher.interfaces.IOrderCompletionTaskPublisher;
import sg.edu.nus.foc.order.messagingpublisher.interfaces.IOpenOrderRefundTaskPublisher;

class OrderOutboxDispatcherTest {
    private static final Instant CREATED_AT = Instant.parse("2026-10-01T10:00:00Z");

    @Test
    void dispatchesClaimedCompletionAndMarksItPublished() {
        OrderEventOutboxRepository repository = mock(OrderEventOutboxRepository.class);
        IOrderCompletionTaskPublisher completion = mock(IOrderCompletionTaskPublisher.class);
        OrderCompletionTaskEvent event = completionEvent();
        OrderEventOutbox message = claimed(event.getEventId(), event.getEventType(), event);
        when(repository.claim(eq(event.getEventId()), any(Instant.class), any(Instant.class)))
                .thenReturn(Optional.of(message));
        OrderOutboxDispatcher dispatcher = dispatcher(repository, completion);

        dispatcher.dispatch(event.getEventId());

        ArgumentCaptor<OrderCompletionTaskEvent> published = ArgumentCaptor.forClass(OrderCompletionTaskEvent.class);
        verify(completion).publishOrderCompletionTask(published.capture());
        assertEquals(event.getEventId(), published.getValue().getEventId());
        verify(repository).markPublished(eq(event.getEventId()), any(Instant.class));
        verify(repository, never()).scheduleRetry(any(), any(), any());
    }

    @Test
    void publisherFailureSchedulesRetryAndDoesNotEscapeDispatcher() {
        OrderEventOutboxRepository repository = mock(OrderEventOutboxRepository.class);
        IOrderCompletionTaskPublisher completion = mock(IOrderCompletionTaskPublisher.class);
        OrderCompletionTaskEvent event = completionEvent();
        OrderEventOutbox message = claimed(event.getEventId(), event.getEventType(), event);
        when(repository.claim(eq(event.getEventId()), any(Instant.class), any(Instant.class)))
                .thenReturn(Optional.of(message));
        doThrow(new IllegalStateException("Pub/Sub unavailable"))
                .when(completion).publishOrderCompletionTask(any(OrderCompletionTaskEvent.class));

        dispatcher(repository, completion).dispatch(event.getEventId());

        ArgumentCaptor<Instant> retryAt = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<String> error = ArgumentCaptor.forClass(String.class);
        verify(repository).scheduleRetry(eq(event.getEventId()), retryAt.capture(), error.capture());
        assertTrue(retryAt.getValue().isAfter(Instant.now()));
        assertTrue(retryAt.getValue().isBefore(Instant.now().plusSeconds(300)));
        assertEquals("Pub/Sub unavailable", error.getValue());
        verify(repository, never()).markPublished(any(), any());
    }

    @Test
    void dispatchDoesNothingWhenAnotherWorkerOwnsTheLease() {
        OrderEventOutboxRepository repository = mock(OrderEventOutboxRepository.class);
        String eventId = "event-locked";
        when(repository.claim(eq(eventId), any(Instant.class), any(Instant.class))).thenReturn(Optional.empty());

        dispatcher(repository, mock(IOrderCompletionTaskPublisher.class)).dispatch(eventId);

        verify(repository, never()).markPublished(any(), any());
        verify(repository, never()).scheduleRetry(any(), any(), any());
    }

    @Test
    void dispatchesOpenRefundThroughItsTypedPublisher() {
        OrderEventOutboxRepository repository = mock(OrderEventOutboxRepository.class);
        IOpenOrderRefundTaskPublisher publisher = mock(IOpenOrderRefundTaskPublisher.class);
        OpenOrderRefundTaskEvent event = new OpenOrderRefundTaskEvent();
        event.setEventId("cancel-event-1");
        event.setEventType("OpenOrderRefundTaskEvent");
        event.setOrderId("order-1");
        event.setOccurredAt(CREATED_AT);
        OrderEventOutbox message = claimed(event.getEventId(), event.getEventType(), event);
        when(repository.claim(eq(event.getEventId()), any(Instant.class), any(Instant.class)))
                .thenReturn(Optional.of(message));

        dispatcher(repository, mock(IOrderCompletionTaskPublisher.class), publisher)
                .dispatch(event.getEventId());

        verify(publisher).publishOpenOrderRefundTask(any(OpenOrderRefundTaskEvent.class));
        verify(repository).markPublished(eq(event.getEventId()), any(Instant.class));
    }

    @Test
    void routesPendingLegacyExpirationOutboxEventToTheOpenRefundTopic() {
        OrderEventOutboxRepository repository = mock(OrderEventOutboxRepository.class);
        IOpenOrderRefundTaskPublisher publisher = mock(IOpenOrderRefundTaskPublisher.class);
        String payload = """
                {"eventId":"expiry-event-1","eventType":"OrderExpirationTaskEvent","eventVersion":1,
                 "orderId":"order-expired","orderVersion":2,"occurredAt":"2026-10-01T10:00:00Z",
                 "actorId":"lifecycle","order":{"id":"order-expired","status":"EXPIRED",
                 "offeredCredits":4,"deliveryTimeLimitMinutes":20,"version":2}}
                """;
        OrderEventOutbox message = OrderEventOutbox.pending(
                "expiry-event-1", "order-expired", "OrderExpirationTaskEvent", 1, 2, payload, CREATED_AT);
        message.claim(CREATED_AT, CREATED_AT.plusSeconds(30));
        when(repository.claim(eq("expiry-event-1"), any(Instant.class), any(Instant.class)))
                .thenReturn(Optional.of(message));

        dispatcher(repository, mock(IOrderCompletionTaskPublisher.class), publisher).dispatch("expiry-event-1");

        ArgumentCaptor<OpenOrderRefundTaskEvent> captured = ArgumentCaptor.forClass(OpenOrderRefundTaskEvent.class);
        verify(publisher).publishOpenOrderRefundTask(captured.capture());
        assertEquals("expiry-event-1", captured.getValue().getEventId());
        assertEquals("OpenOrderRefundTaskEvent", captured.getValue().getEventType());
        verify(repository).markPublished(eq("expiry-event-1"), any(Instant.class));
    }

    @Test
    void routesPendingLegacyCancellationOutboxEventToTheOpenRefundTopic() {
        OrderEventOutboxRepository repository = mock(OrderEventOutboxRepository.class);
        IOpenOrderRefundTaskPublisher publisher = mock(IOpenOrderRefundTaskPublisher.class);
        String payload = """
                {"eventId":"cancel-event-legacy","eventType":"OpenOrderCancellationTaskEvent","eventVersion":1,
                 "orderId":"order-cancelled","orderVersion":2,"occurredAt":"2026-10-01T10:00:00Z",
                 "actorId":"requester-1","order":{"id":"order-cancelled","status":"CANCELLED",
                 "offeredCredits":4,"deliveryTimeLimitMinutes":20,"version":2}}
                """;
        OrderEventOutbox message = OrderEventOutbox.pending(
                "cancel-event-legacy", "order-cancelled", "OpenOrderCancellationTaskEvent", 1, 2, payload, CREATED_AT);
        message.claim(CREATED_AT, CREATED_AT.plusSeconds(30));
        when(repository.claim(eq("cancel-event-legacy"), any(Instant.class), any(Instant.class)))
                .thenReturn(Optional.of(message));

        dispatcher(repository, mock(IOrderCompletionTaskPublisher.class), publisher)
                .dispatch("cancel-event-legacy");

        ArgumentCaptor<OpenOrderRefundTaskEvent> captured = ArgumentCaptor.forClass(OpenOrderRefundTaskEvent.class);
        verify(publisher).publishOpenOrderRefundTask(captured.capture());
        assertEquals("cancel-event-legacy", captured.getValue().getEventId());
        assertEquals("OpenOrderRefundTaskEvent", captured.getValue().getEventType());
        verify(repository).markPublished(eq("cancel-event-legacy"), any(Instant.class));
    }

    private OrderOutboxDispatcher dispatcher(
            OrderEventOutboxRepository repository,
            IOrderCompletionTaskPublisher completion) {
        return dispatcher(repository, completion, mock(IOpenOrderRefundTaskPublisher.class));
    }

    private OrderOutboxDispatcher dispatcher(
            OrderEventOutboxRepository repository,
            IOrderCompletionTaskPublisher completion,
            IOpenOrderRefundTaskPublisher openRefund) {
        return new OrderOutboxDispatcher(
                repository,
                completion,
                openRefund,
                mock(IAcceptedOrderCancellationTaskPublisher.class),
                JsonMapper.builder().build());
    }

    private OrderEventOutbox claimed(String eventId, String eventType, Object event) {
        JsonMapper mapper = JsonMapper.builder().build();
        OrderTaskEvent taskEvent = (OrderTaskEvent) event;
        OrderEventOutbox outbox = OrderEventOutbox.pending(
                eventId,
                taskEvent.getOrderId(),
                eventType,
                taskEvent.getEventVersion(),
                taskEvent.getOrderVersion(),
                mapper.writeValueAsString(event),
                CREATED_AT);
        outbox.claim(CREATED_AT, CREATED_AT.plusSeconds(30));
        assertEquals(OutboxState.IN_PROGRESS, outbox.getState());
        return outbox;
    }

    private OrderCompletionTaskEvent completionEvent() {
        OrderCompletionTaskEvent event = new OrderCompletionTaskEvent();
        event.setEventId("event-1");
        event.setEventType("OrderCompletionTaskEvent");
        event.setEventVersion(1);
        event.setOrderId("order-1");
        event.setOrderVersion(2);
        event.setOccurredAt(CREATED_AT);
        event.setActorId("requester-1");
        return event;
    }
}
