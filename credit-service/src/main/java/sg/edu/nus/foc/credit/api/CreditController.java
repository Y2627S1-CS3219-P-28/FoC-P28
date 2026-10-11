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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sg.edu.nus.foc.credit.credit.CreditReservation;
import sg.edu.nus.foc.credit.credit.CreditService;
import sg.edu.nus.foc.credit.credit.RegistrationResult;
import sg.edu.nus.foc.credit.credit.ReservationResult;
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

    @GetMapping("/me")
    @Operation(summary = "Get current credit balances for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current credit balances"),
            @ApiResponse(responseCode = "401", description = "Firebase ID token is missing or invalid"),
            @ApiResponse(responseCode = "404", description = "Credit account has not been provisioned")
    })
    public BalanceResponse balance(JwtAuthenticationToken caller) {
        return BalanceResponse.from(service.getAccount(caller.getName()));
    }

    @GetMapping("/me/transactions")
    @Operation(summary = "List the authenticated user's credit transactions, newest first")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated credit transaction history"),
            @ApiResponse(responseCode = "400", description = "Pagination parameters are invalid"),
            @ApiResponse(responseCode = "401", description = "Firebase ID token is missing or invalid"),
            @ApiResponse(responseCode = "404", description = "Credit account has not been provisioned")
    })
    public TransactionPageResponse transactions(
            @Parameter(description = "One-based page number")
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "Transactions per page, from 1 to 100")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            JwtAuthenticationToken caller) {
        return TransactionPageResponse.from(service.getTransactions(caller.getName(), page, size));
    }

    @PutMapping("/orders/{orderId}/reservation")
    @Operation(summary = "Reserve a requester's usable credits for an order")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Credits reserved"),
            @ApiResponse(responseCode = "200", description = "Existing identical reservation returned")
    })
    public ResponseEntity<ReservationResponse> reserve(
            @Parameter(description = "Opaque Order Service order ID")
            @PathVariable @Size(max = 128) String orderId,
            @Valid @RequestBody ReserveCreditsRequest request,
            JwtAuthenticationToken caller) {
        requireSelf(request.requesterId(), caller);
        ReservationResult result = service.reserve(orderId, request.requesterId(), request.amount());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status)
                .body(ReservationResponse.from(result.reservation(), result.account()));
    }

    @GetMapping("/orders/{orderId}/reservation")
    @Operation(summary = "Recover the authenticated requester's reservation status for an order")
    @ApiResponse(responseCode = "200", description = "Reservation status")
    public ReservationResponse reservation(
            @Parameter(description = "Opaque Order Service order ID")
            @PathVariable @Size(max = 128) String orderId,
            JwtAuthenticationToken caller) {
        CreditReservation reservation = service.getReservation(orderId, caller.getName());
        return ReservationResponse.from(reservation);
    }

    @PutMapping("/orders/{orderId}/courier-assignment")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Assign the authenticated courier to an active order reservation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Courier assignment recorded or replayed"),
            @ApiResponse(responseCode = "403",
                    description = "Caller lacks the courier role or identity does not match"),
            @ApiResponse(responseCode = "404", description = "Reservation or courier credit account not found"),
            @ApiResponse(responseCode = "409", description = "Reservation is inactive or assigned elsewhere"),
            @ApiResponse(responseCode = "503", description = "User Service role lookup is unavailable")
    })
    public ResponseEntity<Void> assignCourier(
            @Parameter(description = "Opaque Order Service order ID")
            @PathVariable @Size(max = 128) String orderId,
            @Valid @RequestBody CourierAssignmentRequest request,
            JwtAuthenticationToken caller) {
        requireSelf(request.courierId(), caller);
        service.assignCourier(orderId, request.courierId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/orders/{orderId}/hold-for-reopen")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Clear the assigned courier while retaining reserved credits for reopening")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation held for reopen or already held"),
            @ApiResponse(responseCode = "403", description = "Caller is not the assigned courier"),
            @ApiResponse(responseCode = "404", description = "Reservation not found"),
            @ApiResponse(responseCode = "409", description = "Reservation is no longer active"),
            @ApiResponse(responseCode = "503", description = "User Service role lookup is unavailable")
    })
    public ResponseEntity<Void> holdForReopen(
            @Parameter(description = "Opaque Order Service order ID")
            @PathVariable @Size(max = 128) String orderId,
            JwtAuthenticationToken caller) {
        service.holdForReopen(orderId, caller.getName());
        return ResponseEntity.ok().build();
    }

    private static void requireSelf(String userId, JwtAuthenticationToken caller) {
        if (!caller.getName().equals(userId)) {
            throw new ForbiddenException("The authenticated user does not match the requested credit account.");
        }
    }
}
