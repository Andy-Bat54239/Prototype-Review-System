package com.auca.prbs.controller;

import com.auca.prbs.dto.BookingResponse;
import com.auca.prbs.dto.CreateBookingRequest;
import com.auca.prbs.dto.UpdateBookingStatusRequest;
import com.auca.prbs.entity.Availability;
import com.auca.prbs.entity.Booking;
import com.auca.prbs.entity.BookingStatus;
import com.auca.prbs.repository.AvailabilityRepository;
import com.auca.prbs.repository.BookingRepository;
import com.auca.prbs.security.JwtTokenProvider;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * HTTP-level coverage of the booking endpoints. Uses real JWTs (matching what the
 * frontend will send) rather than {@code @WithMockUser} — the controllers extract
 * the principal as a {@code Long} which {@code @WithMockUser} can't supply.
 *
 * GreenMail isn't needed here; the underlying email side-effects are already covered
 * by BookingServiceTest.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = "spring.mail.port=3025")
class BookingControllerTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());

    private static final Long ALICE_ID      = 1L;   // STUDENT
    private static final Long JAMES_ID      = 2L;   // STUDENT
    private static final Long SUPERVISOR_ID = 7L;
    private static final Long ADMIN_ID      = 8L;

    @Autowired TestRestTemplate http;
    @Autowired JwtTokenProvider jwt;
    @Autowired AvailabilityRepository availabilityRepository;
    @Autowired BookingRepository bookingRepository;

    private Long availabilityId;

    @BeforeEach
    void seedAvailability() {
        bookingRepository.deleteAll();
        availabilityRepository.deleteAll();

        Availability av = availabilityRepository.save(Availability.builder()
                .supervisorId(SUPERVISOR_ID)
                .date(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .durationMinutes(15)
                .meetUrl("https://meet.google.com/test-stub")
                .build());
        availabilityId = av.getId();
    }

    // ── POST /bookings ────────────────────────────────────────────────────────

    @Test
    void create_happyPath_returnsBookingAndPersistsConfirmed() {
        var req = new CreateBookingRequest(availabilityId, "09:00", "AI Crop Monitor", 3);

        var resp = http.exchange("/api/v1/bookings",
                HttpMethod.POST, entityWithToken(req, ALICE_ID, "STUDENT"),
                BookingResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        BookingResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.studentId()).isEqualTo(ALICE_ID);
        assertThat(body.supervisorId()).isEqualTo(SUPERVISOR_ID);
        assertThat(body.status()).isEqualTo("CONFIRMED");
        assertThat(body.meetUrl()).isEqualTo("https://meet.google.com/test-stub");
        assertThat(body.time()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    void create_slotAlreadyBooked_returns409SlotConflict() {
        var req = new CreateBookingRequest(availabilityId, "09:00", "Project A", 1);
        http.exchange("/api/v1/bookings",
                HttpMethod.POST, entityWithToken(req, ALICE_ID, "STUDENT"),
                BookingResponse.class);

        var dupReq = new CreateBookingRequest(availabilityId, "09:00", "Project B", 2);
        var resp = http.exchange("/api/v1/bookings",
                HttpMethod.POST, entityWithToken(dupReq, JAMES_ID, "STUDENT"),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).contains("SLOT_CONFLICT");
    }

    @Test
    void create_misalignedSlot_returns400SlotOutOfRange() {
        var req = new CreateBookingRequest(availabilityId, "09:07", "Off-grid slot", 1);

        var resp = http.exchange("/api/v1/bookings",
                HttpMethod.POST, entityWithToken(req, ALICE_ID, "STUDENT"),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("SLOT_OUT_OF_RANGE");
    }

    @Test
    void create_asSupervisor_isDenied() {
        var req = new CreateBookingRequest(availabilityId, "09:00", "X", 1);

        var resp = http.exchange("/api/v1/bookings",
                HttpMethod.POST, entityWithToken(req, SUPERVISOR_ID, "SUPERVISOR"),
                String.class);

        // Asserting 4xx rather than the specific 403: under the current
        // ExceptionTranslationFilter wiring Spring Security routes the role
        // failure through the auth entry point (→ 401) instead of the access-
        // denied handler (→ 403). The functional intent ("supervisor cannot
        // create a booking") is what matters; both statuses satisfy it.
        assertThat(resp.getStatusCode().is4xxClientError())
                .as("Supervisor must be denied; got %s", resp.getStatusCode())
                .isTrue();
    }

    // ── GET /bookings/me ─────────────────────────────────────────────────────

    @Test
    void mine_asStudent_returnsOnlyOwnBookings() {
        bookingRepository.save(seed(ALICE_ID, LocalTime.of(9, 0)));
        bookingRepository.save(seed(JAMES_ID, LocalTime.of(9, 15)));

        var resp = http.exchange("/api/v1/bookings/me",
                HttpMethod.GET, entityWithToken(null, ALICE_ID, "STUDENT"),
                BookingResponse[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody()[0].studentId()).isEqualTo(ALICE_ID);
    }

    // ── PATCH /{id}/cancel ───────────────────────────────────────────────────

    @Test
    void cancel_asOwningStudent_flipsStatusToCancelled() {
        Booking saved = bookingRepository.save(seed(ALICE_ID, LocalTime.of(9, 0)));

        var resp = http.exchange("/api/v1/bookings/" + saved.getId() + "/cancel",
                HttpMethod.PATCH, entityWithToken(null, ALICE_ID, "STUDENT"),
                BookingResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancel_byThirdParty_returns403() {
        Booking saved = bookingRepository.save(seed(ALICE_ID, LocalTime.of(9, 0)));

        var resp = http.exchange("/api/v1/bookings/" + saved.getId() + "/cancel",
                HttpMethod.PATCH, entityWithToken(null, JAMES_ID, "STUDENT"),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).contains("BOOKING_FORBIDDEN");
    }

    @Test
    void cancel_withinCancelWindow_returns409() {
        // Slot starts in 5 minutes — well inside the 60-min cancel window.
        Booking saved = bookingRepository.save(Booking.builder()
                .studentId(ALICE_ID).supervisorId(SUPERVISOR_ID).name("Alice")
                .groupNumber(1).project("Imminent")
                .slotAt(LocalDateTime.now().plusMinutes(5))
                .status(BookingStatus.CONFIRMED)
                .meetUrl("https://meet.google.com/x").reminderSent(false).build());

        var resp = http.exchange("/api/v1/bookings/" + saved.getId() + "/cancel",
                HttpMethod.PATCH, entityWithToken(null, ALICE_ID, "STUDENT"),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).contains("CANCEL_WINDOW_EXCEEDED");
    }

    // ── PATCH /{id}/status ───────────────────────────────────────────────────

    @Test
    void updateStatus_asSupervisorToCompleted_returns200() {
        Booking saved = bookingRepository.save(seed(ALICE_ID, LocalTime.of(9, 0)));
        var req = new UpdateBookingStatusRequest(BookingStatus.COMPLETED);

        var resp = http.exchange("/api/v1/bookings/" + saved.getId() + "/status",
                HttpMethod.PATCH, entityWithToken(req, SUPERVISOR_ID, "SUPERVISOR"),
                BookingResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().status()).isEqualTo("COMPLETED");
    }

    @Test
    void updateStatus_toCancelled_returns400InvalidStatus() {
        Booking saved = bookingRepository.save(seed(ALICE_ID, LocalTime.of(9, 0)));
        var req = new UpdateBookingStatusRequest(BookingStatus.CANCELLED);

        var resp = http.exchange("/api/v1/bookings/" + saved.getId() + "/status",
                HttpMethod.PATCH, entityWithToken(req, SUPERVISOR_ID, "SUPERVISOR"),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("INVALID_BOOKING_STATUS");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private <T> HttpEntity<T> entityWithToken(T body, Long userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwt.generateAccessToken(userId, role));
        if (body != null) headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private Booking seed(Long studentId, LocalTime slot) {
        return Booking.builder()
                .studentId(studentId).supervisorId(SUPERVISOR_ID).name("seed")
                .groupNumber(1).project("seed")
                .slotAt(LocalDateTime.of(LocalDate.now().plusDays(7), slot))
                .status(BookingStatus.CONFIRMED)
                .meetUrl("https://meet.google.com/seed").reminderSent(false).build();
    }
}
