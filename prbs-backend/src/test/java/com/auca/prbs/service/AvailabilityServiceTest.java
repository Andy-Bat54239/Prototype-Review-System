package com.auca.prbs.service;

import com.auca.prbs.dto.SlotResponse;
import com.auca.prbs.entity.Availability;
import com.auca.prbs.entity.Booking;
import com.auca.prbs.entity.BookingStatus;
import com.auca.prbs.repository.AvailabilityRepository;
import com.auca.prbs.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "spring.mail.port=3025")
class AvailabilityServiceTest {

    private static final Long SUPERVISOR_ID = 7L;
    private static final Long ALICE_ID      = 1L;

    @Autowired AvailabilityService availabilityService;
    @Autowired AvailabilityRepository availabilityRepository;
    @Autowired BookingRepository bookingRepository;

    @Test
    void generateSlots_yieldsAlignedTimes_droppingPartialTrailing() {
        Availability av = Availability.builder()
                .supervisorId(SUPERVISOR_ID)
                .date(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .durationMinutes(15)
                .meetUrl("x").build();

        List<LocalTime> slots = availabilityService.generateSlots(av);

        assertThat(slots).containsExactly(
                LocalTime.of(9, 0), LocalTime.of(9, 15),
                LocalTime.of(9, 30), LocalTime.of(9, 45));
    }

    @Test
    void generateSlots_withRemainder_keepsOnlyWholeSlots() {
        Availability av = Availability.builder()
                .supervisorId(SUPERVISOR_ID)
                .date(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 10))  // 70 minutes; 15-min slots → 4 fit, last 10 min dropped
                .durationMinutes(15)
                .meetUrl("x").build();

        assertThat(availabilityService.generateSlots(av)).hasSize(4);
    }

    @Test
    @Transactional
    void getSlots_marksBookedTimesUnavailable() {
        Availability av = availabilityRepository.save(Availability.builder()
                .supervisorId(SUPERVISOR_ID)
                .date(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .durationMinutes(15).meetUrl("https://meet/x").build());

        bookingRepository.save(Booking.builder()
                .studentId(ALICE_ID).supervisorId(SUPERVISOR_ID).name("Alice").groupNumber(1)
                .project("X").slotAt(LocalDateTime.of(av.getDate(), LocalTime.of(9, 15)))
                .status(BookingStatus.CONFIRMED).meetUrl("https://meet/x").reminderSent(false).build());
        // A cancelled booking does NOT block the slot.
        bookingRepository.save(Booking.builder()
                .studentId(ALICE_ID).supervisorId(SUPERVISOR_ID).name("Alice").groupNumber(1)
                .project("Y").slotAt(LocalDateTime.of(av.getDate(), LocalTime.of(9, 30)))
                .status(BookingStatus.CANCELLED).meetUrl("https://meet/x").reminderSent(false).build());

        List<SlotResponse> slots = availabilityService.getSlotsForAvailability(av.getId());

        assertThat(slots).hasSize(4);
        assertThat(slots.stream().filter(s -> !s.available()).map(SlotResponse::time))
                .containsExactly(LocalTime.of(9, 15));
    }
}
