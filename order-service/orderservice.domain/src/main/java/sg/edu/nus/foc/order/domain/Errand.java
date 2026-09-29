package sg.edu.nus.foc.order.domain;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="errands")
public class Errand {
    @Id @Column(length=36) private String id;
    @Column(nullable=false, length=128) private String requesterId;
    @Column(nullable=false, length=100) private String description;
    @Column(nullable=false, length=128) private String pickupSupplierId;
    @Column(nullable=false, length=128) private String deliverySupplierId;
    @Column(nullable=false) private long creditAmount;
    @Column(nullable=false) private int deliveryDurationMinutes;
    @Column(nullable=false) private Instant expiresAt;
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false, length=20) private String status;
    @Column(length=36) private String orderId;
    @Column(nullable=false, length=64) private String requestFingerprint;
    @Version private Long version;
    protected Errand() {}
    public Errand(String id, String requesterId, String description, String pickupSupplierId,
        String deliverySupplierId, long creditAmount, int deliveryDurationMinutes,
        Instant expiresAt, Instant createdAt, String requestFingerprint) {
        this.id=id; this.requesterId=requesterId; this.description=description;
        this.pickupSupplierId=pickupSupplierId; this.deliverySupplierId=deliverySupplierId;
        this.creditAmount=creditAmount; this.deliveryDurationMinutes=deliveryDurationMinutes;
        this.expiresAt=expiresAt; this.createdAt=createdAt; this.requestFingerprint=requestFingerprint;
        this.status="OPEN";
    }
    public void accept(String courierId, String orderId, long expectedVersion, Instant now) {
        if (requesterId.equals(courierId)) throw new OrderProblem("FORBIDDEN", "You cannot accept your own errand.");
        if (getVersion()!=expectedVersion) throw OrderProblem.conflict("The errand changed. Refresh and try again.");
        if (!"OPEN".equals(status) || !now.isBefore(expiresAt) || this.orderId!=null)
            throw OrderProblem.conflict("This errand is no longer available.");
        this.status="ACCEPTED"; this.orderId=orderId;
    }
    public String getId() { return id; }
    public String getRequesterId() { return requesterId; }
    public String getDescription() { return description; }
    public String getPickupSupplierId() { return pickupSupplierId; }
    public String getDeliverySupplierId() { return deliverySupplierId; }
    public long getCreditAmount() { return creditAmount; }
    public int getDeliveryDurationMinutes() { return deliveryDurationMinutes; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
    public String getOrderId() { return orderId; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public long getVersion() { return version==null ? 0 : version; }
}
