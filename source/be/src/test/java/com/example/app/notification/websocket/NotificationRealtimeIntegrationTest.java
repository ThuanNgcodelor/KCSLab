package com.example.app.notification.websocket;

import com.example.app.auth.service.TokenService;
import com.example.app.notification.entity.StaffNotificationType;
import com.example.app.notification.service.StaffNotificationService;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.frontend-origin=http://localhost:5173")
@EnabledIfEnvironmentVariable(named = "KCS_ISOLATED_DB", matches = "true")
class NotificationRealtimeIntegrationTest {
    @LocalServerPort private int port;
    @Autowired private UserRepository users;
    @Autowired private TokenService tokens;
    @Autowired private StaffNotificationService notifications;
    @Autowired private TransactionTemplate transactions;
    @Autowired private ObjectMapper mapper;

    @Test
    void publishesToBothAuthenticatedTabsOnlyAfterCommit() throws Exception {
        String unique = UUID.randomUUID().toString();
        var receiver = users.save(new User(unique + "@example.test", "KCS test", "unused", Role.TECHNICIAN));
        var sender = users.save(new User("sender-" + unique + "@example.test", "Tiếp nhận test", "unused", Role.RECEPTIONIST));
        var first = new Messages();
        var second = new Messages();

        try (var client = HttpClient.newHttpClient()) {
            WebSocket one = connect(client, receiver, first);
            WebSocket two = connect(client, receiver, second);
            try {
                transactions.executeWithoutResult(status -> {
                    notify(receiver, sender, 101L);
                    status.setRollbackOnly();
                });
                notify(receiver, sender, 102L);

                for (var listener : List.of(first, second)) {
                    String text = listener.received.poll(5, TimeUnit.SECONDS);
                    assertThat(text).isNotNull();
                    var event = mapper.readTree(text);
                    assertThat(event.path("entityId").asLong()).isEqualTo(102L);
                    assertThat(event.path("actor").path("displayName").asText()).isEqualTo(sender.getDisplayName());
                }
                assertThat(notifications.findFor(receiver, 0, 10, "createdAt,desc", false).content())
                        .singleElement().satisfies(value -> assertThat(value.entityId()).isEqualTo(102L));
            } finally {
                one.abort();
                two.abort();
            }
        }
    }

    private void notify(User receiver, User sender, long id) {
        notifications.notifyUsers(List.of(receiver), sender, StaffNotificationType.TRANSFER_SUBMITTED,
                "Bàn giao mẫu", "KCS có mẫu đang chờ nhận", "/admin/receiving/transfers/" + id, "TRANSFER", id);
    }

    private WebSocket connect(HttpClient client, User user, Messages messages) throws Exception {
        return client.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .header("Cookie", "access_token=" + tokens.createAccessToken(user))
                .header("Origin", "http://localhost:5173")
                .buildAsync(URI.create("ws://localhost:" + port + "/ws/notifications"), messages)
                .get(5, TimeUnit.SECONDS);
    }

    private static final class Messages implements WebSocket.Listener {
        private final BlockingQueue<String> received = new LinkedBlockingQueue<>();
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                received.add(buffer.toString());
                buffer.setLength(0);
            }
            socket.request(1);
            return null;
        }
    }
}
