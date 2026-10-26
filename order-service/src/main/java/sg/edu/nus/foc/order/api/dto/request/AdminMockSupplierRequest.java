package sg.edu.nus.foc.order.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminMockSupplierRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 40)
    private String type;

    @NotBlank
    @Size(max = 100)
    private String building;

    @Size(max = 10)
    private String floor;

    @Size(max = 200)
    private String locationDescription;

    @NotNull
    @DecimalMin("-90")
    @DecimalMax("90")
    private Double latitude;

    @NotNull
    @DecimalMin("-180")
    @DecimalMax("180")
    private Double longitude;

    @NotNull
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$")
    private String openingTime;

    @NotNull
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$")
    private String closingTime;

    @Size(max = 500)
    @Pattern(regexp = "^$|^https?://\\S+$")
    private String imageUrl;
}
