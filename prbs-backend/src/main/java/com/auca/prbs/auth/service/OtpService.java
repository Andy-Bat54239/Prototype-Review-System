package com.auca.prbs.auth.service;

import com.auca.prbs.auth.entity.OtpToken;
import com.auca.prbs.auth.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {

    public static final int MAX_ATTEMPTS = 5;

    private final OtpTokenRepository otpTokenRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public String generateAndPersist(Long userId, int expiryMinutes) {
        String plain = String.format("%06d", random.nextInt(1_000_000));
        otpTokenRepository.save(OtpToken.builder()
                .userId(userId)
                .tokenHash(encoder.encode(plain))
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .used(false)
                .attempts(0)
                .build());
        return plain;
    }

    @Transactional
    public boolean verifyLatest(Long userId, String submittedCode) {
        Optional<OtpToken> latest = otpTokenRepository
                .findFirstByUserIdAndUsedFalseOrderByExpiresAtDesc(userId);
        if (latest.isEmpty()) return false;

        OtpToken token = latest.get();
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) return false;
        if (token.getAttempts() >= MAX_ATTEMPTS)                return false;

        boolean matches = encoder.matches(submittedCode, token.getTokenHash());
        if (matches) token.setUsed(true);
        else         token.setAttempts(token.getAttempts() + 1);
        otpTokenRepository.save(token);
        return matches;
    }
}
