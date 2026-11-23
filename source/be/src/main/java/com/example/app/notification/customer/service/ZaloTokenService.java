package com.example.app.notification.customer.service;

import com.example.app.notification.customer.config.CustomerNotificationProperties;
import com.example.app.notification.customer.repository.CustomerNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.*;
import java.util.Map;
import java.util.UUID;

@Service
public class ZaloTokenService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ZaloTokenService.class);
    private final CustomerNotificationProperties properties;
    private final ZaloTokenCipher cipher;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper json;
    private final RestClient http;

    @Autowired
    public ZaloTokenService(CustomerNotificationProperties properties, ZaloTokenCipher cipher, JdbcTemplate jdbc,
                            PlatformTransactionManager manager, ObjectMapper json) {
        this(properties, cipher, jdbc, manager, json, client());
    }

    public ZaloTokenService(CustomerNotificationProperties properties, ZaloTokenCipher cipher, JdbcTemplate jdbc,
                            PlatformTransactionManager manager, ObjectMapper json, RestClient http) {
        this.properties = properties; this.cipher = cipher; this.jdbc = jdbc;
        tx = new TransactionTemplate(manager); this.json = json; this.http = http;
    }

    private static RestClient client() {
        var factory = new SimpleClientHttpRequestFactory(); factory.setConnectTimeout(5000); factory.setReadTimeout(10000);
        return RestClient.builder().baseUrl("https://oauth.zaloapp.com").requestFactory(factory).build();
    }

    public String scope() {
        // A refresh token belongs to App/OA, including when switching development/live mode.
        return CustomerNotificationRepository.sha256(properties.getAppId() + "|" + properties.getOaId());
    }

    private TokenRow row() {
        var rows = jdbc.query("SELECT * FROM zalo_managed_tokens WHERE scope_key=?", (r, i) -> new TokenRow(r.getString("encrypted_pair"),
                r.getObject("access_expires_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                r.getObject("refresh_expires_at", LocalDateTime.class).toInstant(ZoneOffset.UTC), r.getString("state")), scope());
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public String currentAccessToken() {
        var row = row();
        if (row == null) return properties.getMode() == CustomerNotificationProperties.Mode.ZALO_DEVELOPMENT ? properties.getAccessToken() : "";
        if (!row.state().equals("READY") || !row.accessExpiry().isAfter(Instant.now()) || !cipher.configured()) return "";
        try { return json.readTree(cipher.decrypt(scope(), row.encrypted())).path("access_token").asText(""); }
        catch (RuntimeException exception) { return ""; }
    }

    public Health health() {
        var row = row();
        if (row == null) return new Health("NOT_PROVISIONED", null, null, false);
        String state = row.state();
        if (state.equals("READY")) {
            if (!cipher.configured()) state = "ENCRYPTION_UNAVAILABLE";
            else if (!row.refreshExpiry().isAfter(Instant.now())) state = "NEEDS_AUTH";
            else if (!row.accessExpiry().isAfter(Instant.now())) state = "EXPIRED";
            else {
                try { if (json.readTree(cipher.decrypt(scope(), row.encrypted())).path("access_token").asText("").isBlank()) state = "NEEDS_AUTH"; }
                catch (RuntimeException exception) { state = "ENCRYPTION_UNAVAILABLE"; }
            }
        }
        return new Health(state, row.accessExpiry(), row.refreshExpiry(), cipher.configured());
    }

    /** Initial provisioning only. Never return credentials to the caller or update .env. */
    public void provision(String accessToken, String refreshToken, long expiresIn) {
        if (accessToken == null || accessToken.isBlank() || accessToken.length() > 16000 || refreshToken == null || refreshToken.isBlank()
                || refreshToken.length() > 16000 || expiresIn < 1 || expiresIn > 604800) throw new IllegalStateException("TOKEN_INPUT_INVALID");
        if (!cipher.configured() || !isZaloMode() || properties.getAppId().isBlank() || properties.getOaId().isBlank()) throw new IllegalStateException("TOKEN_CONFIGURATION_INCOMPLETE");
        String encrypted = cipher.encrypt(scope(), json.writeValueAsString(Map.of("access_token", accessToken, "refresh_token", refreshToken)));
        tx.executeWithoutResult(status -> {
            var states = jdbc.queryForList("SELECT state FROM zalo_managed_tokens WHERE scope_key=? FOR UPDATE", String.class, scope());
            if (!states.isEmpty() && states.getFirst().equals("REFRESHING")) throw new IllegalStateException("TOKEN_REFRESH_IN_PROGRESS");
            jdbc.update("""
                    INSERT INTO zalo_managed_tokens(scope_key,app_id,oa_id,provider_mode,encrypted_pair,access_expires_at,refresh_expires_at,state,updated_at)
                    VALUES (?,?,?,?,?,DATE_ADD(UTC_TIMESTAMP(6), INTERVAL ? SECOND),DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 3 MONTH),'READY',UTC_TIMESTAMP(6))
                    ON DUPLICATE KEY UPDATE encrypted_pair=VALUES(encrypted_pair),access_expires_at=VALUES(access_expires_at),
                    refresh_expires_at=VALUES(refresh_expires_at),state='READY',lease_owner=NULL,lease_until=NULL,reason_code=NULL,updated_at=UTC_TIMESTAMP(6)
                    """, scope(), properties.getAppId(), properties.getOaId(), properties.getMode().name(), encrypted, expiresIn);
        });
    }

    @Scheduled(fixedDelayString = "${app.customer-notifications.token-check-delay-ms:60000}")
    public void scheduled() {
        if (!properties.isTokenRefreshEnabled() || !isZaloMode()) return;
        try { refreshDue(); }
        catch (RuntimeException exception) { log.warn("Zalo token maintenance failed; inspect token health"); }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void refreshDue() {
        if (!properties.isTokenRefreshEnabled() || !isZaloMode() || !cipher.configured() || properties.getAppSecret().isBlank()) return;
        String key = scope(), owner = UUID.randomUUID().toString();
        String encrypted = tx.execute(status -> {
            jdbc.update("""
                    UPDATE zalo_managed_tokens SET state='NEEDS_AUTH',reason_code='REFRESH_RESULT_UNKNOWN',lease_owner=NULL,lease_until=NULL
                    WHERE scope_key=? AND state='REFRESHING' AND lease_until<UTC_TIMESTAMP(6)
                    """, key);
            int claimed = jdbc.update("""
                    UPDATE zalo_managed_tokens SET state='REFRESHING',lease_owner=?,lease_until=DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 60 SECOND)
                    WHERE scope_key=? AND state='READY' AND access_expires_at<=DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 10 MINUTE)
                      AND refresh_expires_at>UTC_TIMESTAMP(6)
                    """, owner, key);
            return claimed == 1 ? jdbc.queryForObject("SELECT encrypted_pair FROM zalo_managed_tokens WHERE scope_key=?", String.class, key) : null;
        });
        if (encrypted == null) return;
        try {
            String refresh = json.readTree(cipher.decrypt(key, encrypted)).path("refresh_token").asText("");
            if (refresh.isBlank()) throw new IllegalStateException();
            var form = new LinkedMultiValueMap<String, String>(); form.add("app_id", properties.getAppId());
            form.add("refresh_token", refresh); form.add("grant_type", "refresh_token");
            String response = http.post().uri("/v4/oa/access_token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("secret_key", properties.getAppSecret()).body(form).retrieve().body(String.class);
            var tree = json.readTree(response);
            String access = tree.path("access_token").asText(""), next = tree.path("refresh_token").asText("");
            long expiry = Long.parseLong(tree.path("expires_in").asText("0"));
            if (access.isBlank() || next.isBlank() || access.length() > 16000 || next.length() > 16000 || expiry < 1 || expiry > 604800) throw new IllegalStateException();
            String pair = cipher.encrypt(key, json.writeValueAsString(Map.of("access_token", access, "refresh_token", next)));
            jdbc.update("""
                    UPDATE zalo_managed_tokens SET encrypted_pair=?,access_expires_at=DATE_ADD(UTC_TIMESTAMP(6),INTERVAL ? SECOND),
                    refresh_expires_at=DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 3 MONTH),state='READY',lease_owner=NULL,lease_until=NULL,
                    reason_code=NULL,updated_at=UTC_TIMESTAMP(6) WHERE scope_key=? AND state='REFRESHING' AND lease_owner=?
                    """, pair, expiry, key, owner);
        } catch (RuntimeException exception) {
            jdbc.update("UPDATE zalo_managed_tokens SET state='NEEDS_AUTH',reason_code='REFRESH_RESULT_UNKNOWN',lease_owner=NULL,lease_until=NULL WHERE scope_key=? AND lease_owner=?", key, owner);
        }
    }

    private boolean isZaloMode() { return properties.getMode() == CustomerNotificationProperties.Mode.ZALO_DEVELOPMENT || properties.getMode() == CustomerNotificationProperties.Mode.ZALO_LIVE; }
    private record TokenRow(String encrypted, Instant accessExpiry, Instant refreshExpiry, String state) {
        @Override public String toString() { return "TokenRow[redacted]"; }
    }
    public record Health(String state, Instant accessExpiresAt, Instant refreshExpiresAt, boolean encryptionConfigured) {}
}
