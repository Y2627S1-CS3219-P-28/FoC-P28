package sg.edu.nus.foc.order.adapter;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import sg.edu.nus.foc.order.application.CreditServicePort;
import sg.edu.nus.foc.order.application.SupplierServicePort;
import sg.edu.nus.foc.order.application.UserServicePort;

@Component
@Profile("!http")
@ConditionalOnProperty(name="order.peers.mode", havingValue="mock", matchIfMissing=true)
public class MockPeerAdapters implements UserServicePort, SupplierServicePort, CreditServicePort {
    public void verifyRequester(String userId, String authorization) { require(userId, "requester"); }
    public void verifyCourier(String userId, String authorization) { require(userId, "courier"); }
    public void validatePair(String pickupSupplierId, String deliverySupplierId, String authorization) {
        require(pickupSupplierId, "pickup supplier"); require(deliverySupplierId, "delivery supplier");
        if (pickupSupplierId.equals(deliverySupplierId)) throw new IllegalArgumentException("Supplier locations must differ.");
    }
    public void reserve(String orderId, String requesterId, long amount, String authorization) {
        require(orderId, "order"); require(requesterId, "requester"); if (amount <= 0) throw new IllegalArgumentException("Credit amount must be positive.");
    }
    private static void require(String value, String name) { if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required."); }
}
