package com.auca.prbs.service;

import com.auca.prbs.entity.OtpToken;
import com.auca.prbs.repository.OtpTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OtpService is part of the service layer but tested via @DataJpaTest because
 * its public API is built around the repository. @Import wires it into the slice
 * context.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OtpService.class)
class OtpServiceTest {

    private static final Long ALICE_ID = 1L; // first seed row in V6

    @Autowired OtpService otpService;
    @Autowired OtpTokenRepository tokens;

    @Test
    @Transactional
    void generateAndPersist_storesHashedTokenAndReturnsPlainCode() {
        String code = otpService.generateAndPersist(ALICE_ID, 10);

        assertThat(code).matches("\\d{6}");

        List<OtpToken> stored = tokens.findAll();
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getTokenHash()).isNotEqualTo(code); // hashed, not plaintext
        assertThat(stored.get(0).getAttempts()).isZero();
        assertThat(stored.get(0).isUsed()).isFalse();
    }

    @Test
    @Transactional
    void verifyLatest_correctCode_marksTokenUsed() {
        String code = otpService.generateAndPersist(ALICE_ID, 10);

        boolean result = otpService.verifyLatest(ALICE_ID, code);

        assertThat(result).isTrue();
        assertThat(tokens.findAll().get(0).isUsed()).isTrue();
    }

    @Test
    @Transactional
    void verifyLatest_wrongCode_incrementsAttempts() {
        String code = otpService.generateAndPersist(ALICE_ID, 10);
        String wrong = "000000".equals(code) ? "111111" : "000000";

        boolean result = otpService.verifyLatest(ALICE_ID, wrong);

        assertThat(result).isFalse();
        assertThat(tokens.findAll().get(0).getAttempts()).isEqualTo(1);
        assertThat(tokens.findAll().get(0).isUsed()).isFalse();
    }

    @Test
    @Transactional
    void verifyLatest_afterFiveFailures_locksToken() {
        String code = otpService.generateAndPersist(ALICE_ID, 10);
        String wrong = "000000".equals(code) ? "111111" : "000000";

        for (int i = 0; i < OtpService.MAX_ATTEMPTS; i++) {
            otpService.verifyLatest(ALICE_ID, wrong);
        }

        // Correct code now refused — token locked.
        assertThat(otpService.verifyLatest(ALICE_ID, code)).isFalse();
    }

    @Test
    @Transactional
    void verifyLatest_expiredToken_returnsFalse() {
        otpService.generateAndPersist(ALICE_ID, 10);
        OtpToken stored = tokens.findAll().get(0);
        stored.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        tokens.save(stored);

        assertThat(otpService.verifyLatest(ALICE_ID, "123456")).isFalse();
    }

    @Test
    @Transactional
    void verifyLatest_noTokenForUser_returnsFalse() {
        assertThat(otpService.verifyLatest(ALICE_ID, "123456")).isFalse();
    }
}
