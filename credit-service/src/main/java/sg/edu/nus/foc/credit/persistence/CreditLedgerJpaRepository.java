/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-07
 * Mode: Boilerplate generation.
 * Scope: Generated the JPA credit ledger repository based on the provided PostgreSQL requirements.
 * Author review: I reviewed for correctness.
 */
package sg.edu.nus.foc.credit.persistence;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface CreditLedgerJpaRepository extends JpaRepository<CreditLedgerEntity, UUID> {
    Page<CreditLedgerEntity> findByUserId(String userId, Pageable pageable);
}
