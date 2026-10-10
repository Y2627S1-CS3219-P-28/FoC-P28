/*
 * AI assistance: OpenAI Codex, 2026-10-09, regression test generation.
 * Vincent approved this one-time Credit security exception in CHANGE-090.
 * Tests use real bearer filters and signed local JWTs, not injected authentication.
 * Credit owner review and live Google delivery verification remain pending.
 */
package sg.edu.nus.foc.credit.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sg.edu.nus.foc.credit.api.CreditController;
import sg.edu.nus.foc.credit.api.CreditOrderEventController;
import sg.edu.nus.foc.credit.config.CreditProperties;
import sg.edu.nus.foc.credit.config.CreditPushProperties;
import sg.edu.nus.foc.credit.credit.CreditService;
import sg.edu.nus.foc.credit.error.GlobalExceptionHandler;
import sg.edu.nus.foc.credit.messaging.AcceptedOrderCancellationEventHandler;
import sg.edu.nus.foc.credit.messaging.OpenOrderRefundEventHandler;
import sg.edu.nus.foc.credit.messaging.OrderCompletionEventHandler;
import sg.edu.nus.foc.credit.messaging.OrderEventMessage;
import sg.edu.nus.foc.credit.messaging.OrderEventPayloadDecoder;

@WebMvcTest
@ContextConfiguration(classes = {CreditController.class, CreditOrderEventController.class, GlobalExceptionHandler.class,
        PubSubPushSecurityTest.LocalKeySecurityConfig.class, PubSubPushSecurityTest.TestBeans.class})
class PubSubPushSecurityTest {

    private static final String AUDIENCE = "https://credit-test.invalid/push";
    private static final String SERVICE_ACCOUNT = "credit-push@test-project.iam.gserviceaccount.com";
    private static final String SUBSCRIPTION = "projects/test-project/subscriptions/refund";
    private static final String PAYLOAD = "{\"eventId\":\"security-test-event\"}";
    private static final String FIREBASE_TOKEN = "firebase-test-token";
    private static final RSAKey KEY = key();
    private static final HttpServer JWKS = server();

    @Autowired MockMvc mvc;
    @MockitoBean RoleProvider roles;
    @MockitoBean CreditService credits;
    @MockitoBean OrderEventPayloadDecoder decoder;
    @MockitoBean OpenOrderRefundEventHandler openRefundHandler;
    @MockitoBean AcceptedOrderCancellationEventHandler acceptedCancellationHandler;
    @MockitoBean OrderCompletionEventHandler completionHandler;
    @MockitoBean(name = "jwtDecoder") JwtDecoder firebaseDecoder;

    @BeforeEach
    void firebaseIdentity() {
        when(firebaseDecoder.decode(anyString())).thenThrow(new BadJwtException("Not a Firebase token"));
        Jwt user = Jwt.withTokenValue(FIREBASE_TOKEN).header("alg", "RS256")
                .subject("courier-user").claim("iss", "https://securetoken.google.com/test-project")
                .issuedAt(Instant.now().minusSeconds(10)).expiresAt(Instant.now().plusSeconds(300)).build();
        doReturn(user).when(firebaseDecoder).decode(FIREBASE_TOKEN);
        when(roles.rolesFor(any())).thenReturn(Set.of(Role.COURIER));
        when(decoder.decode(eq(SUBSCRIPTION), eq(PAYLOAD), eq(SUBSCRIPTION),
                eq("OpenOrderRefundTaskEvent")))
                .thenReturn(new OrderEventMessage(
                        "security-test-event", "OpenOrderRefundTaskEvent", 1,
                        "order-1", 1, Instant.now(), "requester-1",
                        null, null, null));
    }

    @AfterAll
    static void stopKeys() {
        JWKS.stop(0);
    }

    @Test
    void signedPushReachesTypedHandlerEvenWhenUserRoleLookupIsUnavailable() throws Exception {
        when(roles.rolesFor(any())).thenThrow(new RoleLookupException("User unavailable", null));

        push(token("valid")).andExpect(status().isNoContent());

        verify(decoder).decode(SUBSCRIPTION, PAYLOAD, SUBSCRIPTION, "OpenOrderRefundTaskEvent");
        verify(openRefundHandler).handle(any(OrderEventMessage.class));
        verifyNoInteractions(acceptedCancellationHandler, completionHandler);
        verifyNoInteractions(roles, firebaseDecoder, credits);
    }

    @ParameterizedTest
    @ValueSource(strings = {"signature", "issuer", "audience", "email", "unverified", "expired", "firebase"})
    void invalidSignedPushIdentityNeverReachesHandlerOrUserLookup(String invalid) throws Exception {
        push(token(invalid)).andExpect(status().isUnauthorized());
        verifyNoEventInteractions();
        verifyNoInteractions(roles, firebaseDecoder, credits);
    }

    @Test
    void everyTypedPushPathRejectsAMissingToken() throws Exception {
        for (String path : List.of(
                CreditOrderEventController.OPEN_REFUND_PATH,
                CreditOrderEventController.ACCEPTED_CANCELLATION_PATH,
                CreditOrderEventController.COMPLETION_PATH)) {
            mvc.perform(post(path)
                    .contentType(MediaType.APPLICATION_JSON).content(envelope()))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoEventInteractions();
        verifyNoInteractions(roles, firebaseDecoder, credits);
    }

    @Test
    void malformedPushTokenIsRejected() throws Exception {
        push("not-a-jwt").andExpect(status().isUnauthorized());
        verifyNoEventInteractions();
        verifyNoInteractions(roles, firebaseDecoder, credits);
    }

    @Test
    void ordinaryUserEndpointStillResolvesCourierRole() throws Exception {
        mvc.perform(post("/api/credits/orders/order-test/hold-for-reopen")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + FIREBASE_TOKEN))
                .andExpect(status().isOk());
        verify(firebaseDecoder).decode(FIREBASE_TOKEN);
        verify(roles).rolesFor(any());
        verify(credits).holdForReopen("order-test", "courier-user");
        verifyNoEventInteractions();
    }

    @Test
    void nonCourierUserCannotAccessCourierEndpoint() throws Exception {
        when(roles.rolesFor(any())).thenReturn(Set.of(Role.REQUESTER));
        mvc.perform(post("/api/credits/orders/order-test/hold-for-reopen")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + FIREBASE_TOKEN))
                .andExpect(status().isForbidden());
        verify(roles).rolesFor(any());
        verifyNoInteractions(credits);
        verifyNoEventInteractions();
    }

    @Test
    void userRoleLookupOutageStillFailsClosed() throws Exception {
        when(roles.rolesFor(any())).thenThrow(new RoleLookupException("User unavailable", null));
        mvc.perform(post("/api/credits/orders/order-test/hold-for-reopen")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + FIREBASE_TOKEN))
                .andExpect(status().isServiceUnavailable());
        verify(roles).rolesFor(any());
        verifyNoInteractions(credits);
        verifyNoEventInteractions();
    }

    @Test
    void servicePushTokenDoesNotAuthenticateAsFirebaseUser() throws Exception {
        mvc.perform(post("/api/credits/orders/order-test/hold-for-reopen")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("valid")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(roles, credits);
        verifyNoEventInteractions();
    }

    @Test
    void authenticatedCourierCannotAssignAnotherUserIdentity() throws Exception {
        mvc.perform(put("/api/credits/orders/order-test/courier-assignment")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + FIREBASE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"courierId\":\"other-user\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(credits);
        verifyNoEventInteractions();
    }

    private org.springframework.test.web.servlet.ResultActions push(String token) throws Exception {
        return mvc.perform(post(CreditOrderEventController.OPEN_REFUND_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(envelope()));
    }

    private void verifyNoEventInteractions() {
        verifyNoInteractions(decoder, openRefundHandler, acceptedCancellationHandler,
                completionHandler);
    }

    private static String envelope() {
        String data = Base64.getEncoder().encodeToString(PAYLOAD.getBytes(StandardCharsets.UTF_8));
        return "{\"message\":{\"data\":\"" + data + "\",\"messageId\":\"test-message\"},"
                + "\"subscription\":\"" + SUBSCRIPTION + "\"}";
    }

    private static String token(String variant) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject("push-service-subject")
                .issuer(variant.equals("issuer") ? "https://untrusted.invalid" : variant.equals("firebase")
                        ? "https://securetoken.google.com/test-project" : "https://accounts.google.com")
                .audience(variant.equals("audience") ? "https://wrong.invalid" : AUDIENCE)
                .claim("email", variant.equals("email") ? "other@test-project.iam.gserviceaccount.com" : SERVICE_ACCOUNT)
                .claim("email_verified", !variant.equals("unverified"))
                .issueTime(Date.from(now.minusSeconds(7200)))
                .expirationTime(Date.from(variant.equals("expired") ? now.minusSeconds(3600) : now.plusSeconds(300)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(variant.equals("signature") ? key() : KEY));
        return jwt.serialize();
    }

    private static RSAKey key() {
        try {
            return new RSAKeyGenerator(2048).keyID("local-push-test").generate();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static HttpServer server() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            byte[] keys = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            server.createContext("/jwks", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, keys.length);
                try (var body = exchange.getResponseBody()) { body.write(keys); }
            });
            server.start();
            return server;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class LocalKeySecurityConfig extends SecurityConfig {
        // Only the key source is substituted. Both production chains and validators remain real.
        @Override
        JwtDecoder pubSubPushJwtDecoder(CreditPushProperties properties) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(
                    "http://127.0.0.1:" + JWKS.getAddress().getPort() + "/jwks").build();
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefault(), new PubSubPushTokenValidator(properties)));
            return decoder;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean Clock clock() { return Clock.systemUTC(); }
        @Bean CreditProperties creditProperties() {
            return new CreditProperties(new CreditProperties.AuthSettings("test-project", "localhost:9099"),
                    new CreditProperties.UserServiceSettings(CreditProperties.Mode.HTTP,
                            "http://unused-user.invalid", List.of(), null), List.of());
        }
        @Bean CreditPushProperties pushProperties() {
            return new CreditPushProperties("test-project", AUDIENCE, SERVICE_ACCOUNT,
                    "completion", "refund", "unused-accepted-cancellation");
        }
    }
}
