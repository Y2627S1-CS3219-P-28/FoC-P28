package sg.edu.nus.foc.order.domain;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_commands")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommandReceipt {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    public void verify(String expected) {
        if (!fingerprint.equals(expected))
            throw OrderProblem.conflict("Command ID was already used with different input.");
    }
}
