package sg.edu.nus.foc.order.domain;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="order_checkpoints", uniqueConstraints=@UniqueConstraint(columnNames={"order_id","status"}))
public class Checkpoint {
    @Id @Column(length=36) private String id;
    @Column(nullable=false, length=36) private String orderId;
    @Column(nullable=false, length=128) private String courierId;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private Order.Status status;
    @Column(nullable=false) private Instant occurredAt;
    @Column(length=128) private String supplierId;
    protected Checkpoint() {}
    public Checkpoint(Order order, Instant now, String supplierId) {
        this.id=UUID.randomUUID().toString(); this.orderId=order.getId(); this.courierId=order.getCourierId();
        this.status=order.getStatus(); this.occurredAt=now; this.supplierId=supplierId;
    }
    public String getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getCourierId() { return courierId; }
    public Order.Status getStatus() { return status; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getSupplierId() { return supplierId; }
}
