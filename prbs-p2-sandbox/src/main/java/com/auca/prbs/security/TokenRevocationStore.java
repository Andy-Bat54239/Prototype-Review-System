package com.auca.prbs.security;

import java.time.Instant;

/**
 * Records refresh-token JTIs that have been explicitly invalidated (e.g. via logout)
 * so that {@code POST /auth/refresh} can reject them even though their signature is
 * still valid.
 *
 * Only refresh tokens carry a JTI claim — access tokens are short-lived (15 min) and
 * are left to expire naturally rather than checked on every authenticated request.
 *
 * <p>Phase 5's {@code AuthController.logout} owns the calls to this store:
 * <pre>{@code
 *     String jti = jwtTokenProvider.getJti(refreshToken);
 *     Instant exp = jwtTokenProvider.parseToken(refreshToken).getExpiration().toInstant();
 *     tokenRevocationStore.revoke(jti, exp);
 * }</pre>
 * And {@code POST /auth/refresh} calls {@link #isRevoked(String)} before issuing a new
 * access token.
 *
 * <p>Implementations are interchangeable: the prototype ships
 * {@link InMemoryTokenRevocationStore}; a multi-instance deploy should swap in a
 * Redis- or JPA-backed implementation without changing any caller.
 */
public interface TokenRevocationStore {

    /**
     * Marks the given JTI as revoked. The entry can be safely discarded once
     * {@code expiresAt} has passed (an unrevoked token with that JTI would also be
     * invalid by then due to its own expiry claim).
     */
    void revoke(String jti, Instant expiresAt);

    /**
     * @return {@code true} if this JTI has been revoked and the entry hasn't yet
     *         passed its natural expiry; {@code false} otherwise (including for
     *         {@code null} or unknown JTIs).
     */
    boolean isRevoked(String jti);
}
