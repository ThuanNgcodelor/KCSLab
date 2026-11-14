package com.example.app.auth.repository;

import com.example.app.auth.entity.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Long> {
    Optional<RefreshSession> findByTokenHash(String tokenHash);
    void deleteAllByUser_Id(Long userId);
}
