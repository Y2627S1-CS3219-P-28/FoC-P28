/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-08
 * Mode: Code generation.
 * Scope: Defined the supported idempotent Credit Service operations.
 * Author review: I reviewed for correctness.
 */
package sg.edu.nus.foc.credit.persistence;

enum IdempotencyOperation {
    USER_REGISTERED,
    ORDER_CANCELLED,
    ORDER_EXPIRED,
    ORDER_ABORTED,
    ORDER_COMPLETED
}
