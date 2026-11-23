package com.example.app.notification.customer.controller;

import com.example.app.notification.customer.service.ZaloDeliveryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
public class ZaloWebhookController {
    private final ZaloDeliveryService service;
    public ZaloWebhookController(ZaloDeliveryService service) { this.service = service; }

    @PostMapping(value = "/api/v1/webhooks/zbs", consumes = "application/json")
    public ResponseEntity<Void> receive(HttpServletRequest request,
            @RequestHeader(value = "X-ZEvent-Signature", required = false) String signature,
            @RequestHeader(value = "X-ZEvent-Server", required = false) String server) throws IOException {
        byte[] raw = request.getInputStream().readNBytes(16385);
        if (raw.length > 16384) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        service.receive(raw, signature, server);
        return ResponseEntity.ok().header("Cache-Control", "no-store").build();
    }
}
