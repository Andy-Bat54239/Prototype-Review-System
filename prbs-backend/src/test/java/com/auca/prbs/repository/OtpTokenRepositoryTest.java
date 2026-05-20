package com.auca.prbs.repository;

import com.auca.prbs.entity.OtpToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OtpTokenRepositoryTest {

    @Autowired OtpTokenRepository repo;
    @Autowired UserRepository users;

    @Test
    void findFirstUnusedOrderedByExpiry_returnsLatestUnused() {
        Long userId = users.findByEmail("alice@university.ac.rw").orElseThrow().getId();

        OtpToken older = repo.save(OtpToken.builder()
                .userId(userId).tokenHash("hash-old")
                .expiresAt(LocalDateTime.now().plusMinutes(2))
                .used(false).attempts(0).build());
        OtpToken newer = repo.save(OtpToken.builder()
                .userId(userId).tokenHash("hash-new")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false).attempts(0).build());

        var found = repo.findFirstByUserIdAndUsedFalseOrderByExpiresAtDesc(userId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(newer.getId());
        assertThat(found.get().getId()).isNotEqualTo(older.getId());
    }

    @Test
    void usedTokens_areSkipped() {
        Long userId = users.findByEmail("alice@university.ac.rw").orElseThrow().getId();

        repo.save(OtpToken.builder()
                .userId(userId).tokenHash("hash-used")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(true).attempts(0).build());

        var found = repo.findFirstByUserIdAndUsedFalseOrderByExpiresAtDesc(userId);

        assertThat(found).isEmpty();
    }
}
