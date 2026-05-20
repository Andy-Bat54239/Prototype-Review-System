package com.auca.prbs.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Single-instance, process-local revocation store. Entries vanish on JVM restart.
 *
 * <p>Acceptable for the prototype's single-replica deploy. A multi-instance
 * deploy must swap in a shared backend (Redis, JPA) — implement
 * {@link TokenRevocationStore} and register the new bean as {@code @Primary}.
 */
@Service
public class InMemoryTokenRevocationStore implements TokenRevocationStore {

    private final ConcurrentMap<String, Instant> revoked = new ConcurrentHashMap<>();

    @Override
    public void revoke(String jti, Instant expiresAt) {
        if (jti == null || expiresAt == null) return;
        if (expiresAt.isBefore(Instant.now())) return; // already expired; no point storing
        revoked.put(jti, expiresAt);
    }

    @Override
    public boolean isRevoked(String jti) {
        if (jti == null) return false;
        Instant expiresAt = revoked.get(jti);
        if (expiresAt == null) return false;
        if (expiresAt.isBefore(Instant.now())) {
            // Lazy cleanup: the entry is past its natural expiry, so its existence
            // here no longer matters. Remove it and report not-revoked.
            revoked.remove(jti, expiresAt);
            return false;
        }
        return true;
    }

    /**
     * Removes all entries whose natural expiry has passed. Safe to call from a
     * {@code @Scheduled} sweep; lazy cleanup in {@link #isRevoked} also drops
     * stale entries on read.
     *
     * @return number of entries removed.
     */
    public int sweepExpired() {
        Instant now = Instant.now();
        int removed = 0;
        for (var entry : revoked.entrySet()) {
            if (entry.getValue().isBefore(now) && revoked.remove(entry.getKey(), entry.getValue())) {
                removed++;
            }
        }
        return removed;
    }

    /** Visible for tests / introspection. Not a stable monitoring API. */
    int size() {
        return revoked.size();
    }
}
