package sg.edu.nus.foc.order.messagingpublisher.mapper;

import java.time.Instant;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderCheckpoint;
import sg.edu.nus.foc.order.domain.RepostPlan;
import sg.edu.nus.foc.order.messagingpublisher.dto.AcceptedOrderCancellationTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderCheckpointEventSnapshot;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderCompletionTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderEventSnapshot;
import sg.edu.nus.foc.order.messagingpublisher.dto.OpenOrderCancellationTaskEvent;
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
    @Mapping(target = "checkpoints", source = "checkpoints")
    OrderEventSnapshot toSnapshot(Order order, List<OrderCheckpoint> checkpoints, long resultingVersion);

    default OrderEventSnapshot toSnapshot(Order order, List<OrderCheckpoint> checkpoints) {
        long resultingVersion = order == null ? 0 : order.getVersion() + 1;
        return toSnapshot(order, checkpoints, resultingVersion);
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "dueAt", source = "dueAt")
    @Mapping(target = "creditAmount", source = "creditAmount")
    @Mapping(target = "deliveryDurationMinutes", source = "deliveryDurationMinutes")
    @Mapping(target = "used", source = "used")
    RepostPlanEventSnapshot toSnapshot(RepostPlan repostPlan);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "actorId", source = "actorId")
    @Mapping(target = "supplierId", source = "supplierId")
    OrderCheckpointEventSnapshot toSnapshot(OrderCheckpoint checkpoint);

    List<OrderCheckpointEventSnapshot> toCheckpointSnapshots(List<OrderCheckpoint> checkpoints);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "eventType", constant = "OrderCompletionTaskEvent")
    @Mapping(target = "eventVersion", constant = "1")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderVersion", source = "resultingVersion")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "actorId", source = "actorId")
    @Mapping(target = "order", source = "snapshot")
    @Mapping(target = "overdue", source = "overdue")
    @Mapping(target = "overdueAt", source = "overdueAt")
    OrderCompletionTaskEvent toCompletionEvent(
            String eventId,
            Order order,
            long resultingVersion,
            String actorId,
            Instant occurredAt,
            boolean overdue,
            Instant overdueAt,
            OrderEventSnapshot snapshot);

    default OrderCompletionTaskEvent toCompletionEvent(
            String eventId,
            Order order,
            String actorId,
            Instant occurredAt,
            boolean overdue,
            Instant overdueAt,
            OrderEventSnapshot snapshot) {
        long resultingVersion = order == null ? 0 : order.getVersion() + 1;
        return toCompletionEvent(eventId, order, resultingVersion, actorId, occurredAt, overdue, overdueAt, snapshot);
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "eventType", constant = "OpenOrderCancellationTaskEvent")
    @Mapping(target = "eventVersion", constant = "1")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderVersion", source = "resultingVersion")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "actorId", source = "actorId")
    @Mapping(target = "order", source = "snapshot")
    OpenOrderCancellationTaskEvent toOpenCancellationEvent(
            String eventId,
            Order order,
            long resultingVersion,
            String actorId,
            Instant occurredAt,
            OrderEventSnapshot snapshot);

    default OpenOrderCancellationTaskEvent toOpenCancellationEvent(
            String eventId,
            Order order,
            String actorId,
            Instant occurredAt,
            OrderEventSnapshot snapshot) {
        long resultingVersion = order == null ? 0 : order.getVersion() + 1;
        return toOpenCancellationEvent(eventId, order, resultingVersion, actorId, occurredAt, snapshot);
    }

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
