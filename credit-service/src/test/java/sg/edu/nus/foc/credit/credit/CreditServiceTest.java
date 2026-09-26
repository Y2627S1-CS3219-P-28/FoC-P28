/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-25
 * Mode: Test generation and testing assistance.
 * Scope: Generated initial unit and Firestore integration tests for team-finalized credit behavior.
 * Author review: I reviewed for correctness.
 */
package sg.edu.nus.foc.credit.credit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import sg.edu.nus.foc.credit.error.InvalidCreditIdException;
import sg.edu.nus.foc.credit.error.ReservationNotFoundException;

class CreditServiceTest {

    private final RecordingRepository repository = new RecordingRepository();
    private final CreditService service = new CreditService(repository);

    @Test
    void delegatesValidRegistrationReservationAndLookup() {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-09-25T08:00:00Z");
        assertThat(service.initializeAccount(eventId, "user-1", occurredAt)).isSameAs(repository.registration);
        assertThat(service.reserve("order-1", "user-1", 5)).isSameAs(repository.reservation);
        assertThat(service.getReservation("order-1", "user-1")).isEqualTo(repository.creditReservation);
        assertThat(repository.eventId).isEqualTo(eventId);
        assertThat(repository.occurredAt).isEqualTo(occurredAt);
        assertThat(repository.amount).isEqualTo(5);
    }

    @Test
    void rejectsUnsafeFirestoreIdentifiers() {
        for (String invalid : new String[]{null, "", " ", ".", "..", "a/b", "__reserved__",
                "x".repeat(129)}) {
            assertThatThrownBy(() -> service.reserve(invalid, "user-1", 1))
                    .isInstanceOf(InvalidCreditIdException.class);
        }
        assertThatThrownBy(() -> service.initializeAccount(UUID.randomUUID(), "a/b", Instant.now()))
                .isInstanceOf(InvalidCreditIdException.class);
        assertThatThrownBy(() -> service.reserve("order-1", "user-1", 0))
                .isInstanceOf(sg.edu.nus.foc.credit.error.InvalidCreditAmountException.class);
    }

    @Test
    void hidesMissingAndOtherUsersReservationsAsNotFound() {
        assertThatThrownBy(() -> service.getReservation("order-1", "other-user"))
                .isInstanceOf(ReservationNotFoundException.class);
        repository.found = Optional.empty();
        assertThatThrownBy(() -> service.getReservation("order-1", "user-1"))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    private static final class RecordingRepository implements CreditRepository {
        private final CreditAccount account = new CreditAccount("user-1", 50, 5, 1,
                Instant.EPOCH, Instant.EPOCH);
        private final CreditReservation creditReservation = new CreditReservation("order-1", "user-1", null,
                5, ReservationStatus.RESERVED, Instant.EPOCH, Instant.EPOCH, null, null);
        private final RegistrationResult registration = new RegistrationResult(account, true);
        private final ReservationResult reservation = new ReservationResult(creditReservation, account, true);
        private Optional<CreditReservation> found = Optional.of(creditReservation);
        private UUID eventId;
        private Instant occurredAt;
        private long amount;

        @Override
        public RegistrationResult initializeAccount(UUID eventId, String userId, Instant occurredAt) {
            this.eventId = eventId;
            this.occurredAt = occurredAt;
            return registration;
        }

        @Override
        public ReservationResult reserve(String orderId, String requesterId, long amount) {
            this.amount = amount;
            return reservation;
        }

        @Override
        public Optional<CreditReservation> findReservation(String orderId) {
            return found;
        }
    }
}
