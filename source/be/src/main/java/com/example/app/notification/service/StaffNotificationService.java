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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

@Service
public class StaffNotificationService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "readAt");

    private final StaffNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StaffNotificationService(StaffNotificationRepository notificationRepository,
                                    UserRepository userRepository,
                                    ApplicationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public StaffNotificationPageResponse findFor(User viewer, int page, int size,
                                                  String sort, boolean unreadOnly) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Trang phải từ 0 và kích thước trang từ 1 đến " + MAX_PAGE_SIZE);
        }
        NotificationSort parsedSort = parseSort(sort);
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(parsedSort.direction(), parsedSort.field()).and(Sort.by(Sort.Direction.DESC, "id")));
        Page<StaffNotification> result = unreadOnly
                ? notificationRepository.findByRecipient_IdAndReadAtIsNull(viewer.getId(), pageable)
                : notificationRepository.findByRecipient_Id(viewer.getId(), pageable);
        return new StaffNotificationPageResponse(result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast(), parsedSort.canonical(),
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
    public void notifyRoles(Collection<Role> roles, User excludedUser,
                            StaffNotificationType type, String title, String message,
                            String targetUrl, String entityType, Long entityId) {
        notifyUsers(userRepository.findByRoleInAndEnabledTrue(roles), excludedUser,
                type, title, message, targetUrl, entityType, entityId);
    }

    @Transactional
    public void notifyUsers(Collection<User> recipients, User excludedUser,
                            StaffNotificationType type, String title, String message,
                            String targetUrl, String entityType, Long entityId) {
        for (User recipient : recipients) {
            if (excludedUser != null && excludedUser.getId().equals(recipient.getId())) continue;
            String dedupeKey = "%s:%s:%s:%d".formatted(type, entityType, entityId, recipient.getId());
            if (notificationRepository.existsByDedupeKey(dedupeKey)) continue;
            StaffNotification saved = notificationRepository.save(new StaffNotification(
                    recipient, type, title, message, targetUrl, entityType, entityId, dedupeKey));
            eventPublisher.publishEvent(new StaffNotificationCreatedEvent(
                    recipient.getEmail(), toResponse(saved)));
        }
    }

    public StaffNotificationResponse toResponse(StaffNotification notification) {
        return new StaffNotificationResponse(notification.getId(), notification.getType(),
                notification.getTitle(), notification.getMessage(), notification.getTargetUrl(),
                notification.getEntityType(), notification.getEntityId(), notification.getReadAt(),
                notification.getCreatedAt());
    }

    private NotificationSort parseSort(String rawSort) {
        String value = rawSort == null || rawSort.isBlank() ? "createdAt,desc" : rawSort.trim();
        String[] parts = value.split(",", -1);
        if (parts.length != 2 || !SORT_FIELDS.contains(parts[0].trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sort thông báo không hợp lệ");
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(parts[1].trim());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chiều sort chỉ nhận asc hoặc desc");
        }
        String field = parts[0].trim();
        return new NotificationSort(field, direction,
                field + "," + direction.name().toLowerCase(Locale.ROOT));
    }

    private record NotificationSort(String field, Sort.Direction direction, String canonical) {}
}
