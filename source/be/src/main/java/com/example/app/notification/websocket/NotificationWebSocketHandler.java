package com.example.app.notification.websocket;

import com.example.app.notification.dto.StaffNotificationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    public NotificationWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (session.getPrincipal() == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication required"));
            return;
        }
        sessionsByUser.computeIfAbsent(session.getPrincipal().getName(), ignored -> new CopyOnWriteArraySet<>())
                .add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        if (session.getPrincipal() == null) return;
        Set<WebSocketSession> sessions = sessionsByUser.get(session.getPrincipal().getName());
        if (sessions == null) return;
        sessions.remove(session);
        if (sessions.isEmpty()) sessionsByUser.remove(session.getPrincipal().getName(), sessions);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
    }

    public void send(String recipientEmail, StaffNotificationResponse notification) {
        Set<WebSocketSession> sessions = sessionsByUser.get(recipientEmail);
        if (sessions == null || sessions.isEmpty()) return;
        try {
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(notification));
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) session.sendMessage(message);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể gửi thông báo realtime", exception);
        }
    }
}
