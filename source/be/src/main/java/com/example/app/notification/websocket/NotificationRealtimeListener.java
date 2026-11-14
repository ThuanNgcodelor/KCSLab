package com.example.app.notification.websocket;

import com.example.app.notification.event.StaffNotificationCreatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationRealtimeListener {
    private final NotificationWebSocketHandler webSocketHandler;

    public NotificationRealtimeListener(NotificationWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationCreated(StaffNotificationCreatedEvent event) {
        webSocketHandler.send(event.recipientEmail(), event.notification());
    }
}
