package com.auca.prbs.service;

import com.auca.prbs.entity.OtpToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    /** Token is locked (no further verifies accepted) once this many failures occur. */
    public static final int MAX_ATTEMPTS = 5;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    /**
     * Returned together so the caller can email the plain code and persist the hashed token.
     */
    public record OtpResult(String plainCode, OtpToken token) {}

    public OtpResult generate(Long userId, int expiryMinutes) {
        String plain = String.format("%06d", random.nextInt(1_000_000));
        String hash  = encoder.encode(plain);

        OtpToken token = OtpToken.builder()
                .userId(userId)
                .tokenHash(hash)
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .used(false)
                .attempts(0)
                .build();

        return new OtpResult(plain, token);
    }

    /**
     * Verifies a submitted code against the token. Mutates the token's
     * {@code attempts} counter on a failed match so the caller can persist it.
     *
     * Returns false (without checking the code) if the token is null, used,
     * expired, or already at {@link #MAX_ATTEMPTS} — the lockout state.
     */
    public boolean verify(String submittedCode, OtpToken token) {
        if (token == null)                                       return false;
        if (token.isUsed())                                      return false;
        if (token.getExpiresAt().isBefore(LocalDateTime.now()))  return false;
        if (token.getAttempts() >= MAX_ATTEMPTS)                 return false;

        boolean matches = encoder.matches(submittedCode, token.getTokenHash());
        if (!matches) {
            token.setAttempts(token.getAttempts() + 1);
        }
        return matches;
    }
}
