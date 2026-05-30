package com.auca.prbs.booking.dto;

import com.auca.prbs.booking.entity.Booking;
import com.auca.prbs.user.entity.User;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingResponse(
        Long id,
        Long studentId, String studentName,
        Long supervisorId, String supervisorName,
        Integer groupNumber, String project,
        LocalDate date, LocalTime time,
        String status, String meetUrl, boolean reminderSent
) {
    public static BookingResponse of(Booking b, User student, User supervisor) {
        return new BookingResponse(
                b.getId(),
                b.getStudentId(),    student == null ? null : student.getName(),
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
