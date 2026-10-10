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
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(name = "ux_order_repost_original", columnNames = "original_order_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    public static final Duration MINIMUM_POSTING_WINDOW = Duration.ofMinutes(30);
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
        return openWithId(UUID.randomUUID().toString(), requesterId, description, pickup, delivery,
                credits, duration, createdAt, expiresAt, repostPlan);
    }

    /** Durable creation retries use the candidate business ID saved before Credit I/O. */
    public static Order openWithId(String candidateId, String requesterId, String description, String pickup, String delivery,
                             long credits, int duration, Instant createdAt, Instant expiresAt, RepostPlan repostPlan) {
        UUID.fromString(candidateId);
        List<OrderProblem.Detail> errors = new ArrayList<>();
        if (requesterId == null || requesterId.isBlank()) errors.add(new OrderProblem.Detail("requesterId", "A verified requester is required."));
        validateRepostFields(description, credits, duration, createdAt.plus(MINIMUM_POSTING_WINDOW), expiresAt, errors);
        if (pickup == null || pickup.isBlank()) errors.add(new OrderProblem.Detail("pickupSupplierId", "Select a pickup supplier."));
        if (delivery == null || delivery.isBlank()) errors.add(new OrderProblem.Detail("deliverySupplierId", "Select a delivery supplier."));
        else if (delivery.equals(pickup)) errors.add(new OrderProblem.Detail("deliverySupplierId", "Delivery supplier must differ from pickup supplier."));
        rejectInvalidFields(errors);
        Order order = new Order(candidateId, requesterId, description, pickup, delivery,
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

    public Order createManualRepost(String description, long credits, int duration, Instant createdAt, Instant expiresAt) {
        if (status != OrderStatus.EXPIRED || repostedOrderId != null) {
            throw OrderProblem.conflict("Order is not eligible for repost.");
        }
        List<OrderProblem.Detail> errors = new ArrayList<>();
        validateRepostFields(description, credits, duration, createdAt.plus(MINIMUM_POSTING_WINDOW), expiresAt, errors);
        rejectInvalidFields(errors);
        return createRepost(description, credits, duration, createdAt, expiresAt);
    }

    private static void validateRepostFields(String description, long credits, int duration,
            Instant minimumExpiry, Instant expiresAt, List<OrderProblem.Detail> errors) {
        if (description == null || description.isBlank()) errors.add(new OrderProblem.Detail("itemDescription", "Describe what you need."));
        else if (description.length() > 100) errors.add(new OrderProblem.Detail("itemDescription", "Description must be 100 characters or fewer."));
        if (credits < 1) errors.add(new OrderProblem.Detail("offeredCredits", "Offered credits must be at least 1."));
        if (duration < 15) errors.add(new OrderProblem.Detail("deliveryTimeLimitMinutes", "Delivery time must be at least 15 minutes."));
        if (expiresAt == null) errors.add(new OrderProblem.Detail("expiresAt", "Choose an order expiry time."));
        else if (expiresAt.isBefore(minimumExpiry)) errors.add(new OrderProblem.Detail("expiresAt", "Order expiry must be at least 30 minutes from now. Choose a later time."));
    }

    private static void rejectInvalidFields(List<OrderProblem.Detail> errors) {
        if (!errors.isEmpty()) {
            throw new OrderProblem("VALIDATION_ERROR", String.join(" ", errors.stream().map(OrderProblem.Detail::getMessage).toList()), errors);
        }
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
