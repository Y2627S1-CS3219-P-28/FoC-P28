/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-09
 * Mode: Code generation.
 * Scope: Generated credit service event controller based on provided requirements.
 * Author review: I reviewed for correctness.
 */

package sg.edu.nus.foc.credit.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import sg.edu.nus.foc.credit.notification.CreditBalanceEventStream;

@RestController
@RequestMapping("/api/credits")
@Tag(name = "Credits", description = "Closed-economy credit accounts and order reservations")
public class CreditEventController {

    private final CreditBalanceEventStream eventStream;

    public CreditEventController(CreditBalanceEventStream eventStream) {
        this.eventStream = eventStream;
    }

    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream balance change notifications for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credit notification stream opened"),
            @ApiResponse(responseCode = "401", description = "Firebase ID token is missing or invalid"),
            @ApiResponse(responseCode = "503", description = "Credit notification stream is unavailable")
    })
    public ResponseEntity<SseEmitter> events(JwtAuthenticationToken caller) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .cacheControl(CacheControl.noCache())
                .header("X-Accel-Buffering", "no")
                .body(eventStream.subscribe(caller.getName()));
    }
}
