package com.auca.prbs.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryTokenRevocationStoreTest {

    private final InMemoryTokenRevocationStore store = new InMemoryTokenRevocationStore();

    @Test
    void unknownJti_isNotRevoked() {
        assertFalse(store.isRevoked("never-seen"));
    }

    @Test
    void nullJti_isNotRevoked() {
        assertFalse(store.isRevoked(null));
    }

    @Test
    void revokedJti_isReportedRevoked() {
        store.revoke("token-1", Instant.now().plus(1, ChronoUnit.HOURS));

        assertTrue(store.isRevoked("token-1"));
    }

    @Test
    void expiredEntry_isReportedNotRevoked_andLazilyEvicted() {
        store.revoke("token-old", Instant.now().plus(50, ChronoUnit.MILLIS));
        sleep(80);

        assertFalse(store.isRevoked("token-old"));
        assertEquals(0, store.size(), "lazy cleanup should drop the entry on read");
    }

    @Test
    void revokeOfAlreadyExpiredJti_isNoop() {
        store.revoke("already-stale", Instant.now().minus(1, ChronoUnit.MINUTES));

        assertEquals(0, store.size());
        assertFalse(store.isRevoked("already-stale"));
    }

    @Test
    void sweepExpired_removesPastEntries_keepsLiveOnes() {
        store.revoke("live",   Instant.now().plus(1, ChronoUnit.HOURS));
        store.revoke("stale1", Instant.now().plus(30, ChronoUnit.MILLIS));
        store.revoke("stale2", Instant.now().plus(30, ChronoUnit.MILLIS));
        sleep(60);

        int removed = store.sweepExpired();

        assertEquals(2, removed);
        assertEquals(1, store.size());
        assertTrue(store.isRevoked("live"));
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
