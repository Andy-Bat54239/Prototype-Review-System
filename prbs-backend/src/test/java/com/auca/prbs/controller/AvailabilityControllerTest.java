package com.auca.prbs.controller;

import com.auca.prbs.dto.AvailabilityRequest;
import com.auca.prbs.dto.AvailabilityResponse;
import com.auca.prbs.dto.SlotResponse;
import com.auca.prbs.entity.Availability;
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

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = "spring.mail.port=3025")
class AvailabilityControllerTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());

    private static final Long ALICE_ID      = 1L;
    private static final Long SUPERVISOR_ID = 7L;

    @Autowired TestRestTemplate http;
    @Autowired JwtTokenProvider jwt;
    @Autowired AvailabilityRepository availabilityRepository;
    @Autowired BookingRepository bookingRepository;

    @BeforeEach
    void clean() {
        bookingRepository.deleteAll();
        availabilityRepository.deleteAll();
    }

    @Test
    void post_asSupervisor_createsAvailability() {
        var req = new AvailabilityRequest(
                LocalDate.now().plusDays(7),
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                15, "https://meet.google.com/abc");

        var resp = http.exchange("/api/v1/availability",
                HttpMethod.POST, entityWithToken(req, SUPERVISOR_ID, "SUPERVISOR"),
                AvailabilityResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().supervisorId()).isEqualTo(SUPERVISOR_ID);
        assertThat(resp.getBody().durationMinutes()).isEqualTo(15);
        assertThat(availabilityRepository.count()).isEqualTo(1);
    }

    @Test
    void post_asStudent_isDenied() {
        var req = new AvailabilityRequest(
                LocalDate.now().plusDays(7),
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                15, "https://meet/x");

        var resp = http.exchange("/api/v1/availability",
                HttpMethod.POST, entityWithToken(req, ALICE_ID, "STUDENT"),
                String.class);

        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
        assertThat(availabilityRepository.count()).isZero();
    }

    @Test
    void post_endBeforeStart_returns400() {
        var req = new AvailabilityRequest(
                LocalDate.now().plusDays(7),
                LocalTime.of(11, 0), LocalTime.of(9, 0),
                15, "https://meet/x");

        var resp = http.exchange("/api/v1/availability",
                HttpMethod.POST, entityWithToken(req, SUPERVISOR_ID, "SUPERVISOR"),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("VALIDATION_FAILED");
    }

    @Test
    void getList_returnsUpcomingAvailability_andCanFilterBySupervisor() {
        availabilityRepository.save(seed(LocalDate.now().plusDays(5)));
        availabilityRepository.save(seed(LocalDate.now().plusDays(10)));

        var resp = http.exchange("/api/v1/availability?supervisorId=" + SUPERVISOR_ID,
                HttpMethod.GET, entityWithToken(null, ALICE_ID, "STUDENT"),
                AvailabilityResponse[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(2);
        assertThat(resp.getBody()[0].supervisorName()).isEqualTo("Dr. Sarah Mensah");
    }

    @Test
    void getSlots_returnsAllSlotsWithFlags() {
        Availability av = availabilityRepository.save(Availability.builder()
                .supervisorId(SUPERVISOR_ID)
                .date(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .durationMinutes(15).meetUrl("https://meet/x").build());

        var resp = http.exchange("/api/v1/availability/" + av.getId() + "/slots",
                HttpMethod.GET, entityWithToken(null, ALICE_ID, "STUDENT"),
                SlotResponse[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(4);
        assertThat(resp.getBody()[0].available()).isTrue();
    }

    @Test
    void delete_byOwningSupervisor_succeeds() {
        Availability av = availabilityRepository.save(seed(LocalDate.now().plusDays(7)));

        var resp = http.exchange("/api/v1/availability/" + av.getId(),
                HttpMethod.DELETE, entityWithToken(null, SUPERVISOR_ID, "SUPERVISOR"),
                Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(availabilityRepository.findById(av.getId())).isEmpty();
    }

    private <T> HttpEntity<T> entityWithToken(T body, Long userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwt.generateAccessToken(userId, role));
        if (body != null) headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private Availability seed(LocalDate date) {
        return Availability.builder()
                .supervisorId(SUPERVISOR_ID).date(date)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(11, 0))
                .durationMinutes(15).meetUrl("https://meet/test").build();
    }
}
