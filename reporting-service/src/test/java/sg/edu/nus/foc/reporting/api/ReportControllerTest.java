/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Test generation and verification.
 * Scope: Generated automated verification for the team-specified Reporting Service contracts.
 * Author review: I reviewed for correctness
 */
package sg.edu.nus.foc.reporting.api;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import sg.edu.nus.foc.reporting.error.BadRequestException;
import sg.edu.nus.foc.reporting.report.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReportControllerTest {
    private final ReportService service = mock(ReportService.class);
    private final ReportController controller = new ReportController(service);
    private final Report report = new Report("r", "u", ReportCategory.GENERAL, "description", null, null,
            ReportStatus.OPEN, Instant.parse("2026-10-13T00:00:00Z"), null,
            List.of(new Attachment("a", "private", "photo.png", "image/png", 3, Instant.parse("2026-10-13T00:00:00Z"))));

    @Test void createsListsAndResolvesReports() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("u").build();
        when(service.submit(eq("u"), eq("Bearer token"), any(), any(), isNull(), isNull())).thenReturn(report);
        var created = controller.create(new CreateReportRequest(ReportCategory.GENERAL, "description", null), null, jwt);
        assertEquals(HttpStatus.CREATED, created.getStatusCode()); assertEquals("r", created.getBody().id());

        when(service.list(1, 20)).thenReturn(new PageResult<>(List.of(report), 1, 20, 1, 1));
        assertEquals(1, controller.list(1, 20).items().size());
        when(service.resolve("r")).thenReturn(report.resolved(Instant.parse("2026-10-13T01:00:00Z")));
        assertEquals(ReportStatus.RESOLVED, controller.resolve("r", new UpdateReportStatusRequest(ReportStatus.RESOLVED)).status());
    }

    @Test void streamsPrivateAttachmentsAndRejectsReopening() {
        when(service.attachment("r", "a")).thenReturn(new ReportService.AttachmentDownload(report.attachments().getFirst(), new byte[]{1, 2, 3}));
        var response = controller.attachment("r", "a");
        assertArrayEquals(new byte[]{1, 2, 3}, response.getBody());
        assertTrue(response.getHeaders().getCacheControl().contains("no-store"));
        assertTrue(response.getHeaders().getCacheControl().contains("private"));
        assertEquals("no-cache", response.getHeaders().getPragma());
        assertThrows(BadRequestException.class, () -> controller.resolve("r", new UpdateReportStatusRequest(ReportStatus.OPEN)));
    }
}
