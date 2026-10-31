package sg.edu.nus.foc.order.adapter;

import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import sg.edu.nus.foc.order.application.BackgroundRepostChecks;
import sg.edu.nus.foc.order.domain.OrderProblem;
import tools.jackson.databind.JsonNode;

/** Only configured private peer URLs; credentials are neither created nor persisted. */
@Component
@ConditionalOnProperty(name = "order.peers.mode", havingValue = "http")
public class HttpBackgroundRepostChecks implements BackgroundRepostChecks {
    private final RestClient users;
    private final RestClient suppliers;
    private final RestClient credits;

    public HttpBackgroundRepostChecks(@Value("${order.peers.user-url}") String userUrl,
            @Value("${order.peers.supplier-url}") String supplierUrl,
            @Value("${order.peers.credit-url}") String creditUrl,
            @Value("${order.peers.connect-timeout:3s}") Duration connect,
            @Value("${order.peers.read-timeout:5s}") Duration read) {
        users = HttpCreditCommandGateway.client(userUrl, connect, read);
        suppliers = HttpCreditCommandGateway.client(supplierUrl, connect, read);
        credits = HttpCreditCommandGateway.client(creditUrl, connect, read);
    }

    @Override
    public boolean fundingReady(String originalOrderId, String requesterId) {
        try {
            Boolean ready = credits.get().uri(builder -> builder
                    .path("/api/credits/internal/automatic-reposts/{id}/funding-ready")
                    .queryParam("requesterId", requesterId).build(originalOrderId))
                    .retrieve().body(Boolean.class);
            return Boolean.TRUE.equals(ready);
        } catch (RestClientException failure) {
            throw new OrderProblem("SERVICE_UNAVAILABLE", "The original reservation refund is not confirmed.");
        }
    }

    @Override
    public void validate(String requesterId, String pickup, String delivery) {
        try {
            JsonNode requester = users.get().uri("/api/users/internal/order-requesters/{id}", requesterId)
                    .retrieve().body(JsonNode.class);
            if (requester == null || !requesterId.equals(requester.path("userId").asString())
                    || !requester.path("eligible").isBoolean()) {
                throw new OrderProblem("SERVICE_UNAVAILABLE", "User Service did not confirm automatic repost eligibility.");
            }
            if (!requester.path("eligible").asBoolean()) {
                throw OrderProblem.forbidden("The saved requester is no longer eligible to repost.");
            }
            JsonNode pair = suppliers.post().uri("/api/suppliers/internal/order-locations/validate")
                    .body(Map.of("pickupSupplierId", pickup, "deliverySupplierId", delivery))
                    .retrieve().body(JsonNode.class);
            if (pair == null || !pair.path("valid").isBoolean()) {
                throw new OrderProblem("SERVICE_UNAVAILABLE", "Supplier Service did not confirm automatic repost locations.");
            }
            if (!pair.path("valid").asBoolean()) {
                throw new OrderProblem("VALIDATION_ERROR", "Automatic repost locations must remain active and distinct.");
            }
        } catch (RestClientException failure) {
            throw new OrderProblem("SERVICE_UNAVAILABLE", "Automatic repost dependency is unavailable.");
        }
    }
}
