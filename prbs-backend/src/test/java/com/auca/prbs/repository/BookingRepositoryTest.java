package com.auca.prbs.repository;

import com.auca.prbs.entity.Booking;
import com.auca.prbs.entity.BookingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingRepositoryTest {

    @Autowired BookingRepository bookings;
    @Autowired UserRepository users;

    @Test
    void reminderQuery_returnsConfirmedNotRemindedInWindow_skipsOthers() {
        Long studentId    = users.findByEmail("alice@university.ac.rw").orElseThrow().getId();
        Long supervisorId = users.findByEmail("supervisor@university.ac.rw").orElseThrow().getId();
        LocalDateTime now = LocalDateTime.now();

        // In window, confirmed, not reminded → should be returned
        Booking due = save(studentId, supervisorId, now.plusMinutes(15), BookingStatus.CONFIRMED, false);
        // In window, confirmed, ALREADY reminded → skipped
        save(studentId, supervisorId, now.plusMinutes(20), BookingStatus.CONFIRMED, true);
        // In window but cancelled → skipped
        save(studentId, supervisorId, now.plusMinutes(25), BookingStatus.CANCELLED, false);
        // Out of window → skipped
        save(studentId, supervisorId, now.plusMinutes(120), BookingStatus.CONFIRMED, false);

        List<Booking> found = bookings.findByStatusAndReminderSentFalseAndSlotAtBetween(
                BookingStatus.CONFIRMED, now, now.plusMinutes(30));

        assertThat(found).extracting(Booking::getId).containsExactly(due.getId());
    }

    private Booking save(Long studentId, Long supervisorId, LocalDateTime slotAt,
                         BookingStatus status, boolean reminderSent) {
        return bookings.save(Booking.builder()
                .studentId(studentId)
                .supervisorId(supervisorId)
                .name("Alice Uwase")
                .groupNumber(3)
                .project("Test Project")
                .slotAt(slotAt)
                .status(status)
                .meetUrl("https://meet.google.com/test")
                .reminderSent(reminderSent)
                .build());
    }
}
