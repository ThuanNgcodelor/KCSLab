package com.example.app.notification.websocket;

import com.example.app.notification.dto.StaffNotificationResponse;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Map<String, WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    public NotificationWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (session.getPrincipal() == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication required"));
            return;
        }
        sessionsByUser.compute(session.getPrincipal().getName(), (user, sessions) -> {
            var active = sessions == null ? new ConcurrentHashMap<String, WebSocketSession>() : sessions;
            active.put(session.getId(), new ConcurrentWebSocketSessionDecorator(session, 5000, 64 * 1024));
            return active;
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        if (session.getPrincipal() == null) return;
        removeSession(session.getPrincipal().getName(), session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (session.getPrincipal() != null) removeSession(session.getPrincipal().getName(), session.getId());
        if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
    }

    public void send(String recipientEmail, StaffNotificationResponse notification) {
        var sessions = sessionsByUser.get(recipientEmail);
        if (sessions == null || sessions.isEmpty()) return;
        TextMessage message;
        try {
            message = new TextMessage(objectMapper.writeValueAsString(notification));
        } catch (RuntimeException exception) {
            log.warn("Could not encode notification {}", notification.id(), exception);
            return;
        }
        sessions.forEach((id, session) -> {
            try {
                if (session.isOpen()) session.sendMessage(message);
                else removeSession(recipientEmail, id);
            } catch (IOException | RuntimeException exception) {
                removeSession(recipientEmail, id);
                log.debug("Notification delivery failed for session {}", id, exception);
                try {
                    if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
                } catch (IOException | RuntimeException closeException) {
                    log.debug("Could not close notification session {}", id, closeException);
                }
            }
        });
    }

    private void removeSession(String user, String id) {
        sessionsByUser.computeIfPresent(user, (key, sessions) -> {
            sessions.remove(id);
            return sessions.isEmpty() ? null : sessions;
        });
    }
}
