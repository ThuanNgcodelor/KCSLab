package com.example.app.publicapi;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window counter with a bounded key space. State is per application instance; if the
 * backend is ever scaled horizontally the counters must move to Redis (needs confirmation with infra).
 */
final class PublicLookupRateLimiter {
    private static final int MAX_KEYS = 50_000;

    private final int maxEvents;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Long>> events = new ConcurrentHashMap<>();

    PublicLookupRateLimiter(int maxEvents, Duration window, Clock clock) {
        this.maxEvents = maxEvents; this.window = window; this.clock = clock;
    }

    /** True when the key has already used up its allowance inside the window. */
    boolean isBlocked(String key) {
        Deque<Long> deque = events.get(key);
        if (deque == null) return false;
        synchronized (deque) {
            prune(deque);
            return deque.size() >= maxEvents;
        }
    }

    void record(String key) {
        if (events.size() >= MAX_KEYS) evictExpired();
        if (events.size() >= MAX_KEYS && !events.containsKey(key)) return; // fail closed is handled by caller limits
        Deque<Long> deque = events.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            prune(deque);
            deque.addLast(clock.millis());
        }
    }

    void clear(String key) { events.remove(key); }

    /** Seconds until the oldest event leaves the window (at least 1). */
    long retryAfterSeconds(String key) {
        Deque<Long> deque = events.get(key);
        if (deque == null) return 1;
        synchronized (deque) {
            Long oldest = deque.peekFirst();
            if (oldest == null) return 1;
            return Math.max(1, (oldest + window.toMillis() - clock.millis() + 999) / 1000);
        }
    }

    private void prune(Deque<Long> deque) {
        long cutoff = clock.millis() - window.toMillis();
        while (!deque.isEmpty() && deque.peekFirst() <= cutoff) deque.pollFirst();
    }

    private void evictExpired() {
        events.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                prune(entry.getValue());
                return entry.getValue().isEmpty();
            }
        });
    }
}
