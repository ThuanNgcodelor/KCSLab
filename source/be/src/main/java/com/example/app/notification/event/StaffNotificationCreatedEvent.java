package com.example.app.notification.event;

import com.example.app.notification.dto.StaffNotificationResponse;

public record StaffNotificationCreatedEvent(
        String recipientEmail,
        StaffNotificationResponse notification
) {}
