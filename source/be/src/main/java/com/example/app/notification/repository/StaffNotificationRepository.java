package com.example.app.notification.repository;

import com.example.app.notification.entity.StaffNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.Collection;
import com.example.app.notification.entity.StaffNotificationType;

public interface StaffNotificationRepository extends JpaRepository<StaffNotification, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<StaffNotification> {
    Page<StaffNotification> findByRecipient_Id(Long recipientId, Pageable pageable);
    Page<StaffNotification> findByRecipient_IdAndReadAtIsNull(Long recipientId, Pageable pageable);
    @Override
    @EntityGraph(attributePaths = "actor")
    Page<StaffNotification> findAll(Specification<StaffNotification> spec, Pageable pageable);

    long countByRecipient_IdAndReadAtIsNull(Long recipientId);
    long countByRecipient_IdAndTypeInAndReadAtIsNull(Long recipientId, Collection<StaffNotificationType> types);
    Optional<StaffNotification> findByIdAndRecipient_Id(Long id, Long recipientId);
    boolean existsByDedupeKey(String dedupeKey);

    @Modifying
    @Query("""
            update StaffNotification notification
            set notification.readAt = :readAt
            where notification.recipient.id = :recipientId and notification.readAt is null and notification.type in :types
            """)
    int markAllRead(@Param("recipientId") Long recipientId, @Param("readAt") Instant readAt,
                    @Param("types") Collection<StaffNotificationType> types);
}
