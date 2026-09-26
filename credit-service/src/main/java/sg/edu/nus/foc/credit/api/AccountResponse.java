/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-25
 * Mode: Code generation.
 * Scope: Generated the initial HTTP API, validation, ownership enforcement, response mapping, or API types from the team-finalized interface contract.
 * Author review: I reviewed for correctness.
 */
package sg.edu.nus.foc.credit.api;

import java.time.Instant;

import sg.edu.nus.foc.credit.credit.CreditAccount;

public record AccountResponse(
        String userId,
        long totalBalance,
        long reservedBalance,
        long usableBalance,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    static AccountResponse from(CreditAccount account) {
        return new AccountResponse(account.userId(), account.totalBalance(), account.reservedBalance(),
                account.usableBalance(), account.version(), account.createdAt(), account.updatedAt());
    }
}
