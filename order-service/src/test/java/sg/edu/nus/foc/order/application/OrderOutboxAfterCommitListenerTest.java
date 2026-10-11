package sg.edu.nus.foc.order.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class OrderOutboxAfterCommitListenerTest {
    @Test
    void dispatchesRequestedEventAndDoesNotPropagatePostCommitFailure() {
        OrderOutboxDispatcher dispatcher = mock(OrderOutboxDispatcher.class);
        OrderOutboxAfterCommitListener listener = new OrderOutboxAfterCommitListener(dispatcher);
        OrderOutboxDispatchRequested request = new OrderOutboxDispatchRequested("event-1");
        doThrow(new IllegalStateException("database unavailable")).when(dispatcher).dispatch("event-1");

        assertDoesNotThrow(() -> listener.dispatchAfterCommit(request));

        verify(dispatcher).dispatch("event-1");
    }
}
