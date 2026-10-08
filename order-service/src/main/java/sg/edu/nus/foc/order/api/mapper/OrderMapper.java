package sg.edu.nus.foc.order.api.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import sg.edu.nus.foc.order.api.dto.response.OrderPageResponse;
import sg.edu.nus.foc.order.api.dto.response.OrderResponse;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.repository.OrderPage;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "requesterId", source = "requesterId")
    @Mapping(target = "courierId", source = "courierId")
    @Mapping(target = "itemDescription", source = "itemDescription")
    @Mapping(target = "pickupSupplierId", source = "pickupSupplierId")
    @Mapping(target = "deliverySupplierId", source = "deliverySupplierId")
    @Mapping(target = "offeredCredits", source = "offeredCredits")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "deliveryTimeLimitMinutes", source = "deliveryTimeLimitMinutes")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "originalOrderId", source = "originalOrderId")
    @Mapping(target = "repostedOrderId", source = "repostedOrderId")
    @Mapping(
            target = "automaticRepostEnabled",
            source = "repostPlan.enabled",
            defaultValue = "false")
    @Mapping(target = "repostDueAt", source = "repostPlan.dueAt")
    @Mapping(
            target = "repostCreditAmount",
            source = "repostPlan.creditAmount",
            defaultValue = "0")
    @Mapping(
            target = "repostDeliveryDurationMinutes",
            source = "repostPlan.deliveryDurationMinutes",
            defaultValue = "0")
    OrderResponse toResponse(Order order);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "items", source = "items")
    @Mapping(target = "page", expression = "java(orderPage.getPage() + 1)")
    @Mapping(target = "size", source = "size")
    @Mapping(target = "totalItems", source = "totalItems")
    @Mapping(target = "totalPages", source = "totalPages")
    OrderPageResponse toResponse(OrderPage orderPage);
}
