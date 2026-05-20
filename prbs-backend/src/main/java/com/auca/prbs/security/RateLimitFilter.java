package com.auca.prbs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-client fixed-window rate limiter for the public {@code /api/v1/auth/**} endpoints.
 * Caps each client at {@link #MAX_REQUESTS_PER_MINUTE} requests per 60-second window;
 * the (N+1)-th request returns {@code 429 Too Many Requests} with a {@code Retry-After}
 * header.
 *
 * Intentionally in-memory and unsynchronized across replicas — that's fine for the
 * single-instance prototype. A production deploy on multiple instances should swap
 * this for a shared store (Redis token bucket, Bucket4j with a distributed backend).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    public static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_MS = 60_000L;
    private static final String PROTECTED_PATH_PREFIX = "/api/v1/auth/";

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    private static final class Window {
        int count;
        long windowStart;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith(PROTECTED_PATH_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        if (isLimited(clientKey(request))) {
            response.setStatus(429); // jakarta.servlet has no SC_TOO_MANY_REQUESTS constant
            response.setHeader("Retry-After", "60");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isLimited(String key) {
        long now = System.currentTimeMillis();
        Window updated = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart >= WINDOW_MS) {
                Window fresh = new Window();
                fresh.count = 1;
                fresh.windowStart = now;
                return fresh;
            }
            existing.count++;
            return existing;
        });
        return updated.count > MAX_REQUESTS_PER_MINUTE;
    }

    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Clear all per-client counters. Visible for tests; do not call from production code. */
    public void reset() {
        windows.clear();
    }
}
