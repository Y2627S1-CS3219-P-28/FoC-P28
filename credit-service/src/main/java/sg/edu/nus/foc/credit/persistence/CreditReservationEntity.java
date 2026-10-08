/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-07
 * Mode: Boilerplate generation.
 * Scope: Generated the JPA credit reservation entity based on the provided PostgreSQL schema and requirements.
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
import sg.edu.nus.foc.credit.credit.CreditReservation;
import sg.edu.nus.foc.credit.credit.ReservationStatus;

@Entity
@Table(name = "credit_reservations")
public class CreditReservationEntity {

    @Id
    @Column(name = "order_id", length = 128)
    private String orderId;

    @Column(name = "requester_id", nullable = false, length = 128)
    private String requesterId;

    @Column(name = "courier_id", length = 128)
    private String courierId;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    protected CreditReservationEntity() {
    }

    CreditReservation toDomain() {
        return new CreditReservation(orderId, requesterId, courierId, amount, status,
                createdAt, updatedAt, refundedAt, paidAt);
    }

    String requesterId() {
        return requesterId;
    }

    long amount() {
        return amount;
    }
}
