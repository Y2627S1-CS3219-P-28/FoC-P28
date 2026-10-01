package sg.edu.nus.foc.order.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import sg.edu.nus.foc.order.domain.repository.OrderPage;
import sg.edu.nus.foc.order.domain.repository.OrderRepository;

class OrderQueryServiceTest {
    @Test
    void requestedOrdersUseTheRepositoryBoundary() {
        OrderRepository repository = mock(OrderRepository.class);
        OrderPage result = new OrderPage(List.of(), 0, 100, 0, 0);
        when(repository.findRequestedBy("requester", -1, 999)).thenReturn(result);

        new OrderQueryService(repository).requestedBy("requester", -1, 999);

        verify(repository).findRequestedBy("requester", -1, 999);
    }

    @Test
    void courierOrdersUseTheRepositoryBoundary() {
        OrderRepository repository = mock(OrderRepository.class);
        OrderPage result = new OrderPage(List.of(), 2, 20, 0, 0);
        when(repository.findCourierOrders("courier", 2, 20)).thenReturn(result);

        new OrderQueryService(repository).courierFor("courier", 2, 20);

        verify(repository).findCourierOrders("courier", 2, 20);
    }
}
