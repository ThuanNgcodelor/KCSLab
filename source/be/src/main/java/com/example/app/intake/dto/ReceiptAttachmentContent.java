package com.example.app.intake.dto;

import org.springframework.core.io.Resource;

public record ReceiptAttachmentContent(Resource resource, String contentType, String originalName) {}
