package sg.edu.nus.foc.order.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.domain.RepostPlan;
import sg.edu.nus.foc.order.messagingpublisher.dto.AcceptedOrderCancellationTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderCompletionTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OpenOrderRefundTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.mapper.OrderTaskEventMapper;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class OrderTaskEventFactoryTest {
    private static final Instant CREATED_AT = Instant.parse("2026-10-01T10:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-02T10:00:00Z");
    private static final Instant OCCURRED_AT = Instant.parse("2026-10-01T10:15:00Z");

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    private final OrderTaskEventFactory factory = new OrderTaskEventFactory(
            Mappers.getMapper(OrderTaskEventMapper.class));

    @Test
    void mapsOrderFieldsWithoutSerializingCheckpointHistoryIntoTypedEvents() {
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
        Instant overdueAt = CREATED_AT.plusSeconds(30 * 60L);
        OrderCompletionTaskEvent completion = factory.completion(
                "command-1", order, "requester-1", OCCURRED_AT, false, overdueAt);
        OrderCompletionTaskEvent overdueCompletion = factory.completion(
                "command-2", order, "requester-1", OCCURRED_AT, true, overdueAt);
        OpenOrderRefundTaskEvent openCancellation = factory.openRefund(
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
        JsonNode serializedCompletion = objectMapper.valueToTree(completion);
        assertFalse(serializedCompletion.path("order").has("checkpoints"));
        assertFalse(objectMapper.valueToTree(openCancellation).path("order").has("checkpoints"));
        assertFalse(objectMapper.valueToTree(acceptedCancellation).path("order").has("checkpoints"));
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
        assertEquals("OrderCompletionTaskEvent", overdueCompletion.getEventType());
        assertEquals("OpenOrderRefundTaskEvent", openCancellation.getEventType());
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
        OrderCompletionTaskEvent event = factory.completion(
                "command-5", order, "requester-2", OCCURRED_AT, false, OCCURRED_AT.plusSeconds(900));
        OrderTaskEventMapper mapper = Mappers.getMapper(OrderTaskEventMapper.class);

        assertNull(event.getOrder().getCourierId());
        assertNull(event.getOrder().getOriginalOrderId());
        assertNull(event.getOrder().getRepostedOrderId());
        assertNull(event.getOrder().getRepostPlan());
        assertFalse(objectMapper.valueToTree(event).path("order").has("checkpoints"));
        assertNull(mapper.toSnapshot((Order) null, 0L));
        assertNull(mapper.toSnapshot((RepostPlan) null));
        assertNull(mapper.toCompletionEvent(null, null, null, null, false, null, null));
        assertNull(mapper.toOpenRefundEvent(null, null, null, null, null));
        assertNull(mapper.toAcceptedCancellationEvent(null, null, null, null, null));
        assertNotNull(mapper.toCompletionEvent(null, order, "requester-2", OCCURRED_AT, false, null, null));
        assertNotNull(mapper.toCompletionEvent(null, null, "requester-2", OCCURRED_AT, false, null, null));
        assertNotNull(mapper.toCompletionEvent(null, null, null, OCCURRED_AT, false, null, null));
        assertNotNull(mapper.toCompletionEvent(null, null, null, null, true, EXPIRES_AT, event.getOrder()));
        assertNotNull(mapper.toOpenRefundEvent(null, order, "requester-2", OCCURRED_AT, null));
        assertNotNull(mapper.toOpenRefundEvent(null, null, "requester-2", OCCURRED_AT, null));
        assertNotNull(mapper.toOpenRefundEvent(null, null, null, OCCURRED_AT, null));
        assertNotNull(mapper.toOpenRefundEvent(null, null, null, null, event.getOrder()));
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
        assertEquals(order.getId(), mapper.toSnapshot(reposted, reposted.getVersion()).getOriginalOrderId());
    }

    @Test
    void mapsExpiredOrderSnapshotToOpenRefundEventWithStableIdentity() {
        Order order = Order.open(
                "requester-3", "expired item", "pickup-3", "delivery-3", 4, 20,
                CREATED_AT, EXPIRES_AT);
        order.expire(0, EXPIRES_AT);
        OpenOrderRefundTaskEvent first = factory.openRefund("EXPIRE:" + order.getId(), order, EXPIRES_AT);
        OpenOrderRefundTaskEvent retry = factory.openRefund("EXPIRE:" + order.getId(), order, EXPIRES_AT);

        assertEquals(first.getEventId(), retry.getEventId());
        assertEquals("OpenOrderRefundTaskEvent", first.getEventType());
        assertEquals(order.getId(), first.getOrderId());
        assertEquals("lifecycle", first.getActorId());
        assertEquals(OrderStatus.EXPIRED, first.getOrder().getStatus());
        assertFalse(objectMapper.valueToTree(first).path("order").has("checkpoints"));
    }
}
