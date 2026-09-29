package sg.edu.nus.foc.order.domain;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="delivery_orders")
public class Order {
    public enum Status { ACCEPTED, IN_PROGRESS, PICKED_UP, DELIVERED }
    @Id @Column(length=36) private String id;
    @Column(nullable=false, length=36) private String errandId;
    @Column(nullable=false, length=128) private String courierId;
    @Column(nullable=false) private int deliveryDurationMinutes;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private Status status;
    @Column(nullable=false) private Instant acceptedAt;
    private Instant startedAt;
    private Instant pickedUpAt;
    private Instant deliveredAt;
    @Version private Long version;
    protected Order() {}
    public Order(String id, Errand errand, String courierId, Instant acceptedAt) {
        this.id=id; this.errandId=errand.getId(); this.courierId=courierId;
        this.deliveryDurationMinutes=errand.getDeliveryDurationMinutes();
        this.acceptedAt=acceptedAt; this.status=Status.ACCEPTED;
    }
    public void progress(String actorId, long expectedVersion, Status target, Instant now) {
        if (!courierId.equals(actorId)) throw new OrderProblem("FORBIDDEN", "Only the assigned courier may update this delivery.");
        if (getVersion()!=expectedVersion) throw OrderProblem.conflict("The delivery changed. Refresh and try again.");
        if (target.ordinal()!=status.ordinal()+1) throw OrderProblem.conflict("This action is not allowed from "+status+".");
        switch (target) {
            case IN_PROGRESS -> startedAt=now;
            case PICKED_UP -> pickedUpAt=now;
            case DELIVERED -> deliveredAt=now;
            default -> throw OrderProblem.conflict("Invalid progress action.");
        }
        status=target;
    }
    public String getId() { return id; }
    public String getErrandId() { return errandId; }
    public String getCourierId() { return courierId; }
    public int getDeliveryDurationMinutes() { return deliveryDurationMinutes; }
    public Status getStatus() { return status; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getPickedUpAt() { return pickedUpAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public long getVersion() { return version==null ? 0 : version; }
    public Instant getDeliveryDeadline() { return startedAt==null ? null : startedAt.plusSeconds(deliveryDurationMinutes*60L); }
}
