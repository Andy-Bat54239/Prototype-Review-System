package com.auca.prbs.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private static final String SECRET = "prbs-local-dev-secret-must-be-at-least-32-chars";
    private static final long ACCESS_EXPIRY  = 15L * 60 * 1000;
    private static final long REFRESH_EXPIRY = 7L * 24 * 60 * 60 * 1000;

    private final JwtTokenProvider jwt = new JwtTokenProvider(SECRET, ACCESS_EXPIRY, REFRESH_EXPIRY);

    @Test
    void accessToken_roundTrip_preservesUserIdAndRole() {
        String token = jwt.generateAccessToken(42L, "STUDENT");

        assertTrue(jwt.isValid(token));
        assertEquals(42L, jwt.getUserId(token));
        assertEquals("STUDENT", jwt.getRole(token));
    }

    @Test
    void refreshToken_doesNotContainRoleClaim() {
        String token = jwt.generateRefreshToken(1L);

        assertTrue(jwt.isValid(token));
        assertEquals(1L, jwt.getUserId(token));
        assertNull(jwt.parseToken(token).get("role"));
    }

    @Test
    void tamperedToken_isInvalid() {
        String token = jwt.generateAccessToken(1L, "ADMIN") + "x";

        assertFalse(jwt.isValid(token));
    }

    @Test
    void tokenSignedWithDifferentSecret_isInvalid() {
        JwtTokenProvider otherIssuer = new JwtTokenProvider(
                "different-secret-also-at-least-32-characters-long",
                ACCESS_EXPIRY,
                REFRESH_EXPIRY
        );
        String foreignToken = otherIssuer.generateAccessToken(1L, "ADMIN");

        assertFalse(jwt.isValid(foreignToken));
    }

    @Test
    void expiredToken_isInvalid() throws InterruptedException {
        JwtTokenProvider shortLived = new JwtTokenProvider(SECRET, 1L, 1L);
        String token = shortLived.generateAccessToken(1L, "STUDENT");

        Thread.sleep(50);

        assertFalse(shortLived.isValid(token));
    }

    @Test
    void refreshToken_carriesJtiClaim() {
        String token = jwt.generateRefreshToken(1L);

        String jti = jwt.getJti(token);
        assertNotNull(jti, "refresh tokens must include a JTI for revocation addressing");
        assertFalse(jti.isBlank());
    }

    @Test
    void accessToken_hasNoJtiClaim() {
        // Access tokens are short-lived and not revocable; no JTI to track.
        String token = jwt.generateAccessToken(1L, "STUDENT");

        assertNull(jwt.getJti(token));
    }

    @Test
    void twoRefreshTokens_haveDistinctJtis() {
        String a = jwt.getJti(jwt.generateRefreshToken(1L));
        String b = jwt.getJti(jwt.generateRefreshToken(1L));

        assertNotEquals(a, b, "JTIs must be unique so individual tokens can be revoked");
    }
}
