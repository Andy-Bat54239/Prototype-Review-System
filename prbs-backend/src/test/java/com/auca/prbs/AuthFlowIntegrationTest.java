package com.auca.prbs;

import com.auca.prbs.dto.AuthResponse;
import com.auca.prbs.entity.OtpToken;
import com.auca.prbs.repository.OtpTokenRepository;
import com.auca.prbs.repository.UserRepository;
import com.auca.prbs.security.JwtTokenProvider;
import com.auca.prbs.security.RateLimitFilter;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end happy path covering Phase 5:
 *   send-otp → email arrives → verify-otp → tokens issued → logout revokes refresh
 *
 * Uses GreenMail to capture the OTP email (the only way to learn the 6-digit
 * code, since OtpService hashes it before persisting).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.mail.port=3025",
        "spring.mail.host=127.0.0.1"
})
class AuthFlowIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());

    @Autowired TestRestTemplate http;
    @Autowired UserRepository userRepository;
    @Autowired OtpTokenRepository otpTokenRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired RateLimitFilter rateLimitFilter;

    @BeforeEach
    void resetState() throws Exception {
        // Per-IP and per-email counters share singleton state across tests.
        rateLimitFilter.reset();
        // GreenMail accumulates messages across tests; clear so each test reads
        // only its own delivery.
        greenMail.purgeEmailFromAllMailboxes();
    }

    @Test
    void fullAuthFlow_happyPath() {
        // 1. send-otp
        var sendResp = http.postForEntity("/api/v1/auth/send-otp",
                java.util.Map.of("email", "alice@university.ac.rw"), Void.class);
        assertThat(sendResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 2. extract OTP from Mailpit-equivalent (GreenMail)
        String code = extractSixDigitOtp();

        // 3. verify-otp
        var verifyResp = http.postForEntity("/api/v1/auth/verify-otp",
                java.util.Map.of("email", "alice@university.ac.rw", "code", code),
                AuthResponse.class);
        assertThat(verifyResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthResponse auth = verifyResp.getBody();
        assertThat(auth).isNotNull();
        assertThat(auth.accessToken()).isNotBlank();
        assertThat(auth.refreshToken()).isNotBlank();
        assertThat(auth.user().email()).isEqualTo("alice@university.ac.rw");
        assertThat(auth.user().role()).isEqualTo("STUDENT");

        // Token is consumed — verify the latest unused-token list is empty for alice
        OtpToken latestUsed = otpTokenRepository.findAll().stream()
                .filter(t -> t.getUserId().equals(auth.user().id()))
                .reduce((a, b) -> b)  // last
                .orElseThrow();
        assertThat(latestUsed.isUsed()).isTrue();

        // 4. logout revokes the refresh token
        var logoutResp = http.postForEntity("/api/v1/auth/logout",
                java.util.Map.of("refreshToken", auth.refreshToken()), Void.class);
        assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // Revoked-token rejection on /refresh is covered by AuthServiceRevokedTokenTest
        // (service-level, avoids the JDK HttpURLConnection PATCH/401 streaming bug).
    }

    @Test
    void sendOtp_unknownEmail_returns200_silentlyNoEmail() {
        int before = greenMail.getReceivedMessages().length;

        var resp = http.postForEntity("/api/v1/auth/send-otp",
                java.util.Map.of("email", "nobody@nowhere.example"), Void.class);

        // Always-200 to prevent email enumeration.
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        // No email was sent — the success response is a decoy.
        assertThat(greenMail.getReceivedMessages().length).isEqualTo(before);
    }

    @Test
    void sendOtp_inactiveUser_returns200_silentlyNoEmail() {
        int before = greenMail.getReceivedMessages().length;

        var resp = http.postForEntity("/api/v1/auth/send-otp",
                java.util.Map.of("email", "chidi@university.ac.rw"), Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(greenMail.getReceivedMessages().length).isEqualTo(before);
    }

    @Test
    void sendOtp_sixthCallToSameEmail_returns429() {
        String email = "alice@university.ac.rw";
        for (int i = 0; i < RateLimitFilter.MAX_SEND_OTP_PER_EMAIL_PER_MINUTE; i++) {
            var ok = http.postForEntity("/api/v1/auth/send-otp",
                    java.util.Map.of("email", email), Void.class);
            assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        var resp = http.postForEntity("/api/v1/auth/send-otp",
                java.util.Map.of("email", email), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(resp.getBody()).contains("RATE_LIMITED");
    }

    @Test
    void verifyOtp_unknownEmail_returns401OtpInvalid_notUserNotFound() {
        var resp = http.postForEntity("/api/v1/auth/verify-otp",
                java.util.Map.of("email", "nobody@nowhere.example", "code", "123456"),
                String.class);

        // Collapses to OTP_INVALID so attackers can't distinguish unknown email from wrong code.
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).contains("OTP_INVALID");
        assertThat(resp.getBody()).doesNotContain("USER_NOT_FOUND");
    }

    /** Pulls the 6-digit OTP out of the most recent email body. */
    private String extractSixDigitOtp() {
        var msgs = greenMail.getReceivedMessages();
        assertThat(msgs).as("no email received").isNotEmpty();
        String body = GreenMailUtil.getBody(msgs[msgs.length - 1]);
        // Walk digit runs; first one of exactly 6 digits is the OTP. (MIME boundary numbers
        // are >6 digits so they don't collide.)
        Matcher anyDigit = Pattern.compile("(\\d+)").matcher(body);
        while (anyDigit.find()) {
            if (anyDigit.group(1).length() == 6) return anyDigit.group(1);
        }
        throw new AssertionError("6-digit OTP not found in email body:\n" + body);
    }
}
