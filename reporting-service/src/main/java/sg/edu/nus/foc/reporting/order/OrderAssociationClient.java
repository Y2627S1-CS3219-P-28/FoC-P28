/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Implementation.
 * Scope: Generated Reporting Service code for the team-specified reporting, association, storage, security, and administration contracts.
 * Author review: I reviewed for correctness
 */
package sg.edu.nus.foc.reporting.order;

public interface OrderAssociationClient {
    AssociatedOrder placeReportHold(String orderId, String reportId, boolean requireAssignedCourier,
                                    String bearerToken);
    void releaseReportHold(String orderId, String reportId, String bearerToken);
}
