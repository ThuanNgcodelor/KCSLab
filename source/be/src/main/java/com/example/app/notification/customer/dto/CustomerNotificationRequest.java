package com.example.app.notification.customer.dto;

import jakarta.validation.constraints.Size;

public record CustomerNotificationRequest(boolean enabled, @Size(max = 30) String recipientPhone,
                                          boolean consentAccepted) {}
