package com.example.app.notification.websocket;

import com.example.app.notification.event.StaffNotificationCreatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationRealtimeListener {
    private final NotificationWebSocketHandler webSocketHandler;
    private final com.example.app.user.repository.UserRepository users;

    public NotificationRealtimeListener(NotificationWebSocketHandler webSocketHandler, com.example.app.user.repository.UserRepository users) {
        this.webSocketHandler = webSocketHandler;
        this.users = users;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationCreated(StaffNotificationCreatedEvent event) {
        users.findByEmail(event.recipientEmail()).filter(user -> user.isEnabled()
                        && com.example.app.notification.service.StaffNotificationPolicy.allows(user.getRole(), event.notification().type()))
                .ifPresent(user -> webSocketHandler.send(event.recipientEmail(), event.notification()));
    }
}
