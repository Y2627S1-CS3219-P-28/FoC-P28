/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-07
 * Mode: Code generation.
 * Scope: Generated transactional PostgreSQL persistence, locking, idempotency, and ledger behavior from provided requirements.
 * Author review: I reviewed for correctness.
 */
package sg.edu.nus.foc.credit.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.foc.credit.credit.CreditAccount;
import sg.edu.nus.foc.credit.credit.CreditConstants;
import sg.edu.nus.foc.credit.credit.CreditRepository;
import sg.edu.nus.foc.credit.credit.CreditReservation;
import sg.edu.nus.foc.credit.credit.RegistrationResult;
import sg.edu.nus.foc.credit.credit.ReservationResult;
import sg.edu.nus.foc.credit.error.AccountNotFoundException;
import sg.edu.nus.foc.credit.error.EventConflictException;
import sg.edu.nus.foc.credit.error.InsufficientCreditsException;
import sg.edu.nus.foc.credit.error.ReservationConflictException;

@Repository
public class JpaCreditRepository implements CreditRepository {

    private final CreditAccountJpaRepository accounts;
    private final CreditReservationJpaRepository reservations;
    private final CreditIdempotencyJpaRepository idempotencyRecords;
    private final CreditLedgerJpaRepository ledger;
    private final Clock clock;

    public JpaCreditRepository(CreditAccountJpaRepository accounts,
                               CreditReservationJpaRepository reservations,
                               CreditIdempotencyJpaRepository idempotencyRecords,
                               CreditLedgerJpaRepository ledger,
                               Clock clock) {
        this.accounts = accounts;
        this.reservations = reservations;
        this.idempotencyRecords = idempotencyRecords;
        this.ledger = ledger;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RegistrationResult initializeAccount(UUID eventId, String userId, Instant occurredAt) {
        String eventKey = eventId.toString();
        String hash = payloadHash(IdempotencyOperation.USER_REGISTERED, userId, null, occurredAt);
        Instant now = clock.instant();
        int claimed = idempotencyRecords.insertIfAbsent(
                IdempotencyOperation.USER_REGISTERED, eventKey, hash, now);

        if (claimed == 0) {
            CreditIdempotencyRecordEntity existing = idempotencyRecords.findById(
                    new CreditIdempotencyRecordId(IdempotencyOperation.USER_REGISTERED, eventKey))
                    .orElseThrow();
            if (!hash.equals(existing.payloadHash())) {
                throw new EventConflictException(eventKey);
            }
            CreditAccount account = accounts.findById(userId)
                    .map(CreditAccountEntity::toDomain)
                    .orElseThrow(() -> new IllegalStateException(
                            "Processed registration event has no credit account"));
            return new RegistrationResult(account, false);
        }

        int created = accounts.insertIfAbsent(userId, CreditConstants.INITIAL_ALLOCATION, now);
        CreditAccountEntity account = accounts.findById(userId).orElseThrow();
        if (created == 1) {
            ledger.save(CreditLedgerEntity.initialAllocation(
                    userId, eventKey, CreditConstants.INITIAL_ALLOCATION, occurredAt, now));
        }
        return new RegistrationResult(account.toDomain(), created == 1);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CreditAccount> findAccount(String userId) {
        return accounts.findById(userId).map(CreditAccountEntity::toDomain);
    }

    @Override
    @Transactional
    public ReservationResult reserve(String orderId, String requesterId, long amount) {
        Optional<CreditReservationEntity> existing = reservations.findById(orderId);
        if (existing.isPresent()) {
            return replayReservation(existing.get(), orderId, requesterId, amount);
        }
        if (!accounts.existsById(requesterId)) {
            throw new AccountNotFoundException(requesterId);
        }

        Instant now = clock.instant();
        int created = reservations.insertIfAbsent(orderId, requesterId, amount, now);
        CreditReservationEntity reservation = reservations.findById(orderId).orElseThrow();
        if (created == 0) {
            return replayReservation(reservation, orderId, requesterId, amount);
        }

        CreditAccountEntity account = accounts.findByUserIdForUpdate(requesterId)
                .orElseThrow(() -> new AccountNotFoundException(requesterId));
        CreditAccount current = account.toDomain();
        if (current.usableBalance() < amount) {
            throw new InsufficientCreditsException(current.usableBalance(), amount);
        }

        account.reserve(amount, now);
        ledger.save(CreditLedgerEntity.reservation(requesterId, orderId, amount, now));
        return new ReservationResult(reservation.toDomain(), account.toDomain(), true);
    }

    private ReservationResult replayReservation(CreditReservationEntity reservation, String orderId,
                                                String requesterId, long amount) {
        if (!reservation.requesterId().equals(requesterId) || reservation.amount() != amount) {
            throw new ReservationConflictException(orderId);
        }
        CreditAccount account = accounts.findById(requesterId)
                .map(CreditAccountEntity::toDomain)
                .orElseThrow(() -> new IllegalStateException(
                        "Credit reservation has no requester account"));
        return new ReservationResult(reservation.toDomain(), account, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CreditReservation> findReservation(String orderId) {
        return reservations.findById(orderId).map(CreditReservationEntity::toDomain);
    }

    static String payloadHash(IdempotencyOperation operation, String userId, String orderId,
                              Instant occurredAt) {
        String canonical = operation.name() + "\n" + nullToEmpty(userId) + "\n"
                + nullToEmpty(orderId)
                + "\n" + occurredAt;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
