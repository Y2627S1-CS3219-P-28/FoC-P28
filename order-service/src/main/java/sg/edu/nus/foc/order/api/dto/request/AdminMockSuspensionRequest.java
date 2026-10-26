package sg.edu.nus.foc.order.api.dto.request;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminMockSuspensionRequest {

    @NotNull
    @Future
    private Instant suspensionEndDate;
}
