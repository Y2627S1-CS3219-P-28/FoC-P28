/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-25
 * Mode: Code generation.
 * Scope: Generated the initial Firestore transaction implementation for team-finalized idempotent allocation, reservation, and ledger behavior.
 * Author review: I reviewed for correctness and edited where needed.
 */
package sg.edu.nus.foc.credit.credit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;
import sg.edu.nus.foc.credit.error.AccountNotFoundException;
import sg.edu.nus.foc.credit.error.EventConflictException;
import sg.edu.nus.foc.credit.error.InsufficientCreditsException;
import sg.edu.nus.foc.credit.error.ReservationConflictException;

@Repository
public class FirestoreCreditRepository implements CreditRepository {

    static final String ACCOUNTS = "creditAccounts";
    static final String RESERVATIONS = "creditReservations";
    static final String EVENTS = "processedEvents";
    static final String LEDGER = "creditLedger";

    private final Firestore firestore;
    private final Clock clock;

    public FirestoreCreditRepository(Firestore firestore, Clock clock) {
        this.firestore = firestore;
        this.clock = clock;
    }

    @Override
    public RegistrationResult initializeAccount(UUID eventId, String userId, Instant occurredAt) {
        String eventKey = eventId.toString();
        String payloadHash = payloadHash(ProcessedEventType.USER_REGISTERED, userId, null, occurredAt);
        return await(firestore.runTransaction(transaction -> {
            DocumentReference eventRef = events().document(eventKey);
            DocumentReference accountRef = accounts().document(userId);
            DocumentSnapshot event = transaction.get(eventRef).get();
            DocumentSnapshot account = transaction.get(accountRef).get();

            if (event.exists()) {
                if (!payloadHash.equals(event.getString("payloadHash"))) {
                    throw new EventConflictException(eventKey);
                }
                if (!account.exists()) {
                    throw new IllegalStateException("Processed registration event has no credit account");
                }
                return new RegistrationResult(fromAccount(account), false);
            }

            Instant now = clock.instant();
            transaction.set(eventRef, eventDocument(eventKey, ProcessedEventType.USER_REGISTERED,
                    userId, null, payloadHash, occurredAt, now));
            if (account.exists()) {
                return new RegistrationResult(fromAccount(account), false);
            }

            CreditAccount created = new CreditAccount(userId, CreditConstants.INITIAL_ALLOCATION,
                    0, 0, now, now);
            DocumentReference ledgerRef = ledger().document();
            transaction.set(accountRef, accountDocument(created));
            transaction.set(ledgerRef, ledgerDocument(ledgerRef.getId(), userId, null, eventKey,
                    LedgerEntryType.INITIAL_ALLOCATION, CreditConstants.INITIAL_ALLOCATION,
                    CreditConstants.INITIAL_ALLOCATION, 0, occurredAt, now));
            return new RegistrationResult(created, true);
        }));
    }

    @Override
    public ReservationResult reserve(String orderId, String requesterId, long amount) {
        return await(firestore.runTransaction(transaction -> {
            DocumentReference reservationRef = reservations().document(orderId);
            DocumentReference accountRef = accounts().document(requesterId);
            DocumentSnapshot reservation = transaction.get(reservationRef).get();
            DocumentSnapshot account = transaction.get(accountRef).get();

            if (reservation.exists()) {
                CreditReservation existing = fromReservation(reservation);
                if (!existing.requesterId().equals(requesterId) || existing.amount() != amount) {
                    throw new ReservationConflictException(orderId);
                }
                if (!account.exists()) {
                    throw new IllegalStateException("Credit reservation has no requester account");
                }
                return new ReservationResult(existing, fromAccount(account), false);
            }
            if (!account.exists()) {
                throw new AccountNotFoundException(requesterId);
            }

            CreditAccount current = fromAccount(account);
            if (current.usableBalance() < amount) {
                throw new InsufficientCreditsException(current.usableBalance(), amount);
            }

            Instant now = clock.instant();
            CreditAccount updated = new CreditAccount(current.userId(), current.totalBalance(),
                    Math.addExact(current.reservedBalance(), amount), Math.addExact(current.version(), 1),
                    current.createdAt(), now);
            CreditReservation created = new CreditReservation(orderId, requesterId, null, amount,
                    ReservationStatus.RESERVED, now, now, null, null);
            DocumentReference ledgerRef = ledger().document();
            transaction.set(accountRef, accountDocument(updated));
            transaction.set(reservationRef, reservationDocument(created));
            transaction.set(ledgerRef, ledgerDocument(ledgerRef.getId(), requesterId, orderId, null,
                    LedgerEntryType.RESERVATION, amount, 0, amount, now, now));
            return new ReservationResult(created, updated, true);
        }));
    }

    @Override
    public Optional<CreditReservation> findReservation(String orderId) {
        DocumentSnapshot reservation = await(reservations().document(orderId).get());
        return reservation.exists() ? Optional.of(fromReservation(reservation)) : Optional.empty();
    }

    private CollectionReference accounts() {
        return firestore.collection(ACCOUNTS);
    }

    private CollectionReference reservations() {
        return firestore.collection(RESERVATIONS);
    }

    private CollectionReference events() {
        return firestore.collection(EVENTS);
    }

    private CollectionReference ledger() {
        return firestore.collection(LEDGER);
    }

    static Map<String, Object> accountDocument(CreditAccount account) {
        Map<String, Object> document = new HashMap<>();
        document.put("userId", account.userId());
        document.put("totalBalance", account.totalBalance());
        document.put("reservedBalance", account.reservedBalance());
        document.put("version", account.version());
        document.put("createdAt", timestamp(account.createdAt()));
        document.put("updatedAt", timestamp(account.updatedAt()));
        return document;
    }

    static Map<String, Object> reservationDocument(CreditReservation reservation) {
        Map<String, Object> document = new HashMap<>();
        document.put("orderId", reservation.orderId());
        document.put("requesterId", reservation.requesterId());
        document.put("courierId", reservation.courierId());
        document.put("amount", reservation.amount());
        document.put("status", reservation.status().name());
        document.put("createdAt", timestamp(reservation.createdAt()));
        document.put("updatedAt", timestamp(reservation.updatedAt()));
        document.put("refundedAt", nullableTimestamp(reservation.refundedAt()));
        document.put("paidAt", nullableTimestamp(reservation.paidAt()));
        return document;
    }

    private static Map<String, Object> eventDocument(String eventId, ProcessedEventType eventType,
                                                      String userId, String orderId, String payloadHash,
                                                      Instant occurredAt, Instant processedAt) {
        Map<String, Object> document = new HashMap<>();
        document.put("eventId", eventId);
        document.put("eventType", eventType.name());
        document.put("userId", userId);
        document.put("orderId", orderId);
        document.put("payloadHash", payloadHash);
        document.put("occurredAt", timestamp(occurredAt));
        document.put("processedAt", timestamp(processedAt));
        return document;
    }

    private static Map<String, Object> ledgerDocument(String entryId, String userId, String orderId,
                                                       String eventId, LedgerEntryType entryType, long amount,
                                                       long totalBalanceDelta, long reservedBalanceDelta,
                                                       Instant occurredAt, Instant createdAt) {
        Map<String, Object> document = new HashMap<>();
        document.put("entryId", entryId);
        document.put("userId", userId);
        document.put("orderId", orderId);
        document.put("eventId", eventId);
        document.put("entryType", entryType.name());
        document.put("amount", amount);
        document.put("totalBalanceDelta", totalBalanceDelta);
        document.put("reservedBalanceDelta", reservedBalanceDelta);
        document.put("occurredAt", timestamp(occurredAt));
        document.put("createdAt", timestamp(createdAt));
        return document;
    }

    static CreditAccount fromAccount(DocumentSnapshot document) {
        return new CreditAccount(document.getString("userId"), requiredLong(document, "totalBalance"),
                requiredLong(document, "reservedBalance"), requiredLong(document, "version"),
                instant(document, "createdAt"), instant(document, "updatedAt"));
    }

    static CreditReservation fromReservation(DocumentSnapshot document) {
        return new CreditReservation(document.getString("orderId"), document.getString("requesterId"),
                document.getString("courierId"), requiredLong(document, "amount"),
                ReservationStatus.valueOf(document.getString("status")), instant(document, "createdAt"),
                instant(document, "updatedAt"), nullableInstant(document, "refundedAt"),
                nullableInstant(document, "paidAt"));
    }

    private static long requiredLong(DocumentSnapshot document, String field) {
        Long value = document.getLong(field);
        if (value == null) {
            throw new IllegalStateException("Credit document is missing " + field);
        }
        return value;
    }

    private static Instant instant(DocumentSnapshot document, String field) {
        Instant value = nullableInstant(document, field);
        if (value == null) {
            throw new IllegalStateException("Credit document is missing " + field);
        }
        return value;
    }

    private static Instant nullableInstant(DocumentSnapshot document, String field) {
        Timestamp value = document.getTimestamp(field);
        return value != null ? value.toSqlTimestamp().toInstant() : null;
    }

    private static Timestamp timestamp(Instant value) {
        return Timestamp.ofTimeSecondsAndNanos(value.getEpochSecond(), value.getNano());
    }

    private static Timestamp nullableTimestamp(Instant value) {
        return value != null ? timestamp(value) : null;
    }

    static String payloadHash(ProcessedEventType type, String userId, String orderId, Instant occurredAt) {
        String canonical = type.name() + "\n" + nullToEmpty(userId) + "\n" + nullToEmpty(orderId)
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

    private static <T> T await(ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Firestore", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            while (cause instanceof ExecutionException && cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Firestore operation failed", cause);
        }
    }
}
