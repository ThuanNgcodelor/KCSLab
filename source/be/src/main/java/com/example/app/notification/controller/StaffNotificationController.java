package com.example.app.notification.controller;

import com.example.app.notification.dto.StaffNotificationPageResponse;
import com.example.app.notification.dto.StaffNotificationResponse;
import com.example.app.notification.service.StaffNotificationService;
import com.example.app.user.entity.User;
import com.example.app.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class StaffNotificationController {
    private final StaffNotificationService notificationService;
    private final UserService userService;

    public StaffNotificationController(StaffNotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    public StaffNotificationPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "false") boolean unread,
            @AuthenticationPrincipal Jwt jwt) {
        return notificationService.findFor(currentUser(jwt), page, size, sort, unread);
    }

    @PatchMapping("/{id}")
    public StaffNotificationResponse markRead(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return notificationService.markRead(id, currentUser(jwt));
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("updated", notificationService.markAllRead(currentUser(jwt)));
    }

    private User currentUser(Jwt jwt) {
        return userService.getByEmail(jwt.getSubject());
    }
}
