package com.example.app.dossier.controller;

import com.example.app.dossier.storage.DossierStorageService;
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
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class StorageAdminController {
    private final DossierStorageService storage;
    private final UserService users;
    public StorageAdminController(DossierStorageService storage, UserService users) { this.storage=storage; this.users=users; }
    public record Register(@NotBlank @Size(max=120) String label, @NotBlank @Size(max=700) String path) {}
    public record Target(@NotNull @Positive Long locationId, @Min(0) long version) {}
    @GetMapping("/storage-locations")
    public ResponseEntity<DossierStorageService.Overview> list() { return response(storage.overview()); }
    @PostMapping("/storage-locations")
    public ResponseEntity<DossierStorageService.Overview> register(@Valid @RequestBody Register r, @AuthenticationPrincipal Jwt jwt) {
        return response(storage.register(r.label(),r.path(),users.getByEmail(jwt.getSubject()).getId()));
    }
    @PostMapping("/storage-locations/{id}/validate")
    public ResponseEntity<DossierStorageService.Overview> validate(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return response(storage.validate(id,users.getByEmail(jwt.getSubject()).getId()));
    }
    @PutMapping("/storage-write-target")
    public ResponseEntity<DossierStorageService.Overview> activate(@Valid @RequestBody Target r, @AuthenticationPrincipal Jwt jwt) {
        return response(storage.activate(r.locationId(),r.version(),users.getByEmail(jwt.getSubject()).getId()));
    }
    private ResponseEntity<DossierStorageService.Overview> response(DossierStorageService.Overview value) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);
    }
}
