package com.example.app.notification.dto;

import java.util.List;

public record StaffNotificationPageResponse(
        List<StaffNotificationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sort,
        long unreadCount
) {}
