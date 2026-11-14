package com.example.app.notification.repository;

import com.example.app.notification.entity.StaffNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface StaffNotificationRepository extends JpaRepository<StaffNotification, Long> {
    Page<StaffNotification> findByRecipient_Id(Long recipientId, Pageable pageable);
    Page<StaffNotification> findByRecipient_IdAndReadAtIsNull(Long recipientId, Pageable pageable);
    long countByRecipient_IdAndReadAtIsNull(Long recipientId);
    Optional<StaffNotification> findByIdAndRecipient_Id(Long id, Long recipientId);
    boolean existsByDedupeKey(String dedupeKey);

    @Modifying
    @Query("""
            update StaffNotification notification
            set notification.readAt = :readAt
            where notification.recipient.id = :recipientId and notification.readAt is null
            """)
    int markAllRead(@Param("recipientId") Long recipientId, @Param("readAt") Instant readAt);
}
