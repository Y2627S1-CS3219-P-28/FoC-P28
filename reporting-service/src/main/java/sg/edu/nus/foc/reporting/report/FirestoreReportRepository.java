/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Implementation.
 * Scope: Generated Reporting Service code for the team-specified reporting, association, storage, security, and administration contracts.
 * Author review: I reviewed for correctness
 */
package sg.edu.nus.foc.reporting.report;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;
import sg.edu.nus.foc.reporting.error.DependencyUnavailableException;

@Repository
public class FirestoreReportRepository implements ReportRepository {
    static final String COLLECTION = "reports";
    private final Firestore firestore;

    public FirestoreReportRepository(Firestore firestore) { this.firestore = firestore; }

    @Override
    public Report save(Report report) {
        await(firestore.collection(COLLECTION).document(report.id()).set(toDocument(report)));
        return report;
    }

    @Override
    public Optional<Report> findById(String id) {
        if (!validId(id)) return Optional.empty();
        DocumentSnapshot snapshot = await(firestore.collection(COLLECTION).document(id).get());
        return snapshot.exists() ? Optional.of(fromDocument(snapshot)) : Optional.empty();
    }

    @Override
    public PageResult<Report> findAll(int page, int size) {
        List<Report> reports = await(firestore.collection(COLLECTION).get()).getDocuments().stream()
                .map(FirestoreReportRepository::fromDocument)
                .sorted(Comparator.comparing(Report::createdAt).reversed())
                .toList();
        int start = Math.min((page - 1) * size, reports.size());
        int end = Math.min(start + size, reports.size());
        int pages = reports.isEmpty() ? 0 : (reports.size() + size - 1) / size;
        return new PageResult<>(reports.subList(start, end), page, size, reports.size(), pages);
    }

    @Override
    public Optional<Report> resolve(String id, Instant resolvedAt) {
        if (!validId(id)) return Optional.empty();
        return await(firestore.runTransaction(transaction -> {
            var ref = firestore.collection(COLLECTION).document(id);
            DocumentSnapshot snapshot = transaction.get(ref).get();
            if (!snapshot.exists()) return Optional.empty();
            Report current = fromDocument(snapshot);
            Report resolved = current.resolved(resolvedAt);
            if (resolved != current) transaction.set(ref, toDocument(resolved));
            return Optional.of(resolved);
        }));
    }

    private static Map<String, Object> toDocument(Report report) {
        List<Map<String, Object>> attachments = report.attachments().stream().map(item -> Map.<String, Object>of(
                "id", item.id(), "objectKey", item.objectKey(), "filename", item.filename(),
                "contentType", item.contentType(), "size", item.size(),
                "uploadedAt", Timestamp.ofTimeSecondsAndNanos(item.uploadedAt().getEpochSecond(), item.uploadedAt().getNano()))).toList();
        var map = new java.util.HashMap<String, Object>();
        map.put("id", report.id()); map.put("reporterId", report.reporterId());
        map.put("category", report.category().name()); map.put("description", report.description());
        map.put("orderId", report.orderId()); map.put("courierId", report.courierId());
        map.put("status", report.status().name());
        map.put("createdAt", Timestamp.ofTimeSecondsAndNanos(report.createdAt().getEpochSecond(), report.createdAt().getNano()));
        map.put("resolvedAt", report.resolvedAt() == null ? null : Timestamp.ofTimeSecondsAndNanos(report.resolvedAt().getEpochSecond(), report.resolvedAt().getNano()));
        map.put("attachments", attachments);
        return map;
    }

    @SuppressWarnings("unchecked")
    static Report fromDocument(DocumentSnapshot snapshot) {
        List<Attachment> attachments = new ArrayList<>();
        Object raw = snapshot.get("attachments");
        if (raw instanceof List<?> list) for (Object entry : list) if (entry instanceof Map<?, ?> item) {
            Timestamp uploaded = (Timestamp) item.get("uploadedAt");
            attachments.add(new Attachment((String) item.get("id"), (String) item.get("objectKey"),
                    (String) item.get("filename"), (String) item.get("contentType"),
                    ((Number) item.get("size")).longValue(), uploaded.toDate().toInstant()));
        }
        Timestamp created = snapshot.getTimestamp("createdAt");
        Timestamp resolved = snapshot.getTimestamp("resolvedAt");
        return new Report(snapshot.getId(), snapshot.getString("reporterId"),
                ReportCategory.valueOf(snapshot.getString("category")), snapshot.getString("description"),
                snapshot.getString("orderId"), snapshot.getString("courierId"),
                ReportStatus.valueOf(snapshot.getString("status")), created.toDate().toInstant(),
                resolved == null ? null : resolved.toDate().toInstant(), attachments);
    }

    static boolean validId(String id) { return id != null && !id.isBlank() && !id.contains("/"); }
    private static <T> T await(ApiFuture<T> future) {
        try { return future.get(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new DependencyUnavailableException("Report storage is unavailable.", e); }
        catch (ExecutionException e) { throw new DependencyUnavailableException("Report storage is unavailable.", e.getCause()); }
    }
}
