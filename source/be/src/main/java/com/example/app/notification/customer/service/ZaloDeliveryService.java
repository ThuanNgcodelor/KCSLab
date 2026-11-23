package com.example.app.notification.customer.service;

import com.example.app.notification.customer.config.CustomerNotificationProperties;
import com.example.app.notification.customer.repository.CustomerNotificationRepository;
import com.example.app.notification.customer.repository.ZaloDeliveryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class ZaloDeliveryService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ZaloDeliveryService.class);
    private final CustomerNotificationProperties properties;
    private final ZaloDeliveryRepository repository;
    private final ZaloTokenService tokens;
    private final ObjectMapper json;
    private final RestClient http;

    @Autowired
    public ZaloDeliveryService(CustomerNotificationProperties properties, ZaloDeliveryRepository repository, ZaloTokenService tokens, ObjectMapper json) {
        this(properties, repository, tokens, json, client());
    }
    public ZaloDeliveryService(CustomerNotificationProperties properties, ZaloDeliveryRepository repository, ZaloTokenService tokens, ObjectMapper json, RestClient http) {
        this.properties = properties; this.repository = repository; this.tokens = tokens; this.json = json; this.http = http;
    }
    private static RestClient client() {
        var factory = new SimpleClientHttpRequestFactory(); factory.setConnectTimeout(5000); factory.setReadTimeout(10000);
        return RestClient.builder().baseUrl("https://business.openapi.zalo.me").requestFactory(factory).build();
    }

    public void receive(byte[] raw, String signature, String server) {
        if (!properties.isWebhookEnabled() || properties.getWebhookSecret().isBlank() || properties.getAppId().isBlank() || properties.getOaId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Webhook chưa được cấu hình");
        }
        if (raw.length > 16384) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        try {
            var data = json.readTree(raw);
            String app = data.path("app_id").asText(), oa = data.path("sender").path("id").asText();
            String timestamp = data.path("timestamp").asText();
            if (!app.equals(properties.getAppId()) || !oa.equals(properties.getOaId()) || !"ZNS".equals(server)) throw forbidden();
            String supplied = signature == null ? "" : signature.trim();
            if (supplied.startsWith("mac=")) supplied = supplied.substring(4);
            if (!supplied.matches("[a-fA-F0-9]{64}")) throw forbidden();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(app.getBytes(StandardCharsets.UTF_8)); digest.update(raw);
            digest.update(timestamp.getBytes(StandardCharsets.UTF_8)); digest.update(properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8));
            if (!MessageDigest.isEqual(digest.digest(), HexFormat.of().parseHex(supplied))) throw forbidden();
            long now = Instant.now().toEpochMilli(), eventTime = Long.parseLong(timestamp);
            int maxAge = Math.clamp(properties.getWebhookMaxAgeSeconds(), 60, 604800);
            if (eventTime < now - maxAge * 1000L || eventTime > now + 300000) throw forbidden();
            if (!data.path("event_name").asText().equals("user_received_message")) throw new IllegalArgumentException();
            String msg = data.path("message").path("msg_id").asText(), tracking = data.path("message").path("tracking_id").asText();
            if (msg.isBlank() || msg.length() > 160 || !tracking.matches("[A-Za-z0-9]{1,48}")) throw new IllegalArgumentException();
            Instant delivered = Instant.ofEpochMilli(Long.parseLong(data.path("message").path("delivery_time").asText()));
            if (delivered.isBefore(Instant.EPOCH) || delivered.toEpochMilli() > now + 300000) throw new IllegalArgumentException();
            String phone = RecipientPhone.normalize(data.path("recipient").path("id").asText());
            repository.receive(app, oa, msg, tracking, CustomerNotificationRepository.sha256(phone), delivered);
        } catch (org.springframework.dao.DataAccessException exception) { throw exception; }
        catch (ResponseStatusException exception) { throw exception; }
        catch (RuntimeException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook không hợp lệ"); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    private ResponseStatusException forbidden() { return new ResponseStatusException(HttpStatus.FORBIDDEN, "Webhook không hợp lệ"); }

    @Scheduled(fixedDelayString = "${app.customer-notifications.delivery-check-delay-ms:30000}")
    public void scheduled() {
        if (!properties.isWebhookEnabled() && !properties.isPollingEnabled()) return;
        try { runOnce(); } catch (RuntimeException exception) { log.warn("Zalo delivery reconciliation failed; retrying next cycle"); }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void runOnce() {
        repository.applyPending();
        if (!properties.isPollingEnabled() || !java.util.Set.of("ZALO_DEVELOPMENT", "ZALO_LIVE").contains(properties.getMode().name())) return;
        String token = tokens.currentAccessToken(); if (token.isBlank()) return;
        for (var check : repository.dueChecks(properties.getAppId(), properties.getOaId(), properties.getMode().name(), Math.clamp(properties.getMaxStatusChecks(), 1, 20))) {
            if (!repository.claimCheck(check)) continue;
            try {
                var response = http.get().uri(builder -> builder.path("/message/status").queryParam("message_id", check.messageId()).queryParam("phone", check.phone()).build())
                        .header("access_token", token).retrieve().body(String.class);
                var tree = json.readTree(response);
                if (!tree.has("error") || !tree.get("error").isIntegralNumber() || tree.get("error").asInt() != 0) {
                    repository.checked(check, -2, null, "STATUS_PROVIDER_ERROR"); continue;
                }
                var stateNode = tree.path("data").path("status");
                if (!stateNode.isIntegralNumber()) throw new IllegalArgumentException();
                int state = stateNode.asInt(-2);
                if (state < -1 || state > 1) throw new IllegalArgumentException();
                Instant when = state == 1 ? Instant.ofEpochMilli(Long.parseLong(tree.path("data").path("delivery_time").asText())) : null;
                if (when != null && when.isAfter(Instant.now().plusSeconds(300))) throw new IllegalArgumentException();
                repository.checked(check, state, when, "STATUS_" + state);
            } catch (RuntimeException exception) { repository.checked(check, -2, null, "STATUS_CHECK_FAILED"); }
        }
    }
}
