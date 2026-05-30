package com.auca.prbs.booking.service;

import com.auca.prbs.availability.entity.Availability;
import com.auca.prbs.availability.exception.AvailabilityNotFoundException;
import com.auca.prbs.availability.repository.AvailabilityRepository;
import com.auca.prbs.booking.dto.CreateBookingRequest;
import com.auca.prbs.booking.entity.Booking;
import com.auca.prbs.booking.entity.BookingStatus;
import com.auca.prbs.booking.exception.BookingAccessDeniedException;
import com.auca.prbs.booking.exception.BookingNotFoundException;
import com.auca.prbs.booking.exception.CancelWindowExceededException;
import com.auca.prbs.booking.exception.InvalidBookingStatusException;
import com.auca.prbs.booking.exception.SlotConflictException;
import com.auca.prbs.booking.exception.SlotOutsideAvailabilityException;
import com.auca.prbs.booking.repository.BookingRepository;
import com.auca.prbs.notification.service.EmailService;
import com.auca.prbs.user.entity.Settings;
import com.auca.prbs.user.entity.User;
import com.auca.prbs.user.exception.UserNotFoundException;
import com.auca.prbs.user.repository.SettingsRepository;
import com.auca.prbs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Set;

/**
 * Booking lifecycle + business rules. In the monolith, cross-package collaboration
 * is direct injection of {@link UserRepository}, {@link SettingsRepository},
 * {@link AvailabilityRepository}, and {@link EmailService}. The boundary remains
 * mappable to a microservices split: booking never touches user / availability /
 * notification controllers, DTOs, or services-without-a-repository-or-service-suffix.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    private static final Set<BookingStatus> SUPERVISOR_FINAL_STATUSES =
            EnumSet.of(BookingStatus.COMPLETED, BookingStatus.NO_SHOW);

    private final BookingRepository bookingRepository;
    private final AvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final SettingsRepository settingsRepository;
    private final EmailService emailService;

    @Transactional
    public Booking createBookingForStudent(Long studentId, CreateBookingRequest req) {
        User student = userRepository.findById(studentId).orElseThrow(UserNotFoundException::new);
        Availability availability = availabilityRepository.findById(req.availabilityId())
                .orElseThrow(AvailabilityNotFoundException::new);

        LocalTime slotTime = LocalTime.parse(req.slotTime());
        ensureSlotIsValid(availability, slotTime);
        LocalDateTime slotAt = LocalDateTime.of(availability.getDate(), slotTime);

        if (bookingRepository.existsBySupervisorIdAndSlotAtAndStatus(
                availability.getSupervisorId(), slotAt, BookingStatus.CONFIRMED)) {
            throw new SlotConflictException();
        }

        Booking saved = bookingRepository.save(Booking.builder()
                .studentId(studentId)
                .supervisorId(availability.getSupervisorId())
                .name(student.getName())
                .groupNumber(req.groupNumber())
                .project(req.project())
                .slotAt(slotAt)
                .status(BookingStatus.CONFIRMED)
                .meetUrl(availability.getMeetUrl())
                .reminderSent(false)
                .build());

        notifyParticipants(saved, "scheduled");
        return saved;
    }

    @Transactional
    public Booking cancelBookingByUser(Long bookingId, Long requestingUserId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(BookingNotFoundException::new);

        boolean isParticipant = booking.getStudentId().equals(requestingUserId)
                             || booking.getSupervisorId().equals(requestingUserId);
        if (!isParticipant) throw new BookingAccessDeniedException();

        int cancelWindow = settingsRepository.findById(Settings.SINGLETON_ID)
                .map(Settings::getCancelWindow)
                .orElse(60);
        LocalDateTime cutoff = booking.getSlotAt().minusMinutes(cancelWindow);
        if (LocalDateTime.now().isAfter(cutoff)) throw new CancelWindowExceededException(cancelWindow);

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        notifyParticipants(saved, "cancelled");
        return saved;
    }

    @Transactional
    public Booking updateStatus(Long bookingId, Long supervisorId, BookingStatus newStatus) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(BookingNotFoundException::new);
        if (!booking.getSupervisorId().equals(supervisorId)) throw new BookingAccessDeniedException();
        if (!SUPERVISOR_FINAL_STATUSES.contains(newStatus)) {
            throw new InvalidBookingStatusException("Status must be COMPLETED or NO_SHOW; cancel via /cancel");
        }
        booking.setStatus(newStatus);
        return bookingRepository.save(booking);
    }

    // ── internals ───────────────────────────────────────────────────────────

    private void ensureSlotIsValid(Availability av, LocalTime slotTime) {
        LocalTime slotEnd = slotTime.plusMinutes(av.getDurationMinutes());
        if (slotTime.isBefore(av.getStartTime()))
            throw new SlotOutsideAvailabilityException("Slot starts before the supervisor's availability window");
        if (slotEnd.isAfter(av.getEndTime()))
            throw new SlotOutsideAvailabilityException("Slot ends after the supervisor's availability window");
        int minutesFromStart = (slotTime.toSecondOfDay() - av.getStartTime().toSecondOfDay()) / 60;
        if (minutesFromStart % av.getDurationMinutes() != 0)
            throw new SlotOutsideAvailabilityException(
                    "Slot must align to a " + av.getDurationMinutes() + "-minute boundary");
    }

    private void notifyParticipants(Booking booking, String event) {
        User student    = userRepository.findById(booking.getStudentId()).orElse(null);
        User supervisor = userRepository.findById(booking.getSupervisorId()).orElse(null);
        if (student == null || supervisor == null) {
            log.warn("Skipping notifications — could not resolve users for booking {}", booking.getId());
            return;
        }

        String date = DATE_FMT.format(booking.getSlotAt());
        String time = TIME_FMT.format(booking.getSlotAt());

        try {
            if ("scheduled".equals(event)) {
                emailService.sendBookingConfirmation(
                        student.getEmail(), student.getName(),
                        booking.getProject(), date, time, booking.getMeetUrl());
            }
            emailService.sendSupervisorAlert(
                    supervisor.getEmail(), supervisor.getName(),
                    event, student.getName(), booking.getProject(), date, time);
        } catch (Exception e) {
            log.warn("Notification dispatch failed for booking {} ({}): {}", booking.getId(), event, e.getMessage());
        }
    }
}
