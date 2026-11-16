package com.example.app.transfer.receiving;

import com.example.app.intake.dto.ReceiptAttachmentContent;
import com.example.app.common.dto.PagedResponse;
import com.example.app.transfer.dto.TransferDecisionRequest;
import com.example.app.transfer.entity.TransferStatus;
import com.example.app.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/receiving")
@PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
public class ReceivingController {
    private final ReceivingService receiving;
    private final UserService users;

    public ReceivingController(ReceivingService receiving, UserService users) {
        this.receiving = receiving;
        this.users = users;
    }

    @GetMapping("/dashboard")
    public ReceivingService.Dashboard dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal Jwt jwt) {
        return receiving.dashboard(users.getByEmail(jwt.getSubject()), from, to);
    }

    @GetMapping("/transfers")
    public PagedResponse<ReceivingTransferResponse> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String sort, @AuthenticationPrincipal Jwt jwt) {
        return receiving.list(users.getByEmail(jwt.getSubject()), page, size, status, q, from, to, sort);
    }

    @GetMapping("/transfers/{id}")
    public ReceivingTransferResponse detail(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return receiving.detail(id, users.getByEmail(jwt.getSubject()));
    }

    @GetMapping("/transfers/{id}/evidence")
    public List<ReceivingEvidenceResponse> evidence(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return receiving.evidence(id, users.getByEmail(jwt.getSubject()));
    }

    @GetMapping("/transfers/{id}/evidence/{attachmentId}/content")
    public ResponseEntity<Resource> content(@PathVariable Long id, @PathVariable Long attachmentId,
                                            @AuthenticationPrincipal Jwt jwt) {
        ReceiptAttachmentContent attachment = receiving.content(id, attachmentId, users.getByEmail(jwt.getSubject()));
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(attachment.originalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(attachment.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(attachment.resource());
    }

    @PostMapping("/transfers/{id}/decision")
    public ReceivingTransferResponse decide(@PathVariable Long id, @Valid @RequestBody TransferDecisionRequest request,
                                            @AuthenticationPrincipal Jwt jwt) {
        return receiving.decide(id, request, users.getByEmail(jwt.getSubject()));
    }
}
