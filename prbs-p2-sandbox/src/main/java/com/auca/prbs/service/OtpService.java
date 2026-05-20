package com.auca.prbs.service;

import com.auca.prbs.entity.OtpToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

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
                .build();

        return new OtpResult(plain, token);
    }

    public boolean verify(String submittedCode, OtpToken token) {
        if (token == null)                                       return false;
        if (token.isUsed())                                      return false;
        if (token.getExpiresAt().isBefore(LocalDateTime.now()))  return false;
        return encoder.matches(submittedCode, token.getTokenHash());
    }
}
