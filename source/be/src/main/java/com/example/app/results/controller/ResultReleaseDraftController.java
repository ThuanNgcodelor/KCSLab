package com.example.app.results.controller;

import com.example.app.common.dto.PagedResponse;
import com.example.app.results.dto.ResultReleaseDetailResponse;
import com.example.app.results.dto.ResultReleaseDraftResponse;
import com.example.app.results.dto.ResultReleaseListItem;
import com.example.app.results.entity.ResultReleaseStatus;
import com.example.app.results.service.ResultReleaseDraftService;
import com.example.app.results.service.ResultReleaseService;
import com.example.app.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/** Role access is enforced in SecurityConfig and again inside {@link ResultReleaseService}. */
@RestController
@RequestMapping("/api/v1/result-releases")
public class ResultReleaseDraftController {
    private final ResultReleaseDraftService drafts;
    private final ResultReleaseService workflow;
    private final UserService users;
    private final com.example.app.results.service.ResultReportFileService reportFiles;
    private final com.example.app.results.service.ResultEntryService entries;
    private final com.example.app.results.service.ResultReportRefreshService refresh;

    public ResultReleaseDraftController(ResultReleaseDraftService drafts, ResultReleaseService workflow, UserService users,
                                      com.example.app.results.service.ResultReportFileService reportFiles,
                                      com.example.app.results.service.ResultEntryService entries,
                                      com.example.app.results.service.ResultReportRefreshService refresh) {
        this.drafts = drafts; this.workflow = workflow; this.users = users; this.reportFiles = reportFiles; this.entries = entries;
        this.refresh = refresh;
    }

    public record CreateDraftRequest(@NotNull Long testingRecordId) {}
    public record ReasonRequest(@NotBlank @Size(max = 500) String reason) {}
    public record PublishRequest(Set<Long> publicFileIds) {}
    public record ReportRow(@Size(max = 200) String parameter, @Size(max = 60) String unit, @Size(max = 200) String method,
                            @Size(max = 120) String result, @Size(max = 255) String note) {}
    public record ReportRequest(@jakarta.validation.constraints.NotEmpty @Size(max = 40) List<@Valid ReportRow> rows,
                                @Size(max = 120) String qualityHeadName, @Size(max = 120) String directorName) {}

    public record EntryRequest(@NotNull Long transferItemId, java.time.LocalDate testingDate,
                               @jakarta.validation.constraints.NotEmpty @Size(max = 40) List<@Valid ReportRow> rows,
                               boolean submit) {}

    /** Sample block of the KQKN report for a received sample (auto-filled from Phiếu 3/4). */
    @GetMapping("/report-header")
    public ResultReleaseService.ReportHeader reportHeader(@RequestParam Long transferItemId) {
        return workflow.header(transferItemId);
    }

    /** Result-entry flow: opens the testing record and its KQKN dossier with the typed rows in one step. */
    @PostMapping("/from-entry")
    @ResponseStatus(HttpStatus.CREATED)
    public ResultReleaseDetailResponse createFromEntry(@Valid @RequestBody EntryRequest request, @AuthenticationPrincipal Jwt jwt) {
        return entries.create(request.transferItemId(), request.testingDate(), request.rows().stream()
                .map(row -> new ResultReleaseService.ReportRowInput(row.parameter(), row.unit(), row.method(), row.result(), row.note()))
                .toList(), request.submit(), users.getByEmail(jwt.getSubject()));
    }

    @GetMapping("/{id}/sample-photos/{attachmentId}")
    public ResponseEntity<Resource> samplePhoto(@PathVariable Long id, @PathVariable Long attachmentId) {
        var stored = workflow.samplePhoto(id, attachmentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stored.contentType()))
                .contentLength(stored.size())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=600")
                .body(stored.resource());
    }

    @GetMapping("/{id}/report-docx")
    public ResponseEntity<byte[]> reportDocx(@PathVariable Long id) {
        var file = workflow.reportDocx(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff").header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(file.content());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResultReleaseDraftResponse create(@Valid @RequestBody CreateDraftRequest request, @AuthenticationPrincipal Jwt jwt) {
        return drafts.create(request.testingRecordId(), users.getByEmail(jwt.getSubject()));
    }

    @GetMapping
    public PagedResponse<ResultReleaseListItem> list(@RequestParam(required = false) ResultReleaseStatus status,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to,
            @RequestParam(defaultValue = "updatedAt,desc") String sort,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return workflow.list(status, query, from, to, page, size, sort);
    }

    @GetMapping("/ready")
    public List<ResultReleaseService.ReadyRecord> ready() { return workflow.readyRecords(); }

    @GetMapping("/{id}")
    public ResultReleaseDetailResponse find(@PathVariable Long id) { return workflow.detail(id); }

    @GetMapping("/{id}/events")
    public List<ResultReleaseDetailResponse.EventResponse> events(@PathVariable Long id) { return workflow.history(id); }

    @PutMapping("/{id}/report")
    public ResultReleaseDetailResponse saveReport(@PathVariable Long id, @Valid @RequestBody ReportRequest request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        return workflow.updateReport(id, request.rows().stream().map(row -> new ResultReleaseService.ReportRowInput(
                row.parameter(), row.unit(), row.method(), row.result(), row.note())).toList(),
                request.qualityHeadName(), request.directorName(), users.getByEmail(jwt.getSubject()));
    }

    /** Watermarked draft of the KQKN PDF so the dossier can be checked before submission. */
    @GetMapping("/{id}/report-preview")
    public ResponseEntity<byte[]> preview(@PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("kqkn-xem-truoc.pdf").build().toString())
                .header("X-Content-Type-Options", "nosniff").header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(workflow.previewPdf(id));
    }

    @PostMapping(value = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultReleaseDetailResponse upload(@PathVariable Long id, @RequestPart("files") List<MultipartFile> files,
                                              @AuthenticationPrincipal Jwt jwt) {
        return workflow.addFiles(id, files, users.getByEmail(jwt.getSubject()));
    }

    /** The KQKN report as one DOCX (converted to PDF on the server) or PDF file. Replaces any earlier report file. */
    @PostMapping(value = "/{id}/report-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultReleaseDetailResponse uploadReportFile(@PathVariable Long id, @RequestPart("file") MultipartFile file,
                                                        @AuthenticationPrincipal Jwt jwt) {
        return reportFiles.upload(id, file, users.getByEmail(jwt.getSubject()));
    }

    @DeleteMapping("/{id}/report-file")
    public ResultReleaseDetailResponse removeReportFile(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return workflow.removeReport(id, users.getByEmail(jwt.getSubject()));
    }

    @DeleteMapping("/{id}/files/{fileId}")
    public ResultReleaseDetailResponse removeFile(@PathVariable Long id, @PathVariable Long fileId,
                                                  @AuthenticationPrincipal Jwt jwt) {
        return workflow.removeFile(id, fileId, users.getByEmail(jwt.getSubject()));
    }

    @GetMapping("/{id}/files/{fileId}/content")
    public ResponseEntity<Resource> download(@PathVariable Long id, @PathVariable Long fileId) {
        var stored = workflow.loadFile(id, fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stored.contentType()))
                .contentLength(stored.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(stored.originalName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(stored.resource());
    }

    @PostMapping("/{id}/submit")
    public ResultReleaseDetailResponse submit(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return workflow.submit(id, users.getByEmail(jwt.getSubject()));
    }

    @PostMapping("/{id}/return")
    public ResultReleaseDetailResponse giveBack(@PathVariable Long id, @Valid @RequestBody ReasonRequest request,
                                                @AuthenticationPrincipal Jwt jwt) {
        return workflow.giveBack(id, request.reason(), users.getByEmail(jwt.getSubject()));
    }

    @PostMapping("/{id}/publish")
    public ResultReleaseDetailResponse publish(@PathVariable Long id, @RequestBody(required = false) PublishRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        return workflow.publish(id, request == null ? null : request.publicFileIds(), users.getByEmail(jwt.getSubject()));
    }

    /** Address printed in the QR of every KQKN (PUBLIC_LOOKUP_URL) and how many reports are published. */
    @GetMapping("/lookup-settings")
    public com.example.app.results.service.ResultReportRefreshService.Settings lookupSettings() { return refresh.settings(); }

    /** Rebuilds the PDF of every published report with the current lookup address (after moving to a domain). */
    @PostMapping("/regenerate-reports")
    public com.example.app.results.service.ResultReportRefreshService.Result regenerateAll(@AuthenticationPrincipal Jwt jwt) {
        return refresh.regenerateAll(users.getByEmail(jwt.getSubject()));
    }

    @PostMapping("/{id}/regenerate-report")
    public ResultReleaseDetailResponse regenerateReport(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return workflow.regenerateReport(id, users.getByEmail(jwt.getSubject()));
    }

    @PostMapping("/{id}/revoke")
    public ResultReleaseDetailResponse revoke(@PathVariable Long id, @Valid @RequestBody ReasonRequest request,
                                              @AuthenticationPrincipal Jwt jwt) {
        return workflow.revoke(id, request.reason(), users.getByEmail(jwt.getSubject()));
    }

    @PostMapping("/{id}/revisions")
    @ResponseStatus(HttpStatus.CREATED)
    public ResultReleaseDetailResponse revise(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return workflow.revise(id, users.getByEmail(jwt.getSubject()));
    }
}
