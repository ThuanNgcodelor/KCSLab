package com.example.app.notification.customer;

import com.example.app.notification.customer.config.CustomerNotificationProperties;
import com.example.app.notification.customer.repository.ZaloDeliveryRepository;
import com.example.app.notification.customer.service.ZaloDeliveryService;
import com.example.app.notification.customer.service.ZaloTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ZaloWebhookPersistenceTest {
    @Test void databaseFailureIsNotAcknowledgedAsSuccessfulOrClassifiedAsBadProviderPayload() throws Exception {
        var props = new CustomerNotificationProperties(); props.setWebhookEnabled(true); props.setWebhookSecret("test-key"); props.setAppId("test-app"); props.setOaId("test-oa");
        var repository = mock(ZaloDeliveryRepository.class);
        doThrow(new DataAccessResourceFailureException("test DB unavailable")).when(repository).receive(anyString(), anyString(), anyString(), anyString(), anyString(), any());
        String time = Long.toString(Instant.now().toEpochMilli());
        byte[] body = ("{\"app_id\":\"test-app\",\"sender\":{\"id\":\"test-oa\"},\"recipient\":{\"id\":\"84900000099\"},"
                + "\"event_name\":\"user_received_message\",\"timestamp\":\"" + time + "\",\"message\":{\"msg_id\":\"test-message\",\"tracking_id\":\"CFCDEVtest\",\"delivery_time\":\"" + time + "\"}}").getBytes(StandardCharsets.UTF_8);
        var digest = MessageDigest.getInstance("SHA-256"); digest.update("test-app".getBytes(StandardCharsets.UTF_8)); digest.update(body);
        digest.update(time.getBytes(StandardCharsets.UTF_8)); digest.update("test-key".getBytes(StandardCharsets.UTF_8));
        var service = new ZaloDeliveryService(props, repository, mock(ZaloTokenService.class), JsonMapper.builder().build());
        assertThatThrownBy(() -> service.receive(body, HexFormat.of().formatHex(digest.digest()), "ZNS")).isInstanceOf(DataAccessResourceFailureException.class);
    }
}
