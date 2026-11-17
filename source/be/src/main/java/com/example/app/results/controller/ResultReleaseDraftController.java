package com.example.app.results.controller;

import com.example.app.results.dto.ResultReleaseDraftResponse;
import com.example.app.results.service.ResultReleaseDraftService;
import com.example.app.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/result-releases")
public class ResultReleaseDraftController {
    private final ResultReleaseDraftService drafts;
    private final UserService users;
    public ResultReleaseDraftController(ResultReleaseDraftService drafts, UserService users) { this.drafts = drafts; this.users = users; }
    public record CreateDraftRequest(@NotNull Long testingRecordId) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResultReleaseDraftResponse create(@Valid @RequestBody CreateDraftRequest request, @AuthenticationPrincipal Jwt jwt) {
        return drafts.create(request.testingRecordId(), users.getByEmail(jwt.getSubject()));
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    public ResultReleaseDraftResponse find(@PathVariable Long id) { return drafts.find(id); }
}
