package com.auca.prbs.auth.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-email send-otp rate limit. Same algorithm as the monolith's
 * RateLimitFilter, just scoped to one bucket since this service owns one path.
 */
@Service
public class RateLimiter {

    public static final int MAX_SEND_OTP_PER_EMAIL_PER_MINUTE = 5;
    private static final long WINDOW_MS = 60_000L;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    private static final class Window {
        int count;
        long windowStart;
    }

    public boolean tryEmailQuota(String email) {
        if (email == null || email.isBlank()) return true;
        long now = System.currentTimeMillis();
        Window updated = windows.compute(email.toLowerCase(), (k, existing) -> {
            if (existing == null || now - existing.windowStart >= WINDOW_MS) {
                Window fresh = new Window();
                fresh.count = 1;
                fresh.windowStart = now;
                return fresh;
            }
            existing.count++;
            return existing;
        });
        return updated.count <= MAX_SEND_OTP_PER_EMAIL_PER_MINUTE;
    }

    public void reset() { windows.clear(); }
}
