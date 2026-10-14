/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Test generation and verification.
 * Scope: Generated automated verification for the team-specified Reporting Service contracts.
 * Author review: I reviewed for correctness
 */
package sg.edu.nus.foc.reporting.report;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import sg.edu.nus.foc.reporting.error.BadRequestException;
import sg.edu.nus.foc.reporting.error.DependencyUnavailableException;
import sg.edu.nus.foc.reporting.error.NotFoundException;
import sg.edu.nus.foc.reporting.order.AssociatedOrder;
import sg.edu.nus.foc.reporting.order.OrderAssociationClient;
import sg.edu.nus.foc.reporting.storage.AttachmentStorage;
import sg.edu.nus.foc.reporting.storage.StoredObject;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-13T10:00:00Z");
    private final MemoryRepository repository = new MemoryRepository();
    private final MemoryStorage storage = new MemoryStorage();
    private final RecordingOrders orders = new RecordingOrders();
    private ReportService service;

    @BeforeEach void setUp() { service = new ReportService(repository, storage, orders, new ImageValidator(), Clock.fixed(NOW, ZoneOffset.UTC)); }

    @Test void supportsAllCategoriesConditionalOrdersNullableCourierAndMultipleReports() {
        for (ReportCategory category : ReportCategory.values()) {
            String requested = category.requiresOrder() ? "alias" : null;
            Report report = service.submit("reporter", "Bearer token", category, " details ", requested, List.of());
            assertEquals(category, report.category()); assertEquals("details", report.description());
            assertEquals(requested == null ? null : "authoritative", report.orderId());
            assertNull(report.courierId()); assertEquals(ReportStatus.OPEN, report.status());
        }
        Report another = service.submit("reporter", "Bearer token", ReportCategory.CREDITS, "again", "alias", List.of());
        assertEquals("authoritative", another.orderId());
        assertEquals(5, repository.values.size());
        assertEquals(List.of("alias", "alias", "alias"), orders.ids);
        assertTrue(orders.tokens.stream().allMatch("Bearer token"::equals));
    }

    @Test void requiresOrdersOnlyForOrderAndCourier() {
        assertThrows(BadRequestException.class, () -> service.submit("u", "Bearer x", ReportCategory.ORDER, "x", null, List.of()));
        assertThrows(BadRequestException.class, () -> service.submit("u", "Bearer x", ReportCategory.COURIER, "x", " ", List.of()));
        assertDoesNotThrow(() -> service.submit("u", "Bearer x", ReportCategory.GENERAL, "x", null, null));
    }

    @Test void storesPhotoMetadataWithoutUserObjectNamesAndRollsBackPartialUploads() {
        Report report = service.submit("u", "Bearer x", ReportCategory.GENERAL, "x", null, List.of(png("../../unsafe?.png")));
        Attachment attachment = report.attachments().getFirst();
        assertTrue(attachment.objectKey().matches("reports/[^/]+/[^/]+"));
        assertFalse(attachment.objectKey().contains("unsafe")); assertEquals("unsafe_.png", attachment.filename());

        storage.failAt = storage.puts + 2;
        assertThrows(DependencyUnavailableException.class, () -> service.submit("u", "Bearer x", ReportCategory.GENERAL, "x", null, List.of(png("a"), png("b"))));
        assertEquals(2, storage.deleted.size());
    }

    @Test void cleansUploadsWhenFirestoreFails() {
        repository.failSave = true;
        assertThrows(DependencyUnavailableException.class, () -> service.submit("u", "Bearer x", ReportCategory.GENERAL, "x", null, List.of(png("a"))));
        assertEquals(1, storage.deleted.size());
    }

    @Test void listsNewestResolvesIdempotentlyRetainsAttachmentsAndRejectsCrossReportAccess() {
        Report report = service.submit("u", "Bearer x", ReportCategory.GENERAL, "x", null, List.of(png("a.png")));
        assertEquals(1, service.list(1, 20).totalItems());
        Report first = service.resolve(report.id()); Report second = service.resolve(report.id());
        assertEquals(ReportStatus.RESOLVED, first.status()); assertEquals(first.resolvedAt(), second.resolvedAt());
        assertEquals(1, second.attachments().size()); assertFalse(storage.objects.isEmpty());
        assertArrayEquals(pngBytes(), service.attachment(report.id(), report.attachments().getFirst().id()).bytes());
        assertThrows(NotFoundException.class, () -> service.attachment(report.id(), "another-report-attachment"));
        assertThrows(NotFoundException.class, () -> service.resolve("missing"));
    }

    private static MockMultipartFile png(String name) { return new MockMultipartFile("photos", name, "image/png", pngBytes()); }
    private static byte[] pngBytes() { return new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}; }

    static class RecordingOrders implements OrderAssociationClient {
        final List<String> ids = new ArrayList<>(); final List<String> tokens = new ArrayList<>();
        @Override public AssociatedOrder find(String id, String token) { ids.add(id); tokens.add(token); return new AssociatedOrder("authoritative", null); }
    }
    static class MemoryStorage implements AttachmentStorage {
        final Map<String, StoredObject> objects = new LinkedHashMap<>(); final List<String> deleted = new ArrayList<>(); int puts; int failAt = -1;
        @Override public void put(String key, String type, byte[] bytes) { puts++; if (puts == failAt) throw new DependencyUnavailableException("down"); objects.put(key, new StoredObject(bytes, type)); }
        @Override public StoredObject get(String key) { return Optional.ofNullable(objects.get(key)).orElseThrow(() -> new NotFoundException("missing")); }
        @Override public void delete(String key) { deleted.add(key); objects.remove(key); }
    }
    static class MemoryRepository implements ReportRepository {
        final Map<String, Report> values = new LinkedHashMap<>(); boolean failSave;
        @Override public Report save(Report report) { if (failSave) throw new RuntimeException("firestore"); values.put(report.id(), report); return report; }
        @Override public Optional<Report> findById(String id) { return Optional.ofNullable(values.get(id)); }
        @Override public PageResult<Report> findAll(int page, int size) { return new PageResult<>(List.copyOf(values.values()), page, size, values.size(), values.isEmpty() ? 0 : 1); }
        @Override public Optional<Report> resolve(String id, Instant at) { return Optional.ofNullable(values.computeIfPresent(id, (key, report) -> report.resolved(at))); }
    }
}
