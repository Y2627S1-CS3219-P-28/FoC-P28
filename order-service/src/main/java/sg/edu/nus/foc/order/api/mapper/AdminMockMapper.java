package sg.edu.nus.foc.order.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import sg.edu.nus.foc.order.api.dto.request.AdminMockSupplierRequest;
import sg.edu.nus.foc.order.api.dto.response.AdminMockOrderResponse;
import sg.edu.nus.foc.order.api.dto.response.AdminMockSupplierResponse;
import sg.edu.nus.foc.order.api.dto.response.AdminMockUserResponse;
import sg.edu.nus.foc.order.api.dto.response.AdminMockTransactionResponse;

/** Response copies prevent HTTP serialization or callers mutating the demo store. */
@Mapper(componentModel = "spring")
public interface AdminMockMapper {
    sg.edu.nus.foc.order.api.dto.response.ReportDemoResponse copyReport(sg.edu.nus.foc.order.api.dto.response.ReportDemoResponse source);
    sg.edu.nus.foc.order.api.dto.request.ReportEvidence copyEvidence(sg.edu.nus.foc.order.api.dto.request.ReportEvidence source);

    AdminMockUserResponse copyUser(AdminMockUserResponse source);

    AdminMockSupplierResponse copySupplier(AdminMockSupplierResponse source);

    AdminMockOrderResponse copyOrder(AdminMockOrderResponse source);

    AdminMockTransactionResponse copyTransaction(AdminMockTransactionResponse source);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "openNow", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AdminMockSupplierResponse toSupplier(AdminMockSupplierRequest request);
}
