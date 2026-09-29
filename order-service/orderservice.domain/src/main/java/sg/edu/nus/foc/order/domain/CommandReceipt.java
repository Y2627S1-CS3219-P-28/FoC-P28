package sg.edu.nus.foc.order.domain;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Optional;

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

    public Optional<OrderProblem> verify(String expected) {
        if (!fingerprint.equals(expected))
            return Optional.of(
                    OrderProblem.conflict("Command ID was already used with different input."));
        return Optional.empty();
    }
}
