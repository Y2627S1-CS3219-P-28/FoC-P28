/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-25
 * Mode: Code generation.
 * Scope: Generated the initial HTTP API, validation, ownership enforcement, response mapping, or API types from the team-finalized interface contract.
 * Author review: I reviewed for correctness and edited where needed.
 */
package sg.edu.nus.foc.credit.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sg.edu.nus.foc.credit.credit.CreditService;
import sg.edu.nus.foc.credit.credit.RegistrationResult;
import sg.edu.nus.foc.credit.error.ForbiddenException;

@RestController
@Validated
@RequestMapping("/api/credits")
@Tag(name = "Credits", description = "Closed-economy credit accounts and order reservations")
public class CreditController {

    private final CreditService service;

    public CreditController(CreditService service) {
        this.service = service;
    }

    @PostMapping("/registration-facts")
    @Operation(summary = "Allocate exactly 50 credits for an authenticated registration fact (F1.1)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Credit account initialized"),
            @ApiResponse(responseCode = "200", description = "Registration fact replayed idempotently")
    })
    public ResponseEntity<AccountResponse> register(@Valid @RequestBody RegistrationFactRequest request,
                                                     JwtAuthenticationToken caller) {
        requireSelf(request.userId(), caller);
        RegistrationResult result = service.initializeAccount(request.eventId(), request.userId(),
                request.occurredAt());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(AccountResponse.from(result.account()));
    }
    private static void requireSelf(String userId, JwtAuthenticationToken caller) {
        if (!caller.getName().equals(userId)) {
            throw new ForbiddenException("The authenticated user does not match the requested credit account.");
        }
    }
}
