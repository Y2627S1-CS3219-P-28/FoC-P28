/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-07
 * Mode: Boilerplate generation.
 * Scope: Generated the JPA credit ledger entity based on the provided PostgreSQL schema and requirements.
 * Author review: I reviewed for correctness.
 */
package sg.edu.nus.foc.credit.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import sg.edu.nus.foc.credit.credit.LedgerEffectType;

@Entity
@Table(name = "credit_ledger")
public class CreditLedgerEntity {

    @Id
    @Column(name = "entry_id")
    private UUID entryId;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "order_id", length = 128)
    private String orderId;

    @Column(name = "origin_type", nullable = false, length = 80)
    private String originType;

    @Column(name = "origin_id", nullable = false, length = 128)
    private String originId;

    @Enumerated(EnumType.STRING)
    @Column(name = "effect_type", nullable = false, length = 40)
    private LedgerEffectType effectType;

    @Column(nullable = false)
    private long amount;

    @Column(name = "total_balance_delta", nullable = false)
    private long totalBalanceDelta;

    @Column(name = "reserved_balance_delta", nullable = false)
    private long reservedBalanceDelta;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CreditLedgerEntity() {
    }

    private CreditLedgerEntity(String userId, String orderId, String originType, String originId,
                               LedgerEffectType effectType, long amount, long totalBalanceDelta,
                               long reservedBalanceDelta, Instant occurredAt, Instant createdAt) {
        this.entryId = UUID.randomUUID();
        this.userId = userId;
        this.orderId = orderId;
        this.originType = originType;
        this.originId = originId;
        this.effectType = effectType;
        this.amount = amount;
        this.totalBalanceDelta = totalBalanceDelta;
        this.reservedBalanceDelta = reservedBalanceDelta;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
    }

    static CreditLedgerEntity initialAllocation(String userId, String eventId, long amount,
                                                 Instant occurredAt, Instant createdAt) {
        return new CreditLedgerEntity(userId, null, "REGISTRATION_EVENT", eventId,
                LedgerEffectType.INITIAL_ALLOCATION, amount, amount, 0, occurredAt, createdAt);
    }

    static CreditLedgerEntity reservation(String userId, String orderId, long amount, Instant now) {
        return new CreditLedgerEntity(userId, orderId, "ORDER_RESERVATION", orderId,
                LedgerEffectType.RESERVATION, amount, 0, amount, now, now);
    }

    static CreditLedgerEntity refund(String userId, String orderId, String eventType,
                                     String eventId, long amount, Instant occurredAt, Instant now) {
        return new CreditLedgerEntity(userId, orderId, eventType, eventId,
                LedgerEffectType.REFUND, amount, 0, -amount, occurredAt, now);
    }

    static CreditLedgerEntity paymentSent(String userId, String orderId, String eventId,
                                           long amount, Instant occurredAt, Instant now) {
        return new CreditLedgerEntity(userId, orderId, "ORDER_COMPLETION_EVENT", eventId,
                LedgerEffectType.PAYMENT_SENT, amount, -amount, -amount, occurredAt, now);
    }

    static CreditLedgerEntity paymentReceived(String userId, String orderId, String eventId,
                                               long amount, Instant occurredAt, Instant now) {
        return new CreditLedgerEntity(userId, orderId, "ORDER_COMPLETION_EVENT", eventId,
                LedgerEffectType.PAYMENT_RECEIVED, amount, amount, 0, occurredAt, now);
    }
}
