/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Implementation.
 * Scope: Generated Reporting Service code for the team-specified reporting, association, storage, security, and administration contracts.
 * Author review: I reviewed for correctness
 */
package sg.edu.nus.foc.reporting.order;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;
import sg.edu.nus.foc.reporting.config.ReportingProperties;
import sg.edu.nus.foc.reporting.error.DependencyUnavailableException;
import sg.edu.nus.foc.reporting.error.NotFoundException;

@Component
public class HttpOrderAssociationClient implements OrderAssociationClient {
    private final RestClient client;
    @Autowired
    public HttpOrderAssociationClient(ReportingProperties properties) {
        Duration timeout = properties.orderService().timeout();
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout); factory.setReadTimeout(timeout);
        client = RestClient.builder().baseUrl(properties.orderService().baseUrl()).requestFactory(factory).build();
    }
    HttpOrderAssociationClient(RestClient client) { this.client = client; }
    @Override public AssociatedOrder find(String orderId, String bearerToken) {
        try {
            String id = UriUtils.encodePathSegment(orderId, StandardCharsets.UTF_8);
            AssociatedOrder result = client.get().uri(URI.create("/api/orders/mine/associated/" + id))
                    .header(HttpHeaders.AUTHORIZATION, bearerToken).retrieve().body(AssociatedOrder.class);
            if (result == null || result.id() == null) throw new DependencyUnavailableException("Order Service returned an invalid association.");
            return result;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) throw new NotFoundException("The selected order is unavailable for reporting.");
            throw new DependencyUnavailableException("Order Service is unavailable.", e);
        } catch (RestClientException e) { throw new DependencyUnavailableException("Order Service is unavailable.", e); }
    }
}
