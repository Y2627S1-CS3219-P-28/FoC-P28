package sg.edu.nus.foc.order.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlaceCompletionHoldRequest {

    @NotBlank
    @Size(max = 128)
    private String requesterId;
}
