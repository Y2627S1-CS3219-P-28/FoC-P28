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

import sg.edu.nus.foc.reporting.error.ConflictException;
import sg.edu.nus.foc.reporting.error.ForbiddenException;

public record Report(String id, String reporterId, ReportCategory category, String description,
                     String orderId, String courierId, ReportStatus status, Instant createdAt,
                     Instant responseDeadlineAt, String courierResponse, Instant respondedAt,
                     Instant resolvedAt, List<Attachment> attachments,
                     List<Attachment> responseAttachments) {
    public Report {
        attachments = List.copyOf(attachments == null ? List.of() : attachments);
        responseAttachments = List.copyOf(responseAttachments == null ? List.of() : responseAttachments);
    }

    public Report reconcile(Instant now) {
        if (status != ReportStatus.AWAITING_COURIER_RESPONSE || responseDeadlineAt == null
                || now.isBefore(responseDeadlineAt)) return this;
        return copy(ReportStatus.READY_FOR_REVIEW, responseDeadlineAt, courierResponse, respondedAt, resolvedAt,
                responseAttachments);
    }

    public Report respond(String callerId, String explanation, Instant at, List<Attachment> evidence) {
        Report current = reconcile(at);
        if (courierId == null || !courierId.equals(callerId)) {
            throw new ForbiddenException("Only the reported courier can respond to this report.");
        }
        if (current.respondedAt != null) throw new ConflictException("A courier response has already been submitted.");
        if (current.status != ReportStatus.AWAITING_COURIER_RESPONSE) {
            throw new ConflictException(responseDeadlineAt != null && !at.isBefore(responseDeadlineAt)
                    ? "The 24-hour response window has closed."
                    : "This report no longer accepts a courier response.");
        }
        return copy(ReportStatus.READY_FOR_REVIEW, responseDeadlineAt, explanation, at, resolvedAt, evidence);
    }

    public Report resolved(Instant at) {
        Report current = reconcile(at);
        if (current.status == ReportStatus.RESOLVED) return current;
        if (current.status != ReportStatus.READY_FOR_REVIEW) {
            throw new ConflictException("The courier may respond before this report is resolved.");
        }
        return copy(ReportStatus.RESOLVED, current.responseDeadlineAt, current.courierResponse,
                current.respondedAt, at, current.responseAttachments);
    }

    public boolean participant(String callerId) {
        return reporterId.equals(callerId) || (courierId != null && courierId.equals(callerId));
    }

    private Report copy(ReportStatus nextStatus, Instant deadline, String response, Instant responseAt,
                        Instant resolutionAt, List<Attachment> responseEvidence) {
        return new Report(id, reporterId, category, description, orderId, courierId, nextStatus, createdAt,
                deadline, response, responseAt, resolutionAt, attachments, responseEvidence);
    }
}
