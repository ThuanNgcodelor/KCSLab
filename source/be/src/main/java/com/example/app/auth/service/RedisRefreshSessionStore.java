package com.example.app.auth.service;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Profile("mysql")
public class RedisRefreshSessionStore implements RefreshSessionStore {
    private static final String SESSION_PREFIX = "cfc:auth:refresh:";
    private static final String USER_PREFIX = "cfc:auth:user:";
    private static final Duration USER_INDEX_TTL = Duration.ofDays(31);
    private static final DefaultRedisScript<String> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); "
                    + "if value then redis.call('DEL', KEYS[1]); end; return value;",
            String.class);
    private final StringRedisTemplate redis;

    public RedisRefreshSessionStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void save(String tokenHash, Long userId, String email, boolean remembered, Duration ttl) {
        execute(() -> {
            redis.opsForValue().set(sessionKey(tokenHash), encode(userId, email, remembered), ttl);
            redis.opsForSet().add(userKey(userId), tokenHash);
            // The index must outlive every supported session. Using the current
            // session TTL here could shorten a remembered session's revocation index.
            redis.expire(userKey(userId), USER_INDEX_TTL);
            return null;
        });
    }

    @Override
    public Optional<SessionData> consume(String tokenHash) {
        return execute(() -> {
            // Redis bundled with Laragon is currently 5.x (GETDEL requires 6.2+).
            // Lua keeps consume atomic, preserving one-time refresh-token rotation.
            String value = redis.execute(CONSUME_SCRIPT, List.of(sessionKey(tokenHash)));
            if (value == null) return Optional.empty();
            SessionData session = decode(value);
            redis.opsForSet().remove(userKey(session.userId()), tokenHash);
            return Optional.of(session);
        });
    }

    @Override
    public void delete(String tokenHash) {
        consume(tokenHash);
    }

    @Override
    public void deleteAllForUser(Long userId) {
        execute(() -> {
            Set<String> hashes = redis.opsForSet().members(userKey(userId));
            if (hashes != null && !hashes.isEmpty()) {
                var keys = hashes.stream().map(this::sessionKey)
                        .collect(Collectors.toCollection(ArrayList::new));
                redis.delete(keys);
            }
            redis.delete(userKey(userId));
            return null;
        });
    }

    private String encode(Long userId, String email, boolean remembered) {
        return userId + "\n" + (remembered ? "1" : "0") + "\n" + email;
    }

    private SessionData decode(String value) {
        String[] parts = value.split("\n", 3);
        if (parts.length != 3) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh session không hợp lệ");
        }
        try {
            return new SessionData(Long.parseLong(parts[0]), parts[2], "1".equals(parts[1]));
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh session không hợp lệ", exception);
        }
    }

    private String sessionKey(String hash) { return SESSION_PREFIX + hash; }
    private String userKey(Long userId) { return USER_PREFIX + userId; }

    private <T> T execute(RedisOperation<T> operation) {
        try {
            return operation.run();
        } catch (DataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Dịch vụ duy trì đăng nhập đang tạm gián đoạn", exception);
        }
    }

    @FunctionalInterface
    private interface RedisOperation<T> { T run(); }
}
