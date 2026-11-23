package com.example.app.notification.customer;

import com.example.app.intake.dto.CreateReceiptRequest;
import com.example.app.intake.dto.ReceiptResponse;
import com.example.app.intake.entity.ReceiptAttachmentCategory;
import com.example.app.intake.repository.ReceiptRepository;
import com.example.app.intake.service.ReceiptService;
import com.example.app.notification.customer.config.CustomerNotificationProperties;
import com.example.app.notification.customer.provider.CustomerMessageProvider;
import com.example.app.notification.customer.provider.FakeCustomerMessageProvider;
import com.example.app.notification.customer.provider.ZaloTemplateProvider;
import com.example.app.notification.customer.repository.CustomerNotificationRepository;
import com.example.app.notification.customer.service.CustomerNotificationWorker;
import com.example.app.notification.customer.service.IntakeCustomerNotificationService;
import com.example.app.notification.customer.service.CustomerNotificationOperations;
import com.example.app.notification.customer.service.ZaloDeliveryService;
import com.example.app.notification.customer.service.ZaloTokenService;
import com.example.app.notification.customer.service.ZaloTokenCipher;
import com.example.app.notification.customer.repository.ZaloDeliveryRepository;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.customer-notifications.mode=FAKE", "app.customer-notifications.worker-enabled=false",
        "app.customer-notifications.token-check-delay-ms=3600000", "app.customer-notifications.delivery-check-delay-ms=3600000",
        "app.storage.root=build/test-storage/customer-notifications"})
@EnabledIfEnvironmentVariable(named = "KCS_ISOLATED_DB", matches = "true")
class CustomerNotificationIntegrationTest {
    @Autowired ReceiptService receipts;
    @Autowired ReceiptRepository receiptRepository;
    @Autowired UserRepository users;
    @Autowired CustomerNotificationRepository outbox;
    @Autowired CustomerNotificationWorker worker;
    @Autowired IntakeCustomerNotificationService service;
    @Autowired CustomerNotificationProperties properties;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @Autowired ObjectMapper json;
    @Autowired WebApplicationContext context;
    @Autowired ZaloDeliveryService delivery;
    @Autowired ZaloDeliveryRepository deliveryRepository;
    @Autowired ZaloTokenService tokens;
    @Autowired ZaloTokenCipher cipher;
    @Autowired CustomerNotificationOperations operations;
    @Autowired org.springframework.security.oauth2.jwt.JwtEncoder jwtEncoder;
    @MockitoBean ZaloTemplateProvider zalo;
    @MockitoSpyBean FakeCustomerMessageProvider fake;
    private User receptionist;
    private User admin;
    private TransactionTemplate transactions;
    private MockMvc mvc;
    private final List<Long> ownedReceipts = new ArrayList<>();
    private final Set<Long> ownedCustomers = new HashSet<>();
    private final String unique = UUID.randomUUID().toString().substring(0, 8);

    @BeforeEach
    void setUp() {
        properties.setMode(CustomerNotificationProperties.Mode.FAKE);
        properties.setAllowedPhones(""); properties.setAppId(""); properties.setOaId(""); properties.setTemplateId("");
        properties.setBindingsJson("{}"); properties.setAccessToken(""); properties.setSessionKey(""); properties.setMaxCallsPerSession(5);
        properties.setWebhookEnabled(false); properties.setWebhookSecret(""); properties.setPollingEnabled(false);
        properties.setTokenRefreshEnabled(false); properties.setTokenEncryptionKey(""); properties.setAppSecret("");
        properties.setLiveEnabled(false); properties.setLiveAllowAllRecipients(false); properties.setMaxStatusChecks(6);
        jdbc.update("UPDATE customer_notification_controls SET paused=false WHERE id=1");
        transactions = new TransactionTemplate(manager);
        receptionist = users.save(new User(unique + "-rec@example.test", "Test reception", "unused", Role.RECEPTIONIST));
        admin = users.save(new User(unique + "-admin@example.test", "Test admin", "unused", Role.ADMIN));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void cleanup() {
        transactions.executeWithoutResult(status -> {
            for (Long id : ownedReceipts) {
                jdbc.update("DELETE a FROM notification_delivery_attempts a JOIN customer_notification_outbox o ON o.id=a.outbox_id WHERE o.receipt_id=?", id);
                jdbc.update("DELETE FROM customer_notification_outbox WHERE receipt_id=?", id);
                jdbc.update("DELETE FROM receipt_notification_consents WHERE receipt_id=?", id);
                jdbc.update("DELETE FROM staff_notifications WHERE entity_type='RECEIPT' AND entity_id=?", id);
                jdbc.update("DELETE FROM receipt_attachments WHERE receipt_id=?", id);
                jdbc.update("DELETE FROM samples WHERE receipt_id=?", id);
                jdbc.update("DELETE FROM receipts WHERE id=?", id);
            }
            for (Long id : ownedCustomers) jdbc.update("DELETE FROM customers WHERE id=?", id);
            jdbc.update("DELETE FROM customer_notification_test_budgets WHERE session_key=?", "test-" + unique);
            jdbc.update("DELETE FROM zalo_delivery_inbox WHERE app_id=?", "test-app-" + unique);
            jdbc.update("DELETE FROM zalo_managed_tokens WHERE app_id=?", "test-app-" + unique);
            jdbc.update("DELETE FROM customer_notification_operation_audit WHERE actor_user_id IN (?,?)", receptionist.getId(), admin.getId());
            jdbc.update("UPDATE customer_notification_controls SET paused=false WHERE id=1");
            jdbc.update("DELETE FROM users WHERE id IN (?,?)", receptionist.getId(), admin.getId());
        });
    }

    private CreateReceiptRequest request(String number, String phone, int samples, boolean enabled) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 0; index < samples; index++) rows.add(Map.of("sourceCode", "CT901", "productType", "SR", "name", "Test sample " + index,
                "quantity", "1 kg", "packaging", "Test bag", "condition", "Intact", "requestedParameters", "Cadimi"));
        Map<String, Object> body = new LinkedHashMap<>(Map.of("customerName", "Test customer " + number, "customerPhone", "0900000099",
                "requestNumber", "QA-" + unique + "-" + number, "deliveredBy", "Test delivery", "sampleName", "Test sample",
                "sampleCondition", "Intact", "sealStatus", "INTACT", "acceptanceDecision", "ACCEPTED", "samples", rows));
        if (enabled) body.put("customerNotification", Map.of("enabled", true, "recipientPhone", phone, "consentAccepted", true));
        return json.readValue(json.writeValueAsString(body), CreateReceiptRequest.class);
    }

    private ReceiptResponse create(String number, int samples) {
        var receipt = transactions.execute(status -> receipts.create(request(number, "0900000099", samples, true), receptionist));
        own(receipt);
        return receipt;
    }

    private void own(ReceiptResponse receipt) {
        ownedReceipts.add(receipt.id());
        ownedCustomers.add(jdbc.queryForObject("SELECT customer_id FROM receipts WHERE id=?", Long.class, receipt.id()));
    }

    private CustomerNotificationRepository.Job job(ReceiptResponse receipt) { return outbox.jobs(receipt.id(), 0, 20).getFirst(); }

    private void development() {
        properties.setMode(CustomerNotificationProperties.Mode.ZALO_DEVELOPMENT);
        properties.setAllowedPhones("0900000099"); properties.setAppId("test-app-" + unique); properties.setOaId("test-oa");
        properties.setTemplateId("test-template"); properties.setAccessToken("test-only-not-real");
        properties.setSessionKey("test-" + unique);
        properties.setBindingsJson("{\"order_code\":{\"source\":\"requestNumber\",\"maxLength\":80}}");
    }

    @Test
    void multipleOrdersForOneCustomerHaveIndependentImmutableSnapshotsAndOneEventPerReceipt() {
        doAnswer(invocation -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return invocation.callRealMethod();
        }).when(fake).send(any());
        var first = create("first", 3);
        var second = create("second", 1);
        assertThat(job(first).createdAt()).isCloseTo(java.time.Instant.now(), org.assertj.core.api.Assertions.within(20, java.time.temporal.ChronoUnit.SECONDS));
        assertThat(job(first).payload()).contains("\"sampleCount\":\"3\"", first.requestNumber(), "Test customer first");
        assertThat(job(second).payload()).contains(second.requestNumber(), "Test customer second");
        transactions.executeWithoutResult(status -> service.record(receiptRepository.findById(first.id()).orElseThrow(), request("first", "0900000099", 3, true), receptionist));
        assertThat(outbox.count(first.id())).isEqualTo(1);
        worker.process(job(first).id()); worker.process(job(first).id()); worker.process(job(second).id());
        assertThat(job(first).status()).isEqualTo("SIMULATED");
        assertThat(outbox.attempts(job(first).id())).hasSize(1);
        assertThat(job(second).status()).isEqualTo("SIMULATED");
        verifyNoInteractions(zalo);
    }

    @Test
    void explicitOptOutCannotBeManuallyRetried() {
        development();
        when(zalo.send(any())).thenReturn(new CustomerMessageProvider.Outcome("FAILED_FINAL", "-141", null));
        var receipt = create("optout", 1);
        long id = job(receipt).id(); worker.process(id);
        assertThatThrownBy(() -> service.retry(receipt.id(), id, "optout-retry", "Test", admin)).hasMessageContaining("409");
        verify(zalo, times(1)).send(any());
    }

    @Test
    void workerCannotSeeUncommittedReceiptAndRollbackLeavesNoSendJob() throws Exception {
        long before = jdbc.queryForObject("SELECT COUNT(*) FROM customer_notification_outbox", Long.class);
        try (var executor = Executors.newSingleThreadExecutor()) {
            transactions.executeWithoutResult(status -> {
                var receipt = receipts.create(request("rollback", "0900000099", 1, true), receptionist);
                long id = job(receipt).id();
                try { executor.submit(() -> worker.process(id)).get(5, TimeUnit.SECONDS); }
                catch (Exception exception) { throw new RuntimeException(exception); }
                verifyNoInteractions(fake, zalo);
                status.setRollbackOnly();
            });
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM customer_notification_outbox", Long.class)).isEqualTo(before);
    }

    @Test
    void oldRequestDoesNotCreateAnOutboxAndInvalidConsentRollsBackReceipt() {
        var old = receipts.create(request("legacy", "", 1, false), receptionist); own(old);
        assertThat(outbox.count(old.id())).isZero();
        var invalid = request("invalid", "0900000099", 1, true);
        var encoded = json.writeValueAsString(invalid).replace("\"consentAccepted\":true", "\"consentAccepted\":false");
        assertThatThrownBy(() -> receipts.create(json.readValue(encoded, CreateReceiptRequest.class), receptionist)).hasMessageContaining("đồng ý");
    }

    @Test
    void outsideAllowlistAndMissingConfigurationBlockNetworkButKeepReceipt() {
        development(); properties.setAllowedPhones("0900000100");
        var outside = create("outside", 1);
        assertThat(job(outside).reason()).isEqualTo("OUTSIDE_TEST_ALLOWLIST");
        worker.process(job(outside).id());
        properties.setAllowedPhones("");
        assertThat(service.settings().ready()).isFalse();
        assertThat(service.settings().issues()).contains("TEST_RECIPIENT_REQUIRED");
        verifyNoInteractions(zalo, fake);
    }

    @Test
    void offAndChangedModeCannotReplayOldTestMessages() {
        var pending = create("pending", 1);
        properties.setMode(CustomerNotificationProperties.Mode.OFF);
        worker.process(job(pending).id());
        var off = create("off", 1);
        assertThat(job(off).status()).isEqualTo("CANCELLED");
        development(); worker.process(job(pending).id());
        assertThat(job(pending).reason()).isEqualTo("PROVIDER_MODE_CHANGED");
        verifyNoInteractions(zalo, fake);
    }

    @Test
    void twoWorkersClaimOnlyOneAttemptAndExpiredLeaseBecomesUnknown() throws Exception {
        var receipt = create("concurrent", 1);
        long id = job(receipt).id();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> outbox.claim(id, "FAKE", "FAKE", 5));
            var two = executor.submit(() -> outbox.claim(id, "FAKE", "FAKE", 5));
            assertThat(Arrays.asList(one.get(5, TimeUnit.SECONDS), two.get(5, TimeUnit.SECONDS))).filteredOn(Objects::nonNull).hasSize(1);
        }
        jdbc.update("UPDATE customer_notification_outbox SET lease_until=DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 SECOND) WHERE id=?", id);
        outbox.recoverExpired(); worker.process(id);
        assertThat(job(receipt).status()).isEqualTo("UNKNOWN");
        assertThat(outbox.attempts(id)).singleElement().extracting(CustomerNotificationRepository.Attempt::status).isEqualTo("UNKNOWN");
        verifyNoInteractions(fake, zalo);
    }

    @Test
    void uncertainSendIsNotRetriedAndCallCapSurvivesWorkerRuns() {
        development(); properties.setMaxCallsPerSession(1);
        when(zalo.send(any())).thenReturn(CustomerMessageProvider.Outcome.unknown("TEST_TIMEOUT"));
        var first = create("timeout", 1); var second = create("overbudget", 1);
        worker.process(job(first).id()); worker.process(job(first).id()); worker.process(job(second).id());
        assertThat(job(first).status()).isEqualTo("UNKNOWN");
        assertThat(job(second).reason()).isEqualTo("TEST_CALL_LIMIT");
        verify(zalo, times(1)).send(any());
    }

    @Test
    void changedSessionDoesNotSendOldQueueAndPayloadSurvivesTemplateConfigChange() {
        development();
        var receipt = create("snapshot", 1);
        properties.setTemplateId("different-template");
        assertThat(job(receipt).templateId()).isEqualTo("test-template");
        properties.setSessionKey("different-session"); worker.process(job(receipt).id());
        assertThat(job(receipt).reason()).isEqualTo("TEST_SCOPE_CHANGED");
        verifyNoInteractions(zalo);
    }

    @Test
    void fakeTransientFailureBacksOffAndCannotSendAgainBeforeDue() {
        doReturn(new CustomerMessageProvider.Outcome("RETRY_WAIT", "FAKE_RETRY", null)).when(fake).send(any());
        var receipt = create("backoff", 1); long id = job(receipt).id();
        worker.process(id); worker.process(id);
        assertThat(job(receipt).status()).isEqualTo("RETRY_WAIT");
        verify(fake, times(1)).send(any());
        jdbc.update("UPDATE customer_notification_outbox SET next_attempt_at=DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 SECOND) WHERE id=?", id);
        doCallRealMethod().when(fake).send(any()); worker.process(id);
        assertThat(job(receipt).status()).isEqualTo("SIMULATED");
        assertThat(outbox.attempts(id)).hasSize(2);
    }

    @Test
    void retryChecksRoleParentIdAndIdempotencyAndUnknownIsBlocked() {
        doReturn(new CustomerMessageProvider.Outcome("FAILED_FINAL", "TEST_FAILURE", null)).when(fake).send(any());
        var receipt = create("retry", 1); long id = job(receipt).id(); worker.process(id);
        assertThatThrownBy(() -> service.retry(receipt.id(), id, "retry-test", "Test", receptionist)).hasMessageContaining("403");
        service.retry(receipt.id(), id, "retry-test", "Test retry", admin);
        service.retry(receipt.id(), id, "retry-test", "Test retry", admin);
        worker.process(id);
        service.retry(receipt.id(), id, "retry-test", "Test retry", admin);
        assertThat(outbox.attempts(id)).hasSize(2);
        jdbc.update("UPDATE customer_notification_outbox SET status='UNKNOWN' WHERE id=?", id);
        assertThatThrownBy(() -> service.retry(receipt.id(), id, "retry-new", "Test", admin)).hasMessageContaining("409");
        var other = create("other", 1);
        assertThatThrownBy(() -> service.retry(other.id(), id, "cross", "Test", admin)).hasMessageContaining("404");
    }

    private byte[] event(String tracking, String message, String phone, Instant when) {
        return json.writeValueAsBytes(Map.of("app_id", properties.getAppId(), "sender", Map.of("id", properties.getOaId()),
                "recipient", Map.of("id", phone), "event_name", "user_received_message", "timestamp", Long.toString(Instant.now().toEpochMilli()),
                "message", Map.of("msg_id", message, "tracking_id", tracking, "delivery_time", Long.toString(when.toEpochMilli()))));
    }

    private String signature(byte[] body) throws Exception {
        String stamp = json.readTree(body).path("timestamp").asText();
        var digest = MessageDigest.getInstance("SHA-256");
        digest.update(properties.getAppId().getBytes(StandardCharsets.UTF_8)); digest.update(body);
        digest.update(stamp.getBytes(StandardCharsets.UTF_8)); digest.update(properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest.digest());
    }

    private void webhook() { properties.setWebhookEnabled(true); properties.setWebhookSecret("test-webhook-key-not-real"); }

    private void managedTokens() {
        properties.setTokenEncryptionKey(Base64.getEncoder().encodeToString(new byte[32]));
        properties.setTokenRefreshEnabled(true); properties.setAppSecret("test-app-secret-not-real");
        tokens.provision("access-initial-test", "refresh-initial-test", 90000);
    }

    @Test
    void webhookIsDurableDeduplicatedAndCanArriveBeforeSendResponseWithoutDowngrade() throws Exception {
        development(); webhook();
        var receipt = create("early-webhook", 1); long id = job(receipt).id();
        var claimed = outbox.claim(id, properties.getMode().name(), properties.getSessionKey(), 5);
        byte[] body = event(claimed.trackingId(), "early-message", "84900000099", Instant.now());
        delivery.receive(body, signature(body), "ZNS"); delivery.receive(body, signature(body), "ZNS");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM zalo_delivery_inbox WHERE app_id=?", Long.class, properties.getAppId())).isEqualTo(1);
        deliveryRepository.applyPending();
        outbox.finish(claimed, CustomerMessageProvider.Outcome.accepted("early-message"));
        assertThat(job(receipt).status()).isEqualTo("DELIVERED");
        assertThat(outbox.attempts(id)).singleElement().satisfies(attempt -> {
            assertThat(attempt.deliveredAt()).isNotNull(); assertThat(attempt.deliverySource()).isEqualTo("WEBHOOK");
        });
        worker.process(id); verifyNoInteractions(zalo);
    }

    @Test
    void pendingWebhookMatchesLaterAndWrongRecipientOrMessageCannotForgeDelivery() throws Exception {
        development(); webhook();
        byte[] early = event("CFCDEVnotyet", "unmatched", "84900000099", Instant.now());
        delivery.receive(early, signature(early), "ZNS"); deliveryRepository.applyPending();
        assertThat(jdbc.queryForObject("SELECT status FROM zalo_delivery_inbox WHERE tracking_id='CFCDEVnotyet'", String.class)).isEqualTo("PENDING");
        var receipt = create("mismatch", 1); long id = job(receipt).id();
        var claim = outbox.claim(id, properties.getMode().name(), properties.getSessionKey(), 5);
        outbox.finish(claim, CustomerMessageProvider.Outcome.accepted("expected-message"));
        byte[] bad = event(claim.trackingId(), "wrong-message", "84900000099", Instant.now());
        delivery.receive(bad, signature(bad), "ZNS"); deliveryRepository.applyPending();
        byte[] badPhone = event(claim.trackingId(), "expected-message", "84900000100", Instant.now());
        delivery.receive(badPhone, signature(badPhone), "ZNS"); deliveryRepository.applyPending();
        assertThat(job(receipt).status()).isEqualTo("ACCEPTED");
        byte[] good = event(claim.trackingId(), "expected-message", "84900000099", Instant.now());
        delivery.receive(good, signature(good), "ZNS"); deliveryRepository.applyPending();
        assertThat(job(receipt).status()).isEqualTo("DELIVERED");
    }

    @Test
    void lateCallbackUpdatesOnlyItsAttemptAndPreventsAnotherRetry() throws Exception {
        development(); webhook();
        var receipt = create("late", 1); long id = job(receipt).id();
        var first = outbox.claim(id, properties.getMode().name(), properties.getSessionKey(), 5);
        outbox.finish(first, new CustomerMessageProvider.Outcome("FAILED_FINAL", "TEST_REJECTED", null));
        service.retry(receipt.id(), id, "retry-late", "Test late", admin);
        var next = outbox.claim(id, properties.getMode().name(), properties.getSessionKey(), 5);
        byte[] body = event(first.trackingId(), "late-first-message", "84900000099", Instant.now());
        delivery.receive(body, signature(body), "ZNS"); deliveryRepository.applyPending();
        assertThat(job(receipt).status()).isEqualTo("SENDING");
        outbox.finish(next, new CustomerMessageProvider.Outcome("FAILED_FINAL", "TEST_REJECTED", null));
        assertThat(outbox.attempts(id).stream().filter(a -> a.status().equals("DELIVERED"))).hasSize(1);
        assertThatThrownBy(() -> service.retry(receipt.id(), id, "retry-again", "Unsafe duplicate", admin)).hasMessageContaining("409");
    }

    @Test
    void webhookChecksRawSignatureTimeScopePayloadSizeAndIgnoresStaleAuthCookie() throws Exception {
        development(); webhook();
        byte[] body = event("CFCDEVsecurity", "security-message", "84900000099", Instant.now());
        String url = "/api/v1/webhooks/zbs";
        mvc.perform(post(url).cookie(new jakarta.servlet.http.Cookie("access_token", "expired-invalid"))
                .header("X-ZEvent-Signature", "mac=" + signature(body)).header("X-ZEvent-Server", "ZNS")
                .contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(post(url).contentType("application/json").content(body)).andExpect(status().isForbidden());
        byte[] modified = (new String(body, StandardCharsets.UTF_8) + " ").getBytes(StandardCharsets.UTF_8);
        mvc.perform(post(url).header("X-ZEvent-Signature", signature(body)).header("X-ZEvent-Server", "ZNS")
                .contentType("application/json").content(modified)).andExpect(status().isForbidden());
        var tree = json.readTree(body).deepCopy(); ((tools.jackson.databind.node.ObjectNode) tree).put("timestamp", "1000");
        byte[] stale = json.writeValueAsBytes(tree);
        mvc.perform(post(url).header("X-ZEvent-Signature", signature(stale)).header("X-ZEvent-Server", "ZNS")
                .contentType("application/json").content(stale)).andExpect(status().isForbidden());
        mvc.perform(post(url).contentType("application/json").content(new byte[16385])).andExpect(status().isPayloadTooLarge());
        mvc.perform(post("/api/v1/webhooks/other").with(csrf()).contentType("application/json").content(body)).andExpect(status().isUnauthorized());
    }

    @Test
    void statusPollingUsesMessageAndPhoneIsBoundedAndNeverAuthorizesResend() {
        development(); properties.setPollingEnabled(true); properties.setMaxStatusChecks(2);
        when(zalo.send(any())).thenReturn(CustomerMessageProvider.Outcome.accepted("poll-message"));
        var receipt = create("poll", 1); long id = job(receipt).id(); worker.process(id);
        var builder = RestClient.builder().baseUrl("https://business.openapi.zalo.me");
        var http = MockRestServiceServer.bindTo(builder).build();
        var checks = new ZaloDeliveryService(properties, deliveryRepository, tokens, json, builder.build());
        http.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://business.openapi.zalo.me/message/status?message_id=poll-message&phone=84900000099"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess("{\"error\":0,\"data\":{\"status\":-1}}", MediaType.APPLICATION_JSON));
        http.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything()).andRespond(request -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess("{\"error\":0,\"data\":{\"status\":0}}", MediaType.APPLICATION_JSON).createResponse(request);
        });
        checks.runOnce(); checks.runOnce();
        jdbc.update("UPDATE notification_delivery_attempts SET next_status_check_at=NULL WHERE outbox_id=?", id);
        checks.runOnce();
        jdbc.update("UPDATE notification_delivery_attempts SET next_status_check_at=NULL WHERE outbox_id=?", id);
        checks.runOnce(); worker.process(id);
        assertThat(job(receipt).status()).isEqualTo("ACCEPTED"); assertThat(outbox.attempts(id).getFirst().statusCheckCount()).isEqualTo(2);
        verify(zalo, times(1)).send(any()); http.verify();
    }

    @Test
    void deliveredStatusStoresEvidenceAndLaterNotFoundCannotDowngradeIt() {
        development(); properties.setPollingEnabled(true);
        when(zalo.send(any())).thenReturn(CustomerMessageProvider.Outcome.accepted("delivered-message"));
        var receipt = create("poll-delivered", 1); long id = job(receipt).id(); worker.process(id);
        var builder = RestClient.builder().baseUrl("https://business.openapi.zalo.me"); var http = MockRestServiceServer.bindTo(builder).build();
        http.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything()).andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                "{\"error\":0,\"data\":{\"status\":1,\"delivery_time\":\"" + Instant.now().toEpochMilli() + "\"}}", MediaType.APPLICATION_JSON));
        var original = deliveryRepository.dueChecks(properties.getAppId(), properties.getOaId(), properties.getMode().name(), 6).getFirst();
        new ZaloDeliveryService(properties, deliveryRepository, tokens, json, builder.build()).runOnce();
        deliveryRepository.checked(original, -1, null, "STATUS_-1");
        assertThat(job(receipt).status()).isEqualTo("DELIVERED");
        assertThat(outbox.attempts(id).getFirst().deliverySource()).isEqualTo("POLL"); http.verify();
    }

    @Test
    void managedTokenRefreshRotatesEncryptedPairOnceAcrossConcurrentWorkersAndModes() throws Exception {
        development(); managedTokens(); String key = tokens.scope();
        properties.setMode(CustomerNotificationProperties.Mode.ZALO_LIVE); assertThat(tokens.scope()).isEqualTo(key);
        properties.setMode(CustomerNotificationProperties.Mode.ZALO_DEVELOPMENT);
        String stored = jdbc.queryForObject("SELECT encrypted_pair FROM zalo_managed_tokens WHERE scope_key=?", String.class, key);
        assertThat(stored).doesNotContain("access-initial-test", "refresh-initial-test");
        jdbc.update("UPDATE zalo_managed_tokens SET access_expires_at=DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 1 MINUTE) WHERE scope_key=?", key);
        var builder = RestClient.builder().baseUrl("https://oauth.zaloapp.com"); var http = MockRestServiceServer.bindTo(builder).build();
        http.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://oauth.zaloapp.com/v4/oa/access_token"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("secret_key", "test-app-secret-not-real"))
                .andRespond(request -> {
                    assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                    return org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                            "{\"access_token\":\"access-next-test\",\"refresh_token\":\"refresh-next-test\",\"expires_in\":\"90000\"}", MediaType.APPLICATION_JSON).createResponse(request);
                });
        var refresher = new ZaloTokenService(properties, cipher, jdbc, manager, json, builder.build());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(refresher::refreshDue); var two = executor.submit(refresher::refreshDue);
            one.get(5, TimeUnit.SECONDS); two.get(5, TimeUnit.SECONDS);
        }
        assertThat(tokens.currentAccessToken()).isEqualTo("access-next-test"); assertThat(tokens.health().state()).isEqualTo("READY");
        http.verify();
        properties.setTokenEncryptionKey(Base64.getEncoder().encodeToString(new byte[32]).replace('A', 'B'));
        assertThat(tokens.currentAccessToken()).isEmpty(); assertThat(tokens.health().state()).isEqualTo("ENCRYPTION_UNAVAILABLE");
    }

    @Test
    void unknownRefreshResponseRequiresReauthorizationInsteadOfReusingRefreshToken() {
        development(); managedTokens();
        jdbc.update("UPDATE zalo_managed_tokens SET access_expires_at=UTC_TIMESTAMP(6) WHERE scope_key=?", tokens.scope());
        var builder = RestClient.builder().baseUrl("https://oauth.zaloapp.com"); var http = MockRestServiceServer.bindTo(builder).build();
        http.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything())
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withException(new java.io.IOException("test timeout")));
        var refresher = new ZaloTokenService(properties, cipher, jdbc, manager, json, builder.build());
        refresher.refreshDue(); refresher.refreshDue();
        assertThat(tokens.health().state()).isEqualTo("NEEDS_AUTH"); assertThat(tokens.currentAccessToken()).isEmpty(); http.verify();
    }

    @Test
    void tokenRotationDefersQueuedJobsWithoutConsumingSendBudgetOrDroppingThem() {
        development(); managedTokens();
        var receipt = create("rotation-queued", 1); long id = job(receipt).id();
        jdbc.update("UPDATE zalo_managed_tokens SET state='REFRESHING' WHERE scope_key=?", tokens.scope());
        worker.process(id);
        assertThat(job(receipt).status()).isEqualTo("QUEUED"); assertThat(outbox.attempts(id)).isEmpty();
        jdbc.update("UPDATE zalo_managed_tokens SET state='READY' WHERE scope_key=?", tokens.scope());
        when(zalo.send(any())).thenReturn(CustomerMessageProvider.Outcome.accepted("after-rotation"));
        worker.process(id); assertThat(job(receipt).status()).isEqualTo("ACCEPTED");
        verify(zalo, times(1)).send(any());
    }

    @Test
    void crashedRefreshLeaseIsNotReusedAndAdminProvisionIsAuditedWithoutReturningSecrets() throws Exception {
        development(); managedTokens();
        jdbc.update("UPDATE zalo_managed_tokens SET state='REFRESHING',lease_owner='crashed',lease_until=DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 1 SECOND) WHERE scope_key=?", tokens.scope());
        var builder = RestClient.builder().baseUrl("https://oauth.zaloapp.com"); var http = MockRestServiceServer.bindTo(builder).build();
        new ZaloTokenService(properties, cipher, jdbc, manager, json, builder.build()).refreshDue();
        assertThat(tokens.health().state()).isEqualTo("NEEDS_AUTH"); http.verify();
        String body = json.writeValueAsString(Map.of("accessToken", "reprovision-access-test", "refreshToken", "reprovision-refresh-test",
                "expiresIn", 90000, "requestId", "provision-" + unique, "reason", "Test recovery"));
        String url = "/api/v1/admin/customer-notifications/token";
        mvc.perform(post(url).with(csrf()).with(jwt().authorities(() -> "ROLE_RECEPTIONIST")).contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post(url).with(csrf()).with(jwt().jwt(t -> t.subject(admin.getEmail())).authorities(() -> "ROLE_ADMIN"))
                .contentType("application/json").content(body)).andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(tokens.currentAccessToken()).isEqualTo("reprovision-access-test");
        mvc.perform(get("/api/v1/admin/customer-notifications").with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("reprovision-access-test"))));
        assertThat(jdbc.queryForObject("SELECT request_hash FROM customer_notification_operation_audit WHERE request_id=?", String.class, "provision-" + unique))
                .matches("[a-f0-9]{64}");
    }

    @Test
    void adminControlsEnforceRoleCsrfIdempotencyMaskingAndAudit() throws Exception {
        var receipt = create("operations", 1);
        String base = "/api/v1/admin/customer-notifications";
        mvc.perform(get(base)).andExpect(status().isUnauthorized());
        mvc.perform(get(base).with(jwt().authorities(() -> "ROLE_RECEPTIONIST"))).andExpect(status().isForbidden());
        mvc.perform(get(base + "/deliveries").with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("84900000099"))));
        var actor = jwt().jwt(t -> t.subject(admin.getEmail())).authorities(() -> "ROLE_ADMIN");
        String body = "{\"paused\":true,\"requestId\":\"pause-" + unique + "\",\"reason\":\"Test pause\"}";
        var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder().issuer("cfc-lab-management")
                .subject(admin.getEmail()).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).claim("role", "ADMIN").build();
        String cookieToken = jwtEncoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(claims)).getTokenValue();
        mvc.perform(post(base + "/pause").cookie(new jakarta.servlet.http.Cookie("access_token", cookieToken))
                .contentType("application/json").content(body)).andExpect(status().isForbidden());
        var csrfCookie = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        mvc.perform(post(base + "/pause").cookie(new jakarta.servlet.http.Cookie("access_token", cookieToken), csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue()).contentType("application/json").content(body)).andExpect(status().isNoContent());
        mvc.perform(post(base + "/pause").with(actor).with(csrf()).contentType("application/json").content(body)).andExpect(status().isNoContent());
        worker.process(job(receipt).id()); assertThat(outbox.attempts(job(receipt).id())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM customer_notification_operation_audit WHERE actor_user_id=?", Long.class, admin.getId())).isEqualTo(1);
        mvc.perform(post(base + "/pause").with(actor).with(csrf()).contentType("application/json").content(body.replace("true", "false"))).andExpect(status().isConflict());
        operations.pause(false, "resume-" + unique, "Test resume", admin.getId()); worker.process(job(receipt).id());
        assertThat(job(receipt).status()).isEqualTo("SIMULATED");
    }

    @Test
    void liveRequiresExplicitGateManagedTokenTrackingAndDoesNotReplayDevelopmentQueue() {
        development(); var old = create("development-only", 1); managedTokens();
        properties.setMode(CustomerNotificationProperties.Mode.ZALO_LIVE);
        assertThat(service.settings().issues()).contains("LIVE_NOT_ENABLED", "DELIVERY_TRACKING_REQUIRED");
        properties.setLiveEnabled(true); properties.setPollingEnabled(true);
        assertThat(service.settings().ready()).isTrue();
        worker.process(job(old).id()); assertThat(job(old).reason()).isEqualTo("PROVIDER_MODE_CHANGED");
        var live = create("live-queued", 1); assertThat(job(live).mode()).isEqualTo("ZALO_LIVE");
        assertThat(jdbc.queryForObject("SELECT consent_version FROM receipt_notification_consents WHERE receipt_id=?", String.class, live.id())).isEqualTo("intake-zalo-v1");
        properties.setLiveEnabled(false); worker.process(job(live).id()); assertThat(job(live).status()).isEqualTo("BLOCKED");
        verifyNoInteractions(zalo);
    }

    @Test
    void uploadDoesNotCreateAnotherEventAndStatusApiMasksPhoneAndEnforcesRoles() throws Exception {
        var receipt = create("api", 1);
        receipts.attach(receipt.id(), new MockMultipartFile("file", "test.pdf", "application/pdf", "%PDF-test".getBytes()),
                ReceiptAttachmentCategory.FORM_3, "test", null, receptionist);
        assertThat(outbox.count(receipt.id())).isEqualTo(1);
        String url = "/api/v1/receipts/" + receipt.id() + "/notification-deliveries";
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(get(url).with(jwt().authorities(() -> "ROLE_TECHNICIAN"))).andExpect(status().isForbidden());
        mvc.perform(get(url).with(jwt().jwt(token -> token.subject(receptionist.getEmail())).authorities(() -> "ROLE_RECEPTIONIST")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.content[0].recipientMasked").value("••••••0099"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("84900000099"))));
        String retryUrl = url + "/" + job(receipt).id() + "/retry";
        mvc.perform(post(retryUrl).with(csrf()).with(jwt().authorities(() -> "ROLE_RECEPTIONIST"))
                .contentType("application/json").content("{\"reason\":\"Test\",\"requestId\":\"test\"}"))
                .andExpect(status().isForbidden());
    }
}
