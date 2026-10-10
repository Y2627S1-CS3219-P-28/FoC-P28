/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-25
 * Mode: Test generation and testing assistance.
 * Scope: Generated initial tests for the team-finalized Firebase and Spring Security behavior.
 * Author review: I reviewed for correctness.
 */
package sg.edu.nus.foc.credit.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.cors.CorsConfiguration;
import sg.edu.nus.foc.credit.config.CreditProperties;
import sg.edu.nus.foc.credit.config.CreditPushProperties;
import tools.jackson.databind.json.JsonMapper;

class SecurityComponentsTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void selectsConfiguredJwtDecoder() {
        CreditProperties emulator = new CreditProperties(
                new CreditProperties.AuthSettings("p", "localhost:9099"), null, List.of());
        CreditProperties production = new CreditProperties(
                new CreditProperties.AuthSettings("p", ""), null, List.of());
        assertThat(config.jwtDecoder(emulator)).isInstanceOf(EmulatorJwtDecoder.class);
        assertThat(config.jwtDecoder(production)).isInstanceOf(NimbusJwtDecoder.class);
        assertThat(config.pubSubPushJwtDecoder(pushProperties())).isInstanceOf(NimbusJwtDecoder.class);
    }

    @Test
    void choosesConfiguredRoleProviderAndRejectsMissingHttpUrl() {
        CreditProperties mock = new CreditProperties(null,
                new CreditProperties.UserServiceSettings(
                        CreditProperties.Mode.MOCK, null, List.of("admin@example.com"), null), null);
        CreditProperties http = new CreditProperties(null,
                new CreditProperties.UserServiceSettings(
                        CreditProperties.Mode.HTTP, "http://user-service", null, Duration.ofSeconds(1)), null);
        CreditProperties invalid = new CreditProperties(null,
                new CreditProperties.UserServiceSettings(CreditProperties.Mode.HTTP, " ", null, null), null);

        assertThat(config.roleProvider(mock))
                .isInstanceOf(MockUserServiceRoleProvider.class);
        assertThat(config.roleProvider(http))
                .isInstanceOf(HttpUserServiceRoleProvider.class);
        assertThatThrownBy(() -> config.roleProvider(invalid))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("USER_SERVICE_URL");
    }

    @Test
    void validatesTheExactGooglePushIdentityAndAudience() {
        PubSubPushTokenValidator validator = new PubSubPushTokenValidator(pushProperties());

        assertThat(validator.validate(pushToken(Map.of())).hasErrors()).isFalse();
        assertThat(validator.validate(pushToken(Map.of("iss", "attacker"))).hasErrors()).isTrue();
        assertThat(validator.validate(pushToken(Map.of("aud", List.of("wrong")))).hasErrors()).isTrue();
        assertThat(validator.validate(pushToken(Map.of("email", "other@example.com"))).hasErrors()).isTrue();
        assertThat(validator.validate(pushToken(Map.of("email_verified", false))).hasErrors()).isTrue();
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

        MockHttpServletResponse unavailable = new MockHttpServletResponse();
        handlers.commence(request, unavailable, new AuthenticationServiceException(
                "unavailable", new RoleLookupException("lookup failed", null)));
        assertThat(unavailable.getStatus()).isEqualTo(503);
        assertThat(unavailable.getHeader("WWW-Authenticate")).isNull();
        assertThat(unavailable.getContentAsString())
                .contains("\"error\":\"SERVICE_UNAVAILABLE\"")
                .contains("User Service role lookup is unavailable.");
    }

    private CreditPushProperties pushProperties() {
        return new CreditPushProperties("project", "https://credit.example.com", "push@example.com",
                "completion", "refund");
    }

    private Jwt pushToken(Map<String, Object> overrides) {
        Map<String, Object> claims = new java.util.HashMap<>(Map.of(
                "iss", "https://accounts.google.com",
                "aud", List.of("https://credit.example.com"),
                "email", "push@example.com",
                "email_verified", true,
                "sub", "123"));
        claims.putAll(overrides);
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claims(values -> values.putAll(claims))
                .build();
    }
}
