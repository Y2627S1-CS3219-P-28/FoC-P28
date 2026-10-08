/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-07
 * Mode: Boilerplate generation.
 * Scope: Generated the JPA reservation repository based on the provided PostgreSQL requirements.
 * Author review: I reviewed for correctness.
 */
package sg.edu.nus.foc.credit.persistence;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CreditReservationJpaRepository extends JpaRepository<CreditReservationEntity, String> {

    @Modifying
    @Query(value = """
            insert into credit_reservations
                (order_id, requester_id, amount, status, created_at, updated_at)
            values (:orderId, :requesterId, :amount, 'RESERVED', :createdAt, :createdAt)
            on conflict (order_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("orderId") String orderId,
                       @Param("requesterId") String requesterId,
                       @Param("amount") long amount,
                       @Param("createdAt") Instant createdAt);
}
