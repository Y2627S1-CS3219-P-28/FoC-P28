package sg.edu.nus.foc.reporting.order;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import sg.edu.nus.foc.reporting.error.ConflictException;
import sg.edu.nus.foc.reporting.error.DependencyUnavailableException;
import sg.edu.nus.foc.reporting.error.NotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HttpOrderAssociationClientTest {
    @Test void placesTheAtomicActiveOrderHoldAndForwardsAuthorization() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://orders");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("http://orders/api/orders/mine/active/order%20one/report-holds/report%2Fone"))
                .andExpect(method(org.springframework.http.HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andExpect(content().json("{\"requireAssignedCourier\":true}"))
                .andRespond(withSuccess("{\"id\":\"order one\",\"requesterId\":\"owner\",\"courierId\":\"courier\",\"status\":\"DELIVERED\",\"reportId\":\"report/one\",\"isCompletionHold\":true}", MediaType.APPLICATION_JSON));
        AssociatedOrder order = new HttpOrderAssociationClient(builder.build())
                .placeReportHold("order one", "report/one", true, "Bearer abc");
        assertEquals("owner", order.requesterId());
        assertEquals("courier", order.courierId());
        server.verify();
    }

    @Test void releasesTheAdminReportHold() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://orders");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://orders/api/orders/admin/order/completion-holds/report/release"))
                .andExpect(method(org.springframework.http.HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin"))
                .andRespond(withNoContent());
        new HttpOrderAssociationClient(builder.build()).releaseReportHold("order", "report", "Bearer admin");
        server.verify();
    }

    @Test void mapsMissingConflictAndDependencyFailures() {
        RestClient.Builder missingBuilder = RestClient.builder().baseUrl("http://orders");
        MockRestServiceServer missing = MockRestServiceServer.bindTo(missingBuilder).build();
        missing.expect(anything()).andRespond(withResourceNotFound());
        assertThrows(NotFoundException.class, () -> new HttpOrderAssociationClient(missingBuilder.build())
                .placeReportHold("id", "report", false, "Bearer x"));

        RestClient.Builder conflictBuilder = RestClient.builder().baseUrl("http://orders");
        MockRestServiceServer conflict = MockRestServiceServer.bindTo(conflictBuilder).build();
        conflict.expect(anything()).andRespond(withStatus(org.springframework.http.HttpStatus.CONFLICT));
        assertThrows(ConflictException.class, () -> new HttpOrderAssociationClient(conflictBuilder.build())
                .placeReportHold("id", "report", true, "Bearer x"));

        RestClient.Builder downBuilder = RestClient.builder().baseUrl("http://orders");
        MockRestServiceServer down = MockRestServiceServer.bindTo(downBuilder).build();
        down.expect(anything()).andRespond(withServerError());
        assertThrows(DependencyUnavailableException.class, () -> new HttpOrderAssociationClient(downBuilder.build())
                .placeReportHold("id", "report", false, "Bearer x"));
    }
}
