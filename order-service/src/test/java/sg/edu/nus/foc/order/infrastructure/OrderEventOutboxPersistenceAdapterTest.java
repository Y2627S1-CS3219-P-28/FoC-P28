package sg.edu.nus.foc.order.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;
import sg.edu.nus.foc.order.domain.OrderEventOutbox;
import sg.edu.nus.foc.order.domain.OutboxState;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderCompletionTaskEvent;

class OrderEventOutboxPersistenceAdapterTest {
    @Test
    void serializesTypedEventWithStableMetadataAsPendingOutboxRow() {
        JpaOrderEventOutboxRepository repository = mock(JpaOrderEventOutboxRepository.class);
        JsonMapper mapper = JsonMapper.builder().build();
        OrderEventOutboxPersistenceAdapter adapter = new OrderEventOutboxPersistenceAdapter(repository, mapper);
        OrderCompletionTaskEvent event = new OrderCompletionTaskEvent();
        event.setEventId("event-1");
        event.setEventType("OrderCompletionTaskEvent");
        event.setEventVersion(1);
        event.setOrderId("order-1");
        event.setOrderVersion(3);
        event.setOccurredAt(Instant.parse("2026-10-01T10:00:00Z"));
        event.setActorId("requester-1");
        when(repository.save(any(OrderEventOutbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adapter.enqueue(event);

        ArgumentCaptor<OrderEventOutbox> outbox = ArgumentCaptor.forClass(OrderEventOutbox.class);
        verify(repository).save(outbox.capture());
        assertEquals(event.getEventId(), outbox.getValue().getEventId());
        assertEquals(event.getOrderId(), outbox.getValue().getOrderId());
        assertEquals(event.getEventType(), outbox.getValue().getEventType());
        assertEquals(event.getEventVersion(), outbox.getValue().getEventVersion());
        assertEquals(event.getOrderVersion(), outbox.getValue().getOrderVersion());
        assertEquals(OutboxState.PENDING, outbox.getValue().getState());
        assertEquals(event.getEventId(), mapper.readValue(
                outbox.getValue().getPayload(), OrderCompletionTaskEvent.class).getEventId());
    }
}
