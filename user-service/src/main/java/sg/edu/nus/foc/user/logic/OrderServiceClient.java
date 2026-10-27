package sg.edu.nus.foc.user.logic;

import java.util.UUID;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import sg.edu.nus.foc.user.dto.ActiveInvolvementResponse;

@Component
public class OrderServiceClient {
    private final RestClient restClient;
    private record RegistrationFactRequest(
            UUID eventId,
            String userId,
            Instant occurredAt
    ) {}

    public OrderServiceClient(
            RestClient.Builder builder,
            @Value("${order-service.url}") String orderServiceUrl) {
        
        
        this.restClient = builder
                .baseUrl(orderServiceUrl)
                .build();
    }

    public boolean hasActiveInvolvement(String userId, String token) {

        ActiveInvolvementResponse response = restClient.get()
                .uri("/api/orders/users/{userId}/active-involvement", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(ActiveInvolvementResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Order service returned an empty response");
        }

        return response.hasActiveOrders();
    }
}
