package sg.edu.nus.foc.order.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class OrderLifecycleSchedulerTest {
    @Test
    void runsBothChecksInOrderWithOneCapturedTime() {
        LifecycleProcessingService lifecycle = mock(LifecycleProcessingService.class);
        when(lifecycle.expireDue(any())).thenReturn(2);
        when(lifecycle.autoCompleteDue(any())).thenReturn(3);
        new OrderLifecycleScheduler(lifecycle).processDueOrders();

        ArgumentCaptor<Instant> expiry = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> completion = ArgumentCaptor.forClass(Instant.class);
        InOrder inOrder = inOrder(lifecycle);
        inOrder.verify(lifecycle).expireDue(expiry.capture());
        inOrder.verify(lifecycle).autoCompleteDue(completion.capture());
        assertEquals(expiry.getValue(), completion.getValue());
        verifyNoMoreInteractions(lifecycle);
    }

    @Test
    void expiryFailureDoesNotSkipCompletion() {
        LifecycleProcessingService lifecycle = mock(LifecycleProcessingService.class);
        when(lifecycle.expireDue(any())).thenThrow(new IllegalStateException("expiry unavailable"));
        when(lifecycle.autoCompleteDue(any())).thenReturn(1);
        assertDoesNotThrow(new OrderLifecycleScheduler(lifecycle)::processDueOrders);
        verify(lifecycle).autoCompleteDue(any());
    }

    @Test
    void completionFailureDoesNotPreventNextPass() {
        LifecycleProcessingService lifecycle = mock(LifecycleProcessingService.class);
        when(lifecycle.autoCompleteDue(any())).thenThrow(new IllegalStateException("completion unavailable"));
        OrderLifecycleScheduler scheduler = new OrderLifecycleScheduler(lifecycle);
        assertDoesNotThrow(scheduler::processDueOrders);
        assertDoesNotThrow(scheduler::processDueOrders);
        verify(lifecycle, times(2)).expireDue(any());
        verify(lifecycle, times(2)).autoCompleteDue(any());
    }
}
