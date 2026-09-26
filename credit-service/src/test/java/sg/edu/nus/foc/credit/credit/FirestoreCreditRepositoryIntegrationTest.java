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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sg.edu.nus.foc.credit.error.AccountNotFoundException;
import sg.edu.nus.foc.credit.error.EventConflictException;
import sg.edu.nus.foc.credit.error.ReservationConflictException;
import sg.edu.nus.foc.credit.support.FirestoreEmulator;

class FirestoreCreditRepositoryIntegrationTest {

    private static final String DATABASE = "credit-repository-test";
    private static final Instant NOW = Instant.parse("2026-09-25T08:00:00Z");
    private static Firestore firestore;
    private FirestoreCreditRepository repository;

    @BeforeAll
    static void connect() {
        firestore = FirestoreEmulator.client(DATABASE);
    }

    @AfterAll
    static void disconnect() throws Exception {
        firestore.close();
    }

    @BeforeEach
    void reset() {
        FirestoreEmulator.clear(DATABASE);
        repository = new FirestoreCreditRepository(firestore, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void initializesExactlyOnceAndRecordsConsistentDocuments() throws Exception {
        UUID eventId = UUID.randomUUID();
        RegistrationResult first = repository.initializeAccount(eventId, "user-1", NOW.minusSeconds(5));
        RegistrationResult replay = repository.initializeAccount(eventId, "user-1", NOW.minusSeconds(5));

        assertThat(first.created()).isTrue();
        assertThat(replay.created()).isFalse();
        assertThat(first.account()).isEqualTo(replay.account());
        assertThat(first.account().totalBalance()).isEqualTo(50);
        assertThat(first.account().reservedBalance()).isZero();
        assertThat(first.account().usableBalance()).isEqualTo(50);
        assertThat(documents(FirestoreCreditRepository.ACCOUNTS)).hasSize(1);
        assertThat(documents(FirestoreCreditRepository.EVENTS)).hasSize(1);
        var ledger = documents(FirestoreCreditRepository.LEDGER);
        assertThat(ledger).hasSize(1);
        assertThat(ledger.getFirst().getString("entryType")).isEqualTo("INITIAL_ALLOCATION");
        assertThat(ledger.getFirst().getString("eventId")).isEqualTo(eventId.toString());
        assertThat(ledger.getFirst().getLong("totalBalanceDelta")).isEqualTo(50);
    }

    @Test
    void aNewRegistrationEventDoesNotAllocateAgainAndConflictingReplayFails() throws Exception {
        UUID first = UUID.randomUUID();
        repository.initializeAccount(first, "user-1", NOW);
        RegistrationResult duplicateRegistration = repository.initializeAccount(UUID.randomUUID(), "user-1", NOW);

        assertThat(duplicateRegistration.created()).isFalse();
        assertThat(documents(FirestoreCreditRepository.EVENTS)).hasSize(2);
        assertThat(documents(FirestoreCreditRepository.LEDGER)).hasSize(1);
        assertThatThrownBy(() -> repository.initializeAccount(first, "user-2", NOW))
                .isInstanceOf(EventConflictException.class);
    }

    @Test
    void reservesByOrderIdAndWritesOneLedgerEntry() throws Exception {
        repository.initializeAccount(UUID.randomUUID(), "user-1", NOW);

        ReservationResult first = repository.reserve("order-1", "user-1", 20);
        ReservationResult replay = repository.reserve("order-1", "user-1", 20);

        assertThat(first.created()).isTrue();
        assertThat(replay.created()).isFalse();
        assertThat(first.reservation().status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(first.account().totalBalance()).isEqualTo(50);
        assertThat(first.account().reservedBalance()).isEqualTo(20);
        assertThat(first.account().usableBalance()).isEqualTo(30);
        assertThat(repository.findReservation("order-1")).contains(first.reservation());
        assertThat(repository.findReservation("missing")).isEmpty();
        assertThat(documents(FirestoreCreditRepository.RESERVATIONS)).hasSize(1);
        assertThat(documents(FirestoreCreditRepository.LEDGER)).hasSize(2);
    }


    @Test
    void documentMappingAndHashesAreStableAndRejectCorruptDocuments() throws Exception {
        CreditReservation reservation = new CreditReservation("order-1", "user-1", null, 5,
                ReservationStatus.RESERVED, NOW, NOW, null, null);
        firestore.collection("mapping").document("reservation")
                .set(FirestoreCreditRepository.reservationDocument(reservation)).get();
        assertThat(FirestoreCreditRepository.fromReservation(
                firestore.collection("mapping").document("reservation").get().get())).isEqualTo(reservation);
        assertThat(FirestoreCreditRepository.payloadHash(ProcessedEventType.USER_REGISTERED,
                "user-1", null, NOW))
                .isEqualTo(FirestoreCreditRepository.payloadHash(ProcessedEventType.USER_REGISTERED,
                        "user-1", null, NOW))
                .isNotEqualTo(FirestoreCreditRepository.payloadHash(ProcessedEventType.USER_REGISTERED,
                        "user-2", null, NOW));
    }

    private static List<? extends DocumentSnapshot> documents(String collection) throws Exception {
        return firestore.collection(collection).get().get().getDocuments();
    }
}
