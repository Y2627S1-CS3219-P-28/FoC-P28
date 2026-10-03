package sg.edu.nus.foc.order.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderCheckpoint;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.domain.RepostPlan;
import sg.edu.nus.foc.order.domain.repository.OrderCheckpointRepository;
import sg.edu.nus.foc.order.messagingpublisher.dto.AcceptedOrderCancellationTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderCompletionTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OpenOrderCancellationTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.mapper.OrderTaskEventMapper;

class OrderTaskEventFactoryTest {
    private static final Instant CREATED_AT = Instant.parse("2026-10-01T10:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-02T10:00:00Z");
    private static final Instant OCCURRED_AT = Instant.parse("2026-10-01T10:15:00Z");

    private final OrderCheckpointRepository checkpoints = mock(OrderCheckpointRepository.class);
    private final OrderTaskEventFactory factory = new OrderTaskEventFactory(
            checkpoints,
            Mappers.getMapper(OrderTaskEventMapper.class));

    @Test
    void mapsCompleteOrderAndCheckpointHistoryIntoTypedEvents() {
        RepostPlan repostPlan = new RepostPlan(true, EXPIRES_AT, 5, 45);
        Order order = Order.open(
                "requester-1",
                "item",
                "pickup-1",
                "delivery-1",
                3,
                30,
                CREATED_AT,
                EXPIRES_AT,
                repostPlan);
        order.linkRepost("repost-1");
        List<OrderCheckpoint> history = List.of(
                new OrderCheckpoint(order.getId(), OrderStatus.ACCEPTED, CREATED_AT, "courier-1", "supplier-1"),
                new OrderCheckpoint(order.getId(), OrderStatus.DELIVERED, OCCURRED_AT, "courier-1", "supplier-2"));
        when(checkpoints.findByOrderId(order.getId())).thenReturn(history);

        Instant overdueAt = CREATED_AT.plusSeconds(30 * 60L);
        OrderCompletionTaskEvent completion = factory.completion(
                "command-1", order, "requester-1", OCCURRED_AT, false, overdueAt);
        OrderCompletionTaskEvent overdueCompletion = factory.completion(
                "command-2", order, "requester-1", OCCURRED_AT, true, overdueAt);
        OpenOrderCancellationTaskEvent openCancellation = factory.openCancellation(
                "command-3", order, "requester-1", OCCURRED_AT);
        AcceptedOrderCancellationTaskEvent acceptedCancellation = factory.acceptedCancellation(
                "command-4", order, "requester-1", OCCURRED_AT);

        assertEquals("OrderCompletionTaskEvent", completion.getEventType());
        assertFalse(completion.isOverdue());
        assertEquals(overdueAt, completion.getOverdueAt());
        assertTrue(overdueCompletion.isOverdue());
        assertEquals(overdueAt, overdueCompletion.getOverdueAt());
        assertEquals(1, completion.getEventVersion());
        assertEquals("requester-1", completion.getActorId());
        assertEquals(order.getId(), completion.getOrderId());
        assertEquals(order.getVersion() + 1, completion.getOrderVersion());
        assertEquals(order.getId(), completion.getOrder().getId());
        assertEquals(order.getRequesterId(), completion.getOrder().getRequesterId());
        assertEquals(order.getCourierId(), completion.getOrder().getCourierId());
        assertEquals(order.getItemDescription(), completion.getOrder().getItemDescription());
        assertEquals(order.getPickupSupplierId(), completion.getOrder().getPickupSupplierId());
        assertEquals(order.getDeliverySupplierId(), completion.getOrder().getDeliverySupplierId());
        assertEquals(order.getOfferedCredits(), completion.getOrder().getOfferedCredits());
        assertEquals(order.getVersion() + 1, completion.getOrder().getVersion());
        assertEquals(order.getStatus(), completion.getOrder().getStatus());
        assertEquals(order.getCreatedAt(), completion.getOrder().getCreatedAt());
        assertEquals(order.getExpiresAt(), completion.getOrder().getExpiresAt());
        assertEquals(order.getDeliveryTimeLimitMinutes(), completion.getOrder().getDeliveryTimeLimitMinutes());
        assertEquals(order.getOriginalOrderId(), completion.getOrder().getOriginalOrderId());
        assertEquals(order.getRepostedOrderId(), completion.getOrder().getRepostedOrderId());
        assertEquals(repostPlan.isEnabled(), completion.getOrder().getRepostPlan().isEnabled());
        assertEquals(repostPlan.getDueAt(), completion.getOrder().getRepostPlan().getDueAt());
        assertEquals(repostPlan.getCreditAmount(), completion.getOrder().getRepostPlan().getCreditAmount());
        assertEquals(
                repostPlan.getDeliveryDurationMinutes(),
                completion.getOrder().getRepostPlan().getDeliveryDurationMinutes());
        assertTrue(completion.getOrder().getRepostPlan().isUsed());
        assertEquals(2, completion.getOrder().getCheckpoints().size());
        assertEquals(history.get(0).getId(), completion.getOrder().getCheckpoints().get(0).getId());
        assertEquals(history.get(0).getStatus(), completion.getOrder().getCheckpoints().get(0).getStatus());
        assertEquals(history.get(0).getOccurredAt(), completion.getOrder().getCheckpoints().get(0).getOccurredAt());
        assertEquals(history.get(0).getActorId(), completion.getOrder().getCheckpoints().get(0).getActorId());
        assertEquals(history.get(0).getSupplierId(), completion.getOrder().getCheckpoints().get(0).getSupplierId());
        assertEquals("OrderCompletionTaskEvent", overdueCompletion.getEventType());
        assertEquals("OpenOrderCancellationTaskEvent", openCancellation.getEventType());
        assertEquals("AcceptedOrderCancellationTaskEvent", acceptedCancellation.getEventType());
        assertNotNull(openCancellation.getOrder());
        assertNotNull(acceptedCancellation.getOrder());
    }

    @Test
    void mapsOrdersWithoutOptionalRepostDataAndMapsNullInputs() {
        Order order = Order.open(
                "requester-2",
                "item",
                "pickup-2",
                "delivery-2",
                1,
                15,
                CREATED_AT,
                EXPIRES_AT);
        when(checkpoints.findByOrderId(order.getId())).thenReturn(List.of());

        OrderCompletionTaskEvent event = factory.completion(
                "command-5", order, "requester-2", OCCURRED_AT, false, OCCURRED_AT.plusSeconds(900));
        OrderTaskEventMapper mapper = Mappers.getMapper(OrderTaskEventMapper.class);

        assertNull(event.getOrder().getCourierId());
        assertNull(event.getOrder().getOriginalOrderId());
        assertNull(event.getOrder().getRepostedOrderId());
        assertNull(event.getOrder().getRepostPlan());
        assertTrue(event.getOrder().getCheckpoints().isEmpty());
        assertNull(mapper.toSnapshot((Order) null, null));
        assertNotNull(mapper.toSnapshot((Order) null, List.of()));
        assertNull(mapper.toSnapshot((RepostPlan) null));
        assertNull(mapper.toSnapshot((OrderCheckpoint) null));
        assertNull(mapper.toCheckpointSnapshots(null));
        assertNull(mapper.toCheckpointSnapshots(java.util.Arrays.asList((OrderCheckpoint) null)).get(0));
        assertNull(mapper.toCompletionEvent(null, null, null, null, false, null, null));
        assertNull(mapper.toOpenCancellationEvent(null, null, null, null, null));
        assertNull(mapper.toAcceptedCancellationEvent(null, null, null, null, null));
        assertNotNull(mapper.toCompletionEvent(null, order, "requester-2", OCCURRED_AT, false, null, null));
        assertNotNull(mapper.toCompletionEvent(null, null, "requester-2", OCCURRED_AT, false, null, null));
        assertNotNull(mapper.toCompletionEvent(null, null, null, OCCURRED_AT, false, null, null));
        assertNotNull(mapper.toCompletionEvent(null, null, null, null, true, EXPIRES_AT, event.getOrder()));
        assertNotNull(mapper.toOpenCancellationEvent(null, order, "requester-2", OCCURRED_AT, null));
        assertNotNull(mapper.toOpenCancellationEvent(null, null, "requester-2", OCCURRED_AT, null));
        assertNotNull(mapper.toOpenCancellationEvent(null, null, null, OCCURRED_AT, null));
        assertNotNull(mapper.toOpenCancellationEvent(null, null, null, null, event.getOrder()));
        assertNotNull(mapper.toAcceptedCancellationEvent(null, order, "requester-2", OCCURRED_AT, null));
        assertNotNull(mapper.toAcceptedCancellationEvent(null, null, "requester-2", OCCURRED_AT, null));
        assertNotNull(mapper.toAcceptedCancellationEvent(null, null, null, OCCURRED_AT, null));
        assertNotNull(mapper.toAcceptedCancellationEvent(null, null, null, null, event.getOrder()));
        assertNull(mapper.toCompletionEvent("event-1", order, "requester-2", OCCURRED_AT, false, null, null).getOrder());
        assertNotNull(mapper.toCompletionEvent(
                "event-2", order, "requester-2", OCCURRED_AT, true, EXPIRES_AT, null).getOverdueAt());
        order.expire(0, EXPIRES_AT);
        Order reposted = order.createRepost(
                "repost", 2, 15, EXPIRES_AT.plusSeconds(1), EXPIRES_AT.plusSeconds(3601));
        assertEquals(order.getId(), mapper.toSnapshot(reposted, List.of()).getOriginalOrderId());
    }
}
