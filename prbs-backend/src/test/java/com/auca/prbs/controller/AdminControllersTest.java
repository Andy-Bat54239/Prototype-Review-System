package com.auca.prbs.controller;

import com.auca.prbs.dto.AuthResponse;
import com.auca.prbs.dto.SettingsResponse;
import com.auca.prbs.dto.UpdateSettingsRequest;
import com.auca.prbs.dto.UpdateUserStatusRequest;
import com.auca.prbs.dto.UserResponse;
import com.auca.prbs.entity.UserStatus;
import com.auca.prbs.security.JwtTokenProvider;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * UserController, SettingsController, MeController — admin + identity endpoints.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = "spring.mail.port=3025")
class AdminControllersTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());

    private static final Long ALICE_ID      = 1L;   // STUDENT
    private static final Long CHIDI_ID      = 6L;   // STUDENT, INACTIVE
    private static final Long SUPERVISOR_ID = 7L;
    private static final Long ADMIN_ID      = 8L;

    @Autowired TestRestTemplate http;
    @Autowired JwtTokenProvider jwt;

    // ── UserController ────────────────────────────────────────────────────────

    @Test
    void users_list_asAdmin_returnsAllSeededUsers() {
        var resp = http.exchange("/api/v1/users",
                HttpMethod.GET, entityWithToken(null, ADMIN_ID, "ADMIN"),
                UserResponse[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().length).isGreaterThanOrEqualTo(8);
    }

    @Test
    void users_list_asStudent_isDenied() {
        var resp = http.exchange("/api/v1/users",
                HttpMethod.GET, entityWithToken(null, ALICE_ID, "STUDENT"),
                String.class);
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void users_patchStatus_asAdmin_togglesAndPersists() {
        // Re-activate Chidi (seeded as INACTIVE).
        var req = new UpdateUserStatusRequest(UserStatus.ACTIVE);

        var resp = http.exchange("/api/v1/users/" + CHIDI_ID + "/status",
                HttpMethod.PATCH, entityWithToken(req, ADMIN_ID, "ADMIN"),
                UserResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().status()).isEqualTo("ACTIVE");

        // Flip back so other tests aren't affected.
        http.exchange("/api/v1/users/" + CHIDI_ID + "/status", HttpMethod.PATCH,
                entityWithToken(new UpdateUserStatusRequest(UserStatus.INACTIVE),
                        ADMIN_ID, "ADMIN"),
                UserResponse.class);
    }

    // ── SettingsController ───────────────────────────────────────────────────

    @Test
    void settings_get_asAdmin_returnsSingleton() {
        var resp = http.exchange("/api/v1/settings",
                HttpMethod.GET, entityWithToken(null, ADMIN_ID, "ADMIN"),
                SettingsResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().otpExpiry()).isEqualTo(10);
        assertThat(resp.getBody().cancelWindow()).isEqualTo(60);
        assertThat(resp.getBody().reminderTime()).isEqualTo(30);
    }

    @Test
    void settings_patch_asAdmin_updatesOnlyProvidedFields() {
        var req = new UpdateSettingsRequest(null, 90, null);  // only cancelWindow

        var resp = http.exchange("/api/v1/settings",
                HttpMethod.PATCH, entityWithToken(req, ADMIN_ID, "ADMIN"),
                SettingsResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().otpExpiry()).isEqualTo(10);       // unchanged
        assertThat(resp.getBody().cancelWindow()).isEqualTo(90);
        assertThat(resp.getBody().reminderTime()).isEqualTo(30);    // unchanged

        // Restore so the next test sees the seeded value.
        http.exchange("/api/v1/settings", HttpMethod.PATCH,
                entityWithToken(new UpdateSettingsRequest(null, 60, null), ADMIN_ID, "ADMIN"),
                SettingsResponse.class);
    }

    @Test
    void settings_patch_asStudent_isDenied() {
        var req = new UpdateSettingsRequest(5, null, null);

        var resp = http.exchange("/api/v1/settings",
                HttpMethod.PATCH, entityWithToken(req, ALICE_ID, "STUDENT"),
                String.class);

        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    // ── MeController ─────────────────────────────────────────────────────────

    @Test
    void me_returnsAuthenticatedUserSummary() {
        var resp = http.exchange("/api/v1/me",
                HttpMethod.GET, entityWithToken(null, ALICE_ID, "STUDENT"),
                AuthResponse.UserSummary.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().email()).isEqualTo("alice@university.ac.rw");
        assertThat(resp.getBody().role()).isEqualTo("STUDENT");
    }

    @Test
    void me_withoutToken_returns401() {
        var resp = http.exchange("/api/v1/me",
                HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private <T> HttpEntity<T> entityWithToken(T body, Long userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwt.generateAccessToken(userId, role));
        if (body != null) headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
