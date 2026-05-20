package com.auca.prbs.service;

import com.auca.prbs.entity.OtpToken;
import com.auca.prbs.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * OTP generation + verification, now backed by JPA. The previous sandbox version
 * passed an {@link OtpToken} by reference; this version owns persistence end-to-end
 * so AuthController doesn't need to know about repositories.
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    /** Token is locked (no further verifies accepted) once this many failures occur. */
    public static final int MAX_ATTEMPTS = 5;

    private final OtpTokenRepository otpTokenRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    /**
     * Generates a fresh 6-digit code, persists the hashed token, and returns the
     * plain code so the caller can email it.
     */
    @Transactional
    public String generateAndPersist(Long userId, int expiryMinutes) {
        String plain = String.format("%06d", random.nextInt(1_000_000));
        OtpToken token = OtpToken.builder()
                .userId(userId)
                .tokenHash(encoder.encode(plain))
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .used(false)
                .attempts(0)
                .build();
        otpTokenRepository.save(token);
        return plain;
    }

    /**
     * Verifies the submitted code against the user's most recent unused token.
     * On success, marks the token used. On failure, increments the attempt counter
     * (locking the token at {@link #MAX_ATTEMPTS}). Persists either way.
     *
     * @return {@code true} only if the code matched and the token was not locked,
     *         used, or expired.
     */
    @Transactional
    public boolean verifyLatest(Long userId, String submittedCode) {
        Optional<OtpToken> latest = otpTokenRepository
                .findFirstByUserIdAndUsedFalseOrderByExpiresAtDesc(userId);
        if (latest.isEmpty()) return false;

        OtpToken token = latest.get();
        if (token.getExpiresAt().isBefore(LocalDateTime.now()))  return false;
        if (token.getAttempts() >= MAX_ATTEMPTS)                 return false;

        boolean matches = encoder.matches(submittedCode, token.getTokenHash());
        if (matches) {
            token.setUsed(true);
        } else {
            token.setAttempts(token.getAttempts() + 1);
        }
        otpTokenRepository.save(token);
        return matches;
    }
}
