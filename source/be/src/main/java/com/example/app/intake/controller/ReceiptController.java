package com.example.app.intake.controller;

import com.example.app.intake.dto.CreateReceiptRequest;
import com.example.app.intake.dto.ReceiptAttachmentContent;
import com.example.app.intake.dto.ReceiptAttachmentResponse;
import com.example.app.intake.dto.ReceiptResponse;
import com.example.app.intake.entity.ReceiptAttachmentCategory;
import com.example.app.intake.service.ReceiptService;
import com.example.app.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.time.Instant;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/receipts")
public class ReceiptController {
    private final ReceiptService receiptService;
    private final UserService userService;
    public ReceiptController(ReceiptService receiptService, UserService userService) { this.receiptService = receiptService; this.userService = userService; }
    @GetMapping public List<ReceiptResponse> list() { return receiptService.findAll(); }
    @GetMapping("/{id}") public ReceiptResponse get(@PathVariable Long id) { return receiptService.findById(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ReceiptResponse create(@Valid @RequestBody CreateReceiptRequest request, @AuthenticationPrincipal Jwt jwt) { return receiptService.create(request, userService.getByEmail(jwt.getSubject())); }
    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReceiptAttachmentResponse attach(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "OTHER") ReceiptAttachmentCategory category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant capturedAt,
            @AuthenticationPrincipal Jwt jwt) {
        return receiptService.attach(id, file, category, description, capturedAt,
                userService.getByEmail(jwt.getSubject()));
    }

    @GetMapping("/{receiptId}/attachments/{attachmentId}/content")
    public ResponseEntity<Resource> attachmentContent(@PathVariable Long receiptId,
                                                      @PathVariable Long attachmentId) {
        ReceiptAttachmentContent content = receiptService.loadAttachment(receiptId, attachmentId);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(content.originalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content.resource());
    }
}
