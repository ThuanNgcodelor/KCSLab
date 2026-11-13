package com.example.app.intake.controller;

import com.example.app.intake.dto.CreateReceiptRequest;
import com.example.app.intake.dto.ReceiptAttachmentResponse;
import com.example.app.intake.dto.ReceiptResponse;
import com.example.app.intake.service.ReceiptService;
import com.example.app.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    public ReceiptAttachmentResponse attach(@PathVariable Long id, @RequestPart("file") MultipartFile file) { return receiptService.attach(id, file); }
}
