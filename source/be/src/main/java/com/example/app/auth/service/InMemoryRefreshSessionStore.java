package com.example.app.auth.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("!mysql")
public class InMemoryRefreshSessionStore implements RefreshSessionStore {
    private final ConcurrentHashMap<String, StoredSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<String>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void save(String tokenHash, Long userId, String email, boolean remembered, Duration ttl) {
        sessions.put(tokenHash, new StoredSession(
                new SessionData(userId, email, remembered), Instant.now().plus(ttl)));
        userSessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(tokenHash);
    }

    @Override
    public Optional<SessionData> consume(String tokenHash) {
        StoredSession stored = sessions.remove(tokenHash);
        if (stored == null) return Optional.empty();
        Set<String> hashes = userSessions.get(stored.data().userId());
        if (hashes != null) hashes.remove(tokenHash);
        return stored.expiresAt().isAfter(Instant.now()) ? Optional.of(stored.data()) : Optional.empty();
    }

    @Override
    public void delete(String tokenHash) {
        consume(tokenHash);
    }

    @Override
    public void deleteAllForUser(Long userId) {
        Set<String> hashes = userSessions.remove(userId);
        if (hashes != null) hashes.forEach(sessions::remove);
    }

    private record StoredSession(SessionData data, Instant expiresAt) { }
}
