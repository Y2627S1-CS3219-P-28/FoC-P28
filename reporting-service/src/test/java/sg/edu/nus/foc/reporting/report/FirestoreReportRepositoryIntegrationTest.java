/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Test generation and verification.
 * Scope: Generated automated verification for the team-specified Reporting Service contracts.
 * Author review: I reviewed for correctness
 */
package sg.edu.nus.foc.reporting.report;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sg.edu.nus.foc.reporting.support.FirestoreEmulator;

import static org.junit.jupiter.api.Assertions.*;

class FirestoreReportRepositoryIntegrationTest {
    private static final String DATABASE = "reporting-repository-test";
    private static Firestore firestore;
    private FirestoreReportRepository repository;

    @BeforeAll static void connect() { firestore = FirestoreEmulator.client(DATABASE); }
    @AfterAll static void close() throws Exception { firestore.close(); }
    @BeforeEach void reset() { FirestoreEmulator.clear(DATABASE); repository = new FirestoreReportRepository(firestore); }

    @Test void roundTripsMetadataAndListsNewestBeforePaginating() {
        Attachment attachment = new Attachment("a1", "reports/r1/a1", "receipt.png", "image/png", 8, Instant.parse("2026-10-13T01:00:00Z"));
        Report first = report("r1", Instant.parse("2026-10-13T01:00:00Z"), List.of(attachment));
        Report second = report("r2", Instant.parse("2026-10-13T02:00:00Z"), List.of());
        Report third = report("r3", Instant.parse("2026-10-13T03:00:00Z"), List.of());
        repository.save(first); repository.save(second); repository.save(third);
        assertEquals(first, repository.findById("r1").orElseThrow());
        PageResult<Report> page = repository.findAll(1, 2);
        assertEquals(List.of(third, second), page.items()); assertEquals(3, page.totalItems()); assertEquals(2, page.totalPages());
        assertEquals(List.of(first), repository.findAll(2, 2).items());
        assertTrue(repository.findAll(3, 2).items().isEmpty());
    }

    @Test void updatesTransactionallyAndFiltersParticipantPages() {
        Report open = repository.save(report("open", Instant.parse("2026-10-13T01:00:00Z"), List.of()));
        Instant resolvedAt = Instant.parse("2026-10-13T04:00:00Z");
        Report resolved = repository.update(open.id(), item -> item.resolved(resolvedAt)).orElseThrow();
        assertEquals(ReportStatus.RESOLVED, resolved.status()); assertEquals(resolvedAt, resolved.resolvedAt());
        assertEquals(resolved, repository.update(open.id(), item -> item.resolved(Instant.parse("2026-10-13T05:00:00Z"))).orElseThrow());
        assertEquals(1, repository.findByReporterId("reporter", 1, 20).totalItems());
        assertEquals(1, repository.findByCourierId("courier", 1, 20).totalItems());
        assertEquals(0, repository.findByCourierId("other", 1, 20).totalItems());
        assertTrue(repository.findById("a/b").isEmpty()); assertTrue(repository.findById(null).isEmpty());
        assertTrue(repository.update("missing", item -> item).isEmpty()); assertTrue(repository.update("a/b", item -> item).isEmpty());
    }

    @Test void persistsCourierResponseAndReadsLegacyOpenAsReadyForReview() throws Exception {
        Instant created = Instant.parse("2026-10-13T01:00:00Z");
        Instant deadline = created.plusSeconds(86_400);
        Attachment evidence = new Attachment("response-a", "reports/r/response/a", "handoff.webp",
                "image/webp", 4, created.plusSeconds(60));
        Report awaiting = new Report("response", "reporter", ReportCategory.COURIER, "description",
                "order", "courier", ReportStatus.AWAITING_COURIER_RESPONSE, created, deadline,
                null, null, null, List.of(), List.of());
        repository.save(awaiting);
        Report responded = repository.update("response", item -> item.respond("courier", "Intact at handoff",
                created.plusSeconds(60), List.of(evidence))).orElseThrow();
        assertEquals(responded, repository.findById("response").orElseThrow());
        assertEquals(List.of(evidence), responded.responseAttachments());

        var legacy = new HashMap<String, Object>();
        legacy.put("id", "legacy"); legacy.put("reporterId", "legacy-reporter");
        legacy.put("category", "GENERAL"); legacy.put("description", "old record");
        legacy.put("status", "OPEN"); legacy.put("createdAt", Timestamp.of(java.util.Date.from(created)));
        legacy.put("attachments", List.of());
        firestore.collection(FirestoreReportRepository.COLLECTION).document("legacy").set(legacy).get();

        Report migrated = repository.findById("legacy").orElseThrow();
        assertEquals(ReportStatus.READY_FOR_REVIEW, migrated.status());
        assertNull(migrated.responseDeadlineAt());
        assertTrue(migrated.responseAttachments().isEmpty());
    }

    private static Report report(String id, Instant created, List<Attachment> attachments) {
        return new Report(id, "reporter", ReportCategory.ORDER, "description", "order", "courier",
                ReportStatus.READY_FOR_REVIEW, created, null, null, null, null, attachments, List.of());
    }
}
