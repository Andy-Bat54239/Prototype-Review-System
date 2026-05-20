package com.auca.prbs.service;

import com.auca.prbs.exception.TokenInvalidException;
import com.auca.prbs.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Direct service-level test for the revoke-then-refresh flow. Lives at the
 * service layer (not HTTP) to dodge the JDK HttpURLConnection retry bug that
 * prevents TestRestTemplate from reading 401 responses on POSTs with a body.
 */
@SpringBootTest
class AuthServiceRevokedTokenTest {

    @Autowired AuthService authService;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @Test
    void refresh_withRevokedToken_throwsTokenInvalid() {
        Long aliceId = 1L; // first seed row
        String refresh = jwtTokenProvider.generateRefreshToken(aliceId);

        // Sanity: refresh works before logout.
        authService.refresh(refresh);

        authService.logout(refresh);

        assertThatThrownBy(() -> authService.refresh(refresh))
                .isInstanceOf(TokenInvalidException.class);
    }
}
