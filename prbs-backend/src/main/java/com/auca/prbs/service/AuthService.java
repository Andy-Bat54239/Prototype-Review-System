package com.auca.prbs.service;

import com.auca.prbs.dto.AuthResponse;
import com.auca.prbs.entity.Settings;
import com.auca.prbs.entity.User;
import com.auca.prbs.entity.UserStatus;
import com.auca.prbs.exception.OtpInvalidException;
import com.auca.prbs.exception.TokenInvalidException;
import com.auca.prbs.exception.UserInactiveException;
import com.auca.prbs.exception.UserNotFoundException;
import com.auca.prbs.repository.SettingsRepository;
import com.auca.prbs.repository.UserRepository;
import com.auca.prbs.security.JwtTokenProvider;
import com.auca.prbs.security.TokenRevocationStore;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Orchestrates the four /auth endpoints. Pulls OTP expiry from Settings, looks
 * up users by email, persists tokens via {@link OtpService}, signs JWTs via
 * {@link JwtTokenProvider}, and tracks revoked refresh tokens in
 * {@link TokenRevocationStore}.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SettingsRepository settingsRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRevocationStore tokenRevocationStore;

    public void sendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);
        if (user.getStatus() != UserStatus.ACTIVE) throw new UserInactiveException();

        int expiryMinutes = settingsRepository.findById(Settings.SINGLETON_ID)
                .map(Settings::getOtpExpiry)
                .orElse(10);

        String code = otpService.generateAndPersist(user.getId(), expiryMinutes);
        emailService.sendOtp(user.getEmail(), user.getName(), code);
    }

    public AuthResponse verifyOtp(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);
        if (user.getStatus() != UserStatus.ACTIVE) throw new UserInactiveException();

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
