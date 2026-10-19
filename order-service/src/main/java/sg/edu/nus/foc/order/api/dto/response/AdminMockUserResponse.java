package sg.edu.nus.foc.order.api.dto.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** User wire fields plus explicitly sample-only profile and credit projections. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminMockUserResponse {

    private String id;
    private String userId;
    private String username;
    private String email;
    private List<String> roles;
    private int penalty;
    @JsonProperty("isCourierSuspended")
    private boolean isCourierSuspended;
    private Instant suspensionEndDate;
    private String phoneNumber;
    private String address;
    private boolean active;
    private long usableBalance;
    private long reservedBalance;
    private long totalBalance;
}
