package com.example.app.notification.websocket;

import com.example.app.notification.dto.StaffNotificationResponse;
import com.example.app.notification.entity.StaffNotificationType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.security.Principal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationWebSocketHandlerTest {

    @Test
    void aFailedTabDoesNotStopDeliveryToAnotherTabOfTheSameRecipient() throws Exception {
        var mapper = JsonMapper.builder().build();
        var handler = new NotificationWebSocketHandler(mapper);
        var failedTab = session("first", "kcs@example.test");
        var healthyTab = session("second", "kcs@example.test");
        var otherAccount = session("third", "intake@example.test");
        handler.afterConnectionEstablished(failedTab);
        handler.afterConnectionEstablished(healthyTab);
        handler.afterConnectionEstablished(otherAccount);
        doThrow(new IOException("Connection closed")).when(failedTab).sendMessage(any());
        var notification = new StaffNotificationResponse(42L, StaffNotificationType.TRANSFER_SUBMITTED,
                "Mẫu mới", "Có mẫu cần nhận", "/admin/receiving/transfers/1", "TRANSFER", 1L,
                null, Instant.parse("2026-11-20T05:00:00Z"),
                new StaffNotificationResponse.Actor("Tiếp nhận", "/api/v1/users/2/avatar?v=1"));

        handler.send("kcs@example.test", notification);
        handler.send("kcs@example.test", notification);

        var messages = ArgumentCaptor.forClass(TextMessage.class);
        verify(healthyTab, times(2)).sendMessage(messages.capture());
        assertThat(mapper.readTree(messages.getValue().getPayload()).path("actor").path("displayName").asText())
                .isEqualTo("Tiếp nhận");
        verify(failedTab).sendMessage(any());
        verify(failedTab).close(CloseStatus.SERVER_ERROR);
        verify(otherAccount, never()).sendMessage(any());

        handler.afterConnectionClosed(healthyTab, CloseStatus.NORMAL);
        handler.send("kcs@example.test", notification);
        verify(healthyTab, times(2)).sendMessage(any());
    }

    private WebSocketSession session(String id, String email) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.getPrincipal()).thenReturn((Principal) () -> email);
        when(session.isOpen()).thenReturn(true);
        return session;
    }
}
