package com.example.app.intake.controller;

import com.example.app.intake.dto.IntakeDashboardResponse;
import com.example.app.intake.service.IntakeDashboardService;
import com.example.app.user.entity.User;
import com.example.app.user.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/intake/dashboard")
public class IntakeDashboardController {
    private final IntakeDashboardService dashboardService;
    private final UserService userService;

    public IntakeDashboardController(IntakeDashboardService dashboardService, UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    @GetMapping("/receptionist")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public IntakeDashboardResponse receptionist(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal Jwt jwt) {
        User viewer = userService.getByEmail(jwt.getSubject());
        return dashboardService.summarize(from, to, viewer.getId());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public IntakeDashboardResponse admin(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long receivedBy) {
        return dashboardService.summarize(from, to, receivedBy);
    }
}
