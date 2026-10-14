/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Implementation.
 * Scope: Generated Reporting Service code for the team-specified reporting, association, storage, security, and administration contracts.
 * Author review: I reviewed for correctness
 */
package sg.edu.nus.foc.reporting.report;

import java.time.Instant;
import java.util.List;

public record Report(String id, String reporterId, ReportCategory category, String description,
                     String orderId, String courierId, ReportStatus status, Instant createdAt,
                     Instant resolvedAt, List<Attachment> attachments) {
    public Report { attachments = List.copyOf(attachments); }
    public Report resolved(Instant at) { return status == ReportStatus.RESOLVED ? this : new Report(id, reporterId, category, description, orderId, courierId, ReportStatus.RESOLVED, createdAt, at, attachments); }
}
