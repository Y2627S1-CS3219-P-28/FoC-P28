package sg.edu.nus.foc.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(name = "ux_order_repost_original", columnNames = "original_order_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    public static final Duration AUTOMATIC_COMPLETION_DELAY = Duration.ofHours(48);

    @Id
    @Column(name = "row_id", nullable = false)
    private UUID rowId;
    @Column(nullable = false, unique = true, length = 36)
    private String id;
    @Transient
    private UUID attemptId;
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
    @Column(length = 40)
    private String repostFailureCode;
    @Column(length = 256)
    private String repostFailureMessage;
    private Instant repostFailureAt;

    private Order(String id, String requesterId, String description, String pickup, String delivery,
                   long credits, int duration, Instant createdAt, Instant expiresAt, String originalOrderId) {
        this.rowId = UUID.randomUUID();
        this.id = id;
        this.requesterId = requesterId;
        this.itemDescription = description;
        this.pickupSupplierId = pickup;
        this.deliverySupplierId = delivery;
        this.offeredCredits = credits;
        this.deliveryTimeLimitMinutes = duration;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.originalOrderId = originalOrderId;
        this.status = OrderStatus.OPEN;
    }

    public static Order open(String requesterId, String description, String pickup, String delivery,
                             long credits, int duration, Instant createdAt, Instant expiresAt) {
        return open(requesterId, description, pickup, delivery, credits, duration, createdAt, expiresAt, null);
    }

    public static Order open(String requesterId, String description, String pickup, String delivery,
                             long credits, int duration, Instant createdAt, Instant expiresAt, RepostPlan repostPlan) {
        if (requesterId == null || requesterId.isBlank() || description == null || description.isBlank()
                || pickup == null || delivery == null || pickup.equals(delivery) || credits <= 0
                || duration < 15 || expiresAt == null || !expiresAt.isAfter(createdAt)
                || expiresAt.isBefore(createdAt.plusSeconds(30 * 60L))) {
            throw new OrderProblem("VALIDATION_ERROR", "Invalid order creation data.");
        }
        Order order = new Order(UUID.randomUUID().toString(), requesterId, description, pickup, delivery,
                credits, duration, createdAt, expiresAt, null);
        if (repostPlan != null) {
            repostPlan.validateAgainst(expiresAt);
        }
        order.repostPlan = repostPlan;
        return order;
    }

    public void accept(String courierId, long expectedVersion, Instant now) {
        validateAcceptance(courierId, expectedVersion, now);
        this.courierId = courierId;
        this.status = OrderStatus.ACCEPTED;
    }

    public void validateAcceptance(String courierId, long expectedVersion, Instant now) {
        requireVersion(expectedVersion);
        requireStatus(OrderStatus.OPEN);
        if (requesterId.equals(courierId)) {
            throw OrderProblem.forbidden("Requester cannot accept their own order.");
        }
        if (this.courierId != null) {
            throw OrderProblem.conflict("Order already has an assigned courier.");
        }
        if (!now.isBefore(expiresAt)) {
            throw OrderProblem.conflict("Order has expired.");
        }
        if (courierId == null || courierId.isBlank()) {
            throw new OrderProblem("VALIDATION_ERROR", "Courier is required.");
        }
    }

    public void start(String courierId, long expectedVersion) {
        progress(courierId, expectedVersion, OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS);
    }

    public void markPickedUp(String courierId, long expectedVersion) {
        progress(courierId, expectedVersion, OrderStatus.IN_PROGRESS, OrderStatus.PICKED_UP);
    }

    public void markDelivered(String courierId, long expectedVersion) {
        progress(courierId, expectedVersion, OrderStatus.PICKED_UP, OrderStatus.DELIVERED);
    }

    private void progress(String actor, long expectedVersion, OrderStatus from, OrderStatus to) {
        requireVersion(expectedVersion);
        requireStatus(from);
        if (courierId == null || !courierId.equals(actor)) {
            throw OrderProblem.forbidden("Only the assigned courier may act.");
        }
        status = to;
    }

    public void confirmCompletion(String actor, long expectedVersion) {
        validateCompletion(actor, expectedVersion);
        status = OrderStatus.COMPLETED;
    }

    public void completeAutomatically(long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(OrderStatus.DELIVERED);
        status = OrderStatus.COMPLETED;
    }

    public void validateCompletion(String actor, long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(OrderStatus.DELIVERED);
        if (!requesterId.equals(actor)) {
            throw OrderProblem.forbidden("Only the requester may confirm completion.");
        }
    }

    public void cancelOpen(String actor, long expectedVersion) {
        validateOpenCancellation(actor, expectedVersion);
        status = OrderStatus.CANCELLED;
    }

    public void validateOpenCancellation(String actor, long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(OrderStatus.OPEN);
        if (!requesterId.equals(actor)) {
            throw OrderProblem.forbidden("Only the requester may cancel.");
        }
    }

    public void reopenAfterAcceptedCancellation(String actor, long expectedVersion, Instant now) {
        validateAcceptedCancellation(actor, expectedVersion);
        if (!now.isBefore(expiresAt)) {
            throw OrderProblem.conflict("Expired order cannot be reopened.");
        }
        status = OrderStatus.OPEN;
        courierId = null;
    }

    public void abortAfterAcceptedCancellation(String actor, long expectedVersion, Instant now) {
        validateAcceptedCancellation(actor, expectedVersion);
        if (now.isBefore(expiresAt)) {
            throw OrderProblem.conflict("Unexpired order must be reopened after Credit confirms the hold.");
        }
        status = OrderStatus.EXPIRED;
        courierId = null;
    }

    public void validateAcceptedCancellation(String actor, long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(OrderStatus.ACCEPTED);
        if (courierId == null || !courierId.equals(actor)) {
            throw OrderProblem.forbidden("Only the assigned courier may cancel an accepted order.");
        }
    }

    public void expire(long expectedVersion, Instant now) {
        requireVersion(expectedVersion);
        requireStatus(OrderStatus.OPEN);
        if (now.isBefore(expiresAt)) {
            throw OrderProblem.conflict("Order is not due for expiry.");
        }
        status = OrderStatus.EXPIRED;
    }

    public void configureReposting(RepostPlan plan, String actor, long expectedVersion) {
        throw OrderProblem.conflict("Automatic repost settings must be chosen when the order is created.");
    }

    public boolean eligibleForAutomaticRepost(Instant now) {
        return status == OrderStatus.EXPIRED && repostedOrderId == null && repostPlan != null
                && repostPlan.isEnabled() && repostPlan.isDueAt(now) && !repostPlan.isUsed()
                && repostPlan.hasFutureExpiry(now);
    }

    public void linkRepost(String repostId) {
        if (repostedOrderId != null) {
            throw OrderProblem.conflict("Order already has a repost.");
        }
        repostedOrderId = repostId;
        repostFailureCode = null;
        repostFailureMessage = null;
        repostFailureAt = null;
        if (repostPlan != null) {
            repostPlan.markUsed();
        }
    }

    public Order createRepost(String description, long credits, int duration, Instant createdAt, Instant expiresAt) {
        if (status != OrderStatus.EXPIRED || repostedOrderId != null) {
            throw OrderProblem.conflict("Order is not eligible for repost.");
        }
        if (description == null || description.isBlank() || description.length() > 100
                || credits <= 0 || duration < 15 || expiresAt == null || !expiresAt.isAfter(createdAt)) {
            throw new OrderProblem("VALIDATION_ERROR", "Invalid repost details or expired new expiry.");
        }
        return new Order(UUID.randomUUID().toString(), requesterId, description, pickupSupplierId,
                deliverySupplierId, credits, duration, createdAt, expiresAt, id);
    }

    public void recordRepostFailure(String code, String message, Instant occurredAt) {
        requireStatus(OrderStatus.EXPIRED);
        if (repostedOrderId != null) {
            throw OrderProblem.conflict("Order already has a repost.");
        }
        if (repostFailureAt == null || !occurredAt.isBefore(repostFailureAt)) {
            repostFailureCode = code;
            repostFailureMessage = message;
            repostFailureAt = occurredAt;
        }
    }

    public void requireVersion(long expected) {
        if (version != expected) {
            throw OrderProblem.conflict("Order version is stale.");
        }
    }

    public static Order historicalAttempt(OrderCourierAttempt attempt) {
        Order history = new Order(attempt.getOrderId(), attempt.getRequesterId(),
                attempt.getItemDescription(), attempt.getPickupSupplierId(), attempt.getDeliverySupplierId(),
                attempt.getOfferedCredits(), attempt.getDeliveryTimeLimitMinutes(), attempt.getCreatedAt(),
                attempt.getExpiresAt(), attempt.getOriginalOrderId());
        history.rowId = attempt.getId();
        history.attemptId = attempt.getId();
        history.courierId = attempt.getCourierId();
        history.status = OrderStatus.ABORTED;
        history.version = attempt.getOrderVersion();
        history.repostedOrderId = attempt.getRepostedOrderId();
        return history;
    }

    private void requireStatus(OrderStatus expected) {
        if (status != expected) {
            throw OrderProblem.conflict("Order must be " + expected + ".");
        }
    }
}
