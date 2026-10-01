package sg.edu.nus.foc.order.adapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import sg.edu.nus.foc.order.application.CreditServicePort;
import sg.edu.nus.foc.order.application.SupplierServicePort;
import sg.edu.nus.foc.order.application.UserServicePort;

/** Local-only peer adapter with an in-memory credit reservation lifecycle. */
@Component
@Profile("!http")
@ConditionalOnProperty(
        name = "order.peers.mode",
        havingValue = "mock",
        matchIfMissing = true)
public class MockPeerAdapters implements UserServicePort, SupplierServicePort, CreditServicePort {
    private static final long INITIAL_CREDITS = 50;

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();
    private final Map<String, Outcome> outcomes = new ConcurrentHashMap<>();

    @Override
    public String verifyRequester(String userId, String authorization) {
        require(userId, "requester");
        account(userId);
        return userId;
    }

    @Override
    public String verifyCourier(String userId, String authorization) {
        require(userId, "courier");
        account(userId);
        return userId;
    }

    @Override
    public void validatePair(
            String pickupSupplierId,
            String deliverySupplierId,
            String authorization) {
        require(pickupSupplierId, "pickup supplier");
        require(deliverySupplierId, "delivery supplier");
        if (pickupSupplierId.equals(deliverySupplierId)) {
            throw new IllegalArgumentException("Supplier locations must differ.");
        }
    }

    @Override
    public synchronized void reserve(
            String orderId,
            String requesterId,
            long amount,
            String authorization) {
        require(orderId, "order");
        require(requesterId, "requester");
        requireAmount(amount);

        Reservation existing = reservations.get(orderId);
        if (existing != null) {
            if (!existing.getRequesterId().equals(requesterId)
                    || existing.getAmount() != amount
                    || existing.getState() != ReservationState.RESERVED) {
                throw new IllegalStateException(
                        "Order already has a conflicting credit reservation.");
            }
            return;
        }

        Account account = account(requesterId);
        if (account.available() < amount) {
            throw new IllegalStateException("Insufficient usable credit balance.");
        }
        account.reserved += amount;
        reservations.put(
                orderId,
                new Reservation(requesterId, amount, ReservationState.RESERVED));
    }

    @Override
    public synchronized void settle(
            String commandId,
            String orderId,
            String requesterId,
            String courierId,
            long amount,
            long expectedOrderVersion,
            String authorization) {
        require(commandId, "credit command");
        require(orderId, "order");
        require(requesterId, "requester");
        require(courierId, "courier");
        requireAmount(amount);

        Outcome previous = outcomes.get(commandId);
        if (previous != null) {
            if (!previous.getOrderId().equals(orderId)
                    || !previous.getKind().equals("SETTLED")) {
                throw new IllegalStateException(
                        "Credit command conflicts with an earlier outcome.");
            }
            return;
        }

        requireReservation(orderId, requesterId, amount);
        Account requester = account(requesterId);
        Account courier = account(courierId);
        requester.total -= amount;
        requester.reserved -= amount;
        courier.total += amount;
        reservations.put(
                orderId,
                new Reservation(requesterId, amount, ReservationState.SETTLED));
        outcomes.put(commandId, new Outcome(orderId, "SETTLED"));
    }

    @Override
    public synchronized void release(
            String commandId,
            String orderId,
            String requesterId,
            long amount,
            String outcome,
            long expectedOrderVersion,
            String authorization) {
        require(commandId, "credit command");
        require(orderId, "order");
        require(requesterId, "requester");
        requireAmount(amount);
        require(outcome, "credit outcome");

        if (!outcome.equals("CANCELLED") && !outcome.equals("EXPIRED")) {
            throw new IllegalArgumentException(
                    "Credit outcome must be CANCELLED or EXPIRED.");
        }

        Outcome previous = outcomes.get(commandId);
        if (previous != null) {
            if (!previous.getOrderId().equals(orderId)
                    || !previous.getKind().equals(outcome)) {
                throw new IllegalStateException(
                        "Credit command conflicts with an earlier outcome.");
            }
            return;
        }

        requireReservation(orderId, requesterId, amount);
        account(requesterId).reserved -= amount;
        reservations.put(
                orderId,
                new Reservation(requesterId, amount, ReservationState.RELEASED));
        outcomes.put(commandId, new Outcome(orderId, outcome));
    }

    /** Test/support view; not a production Credit API. */
    public synchronized CreditSnapshot creditSnapshot(String userId) {
        Account value = account(userId);
        return new CreditSnapshot(value.total, value.reserved, value.available());
    }

    /** Test/support view; not a production Credit API. */
    public synchronized String reservationState(String orderId) {
        Reservation reservation = reservations.get(orderId);
        return reservation == null ? null : reservation.getState().name();
    }

    private Account account(String userId) {
        return accounts.computeIfAbsent(userId, ignored -> new Account(INITIAL_CREDITS));
    }

    private void requireReservation(String orderId, String requesterId, long amount) {
        Reservation reservation = reservations.get(orderId);
        if (reservation == null || reservation.getState() != ReservationState.RESERVED) {
            throw new IllegalStateException("No active credit reservation exists for order.");
        }
        if (!reservation.getRequesterId().equals(requesterId)
                || reservation.getAmount() != amount) {
            throw new IllegalStateException(
                    "Credit reservation does not match the order outcome.");
        }
    }

    private static void requireAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive.");
        }
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
    }

    @Value
    public static class CreditSnapshot {
        long total;
        long reserved;
        long available;
    }

    @Value
    private static class Outcome {
        String orderId;
        String kind;
    }

    @Value
    private static class Reservation {
        String requesterId;
        long amount;
        ReservationState state;
    }

    private enum ReservationState {
        RESERVED,
        SETTLED,
        RELEASED
    }

    private static final class Account {
        private long total;
        private long reserved;

        private Account(long total) {
            this.total = total;
        }

        private long available() {
            return total - reserved;
        }
    }
}
