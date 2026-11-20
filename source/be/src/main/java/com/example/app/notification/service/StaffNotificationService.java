package com.example.app.notification.service;

import com.example.app.notification.dto.StaffNotificationPageResponse;
import com.example.app.notification.dto.StaffNotificationResponse;
import com.example.app.notification.entity.StaffNotification;
import com.example.app.notification.entity.StaffNotificationType;
import com.example.app.notification.event.StaffNotificationCreatedEvent;
import com.example.app.notification.repository.StaffNotificationRepository;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import com.example.app.user.service.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collection;

@Service
public class StaffNotificationService {

    private final StaffNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserService userService;

    public StaffNotificationService(StaffNotificationRepository notificationRepository,
                                    UserRepository userRepository,
                                    ApplicationEventPublisher eventPublisher,
                                    UserService userService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.userService = userService;
    }

    private static final java.util.Map<String, String> LIST_SORT = java.util.Map.of(
            "createdAt", "createdAt", "type", "type", "title", "title", "readAt", "readAt");

    @Transactional(readOnly = true)
    public StaffNotificationPageResponse findFor(User viewer, int page, int size, String sort, boolean unreadOnly) {
        return findFor(viewer, page, size, sort, unreadOnly, null, null, null, null);
    }

    /** Server-side search, type filter and Vietnam-date range; the unread counter ignores the filters. */
    @Transactional(readOnly = true)
    public StaffNotificationPageResponse findFor(User viewer, int page, int size, String sort, boolean unreadOnly,
                                                 String q, StaffNotificationType type,
                                                 java.time.LocalDate from, java.time.LocalDate to) {
        com.example.app.common.query.ListQuery.requireOrderedRange(from, to);
        var parsed = com.example.app.common.query.ListQuery.parseSort(sort, LIST_SORT, "createdAt,desc");
        org.springframework.data.jpa.domain.Specification<StaffNotification> spec =
                (root, query, builder) -> builder.equal(root.get("recipient").get("id"), viewer.getId());
        if (unreadOnly) spec = spec.and((root, query, builder) -> builder.isNull(root.get("readAt")));
        if (type != null) spec = spec.and((root, query, builder) -> builder.equal(root.get("type"), type));
        if (from != null) {
            var start = com.example.app.common.query.ListQuery.startOfDay(from);
            spec = spec.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), start));
        }
        if (to != null) {
            var end = com.example.app.common.query.ListQuery.endExclusive(to);
            spec = spec.and((root, query, builder) -> builder.lessThan(root.get("createdAt"), end));
        }
        if (com.example.app.common.query.ListQuery.hasText(q)) {
            String pattern = com.example.app.common.query.ListQuery.contains(q);
            spec = spec.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("title")), pattern, '\\'),
                    builder.like(builder.lower(root.get("message")), pattern, '\\')));
        }
        Page<StaffNotification> result = notificationRepository.findAll(spec,
                com.example.app.common.query.ListQuery.page(page, size, parsed));
        return new StaffNotificationPageResponse(result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast(), parsed.canonical(),
                notificationRepository.countByRecipient_IdAndReadAtIsNull(viewer.getId()));
    }

    @Transactional
    public StaffNotificationResponse markRead(Long id, User viewer) {
        StaffNotification notification = notificationRepository.findByIdAndRecipient_Id(id, viewer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy thông báo"));
        notification.markRead();
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public int markAllRead(User viewer) {
        return notificationRepository.markAllRead(viewer.getId(), Instant.now());
    }

    @Transactional
    public void notifyRoles(Collection<Role> roles, User actor,
                            StaffNotificationType type, String title, String message,
                            String targetUrl, String entityType, Long entityId) {
        notifyUsers(userRepository.findByRoleInAndEnabledTrue(roles), actor,
                type, title, message, targetUrl, entityType, entityId);
    }

    @Transactional
    public void notifyUsers(Collection<User> recipients, User actor,
                            StaffNotificationType type, String title, String message,
                            String targetUrl, String entityType, Long entityId) {
        for (User recipient : recipients) {
            if (actor != null && actor.getId().equals(recipient.getId())) continue;
            String dedupeKey = "%s:%s:%s:%d".formatted(type, entityType, entityId, recipient.getId());
            if (notificationRepository.existsByDedupeKey(dedupeKey)) continue;
            StaffNotification saved = notificationRepository.save(new StaffNotification(
                    recipient, actor, type, title, message, targetUrl, entityType, entityId, dedupeKey));
            eventPublisher.publishEvent(new StaffNotificationCreatedEvent(
                    recipient.getEmail(), toResponse(saved)));
        }
    }

    public StaffNotificationResponse toResponse(StaffNotification notification) {
        User actor = notification.getActor();
        return new StaffNotificationResponse(notification.getId(), notification.getType(),
                notification.getTitle(), notification.getMessage(), notification.getTargetUrl(),
                notification.getEntityType(), notification.getEntityId(), notification.getReadAt(),
                notification.getCreatedAt(), actor == null ? null
                : new StaffNotificationResponse.Actor(actor.getDisplayName(), userService.avatarUrl(actor)));
    }

}
