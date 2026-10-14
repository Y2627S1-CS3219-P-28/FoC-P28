/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Test generation and verification.
 * Scope: Generated automated verification for the team-specified Reporting Service contracts.
 * Author review: I reviewed for correctness
 */
package sg.edu.nus.foc.reporting.report;

import java.time.Instant;
import java.util.List;
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

    @Test void resolvesTransactionallyAndIdempotentlyAndHidesInvalidOrUnknownIds() {
        Report open = repository.save(report("open", Instant.parse("2026-10-13T01:00:00Z"), List.of()));
        Instant resolvedAt = Instant.parse("2026-10-13T04:00:00Z");
        Report resolved = repository.resolve(open.id(), resolvedAt).orElseThrow();
        assertEquals(ReportStatus.RESOLVED, resolved.status()); assertEquals(resolvedAt, resolved.resolvedAt());
        assertEquals(resolved, repository.resolve(open.id(), Instant.parse("2026-10-13T05:00:00Z")).orElseThrow());
        assertTrue(repository.findById("a/b").isEmpty()); assertTrue(repository.findById(null).isEmpty());
        assertTrue(repository.resolve("missing", resolvedAt).isEmpty()); assertTrue(repository.resolve("a/b", resolvedAt).isEmpty());
    }

    private static Report report(String id, Instant created, List<Attachment> attachments) {
        return new Report(id, "reporter", ReportCategory.ORDER, "description", "order", null, ReportStatus.OPEN, created, null, attachments);
    }
}
