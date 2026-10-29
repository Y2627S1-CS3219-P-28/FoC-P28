package sg.edu.nus.foc.order.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaceCompletionHoldRequest {
    @NotNull private Boolean requireAssignedCourier;
}
