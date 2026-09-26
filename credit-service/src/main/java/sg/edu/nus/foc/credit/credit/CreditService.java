/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-25
 * Mode: Code generation.
 * Scope: Generated the initial business validation and reservation-ownership implementation from team-finalized behavior.
 * Author review: I reviewed for correctness.
 */
package sg.edu.nus.foc.credit.credit;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import sg.edu.nus.foc.credit.error.InvalidCreditIdException;
import sg.edu.nus.foc.credit.error.InvalidCreditAmountException;
import sg.edu.nus.foc.credit.error.ReservationNotFoundException;

@Service
public class CreditService {

    private final CreditRepository repository;

    public CreditService(CreditRepository repository) {
        this.repository = repository;
    }

    public RegistrationResult initializeAccount(UUID eventId, String userId, Instant occurredAt) {
        requireDocumentId(userId, "userId");
        return repository.initializeAccount(eventId, userId, occurredAt);
    }

    public ReservationResult reserve(String orderId, String requesterId, long amount) {
        requireDocumentId(orderId, "orderId");
        requireDocumentId(requesterId, "requesterId");
        if (amount <= 0) {
            throw new InvalidCreditAmountException(amount);
        }
        return repository.reserve(orderId, requesterId, amount);
    }

    public CreditReservation getReservation(String orderId, String requesterId) {
        requireDocumentId(orderId, "orderId");
        return repository.findReservation(orderId)
                .filter(reservation -> reservation.requesterId().equals(requesterId))
                .orElseThrow(() -> new ReservationNotFoundException(orderId));
    }

    static void requireDocumentId(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 128 || value.contains("/")
                || value.equals(".") || value.equals("..")
                || value.startsWith("__") && value.endsWith("__")) {
            throw new InvalidCreditIdException(field);
        }
    }
}
