package com.example.app.notification.customer.controller;

import com.example.app.notification.customer.service.IntakeCustomerNotificationService;
import com.example.app.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/receipts")
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
public class CustomerNotificationController {
    private final IntakeCustomerNotificationService service;
    private final UserService users;

    public CustomerNotificationController(IntakeCustomerNotificationService service, UserService users) {
        this.service = service; this.users = users;
    }

    @GetMapping("/notification-settings")
    public ResponseEntity<IntakeCustomerNotificationService.Settings> settings() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.settings());
    }

    @GetMapping("/{id}/notification-deliveries")
    public ResponseEntity<IntakeCustomerNotificationService.DeliveryPage> list(@PathVariable long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.list(id, page, size, users.getByEmail(jwt.getSubject())));
    }

    public record RetryRequest(@NotBlank @Size(max = 255) String reason,
                               @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String requestId) {}

    @PostMapping("/{id}/notification-deliveries/{deliveryId}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IntakeCustomerNotificationService.Delivery> retry(@PathVariable long id, @PathVariable long deliveryId,
            @Valid @RequestBody RetryRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore())
                .body(service.retry(id, deliveryId, request.requestId(), request.reason(), users.getByEmail(jwt.getSubject())));
    }
}
