/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Implementation.
 * Scope: Generated Reporting Service code for the team-specified reporting, association, storage, security, and administration contracts.
 * Author review: I reviewed for correctness
 */
package sg.edu.nus.foc.reporting.api;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import sg.edu.nus.foc.reporting.error.BadRequestException;
import sg.edu.nus.foc.reporting.report.ReportService;
import sg.edu.nus.foc.reporting.report.ReportStatus;

@Validated
@RestController
@RequestMapping("/api/reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {
    private final ReportService reports;
    public ReportController(ReportService reports) { this.reports = reports; }

    @Operation(summary = "Submit a report with up to five photos")
    @ApiResponse(responseCode = "201", description = "Report created")
    @ApiResponse(responseCode = "400", description = "Invalid report or photos")
    @ApiResponse(responseCode = "404", description = "Order association unavailable")
    @ApiResponse(responseCode = "503", description = "Order or storage dependency unavailable")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ReportResponse> create(@Valid @RequestPart("report") CreateReportRequest request,
                                          @RequestPart(value = "photos", required = false) List<MultipartFile> photos,
                                          @AuthenticationPrincipal Jwt jwt) {
        String bearer = "Bearer " + jwt.getTokenValue();
        return ResponseEntity.status(HttpStatus.CREATED).body(ReportResponse.from(reports.submit(jwt.getSubject(), bearer,
                request.category(), request.description(), request.orderId(), photos)));
    }

    @Operation(summary = "List all reports newest first")
    @ApiResponse(responseCode = "200", description = "Reports page")
    @PreAuthorize("@callerRoles.isAdmin(authentication)")
    @GetMapping
    PageResponse<ReportResponse> list(@Parameter(description = "1-based page") @RequestParam(defaultValue = "1") @Min(1) int page,
                                      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = reports.list(page, size);
        return new PageResponse<>(result.items().stream().map(ReportResponse::from).toList(), result.page(), result.size(), result.totalItems(), result.totalPages());
    }

    @Operation(summary = "Download a report attachment")
    @ApiResponse(responseCode = "200", description = "Private attachment bytes", content = @Content(schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "404", description = "Report or attachment not found")
    @PreAuthorize("@callerRoles.isAdmin(authentication)")
    @GetMapping("/{reportId}/attachments/{attachmentId}")
    ResponseEntity<byte[]> attachment(@PathVariable String reportId, @PathVariable String attachmentId) {
        var download = reports.attachment(reportId, attachmentId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(download.metadata().filename()).build().toString())
                .contentType(MediaType.parseMediaType(download.metadata().contentType())).contentLength(download.bytes().length).body(download.bytes());
    }

    @Operation(summary = "Resolve a report")
    @ApiResponse(responseCode = "200", description = "Resolved report")
    @ApiResponse(responseCode = "404", description = "Report not found")
    @PreAuthorize("@callerRoles.isAdmin(authentication)")
    @PatchMapping("/{id}/status")
    ReportResponse resolve(@PathVariable String id, @Valid @org.springframework.web.bind.annotation.RequestBody UpdateReportStatusRequest request) {
        if (request.status() != ReportStatus.RESOLVED) throw new BadRequestException("Status must be RESOLVED.", "status");
        return ReportResponse.from(reports.resolve(id));
    }
}
