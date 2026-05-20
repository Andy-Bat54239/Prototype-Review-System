package com.auca.prbs.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpServiceTest {

    private final OtpService otpService = new OtpService();

    @Test
    void generated_code_isExactlySixDigits() {
        var result = otpService.generate(1L, 10);

        assertTrue(result.plainCode().matches("\\d{6}"),
                "expected 6 digits, got " + result.plainCode());
    }

    @Test
    void correctCode_verifiesSuccessfully() {
        var result = otpService.generate(1L, 10);

        assertTrue(otpService.verify(result.plainCode(), result.token()));
    }

    @Test
    void wrongCode_failsVerification() {
        var result = otpService.generate(1L, 10);
        String wrong = result.plainCode().equals("000000") ? "111111" : "000000";

        assertFalse(otpService.verify(wrong, result.token()));
    }

    @Test
    void usedToken_isRejected() {
        var result = otpService.generate(1L, 10);
        result.token().setUsed(true);

        assertFalse(otpService.verify(result.plainCode(), result.token()));
    }

    @Test
    void expiredToken_isRejected() {
        var result = otpService.generate(1L, 10);
        result.token().setExpiresAt(LocalDateTime.now().minusMinutes(1));

        assertFalse(otpService.verify(result.plainCode(), result.token()));
    }

    @Test
    void consecutiveCodes_areDifferent() {
        // SecureRandom across 1,000,000 possibilities — collisions are vanishingly rare.
        // Generate 10 and assert we see at least 2 distinct values.
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            codes.add(otpService.generate(1L, 10).plainCode());
        }
        assertTrue(codes.size() > 1, "expected variety across 10 generated codes");
    }
}
