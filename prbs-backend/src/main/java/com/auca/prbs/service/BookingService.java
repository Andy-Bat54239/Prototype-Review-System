package com.auca.prbs.service;

import com.auca.prbs.entity.Booking;
import com.auca.prbs.entity.BookingStatus;
import com.auca.prbs.entity.User;
import com.auca.prbs.repository.BookingRepository;
import com.auca.prbs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * Phase 6: booking lifecycle with notification hooks. P3 will plug in the
 * /bookings REST endpoints; the service here is the seam where email side-effects
 * fire after persistence succeeds.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("h:mm a");

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public Booking createBooking(Booking booking) {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setReminderSent(false);
        Booking saved = bookingRepository.save(booking);

        notifyParticipants(saved, "scheduled");
        return saved;
    }

    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);

        notifyParticipants(saved, "cancelled");
        return saved;
    }

    private void notifyParticipants(Booking booking, String event) {
        User student    = userRepository.findById(booking.getStudentId()).orElseThrow();
        User supervisor = userRepository.findById(booking.getSupervisorId()).orElseThrow();

        String date = DATE_FMT.format(booking.getSlotAt());
        String time = TIME_FMT.format(booking.getSlotAt());

        if ("scheduled".equals(event)) {
            emailService.sendBookingConfirmation(
                    student.getEmail(), student.getName(),
                    booking.getProject(), date, time, booking.getMeetUrl());
        }
        emailService.sendSupervisorAlert(
                supervisor.getEmail(), supervisor.getName(),
                event, student.getName(), booking.getProject(), date, time);
    }
}
