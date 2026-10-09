/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-25
 * Mode: Test generation and testing assistance.
 * Scope: Generated initial unit tests for team-finalized credit behavior.
 * Author review: I reviewed for correctness.
 */
package sg.edu.nus.foc.credit.credit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import sg.edu.nus.foc.credit.error.InvalidCreditIdException;
import sg.edu.nus.foc.credit.error.InvalidOrderEventException;
import sg.edu.nus.foc.credit.error.ReservationNotFoundException;

@ExtendWith(OutputCaptureExtension.class)
class CreditServiceTest {

    private final RecordingRepository repository = new RecordingRepository();
    private final CreditService service = new CreditService(repository);

    @Test
    void delegatesValidRegistrationReservationAndLookup(CapturedOutput output) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-09-25T08:00:00Z");
        assertThat(service.initializeAccount(eventId, "user-1", occurredAt)).isSameAs(repository.registration);
        assertThat(service.getAccount("user-1")).isEqualTo(repository.account);
        assertThat(service.reserve("order-1", "user-1", 5)).isSameAs(repository.reservation);
        assertThat(service.getReservation("order-1", "user-1")).isEqualTo(repository.creditReservation);
        assertThat(repository.eventId).isEqualTo(eventId);
        assertThat(repository.occurredAt).isEqualTo(occurredAt);
        assertThat(repository.amount).isEqualTo(5);
        assertThat(output)
                .contains("credit_account_initialized userId=user-1 initialCredits=50 outcome=allocated")
                .contains("credits_reserved requesterId=user-1 orderId=order-1 amount=5 outcome=reserved");
    }

    @Test
    void rejectsUnsafeOpaqueIdentifiers(CapturedOutput output) {
        for (String invalid : new String[]{null, "", " ", ".", "..", "a/b", "__reserved__",
                "x".repeat(129)}) {
            assertThatThrownBy(() -> service.reserve(invalid, "user-1", 1))
                    .isInstanceOf(InvalidCreditIdException.class);
        }
        assertThatThrownBy(() -> service.initializeAccount(UUID.randomUUID(), "a/b", Instant.now()))
                .isInstanceOf(InvalidCreditIdException.class);
        assertThatThrownBy(() -> service.reserve("order-1", "user-1", 0))
                .isInstanceOf(sg.edu.nus.foc.credit.error.InvalidCreditAmountException.class);
        assertThat(output).contains("credit_operation_failed");
    }

    @Test
    void hidesMissingAndOtherUsersReservationsAsNotFound() {
        assertThatThrownBy(() -> service.getReservation("order-1", "other-user"))
                .isInstanceOf(ReservationNotFoundException.class);
        repository.found = Optional.empty();
        assertThatThrownBy(() -> service.getReservation("order-1", "user-1"))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void rejectsMissingCreditAccount() {
        repository.accountFound = Optional.empty();
        assertThatThrownBy(() -> service.getAccount("missing"))
                .isInstanceOf(sg.edu.nus.foc.credit.error.AccountNotFoundException.class);
    }

    @Test
    void delegatesAssignmentHoldAndValidOutcomes(CapturedOutput output) {
        service.assignCourier("order-1", "courier-1");
        service.holdForReopen("order-1", "courier-1");
        CreditOutcomeEvent refund = outcome(
                CreditOutcomeType.OPEN_ORDER_REFUND, null, "CANCELLED");
        CreditOutcomeEvent expiredRefund = outcome(
                CreditOutcomeType.OPEN_ORDER_REFUND, null, "EXPIRED");
        CreditOutcomeEvent acceptedCancellation = outcome(
                CreditOutcomeType.ACCEPTED_ORDER_CANCELLATION, null, "ABORTED");
        CreditOutcomeEvent completion = outcome(
                CreditOutcomeType.ORDER_COMPLETION, "courier-1", "COMPLETED");

        service.processOutcome(refund);
        service.processOutcome(expiredRefund);
        service.processOutcome(acceptedCancellation);
        service.processOutcome(completion);

        assertThat(repository.assignedCourier).isEqualTo("courier-1");
        assertThat(repository.holdCaller).isEqualTo("courier-1");
        assertThat(repository.refund).isSameAs(acceptedCancellation);
        assertThat(repository.settlement).isSameAs(completion);
        assertThat(output)
                .contains("credit_reservation_assigned orderId=order-1 courierId=courier-1")
                .contains("credit_reservation_held_for_reopen orderId=order-1 courierId=courier-1")
                .contains("credits_refunded requesterId=user-1 orderId=order-1 amount=5 reason=OPEN_ORDER_REFUND")
                .contains("credits_refunded requesterId=user-1 orderId=order-1 amount=5 reason=ACCEPTED_ORDER_CANCELLATION")
                .contains("credits_transferred requesterId=user-1 courierId=courier-1 orderId=order-1 amount=5");
    }

    @Test
    void rejectsInvalidOutcomeContracts() {
        assertThatThrownBy(() -> service.processOutcome(null))
                .isInstanceOf(InvalidOrderEventException.class);
        assertThatThrownBy(() -> service.processOutcome(outcome(
                CreditOutcomeType.OPEN_ORDER_REFUND, "courier-1", "CANCELLED")))
                .isInstanceOf(InvalidOrderEventException.class);
        assertThatThrownBy(() -> service.processOutcome(outcome(
                CreditOutcomeType.ACCEPTED_ORDER_CANCELLATION, null, "OPEN")))
                .isInstanceOf(InvalidOrderEventException.class);
        assertThatThrownBy(() -> service.processOutcome(outcome(
                CreditOutcomeType.ORDER_COMPLETION, null, "COMPLETED")))
                .isInstanceOf(InvalidOrderEventException.class);
        assertThatThrownBy(() -> service.processOutcome(outcome(
                CreditOutcomeType.ORDER_COMPLETION, "courier-1", "OPEN")))
                .isInstanceOf(InvalidOrderEventException.class);
    }

    @Test
    void rejectsIncompleteOutcomeMetadata() {
        CreditOutcomeEvent valid = outcome(CreditOutcomeType.OPEN_ORDER_REFUND, null, "CANCELLED");
        for (CreditOutcomeEvent invalid : new CreditOutcomeEvent[]{
                new CreditOutcomeEvent(valid.eventId(), null, 1, valid.orderId(), 2, valid.occurredAt(),
                        valid.actorId(), valid.requesterId(), null, 5, valid.orderStatus(), false, null),
                new CreditOutcomeEvent(valid.eventId(), valid.type(), 1, valid.orderId(), 2, null,
                        valid.actorId(), valid.requesterId(), null, 5, valid.orderStatus(), false, null),
                new CreditOutcomeEvent(valid.eventId(), valid.type(), 2, valid.orderId(), 2, valid.occurredAt(),
                        valid.actorId(), valid.requesterId(), null, 5, valid.orderStatus(), false, null),
                new CreditOutcomeEvent(valid.eventId(), valid.type(), 1, valid.orderId(), -1, valid.occurredAt(),
                        valid.actorId(), valid.requesterId(), null, 5, valid.orderStatus(), false, null),
                new CreditOutcomeEvent(valid.eventId(), valid.type(), 1, valid.orderId(), 2, valid.occurredAt(),
                        valid.actorId(), valid.requesterId(), null, 0, valid.orderStatus(), false, null)
        }) {
            assertThatThrownBy(() -> service.processOutcome(invalid))
                    .isInstanceOf(InvalidOrderEventException.class);
        }
    }

    private CreditOutcomeEvent outcome(CreditOutcomeType type, String courierId, String status) {
        return new CreditOutcomeEvent("event-1", type, 1, "order-1", 2, Instant.EPOCH,
                "actor-1", "user-1", courierId, 5, status, false, null);
    }

    private static final class RecordingRepository implements CreditRepository {
        private final CreditAccount account = new CreditAccount("user-1", 50, 5,
                Instant.EPOCH, Instant.EPOCH);
        private final CreditReservation creditReservation = new CreditReservation("order-1", "user-1", null,
                5, ReservationStatus.RESERVED, Instant.EPOCH, Instant.EPOCH, null, null);
        private final RegistrationResult registration = new RegistrationResult(account, true);
        private final ReservationResult reservation = new ReservationResult(creditReservation, account, true);
        private Optional<CreditReservation> found = Optional.of(creditReservation);
        private Optional<CreditAccount> accountFound = Optional.of(account);
        private UUID eventId;
        private Instant occurredAt;
        private long amount;
        private String assignedCourier;
        private String holdCaller;
        private CreditOutcomeEvent refund;
        private CreditOutcomeEvent settlement;

        @Override
        public RegistrationResult initializeAccount(UUID eventId, String userId, Instant occurredAt) {
            this.eventId = eventId;
            this.occurredAt = occurredAt;
            return registration;
        }

        @Override
        public Optional<CreditAccount> findAccount(String userId) {
            return accountFound;
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

        @Override
        public void assignCourier(String orderId, String courierId) {
            assignedCourier = courierId;
        }

        @Override
        public void holdForReopen(String orderId, String callerId) {
            holdCaller = callerId;
        }

        @Override
        public void refund(CreditOutcomeEvent event) {
            refund = event;
        }

        @Override
        public void settle(CreditOutcomeEvent event) {
            settlement = event;
        }
    }
}
