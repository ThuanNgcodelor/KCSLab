package com.example.app.notification.service;

import com.example.app.notification.entity.StaffNotificationType;
import com.example.app.user.entity.Role;

import java.util.EnumSet;
import java.util.Set;

/** The same role boundary applies when creating, reading, counting and publishing staff notices. */
public final class StaffNotificationPolicy {
    private StaffNotificationPolicy() {}

    public static Set<StaffNotificationType> typesFor(Role role) {
        return switch (role) {
            case ADMIN -> EnumSet.allOf(StaffNotificationType.class);
            case RECEPTIONIST -> EnumSet.of(StaffNotificationType.TEST_REQUEST_CREATED,
                    StaffNotificationType.TRANSFER_ACCEPTED, StaffNotificationType.TRANSFER_REJECTED);
            case TECHNICIAN -> EnumSet.of(StaffNotificationType.TRANSFER_SUBMITTED);
        };
    }

    public static boolean allows(Role role, StaffNotificationType type) { return typesFor(role).contains(type); }
}
