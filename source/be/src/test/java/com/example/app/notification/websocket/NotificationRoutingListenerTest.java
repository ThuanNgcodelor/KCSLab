package com.example.app.notification.websocket;

import com.example.app.notification.dto.StaffNotificationResponse;
import com.example.app.notification.entity.StaffNotificationType;
import com.example.app.notification.event.StaffNotificationCreatedEvent;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import static org.mockito.Mockito.*;

class NotificationRoutingListenerTest {
    @Test void realtimeChecksCurrentRoleAndEnabledStateBeforePublishing() {
        var sockets = mock(NotificationWebSocketHandler.class); var users = mock(UserRepository.class);
        var listener = new NotificationRealtimeListener(sockets, users);
        var user = new User("role-test@example.test", "Test", "unused", Role.RECEPTIONIST);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        var response = new StaffNotificationResponse(1L, StaffNotificationType.TRANSFER_SUBMITTED, "Test", "Test", "/x", "TRANSFER", 1L, null, Instant.now(), null);
        var event = new StaffNotificationCreatedEvent(user.getEmail(), response);
        listener.onNotificationCreated(event); verifyNoInteractions(sockets);
        user.updateProfile("Test", Role.TECHNICIAN, false);
        listener.onNotificationCreated(event); verifyNoInteractions(sockets);
        user.updateProfile("Test", Role.TECHNICIAN, true);
        listener.onNotificationCreated(event); verify(sockets).send(user.getEmail(), response);
    }
}
