package sg.edu.nus.foc.order.api;

import org.mapstruct.*;

import sg.edu.nus.foc.order.application.ErrandWorkflow.*;
import sg.edu.nus.foc.order.contracts.ErrandContracts.*;
import sg.edu.nus.foc.order.domain.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ErrandMapper {
    CreateInput input(CreateRequest source);

    ActionInput input(ActionRequest source);

    @Mapping(target = ".", source = "errand")
    ErrandResponse response(ErrandView source);

    @Mapping(target = ".", source = "order")
    OrderResponse response(OrderView source);

    CheckpointResponse response(Checkpoint source);

    SupplierResponse response(SupplierGateway.Supplier source);
}
