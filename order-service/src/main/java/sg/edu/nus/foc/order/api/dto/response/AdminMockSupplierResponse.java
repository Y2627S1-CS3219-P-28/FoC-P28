package sg.edu.nus.foc.order.api.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminMockSupplierResponse {

    private String id;
    private String name;
    private String type;
    private String building;
    private String floor;
    private String locationDescription;
    private double latitude;
    private double longitude;
    private String openingTime;
    private String closingTime;
    private String imageUrl;
    private boolean active;
    private boolean openNow;
    private Instant createdAt;
    private Instant updatedAt;
}
