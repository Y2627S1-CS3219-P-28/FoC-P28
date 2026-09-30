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
}
