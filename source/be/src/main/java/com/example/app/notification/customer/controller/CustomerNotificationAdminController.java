package com.example.app.notification.customer.controller;

import com.example.app.notification.customer.service.CustomerNotificationOperations;
import com.example.app.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/customer-notifications")
@PreAuthorize("hasRole('ADMIN')")
public class CustomerNotificationAdminController {
    private final CustomerNotificationOperations operations;
    private final UserService users;
    public CustomerNotificationAdminController(CustomerNotificationOperations operations, UserService users) { this.operations = operations; this.users = users; }

    @GetMapping
    public ResponseEntity<CustomerNotificationOperations.Overview> overview() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(operations.overview());
    }
    @GetMapping("/deliveries")
    public ResponseEntity<CustomerNotificationOperations.QueuePage> list(@RequestParam(required=false) String status,
            @RequestParam(required=false) String q, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(operations.queue(status, q, page, size));
    }

    public record PauseRequest(boolean paused, @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,64}") String requestId, @NotBlank @Size(max=255) String reason) {}
    @PostMapping("/pause")
    public ResponseEntity<Void> pause(@Valid @RequestBody PauseRequest request, @AuthenticationPrincipal Jwt jwt) {
        operations.pause(request.paused(), request.requestId(), request.reason(), users.getByEmail(jwt.getSubject()).getId());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    public record TokenRequest(@NotBlank @Size(max=16000) String accessToken, @NotBlank @Size(max=16000) String refreshToken,
                               @Min(1) @Max(604800) long expiresIn,
                               @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,64}") String requestId, @NotBlank @Size(max=255) String reason) {
        @Override public String toString() { return "TokenRequest[redacted]"; }
    }
    @PostMapping("/token")
    public ResponseEntity<Void> token(@Valid @RequestBody TokenRequest request, @AuthenticationPrincipal Jwt jwt) {
        operations.provision(request.accessToken(), request.refreshToken(), request.expiresIn(), request.requestId(), request.reason(), users.getByEmail(jwt.getSubject()).getId());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
