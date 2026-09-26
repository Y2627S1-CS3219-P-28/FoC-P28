/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-25
 * Mode: Test generation and testing assistance.
 * Scope: Generated initial tests for the team-finalized Firebase and Spring Security behavior.
 * Author review: I reviewed for correctness.
 */
package sg.edu.nus.foc.credit.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.cors.CorsConfiguration;
import sg.edu.nus.foc.credit.config.CreditProperties;
import tools.jackson.databind.json.JsonMapper;

class SecurityComponentsTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void selectsConfiguredJwtDecoder() {
        CreditProperties emulator = new CreditProperties(null,
                new CreditProperties.AuthSettings("p", "localhost:9099"), List.of());
        CreditProperties production = new CreditProperties(null,
                new CreditProperties.AuthSettings("p", ""), List.of());
        assertThat(config.jwtDecoder(emulator)).isInstanceOf(EmulatorJwtDecoder.class);
        assertThat(config.jwtDecoder(production)).isInstanceOf(NimbusJwtDecoder.class);
    }

    @Test
    void configuresCorsForCreditMethodsAndOrigins() {
        CreditProperties properties = new CreditProperties(null, null, List.of("https://example.com"));
        CorsConfiguration cors = config.corsConfigurationSource(properties)
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/credits"));
        assertThat(cors.getAllowedOrigins()).containsExactly("https://example.com");
        assertThat(cors.getAllowedMethods()).containsExactly("GET", "POST", "PUT", "OPTIONS");
        assertThat(cors.getAllowedHeaders()).contains("Authorization", "Content-Type", "X-Request-Id");
    }

    @Test
    void securityHandlersWriteJsonForDeniedRequests() throws Exception {
        JsonSecurityHandlers handlers = new JsonSecurityHandlers(JsonMapper.builder().build(),
                Clock.fixed(Instant.parse("2026-09-25T00:00:00Z"), ZoneOffset.UTC));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/credits/orders/o/reservation");
        MockHttpServletResponse denied = new MockHttpServletResponse();
        handlers.handle(request, denied, new AccessDeniedException("no"));
        assertThat(denied.getStatus()).isEqualTo(403);
        assertThat(denied.getContentAsString()).contains("\"error\":\"FORBIDDEN\"");

        MockHttpServletResponse unauthenticated = new MockHttpServletResponse();
        handlers.commence(request, unauthenticated,
                new org.springframework.security.authentication.BadCredentialsException("bad"));
        assertThat(unauthenticated.getStatus()).isEqualTo(401);
        assertThat(unauthenticated.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }
}
