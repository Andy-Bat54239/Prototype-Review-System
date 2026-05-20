package com.auca.prbs.dto;

import com.auca.prbs.entity.Booking;
import com.auca.prbs.entity.User;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Booking as returned to the frontend. Splits {@code slotAt} into separate {@code date}
 * and {@code time} fields to match the shape the React prototype already expects
 * (MOCK_BOOKINGS_INIT in prbs-app/data.js).
 */
public record BookingResponse(
        Long id,
        Long studentId,
        String studentName,
        Long supervisorId,
        String supervisorName,
        Integer groupNumber,
        String project,
        LocalDate date,
        LocalTime time,
        String status,
        String meetUrl,
        boolean reminderSent
) {
    public static BookingResponse of(Booking b, User student, User supervisor) {
        return new BookingResponse(
                b.getId(),
                b.getStudentId(),  student == null ? null : student.getName(),
                b.getSupervisorId(), supervisor == null ? null : supervisor.getName(),
                b.getGroupNumber(),
                b.getProject(),
                b.getSlotAt().toLocalDate(),
                b.getSlotAt().toLocalTime(),
                b.getStatus().name(),
                b.getMeetUrl(),
                b.isReminderSent()
        );
    }
}
