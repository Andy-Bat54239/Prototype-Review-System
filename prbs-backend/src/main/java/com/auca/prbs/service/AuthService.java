package com.auca.prbs.service;

import com.auca.prbs.dto.AuthResponse;
import com.auca.prbs.entity.Settings;
import com.auca.prbs.entity.User;
import com.auca.prbs.entity.UserStatus;
import com.auca.prbs.exception.OtpInvalidException;
import com.auca.prbs.exception.TokenInvalidException;
import com.auca.prbs.exception.TooManyRequestsException;
import com.auca.prbs.exception.UserInactiveException;
import com.auca.prbs.exception.UserNotFoundException;
import com.auca.prbs.repository.SettingsRepository;
import com.auca.prbs.repository.UserRepository;
import com.auca.prbs.security.JwtTokenProvider;
import com.auca.prbs.security.RateLimitFilter;
import com.auca.prbs.security.TokenRevocationStore;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Orchestrates the four /auth endpoints. Pulls OTP expiry from Settings, looks
 * up users by email, persists tokens via {@link OtpService}, signs JWTs via
 * {@link JwtTokenProvider}, and tracks revoked refresh tokens in
 * {@link TokenRevocationStore}.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /**
     * Approximate cost of the BCrypt hash performed on the happy path. The no-op
     * branches sleep for at least this long so an attacker cannot tell an unknown
     * email apart from a known one by response time.
     */
    private static final long EQUALIZED_PAUSE_MS = 120;

    private final UserRepository userRepository;
    private final SettingsRepository settingsRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRevocationStore tokenRevocationStore;
    private final RateLimitFilter rateLimitFilter;

    /**
     * Always returns successfully — by design. Unknown emails and inactive accounts
     * are silently no-op'd to prevent enumeration. A constant-time pause keeps the
     * response shape indistinguishable from the happy path.
     */
    public void sendOtp(String email) {
        long startNs = System.nanoTime();

        // Per-email cap defeats IP-rotation OTP-spam against a specific user.
        // We DO surface this one as 429 (rather than always-200) — at the point an
        // attacker rotates IPs and still hammers one address, hiding the limit
        // protects nothing and obscures rate-limiting from legitimate users too.
        if (!rateLimitFilter.tryEmailQuota(email)) {
            equalizeResponseTime(startNs);
            throw new TooManyRequestsException();
        }

        Optional<User> maybe = userRepository.findByEmail(email);
        if (maybe.isPresent() && maybe.get().getStatus() == UserStatus.ACTIVE) {
            User user = maybe.get();
            int expiryMinutes = settingsRepository.findById(Settings.SINGLETON_ID)
                    .map(Settings::getOtpExpiry)
                    .orElse(10);
            String code = otpService.generateAndPersist(user.getId(), expiryMinutes);
            emailService.sendOtp(user.getEmail(), user.getName(), code);
        } else {
            log.debug("send-otp called for {} — no-op (unknown or inactive)", email);
        }

        equalizeResponseTime(startNs);
    }

    private void equalizeResponseTime(long startNs) {
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
        long pad = EQUALIZED_PAUSE_MS - elapsedMs;
        if (pad > 0) {
            try { Thread.sleep(pad); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    /**
     * Unknown email, inactive account, and wrong code all surface as the same
     * {@code OTP_INVALID} response. Distinguishing them would leak which emails
     * are registered.
     */
    public AuthResponse verifyOtp(String email, String code) {
        long startNs = System.nanoTime();

        Optional<User> maybe = userRepository.findByEmail(email);
        if (maybe.isEmpty() || maybe.get().getStatus() != UserStatus.ACTIVE) {
            equalizeResponseTime(startNs);
            throw new OtpInvalidException();
        }

        User user = maybe.get();
        if (!otpService.verifyLatest(user.getId(), code)) {
            throw new OtpInvalidException();
        }
        return issueTokens(user);
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.isValid(refreshToken)) throw new TokenInvalidException();
        String jti = jwtTokenProvider.getJti(refreshToken);
        if (tokenRevocationStore.isRevoked(jti)) throw new TokenInvalidException();

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(TokenInvalidException::new);
        if (user.getStatus() != UserStatus.ACTIVE) throw new UserInactiveException();

        // Only a new access token; refresh stays the same.
        String newAccess = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole().name());
        return new AuthResponse(newAccess, refreshToken, summary(user));
    }

    public void logout(String refreshToken) {
        if (!jwtTokenProvider.isValid(refreshToken)) return; // idempotent
        Claims claims = jwtTokenProvider.parseToken(refreshToken);
        String jti = claims.getId();
        Instant exp = claims.getExpiration().toInstant();
        tokenRevocationStore.revoke(jti, exp);
    }

    private AuthResponse issueTokens(User user) {
        String access  = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole().name());
        String refresh = jwtTokenProvider.generateRefreshToken(user.getId());
        return new AuthResponse(access, refresh, summary(user));
    }

    private static AuthResponse.UserSummary summary(User user) {
        return new AuthResponse.UserSummary(user.getId(), user.getEmail(), user.getName(), user.getRole().name());
    }
}
