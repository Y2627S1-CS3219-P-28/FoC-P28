/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Implementation.
 * Scope: Generated Reporting Service code for the team-specified reporting, association, storage, security, and administration contracts.
 * Author review: I reviewed for correctness
 */
package sg.edu.nus.foc.reporting.report;

import java.time.Instant;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface ReportRepository {
    Report save(Report report);
    Optional<Report> findById(String id);
    PageResult<Report> findAll(int page, int size);
    default PageResult<Report> findAll(int page, int size, String orderId, ReportStatus status) {
        return findAll(page, size);
    }
    PageResult<Report> findByReporterId(String reporterId, int page, int size);
    PageResult<Report> findByCourierId(String courierId, int page, int size);
    Optional<Report> update(String id, UnaryOperator<Report> operation);
    default java.util.List<String> dueResponseIds(Instant now) { return java.util.List.of(); }
}
