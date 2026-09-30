package sg.edu.nus.foc.order.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(name = "ux_order_repost_original", columnNames = "original_order_id"))
public class Order {
    @Id
    private String id;
    @Column(nullable = false, length = 128)
    private String requesterId;
    @Column(length = 128)
    private String courierId;
    @Column(nullable = false, length = 100)
    private String itemDescription;
    @Column(nullable = false, length = 128)
    private String pickupSupplierId;
    @Column(nullable = false, length = 128)
    private String deliverySupplierId;
    @Column(nullable = false)
    private long offeredCredits;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private int deliveryTimeLimitMinutes;
    @Version
    private long version;
    @Column(name = "original_order_id", length = 36)
    private String originalOrderId;
    @Column(name = "reposted_order_id", length = 36)
    private String repostedOrderId;
    @Embedded
    private RepostPlan repostPlan;

    protected Order() {}

    private Order(String id, String requesterId, String description, String pickup, String delivery,
                   long credits, int duration, Instant createdAt, Instant expiresAt, String originalOrderId) {
        this.id = id; this.requesterId = requesterId; this.itemDescription = description;
        this.pickupSupplierId = pickup; this.deliverySupplierId = delivery; this.offeredCredits = credits;
        this.deliveryTimeLimitMinutes = duration; this.createdAt = createdAt; this.expiresAt = expiresAt;
        this.originalOrderId = originalOrderId; this.status = OrderStatus.OPEN;
    }

    public static Order open(String requesterId, String description, String pickup, String delivery,
                             long credits, int duration, Instant createdAt, Instant expiresAt) {
        if (requesterId == null || requesterId.isBlank() || description == null || description.isBlank()
                || pickup == null || delivery == null || pickup.equals(delivery) || credits <= 0
                || duration < 15 || expiresAt == null || !expiresAt.isAfter(createdAt)
                || expiresAt.isBefore(createdAt.plusSeconds(30 * 60L))) {
            throw new OrderProblem("VALIDATION_ERROR", "Invalid order creation data.");
        }
        return new Order(UUID.randomUUID().toString(), requesterId, description, pickup, delivery,
                credits, duration, createdAt, expiresAt, null);
    }

    public void accept(String courierId, long expectedVersion, Instant now) {
        requireVersion(expectedVersion); requireStatus(OrderStatus.OPEN);
        if (requesterId.equals(courierId)) throw OrderProblem.forbidden("Requester cannot accept their own order.");
        if (!now.isBefore(expiresAt)) throw OrderProblem.conflict("Order has expired.");
        if (courierId == null || courierId.isBlank()) throw new OrderProblem("VALIDATION_ERROR", "Courier is required.");
        this.courierId = courierId; this.status = OrderStatus.ACCEPTED;
    }

    public void start(String courierId, long expectedVersion) { progress(courierId, expectedVersion, OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS); }
    public void markPickedUp(String courierId, long expectedVersion) { progress(courierId, expectedVersion, OrderStatus.IN_PROGRESS, OrderStatus.PICKED_UP); }
    public void markDelivered(String courierId, long expectedVersion) { progress(courierId, expectedVersion, OrderStatus.PICKED_UP, OrderStatus.DELIVERED); }

    private void progress(String actor, long expectedVersion, OrderStatus from, OrderStatus to) {
        requireVersion(expectedVersion); requireStatus(from);
        if (courierId == null || !courierId.equals(actor)) throw OrderProblem.forbidden("Only the assigned courier may act.");
        status = to;
    }

    public void confirmCompletion(String actor, long expectedVersion) {
        requireVersion(expectedVersion); requireStatus(OrderStatus.DELIVERED);
        if (!requesterId.equals(actor)) throw OrderProblem.forbidden("Only the requester may confirm completion.");
        status = OrderStatus.COMPLETED;
    }

    public void cancelOpen(String actor, long expectedVersion) {
        requireVersion(expectedVersion); requireStatus(OrderStatus.OPEN);
        if (!requesterId.equals(actor)) throw OrderProblem.forbidden("Only the requester may cancel.");
        status = OrderStatus.CANCELLED;
    }

    public void expire(long expectedVersion, Instant now) {
        requireVersion(expectedVersion); requireStatus(OrderStatus.OPEN);
        if (now.isBefore(expiresAt)) throw OrderProblem.conflict("Order is not due for expiry.");
        status = OrderStatus.EXPIRED;
    }

    public void configureReposting(RepostPlan plan, String actor, long expectedVersion) {
        requireVersion(expectedVersion); requireStatus(OrderStatus.OPEN);
        if (!requesterId.equals(actor)) throw OrderProblem.forbidden("Only the requester may configure reposting.");
        this.repostPlan = plan;
    }

    public boolean eligibleForAutomaticRepost(Instant now) {
        return status == OrderStatus.EXPIRED && repostedOrderId == null && repostPlan != null
                && repostPlan.enabled() && repostPlan.dueAt(now) && !repostPlan.used();
    }

    public void linkRepost(String repostId) {
        if (repostedOrderId != null) throw OrderProblem.conflict("Order already has a repost.");
        repostedOrderId = repostId;
        if (repostPlan != null) repostPlan.markUsed();
    }

    public Order createRepost(String description, long credits, int duration, Instant createdAt, Instant expiresAt) {
        if (status != OrderStatus.EXPIRED || repostedOrderId != null) throw OrderProblem.conflict("Order is not eligible for repost.");
        return new Order(UUID.randomUUID().toString(), requesterId, description, pickupSupplierId,
                deliverySupplierId, credits, duration, createdAt, expiresAt, id);
    }

    public void requireVersion(long expected) { if (version != expected) throw OrderProblem.conflict("Order version is stale."); }
    private void requireStatus(OrderStatus expected) { if (status != expected) throw OrderProblem.conflict("Order must be " + expected + "."); }

    public String getId() { return id; }
    public String getRequesterId() { return requesterId; }
    public String getCourierId() { return courierId; }
    public String getItemDescription() { return itemDescription; }
    public String getPickupSupplierId() { return pickupSupplierId; }
    public String getDeliverySupplierId() { return deliverySupplierId; }
    public long getOfferedCredits() { return offeredCredits; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getDeliveryTimeLimitMinutes() { return deliveryTimeLimitMinutes; }
    public long getVersion() { return version; }
    public String getOriginalOrderId() { return originalOrderId; }
    public String getRepostedOrderId() { return repostedOrderId; }
    public RepostPlan getRepostPlan() { return repostPlan; }
}
