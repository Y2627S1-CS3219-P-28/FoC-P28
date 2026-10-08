package sg.edu.nus.foc.order.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import sg.edu.nus.foc.order.domain.OrderStatus;

class OrderPersistenceAdapterTest {
    @Test
    void requesterVisibilityIsFilteredInBothContentAndCountQueries() throws NoSuchMethodException {
        Query query = JpaOrderRepository.class
                .getMethod("findByRequesterIdOrderByCreatedAtDesc", String.class, Pageable.class)
                .getAnnotation(Query.class);

        assertNotNull(query, "Superseded expired orders must be filtered before database pagination");
        for (String statement : List.of(query.value(), query.countQuery())) {
            assertTrue(statement.contains("o.requesterId = :requesterId"));
            assertTrue(statement.contains("o.repostedOrderId is null"));
            assertTrue(statement.contains("o.status <> sg.edu.nus.foc.order.domain.OrderStatus.EXPIRED"));
        }
    }

    @Test
    void autoCompletionSelectionUsesDatabaseEligibleDeliveredCheckpointCutoff() {
        JpaOrderRepository jpa = mock(JpaOrderRepository.class);
        Instant cutoff = Instant.parse("2026-10-05T10:00:00Z");
        when(jpa.findDueForAutoCompletion(OrderStatus.DELIVERED, cutoff)).thenReturn(List.of());
        OrderPersistenceAdapter adapter = new OrderPersistenceAdapter(jpa);

        adapter.findDueForAutoCompletion(cutoff);

        verify(jpa).findDueForAutoCompletion(OrderStatus.DELIVERED, cutoff);
    }

    @Test
    void absentStatusUsesPagedUnfilteredQuerySortedNewestFirst() {
        JpaOrderRepository jpa = mock(JpaOrderRepository.class);
        when(jpa.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        OrderPersistenceAdapter adapter = new OrderPersistenceAdapter(jpa);

        adapter.findAllOrders(null, 2, 25);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(jpa).findAll(pageable.capture());
        assertEquals(PageRequest.of(2, 25, Sort.by(Sort.Direction.DESC, "createdAt")), pageable.getValue());
    }

    @Test
    void suppliedStatusUsesExactStatusFilterAndClampsThePageSize() {
        JpaOrderRepository jpa = mock(JpaOrderRepository.class);
        when(jpa.findByStatusOrderByCreatedAtDesc(eq(OrderStatus.COMPLETED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        OrderPersistenceAdapter adapter = new OrderPersistenceAdapter(jpa);

        adapter.findAllOrders(OrderStatus.COMPLETED, 0, 200);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(jpa).findByStatusOrderByCreatedAtDesc(eq(OrderStatus.COMPLETED), pageable.capture());
        assertEquals(100, pageable.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), pageable.getValue().getSort());
    }
}
