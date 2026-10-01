package sg.edu.nus.foc.order.application;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.foc.order.domain.CommandReceipt;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderCheckpoint;
import sg.edu.nus.foc.order.domain.OrderProblem;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.domain.RepostPlan;
import sg.edu.nus.foc.order.domain.repository.CommandReceiptRepository;
import sg.edu.nus.foc.order.domain.repository.OrderCheckpointRepository;
import sg.edu.nus.foc.order.domain.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderRepostService {
    private final OrderRepository orders;
    private final OrderCheckpointRepository checkpoints;
    private final CommandReceiptRepository receipts;
    private final SupplierServicePort suppliers;
    private final CreditServicePort credits;
    private final UserServicePort users;
    private final OrderAuditLogger audit;

    @Transactional
    public Order configure(
            String commandId,
            String id,
            String actor,
            long version,
            RepostPlan plan,
            String authorization) {
        throw OrderProblem.conflict(
                "Automatic repost settings must be chosen when the order is created.");
    }

    @Transactional
    public Order automatic(String commandId, String id, Instant now, String authorization) {
        Optional<CommandReceipt> previous = receipts.findExisting(
                "AUTO_REPOST",
                commandId);
        if (previous.isPresent()) {
            return orders.get(previous.get().getOrderId()).orElseThrow();
        }

        Order original = findForUpdate(id);
        if (!original.eligibleForAutomaticRepost(now)) {
            throw OrderProblem.conflict("Order is not eligible for automatic repost.");
        }

        RepostPlan plan = original.getRepostPlan();
        suppliers.validatePair(
                original.getPickupSupplierId(),
                original.getDeliverySupplierId(),
                authorization);

        Order repost = original.createRepost(
                original.getItemDescription(),
                plan.getCreditAmount(),
                plan.getDeliveryDurationMinutes(),
                now,
                now.plusSeconds(plan.getDeliveryDurationMinutes() * 60L));

        return saveRepost("AUTO_REPOST", commandId, original, repost, now, authorization);
    }

    @Transactional
    public Order manual(
            String commandId,
            String id,
            String actor,
            long version,
            String description,
            long creditsAmount,
            int duration,
            Instant expiresAt,
            String authorization) {
        Optional<CommandReceipt> previous = receipts.findExisting(
                "MANUAL_REPOST",
                commandId);
        if (previous.isPresent()) {
            return orders.get(previous.get().getOrderId()).orElseThrow();
        }

        String authenticatedActor = users.verifyRequester(actor, authorization);
        Order original = findForUpdate(id);
        original.requireVersion(version);

        if (!original.getRequesterId().equals(authenticatedActor)) {
            throw OrderProblem.forbidden("Only the requester may repost.");
        }
        if (original.getStatus() != OrderStatus.EXPIRED) {
            throw OrderProblem.conflict("Only an expired order may be reposted.");
        }

        suppliers.validatePair(
                original.getPickupSupplierId(),
                original.getDeliverySupplierId(),
                authorization);

        Instant createdAt = Instant.now();
        Order repost = original.createRepost(
                description,
                creditsAmount,
                duration,
                createdAt,
                expiresAt);

        return saveRepost(
                "MANUAL_REPOST",
                commandId,
                original,
                repost,
                createdAt,
                authorization);
    }

    private Order saveRepost(
            String operation,
            String commandId,
            Order original,
            Order repost,
            Instant createdAt,
            String authorization) {
        credits.reserve(
                repost.getId(),
                repost.getRequesterId(),
                repost.getOfferedCredits(),
                authorization);

        original.linkRepost(repost.getId());
        orders.save(original);
        Order saved = orders.save(repost);

        checkpoints.save(new OrderCheckpoint(
                saved.getId(),
                saved.getStatus(),
                createdAt,
                saved.getRequesterId(),
                null));
        receipts.save(new CommandReceipt(operation, commandId, saved.getId(), Instant.now()));
        audit.dependency("credit-service", "reserve", saved.getId(), "accepted");
        audit.action(operation, saved.getId(), saved.getRequesterId(), commandId, "accepted");
        return saved;
    }

    private Order findForUpdate(String id) {
        return orders.getForUpdate(id)
                .orElseThrow(() -> OrderProblem.notFound("Order not found."));
    }
}
