/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Test generation and verification.
 * Scope: Generated automated verification for the team-specified Reporting Service contracts.
 * Author review: I reviewed for correctness
 */
package sg.edu.nus.foc.reporting.order;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import sg.edu.nus.foc.reporting.error.DependencyUnavailableException;
import sg.edu.nus.foc.reporting.error.NotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HttpOrderAssociationClientTest {
    @Test void callsOnlyAuthoritativeSingleOrderEndpointAndForwardsBearerToken() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://orders");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("http://orders/api/orders/mine/associated/order%20one"))
                .andExpect(method(HttpMethod.GET)).andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andRespond(withSuccess("{\"id\":\"authoritative\",\"courierId\":null}", MediaType.APPLICATION_JSON));
        AssociatedOrder order = new HttpOrderAssociationClient(builder.build()).find("order one", "Bearer abc");
        assertEquals("authoritative", order.id()); assertNull(order.courierId()); server.verify();
    }

    @Test void mapsUnrelatedToNotFoundAndOutagesToServiceUnavailable() {
        RestClient.Builder missingBuilder = RestClient.builder().baseUrl("http://orders");
        MockRestServiceServer missing = MockRestServiceServer.bindTo(missingBuilder).build();
        missing.expect(requestTo("http://orders/api/orders/mine/associated/nope")).andRespond(withResourceNotFound());
        assertThrows(NotFoundException.class, () -> new HttpOrderAssociationClient(missingBuilder.build()).find("nope", "Bearer x"));

        RestClient.Builder downBuilder = RestClient.builder().baseUrl("http://orders");
        MockRestServiceServer down = MockRestServiceServer.bindTo(downBuilder).build();
        down.expect(requestTo("http://orders/api/orders/mine/associated/id")).andRespond(withServerError());
        assertThrows(DependencyUnavailableException.class, () -> new HttpOrderAssociationClient(downBuilder.build()).find("id", "Bearer x"));
    }
}
