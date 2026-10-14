/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Implementation.
 * Scope: Generated Reporting Service code for the team-specified reporting, association, storage, security, and administration contracts.
 * Author review: I reviewed for correctness
 */
package sg.edu.nus.foc.reporting.report;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sg.edu.nus.foc.reporting.error.BadRequestException;
import sg.edu.nus.foc.reporting.error.DependencyUnavailableException;
import sg.edu.nus.foc.reporting.error.NotFoundException;
import sg.edu.nus.foc.reporting.order.AssociatedOrder;
import sg.edu.nus.foc.reporting.order.OrderAssociationClient;
import sg.edu.nus.foc.reporting.storage.AttachmentStorage;
import sg.edu.nus.foc.reporting.storage.StoredObject;

@Service
public class ReportService {
    private final ReportRepository repository;
    private final AttachmentStorage storage;
    private final OrderAssociationClient orders;
    private final ImageValidator images;
    private final Clock clock;

    public ReportService(ReportRepository repository, AttachmentStorage storage, OrderAssociationClient orders,
                         ImageValidator images, Clock clock) {
        this.repository = repository; this.storage = storage; this.orders = orders; this.images = images; this.clock = clock;
    }

    public Report submit(String reporterId, String bearerToken, ReportCategory category, String description,
                         String requestedOrderId, List<MultipartFile> photos) {
        String orderId = normalize(requestedOrderId);
        if (category.requiresOrder() && orderId == null) throw new BadRequestException("An order is required for this category.", "orderId");
        List<ImageValidator.ValidatedImage> validated = images.validate(photos);
        AssociatedOrder associated = orderId == null ? null : orders.find(orderId, bearerToken);
        String reportId = UUID.randomUUID().toString();
        var attachments = new ArrayList<Attachment>();
        var uploadAttempts = new ArrayList<String>();
        try {
            for (var image : validated) {
                String attachmentId = UUID.randomUUID().toString();
                String key = "reports/" + reportId + "/" + attachmentId;
                uploadAttempts.add(key);
                storage.put(key, image.contentType(), image.bytes());
                attachments.add(new Attachment(attachmentId, key, image.filename(), image.contentType(), image.bytes().length, clock.instant()));
            }
            Report report = new Report(reportId, reporterId, category, description.strip(),
                    associated == null ? null : associated.id(), associated == null ? null : associated.courierId(),
                    ReportStatus.OPEN, clock.instant(), null, attachments);
            return repository.save(report);
        } catch (RuntimeException failure) {
            uploadAttempts.forEach(key -> {
                try { storage.delete(key); }
                catch (RuntimeException ignored) { /* rollback is best-effort; preserve the original failure */ }
            });
            if (failure instanceof BadRequestException || failure instanceof NotFoundException || failure instanceof DependencyUnavailableException) throw failure;
            throw new DependencyUnavailableException("The report could not be saved.", failure);
        }
    }

    public PageResult<Report> list(int page, int size) { return repository.findAll(page, size); }
    public Report resolve(String id) { return repository.resolve(id, clock.instant()).orElseThrow(() -> new NotFoundException("Report not found.")); }
    public AttachmentDownload attachment(String reportId, String attachmentId) {
        Report report = repository.findById(reportId).orElseThrow(() -> new NotFoundException("Attachment not found."));
        Attachment metadata = report.attachments().stream().filter(item -> item.id().equals(attachmentId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Attachment not found."));
        StoredObject object = storage.get(metadata.objectKey());
        return new AttachmentDownload(metadata, object.bytes());
    }
    private static String normalize(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    public record AttachmentDownload(Attachment metadata, byte[] bytes) { public AttachmentDownload { bytes = bytes.clone(); } }
}
