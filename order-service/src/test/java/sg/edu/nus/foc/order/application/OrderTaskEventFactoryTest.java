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
        RepostPlan repostPlan = new RepostPlan(true, EXPIRES_AT, 5, 45, EXPIRES_AT.plusSeconds(3600));
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
        assertEquals(order.getStatus(), completion.getOrderStatus());
        assertEquals(order.getCourierId(), completion.getCourierId());
        assertEquals(order.getOfferedCredits(), completion.getCreditAmount());
        assertEquals(2, completion.getEventVersion());
        assertEquals("requester-1", acceptedCancellation.getActorId());
        assertEquals(order.getId(), completion.getOrderId());
        assertEquals(order.getVersion() + 1, completion.getOrderVersion());
        JsonNode serializedCompletion = objectMapper.valueToTree(completion);
        assertFalse(serializedCompletion.path("order").has("checkpoints"));
        assertFalse(objectMapper.valueToTree(openCancellation).path("order").has("checkpoints"));
        assertFalse(objectMapper.valueToTree(acceptedCancellation).path("order").has("checkpoints"));
        assertEquals(order.getId(), acceptedCancellation.getOrder().getId());
        assertEquals(order.getRequesterId(), acceptedCancellation.getOrder().getRequesterId());
        assertEquals(order.getCourierId(), acceptedCancellation.getOrder().getCourierId());
        assertEquals(order.getItemDescription(), acceptedCancellation.getOrder().getItemDescription());
        assertEquals(order.getPickupSupplierId(), acceptedCancellation.getOrder().getPickupSupplierId());
        assertEquals(order.getDeliverySupplierId(), acceptedCancellation.getOrder().getDeliverySupplierId());
        assertEquals(order.getOfferedCredits(), acceptedCancellation.getOrder().getOfferedCredits());
        assertEquals(order.getVersion() + 1, acceptedCancellation.getOrder().getVersion());
        assertEquals(order.getStatus(), acceptedCancellation.getOrder().getStatus());
        assertEquals(order.getCreatedAt(), acceptedCancellation.getOrder().getCreatedAt());
        assertEquals(order.getExpiresAt(), acceptedCancellation.getOrder().getExpiresAt());
        assertEquals(order.getDeliveryTimeLimitMinutes(), acceptedCancellation.getOrder().getDeliveryTimeLimitMinutes());
        assertEquals(order.getOriginalOrderId(), acceptedCancellation.getOrder().getOriginalOrderId());
        assertEquals(order.getRepostedOrderId(), acceptedCancellation.getOrder().getRepostedOrderId());
        assertEquals(repostPlan.isEnabled(), acceptedCancellation.getOrder().getRepostPlan().isEnabled());
        assertEquals(repostPlan.getDueAt(), acceptedCancellation.getOrder().getRepostPlan().getDueAt());
        assertEquals(repostPlan.getCreditAmount(), acceptedCancellation.getOrder().getRepostPlan().getCreditAmount());
        assertEquals(
                repostPlan.getDeliveryDurationMinutes(),
                acceptedCancellation.getOrder().getRepostPlan().getDeliveryDurationMinutes());
        assertTrue(acceptedCancellation.getOrder().getRepostPlan().isUsed());
        assertEquals("OrderCompletionTaskEvent", overdueCompletion.getEventType());
        assertEquals("OpenOrderRefundTaskEvent", openCancellation.getEventType());
        assertEquals("AcceptedOrderCancellationTaskEvent", acceptedCancellation.getEventType());
        assertEquals(order.getOfferedCredits(), openCancellation.getCreditAmount());
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
        AcceptedOrderCancellationTaskEvent event = factory.acceptedCancellation(
                "command-5", order, "requester-2", OCCURRED_AT);
        OrderTaskEventMapper mapper = Mappers.getMapper(OrderTaskEventMapper.class);

        assertNull(event.getOrder().getCourierId());
        assertNull(event.getOrder().getOriginalOrderId());
        assertNull(event.getOrder().getRepostedOrderId());
        assertNull(event.getOrder().getRepostPlan());
        assertFalse(objectMapper.valueToTree(event).path("order").has("checkpoints"));
        assertNull(mapper.toSnapshot((Order) null, 0L));
        assertNull(mapper.toSnapshot((RepostPlan) null));
        assertNull(mapper.toCompletionEvent(null, null, 0, null));
        assertNull(mapper.toOpenRefundEvent(null, null, 0, null));
        assertNull(mapper.toAcceptedCancellationEvent(null, null, null, null, null));
        assertNotNull(mapper.toCompletionEvent("event", order, 1, OCCURRED_AT));
        assertNotNull(mapper.toOpenRefundEvent("event", order, 1, OCCURRED_AT));
        assertNotNull(mapper.toAcceptedCancellationEvent(null, order, "requester-2", OCCURRED_AT, null));
        assertNotNull(mapper.toAcceptedCancellationEvent(null, null, "requester-2", OCCURRED_AT, null));
        assertNotNull(mapper.toAcceptedCancellationEvent(null, null, null, OCCURRED_AT, null));
        assertNotNull(mapper.toAcceptedCancellationEvent(null, null, null, null, event.getOrder()));
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
        assertEquals(2, first.getEventVersion());
        assertEquals(OrderStatus.EXPIRED, first.getOrderStatus());
        assertFalse(objectMapper.valueToTree(first).path("order").has("checkpoints"));
    }
}
