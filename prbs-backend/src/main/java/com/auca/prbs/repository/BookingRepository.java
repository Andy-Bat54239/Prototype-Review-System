package com.auca.prbs.repository;

import com.auca.prbs.entity.Booking;
import com.auca.prbs.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * The reminder query: confirmed bookings within the upcoming reminder window
     * that haven't been notified yet. ReminderScheduler calls this every minute.
     */
    List<Booking> findByStatusAndReminderSentFalseAndSlotAtBetween(
            BookingStatus status,
            LocalDateTime from,
            LocalDateTime until
    );

    List<Booking> findByStudentIdOrderBySlotAtDesc(Long studentId);

    List<Booking> findBySupervisorIdOrderBySlotAtDesc(Long supervisorId);

    /** Slot-conflict guard: another CONFIRMED booking already holds this supervisor's slot. */
    boolean existsBySupervisorIdAndSlotAtAndStatus(Long supervisorId, LocalDateTime slotAt, BookingStatus status);
}
