package sg.edu.nus.foc.order.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
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
        assertThrows(IllegalStateException.class, () -> adapters.verifyRequester("requester", "Bearer token"));
        userServer.verify();
    }

    @Test
    void rejectsIneligibleCourier() {
        RestClient.Builder userBuilder = RestClient.builder();
        MockRestServiceServer userServer = MockRestServiceServer.bindTo(userBuilder).build();
        HttpPeerAdapters adapters = new HttpPeerAdapters(userBuilder.build(), RestClient.builder().build(), RestClient.builder().build());
        userServer.expect(requestTo("/api/users/courier-eligibility"))
            .andRespond(withSuccess("{\"isCourierEligible\":false}", MediaType.APPLICATION_JSON));
        assertThrows(IllegalStateException.class, () -> adapters.verifyCourier("courier", "Bearer token"));
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

        assertThrows(IllegalStateException.class, () -> adapters.verifyRequester("requester", null));
        assertThrows(IllegalStateException.class, () -> adapters.verifyRequester("requester", null));
        assertThrows(IllegalStateException.class, () -> adapters.verifyRequester("requester", null));
        assertEquals("courier", adapters.verifyCourier("courier", null));
        userServer.verify();
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
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().json("{\"commandId\":\"hold\",\"requesterId\":\"requester\",\"courierId\":\"courier\",\"amount\":2,\"expectedOrderVersion\":0}"))
                .andRespond(withSuccess());
        creditServer.expect(requestTo("/api/credits/orders/order/settlement"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andRespond(withSuccess());
        creditServer.expect(requestTo("/api/credits/orders/order/release"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andRespond(withSuccess());

        adapters.validatePair("p", "d", null);
        adapters.reserve("order", "requester", 2, null);
        adapters.holdForReopen("hold", "order", "requester", "courier", 2, 0, null);
        adapters.settle("settle", "order", "requester", "courier", 2, 0, null);
        adapters.release("release", "order", "requester", 2, "EXPIRED", 0, null);

        supplierServer.verify();
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
                "hold", "order", "requester", "courier", 2, 0, "Bearer token"));

        creditServer.verify();
    }
}
