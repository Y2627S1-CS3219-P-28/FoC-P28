package sg.edu.nus.foc.order.domain;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Optional;

@Entity
@Table(name = "errands")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Errand {
    public enum Status {
        OPEN,
        ACCEPTED
    }

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 128)
    private String requesterId;

    @Column(nullable = false, length = 100)
    private String description;

    @Column(nullable = false, length = 128)
    private String pickupSupplierId;

    @Column(nullable = false, length = 128)
    private String deliverySupplierId;

    @Column(nullable = false)
    private long creditAmount;

    @Column(nullable = false)
    private int deliveryDurationMinutes;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(length = 36)
    private String orderId;

    @Column(nullable = false, length = 64)
    private String requestFingerprint;

    @Getter(AccessLevel.NONE)
    @Version
    private Long version;

    public Errand(
            String id,
            String requesterId,
            String description,
            String pickupSupplierId,
            String deliverySupplierId,
            long creditAmount,
            int deliveryDurationMinutes,
            Instant expiresAt,
            Instant createdAt,
            String requestFingerprint) {
        this.id = id;
        this.requesterId = requesterId;
        this.description = description;
        this.pickupSupplierId = pickupSupplierId;
        this.deliverySupplierId = deliverySupplierId;
        this.creditAmount = creditAmount;
        this.deliveryDurationMinutes = deliveryDurationMinutes;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.requestFingerprint = requestFingerprint;
        this.status = Status.OPEN;
    }

    public Optional<OrderProblem> accept(
            String courierId, String orderId, long expectedVersion, Instant now) {
        if (requesterId.equals(courierId))
            return Optional.of(
                    new OrderProblem(
                            OrderProblem.Code.FORBIDDEN, "You cannot accept your own errand."));
        if (getVersion() != expectedVersion)
            return Optional.of(OrderProblem.conflict("The errand changed. Refresh and try again."));
        if (status != Status.OPEN || !now.isBefore(expiresAt) || this.orderId != null)
            return Optional.of(OrderProblem.conflict("This errand is no longer available."));
        this.status = Status.ACCEPTED;
        this.orderId = orderId;
        return Optional.empty();
    }

    public long getVersion() {
        return version == null ? 0 : version;
    }
}
