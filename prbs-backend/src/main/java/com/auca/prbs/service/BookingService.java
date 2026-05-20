package com.auca.prbs.service;

import com.auca.prbs.dto.CreateBookingRequest;
import com.auca.prbs.entity.Availability;
import com.auca.prbs.entity.Booking;
import com.auca.prbs.entity.BookingStatus;
import com.auca.prbs.entity.Settings;
import com.auca.prbs.entity.User;
import com.auca.prbs.exception.AvailabilityNotFoundException;
import com.auca.prbs.exception.BookingAccessDeniedException;
import com.auca.prbs.exception.BookingNotFoundException;
import com.auca.prbs.exception.CancelWindowExceededException;
import com.auca.prbs.exception.InvalidBookingStatusException;
import com.auca.prbs.exception.SlotConflictException;
import com.auca.prbs.exception.SlotOutsideAvailabilityException;
import com.auca.prbs.exception.UserNotFoundException;
import com.auca.prbs.repository.AvailabilityRepository;
import com.auca.prbs.repository.BookingRepository;
import com.auca.prbs.repository.SettingsRepository;
import com.auca.prbs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Set;

/**
 * Booking lifecycle with notification hooks (Phase 6) plus the business rules
 * P1 owns: slot validation against the chosen Availability, slot-conflict guard,
 * cancel-window enforcement.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("h:mm a");

    /** Supervisors may flip status to either of these after the session. */
    private static final Set<BookingStatus> SUPERVISOR_FINAL_STATUSES =
            EnumSet.of(BookingStatus.COMPLETED, BookingStatus.NO_SHOW);

    private final BookingRepository bookingRepository;
    private final AvailabilityRepository availabilityRepository;
    private final SettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // ── Public API: controller-facing ────────────────────────────────────────

    /**
     * Student-initiated booking. Picks a slot inside a chosen {@link Availability};
     * the supervisor and meet URL are derived from the Availability row.
     */
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

        Booking booking = Booking.builder()
                .studentId(studentId)
                .supervisorId(availability.getSupervisorId())
                .name(student.getName())
                .groupNumber(req.groupNumber())
                .project(req.project())
                .slotAt(slotAt)
                .status(BookingStatus.CONFIRMED)
                .meetUrl(availability.getMeetUrl())
                .reminderSent(false)
                .build();
        Booking saved = bookingRepository.save(booking);

        notifyParticipants(saved, "scheduled");
        return saved;
    }

    /**
     * Cancel by the booking's student or supervisor. The {@code settings.cancel_window}
     * applies symmetrically to both — letting supervisors cancel at the last minute
     * would surprise students who already prepared to attend. Admins (out of scope
     * here) can bypass via a future force-cancel endpoint.
     */
    @Transactional
    public Booking cancelBookingByUser(Long bookingId, Long requestingUserId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(BookingNotFoundException::new);

        boolean isParticipant = booking.getStudentId().equals(requestingUserId)
                             || booking.getSupervisorId().equals(requestingUserId);
        if (!isParticipant) throw new BookingAccessDeniedException();

        int cancelWindow = settingsRepository.findById(Settings.SINGLETON_ID)
                .map(Settings::getCancelWindow)
                .orElse(60);
        LocalDateTime cutoff = booking.getSlotAt().minusMinutes(cancelWindow);
        if (LocalDateTime.now().isAfter(cutoff)) {
            throw new CancelWindowExceededException(cancelWindow);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        notifyParticipants(saved, "cancelled");
        return saved;
    }

    /**
     * Supervisor marks the session as {@code COMPLETED} or {@code NO_SHOW}.
     * Other status transitions are rejected.
     */
    @Transactional
    public Booking updateStatus(Long bookingId, Long supervisorId, BookingStatus newStatus) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(BookingNotFoundException::new);

        if (!booking.getSupervisorId().equals(supervisorId)) {
            throw new BookingAccessDeniedException();
        }
        if (!SUPERVISOR_FINAL_STATUSES.contains(newStatus)) {
            throw new InvalidBookingStatusException(
                    "Status must be COMPLETED or NO_SHOW; cancel via /cancel endpoint");
        }

        booking.setStatus(newStatus);
        return bookingRepository.save(booking);
    }

    // ── Legacy/internal API kept for backward compat with existing tests ─────

    /** Low-level helper. Skips validation, used by tests and internal flows. */
    @Transactional
    public Booking createBooking(Booking booking) {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setReminderSent(false);
        Booking saved = bookingRepository.save(booking);
        notifyParticipants(saved, "scheduled");
        return saved;
    }

    /** Low-level helper. Skips access control + cancel window; tests-only. */
    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(BookingNotFoundException::new);
        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        notifyParticipants(saved, "cancelled");
        return saved;
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private void ensureSlotIsValid(Availability av, LocalTime slotTime) {
        LocalTime slotEnd = slotTime.plusMinutes(av.getDurationMinutes());
        if (slotTime.isBefore(av.getStartTime())) {
            throw new SlotOutsideAvailabilityException(
                    "Slot starts before the supervisor's availability window");
        }
        if (slotEnd.isAfter(av.getEndTime())) {
            throw new SlotOutsideAvailabilityException(
                    "Slot ends after the supervisor's availability window");
        }
        int minutesFromStart = (slotTime.toSecondOfDay() - av.getStartTime().toSecondOfDay()) / 60;
        if (minutesFromStart % av.getDurationMinutes() != 0) {
            throw new SlotOutsideAvailabilityException(
                    "Slot must align to a " + av.getDurationMinutes() + "-minute boundary");
        }
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
