package com.auca.prbs.booking.service;

import com.auca.prbs.booking.client.NotificationClient;
import com.auca.prbs.booking.client.SettingsClient;
import com.auca.prbs.booking.client.UserServiceClient;
import com.auca.prbs.booking.client.UserView;
import com.auca.prbs.booking.dto.CreateBookingRequest;
import com.auca.prbs.booking.entity.Availability;
import com.auca.prbs.booking.entity.Booking;
import com.auca.prbs.booking.entity.BookingStatus;
import com.auca.prbs.booking.exception.BookingExceptions.*;
import com.auca.prbs.booking.repository.AvailabilityRepository;
import com.auca.prbs.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Booking lifecycle + business rules. Cross-service calls:
 *   - user-service via {@link UserServiceClient} for student/supervisor details
 *   - user-service via {@link SettingsClient} for cancel_window minutes
 *   - notification-service via {@link NotificationClient} to fire emails
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
    private final UserServiceClient userServiceClient;
    private final SettingsClient settingsClient;
    private final NotificationClient notificationClient;

    @Transactional
    public Booking createBookingForStudent(Long studentId, CreateBookingRequest req) {
        UserView student = userServiceClient.findById(studentId).orElseThrow(UserNotFoundException::new);
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
                .name(student.name())
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

        int cancelWindow = safeCancelWindow();
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

    // ── internals ────────────────────────────────────────────────────────────

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

    private int safeCancelWindow() {
        try {
            return settingsClient.get().cancelWindow();
        } catch (Exception e) {
            log.warn("Settings lookup failed; defaulting cancel window to 60 min", e);
            return 60;
        }
    }

    /** Hydrates student/supervisor names and fires the appropriate email side-effects. */
    private void notifyParticipants(Booking booking, String event) {
        UserView student    = userServiceClient.findById(booking.getStudentId()).orElse(null);
        UserView supervisor = userServiceClient.findById(booking.getSupervisorId()).orElse(null);
        if (student == null || supervisor == null) {
            log.warn("Skipping notifications — could not resolve users for booking {}", booking.getId());
            return;
        }

        String date = DATE_FMT.format(booking.getSlotAt());
        String time = TIME_FMT.format(booking.getSlotAt());

        try {
            if ("scheduled".equals(event)) {
                notificationClient.sendBookingConfirmation(payload(student, supervisor, booking, event, date, time));
            }
            notificationClient.sendSupervisorAlert(payload(student, supervisor, booking, event, date, time));
        } catch (Exception e) {
            // Don't fail the booking just because the email side-effect failed.
            log.warn("Notification dispatch failed for booking {} ({}): {}", booking.getId(), event, e.getMessage());
        }
    }

    private static Map<String, String> payload(UserView student, UserView supervisor, Booking booking,
                                               String event, String date, String time) {
        Map<String, String> p = new HashMap<>();
        p.put("studentEmail",    student.email());
        p.put("studentName",     student.name());
        p.put("supervisorEmail", supervisor.email());
        p.put("supervisorName",  supervisor.name());
        p.put("project",         booking.getProject());
        p.put("date",            date);
        p.put("time",            time);
        p.put("meetUrl",         booking.getMeetUrl());
        p.put("event",           event);
        return p;
    }
}
