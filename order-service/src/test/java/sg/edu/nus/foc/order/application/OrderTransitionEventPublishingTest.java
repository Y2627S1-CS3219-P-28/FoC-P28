package sg.edu.nus.foc.order.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import sg.edu.nus.foc.order.domain.CommandReceipt;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderCheckpoint;
import sg.edu.nus.foc.order.domain.OrderProblem;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.domain.repository.CommandReceiptRepository;
import sg.edu.nus.foc.order.domain.repository.OrderCheckpointRepository;
import sg.edu.nus.foc.order.domain.repository.OrderEventOutboxRepository;
import sg.edu.nus.foc.order.domain.repository.OrderRepository;
import sg.edu.nus.foc.order.messagingpublisher.dto.AcceptedOrderCancellationTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderCompletionTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OpenOrderCancellationTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.mapper.OrderTaskEventMapper;

class OrderTransitionEventPublishingTest {
    private static final Instant START = Instant.parse("2026-10-01T10:00:00Z");
    private static final String AUTHORIZATION = "Bearer token";

    @Test
    void commitsCompletionIntentWithResultingOrderAndCheckpoint() {
        Order order = deliveredOrder();
        List<OrderCheckpoint> history = new ArrayList<>(deliveryHistory(order));
        TestDependencies dependencies = dependencies(order, "COMPLETE", "complete-1", history);

        Order result = service(dependencies).complete("complete-1", order.getId(), "requester-1", 0, AUTHORIZATION);

        assertEquals(OrderStatus.COMPLETED, result.getStatus());
        InOrder sequence = inOrder(dependencies.checkpoints, dependencies.orders, dependencies.receipts,
                dependencies.outbox, dependencies.applicationEvents);
        sequence.verify(dependencies.checkpoints).save(any(OrderCheckpoint.class));
        sequence.verify(dependencies.orders).save(order);
        sequence.verify(dependencies.receipts).save(any(CommandReceipt.class));
        ArgumentCaptor<OrderCompletionTaskEvent> event = ArgumentCaptor.forClass(OrderCompletionTaskEvent.class);
        sequence.verify(dependencies.outbox).enqueue(event.capture());
        sequence.verify(dependencies.applicationEvents)
                .publishEvent(new OrderOutboxDispatchRequested(event.getValue().getEventId()));
        assertFalse(event.getValue().isOverdue());
        assertEquals(START.plusSeconds(16 * 60L), event.getValue().getOverdueAt());
        assertEquals(OrderStatus.COMPLETED, event.getValue().getOrder().getStatus());
        assertEquals(1, event.getValue().getOrderVersion());
        assertEquals(1, event.getValue().getOrder().getVersion());
        assertTrue(event.getValue().getOrder().getCheckpoints().stream()
                .anyMatch(checkpoint -> checkpoint.getStatus() == OrderStatus.COMPLETED));
    }

    @Test
    void completionOutboxFailureStopsBeforeRequestingPostCommitDispatch() {
        Order order = deliveredOrder();
        TestDependencies dependencies = dependencies(order, "COMPLETE", "complete-outbox-fail", deliveryHistory(order));
        doThrow(new IllegalStateException("database unavailable"))
                .when(dependencies.outbox).enqueue(any(OrderTaskEvent.class));

        assertThrows(IllegalStateException.class,
                () -> service(dependencies).complete(
                        "complete-outbox-fail", order.getId(), "requester-1", 0, AUTHORIZATION));

        verify(dependencies.applicationEvents, never()).publishEvent(any(Object.class));
    }

    @Test
    void completionEventCarriesOverdueFactsForTheSameEventType() {
        Order order = deliveredOrder();
        List<OrderCheckpoint> history = List.of(
                checkpoint(order, OrderStatus.ACCEPTED, START),
                checkpoint(order, OrderStatus.DELIVERED, START.plusSeconds(16 * 60L)));
        TestDependencies dependencies = dependencies(order, "COMPLETE", "complete-overdue", history);

        service(dependencies).complete("complete-overdue", order.getId(), "requester-1", 0, AUTHORIZATION);

        ArgumentCaptor<OrderCompletionTaskEvent> event = ArgumentCaptor.forClass(OrderCompletionTaskEvent.class);
        verify(dependencies.outbox).enqueue(event.capture());
        assertTrue(event.getValue().isOverdue());
        assertEquals(START.plusSeconds(15 * 60L), event.getValue().getOverdueAt());
        assertEquals("OrderCompletionTaskEvent", event.getValue().getEventType());
    }

    @Test
    void openCancellationCommitsFullResultingEventToOutbox() {
        Order order = Order.open("requester-1", "item", "pickup", "delivery", 3, 15, START, START.plusSeconds(3600));
        TestDependencies dependencies = dependencies(order, "CANCEL", "cancel-1", List.of());

        Order result = service(dependencies).cancel("cancel-1", order.getId(), "requester-1", 0, AUTHORIZATION);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        ArgumentCaptor<OpenOrderCancellationTaskEvent> event = ArgumentCaptor.forClass(OpenOrderCancellationTaskEvent.class);
        verify(dependencies.outbox).enqueue(event.capture());
        assertEquals(OrderStatus.CANCELLED, event.getValue().getOrder().getStatus());
        verify(dependencies.applicationEvents).publishEvent(any(OrderOutboxDispatchRequested.class));
    }

    @Test
    void unexpiredAcceptedCancellationHoldsCreditBeforeReopeningWithoutPublishing() {
        Instant now = Instant.now();
        Order order = Order.open("requester-1", "item", "pickup", "delivery", 3, 15,
                now.minusSeconds(60), now.plusSeconds(3600));
        order.accept("courier-1", 0, now.minusSeconds(30));
        TestDependencies dependencies = dependencies(
                order,
                "CANCEL_ACCEPTED",
                "cancel-accepted-1",
                List.of(checkpoint(order, OrderStatus.ACCEPTED, now.minusSeconds(30))));

        Order result = service(dependencies).cancelAccepted(
                "cancel-accepted-1", order.getId(), "courier-1", 0, AUTHORIZATION);

        assertEquals(OrderStatus.OPEN, result.getStatus());
        assertEquals(null, result.getCourierId());
        InOrder sequence = inOrder(dependencies.credits, dependencies.checkpoints, dependencies.orders, dependencies.receipts);
        sequence.verify(dependencies.credits).holdForReopen(
                "cancel-accepted-1:HOLD_FOR_REOPEN", order.getId(), "requester-1", "courier-1", 3, 0, AUTHORIZATION);
        sequence.verify(dependencies.checkpoints).save(any(OrderCheckpoint.class));
        sequence.verify(dependencies.orders).save(order);
        sequence.verify(dependencies.receipts).save(any(CommandReceipt.class));
        verify(dependencies.outbox, never()).enqueue(any(OrderTaskEvent.class));
        verify(dependencies.applicationEvents, never()).publishEvent(any(Object.class));
    }

    @Test
    void expiredAcceptedCancellationPublishesWithoutCreditHold() {
        Order order = Order.open("requester-1", "item", "pickup", "delivery", 3, 15,
                START, START.plusSeconds(3600));
        order.accept("courier-1", 0, START.plusSeconds(1));
        TestDependencies dependencies = dependencies(
                order,
                "CANCEL_ACCEPTED",
                "cancel-accepted-expired",
                List.of(checkpoint(order, OrderStatus.ACCEPTED, START.plusSeconds(1))));

        Order result = service(dependencies).cancelAccepted(
                "cancel-accepted-expired", order.getId(), "courier-1", 0, AUTHORIZATION);

        assertEquals(OrderStatus.ABORTED, result.getStatus());
        verify(dependencies.credits, never()).holdForReopen(
                any(), any(), any(), any(), anyLong(), anyLong(), any());
        ArgumentCaptor<AcceptedOrderCancellationTaskEvent> event =
                ArgumentCaptor.forClass(AcceptedOrderCancellationTaskEvent.class);
        verify(dependencies.outbox).enqueue(event.capture());
        assertEquals(OrderStatus.ABORTED, event.getValue().getOrder().getStatus());
        assertEquals(null, event.getValue().getOrder().getCourierId());
    }

    @Test
    void creditHoldFailureLeavesAcceptedOrderUnchangedAndDoesNotWriteCancellation() {
        Instant now = Instant.now();
        Order order = Order.open("requester-1", "item", "pickup", "delivery", 3, 15,
                now.minusSeconds(60), now.plusSeconds(3600));
        order.accept("courier-1", 0, now.minusSeconds(30));
        TestDependencies dependencies = dependencies(order, "CANCEL_ACCEPTED", "cancel-credit-fail", List.of());
        doThrow(new IllegalStateException("Credit unavailable"))
                .when(dependencies.credits).holdForReopen(any(), any(), any(), any(), anyLong(), anyLong(), any());

        assertThrows(IllegalStateException.class,
                () -> service(dependencies).cancelAccepted(
                        "cancel-credit-fail", order.getId(), "courier-1", 0, AUTHORIZATION));

        assertEquals(OrderStatus.ACCEPTED, order.getStatus());
        assertEquals("courier-1", order.getCourierId());
        verify(dependencies.checkpoints, never()).save(any(OrderCheckpoint.class));
        verify(dependencies.orders, never()).save(any(Order.class));
        verify(dependencies.receipts, never()).save(any(CommandReceipt.class));
        verify(dependencies.outbox, never()).enqueue(any(OrderTaskEvent.class));
        verify(dependencies.applicationEvents, never()).publishEvent(any(Object.class));
    }

    @Test
    void requesterCannotCancelAcceptedOrderAsCourier() {
        Order order = Order.open("requester-1", "item", "pickup", "delivery", 3, 15, START, START.plusSeconds(3600));
        order.accept("courier-1", 0, START.plusSeconds(1));
        TestDependencies dependencies = dependencies(
                order, "CANCEL_ACCEPTED", "cancel-accepted-requester", List.of());
        when(dependencies.users.verifyCourier("requester-1", AUTHORIZATION)).thenReturn("requester-1");

        OrderProblem problem = assertThrows(OrderProblem.class,
                () -> service(dependencies).cancelAccepted(
                        "cancel-accepted-requester", order.getId(),  "requester-1", 0, AUTHORIZATION));

        assertEquals("FORBIDDEN", problem.getCode());
        verify(dependencies.credits, never()).holdForReopen(
                any(), any(), any(), any(), anyLong(), anyLong(), any());
        verify(dependencies.outbox, never()).enqueue(any(OrderTaskEvent.class));
        verify(dependencies.applicationEvents, never()).publishEvent(any(Object.class));
    }

    private TestDependencies dependencies(
            Order order,
            String operation,
            String commandId,
            List<OrderCheckpoint> initialHistory) {
        OrderRepository orders = mock(OrderRepository.class);
        OrderCheckpointRepository checkpoints = mock(OrderCheckpointRepository.class);
        CommandReceiptRepository receipts = mock(CommandReceiptRepository.class);
        UserServicePort users = mock(UserServicePort.class);
        CreditServicePort credits = mock(CreditServicePort.class);
        OrderEventOutboxRepository outbox = mock(OrderEventOutboxRepository.class);
        ApplicationEventPublisher applicationEvents = mock(ApplicationEventPublisher.class);
        List<OrderCheckpoint> history = new ArrayList<>(initialHistory);
        when(receipts.findExisting(operation, commandId)).thenReturn(Optional.empty());
        when(users.verifyRequester("requester-1", AUTHORIZATION)).thenReturn("requester-1");
        when(users.verifyCourier("courier-1", AUTHORIZATION)).thenReturn("courier-1");
        when(orders.getForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(checkpoints.findByOrderId(order.getId())).thenAnswer(invocation -> List.copyOf(history));
        doAnswer(invocation -> {
            OrderCheckpoint checkpoint = invocation.getArgument(0);
            history.add(checkpoint);
            return checkpoint;
        }).when(checkpoints).save(any(OrderCheckpoint.class));
        return new TestDependencies(orders, checkpoints, receipts, users, credits, outbox, applicationEvents);
    }

    private OrderTransitionService service(TestDependencies dependencies) {
        return new OrderTransitionService(
                dependencies.orders,
                dependencies.checkpoints,
                dependencies.receipts,
                dependencies.users,
                dependencies.credits,
                dependencies.outbox,
                dependencies.applicationEvents,
                new OrderTaskEventFactory(
                        dependencies.checkpoints,
                        Mappers.getMapper(OrderTaskEventMapper.class)),
                mock(OrderAuditLogger.class));
    }

    private Order deliveredOrder() {
        Order order = Order.open("requester-1", "item", "pickup", "delivery", 3, 15, START, START.plusSeconds(3600));
        order.accept("courier-1", 0, START.plusSeconds(60));
        order.start("courier-1", 0);
        order.markPickedUp("courier-1", 0);
        order.markDelivered("courier-1", 0);
        return order;
    }

    private List<OrderCheckpoint> deliveryHistory(Order order) {
        return List.of(
                checkpoint(order, OrderStatus.ACCEPTED, START.plusSeconds(60)),
                checkpoint(order, OrderStatus.DELIVERED, START.plusSeconds(10 * 60L)));
    }

    private OrderCheckpoint checkpoint(Order order, OrderStatus status, Instant occurredAt) {
        return new OrderCheckpoint(order.getId(), status, occurredAt, "courier-1", null);
    }

    private record TestDependencies(
            OrderRepository orders,
            OrderCheckpointRepository checkpoints,
            CommandReceiptRepository receipts,
            UserServicePort users,
            CreditServicePort credits,
            OrderEventOutboxRepository outbox,
            ApplicationEventPublisher applicationEvents) {
    }
}
