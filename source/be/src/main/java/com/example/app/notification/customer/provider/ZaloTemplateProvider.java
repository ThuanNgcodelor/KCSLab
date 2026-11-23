package com.example.app.notification.customer.provider;

import com.example.app.notification.customer.config.CustomerNotificationProperties;
import com.example.app.notification.customer.service.RecipientPhone;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;

/** Only development sends are supported. The destination and mode are enforced again at the HTTP boundary. */
@Component
public class ZaloTemplateProvider implements CustomerMessageProvider {
    private final CustomerNotificationProperties properties;
    private final ObjectMapper json;
    private final RestClient client;
    private final com.example.app.notification.customer.service.ZaloTokenService tokens;

    @org.springframework.beans.factory.annotation.Autowired
    public ZaloTemplateProvider(CustomerNotificationProperties properties, ObjectMapper json, com.example.app.notification.customer.service.ZaloTokenService tokens) {
        this(properties, json, client(), tokens);
    }

    ZaloTemplateProvider(CustomerNotificationProperties properties, ObjectMapper json, RestClient client) {
        this(properties, json, client, null);
    }

    private ZaloTemplateProvider(CustomerNotificationProperties properties, ObjectMapper json, RestClient client, com.example.app.notification.customer.service.ZaloTokenService tokens) {
        this.tokens = tokens;
        this.properties = properties;
        this.json = json;
        this.client = client;
    }

    static RestClient client() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return RestClient.builder().requestFactory(factory).baseUrl("https://business.openapi.zalo.me").build();
    }

    @Override
    public Outcome send(Message message) {
        if (properties.getMode() == CustomerNotificationProperties.Mode.ZALO_DEVELOPMENT && tokens != null && tokens.health().state().equals("REFRESHING")) {
            return new Outcome("RETRY_WAIT", "TOKEN_NOT_READY", null);
        }
        String token = tokens == null ? properties.getAccessToken() : tokens.currentAccessToken();
        if (properties.getMode() != CustomerNotificationProperties.Mode.ZALO_DEVELOPMENT
                || token.isBlank() || !allowed(message.phone())) {
            return new Outcome("BLOCKED", "OUTSIDE_TEST_CONFIGURATION", null);
        }
        return ZaloMessageTransport.send(client, json, message, token, true);
    }

    private boolean allowed(String phone) {
        try {
            return Arrays.stream(properties.getAllowedPhones().split(",")).filter(value -> !value.isBlank())
                    .map(RecipientPhone::normalize).toList().contains(phone);
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
