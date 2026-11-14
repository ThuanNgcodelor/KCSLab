package com.example.app.auth.service;

import java.time.Duration;
import java.util.Optional;

public interface RefreshSessionStore {
    void save(String tokenHash, Long userId, String email, boolean remembered, Duration ttl);
    Optional<SessionData> consume(String tokenHash);
    void delete(String tokenHash);
    void deleteAllForUser(Long userId);

    record SessionData(Long userId, String email, boolean remembered) { }
}
