/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-09
 * Mode: Test generation.
 * Scope: Generated tests for credit event to test the provided requirements.
 * Author review: I reviewed for correctness.
 */

package sg.edu.nus.foc.credit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import sg.edu.nus.foc.credit.error.CreditStreamUnavailableException;
import sg.edu.nus.foc.credit.notification.CreditBalanceEventStream;

class CreditEventControllerTest {

    @Test
    void subscribesOnlyTheAuthenticatedJwtSubject() {
        CreditBalanceEventStream stream = mock(CreditBalanceEventStream.class);
        SseEmitter emitter = new SseEmitter();
        when(stream.subscribe("firebase-user-123")).thenReturn(emitter);
        CreditEventController controller = new CreditEventController(stream);
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("firebase-user-123")
                .build();

        ResponseEntity<SseEmitter> response = controller.events(
                new JwtAuthenticationToken(jwt, List.of()));

        assertThat(response.getBody()).isSameAs(emitter);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_EVENT_STREAM);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).contains("no-cache");
        assertThat(response.getHeaders().getFirst("X-Accel-Buffering")).isEqualTo("no");
        verify(stream).subscribe("firebase-user-123");
    }

    @Test
    void propagatesAnUnavailableInitialStream() {
        CreditBalanceEventStream stream = mock(CreditBalanceEventStream.class);
        CreditStreamUnavailableException failure = new CreditStreamUnavailableException(
                new IllegalStateException("closed"));
        when(stream.subscribe("firebase-user-123")).thenThrow(failure);
        CreditEventController controller = new CreditEventController(stream);
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("firebase-user-123")
                .build();

        assertThatThrownBy(() -> controller.events(new JwtAuthenticationToken(jwt, List.of())))
                .isSameAs(failure);
    }
}
