/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Implementation.
 * Scope: Generated Reporting Service code for the team-specified reporting, association, storage, security, and administration contracts.
 * Author review: I reviewed for correctness
 */
package sg.edu.nus.foc.reporting.api;

import java.time.Instant;
import java.util.List;
import sg.edu.nus.foc.reporting.report.Report;
import sg.edu.nus.foc.reporting.report.ReportCategory;
import sg.edu.nus.foc.reporting.report.ReportStatus;

public record ReportResponse(String id, String reporterId, ReportCategory category, String description,
                             String orderId, String courierId, ReportStatus status, Instant createdAt,
                             Instant resolvedAt, List<AttachmentResponse> attachments) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(report.id(), report.reporterId(), report.category(), report.description(), report.orderId(), report.courierId(),
                report.status(), report.createdAt(), report.resolvedAt(), report.attachments().stream().map(AttachmentResponse::from).toList());
    }
}
