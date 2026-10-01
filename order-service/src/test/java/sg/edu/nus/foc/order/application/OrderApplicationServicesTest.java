package sg.edu.nus.foc.order.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import sg.edu.nus.foc.order.adapter.MockPeerAdapters;
import sg.edu.nus.foc.order.domain.*;
import sg.edu.nus.foc.order.domain.repository.CommandReceiptRepository;
import sg.edu.nus.foc.order.domain.repository.OrderCheckpointRepository;
import sg.edu.nus.foc.order.domain.repository.OrderRepository;

class OrderApplicationServicesTest {
    private static final Instant START = Instant.parse("2026-09-30T00:00:00Z");
    private static final String AUTH = "Bearer token";

    @Test
    void creationReservesCreditsPersistsCheckpointAndReceipt() {
        OrderRepository orders = mock(OrderRepository.class);
        CommandReceiptRepository receipts = mock(CommandReceiptRepository.class);
        OrderCheckpointRepository checkpoints = mock(OrderCheckpointRepository.class);
        UserServicePort users = mock(UserServicePort.class);
        SupplierServicePort suppliers = mock(SupplierServicePort.class);
        CreditServicePort credits = mock(CreditServicePort.class);
        OrderAuditLogger audit = mock(OrderAuditLogger.class);
        when(receipts.findExisting("CREATE", "create-1")).thenReturn(Optional.empty());
        when(users.verifyRequester("requester", AUTH)).thenReturn("requester");
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = new OrderCreationService(orders, receipts, checkpoints, users, suppliers, credits, audit)
            .create("create-1", "requester", "item", "pickup", "delivery", 3, 15,
                START.plusSeconds(86400), null, AUTH);

        assertEquals(OrderStatus.OPEN, result.getStatus());
        verify(suppliers).validatePair("pickup", "delivery", AUTH);
        verify(credits).reserve(result.getId(), "requester", 3, AUTH);
        verify(checkpoints).save(any(OrderCheckpoint.class));
        verify(receipts).save(any(CommandReceipt.class));
        verify(audit).action("CREATE", result.getId(), "requester", "create-1", "accepted");
    }

    @Test
    void creationIsIdempotentByCommandReceipt() {
        Order existing = Order.open("requester", "item", "p", "d", 1, 15, START, START.plusSeconds(1800));
        CommandReceipt receipt = new CommandReceipt("CREATE", "same", existing.getId(), START);
        OrderRepository orders = mock(OrderRepository.class);
        CommandReceiptRepository receipts = mock(CommandReceiptRepository.class);
        when(receipts.findExisting("CREATE", "same")).thenReturn(Optional.of(receipt));
        when(orders.get(existing.getId())).thenReturn(Optional.of(existing));

        Order result = new OrderCreationService(orders, receipts, mock(OrderCheckpointRepository.class),
            mock(UserServicePort.class), mock(SupplierServicePort.class), mock(CreditServicePort.class), mock(OrderAuditLogger.class))
            .create("same", "requester", "item", "p", "d", 1, 15, START.plusSeconds(1800), null, AUTH);

        assertSame(existing, result);
    }

    @Test
    void assignmentAcceptsAndIsIdempotentAndReportsMissingOrder() {
        OrderRepository orders = mock(OrderRepository.class);
        OrderCheckpointRepository checkpoints = mock(OrderCheckpointRepository.class);
        CommandReceiptRepository receipts = mock(CommandReceiptRepository.class);
        UserServicePort users = mock(UserServicePort.class);
        OrderAuditLogger audit = mock(OrderAuditLogger.class);
        Order order = Order.open("requester", "item", "p", "d", 1, 15, START, START.plusSeconds(86400));
        when(receipts.findExisting("ACCEPT", "accept-1")).thenReturn(Optional.empty());
        when(users.verifyCourier("courier", AUTH)).thenReturn("courier");
        when(orders.getForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = new OrderAssignmentService(orders, checkpoints, receipts, users, audit)
            .accept("accept-1", order.getId(), "courier", 0, AUTH);
        assertEquals(OrderStatus.ACCEPTED, result.getStatus());
        verify(checkpoints).save(any(OrderCheckpoint.class));
        verify(receipts).save(any(CommandReceipt.class));

        Order existing = Order.open("requester", "item", "p", "d", 1, 15, START, START.plusSeconds(1800));
        CommandReceipt previous = new CommandReceipt("ACCEPT", "accept-2", existing.getId(), START);
        when(receipts.findExisting("ACCEPT", "accept-2")).thenReturn(Optional.of(previous));
        when(orders.get(existing.getId())).thenReturn(Optional.of(existing));
        assertSame(existing, new OrderAssignmentService(orders, checkpoints, receipts, users, audit)
            .accept("accept-2", "courier", "courier", 0, AUTH));
        when(receipts.findExisting("ACCEPT", "missing")).thenReturn(Optional.empty());
        when(orders.getForUpdate("missing")).thenReturn(Optional.empty());
        assertThrows(OrderProblem.class, () -> new OrderAssignmentService(orders, checkpoints, receipts, users, audit)
            .accept("missing", "missing", "courier", 0, AUTH));
    }

    @Test
    void courierTransitionsPersistCheckpointsAndRequesterOutcomesCallCredit() {
        OrderRepository orders = mock(OrderRepository.class);
        OrderCheckpointRepository checkpoints = mock(OrderCheckpointRepository.class);
        CommandReceiptRepository receipts = mock(CommandReceiptRepository.class);
        UserServicePort users = mock(UserServicePort.class);
        CreditServicePort credits = mock(CreditServicePort.class);
        OrderAuditLogger audit = mock(OrderAuditLogger.class);
        OrderTransitionService service = new OrderTransitionService(orders, checkpoints, receipts, users, credits, audit);
        Order order = Order.open("requester", "item", "p", "d", 2, 15, START, START.plusSeconds(1800));
        order.accept("courier", 0, START.plusSeconds(1));
        when(users.verifyCourier("courier", AUTH)).thenReturn("courier");
        when(receipts.findExisting(anyString(), anyString())).thenReturn(Optional.empty());
        when(orders.getForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.start("start", order.getId(), "courier", 0, AUTH);
        service.pickup("pickup", order.getId(), "courier", 0, AUTH);
        service.deliver("deliver", order.getId(), "courier", 0, AUTH);
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        verify(checkpoints, times(3)).save(any(OrderCheckpoint.class));

        when(users.verifyRequester("requester", AUTH)).thenReturn("requester");
        service.complete("complete", order.getId(), "requester", 0, AUTH);
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        verify(credits).settle("complete", order.getId(), "requester", "courier", 2, 0, AUTH);

        Order cancelled = Order.open("requester", "item", "p", "d", 2, 15, START, START.plusSeconds(1800));
        when(orders.getForUpdate("cancelled")).thenReturn(Optional.of(cancelled));
        service.cancel("cancel", "cancelled", "requester", 0, AUTH);
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        verify(credits).release("cancel", cancelled.getId(), "requester", 2, "CANCELLED", 0, AUTH);
    }

    @Test
    void transitionOutcomeReceiptsAreIdempotent() {
        Order existing = Order.open("requester", "item", "p", "d", 1, 15, START, START.plusSeconds(1800));
        CommandReceipt receipt = new CommandReceipt("COMPLETE", "same", existing.getId(), START);
        CommandReceiptRepository receipts = mock(CommandReceiptRepository.class);
        OrderRepository orders = mock(OrderRepository.class);
        when(receipts.findExisting("COMPLETE", "same")).thenReturn(Optional.of(receipt));
        when(orders.get(existing.getId())).thenReturn(Optional.of(existing));
        OrderTransitionService service = new OrderTransitionService(orders, mock(OrderCheckpointRepository.class), receipts,
            mock(UserServicePort.class), mock(CreditServicePort.class), mock(OrderAuditLogger.class));
        assertSame(existing, service.complete("same", existing.getId(), "requester", 0, AUTH));
    }

    @Test
    void automaticAndManualRepostReserveCreditsAndRespectIdempotency() {
        OrderRepository orders = mock(OrderRepository.class);
        OrderCheckpointRepository checkpoints = mock(OrderCheckpointRepository.class);
        CommandReceiptRepository receipts = mock(CommandReceiptRepository.class);
        SupplierServicePort suppliers = mock(SupplierServicePort.class);
        CreditServicePort credits = mock(CreditServicePort.class);
        UserServicePort users = mock(UserServicePort.class);
        OrderAuditLogger audit = mock(OrderAuditLogger.class);
        OrderRepostService service = new OrderRepostService(orders, checkpoints, receipts, suppliers, credits, users, audit);
        RepostPlan plan = new RepostPlan(true, START, 4, 20);
        Order original = Order.open("requester", "item", "p", "d", 2, 15, START.minusSeconds(1800), START, plan);
        original.expire(0, START);
        when(receipts.findExisting("AUTO_REPOST", "auto")).thenReturn(Optional.empty());
        when(orders.getForUpdate(original.getId())).thenReturn(Optional.of(original));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Order auto = service.automatic("auto", original.getId(), START.plusSeconds(1), AUTH);
        assertEquals(original.getId(), auto.getOriginalOrderId());
        verify(credits).reserve(auto.getId(), "requester", 4, AUTH);

        Order manualOriginal = Order.open("requester", "item", "p", "d", 2, 15, START.minusSeconds(1800), START, null);
        manualOriginal.expire(0, START);
        when(receipts.findExisting("MANUAL_REPOST", "manual")).thenReturn(Optional.empty());
        when(orders.getForUpdate(manualOriginal.getId())).thenReturn(Optional.of(manualOriginal));
        when(users.verifyRequester("requester", AUTH)).thenReturn("requester");
        Order manual = service.manual("manual", manualOriginal.getId(), "requester", 0, "new item", 3, 15, START.plusSeconds(1800), AUTH);
        assertEquals(manualOriginal.getId(), manual.getOriginalOrderId());
        verify(credits).reserve(manual.getId(), "requester", 3, AUTH);

        Order existing = Order.open("requester", "item", "p", "d", 1, 15, START, START.plusSeconds(1800));
        CommandReceipt previous = new CommandReceipt("AUTO_REPOST", "done", existing.getId(), START);
        when(receipts.findExisting("AUTO_REPOST", "done")).thenReturn(Optional.of(previous));
        when(orders.get(existing.getId())).thenReturn(Optional.of(existing));
        assertSame(existing, service.automatic("done", existing.getId(), START, AUTH));
    }

    @Test
    void lifecycleExpiresAndRepostsDueOrders() {
        OrderRepository orders = mock(OrderRepository.class);
        OrderCheckpointRepository checkpoints = mock(OrderCheckpointRepository.class);
        OrderRepostService reposts = mock(OrderRepostService.class);
        CreditServicePort credits = mock(CreditServicePort.class);
        OrderAuditLogger audit = mock(OrderAuditLogger.class);
        LifecycleProcessingService lifecycle = new LifecycleProcessingService(orders, checkpoints, reposts, credits, audit);
        Order expired = Order.open("requester", "item", "p", "d", 2, 15, START.minusSeconds(1800), START);
        when(orders.findDueUnassigned(OrderStatus.OPEN, START))
            .thenReturn(List.of(expired));
        assertEquals(1, lifecycle.expireDue(START, AUTH));
        verify(credits).release(startsWith("EXPIRE:"), eq(expired.getId()), eq("requester"), eq(2L), eq("EXPIRED"), eq(0L), eq(AUTH));
        verify(checkpoints).save(any(OrderCheckpoint.class));

        RepostPlan plan = new RepostPlan(true, START, 1, 15);
        Order due = Order.open("requester", "item", "p", "d", 1, 15, START.minusSeconds(1800), START, plan);
        due.expire(0, START);
        when(orders.findDueUnassigned(OrderStatus.EXPIRED, START))
            .thenReturn(List.of(due));
        assertEquals(1, lifecycle.repostDue(START, AUTH));
        verify(reposts).automatic(startsWith("AUTO_REPOST:"), eq(due.getId()), eq(START), eq(AUTH));
    }

    @Test
    void commandFacadeDispatchesCreateAndAccept() {
        OrderCreationService creation = mock(OrderCreationService.class);
        OrderAssignmentService assignment = mock(OrderAssignmentService.class);
        OrderTransitionService transitions = mock(OrderTransitionService.class);
        OrderRepostService reposts = mock(OrderRepostService.class);
        OrderCommandFacade facade = new OrderCommandFacade(creation, assignment, transitions, reposts);
        RepostPlan plan = new RepostPlan(false, null, 0, 0);
        Order order = Order.open("requester", "item", "p", "d", 1, 15, START, START.plusSeconds(1800));
        when(creation.create("c", "requester", "item", "p", "d", 1, 15, START.plusSeconds(1800), plan, AUTH)).thenReturn(order);
        assertSame(order, facade.create("c", "requester", "item", "p", "d", 1, 15, START.plusSeconds(1800), plan, AUTH));
        when(assignment.accept("a", "id", "courier", 0, AUTH)).thenReturn(order);
        assertSame(order, facade.accept("a", "id", "courier", 0, AUTH));
        verifyNoInteractions(transitions, reposts);
    }

    @Test
    void integratesCreationWithLocalPeerCreditReservation() {
        OrderRepository orders = mock(OrderRepository.class);
        CommandReceiptRepository receipts = mock(CommandReceiptRepository.class);
        OrderCheckpointRepository checkpoints = mock(OrderCheckpointRepository.class);
        MockPeerAdapters peers = new MockPeerAdapters();
        OrderAuditLogger audit = mock(OrderAuditLogger.class);
        when(receipts.findExisting("CREATE", "integration-create")).thenReturn(Optional.empty());
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order created = new OrderCreationService(orders, receipts, checkpoints, peers, peers, peers, audit)
            .create("integration-create", "requester", "pickup", "pickup-id", "delivery-id", 5, 15,
                Instant.now().plusSeconds(3600), null, AUTH);

        assertEquals(OrderStatus.OPEN, created.getStatus());
        assertEquals(new MockPeerAdapters.CreditSnapshot(50, 5, 45), peers.creditSnapshot("requester"));
        assertEquals("RESERVED", peers.reservationState(created.getId()));
        verify(checkpoints).save(any(OrderCheckpoint.class));
    }
}
