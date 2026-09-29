package sg.edu.nus.foc.order.application;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import sg.edu.nus.foc.order.domain.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service
@Profile("!prod & (local | test)")
@Transactional
@RequiredArgsConstructor
public class ErrandWorkflow {
    public record CreateInput(
            String commandId,
            String requesterId,
            String description,
            String pickupSupplierId,
            String deliverySupplierId,
            long creditAmount,
            int deliveryDurationMinutes,
            Instant expiresAt) {}

    public record ActionInput(String commandId, String actorId, long expectedVersion) {}

    public record ErrandView(
            Errand errand, SupplierGateway.Supplier pickup, SupplierGateway.Supplier delivery) {}

    public record OrderView(Order order, ErrandView errand, List<Checkpoint> checkpoints) {}

    private static final Logger log = LoggerFactory.getLogger(ErrandWorkflow.class);
    private final ErrandRepository errands;
    private final OrderRepository orders;
    private final CheckpointRepository checkpoints;
    private final CommandReceiptRepository commands;
    private final SupplierGateway suppliers;
    private final CreditGateway credits;
    private final Clock clock;

    public ErrandView create(CreateInput input) {
        String id = key("create", input.requesterId(), input.commandId());
        String fingerprint = fingerprint(input.toString());
        var existing = errands.findById(id);
        if (existing.isPresent()) {
            if (!existing.get().getRequestFingerprint().equals(fingerprint))
                throw OrderProblem.conflict("Command ID was already used with different input.");
            return view(existing.get());
        }
        Instant now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var errors = new ArrayList<OrderProblem.Detail>();
        if (input.expiresAt().isBefore(now.plusSeconds(1800)))
            errors.add(
                    new OrderProblem.Detail("expiresAt", "Must be at least 30 minutes from now."));
        if (input.pickupSupplierId().equals(input.deliverySupplierId()))
            errors.add(
                    new OrderProblem.Detail(
                            "deliverySupplierId", "Pickup and delivery must be different."));
        if (input.description().codePoints().anyMatch(Character::isISOControl))
            errors.add(
                    new OrderProblem.Detail("description", "Control characters are not allowed."));
        if (!errors.isEmpty())
            throw new OrderProblem("VALIDATION_ERROR", "Check the highlighted fields.", errors);
        suppliers.validatePair(input.pickupSupplierId(), input.deliverySupplierId());
        if (!credits.reserve(id, input.requesterId(), input.creditAmount()))
            throw new OrderProblem("RESERVATION_REJECTED", "Credit reservation was rejected.");
        // Handles rollback during save AND failure at transaction commit. Only stub adapters are
        // wired.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) credits.release(id, input.requesterId());
                    }
                });
        var errand =
                new Errand(
                        id,
                        input.requesterId(),
                        input.description(),
                        input.pickupSupplierId(),
                        input.deliverySupplierId(),
                        input.creditAmount(),
                        input.deliveryDurationMinutes(),
                        input.expiresAt(),
                        now,
                        fingerprint);
        errands.saveAndFlush(errand);
        log.info(
                "action=create errandId={} actorId={} at={} peerMode=stub",
                id,
                input.requesterId(),
                now);
        return view(errand);
    }

    @Transactional(readOnly = true)
    public Page<ErrandView> available(int page, int size) {
        return errands.findByStatusAndExpiresAtAfter(
                        "OPEN",
                        clock.instant(),
                        PageRequest.of(
                                page - 1,
                                size,
                                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"))))
                .map(this::view);
    }

    @Transactional(readOnly = true)
    public ErrandView getErrand(String id) {
        return view(findErrand(id));
    }

    @Transactional(readOnly = true)
    public OrderView getOrder(String id) {
        return view(orders.findById(id).orElseThrow(() -> missing("Delivery")));
    }

    @Transactional(readOnly = true)
    public List<SupplierGateway.Supplier> suppliers() {
        return suppliers.list();
    }

    public OrderView accept(String id, ActionInput input) {
        var errand = errands.lockById(id).orElseThrow(() -> missing("Errand"));
        String receipt = key("accept", id, input.commandId());
        String fingerprint = fingerprint(input.toString());
        if (replayed(receipt, fingerprint)) return getOrder(errand.getOrderId());
        Instant now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        String orderId = UUID.randomUUID().toString();
        errand.accept(input.actorId(), orderId, input.expectedVersion(), now);
        var order = new Order(orderId, errand, input.actorId(), now);
        orders.saveAndFlush(order);
        errands.flush();
        checkpoints.save(new Checkpoint(order, now, null));
        commands.save(new CommandReceipt(receipt, fingerprint));
        log.info(
                "action=accept errandId={} orderId={} actorId={} at={}",
                id,
                orderId,
                input.actorId(),
                now);
        return view(order);
    }

    public OrderView progress(String id, ActionInput input, Order.Status target) {
        var order = orders.lockById(id).orElseThrow(() -> missing("Delivery"));
        String receipt = key("progress", id, input.commandId());
        String fingerprint = fingerprint(target + "|" + input);
        if (replayed(receipt, fingerprint)) return view(order);
        Instant now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        order.progress(input.actorId(), input.expectedVersion(), target, now);
        var errand = findErrand(order.getErrandId());
        String supplierId =
                switch (target) {
                    case PICKED_UP -> errand.getPickupSupplierId();
                    case DELIVERED -> errand.getDeliverySupplierId();
                    default -> null;
                };
        checkpoints.save(new Checkpoint(order, now, supplierId));
        commands.save(new CommandReceipt(receipt, fingerprint));
        orders.flush();
        log.info("action={} orderId={} actorId={} at={}", target, id, input.actorId(), now);
        return view(order);
    }

    private boolean replayed(String id, String fingerprint) {
        var receipt = commands.findById(id);
        receipt.ifPresent(value -> value.verify(fingerprint));
        return receipt.isPresent();
    }

    private Errand findErrand(String id) {
        return errands.findById(id).orElseThrow(() -> missing("Errand"));
    }

    private static OrderProblem missing(String name) {
        return new OrderProblem("NOT_FOUND", name + " was not found.");
    }

    private ErrandView view(Errand e) {
        return new ErrandView(
                e,
                suppliers.resolve(e.getPickupSupplierId()),
                suppliers.resolve(e.getDeliverySupplierId()));
    }

    private OrderView view(Order o) {
        return new OrderView(
                o,
                view(findErrand(o.getErrandId())),
                checkpoints.findByOrderIdOrderByOccurredAtAscIdAsc(o.getId()));
    }

    private static String key(String operation, String resource, String command) {
        return UUID.nameUUIDFromBytes(
                        (operation + "\0" + resource + "\0" + command)
                                .getBytes(StandardCharsets.UTF_8))
                .toString();
    }

    private static String fingerprint(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
