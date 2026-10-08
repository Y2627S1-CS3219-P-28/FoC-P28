package sg.edu.nus.foc.order.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.web.client.RestClient;

class HttpPeerAdaptersTest {
    @Test
    void forwardsAuthenticatedUserAndCourierChecks() {
        RestClient.Builder userBuilder = RestClient.builder();
        MockRestServiceServer userServer = MockRestServiceServer.bindTo(userBuilder).build();
        HttpPeerAdapters adapters = new HttpPeerAdapters(userBuilder.build(), RestClient.builder().build(), RestClient.builder().build());

        userServer.expect(requestTo("/api/users/role-context"))
            .andExpect(header("Authorization", "Bearer token"))
            .andRespond(withSuccess("{\"userId\":\"requester\",\"roles\":[\"requester\"]}", MediaType.APPLICATION_JSON));
        userServer.expect(requestTo("/api/users/courier-eligibility"))
            .andRespond(withSuccess("{\"isCourierEligible\":true}", MediaType.APPLICATION_JSON));
        userServer.expect(requestTo("/api/users/role-context"))
            .andRespond(withSuccess("{\"userId\":\"courier\",\"roles\":[\"courier\"]}", MediaType.APPLICATION_JSON));
        userServer.expect(requestTo("/api/users/role-context"))
            .andRespond(withSuccess("{\"userId\":\"other\",\"roles\":[\"requester\"]}", MediaType.APPLICATION_JSON));

        assertEquals("requester", adapters.verifyRequester("requester", "Bearer token"));
        assertEquals("courier", adapters.verifyCourier("courier", "Bearer token"));
        assertThrows(sg.edu.nus.foc.order.domain.OrderProblem.class, () -> adapters.verifyRequester("requester", "Bearer token"));
        userServer.verify();
    }

    @Test
    void rejectsIneligibleCourier() {
        RestClient.Builder userBuilder = RestClient.builder();
        MockRestServiceServer userServer = MockRestServiceServer.bindTo(userBuilder).build();
        HttpPeerAdapters adapters = new HttpPeerAdapters(userBuilder.build(), RestClient.builder().build(), RestClient.builder().build());
        userServer.expect(requestTo("/api/users/courier-eligibility"))
            .andRespond(withSuccess("{\"isCourierEligible\":false}", MediaType.APPLICATION_JSON));
        assertThrows(sg.edu.nus.foc.order.domain.OrderProblem.class, () -> adapters.verifyCourier("courier", "Bearer token"));
        userServer.verify();
    }

    @Test
    void rejectsMissingOrMismatchedPeerIdentityContext() {
        RestClient.Builder userBuilder = RestClient.builder();
        MockRestServiceServer userServer = MockRestServiceServer.bindTo(userBuilder).build();
        HttpPeerAdapters adapters = new HttpPeerAdapters(userBuilder.build(), RestClient.builder().build(), RestClient.builder().build());
        userServer.expect(requestTo("/api/users/role-context"))
            .andRespond(withSuccess("{\"userId\":\"requester\",\"roles\":null}", MediaType.APPLICATION_JSON));
        userServer.expect(requestTo("/api/users/role-context"))
            .andRespond(withSuccess("{\"userId\":\"\",\"roles\":[\"requester\"]}", MediaType.APPLICATION_JSON));
        userServer.expect(requestTo("/api/users/role-context"))
            .andRespond(withSuccess("{\"userId\":\"requester\",\"roles\":[\"courier\"]}", MediaType.APPLICATION_JSON));
        userServer.expect(requestTo("/api/users/courier-eligibility"))
            .andRespond(withSuccess("{\"isCourierEligible\":true}", MediaType.APPLICATION_JSON));
        userServer.expect(requestTo("/api/users/role-context"))
            .andRespond(withSuccess("{\"userId\":\"courier\",\"roles\":[\"courier\"]}", MediaType.APPLICATION_JSON));

        assertThrows(sg.edu.nus.foc.order.domain.OrderProblem.class, () -> adapters.verifyRequester("requester", null));
        assertThrows(sg.edu.nus.foc.order.domain.OrderProblem.class, () -> adapters.verifyRequester("requester", null));
        assertThrows(sg.edu.nus.foc.order.domain.OrderProblem.class, () -> adapters.verifyRequester("requester", null));
        assertEquals("courier", adapters.verifyCourier("courier", null));
        userServer.verify();
    }

    @Test
    void productionCallerReusesConfirmedRolesButStillChecksCourierEligibility() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpPeerAdapters adapters = new HttpPeerAdapters(
                builder.build(), RestClient.builder().build(), RestClient.builder().build());
        org.springframework.security.oauth2.jwt.Jwt caller = org.springframework.security.oauth2.jwt.Jwt
                .withTokenValue("token").header("alg", "RS256").subject("u1").build();
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken authentication =
                new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                        caller, java.util.List.of(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_REQUESTER"),
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_COURIER")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            server.expect(requestTo("/api/users/courier-eligibility"))
                    .andRespond(withSuccess("{\"isCourierEligible\":true}", MediaType.APPLICATION_JSON));

            assertEquals("u1", adapters.verifyRequester("u1", "Bearer token"));
            assertEquals("u1", adapters.verifyCourier("u1", "Bearer token"));
            assertThrows(sg.edu.nus.foc.order.domain.OrderProblem.class,
                    () -> adapters.verifyRequester("other", "Bearer token"));
            server.verify();
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    void courierEligibilityDependencyFailureIsReportedAsUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpPeerAdapters adapters = new HttpPeerAdapters(
                builder.build(), RestClient.builder().build(), RestClient.builder().build());
        server.expect(requestTo("/api/users/courier-eligibility")).andRespond(withServerError());

        sg.edu.nus.foc.order.domain.OrderProblem problem = assertThrows(
                sg.edu.nus.foc.order.domain.OrderProblem.class,
                () -> adapters.verifyCourier("courier", "Bearer token"));

        assertEquals("SERVICE_UNAVAILABLE", problem.getCode());
        server.verify();
    }

    @Test
    void forwardsSupplierAndCreditCommands() {
        RestClient.Builder supplierBuilder = RestClient.builder();
        RestClient.Builder creditBuilder = RestClient.builder();
        MockRestServiceServer supplierServer = MockRestServiceServer.bindTo(supplierBuilder).build();
        MockRestServiceServer creditServer = MockRestServiceServer.bindTo(creditBuilder).build();
        HttpPeerAdapters adapters = new HttpPeerAdapters(RestClient.builder().build(), supplierBuilder.build(), creditBuilder.build());

        supplierServer.expect(requestTo("/api/suppliers/validate"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andRespond(withSuccess());
        creditServer.expect(requestTo("/api/credits/orders/order/reservation"))
            .andExpect(method(org.springframework.http.HttpMethod.PUT))
            .andRespond(withSuccess());
        creditServer.expect(requestTo("/api/credits/orders/order/hold-for-reopen"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().string(""))
                .andRespond(withSuccess());
        creditServer.expect(requestTo("/api/credits/orders/order/settlement"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(MockRestRequestMatchers.jsonPath("$.expectedOrderVersion").doesNotExist())
            .andRespond(withSuccess());

        adapters.validatePair("p", "d", null);
        adapters.reserve("order", "requester", 2, null);
        adapters.holdForReopen("order", null);
        adapters.settle("settle", "order", "requester", "courier", 2, null);
        supplierServer.verify();
        creditServer.verify();
    }

    @Test
    void assignsCourierWithOrderAndCourierIdsAndWaitsForOk() {
        RestClient.Builder creditBuilder = RestClient.builder();
        MockRestServiceServer creditServer = MockRestServiceServer.bindTo(creditBuilder).build();
        HttpPeerAdapters adapters = new HttpPeerAdapters(
                RestClient.builder().build(),
                RestClient.builder().build(),
                creditBuilder.build());
        creditServer.expect(requestTo("/api/credits/orders/order-1/courier-assignment"))
                .andExpect(method(org.springframework.http.HttpMethod.PUT))
                .andExpect(header("Authorization", "Bearer courier-token"))
                .andExpect(content().json("{\"courierId\":\"courier-1\"}"))
                .andRespond(withSuccess());

        adapters.assignCourier(
                "order-1", "courier-1", "Bearer courier-token");

        creditServer.verify();
    }

    @Test
    void rejectsCreditAssignmentWhenCreditDoesNotReturnOk() {
        RestClient.Builder creditBuilder = RestClient.builder();
        MockRestServiceServer creditServer = MockRestServiceServer.bindTo(creditBuilder).build();
        HttpPeerAdapters adapters = new HttpPeerAdapters(
                RestClient.builder().build(),
                RestClient.builder().build(),
                creditBuilder.build());
        creditServer.expect(requestTo("/api/credits/orders/order-1/courier-assignment"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.ACCEPTED));

        assertThrows(IllegalStateException.class, () -> adapters.assignCourier(
                "order-1", "courier-1", "Bearer courier-token"));

        creditServer.verify();
    }

    @Test
    void rejectsAcceptedButUnconfirmedCreditReopenHoldResponse() {
        RestClient.Builder creditBuilder = RestClient.builder();
        MockRestServiceServer creditServer = MockRestServiceServer.bindTo(creditBuilder).build();
        HttpPeerAdapters adapters = new HttpPeerAdapters(
                RestClient.builder().build(),
                RestClient.builder().build(),
                creditBuilder.build());
        creditServer.expect(requestTo("/api/credits/orders/order/hold-for-reopen"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withStatus(org.springframework.http.HttpStatus.ACCEPTED));

        assertThrows(IllegalStateException.class, () -> adapters.holdForReopen(
                "order", "Bearer token"));

        creditServer.verify();
    }
}
