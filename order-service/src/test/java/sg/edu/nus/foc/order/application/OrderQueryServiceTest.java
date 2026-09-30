package sg.edu.nus.foc.order.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.infrastructure.OrderRepository;

class OrderQueryServiceTest {
    @Test
    void requestedOrdersUseTheRequesterIndexAndBoundPageSize() {
        OrderRepository repository = mock(OrderRepository.class);
        when(repository.findByRequesterIdOrderByCreatedAtDesc(eq("requester"), any())).thenReturn(new PageImpl<>(List.of()));

        new OrderQueryService(repository).requestedBy("requester", -1, 999);

        ArgumentCaptor<org.springframework.data.domain.Pageable> page = ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(repository).findByRequesterIdOrderByCreatedAtDesc(eq("requester"), page.capture());
        assertEquals(0, page.getValue().getPageNumber());
        assertEquals(100, page.getValue().getPageSize());
    }

    @Test
    void courierOrdersUseTheCourierIndex() {
        OrderRepository repository = mock(OrderRepository.class);
        when(repository.findByCourierIdOrderByCreatedAtDesc(eq("courier"), any())).thenReturn(new PageImpl<>(List.of()));

        new OrderQueryService(repository).courierFor("courier", 2, 20);

        verify(repository).findByCourierIdOrderByCreatedAtDesc(eq("courier"), argThat(page -> page.getPageNumber() == 2 && page.getPageSize() == 20));
    }
}
