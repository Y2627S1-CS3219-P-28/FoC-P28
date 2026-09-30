package sg.edu.nus.foc.order.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_checkpoints", uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "status"}))
public class OrderCheckpoint {
    @Id
    private String id = UUID.randomUUID().toString();
    @Column(name = "order_id", nullable = false)
    private String orderId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    @Column(nullable = false)
    private Instant occurredAt;
    private String actorId;
    private String supplierId;

    protected OrderCheckpoint() {}
    public OrderCheckpoint(String orderId, OrderStatus status, Instant occurredAt, String actorId, String supplierId) {
        this.orderId = orderId; this.status = status; this.occurredAt = occurredAt;
        this.actorId = actorId; this.supplierId = supplierId;
    }
    public String getId() { return id; }
    public String getOrderId() { return orderId; }
    public OrderStatus getStatus() { return status; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getActorId() { return actorId; }
    public String getSupplierId() { return supplierId; }
}
