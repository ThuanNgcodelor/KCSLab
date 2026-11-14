package com.example.app.notification.dto;

import com.example.app.notification.entity.StaffNotificationType;

import java.time.Instant;

public record StaffNotificationResponse(
        Long id,
        StaffNotificationType type,
        String title,
        String message,
        String targetUrl,
        String entityType,
        Long entityId,
        Instant readAt,
        Instant createdAt
) {}
