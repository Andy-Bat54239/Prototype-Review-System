package com.auca.prbs.auth.repository;

import com.auca.prbs.auth.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findFirstByUserIdAndUsedFalseOrderByExpiresAtDesc(Long userId);
}
