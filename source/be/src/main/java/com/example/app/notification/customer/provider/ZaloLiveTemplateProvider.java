package com.example.app.notification.customer.provider;

import com.example.app.notification.customer.config.CustomerNotificationProperties;
import com.example.app.notification.customer.service.RecipientPhone;
import com.example.app.notification.customer.service.ZaloTokenService;
import com.example.app.notification.customer.service.CustomerNotificationOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;

/** Production is an independent opt-in provider; it never reuses development queues or static test tokens. */
@Component
public class ZaloLiveTemplateProvider implements CustomerMessageProvider {
    private final CustomerNotificationProperties properties;
    private final ZaloTokenService tokens;
    private final CustomerNotificationOperations operations;
    private final ObjectMapper json;
    private final RestClient client;
    @Autowired
    public ZaloLiveTemplateProvider(CustomerNotificationProperties properties, ZaloTokenService tokens, CustomerNotificationOperations operations, ObjectMapper json) {
        this(properties, tokens, operations, json, ZaloTemplateProvider.client());
    }
    public ZaloLiveTemplateProvider(CustomerNotificationProperties properties, ZaloTokenService tokens, CustomerNotificationOperations operations, ObjectMapper json, RestClient client) {
        this.properties = properties; this.tokens = tokens; this.operations = operations; this.json = json; this.client = client;
    }
    @Override
    public Outcome send(Message message) {
        if (properties.getMode() == CustomerNotificationProperties.Mode.ZALO_LIVE && operations.paused()) {
            return new Outcome("RETRY_WAIT", "SENDING_PAUSED", null);
        }
        if (properties.getMode() != CustomerNotificationProperties.Mode.ZALO_LIVE || !properties.isLiveEnabled()
                || !properties.isTokenRefreshEnabled() || !properties.isPollingEnabled() || properties.getAppSecret().isBlank()
                || properties.getSessionKey().isBlank() || !allowed(message.phone())) {
            return new Outcome("BLOCKED", "LIVE_CONFIGURATION_INCOMPLETE", null);
        }
        String token = tokens.currentAccessToken();
        if (token.isBlank() || !tokens.health().state().equals("READY")) return new Outcome("RETRY_WAIT", "TOKEN_NOT_READY", null);
        return ZaloMessageTransport.send(client, json, message, token, false);
    }
    private boolean allowed(String phone) {
        if (properties.isLiveAllowAllRecipients()) return true;
        try { return Arrays.stream(properties.getAllowedPhones().split(",")).filter(value -> !value.isBlank()).map(RecipientPhone::normalize).toList().contains(phone); }
        catch (RuntimeException exception) { return false; }
    }
}
