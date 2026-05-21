package com.auca.prbs.booking.dto;

import com.auca.prbs.booking.client.UserView;
import com.auca.prbs.booking.entity.Booking;

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
    public static BookingResponse of(Booking b, UserView student, UserView supervisor) {
        return new BookingResponse(
                b.getId(),
                b.getStudentId(),    student == null ? null : student.name(),
                b.getSupervisorId(), supervisor == null ? null : supervisor.name(),
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
