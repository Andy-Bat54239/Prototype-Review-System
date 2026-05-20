package com.auca.prbs.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final long DEFAULT_ACCESS_EXPIRY_MS  = 15L * 60 * 1000;
    private static final long DEFAULT_REFRESH_EXPIRY_MS = 7L * 24 * 60 * 60 * 1000;

    /** Must match the fallback string in application.yml so we can detect dev-only use. */
    static final String DEV_FALLBACK_SECRET = "dev-only-fallback-secret-do-not-use-in-production-min-32";

    private final SecretKey key;
    private final long accessExpiryMs;
    private final long refreshExpiryMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiry-ms:" + DEFAULT_ACCESS_EXPIRY_MS + "}") long accessExpiryMs,
            @Value("${jwt.refresh-expiry-ms:" + DEFAULT_REFRESH_EXPIRY_MS + "}") long refreshExpiryMs) {
        if (DEV_FALLBACK_SECRET.equals(secret)) {
            log.warn("""
                    ────────────────────────────────────────────────────────────────────
                    JWT secret is the committed dev-only fallback.
                    Set the JWT_SECRET environment variable before any non-local deploy.
                    Tokens signed with this key are forgeable by anyone with repo access.
                    ────────────────────────────────────────────────────────────────────""");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiryMs = accessExpiryMs;
        this.refreshExpiryMs = refreshExpiryMs;
    }

    public String generateAccessToken(Long userId, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessExpiryMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())  // JTI — addressable for revocation
                .subject(String.valueOf(userId))
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshExpiryMs))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    /** @return the {@code jti} claim (present on refresh tokens only) or {@code null}. */
    public String getJti(String token) {
        return parseToken(token).getId();
    }
}
