package sg.edu.nus.foc.order.adapter;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import sg.edu.nus.foc.order.adapter.dto.CreditCourierAssignmentRequest;
import sg.edu.nus.foc.order.application.CreditServicePort;
import sg.edu.nus.foc.order.application.SupplierServicePort;
import sg.edu.nus.foc.order.application.UserServicePort;
import sg.edu.nus.foc.order.domain.OrderProblem;
import sg.edu.nus.foc.order.security.dto.UserRoleContextResponse;
import sg.edu.nus.foc.order.security.Role;
import sg.edu.nus.foc.order.security.VerifiedOrderCaller;

@Component
@ConditionalOnProperty(name = "order.peers.mode", havingValue = "http")
public class HttpPeerAdapters implements UserServicePort, SupplierServicePort, CreditServicePort {

    private final RestClient user;
    private final RestClient supplier;
    private final RestClient credit;

    @Autowired
    public HttpPeerAdapters(
            @Value("${order.peers.user-url}") String userUrl,
            @Value("${order.peers.supplier-url}") String supplierUrl,
            @Value("${order.peers.credit-url}") String creditUrl) {
        this(
                RestClient.builder().baseUrl(userUrl).build(),
                RestClient.builder().baseUrl(supplierUrl).build(),
                RestClient.builder().baseUrl(creditUrl).build());
    }

    HttpPeerAdapters(RestClient user, RestClient supplier, RestClient credit) {
        this.user = user;
        this.supplier = supplier;
        this.credit = credit;
    }

    @Override
    public String verifyRequester(String id, String authorization) {
        Optional<String> verifiedIdentity = VerifiedOrderCaller.identityFor(Role.REQUESTER, id);
        if (verifiedIdentity.isPresent()) {
            return verifiedIdentity.get();
        }

        UserRoleContextResponse context = call(
                user,
                "/api/users/role-context",
                authorization,
                UserRoleContextResponse.class);
        requireMatchingRole(context, id, "requester");
        return context.getUserId();
    }

    @Override
    public String verifyCourier(String id, String authorization) {
        Optional<String> verifiedIdentity = VerifiedOrderCaller.identityFor(Role.COURIER, id);
        CourierEligibility eligibility = call(
                user,
                "/api/users/courier-eligibility",
                authorization,
                CourierEligibility.class);
        if (eligibility == null || !eligibility.isCourierEligible()) {
            throw OrderProblem.forbidden("Courier is not eligible to perform this action.");
        }

        if (verifiedIdentity.isPresent()) {
            return verifiedIdentity.get();
        }

        UserRoleContextResponse context = call(
                user,
                "/api/users/role-context",
                authorization,
                UserRoleContextResponse.class);
        requireMatchingRole(context, id, "courier");
        return context.getUserId();
    }

    @Override
    public void validatePair(String pickup, String delivery, String authorization) {
        supplier.post()
                .uri("/api/suppliers/validate")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader(authorization))
                .body(new SupplierPairRequest(pickup, delivery))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void reserve(
            String orderId,
            String requester,
            long amount,
            String authorization) {
        credit.put()
                .uri("/api/credits/orders/{id}/reservation", orderId)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader(authorization))
                .body(new CreditReservationRequest(requester, amount))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void assignCourier(
            String orderId,
            String courierId,
            String authorization) {
        CreditCourierAssignmentRequest request = new CreditCourierAssignmentRequest(courierId);
        ResponseEntity<Void> response = credit.put()
                .uri("/api/credits/orders/{id}/courier-assignment", orderId)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader(authorization))
                .body(request)
                .retrieve()
                .toBodilessEntity();
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            throw new IllegalStateException("Credit Service did not confirm the requested courier assignment.");
        }
    }

    @Override
    public void holdForReopen(
            String orderId,
            String authorization) {
        ResponseEntity<Void> response = credit.post()
                .uri("/api/credits/orders/{id}/hold-for-reopen", orderId)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader(authorization))
                .retrieve()
                .toBodilessEntity();
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            throw new IllegalStateException("Credit Service did not synchronously confirm the reopen hold.");
        }
    }

    @Override
    public void settle(
            String commandId,
            String orderId,
            String requesterId,
            String courierId,
            long amount,
            String authorization) {
        CreditSettlementRequest request = new CreditSettlementRequest(
                commandId,
                requesterId,
                courierId,
                amount);
        credit.post()
                .uri("/api/credits/orders/{id}/settlement", orderId)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader(authorization))
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private static <T> T call(
            RestClient client,
            String path,
            String authorization,
            Class<T> responseType) {
        try {
            return client.get()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader(authorization))
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException exception) {
            throw new OrderProblem("SERVICE_UNAVAILABLE", "User Service verification is unavailable.");
        }
    }

    private static String authorizationHeader(String authorization) {
        return authorization == null ? "" : authorization;
    }

    private static void requireMatchingRole(
            UserRoleContextResponse context,
            String requestedId,
            String role) {
        if (context == null
                || context.getUserId() == null
                || context.getUserId().isBlank()
                || !context.getUserId().equals(requestedId)
                || context.getRoles() == null
                || context.getRoles().stream().noneMatch(value -> role.equalsIgnoreCase(value))) {
            throw OrderProblem.forbidden(
                    "Authenticated user does not match the requested " + role + " identity.");
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SupplierPairRequest {
        private String pickupSupplierId;
        private String deliverySupplierId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class CreditReservationRequest {
        private String requesterId;
        private long amount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class CreditSettlementRequest {
        private String commandId;
        private String requesterId;
        private String courierId;
        private long amount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class CourierEligibility {
        @JsonProperty("isCourierEligible")
        private boolean courierEligible;
    }
}
