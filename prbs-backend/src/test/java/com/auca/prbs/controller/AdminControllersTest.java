package com.auca.prbs.controller;

import com.auca.prbs.dto.AuthResponse;
import com.auca.prbs.dto.ImportUsersResponse;
import com.auca.prbs.dto.SettingsResponse;
import com.auca.prbs.dto.UpdateSettingsRequest;
import com.auca.prbs.dto.UpdateUserStatusRequest;
import com.auca.prbs.dto.UserResponse;
import com.auca.prbs.entity.UserStatus;
import com.auca.prbs.repository.UserRepository;
import com.auca.prbs.security.JwtTokenProvider;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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
    @Autowired UserRepository userRepository;

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

    // ── CSV bulk import ──────────────────────────────────────────────────────

    @Test
    void importCsv_happyPath_createsNewUsers_skipsExistingByEmail() {
        long before = userRepository.count();
        String csv = """
                name,email,role,status
                Andy Biyonga,andy@university.ac.rw,STUDENT,ACTIVE
                Eve Murenzi,eve@university.ac.rw,SUPERVISOR
                # comment row should be ignored
                Alice Uwase,alice@university.ac.rw,STUDENT,ACTIVE
                """;

        var resp = postImport(csv, ADMIN_ID, "ADMIN");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        ImportUsersResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.imported()).isEqualTo(2);       // andy + eve
        assertThat(body.skipped()).isEqualTo(1);        // alice already seeded
        assertThat(body.errors()).isEmpty();
        assertThat(userRepository.count()).isEqualTo(before + 2);

        // Verify roles persisted correctly
        var andy = userRepository.findByEmail("andy@university.ac.rw").orElseThrow();
        assertThat(andy.getRole().name()).isEqualTo("STUDENT");
        var eve  = userRepository.findByEmail("eve@university.ac.rw").orElseThrow();
        assertThat(eve.getRole().name()).isEqualTo("SUPERVISOR");
        assertThat(eve.getStatus().name()).isEqualTo("ACTIVE");  // default
    }

    @Test
    void importCsv_invalidRoleAndBadEmail_returnPerRowErrors() {
        String csv = """
                name,email,role
                Bob,bob@university.ac.rw,GHOST
                Carol,not-an-email,STUDENT
                ,no-name@university.ac.rw,STUDENT
                """;

        var resp = postImport(csv, ADMIN_ID, "ADMIN");

        ImportUsersResponse body = resp.getBody();
        assertThat(body.imported()).isZero();
        assertThat(body.errors()).hasSize(3);
        assertThat(body.errors().get(0).message()).contains("UserRole");
        assertThat(body.errors().get(1).message()).contains("Invalid email");
        assertThat(body.errors().get(2).message()).contains("name");
    }

    @Test
    void importCsv_missingRequiredColumn_returnsHeaderError() {
        String csv = """
                name,email
                Diana,diana@university.ac.rw
                """;

        var resp = postImport(csv, ADMIN_ID, "ADMIN");

        ImportUsersResponse body = resp.getBody();
        assertThat(body.imported()).isZero();
        assertThat(body.errors()).hasSize(1);
        assertThat(body.errors().get(0).row()).isZero();
        assertThat(body.errors().get(0).message()).contains("role");
    }

    @Test
    void importCsv_emptyFile_returnsEmptyError() {
        var resp = postImport("", ADMIN_ID, "ADMIN");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().errors()).extracting("message")
                .anyMatch(m -> ((String) m).contains("empty"));
    }

    @Test
    void importCsv_asNonAdmin_isDenied() {
        // Spring Security returns 401 vs 403 inconsistently for multipart requests
        // (a known quirk of how exception translation interacts with the multipart
        // resolver). Both communicate "blocked" — the contract is that a non-admin
        // student cannot bulk-import users.
        var resp = postImport("name,email,role\nx,x@x.com,STUDENT\n",
                ALICE_ID, "STUDENT");
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
        assertThat(resp.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
    }

    private org.springframework.http.ResponseEntity<ImportUsersResponse> postImport(
            String csv, Long actorId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwt.generateAccessToken(actorId, role));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource fileResource = new ByteArrayResource(csv.getBytes()) {
            @Override public String getFilename() { return "users.csv"; }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        return http.exchange("/api/v1/users/import",
                HttpMethod.POST, new HttpEntity<>(body, headers),
                ImportUsersResponse.class);
    }
}
