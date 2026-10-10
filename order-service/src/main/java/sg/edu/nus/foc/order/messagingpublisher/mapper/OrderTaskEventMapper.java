package sg.edu.nus.foc.order.messagingpublisher.mapper;

import java.time.Instant;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.RepostPlan;
import sg.edu.nus.foc.order.messagingpublisher.dto.AcceptedOrderCancellationTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderCompletionTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderEventSnapshot;
import sg.edu.nus.foc.order.messagingpublisher.dto.OpenOrderRefundTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.RepostPlanEventSnapshot;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderTaskEventMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "requesterId", source = "order.requesterId")
    @Mapping(target = "courierId", source = "order.courierId")
    @Mapping(target = "itemDescription", source = "order.itemDescription")
    @Mapping(target = "pickupSupplierId", source = "order.pickupSupplierId")
    @Mapping(target = "deliverySupplierId", source = "order.deliverySupplierId")
    @Mapping(target = "offeredCredits", source = "order.offeredCredits")
    @Mapping(target = "status", source = "order.status")
    @Mapping(target = "createdAt", source = "order.createdAt")
    @Mapping(target = "expiresAt", source = "order.expiresAt")
    @Mapping(target = "deliveryTimeLimitMinutes", source = "order.deliveryTimeLimitMinutes")
    @Mapping(target = "version", source = "resultingVersion")
    @Mapping(target = "originalOrderId", source = "order.originalOrderId")
    @Mapping(target = "repostedOrderId", source = "order.repostedOrderId")
    @Mapping(target = "repostPlan", source = "order.repostPlan")
    OrderEventSnapshot toSnapshot(Order order, long resultingVersion);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "dueAt", source = "dueAt")
    @Mapping(target = "creditAmount", source = "creditAmount")
    @Mapping(target = "deliveryDurationMinutes", source = "deliveryDurationMinutes")
    @Mapping(target = "used", source = "used")
    RepostPlanEventSnapshot toSnapshot(RepostPlan repostPlan);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "eventType", constant = "OrderCompletionTaskEvent")
    @Mapping(target = "eventVersion", constant = "2")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderVersion", source = "resultingVersion")
    @Mapping(target = "orderStatus", source = "order.status")
    @Mapping(target = "creditAmount", source = "order.offeredCredits")
    @Mapping(target = "courierId", source = "order.courierId")
    @Mapping(target = "occurredAt", source = "occurredAt")
    OrderCompletionTaskEvent toCompletionEvent(
            String eventId, Order order, long resultingVersion, Instant occurredAt);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "eventType", constant = "OpenOrderRefundTaskEvent")
    @Mapping(target = "eventVersion", constant = "2")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderVersion", source = "resultingVersion")
    @Mapping(target = "orderStatus", source = "order.status")
    @Mapping(target = "creditAmount", source = "order.offeredCredits")
    @Mapping(target = "courierId", source = "order.courierId")
    @Mapping(target = "occurredAt", source = "occurredAt")
    OpenOrderRefundTaskEvent toOpenRefundEvent(
            String eventId, Order order, long resultingVersion, Instant occurredAt);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "eventType", constant = "AcceptedOrderCancellationTaskEvent")
    @Mapping(target = "eventVersion", constant = "1")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderVersion", source = "resultingVersion")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "actorId", source = "actorId")
    @Mapping(target = "order", source = "snapshot")
    AcceptedOrderCancellationTaskEvent toAcceptedCancellationEvent(
            String eventId,
            Order order,
            long resultingVersion,
            String actorId,
            Instant occurredAt,
            OrderEventSnapshot snapshot);

    default AcceptedOrderCancellationTaskEvent toAcceptedCancellationEvent(
            String eventId,
            Order order,
            String actorId,
            Instant occurredAt,
            OrderEventSnapshot snapshot) {
        long resultingVersion = order == null ? 0 : order.getVersion() + 1;
        return toAcceptedCancellationEvent(eventId, order, resultingVersion, actorId, occurredAt, snapshot);
    }

}
