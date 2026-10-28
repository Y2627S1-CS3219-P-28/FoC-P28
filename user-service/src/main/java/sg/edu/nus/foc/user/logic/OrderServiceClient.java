package sg.edu.nus.foc.user.logic;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OrderServiceClient {
    private final RestClient restClient;

    public OrderServiceClient(
            RestClient.Builder builder,
            @Value("${order-service.url}") String orderServiceUrl) {
        SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(Duration.ofSeconds(3));
        requests.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = builder
                .baseUrl(orderServiceUrl)
                .requestFactory(requests)
                .build();
    }

    public boolean canDeleteUser(String userId) {
        try {
            Boolean allowed = restClient.get()
                    .uri("/api/orders/internal/users/{userId}/can-delete", userId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Boolean.class);
            if (allowed == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Order deletion eligibility could not be confirmed.");
            }
            return allowed;
        } catch (RestClientException unavailable) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Order deletion eligibility could not be confirmed.", unavailable);
        }
    }
}
