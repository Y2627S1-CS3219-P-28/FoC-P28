package sg.edu.nus.foc.order.adapter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MockPeerAdaptersTest {
    private final MockPeerAdapters peers = new MockPeerAdapters();

    @Test
    void validatesApprovedCreditOutcomeCommands() {
        peers.reserve("order-1", "requester-1", 2, "Bearer token");
        assertEquals(new MockPeerAdapters.CreditSnapshot(50, 2, 48), peers.creditSnapshot("requester-1"));
        assertDoesNotThrow(() -> peers.settle("settle-1", "order-1", "requester-1", "courier-1", 2, 4, "Bearer token"));
        assertEquals(new MockPeerAdapters.CreditSnapshot(48, 0, 48), peers.creditSnapshot("requester-1"));
        assertEquals(new MockPeerAdapters.CreditSnapshot(52, 0, 52), peers.creditSnapshot("courier-1"));
        assertEquals("SETTLED", peers.reservationState("order-1"));
        peers.reserve("order-1-expiry", "requester-1", 2, "Bearer token");
        assertDoesNotThrow(() -> peers.release("release-1", "order-1-expiry", "requester-1", 2, "EXPIRED", 5, "Bearer token"));
    }

    @Test
    void rejectsInvalidCreditOutcomeCommands() {
        assertThrows(IllegalArgumentException.class, () -> peers.settle("", "order-1", "requester-1", "courier-1", 2, 4, null));
        assertThrows(IllegalArgumentException.class, () -> peers.release("release-1", "order-1", "requester-1", 0, "CANCELLED", 5, null));
    }

    @Test
    void releasesReservedCreditsForCancellationAndIsIdempotent() {
        peers.reserve("order-2", "requester-2", 3, "Bearer token");
        peers.release("release-2", "order-2", "requester-2", 3, "CANCELLED", 1, "Bearer token");
        assertEquals(new MockPeerAdapters.CreditSnapshot(50, 0, 50), peers.creditSnapshot("requester-2"));
        assertEquals("RELEASED", peers.reservationState("order-2"));
        assertDoesNotThrow(() -> peers.release("release-2", "order-2", "requester-2", 3, "CANCELLED", 1, "Bearer token"));
    }

    @Test
    void holdsCreditForReopenWithoutRefundAndAllowsLaterSettlementOrRefund() {
        peers.reserve("reopen-order", "requester-reopen", 7, null);

        peers.holdForReopen("hold-1", "reopen-order", "requester-reopen", "courier", 7, 0, null);

        assertEquals(new MockPeerAdapters.CreditSnapshot(50, 7, 43), peers.creditSnapshot("requester-reopen"));
        assertEquals("HELD_FOR_REOPEN", peers.reservationState("reopen-order"));
        assertDoesNotThrow(() -> peers.holdForReopen(
                "hold-1", "reopen-order", "requester-reopen", "courier", 7, 0, null));
        assertThrows(IllegalStateException.class, () -> peers.holdForReopen(
                "hold-1", "reopen-order", "requester-reopen", "courier", 8, 0, null));
        assertDoesNotThrow(() -> peers.settle(
                "settle-after-reopen", "reopen-order", "requester-reopen", "courier", 7, 0, null));
        assertEquals(new MockPeerAdapters.CreditSnapshot(43, 0, 43), peers.creditSnapshot("requester-reopen"));

        peers.reserve("reopen-cancel-order", "requester-cancel", 4, null);
        peers.holdForReopen("hold-cancel", "reopen-cancel-order", "requester-cancel", "courier", 4, 0, null);
        peers.release("release-after-reopen", "reopen-cancel-order", "requester-cancel", 4, "EXPIRED", 0, null);
        assertEquals(new MockPeerAdapters.CreditSnapshot(50, 0, 50), peers.creditSnapshot("requester-cancel"));
    }

    @Test
    void validatesIdentitySupplierAndReservationInputs() {
        assertThrows(IllegalArgumentException.class, () -> peers.verifyRequester("", null));
        assertThrows(IllegalArgumentException.class, () -> peers.verifyCourier(null, null));
        assertThrows(IllegalArgumentException.class, () -> peers.validatePair("same", "same", null));
        assertThrows(IllegalArgumentException.class, () -> peers.validatePair("", "delivery", null));
        assertThrows(IllegalArgumentException.class, () -> peers.reserve("order", "requester", 0, null));
        assertThrows(IllegalArgumentException.class, () -> peers.reserve(null, "requester", 1, null));
    }

    @Test
    void rejectsConflictingAndInsufficientReservations() {
        peers.reserve("order", "requester", 4, null);
        assertDoesNotThrow(() -> peers.reserve("order", "requester", 4, null));
        assertThrows(IllegalStateException.class, () -> peers.reserve("order", "other", 4, null));
        assertThrows(IllegalStateException.class, () -> peers.reserve("order", "requester", 5, null));
        peers.reserve("large", "requester", 46, null);
        assertThrows(IllegalStateException.class, () -> peers.reserve("too-large", "requester", 1, null));
    }

    @Test
    void rejectsInvalidSettlementAndReleaseMatches() {
        assertThrows(IllegalStateException.class,
            () -> peers.settle("settle", "missing", "requester", "courier", 1, 0, null));
        peers.reserve("order", "requester", 2, null);
        assertThrows(IllegalStateException.class,
            () -> peers.settle("settle", "order", "other", "courier", 2, 0, null));
        assertThrows(IllegalStateException.class,
            () -> peers.settle("settle", "order", "requester", "courier", 3, 0, null));
        peers.settle("settle", "order", "requester", "courier", 2, 0, null);
        assertDoesNotThrow(() -> peers.settle("settle", "order", "requester", "courier", 2, 0, null));
        assertThrows(IllegalStateException.class,
            () -> peers.settle("settle", "different", "requester", "courier", 2, 0, null));

        peers.reserve("release-order", "requester", 2, null);
        assertThrows(IllegalArgumentException.class,
            () -> peers.release("release", "release-order", "requester", 2, "PAID", 0, null));
        assertThrows(IllegalStateException.class,
            () -> peers.release("release", "release-order", "other", 2, "EXPIRED", 0, null));
        peers.release("release", "release-order", "requester", 2, "EXPIRED", 0, null);
        assertDoesNotThrow(() -> peers.release("release", "release-order", "requester", 2, "EXPIRED", 0, null));
        assertThrows(IllegalStateException.class,
            () -> peers.release("release", "different", "requester", 2, "EXPIRED", 0, null));
    }
}
