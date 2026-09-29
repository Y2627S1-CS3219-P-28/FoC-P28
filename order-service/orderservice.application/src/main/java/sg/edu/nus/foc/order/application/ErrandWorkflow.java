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
public class ErrandWorkflow extends BaseService {
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

    public ResultWrapper<ErrandView> create(CreateInput input) {
        String id = key("create", input.requesterId(), input.commandId());
        String fingerprint = fingerprint(input.toString());
        Optional<Errand> existing = errands.findById(id);
        if (existing.isPresent()) {
            if (!existing.get().getRequestFingerprint().equals(fingerprint)) {
                return error(
                        OrderProblem.conflict("Command ID was already used with different input."));
            }
            return view(existing.get());
        }
        Instant now = now();
        List<OrderProblem.Detail> errors = new ArrayList<>();
        if (input.expiresAt().isBefore(now.plusSeconds(1800))) {
            errors.add(
                    new OrderProblem.Detail("expiresAt", "Must be at least 30 minutes from now."));
        }
        if (input.pickupSupplierId().equals(input.deliverySupplierId())) {
            errors.add(
                    new OrderProblem.Detail(
                            "deliverySupplierId", "Pickup and delivery must be different."));
        }
        if (input.description().codePoints().anyMatch(Character::isISOControl)) {
            errors.add(
                    new OrderProblem.Detail("description", "Control characters are not allowed."));
        }
        if (!errors.isEmpty()) {
            return validationError(errors);
        }
        Errand errand =
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
        ResultWrapper<ErrandView> result = view(errand);
        if (result.isError()) {
            return result;
        }
        if (!credits.reserve(id, input.requesterId(), input.creditAmount())) {
            return error(
                    new OrderProblem(
                            OrderProblem.Code.RESERVATION_REJECTED,
                            "Credit reservation was rejected."));
        }
        // Register before database writes; covers both write and commit failures.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            credits.release(id, input.requesterId());
                        }
                    }
                });
        errands.saveAndFlush(errand);
        log.info(
                "action=create errandId={} actorId={} at={} peerMode=stub",
                id,
                input.requesterId(),
                now);
        return result;
    }

    @Transactional(readOnly = true)
    public ResultWrapper<Page<ErrandView>> available(int page, int size) {
        Page<Errand> listings =
                errands.findByStatusAndExpiresAtAfter(
                        Errand.Status.OPEN,
                        clock.instant(),
                        PageRequest.of(
                                page - 1,
                                size,
                                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"))));
        List<ErrandView> views = new ArrayList<>();
        for (Errand errand : listings) {
            ResultWrapper<ErrandView> result = view(errand);
            if (result.isError()) {
                return error(result.getError());
            }
            views.add(result.getData());
        }
        return ok(new PageImpl<>(views, listings.getPageable(), listings.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public ResultWrapper<ErrandView> getErrand(String id) {
        Optional<Errand> errand = errands.findById(id);
        if (errand.isEmpty()) {
            return error(missing("Errand"));
        }
        return view(errand.get());
    }

    @Transactional(readOnly = true)
    public ResultWrapper<OrderView> getOrder(String id) {
        Optional<Order> order = orders.findById(id);
        if (order.isEmpty()) {
            return error(missing("Delivery"));
        }
        return orderView(order.get());
    }

    @Transactional(readOnly = true)
    public ResultWrapper<List<SupplierGateway.Supplier>> suppliers() {
        return ok(suppliers.list());
    }

    public ResultWrapper<OrderView> accept(String id, ActionInput input) {
        Optional<Errand> found = errands.lockById(id);
        if (found.isEmpty()) {
            return error(missing("Errand"));
        }
        Errand errand = found.get();
        String receiptId = key("accept", id, input.commandId());
        String fingerprint = fingerprint(input.toString());
        Optional<CommandReceipt> receipt = commands.findById(receiptId);
        if (receipt.isPresent()) {
            Optional<OrderProblem> problem = receipt.get().verify(fingerprint);
            return problem.isPresent() ? error(problem.get()) : getOrder(errand.getOrderId());
        }
        ResultWrapper<ErrandView> listing = view(errand);
        if (listing.isError()) {
            return error(listing.getError());
        }
        Instant now = now();
        String orderId = UUID.randomUUID().toString();
        Optional<OrderProblem> rejection =
                errand.accept(input.actorId(), orderId, input.expectedVersion(), now);
        if (rejection.isPresent()) {
            return error(rejection.get());
        }
        Order order = new Order(orderId, errand, input.actorId(), now);
        orders.saveAndFlush(order);
        errands.flush();
        checkpoints.save(new Checkpoint(order, now, null));
        commands.save(new CommandReceipt(receiptId, fingerprint));
        log.info(
                "action=accept errandId={} orderId={} actorId={} at={}",
                id,
                orderId,
                input.actorId(),
                now);
        return ok(
                new OrderView(
                        order,
                        listing.getData(),
                        checkpoints.findByOrderIdOrderByOccurredAtAscIdAsc(orderId)));
    }

    public ResultWrapper<OrderView> progress(String id, ActionInput input, Order.Status target) {
        Optional<Order> found = orders.lockById(id);
        if (found.isEmpty()) {
            return error(missing("Delivery"));
        }
        Order order = found.get();
        String receiptId = key("progress", id, input.commandId());
        String fingerprint = fingerprint(target + "|" + input);
        Optional<CommandReceipt> receipt = commands.findById(receiptId);
        if (receipt.isPresent()) {
            Optional<OrderProblem> problem = receipt.get().verify(fingerprint);
            return problem.isPresent() ? error(problem.get()) : orderView(order);
        }
        ResultWrapper<ErrandView> listing = getErrand(order.getErrandId());
        if (listing.isError()) {
            return error(listing.getError());
        }
        Instant now = now();
        Optional<OrderProblem> rejection =
                order.progress(input.actorId(), input.expectedVersion(), target, now);
        if (rejection.isPresent()) {
            return error(rejection.get());
        }
        Errand errand = listing.getData().errand();
        String supplierId =
                switch (target) {
                    case PICKED_UP -> errand.getPickupSupplierId();
                    case DELIVERED -> errand.getDeliverySupplierId();
                    default -> null;
                };
        checkpoints.save(new Checkpoint(order, now, supplierId));
        commands.save(new CommandReceipt(receiptId, fingerprint));
        orders.flush();
        log.info("action={} orderId={} actorId={} at={}", target, id, input.actorId(), now);
        return ok(
                new OrderView(
                        order,
                        listing.getData(),
                        checkpoints.findByOrderIdOrderByOccurredAtAscIdAsc(id)));
    }

    private ResultWrapper<ErrandView> view(Errand errand) {
        Optional<SupplierGateway.Supplier> pickup = suppliers.resolve(errand.getPickupSupplierId());
        Optional<SupplierGateway.Supplier> delivery =
                suppliers.resolve(errand.getDeliverySupplierId());
        if (pickup.isEmpty() || delivery.isEmpty()) {
            return error(
                    new OrderProblem(
                            OrderProblem.Code.VALIDATION_ERROR,
                            "Choose a supplier from the sample catalogue.",
                            List.of(
                                    new OrderProblem.Detail(
                                            "supplierId", "Unknown sample supplier."))));
        }
        return ok(new ErrandView(errand, pickup.get(), delivery.get()));
    }

    private ResultWrapper<OrderView> orderView(Order order) {
        return getErrand(order.getErrandId())
                .map(
                        listing ->
                                new OrderView(
                                        order,
                                        listing,
                                        checkpoints.findByOrderIdOrderByOccurredAtAscIdAsc(
                                                order.getId())));
    }

    private static OrderProblem missing(String name) {
        return new OrderProblem(OrderProblem.Code.NOT_FOUND, name + " was not found.");
    }

    private Instant now() {
        return clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
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
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
