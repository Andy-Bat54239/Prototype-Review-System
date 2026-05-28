package com.auca.prbs.auth.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-process refresh-token denylist. For multi-instance auth-service, swap for
 * a Redis-backed impl (the only thing keeping this in-process is convenience).
 */
@Service
public class TokenRevocationStore {

    private final ConcurrentMap<String, Instant> revoked = new ConcurrentHashMap<>();

    public void revoke(String jti, Instant expiresAt) {
        if (jti == null || expiresAt == null || expiresAt.isBefore(Instant.now())) return;
        revoked.put(jti, expiresAt);
    }

    public boolean isRevoked(String jti) {
        if (jti == null) return false;
        Instant exp = revoked.get(jti);
        if (exp == null) return false;
        if (exp.isBefore(Instant.now())) {
            revoked.remove(jti, exp);
            return false;
        }
        return true;
    }
}
