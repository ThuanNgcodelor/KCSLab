package com.example.app.notification.customer.service;

import com.example.app.intake.dto.CreateReceiptRequest;
import com.example.app.intake.entity.Receipt;
import com.example.app.intake.repository.ReceiptRepository;
import com.example.app.notification.customer.config.CustomerNotificationProperties;
import com.example.app.notification.customer.dto.CustomerNotificationRequest;
import com.example.app.notification.customer.repository.CustomerNotificationRepository;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class IntakeCustomerNotificationService {
    public static final String CONSENT_VERSION = "intake-zalo-test-v1";
    public static final String CONSENT_TEXT = "Tôi đồng ý nhận tin xác nhận tiếp nhận của phiếu này qua Zalo tại số điện thoại đã xác nhận.";
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final CustomerNotificationProperties properties;
    private final IntakeTemplateMapper mapper;
    private final CustomerNotificationRepository repository;
    private final ReceiptRepository receipts;
    private final ZaloTokenService tokens;
    private final CustomerNotificationOperations operations;

    public IntakeCustomerNotificationService(CustomerNotificationProperties properties, IntakeTemplateMapper mapper,
                                             CustomerNotificationRepository repository, ReceiptRepository receipts,
                                             ZaloTokenService tokens, CustomerNotificationOperations operations) {
        this.properties = properties;
        this.mapper = mapper;
        this.repository = repository;
        this.receipts = receipts;
        this.tokens = tokens;
        this.operations = operations;
    }

    public void validate(CustomerNotificationRequest request) {
        if (request == null || !request.enabled()) return;
        RecipientPhone.normalize(request.recipientPhone());
        if (!request.consentAccepted()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần xác nhận đồng ý nhận thông báo Zalo");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Receipt receipt, CreateReceiptRequest request, User actor) {
        var notification = request.customerNotification();
        validate(notification);
        if (notification == null || !notification.enabled()) return;
        String phone = RecipientPhone.normalize(notification.recipientPhone());
        var mode = properties.getMode();
        String reason = mode == CustomerNotificationProperties.Mode.OFF ? "SENDING_DISABLED"
                : isZaloMode() && !allowed(phone) ? "OUTSIDE_TEST_ALLOWLIST"
                : !settings().ready() ? "CONFIGURATION_INCOMPLETE" : null;
        String payload = "{}";
        if (reason == null) {
            try {
                var received = receipt.getReceivedAt().atZone(ZONE);
                payload = mapper.render(Map.of("requestNumber", receipt.getRequestNumber(), "receiptCode", receipt.getReceiptCode(),
                        "customerName", request.customerName().trim(), "sampleCount", Integer.toString(receipt.getSamples().size()),
                        "receivedAt", received.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        "receivedDate", received.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))), mode == CustomerNotificationProperties.Mode.FAKE);
            } catch (IllegalArgumentException exception) {
                reason = "TEMPLATE_DATA_INVALID";
            }
        }
        repository.record(receipt.getId(), phone, consentVersion(), actor.getId(), mode.name(), sessionKey(),
                properties.getAppId(), properties.getOaId(), mode == CustomerNotificationProperties.Mode.FAKE ? "FAKE_INTAKE_V1" : properties.getTemplateId(),
                properties.getTemplateVersion(), receipt.getRequestNumber(), payload,
                mode == CustomerNotificationProperties.Mode.OFF ? "CANCELLED" : reason == null ? "QUEUED" : "BLOCKED", reason);
    }

    public Settings settings() {
        List<String> issues = new ArrayList<>();
        if (properties.getMode() == CustomerNotificationProperties.Mode.OFF) issues.add("SENDING_DISABLED");
        if (operations.paused()) issues.add("SENDING_PAUSED");
        if (isZaloMode()) {
            if (properties.getAppId().isBlank() || properties.getAppId().length() > 80) issues.add("APP_ID_REQUIRED");
            if (properties.getOaId().isBlank() || properties.getOaId().length() > 80) issues.add("OA_ID_REQUIRED");
            if (tokens.currentAccessToken().isBlank()) issues.add("ACCESS_TOKEN_REQUIRED");
            if (properties.getTemplateId().isBlank() || properties.getTemplateId().length() > 100) issues.add("TEMPLATE_ID_REQUIRED");
            if (properties.getTemplateVersion().isBlank() || properties.getTemplateVersion().length() > 40) issues.add("TEMPLATE_VERSION_INVALID");
            if (!properties.getSessionKey().matches("[A-Za-z0-9_-]{1,80}")) issues.add("TEST_SESSION_REQUIRED");
            if (properties.getMaxCallsPerSession() < 1 || properties.getMaxCallsPerSession() > (properties.getMode() == CustomerNotificationProperties.Mode.ZALO_LIVE ? 100000 : 20)) issues.add("TEST_CALL_LIMIT_INVALID");
            try {
                var phones = Arrays.stream(properties.getAllowedPhones().split(",")).filter(value -> !value.isBlank()).map(RecipientPhone::normalize).toList();
                if (phones.isEmpty() && !(properties.getMode() == CustomerNotificationProperties.Mode.ZALO_LIVE && properties.isLiveAllowAllRecipients())) issues.add("TEST_RECIPIENT_REQUIRED");
            } catch (RuntimeException exception) { issues.add("TEST_RECIPIENT_INVALID"); }
            try { mapper.bindings(); } catch (IllegalArgumentException exception) { issues.add("TEMPLATE_BINDINGS_INVALID"); }
            if (properties.getMode() == CustomerNotificationProperties.Mode.ZALO_LIVE) {
                if (!properties.isLiveEnabled()) issues.add("LIVE_NOT_ENABLED");
                if (!properties.isTokenRefreshEnabled() || properties.getAppSecret().isBlank() || !tokens.health().state().equals("READY")) issues.add("MANAGED_TOKEN_REQUIRED");
                if (!properties.isPollingEnabled()) issues.add("DELIVERY_TRACKING_REQUIRED");
            }
        }
        return new Settings(properties.getMode().name(), issues.isEmpty(), List.copyOf(issues), consentVersion(), CONSENT_TEXT);
    }

    private String consentVersion() { return properties.getMode() == CustomerNotificationProperties.Mode.ZALO_LIVE ? "intake-zalo-v1" : CONSENT_VERSION; }

    public boolean allowed(String phone) {
        if (properties.getMode() == CustomerNotificationProperties.Mode.ZALO_LIVE && properties.isLiveAllowAllRecipients()) return true;
        try {
            return Arrays.stream(properties.getAllowedPhones().split(",")).filter(value -> !value.isBlank())
                    .map(RecipientPhone::normalize).toList().contains(phone);
        } catch (RuntimeException exception) { return false; }
    }

    public String sessionKey() {
        return isZaloMode() ? properties.getSessionKey() : "FAKE";
    }

    private boolean isZaloMode() { return properties.getMode() == CustomerNotificationProperties.Mode.ZALO_DEVELOPMENT || properties.getMode() == CustomerNotificationProperties.Mode.ZALO_LIVE; }

    public String guard(CustomerNotificationRepository.Job job) {
        if (!properties.getMode().name().equals(job.mode())) return "PROVIDER_MODE_CHANGED";
        if (operations.paused()) return "SENDING_PAUSED";
        if (isZaloMode()) {
            String tokenState = tokens.health().state();
            if (tokenState.equals("REFRESHING") || properties.getMode() == CustomerNotificationProperties.Mode.ZALO_LIVE && !tokenState.equals("READY")) return "TOKEN_NOT_READY";
        }
        if (!settings().ready()) return "CONFIGURATION_INCOMPLETE";
        if (repository.hasDeliveredAttempt(job.id())) return "ALREADY_DELIVERED";
        if (isZaloMode()) {
            if (!sessionKey().equals(job.sessionKey()) || !properties.getAppId().equals(job.appId()) || !properties.getOaId().equals(job.oaId())) return "TEST_SCOPE_CHANGED";
            if (!allowed(job.phone())) return "OUTSIDE_TEST_ALLOWLIST";
        }
        return null;
    }

    @Transactional(readOnly = true)
    public DeliveryPage list(long receiptId, int page, int size, User actor) {
        requireReader(receiptId, actor);
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phân trang không hợp lệ");
        long total = repository.count(receiptId);
        var content = repository.jobs(receiptId, page, size).stream().map(this::response).toList();
        int pages = (int) ((total + size - 1) / size);
        return new DeliveryPage(content, page, size, total, pages, page == 0, page + 1 >= pages, "createdAt,desc");
    }

    @Transactional
    public Delivery retry(long receiptId, long id, String requestId, String reason, User actor) {
        if (actor.getRole() != Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Admin được thử gửi lại");
        requireReader(receiptId, actor);
        var job = repository.lock(receiptId, id);
        if (job == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo của phiếu");
        if (repository.hasRetry(id, requestId)) return response(job);
        if (!job.status().equals("FAILED_FINAL") || job.attemptCount() >= 5 || guard(job) != null
                || job.reason() != null && List.of("-139", "-141").contains(job.reason())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Thông báo này chưa đủ điều kiện thử gửi lại");
        }
        repository.queueRetry(id, requestId, reason.trim(), actor.getId());
        return response(repository.find(id));
    }

    private void requireReader(long receiptId, User actor) {
        if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.RECEPTIONIST) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền xem thông báo khách");
        if (!receipts.existsById(receiptId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu tiếp nhận");
    }

    private Delivery response(CustomerNotificationRepository.Job job) {
        return new Delivery(job.id(), job.requestNumber(), RecipientPhone.mask(job.phone()), job.mode(), job.status(), job.reason(),
                job.createdAt(), repository.attempts(job.id()));
    }

    public record Settings(String mode, boolean ready, List<String> issues, String consentVersion, String consentText) {}
    public record Delivery(long id, String requestNumber, String recipientMasked, String mode, String status, String reasonCode,
                           Instant createdAt, List<CustomerNotificationRepository.Attempt> attempts) {}
    public record DeliveryPage(List<Delivery> content, int page, int size, long totalElements, int totalPages, boolean first, boolean last, String sort) {}
}
