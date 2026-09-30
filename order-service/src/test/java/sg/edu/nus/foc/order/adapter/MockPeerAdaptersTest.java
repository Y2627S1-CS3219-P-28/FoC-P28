package sg.edu.nus.foc.order.adapter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MockPeerAdaptersTest {
    private final MockPeerAdapters peers = new MockPeerAdapters();

    @Test
    void validatesApprovedCreditOutcomeCommands() {
        assertDoesNotThrow(() -> peers.settle("settle-1", "order-1", "requester-1", "courier-1", 2, 4, "Bearer token"));
        assertDoesNotThrow(() -> peers.release("release-1", "order-1", "requester-1", 2, "EXPIRED", 5, "Bearer token"));
    }

    @Test
    void rejectsInvalidCreditOutcomeCommands() {
        assertThrows(IllegalArgumentException.class, () -> peers.settle("", "order-1", "requester-1", "courier-1", 2, 4, null));
        assertThrows(IllegalArgumentException.class, () -> peers.release("release-1", "order-1", "requester-1", 0, "CANCELLED", 5, null));
    }
}
