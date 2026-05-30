package com.auca.prbs.auth.service;

import com.auca.prbs.auth.dto.AuthResponse;
import com.auca.prbs.auth.exception.OtpInvalidException;
import com.auca.prbs.auth.exception.TokenInvalidException;
import com.auca.prbs.auth.exception.TooManyRequestsException;
import com.auca.prbs.user.entity.Settings;
import com.auca.prbs.user.entity.User;
import com.auca.prbs.user.entity.UserStatus;
import com.auca.prbs.user.repository.SettingsRepository;
import com.auca.prbs.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Orchestrates the four /auth endpoints. In the monolith, cross-package collaboration
 * is direct injection (UserRepository, SettingsRepository) — no Feign anywhere. The
 * package boundary stays clean: auth never touches user's controllers, DTOs, or
 * services; only the repos. When/if we re-split into microservices later, the only
 * change is swapping these injections back to Feign clients.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final long EQUALIZED_PAUSE_MS = 120;

    private final UserRepository userRepository;
    private final SettingsRepository settingsRepository;
    private final OtpService otpService;
    private final OtpEmailSender otpEmailSender;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRevocationStore tokenRevocationStore;
    private final RateLimiter rateLimiter;

    /**
     * Always returns successfully — by design. Unknown emails and inactive accounts
     * are silently no-op'd to prevent enumeration; a constant-time pause keeps the
     * response shape indistinguishable from the happy path.
     *
     * <p>Per-email rate limit (5/min) surfaces as 429 if exceeded. We treat that
     * as an exception to the always-200 rule because at the point an attacker is
     * spamming one address, the limit is the more important signal.
     */
    public void sendOtp(String email) {
        long startNs = System.nanoTime();
        if (!rateLimiter.tryEmailQuota(email)) {
            equalize(startNs);
            throw new TooManyRequestsException();
        }

        Optional<User> maybe = userRepository.findByEmail(email);
        if (maybe.isPresent() && maybe.get().getStatus() == UserStatus.ACTIVE) {
            User user = maybe.get();
            int expiryMinutes = settingsRepository.findById(Settings.SINGLETON_ID)
                    .map(Settings::getOtpExpiry)
                    .orElse(10);
            String code = otpService.generateAndPersist(user.getId(), expiryMinutes);
            otpEmailSender.sendOtp(user.getEmail(), user.getName(), code);
        } else {
            log.debug("send-otp called for {} — no-op (unknown or inactive)", email);
        }
        equalize(startNs);
    }

    public AuthResponse verifyOtp(String email, String code) {
        long startNs = System.nanoTime();
        Optional<User> maybe = userRepository.findByEmail(email);
        if (maybe.isEmpty() || maybe.get().getStatus() != UserStatus.ACTIVE) {
            equalize(startNs);
            throw new OtpInvalidException();
        }
        User user = maybe.get();
        if (!otpService.verifyLatest(user.getId(), code)) throw new OtpInvalidException();
        return issueTokens(user);
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.isValid(refreshToken)) throw new TokenInvalidException();
        String jti = jwtTokenProvider.getJti(refreshToken);
        if (tokenRevocationStore.isRevoked(jti)) throw new TokenInvalidException();

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        User user = userRepository.findById(userId).orElseThrow(TokenInvalidException::new);
        if (user.getStatus() != UserStatus.ACTIVE) throw new TokenInvalidException();

        String newAccess = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole().name());
        return new AuthResponse(newAccess, refreshToken, summary(user));
    }

    public void logout(String refreshToken) {
        if (!jwtTokenProvider.isValid(refreshToken)) return;
        Claims claims = jwtTokenProvider.parseToken(refreshToken);
        tokenRevocationStore.revoke(claims.getId(), claims.getExpiration().toInstant());
    }

    private AuthResponse issueTokens(User user) {
        return new AuthResponse(
                jwtTokenProvider.generateAccessToken(user.getId(), user.getRole().name()),
                jwtTokenProvider.generateRefreshToken(user.getId()),
                summary(user));
    }

    private static AuthResponse.UserSummary summary(User u) {
        return new AuthResponse.UserSummary(u.getId(), u.getEmail(), u.getName(), u.getRole().name());
    }

    private static void equalize(long startNs) {
        long pad = EQUALIZED_PAUSE_MS - (System.nanoTime() - startNs) / 1_000_000;
        if (pad > 0) try { Thread.sleep(pad); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
