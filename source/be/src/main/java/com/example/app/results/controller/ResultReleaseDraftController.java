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

    public ResultReleaseDraftController(ResultReleaseDraftService drafts, ResultReleaseService workflow, UserService users) {
        this.drafts = drafts; this.workflow = workflow; this.users = users;
    }

    public record CreateDraftRequest(@NotNull Long testingRecordId) {}
    public record ReasonRequest(@NotBlank @Size(max = 500) String reason) {}
    public record PublishRequest(Set<Long> publicFileIds) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResultReleaseDraftResponse create(@Valid @RequestBody CreateDraftRequest request, @AuthenticationPrincipal Jwt jwt) {
        return drafts.create(request.testingRecordId(), users.getByEmail(jwt.getSubject()));
    }

    @GetMapping
    public PagedResponse<ResultReleaseListItem> list(@RequestParam(required = false) ResultReleaseStatus status,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return workflow.list(status, page, size);
    }

    @GetMapping("/{id}")
    public ResultReleaseDetailResponse find(@PathVariable Long id) { return workflow.detail(id); }

    @GetMapping("/{id}/events")
    public List<ResultReleaseDetailResponse.EventResponse> events(@PathVariable Long id) { return workflow.history(id); }

    @PostMapping(value = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultReleaseDetailResponse upload(@PathVariable Long id, @RequestPart("files") List<MultipartFile> files,
                                              @AuthenticationPrincipal Jwt jwt) {
        return workflow.addFiles(id, files, users.getByEmail(jwt.getSubject()));
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
