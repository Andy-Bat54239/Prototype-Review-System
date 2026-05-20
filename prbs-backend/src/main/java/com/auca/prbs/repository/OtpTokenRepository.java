package com.auca.prbs.repository;

import com.auca.prbs.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    /** The most recent unused token issued for this user — the one verify-otp should compare against. */
    Optional<OtpToken> findFirstByUserIdAndUsedFalseOrderByExpiresAtDesc(Long userId);
}
