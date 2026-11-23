package com.example.app.notification.customer.service;

import com.example.app.notification.customer.config.CustomerNotificationProperties;
import com.example.app.notification.customer.provider.CustomerMessageProvider;
import com.example.app.notification.customer.provider.FakeCustomerMessageProvider;
import com.example.app.notification.customer.provider.ZaloTemplateProvider;
import com.example.app.notification.customer.provider.ZaloLiveTemplateProvider;
import com.example.app.notification.customer.repository.CustomerNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class CustomerNotificationWorker {
    private static final Logger LOG = LoggerFactory.getLogger(CustomerNotificationWorker.class);
    private final CustomerNotificationProperties properties;
    private final CustomerNotificationRepository repository;
    private final IntakeCustomerNotificationService service;
    private final FakeCustomerMessageProvider fake;
    private final ZaloTemplateProvider zalo;
    private final ZaloLiveTemplateProvider live;
    private final CustomerNotificationOperations operations;

    public CustomerNotificationWorker(CustomerNotificationProperties properties, CustomerNotificationRepository repository,
                                      IntakeCustomerNotificationService service, FakeCustomerMessageProvider fake, ZaloTemplateProvider zalo,
                                      ZaloLiveTemplateProvider live, CustomerNotificationOperations operations) {
        this.properties = properties; this.repository = repository; this.service = service; this.fake = fake; this.zalo = zalo;
        this.live = live; this.operations = operations;
    }

    @Scheduled(fixedDelayString = "${app.customer-notifications.worker-delay-ms:5000}")
    public void scheduled() {
        if (!properties.isWorkerEnabled()) return;
        try { runOnce(); }
        catch (RuntimeException exception) { LOG.warn("Customer notification worker failed; pending leases will be reconciled"); }
    }

    public void runOnce() {
        if (properties.getMode() == CustomerNotificationProperties.Mode.OFF || operations.paused()) return;
        repository.recoverExpired();
        for (Long id : repository.due(properties.getMode().name())) process(id);
    }

    public void process(long id) {
        if (properties.getMode() == CustomerNotificationProperties.Mode.OFF || operations.paused()) return;
        var job = repository.find(id);
        if (job == null) return;
        String reason = service.guard(job);
        if (reason != null) {
            if (!deferred(reason)) repository.block(id, reason);
            return;
        }
        var claim = repository.claim(id, properties.getMode().name(), service.sessionKey(), properties.getMaxCallsPerSession());
        if (claim == null) return;
        // Re-check after claiming. No receipt transaction or database lock is held during the HTTP request.
        reason = service.guard(claim.job());
        CustomerMessageProvider.Outcome outcome;
        if (reason != null) outcome = new CustomerMessageProvider.Outcome(deferred(reason) ? "RETRY_WAIT" : "BLOCKED", reason, null);
        else {
            try {
                CustomerMessageProvider provider = switch (properties.getMode()) {
                    case FAKE -> fake;
                    case ZALO_DEVELOPMENT -> zalo;
                    case ZALO_LIVE -> live;
                    case OFF -> throw new IllegalStateException("SENDING_DISABLED");
                };
                outcome = provider.send(new CustomerMessageProvider.Message(job.phone(), job.templateId(), job.payload(), claim.trackingId()));
                if (outcome.status().equals("RETRY_WAIT") && !deferred(outcome.code()) && (properties.getMode() == CustomerNotificationProperties.Mode.ZALO_DEVELOPMENT
                        || job.attemptCount() >= 4)) {
                    outcome = new CustomerMessageProvider.Outcome("FAILED_FINAL", outcome.code(), outcome.messageId());
                }
            } catch (RuntimeException exception) { outcome = CustomerMessageProvider.Outcome.unknown("WORKER_INTERRUPTED"); }
        }
        repository.finish(claim, outcome);
    }

    private boolean deferred(String reason) { return "TOKEN_NOT_READY".equals(reason) || "SENDING_PAUSED".equals(reason); }
}
