package com.auca.prbs.booking.repository;

import com.auca.prbs.booking.entity.Booking;
import com.auca.prbs.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByStatusAndReminderSentFalseAndSlotAtBetween(
            BookingStatus status, LocalDateTime from, LocalDateTime until);

    List<Booking> findByStudentIdOrderBySlotAtDesc(Long studentId);
    List<Booking> findBySupervisorIdOrderBySlotAtDesc(Long supervisorId);

    boolean existsBySupervisorIdAndSlotAtAndStatus(Long supervisorId, LocalDateTime slotAt, BookingStatus status);
}
