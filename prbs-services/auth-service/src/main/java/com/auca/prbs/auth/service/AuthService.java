package com.auca.prbs.auth.service;

import com.auca.prbs.auth.client.NotificationClient;
import com.auca.prbs.auth.client.UserServiceClient;
import com.auca.prbs.auth.client.UserView;
import com.auca.prbs.auth.dto.AuthResponse;
import com.auca.prbs.auth.exception.OtpInvalidException;
import com.auca.prbs.auth.exception.TokenInvalidException;
import com.auca.prbs.auth.exception.TooManyRequestsException;
import com.auca.prbs.shared.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final long EQUALIZED_PAUSE_MS = 120;

    private final UserServiceClient userServiceClient;
    private final NotificationClient notificationClient;
    private final OtpEmailSender otpEmailSenderFallback;
    private final OtpService otpService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRevocationStore tokenRevocationStore;
    private final RateLimiter rateLimiter;

    /** OTP expiry — pulled from user-service settings later; constant for now to keep auth-service free of user-service Settings shape. */
    @Value("${auth.otp-expiry-minutes:10}")
    private int otpExpiryMinutes;

    public void sendOtp(String email) {
        long startNs = System.nanoTime();
        if (!rateLimiter.tryEmailQuota(email)) {
            equalize(startNs);
            throw new TooManyRequestsException();
        }

        Optional<UserView> maybe = userServiceClient.findByEmail(email);
        if (maybe.isPresent() && "ACTIVE".equals(maybe.get().status())) {
            UserView user = maybe.get();
            String code = otpService.generateAndPersist(user.id(), otpExpiryMinutes);
            deliverOtp(user, code);
        } else {
            log.debug("send-otp called for {} — no-op (unknown or inactive)", email);
        }
        equalize(startNs);
    }

    /**
     * Try notification-service first; if it's unreachable (it's the only thing
     * standing between a user and being able to log in), fall back to inline SMTP.
     * Auth must work even when other services degrade.
     */
    private void deliverOtp(UserView user, String code) {
        try {
            notificationClient.sendOtp(Map.of(
                    "email", user.email(), "name", user.name(), "code", code));
        } catch (Exception e) {
            log.warn("notification-service unreachable; falling back to inline OTP email: {}", e.getMessage());
            otpEmailSenderFallback.sendOtp(user.email(), user.name(), code);
        }
    }

    public AuthResponse verifyOtp(String email, String code) {
        long startNs = System.nanoTime();
        Optional<UserView> maybe = userServiceClient.findByEmail(email);
        if (maybe.isEmpty() || !"ACTIVE".equals(maybe.get().status())) {
            equalize(startNs);
            throw new OtpInvalidException();
        }
        UserView user = maybe.get();
        if (!otpService.verifyLatest(user.id(), code)) throw new OtpInvalidException();
        return issueTokens(user);
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.isValid(refreshToken)) throw new TokenInvalidException();
        String jti = jwtTokenProvider.getJti(refreshToken);
        if (tokenRevocationStore.isRevoked(jti)) throw new TokenInvalidException();

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        // Re-fetch role from user-service so role changes propagate on refresh.
        // Falls back to OtpInvalid-style failure to avoid leaking that the user was deleted.
        UserView user = userServiceClient.findByEmail(jwtTokenProvider.parseToken(refreshToken).getSubject())
                .orElseThrow(TokenInvalidException::new);
        if (!"ACTIVE".equals(user.status())) throw new TokenInvalidException();

        String newAccess = jwtTokenProvider.generateAccessToken(user.id(), user.role());
        return new AuthResponse(newAccess, refreshToken, summary(user));
    }

    public void logout(String refreshToken) {
        if (!jwtTokenProvider.isValid(refreshToken)) return;
        Claims claims = jwtTokenProvider.parseToken(refreshToken);
        tokenRevocationStore.revoke(claims.getId(), claims.getExpiration().toInstant());
    }

    private AuthResponse issueTokens(UserView u) {
        return new AuthResponse(
                jwtTokenProvider.generateAccessToken(u.id(), u.role()),
                jwtTokenProvider.generateRefreshToken(u.id()),
                summary(u));
    }

    private static AuthResponse.UserSummary summary(UserView u) {
        return new AuthResponse.UserSummary(u.id(), u.email(), u.name(), u.role());
    }

    private static void equalize(long startNs) {
        long pad = EQUALIZED_PAUSE_MS - (System.nanoTime() - startNs) / 1_000_000;
        if (pad > 0) try { Thread.sleep(pad); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
